package ra.tools

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.raw
import ra.core.ref

/**
 * Small writers the tools repeat in nearly every file.
 *
 * The tools are mostly `execute <long chain> run function <one of ours>` lines whose conditions
 * (`anchored eyes positioned ^ ^ ^2 unless data storage ...`) the typed `execute { }` builder makes
 * three times longer than the vanilla text. These helpers keep the chain as vanilla text but take
 * the called function as a [FunctionArgument], so the call is still a checked value and never a
 * string: a typo is a compile error and a missing definition fails `PackValidator`.
 */

/** `ra:tools/<path>`. */
fun tool(path: String): FunctionArgument = ref(Ns.RA, "tools/$path")

/** `execute <chain> run function <fn>[ <args>]`. [args] is `{k:v}` or `with storage ra:x path`. */
fun Function.executeRun(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute $chain run function ${fn.asId()}${if (args.isEmpty()) "" else " $args"}")

/** `execute <chain> run return run function <fn>[ <args>]`. */
fun Function.executeReturnRun(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute $chain run return run function ${fn.asId()}${if (args.isEmpty()) "" else " $args"}")

/** `function <fn> with storage ra:<storage> <path>` -- written as text so the storage spelling stays beet's. */
fun Function.callWith(fn: FunctionArgument, storage: String, path: String? = null) =
	raw("function ${fn.asId()} with storage ra:$storage${if (path == null) "" else " $path"}")

/** The custom-block marker a raycast step picks up: nearest within 1.5 of the probe point. */
const val RAY_TARGET = "@e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest]"

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
