package ra.modules.wires.electric

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.electric_generator.ElectricGenerator
import ra.modules.wires.common.line

object Generator : PackPart {
	val consumeSteam = ref(Ns.WIRES, "electric/consume_steam")
	val generatorTick = ref(Ns.WIRES, "electric/generator_tick")

	override fun Ra.register() {
		pack.define(Generator.consumeSteam) {
			// Internal: burn steam from this neighbouring gas network to fuel the generator.
			// Context: as a neighbouring fluid node.

			raw("execute if score #eu_fuel ra.wires.tmp2 matches 1 run return 0")

			function(RaLib.Transport.Net.read)
			// How much STEAM is here, not how much the network holds. A gas run carrying
			// steam and smoke together still fuels a generator; the primary medium is
			// whichever arrived first and says nothing about whether steam is present.
			raw("execute store result score #eu_steam ra.wires.tmp run data get storage ra:transport cur.amounts.steam")
			raw("execute unless score #eu_steam ra.wires.tmp matches 20.. run return 0")

			line("execute store result score #steam_used ra.wires.tmp run function", RaLib.Transport.Net.take, """{amount:20,medium:"steam"}""")
			raw("execute if score #steam_used ra.wires.tmp matches 1.. run scoreboard players set #eu_fuel ra.wires.tmp2 1")
		}

		pack.define(Generator.generatorTick) {
			// Burn fuel and put the EU it makes onto the grid.
			// Context: as the generator marker, at its position.
			//
			// THE BLOCK IS A BARREL WEARING A FURNACE
			// It used to be a real blast furnace, which meant the block you interacted with
			// had a furnace's own UI: two input slots and a fuel slot, of which only the fuel
			// slot meant anything. Players quite reasonably put ore in the top and waited.
			// A barrel is a plain inventory — drop coal in and that is the whole interaction —
			// and ra_lib:skin/apply puts the furnace back on the outside so it still reads as
			// one. See that function for why mechanics and appearance are separable here.
			//
			// Steam still works. A generator piped into a boiler's steam network burns that
			// instead, so the water -> Boiler -> steam -> EU chain is intact; solid fuel is
			// the direct route, steam is the built one.

			function(RaLib.Util.property, nbt { this["name"] = "generation_rate"; this["default"] = 60; this["min"] = 1 })
			raw("scoreboard players operation #gen.rate ra.wires.tmp = #prop ra.temp")

			// --- Solid fuel, out of the barrel ---
			scoreboard.players.set(literal("#gen.burning"), "ra.wires.tmp", 0)

			// Nothing alight: try to light something.
			line("execute unless data entity @s data.data.burn run function", Fuel.scan)

			raw("execute if data entity @s data.data.burn run scoreboard players set #gen.burning ra.wires.tmp 1")
			raw("execute if score #gen.burning ra.wires.tmp matches 1 store result score #gen.left ra.wires.tmp run data get entity @s data.data.burn 1")
			raw("execute if score #gen.burning ra.wires.tmp matches 1 run scoreboard players remove #gen.left ra.wires.tmp 1")
			raw("execute if score #gen.burning ra.wires.tmp matches 1 store result entity @s data.data.burn int 1 run scoreboard players get #gen.left ra.wires.tmp")
			raw("execute if score #gen.burning ra.wires.tmp matches 1 if score #gen.left ra.wires.tmp matches ..0 run data remove entity @s data.data.burn")

			// --- Steam, out of an adjacent gas network ---
			scoreboard.players.set(literal("#eu_fuel"), "ra.wires.tmp2", 0)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			line("execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function", Generator.consumeSteam)
			raw("execute if score #eu_fuel ra.wires.tmp2 matches 1.. run scoreboard players set #gen.burning ra.wires.tmp 1")
			raw("execute if score #eu_fuel ra.wires.tmp2 matches 1.. run data modify entity @s data.status.fuel set value \"Steam\"")

			line("execute if score #gen.burning ra.wires.tmp matches 0 run function", ElectricGenerator.idle)
			raw("execute if score #gen.burning ra.wires.tmp matches 0 run data modify entity @s data.status.fuel set value \"No fuel\"")
			raw("execute if score #gen.burning ra.wires.tmp matches 0 run data modify entity @s data.status.active set value 0b")
			raw("execute if score #gen.burning ra.wires.tmp matches 0 run return 0")

			// Offer this tick's output to the grid. The network decides how much it can hold
			// and hands back the rest as a refusal — there is no buffer here to overflow.
			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #gen.rate ra.wires.tmp")
			line("execute store result score #eu_made ra.wires.tmp run function", EuApi.offerEu, "with storage ra:wires eu")

			function(ElectricGenerator.running)

			// ACTIVE MEANS BURNING, NOT ACCEPTED
			// This used to set active from how much the grid took, so a generator burning
			// happily into a grid that was already full reported itself as inactive — which
			// is what a generator with no Battery on its grid does the moment it has filled
			// the 50 EU the generator itself contributes. The fuel goes down, the skin is
			// lit, and the readout says it is doing nothing. Burning is the thing this block
			// controls, so burning is what it reports; where the EU went is a separate line.
			raw("data modify entity @s data.status.active set value 1b")
			raw("execute store result entity @s data.status.output int 1 run scoreboard players get #eu_made ra.wires.tmp")
			raw("execute if score #eu_made ra.wires.tmp matches ..0 run data modify entity @s data.status.grid set value \"Full - add a Battery\"")
			raw("execute if score #eu_made ra.wires.tmp matches 1.. run data modify entity @s data.status.grid set value \"Accepting\"")
		}
	}
}
