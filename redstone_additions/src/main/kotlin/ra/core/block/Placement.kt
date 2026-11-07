package ra.core.block

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Tag
import ra.core.raw

/**
 * The three fixed parts of every `handle_placement` function. A handler runs as the spawned bat
 * at the block centre, once per registered block type, and must return 1 only for its own bat:
 *
 * ```
 * placementGuard(block)       // not our bat, or a block is already here -> return 0
 * placeBlock(block)           // ra_lib:placement/place: vanilla block + marker tagged ra.new
 * ...block-specific setup on newMarker(block) / setOnNew(...)...
 * finishPlacement(block)      // drop ra.new and the bat's place tag, return 1
 * ```
 *
 * Several modules kept the beet shape of these steps, which differs at runtime (no "already a
 * marker here" guard, `ra.new` cleared on every marker of the type, the bat's tag left alone); the
 * options reproduce each of them exactly.
 */

/**
 * Not our bat -> `return 0`; and when [checkExisting], a marker of [block] already within 0.5 ->
 * `return 0`. The sensors, wireless, ender and interactive handlers never had the second guard.
 */
fun Function.placementGuard(block: CustomBlock, checkExisting: Boolean = true) {
	raw("execute unless entity @s[tag=${block.placeTag}] run return 0")
	if (checkExisting) raw("execute if entity @e[type=marker,tag=${block.tag},distance=..0.5,limit=1] run return 0")
}

/** `function ra_lib:placement/place {block_id, block_tag, dir_type}` for [block]. */
fun Function.placeBlock(block: CustomBlock) {
	function(RaLib.Placement.place, nbt {
		this["block_id"] = block.physical
		this["block_tag"] = block.id
		this["dir_type"] = block.dirType
	})
}

/**
 * Removes `ra.new` from the fresh marker: every marker of the type within 0.5 ([withinBlock]),
 * or every marker of the type anywhere (there is only ever the one just placed).
 */
fun Function.clearNewTag(block: CustomBlock, withinBlock: Boolean = true) {
	tag(allEntities {
		type = EntityTypes.MARKER
		tag = block.tag
		tag = Tag.NEW
		if (withinBlock) distance = rangeOrDoubleEnd(0.5)
	}) { remove(Tag.NEW) }
}

/**
 * [clearNewTag], then removes the place tag from the bat ([removePlaceTag]), then `return 1`.
 * The infinite, chunk loader and interactive handlers wrote it as
 * `finishPlacement(block, removePlaceTag = false, withinBlock = false)`.
 */
fun Function.finishPlacement(block: CustomBlock, removePlaceTag: Boolean = true, withinBlock: Boolean = true) {
	clearNewTag(block, withinBlock)
	if (removePlaceTag) tag(self()) { remove(block.placeTag) }
	returnValue(1)
}

/** `data modify entity <fresh marker> <path> set value <snbt>`, the marker as [newMarkerSel] spells it. */
fun Function.setOnNew(block: CustomBlock, path: String, snbt: String) =
	raw("data modify entity ${block.newMarkerSel()} $path set value $snbt")

/**
 * Every marker of [block] missing `data.properties.<key>` gets [snbt]. Run from the tick, so
 * blocks placed by an older version get a property added since.
 */
fun Function.defaultProperty(block: CustomBlock, key: String, snbt: String) =
	raw("execute as ${block.markerSel()} unless data entity @s data.properties.$key run data modify entity @s data.properties.$key set value $snbt")
