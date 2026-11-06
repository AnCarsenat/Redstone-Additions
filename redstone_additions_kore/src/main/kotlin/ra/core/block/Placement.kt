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
 * ...block-specific setup on newMarker(block)...
 * finishPlacement(block)      // drop ra.new and the bat's place tag, return 1
 * ```
 */
fun Function.placementGuard(block: CustomBlock) {
	raw("execute unless entity @s[tag=${block.placeTag}] run return 0")
	raw("execute if entity @e[type=marker,tag=${block.tag},distance=..0.5,limit=1] run return 0")
}

/** `function ra_lib:placement/place {block_id, block_tag, dir_type}` for [block]. */
fun Function.placeBlock(block: CustomBlock) {
	function(RaLib.Placement.place, nbt {
		this["block_id"] = block.physical
		this["block_tag"] = block.id
		this["dir_type"] = block.dirType
	})
}

/** Removes `ra.new` from the fresh marker and the place tag from the bat, then returns 1. */
fun Function.finishPlacement(block: CustomBlock) {
	tag(allEntities {
		type = EntityTypes.MARKER
		tag = block.tag
		tag = Tag.NEW
		distance = rangeOrDoubleEnd(0.5)
	}) { remove(Tag.NEW) }
	tag(self()) { remove(block.placeTag) }
	returnValue(1)
}
