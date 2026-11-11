package ra.modules.sensors

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockGive
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.sensors.SensorPoll.startIfIdle
import ra.core.block.WRENCH_READOUT

/**
 * Weather Sensor: powers its shell while the world matches its `mode`, one of [MODES]:
 * `rain`, `thunder`, `day`, `night`. Output as the Entity Detector's: iron blocks in the 3x3x3
 * around it become redstone blocks.
 *
 * Read once a second by [SensorPoll], with loot predicates: `weather_check` for rain and thunder
 * (the dimension's weather, whatever the biome), `time_check` on the overworld clock for day and
 * night (26.1 made `clock` required). Night is 13000-22999 of the day, when the sky darkens and
 * monsters spawn; day is the rest, sunrise included. A mode that is none of these (typed in the
 * Data Handler) keeps the output off.
 */
object WeatherSensor {
	val block = CustomBlock(Ns.SENSORS, "weather_sensor", "Weather Sensor", model = "minecraft:waxed_weathered_copper")
	val item = block.loreItem { lore(loreLine("Powers on rain, thunder, day or night", "gray")) }

	/** Cycle order, the first the default. */
	val MODES = listOf("rain", "thunder", "day", "night")

	/** As the marker, at it: test the mode, switch the shell. */
	val update = block.fn("update")
	val cycleMode = block.fn("cycle_mode")

	private const val T = Obj.TEMP
	private const val ON = "#sn.weather"

	private const val NIGHT = """{type:"minecraft:time_check",clock:"minecraft:overworld",value:{min:13000,max:22999},period:24000}"""
	private val tests = mapOf(
		"rain" to """if predicate {type:"minecraft:weather_check",raining:true}""",
		"thunder" to """if predicate {type:"minecraft:weather_check",thundering:true}""",
		"day" to "unless predicate $NIGHT",
		"night" to "if predicate $NIGHT",
	)

	fun Ra.registerWeatherSensor() {
		block(block)
		wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleMode))
		properties(block) {
			text("mode", default = MODES.first())
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.LIGHTNING_ROD), count = null) {
			pattern("GGG", "RLR", "III")
			key("G", Items.GLASS)
			key("R", Items.REDSTONE)
			key("L", Items.LIGHTNING_ROD)
			key("I", Items.COPPER_INGOT)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", """{mode:"${MODES.first()}"}""")
			// First read now rather than up to a second later.
			executeRun("as ${block.newMarkerSel()} at @s", update)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(update) { update() }
		pack.define(cycleMode) { cycleMode() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("mode", "Mode: ", "aqua", y = 0.8)
			dataLine("output", "Output: ", "yellow", y = 0.6)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) { startIfIdle(block) }
	}

	private fun Function.update() {
		raw("scoreboard players set $ON $T 0")
		for ((mode, test) in tests) {
			raw("execute if data entity @s data.properties{mode:\"$mode\"} $test run scoreboard players set $ON $T 1")
		}
		raw("execute if score $ON $T matches 1 run data modify entity @s data.status.output set value \"on\"")
		raw("execute if score $ON $T matches 0 run data modify entity @s data.status.output set value \"off\"")
		// One fill or the other, never off-then-on: that would pulse the wires every read.
		raw("execute if score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if score $ON $T matches 0 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
	}

	/** Wrench: the next of [MODES], from anything else to the first. As the marker, at the block. */
	private fun Function.cycleMode() {
		raw("scoreboard players set $ON $T 0")
		MODES.forEachIndexed { i, mode ->
			raw("execute if data entity @s data.properties{mode:\"$mode\"} run scoreboard players set $ON $T ${i + 1}")
		}
		// Score k (mode k-1) -> mode k, the last wrapping to the first; 0 (unknown, missing) -> the first.
		for (k in 0..MODES.size) {
			raw("execute if score $ON $T matches $k run data modify entity @s data.properties.mode set value \"${MODES[k % MODES.size]}\"")
		}
		raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.properties.mode",entity:"@s",color:"aqua"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		// The output follows the new mode now, not at the next read.
		function(update)
	}
}
