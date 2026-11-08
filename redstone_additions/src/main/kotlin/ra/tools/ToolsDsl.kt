package ra.tools

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.define
import ra.core.raw
import ra.core.Tag
import ra.core.executeRun
import ra.core.ref

/**
 * Small writers the tools repeat in nearly every file.
 *
 * The tools are mostly `execute <long chain> run function <one of ours>` lines; those go through
 * the shared `executeRun` / `executeReturnRun` / `callWith` of `ra.core` (`Exec.kt`), which keep the
 * chain as vanilla text but take the called function as a checked value.
 */

/** `ra:tools/<path>`. */
fun tool(path: String): FunctionArgument = ref(Ns.RA, "tools/$path")

/**
 * The custom-block marker nearest a known block position (within 1.5). Only for a position that
 * IS a block's centre (a stored x y z); to find what a player is looking at, use [raycast].
 */
const val RAY_TARGET = "@e[type=marker,tag=${Tag.CUSTOM_BLOCK},distance=..1.5,limit=1,sort=nearest]"

/** How far the tools reach, in blocks: creative block reach. */
const val REACH = 5.0

/** The ray's step, in blocks. Small enough not to skip the corner of a block it crosses. */
private const val RAY_STEP = 0.1

/**
 * Run [fn] as the custom block the player is looking at, at its centre: walk the player's look
 * from the eyes in [RAY_STEP] steps, up to [reach]; at each point, a custom-block marker at the
 * centre of the block the point is in is the answer, and a block that is not replaceable (air,
 * water, grass...) with no marker ends the walk with no answer. [guard]: execute subcommands
 * (ending with a space) the whole cast is conditional on. Context: as the player.
 *
 * WHY NOT FOUR POINTS. This used to probe four points one block apart and take the nearest
 * marker within 1.5 of each. 1.5 is wider than a block, so the first probe -- one block in front
 * of the eyes -- picked up a block beside or below the player before the ray ever reached the one
 * under the crosshair: the Data Handler opened, and edited, the wrong block. It also looked
 * through solid blocks. Now it only answers with the block the look actually enters first.
 */
fun Function.raycast(foundFlag: String, fn: FunctionArgument, guard: String = "", reach: Double = REACH) {
	val step = ref(Ns.RA, "tools/ray/" + fn.asId().replace(':', '/'))
	if (datapack.functions.none { it.asId() == step.asId() }) datapack.define(step) {
		val here = "align xyz positioned ~0.5 ~0.5 ~0.5"
		val marker = "@e[type=minecraft:marker,tag=${Tag.CUSTOM_BLOCK},distance=..0.6,limit=1,sort=nearest]"
		raw("execute store success score #ray_hit ${Obj.TEMP} $here if entity $marker")
		raw("execute if score #ray_hit ${Obj.TEMP} matches 1 $here as $marker run function ${fn.asId()}")
		raw("execute if score #ray_hit ${Obj.TEMP} matches 1 run return 1")
		raw("execute unless block ~ ~ ~ #minecraft:replaceable run return 0")
		raw("scoreboard players remove #ray ${Obj.TEMP} 1")
		raw("execute if score #ray ${Obj.TEMP} matches 1.. positioned ^ ^ ^$RAY_STEP run function ${step.asId()}")
	}
	raw("scoreboard players set #ray ${Obj.TEMP} ${kotlin.math.ceil(reach / RAY_STEP).toInt()}")
	// `anchored feet` once at the eyes: the anchor is inherited, and with `eyes` every `^` step in
	// the walk would add the eye height again.
	raw("execute ${guard}at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${step.asId()}")
}

/** Four one-block steps: only the wrench multiblock fallback still probes points like this. */
val REACH_4 = listOf("1", "2", "3", "4")
