package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.api.MultiblockSpec
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.blastforge.recipes.BlastForgeRecipes

/**
 * The Blast Forge's running logic: heat, fuel, and processing.
 *
 * The positions it touches (furnace, barrels, redstone control, smoke) depend only on the facing,
 * and there are four facings, so each per-facing function is generated four times with its
 * coordinates written in, rotated from [BlastForge]'s north-facing offsets:
 * `tick_facing/<facing>`, `process_facing/<facing>` and `consume_fuel/<facing>`. `tick` picks the
 * marker's facing once; nothing is macro-substituted while the forge runs.
 */
object BlastForgeTick {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "blast_forge/$path")

	val process = fn("process")
	val consumeFirstItem = fn("consume_first_item")

	/** The tick's positions for one facing, as `execute` coordinates relative to the base. */
	private class At(val facing: String) {
		private fun coords(p: MultiblockSpec.Pos, y: String? = null): String {
			val r = p.rotated(facing)
			fun axis(v: Int) = if (v == 0) "~" else "~$v"
			return "${axis(r.x)} ${y ?: axis(r.y)} ${axis(r.z)}"
		}

		val in1 = coords(BlastForge.INPUT_1)
		val in2 = coords(BlastForge.INPUT_2)
		val out1 = coords(BlastForge.OUTPUT_1)
		val ctrl = coords(BlastForge.REDSTONE_IN)
		/** The furnace, half a block up: where the particles come from. */
		val smoke = coords(BlastForge.FURNACE, "~0.5")

		val tickFacing = fn("tick_facing/$facing")
		val processFacing = fn("process_facing/$facing")
		val consumeFuel = fn("consume_fuel/$facing")
	}

	private val facings = MultiblockSpec.FACINGS.map(::At)

	/** Fuel item -> heat added. Lava buckets are handled apart: they leave a bucket behind. */
	private val fuels = listOf(
		"coal" to 50,
		"coal_block" to 400,
		"charcoal" to 40,
		"blaze_powder" to 500,
		"blaze_rod" to 1500,
	)

	fun Ra.registerBlastForgeTick() {
		// As the marker, at the base. Dispatches by facing; the redstone control is checked there.
		pack.define(BlastForge.tick) { dispatch { it.tickFacing } }
		// kept: beet contract. Manual entry point (debug / external calls); in normal operation
		// tick_facing calls process_facing directly.
		pack.define(process) { dispatch { it.processFacing } }
		for (at in facings) {
			pack.define(at.tickFacing) { tickFacing(at) }
			pack.define(at.processFacing) { processFacing(at) }
			pack.define(at.consumeFuel) { consumeFuel(at) }
		}
		pack.define(consumeFirstItem) { consumeFirstItem() }
		with(BlastForgeRecipes) { registerRecipes() }
	}

	/** As the marker: the [target] of the marker's `data.facing`. A marker with none does nothing. */
	private fun Function.dispatch(target: (At) -> FunctionArgument) {
		for (at in facings)
			raw("execute if data entity @s data{facing:\"${at.facing}\"} run return run function ${target(at).asId()}")
	}

	/** Per-tick logic for one facing. */
	private fun Function.tickFacing(at: At) {
		val h = Obj.HEAT
		// Redstone control: a redstone block behind the base locks the forge, like a hopper.
		raw("execute if block ${at.ctrl} minecraft:redstone_block run return 0")

		// Heat decay: -1 every 2 ticks (passive cooling).
		scoreboard.players.add(self(), Obj.MB_TIMER, 1)
		raw("execute if score @s ${Obj.MB_TIMER} matches 2.. run scoreboard players remove @s $h 1")
		raw("execute if score @s ${Obj.MB_TIMER} matches 2.. run scoreboard players set @s ${Obj.MB_TIMER} 0")

		// Heat never goes below 0.
		raw("execute if score @s $h matches ..-1 run scoreboard players set @s $h 0")

		// Visual feedback, scaling with heat.
		val smoke = at.smoke
		// Low (1-249): light smoke.
		raw("execute if score @s $h matches 1..249 run particle minecraft:smoke $smoke 1 1 1 0.003 1 normal ${particleViewersSel()}")
		// Medium (250-499): smoke + occasional flame.
		raw("execute if score @s $h matches 250..499 run particle minecraft:smoke $smoke 1 1 1 0.005 2 normal ${particleViewersSel()}")
		raw("execute if score @s $h matches 250..499 run particle minecraft:flame $smoke 1 1 1 0.005 1 normal ${particleViewersSel()}")
		// High (500-749): dense smoke + flames.
		raw("execute if score @s $h matches 500..749 run particle minecraft:smoke $smoke 2 2 2 0.01 2 normal ${particleViewersSel()}")
		raw("execute if score @s $h matches 500..749 run particle minecraft:flame $smoke 1 1 1 0.01 2 normal ${particleViewersSel()}")
		// Max (750-1000): flames + lava drips.
		raw("execute if score @s $h matches 750.. run particle minecraft:flame $smoke 2 2 2 0.015 2 normal ${particleViewersSel()}")
		raw("execute if score @s $h matches 750.. run particle minecraft:lava $smoke 1 1 1 0.005 1 normal ${particleViewersSel()}")

		// Fuel: only below 1000 heat, and only when there is material to process.
		raw("execute if score @s $h matches ..999 run function ${at.consumeFuel.asId()}")

		// Processing needs at least 100 heat.
		raw("execute unless score @s $h matches 100.. run return 0")

		// Speed scales with heat:
		//   100-299: every 100 ticks (5 s)    300-599: every 60 ticks (3 s)
		//   600-899: every 40 ticks (2 s)     900+:    every 20 ticks (1 s)
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s $h matches 100..299 unless score @s ${Obj.COOLDOWN} matches 100.. run return 0")
		raw("execute if score @s $h matches 300..599 unless score @s ${Obj.COOLDOWN} matches 60.. run return 0")
		raw("execute if score @s $h matches 600..899 unless score @s ${Obj.COOLDOWN} matches 40.. run return 0")
		raw("execute if score @s $h matches 900.. unless score @s ${Obj.COOLDOWN} matches 20.. run return 0")
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		function(at.processFacing)
	}

	/**
	 * Recipe matching + IO for one facing. Input: in1 (material barrel); output: out1. Fuel is
	 * consumed separately by consume_fuel.
	 */
	private fun Function.processFacing(at: At) {
		data(Storage.TEMP) { remove("bf") }

		raw("data modify storage ra:temp bf.material set from block ${at.in1} Items[0]")
		raw("execute unless data storage ra:temp bf.material run return 0")

		// Recipe matching (data-driven, see recipes/).
		data(Storage.TEMP) { remove("bf.result") }
		function(BlastForgeRecipes.matchAll)
		raw("execute unless data storage ra:temp bf.result run return 0")

		raw("execute positioned ${at.in1} run function ${consumeFirstItem.asId()}")

		// insert_or_drop, not insert: a full output barrel used to delete the smelt.
		data(Storage.INVENTORY) { modify("item") { set(Storage.TEMP, "bf.result") } }
		raw("execute positioned ${at.out1} run function ${RaLib.Inventory.insertOrDrop.asId()}")

		raw("execute store result entity @s data.status.heat int 1 run scoreboard players get @s ${Obj.HEAT}")

		raw("particle minecraft:flame ${at.smoke} 0.2 0.2 0.2 0.02 10 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.furnace.fire_crackle block ${soundListenersSel(8)} ~ ~ ~ 0.5 1")
		raw("playsound minecraft:block.lava.extinguish block ${soundListenersSel(8)} ~ ~ ~ 0.2 1.5")
	}

	/**
	 * Burns one fuel item from input_2 into heat. As the marker, at the base. Only burns when
	 * input_1 has material to process.
	 */
	private fun Function.consumeFuel(at: At) {
		raw("execute unless data block ${at.in1} Items[0] run return 0")

		raw("data modify storage ra:temp bf.fuel_check set from block ${at.in2} Items[0]")
		raw("execute unless data storage ra:temp bf.fuel_check run return 0")

		for ((id, heat) in fuels) {
			val test = """execute if data storage ra:temp bf.fuel_check{id:"minecraft:$id"}"""
			raw("$test run scoreboard players add @s ${Obj.HEAT} $heat")
			raw("$test positioned ${at.in2} run function ${consumeFirstItem.asId()}")
			raw("$test run return 1")
		}

		// Lava bucket: +5000 heat, and the bucket stays behind.
		val lava = """execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"}"""
		raw("$lava run scoreboard players add @s ${Obj.HEAT} 5000")
		raw("$lava positioned ${at.in2} run data modify block ~ ~ ~ Items[0].id set value \"minecraft:bucket\"")
		raw("$lava positioned ${at.in2} run data modify block ~ ~ ~ Items[0].count set value 1")
		raw("$lava run return 1")

		// Cap heat at 1000.
		raw("execute if score @s ${Obj.HEAT} matches 1001.. run scoreboard players set @s ${Obj.HEAT} 1000")

		data(Storage.TEMP) { remove("bf.fuel_check") }

		// No valid fuel found.
		returnValue(0)
	}

	/** Removes one item from the first occupied slot of the container here. Returns 1 if it did. */
	private fun Function.consumeFirstItem() {
		raw("execute unless data block ~ ~ ~ Items[0] run return 0")

		raw("execute store result score #bf_count ${Obj.TEMP} run data get block ~ ~ ~ Items[0].count")

		// Last item: remove the slot entirely.
		raw("execute if score #bf_count ${Obj.TEMP} matches 1 run data remove block ~ ~ ~ Items[0]")
		raw("execute if score #bf_count ${Obj.TEMP} matches 1 run return 1")

		scoreboard.players.remove(literal("#bf_count"), Obj.TEMP, 1)
		raw("execute store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get #bf_count ${Obj.TEMP}")
	}
}
