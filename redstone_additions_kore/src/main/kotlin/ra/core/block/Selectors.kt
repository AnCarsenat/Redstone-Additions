package ra.core.block

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.SelectorArgument
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.scores.greaterThanOrEqualTo
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Obj
import ra.core.Tag

/** Selectors the pack writes over and over. Named once so the rule they encode is named too. */

/** Every marker of [block]: `@e[type=marker,tag=ra.custom_block.<id>]`. */
fun markers(block: CustomBlock): SelectorArgument = allEntities {
	type = EntityTypes.MARKER
	tag = block.tag
}

/** Markers of [block] flagged broken this tick. */
fun brokenMarkers(block: CustomBlock): SelectorArgument = allEntities {
	type = EntityTypes.MARKER
	tag = Tag.BROKEN
	tag = block.tag
}

/**
 * The marker [block] just summoned at this position, still tagged `ra.new`: the placement
 * handler's way of reaching the marker `ra_lib:placement/place` created a moment ago.
 */
fun newMarker(block: CustomBlock): SelectorArgument = allEntities(limitToOne = true) {
	type = EntityTypes.MARKER
	tag = block.tag
	tag = Tag.NEW
	distance = rangeOrDoubleEnd(0.5)
	sort = Sort.NEAREST
}

/** Players within [radius] who asked for block sounds (user setting `ra.u.snd`). */
fun soundListeners(radius: Double = 16.0): SelectorArgument = allPlayers {
	distance = rangeOrDoubleEnd(radius)
	scores { Obj.USER_SOUND greaterThanOrEqualTo 1 }
}
