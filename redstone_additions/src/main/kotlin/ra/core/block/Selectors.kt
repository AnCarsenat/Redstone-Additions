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

// The same selectors as vanilla text, for commands that stay text (`raw`, execute chains). The
// typed ones above write e.g. `distance=..16.0`; these keep the beet spelling.

/**
 * `@e[type=marker,tag=<block tag>(,<extra>)*]` as text. [extra] are further selector arguments,
 * written as given (`"tag=!ra.new"`, `"limit=1"`).
 */
fun CustomBlock.markerSel(vararg extra: String) =
	"@e[type=marker,tag=$tag${extra.joinToString("") { ",$it" }}]"

/** The fresh marker as text, as the beet handlers spelled it ([newMarker] is the typed form). */
fun CustomBlock.newMarkerSel() = "@e[type=marker,tag=$tag,tag=${Tag.NEW},distance=..0.5,sort=nearest,limit=1]"

/**
 * Players who asked for block sounds, as text: `@a[distance=..<radius>,scores={ra.u.snd=1..}]`,
 * or `@a[scores={ra.u.snd=1..}]` when [radius] is null.
 */
fun soundListenersSel(radius: Int? = 16) =
	"@a[${if (radius == null) "" else "distance=..$radius,"}scores={${Obj.USER_SOUND}=1..}]"

/**
 * Players who asked for block particles, as text: `@a[scores={ra.u.par=1..}]`, or with
 * `distance=..<radius>,` first when [radius] is given.
 */
fun particleViewersSel(radius: Int? = null) =
	"@a[${if (radius == null) "" else "distance=..$radius,"}scores={${Obj.USER_PARTICLES}=1..}]"

/**
 * Players who asked for chat notices (user setting `ra.u.msg`, off by default), as text:
 * `@a[scores={ra.u.msg=1..}]`, or with `distance=..<radius>,` first when [radius] is given.
 * Automatic lines (a sacrifice's result, a machine's progress, a success confirmation) go
 * through this or [noticeOnly]; menus, readouts a player asked for and refusals do not.
 */
fun noticeReceiversSel(radius: Int? = null) =
	"@a[${if (radius == null) "" else "distance=..$radius,"}scores={${Obj.USER_NOTICES}=1..}]"

/**
 * [sel] narrowed to players with chat notices on: `@s` becomes `@s[scores={ra.u.msg=1..}]`,
 * `@a[tag=x,limit=1]` becomes `@a[tag=x,limit=1,scores={ra.u.msg=1..}]`. [sel] must not have a
 * `scores=` argument of its own.
 */
fun noticeOnly(sel: String): String {
	require("scores=" !in sel) { "noticeOnly: $sel already filters on scores" }
	val filter = "scores={${Obj.USER_NOTICES}=1..}"
	return if (sel.endsWith("]")) sel.dropLast(1) + ",$filter]" else "$sel[$filter]"
}

/**
 * Who hears a wrench readout (`[Wrench] Conveyor speed: 3`): the player who swung the wrench,
 * and the players within 10 blocks of them who turned chat notices on. The wrench tags them
 * `ra.wrench_hear` around the cycle (ra.tools.wrench.Wrench, WrenchMenu) and clears it after.
 */
const val WRENCH_HEAR_TAG = "ra.wrench_hear"
const val WRENCH_READOUT = "@a[tag=$WRENCH_HEAR_TAG]"
