package ra.modules.sensors

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.Vec3Frame
import ra.core.Vec3Spec
import ra.core.Vec3i
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
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.readVec3
import ra.core.vec3
import ra.lib.safe.SafeText.guardProperty
import ra.modules.sensors.SensorPoll.startIfIdle

/**
 * Block Detector: powers its shell while the block at `target` is `block`.
 *
 * TARGET. A vec3 property (ra.core.Vec3Props), an offset from the detector in whole blocks: 16
 * each way for players, 64 for admins. Placement aims it at the block in front (the face looking
 * at the player, as a dispenser); the Data Handler's editor nudges it, types it, or [Pick]s the
 * block the player is looking at. It is read with [readVec3]: three integers, clamped to the
 * admin range, so the position pasted into `execute positioned ~$(x) ~$(y) ~$(z)` is never text.
 *
 * BLOCK. An item-type property (the Data Handler's [Set from hand] copies a held block's id): a
 * block id or a block tag (`minecraft:oak_door`, `#minecraft:logs`). Empty: any block but air.
 * Pasted into `if block`, so, as the Held Item Detector's item, it is checked by
 * `ra_lib:safe/guard` (format ID: one bare word) and by a probe that instantiates the same test
 * against the detector's own position: an unknown id fails there, and the value probed last is
 * kept (`data.data.valid_block` / `invalid_block`) so an unchanged value is probed once.
 *
 * OUTPUT as the Entity Detector's: iron blocks in the 3x3x3 around it become redstone blocks. A
 * target in an unloaded chunk reads as no match. Read once a second by [SensorPoll].
 */
object BlockDetector {
	val block = CustomBlock(Ns.SENSORS, "block_detector", "Block Detector", model = "minecraft:waxed_weathered_chiseled_copper", dirType = 3)
	val item = block.loreItem { lore(loreLine("Powers when a set block is at a set position", "gray")) }

	val TARGET = Vec3Spec(
		min = Vec3i(-16), max = Vec3i(16), adminMin = Vec3i(-64), adminMax = Vec3i(64),
		unit = 1, frame = Vec3Frame.RELATIVE, steps = listOf(1, 4, 16), default = Vec3i(0, 1, 0),
	)

	/** Where placement aims it: the block in front, for each facing. */
	private val FRONT = mapOf(
		"north" to Vec3i(0, 0, -1), "south" to Vec3i(0, 0, 1), "west" to Vec3i(-1, 0, 0),
		"east" to Vec3i(1, 0, 0), "up" to Vec3i(0, 1, 0), "down" to Vec3i(0, -1, 0),
	)

	/** As the marker, at it: look at the target, switch the shell. */
	val update = block.fn("update")
	private val find = block.fn("find")
	private val probe = block.fn("probe")
	private val detect = block.fn("detect")
	private val detectAny = block.fn("detect_any")

	private const val T = Obj.TEMP
	private const val ON = "#sn.blk"
	private const val S = "storage ra:temp block_det"

	fun Ra.registerBlockDetector() {
		block(block)
		properties(block) {
			item("block", default = "")
			vec3("target", TARGET)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.OBSERVER), count = null) {
			pattern("CEC", "ROR", "CCC")
			key("C", Items.COPPER_INGOT)
			key("E", Items.ENDER_EYE)
			key("R", Items.REDSTONE)
			key("O", Items.OBSERVER)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			raw("""data modify entity ${block.newMarkerSel()} data.properties set value {block:"",target:${TARGET.default.snbt()}}""")
			for ((f, v) in FRONT) raw("""execute if data storage ra:temp {facing_name:"$f"} run data modify entity ${block.newMarkerSel()} data.properties.target set value ${v.snbt()}""")
			raw("data modify entity ${block.newMarkerSel()} data.status.facing set from storage ra:temp facing_name")
			// First read now rather than up to a second later.
			executeRun("as ${block.newMarkerSel()} at @s", update)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(update) {
			raw("execute store result score $ON $T run function ${find.asId()}")
			raw("data remove $S")
			raw("execute if score $ON $T matches 1 run data modify entity @s data.status.output set value \"on\"")
			raw("execute unless score $ON $T matches 1 run data modify entity @s data.status.output set value \"off\"")
			// One fill or the other, never off-then-on: that would pulse the wires every read.
			raw("execute if score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
			raw("execute unless score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		}
		pack.define(find) { find() }

		// Macro {block}: instantiating it is the test; an unknown block id or tag fails here.
		pack.define(probe) {
			raw("\$execute if block ~ ~ ~ \$(block)")
			raw("return 1")
		}
		// Macro {x, y, z, block}: three integers from readVec3, and a block that passed guard and probe.
		pack.define(detect) {
			raw("\$execute positioned ~\$(x) ~\$(y) ~\$(z) if block ~ ~ ~ \$(block) run return 1")
			raw("return 0")
		}
		// Macro {x, y, z}: anything but air.
		pack.define(detectAny) {
			raw("\$execute positioned ~\$(x) ~\$(y) ~\$(z) if loaded ~ ~ ~ unless block ~ ~ ~ #minecraft:air run return 1")
			raw("return 0")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("block", "Block: ", "aqua", y = 0.8)
			propLine("target", "Target: ", "aqua", y = 0.6)
			dataLine("output", "Output: ", "yellow", y = 0.4)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) { startIfIdle(block) }
	}

	/** As the marker: returns 1 when the target holds the block. */
	private fun Function.find() {
		raw("data remove $S")
		readVec3("target", TARGET, "$S.pos")
		raw("execute unless data entity @s data.properties.block run return run function ${detectAny.asId()} with $S.pos")
		raw("execute if data entity @s data.properties{block:\"\"} run return run function ${detectAny.asId()} with $S.pos")
		guardProperty("block", TextFormat.ID)
		raw("data modify $S.block set from entity @s data.properties.block")

		// Probed already: known bad, stop; known good, skip the probe.
		raw("data modify $S.was set from entity @s data.data.invalid_block")
		raw("execute store success score #sn.chg $T run data modify $S.was set from $S.block")
		raw("execute if score #sn.chg $T matches 0 run return 0")
		raw("data remove $S.was")
		raw("data modify $S.was set from entity @s data.data.valid_block")
		raw("execute store success score #sn.chg $T run data modify $S.was set from $S.block")
		raw("scoreboard players set #sn.ok $T 1")
		// 0 first: a macro that fails to instantiate leaves it so, whatever `store` does on a failure.
		raw("execute if score #sn.chg $T matches 1 run scoreboard players set #sn.ok $T 0")
		raw("execute if score #sn.chg $T matches 1 store result score #sn.ok $T run function ${probe.asId()} with $S")
		raw("execute if score #sn.chg $T matches 1 if score #sn.ok $T matches 1 run data modify entity @s data.data.valid_block set from $S.block")
		raw("execute if score #sn.chg $T matches 1 if score #sn.ok $T matches 1 run data remove entity @s data.data.invalid_block")
		raw("execute if score #sn.ok $T matches 0 run data modify entity @s data.data.invalid_block set from $S.block")
		raw("execute if score #sn.ok $T matches 0 run data remove entity @s data.data.valid_block")
		raw("execute if score #sn.ok $T matches 0 run return 0")
		raw("data remove $S.was")

		raw("data modify $S.pos.block set from $S.block")
		raw("return run function ${detect.asId()} with $S.pos")
	}
}
