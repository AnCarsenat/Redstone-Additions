package ra.modules.wires.common

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.command
import io.github.ayfri.kore.functions.Function

/**
 * One vanilla command with a typed function call in the MIDDLE of its chain.
 *
 * A call at the end (`execute <chain> run function <fn>`) is core `executeRun` /
 * `executeReturnRun`. What is left here are the chains that test a function's result
 * (`execute unless function ra_lib:redstone/any run ...`), which neither the typed `execute { }`
 * builder nor `executeRun` can write. The chain stays vanilla text; every function in it is a
 * [FunctionArgument], so a typo is a compile error and `PackValidator` sees the call.
 *
 * ```
 * line("execute unless function", RaLib.Redstone.any, "run return 0")
 * ```
 */
fun Function.line(text: String, vararg rest: Any) {
	require('\n' !in text) { "line() takes one command: $text" }
	val args = rest.map {
		when (it) {
			is FunctionArgument -> it
			is String -> literal(it).also { _ -> require('\n' !in it) { "line() takes one command: $it" } }
			else -> error("line(): unsupported part $it")
		}
	}
	addLine(command(text, *args.toTypedArray()))
}
