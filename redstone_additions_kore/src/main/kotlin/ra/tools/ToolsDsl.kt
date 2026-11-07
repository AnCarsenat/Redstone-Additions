package ra.tools

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
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

/** The custom-block marker a raycast step picks up: nearest within 1.5 of the probe point. */
const val RAY_TARGET = "@e[type=marker,tag=${Tag.CUSTOM_BLOCK},distance=..1.5,limit=1,sort=nearest]"

/**
 * The tools' reach: probe [steps] points along the player's look, and run [fn] as the first
 * custom-block marker found. Every step after the first is skipped once [foundFlag]
 * (`storage ra:temp <flag>`) has been set by [fn], so only the nearest block answers.
 * Context: as the player.
 */
fun Function.raycast(steps: List<String>, foundFlag: String, fn: FunctionArgument) {
	steps.forEachIndexed { i, step ->
		val guard = if (i == 0) "" else " unless data storage ra:temp $foundFlag"
		executeRun("at @s anchored eyes positioned ^ ^ ^$step$guard as $RAY_TARGET", fn)
	}
}

/** The four one-block steps the Data Handlers, Multimeter and wrench RMB use. */
val REACH_4 = listOf("1", "2", "3", "4")
