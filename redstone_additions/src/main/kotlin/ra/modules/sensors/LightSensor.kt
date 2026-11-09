package ra.modules.sensors

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
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
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.sensors.SensorPoll.startIfIdle

/**
 * Light Sensor: outputs, as an analog signal 0-15, the light level of the block in front of it.
 *
 * OUTPUT. The pack's other sensors switch an iron-block shell to redstone blocks, which is on or
 * off. A level needs a block that holds one: the sensor IS a target block, whose `power` state
 * (0-15) is its output to every side, and it is set with `fill ... target[power=N] replace target`.
 * A target block placed with power by a command resets to 0 (TargetBlock#onPlace), but only when
 * it replaces a different block; replacing a target with a target keeps the state, and a `fill`
 * to the state it already has changes nothing and updates nothing. An arrow hitting it pulses it
 * as in vanilla, and the next read puts the light level back.
 *
 * READING. 26.3 has no `execute if light`; the `location_check` predicate's `light` is the
 * combined level (`max(sky - darkening, block)`), the only one a data pack can test, so there is
 * no block/sky mode. The level is found with a binary search: four predicate tests per read.
 *
 * FRONT. The face looking at the player when placed, as a dispenser (dir_type 3: the marker
 * carries the facing, a target block has none). Read once a second by [SensorPoll].
 */
object LightSensor {
	val block = CustomBlock(Ns.SENSORS, "light_sensor", "Light Sensor", model = "minecraft:target", dirType = 3)
	val item = block.loreItem { lore(loreLine("Outputs the light level in front of it (0-15)", "gray")) }

	/** As the marker, at it: read the level, store it, set the output. */
	val update = block.fn("update")
	private val output = block.fn("output")

	private const val T = Obj.TEMP
	private const val L = "#sn.light"

	fun Ra.registerLightSensor() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DAYLIGHT_DETECTOR), count = null) {
			pattern("GGG", "RDR", "CCC")
			key("G", Items.GLASS)
			key("R", Items.REDSTONE)
			key("D", Items.DAYLIGHT_DETECTOR)
			key("C", Items.COBBLESTONE)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			// ra_lib:placement/place leaves the facing it resolved in ra:temp facing_name.
			raw("data modify entity ${block.newMarkerSel()} data.status.facing set from storage ra:temp facing_name")
			// First read now rather than up to a second later.
			executeRun("as ${block.newMarkerSel()} at @s", update)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(update) { update() }
		// Macro {level}: only ever the integer stored from the score just above.
		pack.define(output) { raw("\$fill ~ ~ ~ ~ ~ ~ minecraft:target[power=\$(level)] replace minecraft:target") }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("level", "Light: ", "yellow", y = 0.8)
			dataLine("facing", "Facing: ", "aqua", y = 0.6)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) { startIfIdle(block) }
	}

	/**
	 * Binary search over the predicate's `light.min`: bit 8, then 4, 2, 1. A line only runs for
	 * the one value the bits above left, and adding its bit makes a value no other line of the same
	 * step tests, so each step runs at most one predicate.
	 */
	private fun Function.update() {
		raw("scoreboard players set $L $T 0")
		for (bit in listOf(8, 4, 2, 1)) for (base in 0 until 16 step bit * 2) {
			val test = if (bit == 8) "" else "if score $L $T matches $base "
			raw("execute ${test}positioned ^ ^ ^1 if predicate {type:\"minecraft:location_check\",predicate:{light:{light:{min:${base + bit}}}}} run scoreboard players add $L $T $bit")
		}
		raw("execute store result entity @s data.status.level int 1 run scoreboard players get $L $T")
		raw("function ${output.asId()} with entity @s data.status")
	}
}
