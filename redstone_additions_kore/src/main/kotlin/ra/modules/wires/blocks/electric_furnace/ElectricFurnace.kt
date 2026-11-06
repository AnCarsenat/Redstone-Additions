package ra.modules.wires.blocks.electric_furnace

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line
import ra.modules.wires.electric.EuApi

object ElectricFurnace : PackPart {
	val cooking = ref(Ns.WIRES, "blocks/electric_furnace/cooking")
	val idle = ref(Ns.WIRES, "blocks/electric_furnace/idle")
	val idleBlocked = ref(Ns.WIRES, "blocks/electric_furnace/idle_blocked")
	val idleEmpty = ref(Ns.WIRES, "blocks/electric_furnace/idle_empty")
	val idleNopower = ref(Ns.WIRES, "blocks/electric_furnace/idle_nopower")
	val refreshDisplay = ref(Ns.WIRES, "blocks/electric_furnace/refresh_display")
	val running = ref(Ns.WIRES, "blocks/electric_furnace/running")
	val stayLit = ref(Ns.WIRES, "blocks/electric_furnace/stay_lit")
	val tick = ref(Ns.WIRES, "blocks/electric_furnace/tick")

	override fun Ra.register() {
		pack.define(ElectricFurnace.cooking) {
			// Working, but not finishing an item this tick. Context: as the marker, at the block.
			//
			// Reached only once tick.mcfunction has established all three conditions: there is
			// something smeltable in the barrel, the destination will take the result, and the
			// grid is holding enough EU for one operation. That is what "cooking" means, so
			// that is what the block is drawn as -- the cooldown only decides which of those
			// ticks is the one an item actually comes out on.
			//
			// No particles and no sound: those mark a completed item and belong to `running`.
			// Emitting them here would turn superpowered into a smoke column.

			raw("data modify entity @s data.status.state set value \"Smelting\"")
			function(ElectricFurnace.stayLit)
		}

		pack.define(ElectricFurnace.idle) {
			// Not working this tick. Context: as the marker, at the block.
			//
			// The repaint is conditional on having BEEN lit, so a furnace sitting idle costs
			// one tag test rather than a skin rebuild every tick.

			raw("execute if entity @s[tag=ra.wires.ef_was_lit] run tag @s remove ra.wires.ef_lit")
			line("execute if entity @s[tag=ra.wires.ef_was_lit] run function", ElectricFurnace.refreshDisplay)
			tag(self()) { remove("ra.wires.ef_was_lit") }
			raw("data modify entity @s data.status.active set value 0b")
			raw("execute if block ~ ~1 ~ minecraft:light[level=10] run setblock ~ ~1 ~ minecraft:air")
		}

		pack.define(ElectricFurnace.idleBlocked) {
			// The destination is missing or full. Context: as the marker, at the block.

			raw("data modify entity @s data.status.state set value \"Output blocked\"")
			function(ElectricFurnace.idle)
		}

		pack.define(ElectricFurnace.idleEmpty) {
			// Nothing smeltable in the input rows. Context: as the marker, at the block.

			raw("data modify entity @s data.status.state set value \"Nothing to smelt\"")
			function(ElectricFurnace.idle)
		}

		pack.define(ElectricFurnace.idleNopower) {
			// The grid could not cover one operation. Context: as the marker, at the block.
			//
			// The cooldown is deliberately NOT reset here, so the furnace retries next tick
			// rather than waiting out a full cycle after a momentary brownout.

			raw("data modify entity @s data.status.state set value \"Not enough EU\"")
			function(ElectricFurnace.idle)
		}

		pack.define(ElectricFurnace.refreshDisplay) {
			// Rebuild the Electric Furnace's skin, lit while it is working.
			// Context: as the marker, at the block.
			//
			// Same arrangement as the EU Generator: a barrel wearing a furnace, because the
			// mechanics wanted are a plain 27-slot inventory and the appearance wanted is a
			// furnace. Through apply_lit, since a block state cannot ride in the block's name
			// -- see ra_lib:skin/apply_lit.
			//
			// A BLAST furnace, matching the item you place it from. It was a plain furnace,
			// which meant the thing in your hand and the thing in the world were different
			// blocks -- a small mismatch that makes a machine look like the wrong machine.
			//
			// The common case is a skin that already exists and only needs its `lit` state
			// changed, so try that first -- see ra_lib:skin/set_lit for why re-running
			// apply_lit for a state change made the block flicker and z-fight. apply_lit is
			// only reached when there is genuinely no display to edit: first placement, or
			// after a chunk reload cleared it.

			raw("data modify storage ra:wires ef.skin.id set value \"electric_furnace\"")
			raw("data modify storage ra:wires ef.skin.lit set value \"false\"")
			raw("execute if entity @s[tag=ra.wires.ef_lit] run data modify storage ra:wires ef.skin.lit set value \"true\"")

			line("execute store result score #ef.skinned ra.wires.tmp run function", RaLib.Skin.setLit, "with storage ra:wires ef.skin")
			raw("execute if score #ef.skinned ra.wires.tmp matches 1.. run return 0")

			line("execute if entity @s[tag=ra.wires.ef_lit] run function", RaLib.Skin.applyLit, """{real:"minecraft:barrel",skin:"minecraft:blast_furnace",id:"electric_furnace",lit:"true"}""")
			line("execute unless entity @s[tag=ra.wires.ef_lit] run function", RaLib.Skin.applyLit, """{real:"minecraft:barrel",skin:"minecraft:blast_furnace",id:"electric_furnace",lit:"false"}""")
		}

		pack.define(ElectricFurnace.running) {
			// One item just finished. Context: as the marker, at the block.
			//
			// The look is stay_lit's job and is shared with `cooking`; what belongs here is
			// only what marks the completion itself -- a puff and a crackle per item, not per
			// tick, so superpowered does not turn into a smoke column and a wall of sound.

			function(ElectricFurnace.stayLit)

			raw("particle minecraft:smoke ~ ~1.1 ~ 0.15 0.05 0.15 0.01 2 normal @a[scores={ra.u.par=1..}]")
			raw("particle minecraft:flame ~ ~0.6 ~ 0.2 0.1 0.2 0.005 1 normal @a[scores={ra.u.par=1..}]")
			raw("playsound minecraft:block.blastfurnace.fire_crackle block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.3 1.6")
		}

		pack.define(ElectricFurnace.stayLit) {
			// Put the furnace into its working look and keep it there.
			// Context: as the marker, at the block.
			//
			// The look, and nothing else -- no particles, no sound. Those belong to the tick
			// an item actually finishes, not to every tick of the cook, so `running` adds
			// them on top of this and `cooking` does not.
			//
			// Repainted only on the tick the state changes, so this is not a skin rebuild
			// every tick for every furnace in the world.

			raw("execute unless entity @s[tag=ra.wires.ef_lit] run tag @s add ra.wires.ef_lit")
			line("execute unless entity @s[tag=ra.wires.ef_was_lit] run function", ElectricFurnace.refreshDisplay)
			tag(self()) { add("ra.wires.ef_was_lit") }

			raw("data modify entity @s data.status.active set value 1b")

			// A lit furnace lights the room in vanilla; a skin is only a picture and emits
			// nothing, so the light is placed explicitly. Only into air, and idle removes only
			// the exact state placed here, so nothing a player built can be replaced.
			raw("execute if block ~ ~1 ~ #minecraft:air run setblock ~ ~1 ~ minecraft:light[level=10]")
		}

		pack.define(ElectricFurnace.tick) {
			// One Electric Furnace. Context: as its marker, at its block.
			//
			// INPUT IS THE WHOLE BARREL; OUTPUT IS A NEIGHBOUR YOU CHOOSE
			// The first version split the barrel by slot -- top row in, lower rows out --
			// which stops the furnace re-smelting its own results but makes automation
			// fiddly, because a hopper cannot be told which rows to touch.
			//
			// So the output left the block entirely. Any smeltable stack anywhere in the
			// barrel is input, and results are pushed into the container on ONE chosen face:
			// under, front, back or top, cycled with the wrench. Nothing comes back into the
			// furnace, so there is no way for it to smelt its own output and no slot rule for
			// anyone to remember. Feed it with a hopper from any side, take from the far
			// container with anything at all.
			//
			// It refuses to smelt when the destination cannot take the result, BEFORE paying
			// any EU, so a blocked output costs nothing and loses nothing.
			//
			// No fuel slot, because there is no fuel. The EU comes off the grid. No enable
			// switch either -- cut the wire if you want it stopped.
			//
			// THE COOLDOWN GATES THE SMELT, NOT THE LOOK
			// It used to gate both: a furnace inside its cooldown returned straight to `idle`,
			// which unlit it. So a furnace that was working looked switched off for all but
			// one tick in every cycle -- one in five on superpowered, one in a hundred on low
			// -- and each flip rebuilt the skin, which is what made it blink out and z-fight.
			//
			// Whether the block looks like it is cooking is a question about its CONDITIONS:
			// is there something to smelt, is there somewhere to put it, is there enough EU.
			// Those are now checked every tick regardless of the cooldown, and they alone
			// decide the skin. The cooldown decides only whether an item comes out this tick.
			// Costs a few more checks per furnace per tick than the old early return; a block
			// that spends its working life drawn as switched off is not worth the saving.

			raw("execute unless data entity @s data.status.state run data modify entity @s data.status.state set value \"Starting\"")

			raw("execute unless block ~ ~ ~ minecraft:barrel run return run data modify entity @s data.status.state set value \"Old block - break and replace\"")
			raw("execute unless data storage ra:wires smelt_map run return run data modify entity @s data.status.state set value \"Recipe table missing - /reload\"")

			function(ElectricFurnaceSettings.readMode)

			// Cooldown. A never-set score reads as ABSENT and every comparison against an
			// absent score is false, so it has to be seeded before it can be counted.
			raw("execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0")
			scoreboard.players.remove(self(), "ra.cooldown", 1)

			line("execute unless data block ~ ~ ~ Items[0] run return run function", ElectricFurnace.idleEmpty)

			function(ElectricFurnaceSmelting.findInput)
			line("execute if score #ef.slot ra.wires.tmp matches ..-1 run return run function", ElectricFurnace.idleEmpty)

			// Where the result is going, and whether it will fit. Checked before the EU is
			// spent and before the input is consumed.
			function(ElectricFurnaceSettings.readOutput)
			function(ElectricFurnaceSmelting.checkTarget, Storage.WIRES, "ef.out")
			line("execute if score #ef.can ra.wires.tmp matches ..0 run return run function", ElectricFurnace.idleBlocked)

			// Is the grid holding enough for one operation? Asked WITHOUT taking any, because
			// this runs on every tick of the cooldown too and a furnace that drained the grid
			// merely by checking would starve everything else on the wire.
			line("execute store result score #ef.bank ra.wires.tmp run function", EuApi.peekEu)
			line("execute if score #ef.bank ra.wires.tmp < #ef.cost ra.wires.tmp run return run function", ElectricFurnace.idleNopower)

			// Something to cook and the power to cook it, so it looks like it is cooking --
			// whether or not this is the tick an item actually comes out.
			line("execute if score @s ra.cooldown matches 1.. run return run function", ElectricFurnace.cooking)

			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ef.cost ra.wires.tmp")
			line("execute store result score #ef.got ra.wires.tmp run function", EuApi.takeEu, "with storage ra:wires eu")
			line("execute if score #ef.got ra.wires.tmp matches ..0 run return run function", ElectricFurnace.idleNopower)

			raw("scoreboard players operation @s ra.cooldown = #ef.speed ra.wires.tmp")
			function(ElectricFurnaceSmelting.smeltOne)
		}
	}
}
