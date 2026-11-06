package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.blastforge.BlastForgeData.facings
import ra.modules.multiblock.blastforge.BlastForgeData.path
import ra.modules.multiblock.blastforge.recipes.BlastForgeRecipes

/**
 * The Blast Forge's running logic: heat, fuel, and processing. Every per-facing function is a
 * macro fed with `bf_dir.<facing>`.
 */
object BlastForgeTick {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "blast_forge/$path")

	val tickFacing = fn("tick_facing")
	val process = fn("process")
	val processFacing = fn("process_facing")
	val consumeFuel = fn("consume_fuel")
	val consumeFirstItem = fn("consume_first_item")

	/** Fuel item -> heat added. Lava buckets are handled apart: they leave a bucket behind. */
	private val fuels = listOf(
		"coal" to 50,
		"coal_block" to 400,
		"charcoal" to 40,
		"blaze_powder" to 500,
		"blaze_rod" to 1500,
	)

	fun Ra.registerBlastForgeTick() {
		// As the marker, at the base. Skips a disabled forge, then dispatches by facing.
		pack.define(BlastForge.tick) {
			raw("execute unless data entity @s data.properties{enabled:1b} run return 0")
			dispatch(tickFacing)
		}
		// Manual entry point (debug / external calls); in normal operation tick_facing calls
		// process_facing directly.
		pack.define(process) { dispatch(processFacing) }
		pack.define(tickFacing) { tickFacing() }
		pack.define(processFacing) { processFacing() }
		pack.define(consumeFuel) { consumeFuel() }
		pack.define(consumeFirstItem) { consumeFirstItem() }
		with(BlastForgeRecipes) { registerRecipes() }
	}

	private fun Function.dispatch(target: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
		for (f in facings) execute {
			ifCondition { data(self(), """data{facing:"$f"}""") }
			run { function(target, Storage.MULTIBLOCK, path(f)) }
		}
	}

	/** Per-tick logic for one facing (already checked enabled). Macro: `bf_dir.<facing>`. */
	private fun Function.tickFacing() {
		val h = Obj.HEAT
		// Redstone control: a redstone block behind the base locks the forge, like a hopper.
		raw("\$execute if block $(ctrl) minecraft:redstone_block run return 0")

		// Heat decay: -1 every 2 ticks (passive cooling).
		raw("scoreboard players add @s ${Obj.MB_TIMER} 1")
		raw("execute if score @s ${Obj.MB_TIMER} matches 2.. run scoreboard players remove @s $h 1")
		raw("execute if score @s ${Obj.MB_TIMER} matches 2.. run scoreboard players set @s ${Obj.MB_TIMER} 0")

		// Heat never goes below 0.
		raw("execute if score @s $h matches ..-1 run scoreboard players set @s $h 0")

		// Visual feedback, scaling with heat.
		// Low (1-249): light smoke.
		raw("\$execute if score @s $h matches 1..249 run particle minecraft:smoke $(smoke) 1 1 1 0.003 1")
		// Medium (250-499): smoke + occasional flame.
		raw("\$execute if score @s $h matches 250..499 run particle minecraft:smoke $(smoke) 1 1 1 0.005 2")
		raw("\$execute if score @s $h matches 250..499 run particle minecraft:flame $(smoke) 1 1 1 0.005 1")
		// High (500-749): dense smoke + flames.
		raw("\$execute if score @s $h matches 500..749 run particle minecraft:smoke $(smoke) 2 2 2 0.01 2")
		raw("\$execute if score @s $h matches 500..749 run particle minecraft:flame $(smoke) 1 1 1 0.01 2")
		// Max (750-1000): flames + lava drips.
		raw("\$execute if score @s $h matches 750.. run particle minecraft:flame $(smoke) 2 2 2 0.015 2")
		raw("\$execute if score @s $h matches 750.. run particle minecraft:lava $(smoke) 1 1 1 0.005 1")

		// Fuel: only below 1000 heat, and only when there is material to process.
		raw("\$execute if score @s $h matches ..999 run function ${consumeFuel.asId()} with storage ra:multiblock bf_dir.$(facing)")

		// Processing needs at least 100 heat.
		raw("execute unless score @s $h matches 100.. run return 0")

		// Speed scales with heat:
		//   100-299: every 100 ticks (5 s)    300-599: every 60 ticks (3 s)
		//   600-899: every 40 ticks (2 s)     900+:    every 20 ticks (1 s)
		raw("scoreboard players add @s ${Obj.COOLDOWN} 1")
		raw("execute if score @s $h matches 100..299 unless score @s ${Obj.COOLDOWN} matches 100.. run return 0")
		raw("execute if score @s $h matches 300..599 unless score @s ${Obj.COOLDOWN} matches 60.. run return 0")
		raw("execute if score @s $h matches 600..899 unless score @s ${Obj.COOLDOWN} matches 40.. run return 0")
		raw("execute if score @s $h matches 900.. unless score @s ${Obj.COOLDOWN} matches 20.. run return 0")
		raw("scoreboard players set @s ${Obj.COOLDOWN} 0")

		raw("\$function ${processFacing.asId()} with storage ra:multiblock bf_dir.$(facing)")
	}

	/**
	 * Recipe matching + IO for one facing. Macro: `bf_dir.<facing>`. Input: in1 (material
	 * barrel); output: out1. Fuel is consumed separately by consume_fuel.
	 */
	private fun Function.processFacing() {
		raw("data remove storage ra:temp bf")

		raw("\$data modify storage ra:temp bf.material set from block $(in1) Items[0]")
		raw("execute unless data storage ra:temp bf.material run return 0")

		// Recipe matching (data-driven, see recipes/).
		raw("data remove storage ra:temp bf.result")
		function(BlastForgeRecipes.matchAll)
		raw("execute unless data storage ra:temp bf.result run return 0")

		raw("\$execute positioned $(in1) run function ${consumeFirstItem.asId()}")

		// insert_or_drop, not insert: a full output barrel used to delete the smelt.
		raw("data modify storage ra:inventory item set from storage ra:temp bf.result")
		raw("\$execute positioned $(out1) run function ${RaLib.Inventory.insertOrDrop.asId()}")

		raw("execute store result entity @s data.status.heat int 1 run scoreboard players get @s ${Obj.HEAT}")

		raw("\$particle minecraft:flame $(smoke) 0.2 0.2 0.2 0.02 10 normal @a[scores={ra.u.par=1..}]")
		raw("playsound minecraft:block.furnace.fire_crackle block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.5 1")
		raw("playsound minecraft:block.lava.extinguish block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.2 1.5")
	}

	/**
	 * Burns one fuel item from input_2 into heat. Macro: `bf_dir.<facing>`. As the marker, at
	 * the base. Only burns when input_1 has material to process.
	 */
	private fun Function.consumeFuel() {
		raw("\$execute unless data block $(in1) Items[0] run return 0")

		raw("\$data modify storage ra:temp bf.fuel_check set from block $(in2) Items[0]")
		raw("execute unless data storage ra:temp bf.fuel_check run return 0")

		for ((id, heat) in fuels) {
			val test = """execute if data storage ra:temp bf.fuel_check{id:"minecraft:$id"}"""
			raw("$test run scoreboard players add @s ${Obj.HEAT} $heat")
			raw("\$$test positioned $(in2) run function ${consumeFirstItem.asId()}")
			raw("$test run return 1")
		}

		// Lava bucket: +5000 heat, and the bucket stays behind.
		val lava = """execute if data storage ra:temp bf.fuel_check{id:"minecraft:lava_bucket"}"""
		raw("$lava run scoreboard players add @s ${Obj.HEAT} 5000")
		raw("\$$lava positioned $(in2) run data modify block ~ ~ ~ Items[0].id set value \"minecraft:bucket\"")
		raw("\$$lava positioned $(in2) run data modify block ~ ~ ~ Items[0].count set value 1")
		raw("$lava run return 1")

		// Cap heat at 1000.
		raw("execute if score @s ${Obj.HEAT} matches 1001.. run scoreboard players set @s ${Obj.HEAT} 1000")

		raw("data remove storage ra:temp bf.fuel_check")

		// No valid fuel found.
		raw("return 0")
	}

	/** Removes one item from the first occupied slot of the container here. Returns 1 if it did. */
	private fun Function.consumeFirstItem() {
		raw("execute unless data block ~ ~ ~ Items[0] run return 0")

		raw("execute store result score #bf_count ${Obj.TEMP} run data get block ~ ~ ~ Items[0].count")

		// Last item: remove the slot entirely.
		raw("execute if score #bf_count ${Obj.TEMP} matches 1 run data remove block ~ ~ ~ Items[0]")
		raw("execute if score #bf_count ${Obj.TEMP} matches 1 run return 1")

		raw("scoreboard players remove #bf_count ${Obj.TEMP} 1")
		raw("execute store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get #bf_count ${Obj.TEMP}")
	}
}
