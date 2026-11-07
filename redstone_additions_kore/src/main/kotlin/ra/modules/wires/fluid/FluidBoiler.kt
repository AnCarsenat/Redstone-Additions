package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object FluidBoiler : PackPart {
	val boilerCleanup = ref(Ns.WIRES, "fluid/boiler_cleanup")
	val boilerScanDst = ref(Ns.WIRES, "fluid/boiler_scan_dst")
	val boilerScanSrc = ref(Ns.WIRES, "fluid/boiler_scan_src")
	val boilerTick = ref(Ns.WIRES, "fluid/boiler_tick")

	override fun Ra.register() {
		pack.define(FluidBoiler.boilerCleanup) {
			// Internal: drop the per-boiler working tags.

			raw("tag @e[type=marker,tag=ra.wires.boil_src] remove ra.wires.boil_src")
			raw("tag @e[type=marker,tag=ra.wires.boil_dst] remove ra.wires.boil_dst")
		}

		pack.define(FluidBoiler.boilerScanDst) {
			// Internal: claim the first adjacent network that can take the steam.
			// Context: as a neighbouring fluid node.

			raw("execute if score #boil_dst ra.wires.tmp matches 1 run return 0")

			// Not the network the water is coming from — that one is full of water.
			raw("execute if entity @s[tag=ra.wires.boil_src] run return 0")
			raw("execute if score @s ra.tr.net = #boil_src_net ra.wires.tmp run return 0")

			function(RaLib.Transport.Net.read)
			// No medium test at all. A network holding something else is not a refusal any
			// more -- steam has its own entry alongside whatever is already in there, and the
			// only thing that can turn the boiler away is having no room for 1000 mL.

			scoreboard.players.operation(literal("#boil_free"), "ra.wires.tmp", Operation.SET, literal("#net_capacity"), "ra.tr.tmp")
			scoreboard.players.operation(literal("#boil_free"), "ra.wires.tmp", Operation.REMOVE, literal("#net_amount"), "ra.tr.tmp")
			raw("execute unless score #boil_free ra.wires.tmp matches 1000.. run return 0")

			scoreboard.players.set(literal("#boil_dst"), "ra.wires.tmp", 1)
			tag(self()) { add("ra.wires.boil_dst") }
		}

		pack.define(FluidBoiler.boilerScanSrc) {
			// Internal: claim the first adjacent network holding enough water.
			// Context: as a neighbouring fluid node.

			raw("execute if score #boil_src ra.wires.tmp matches 1 run return 0")

			function(RaLib.Transport.Net.read)
			// Enough WATER, not enough of whatever the network calls its primary.
			raw("execute store result score #boil_water ra.wires.tmp run data get storage ra:transport cur.amounts.water")
			raw("execute unless score #boil_water ra.wires.tmp matches 1000.. run return 0")

			scoreboard.players.set(literal("#boil_src"), "ra.wires.tmp", 1)
			scoreboard.players.operation(literal("#boil_src_net"), "ra.wires.tmp", Operation.SET, self(), "ra.tr.net")
			tag(self()) { add("ra.wires.boil_src") }
		}

		pack.define(FluidBoiler.boilerTick) {
			// Turn water into steam over a heat source.
			// Context: as the boiler marker, at the boiler position.
			//
			// The boiler is deliberately NOT a member of either network. A node belongs to
			// exactly one network and a network holds one medium, so a boiler that joined
			// would merge its water side and its steam side into a single network that could
			// only ever hold one of them. Instead it reaches into the networks of the nodes
			// next to it: it takes water out of one and offers steam to another.
			//
			// This is the point of the whole fluid system — water in one side, steam out the
			// other, and an EU Generator next to the steam side turning it into power.

			scoreboard.players.add(self(), Obj.COOLDOWN, 1)
			raw("execute unless score @s ${Obj.COOLDOWN} matches 20.. run return 0")
			scoreboard.players.set(self(), Obj.COOLDOWN, 0)

			raw("data modify entity @s data.status.boiler_state set value \"no_heat\"")
			raw("execute unless block ~ ~-1 ~ #ra_wires:heat_sources run return 0")

			scoreboard.players.set(literal("#boil_src"), "ra.wires.tmp", 0)
			scoreboard.players.set(literal("#boil_dst"), "ra.wires.tmp", 0)

			// Water side.
			executeRun("positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanSrc)
			executeRun("positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanSrc)
			executeRun("positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanSrc)
			executeRun("positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanSrc)
			executeRun("positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanSrc)

			raw("data modify entity @s data.status.boiler_state set value \"no_water\"")
			raw("execute if score #boil_src ra.wires.tmp matches 0 run return 0")

			// Steam side. Never the same network the water came from.
			executeRun("positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanDst)
			executeRun("positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanDst)
			executeRun("positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanDst)
			executeRun("positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanDst)
			executeRun("positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", FluidBoiler.boilerScanDst)

			raw("data modify entity @s data.status.boiler_state set value \"no_outlet\"")
			executeRun("if score #boil_dst ra.wires.tmp matches 0", FluidBoiler.boilerCleanup)
			raw("execute if score #boil_dst ra.wires.tmp matches 0 run return 0")

			// Both sides confirmed, so neither half of this can fail and strand the water.
			executeRun("as @e[type=marker,tag=ra.wires.boil_src,limit=1]", RaLib.Transport.Net.take, """{amount:1000,medium:"water"}""")
			executeRun("as @e[type=marker,tag=ra.wires.boil_dst,limit=1]", RaLib.Transport.Net.offer, """{amount:1000,medium:"steam"}""")

			function(FluidBoiler.boilerCleanup)

			raw("particle minecraft:cloud ~ ~1 ~ 0.3 0.2 0.3 0.02 12 normal ${particleViewersSel()}")
			raw("playsound minecraft:block.fire.extinguish block ${soundListenersSel(12)} ~ ~ ~ 0.3 1.6")
			raw("data modify entity @s data.status.boiler_state set value \"boiling\"")
		}
	}
}
