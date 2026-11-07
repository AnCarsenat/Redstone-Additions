package ra.modules.wires.common

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.command
import io.github.ayfri.kore.functions.Function

/**
 * One vanilla command whose function calls are typed values.
 *
 * The wires pack is mostly `execute <chain> run function <fn>` with chains (`if score A < B`,
 * `unless function ra_lib:redstone/any`, `store result ... run function`) that the typed
 * `execute { }` builder can only write markedly longer than vanilla. The chain stays vanilla
 * text; every function in it is a [FunctionArgument], so a typo is a compile error and
 * `PackValidator` sees the call -- the same thing Kore's own `returnRun(function)` does.
 *
 * ```
 * line("execute if entity @s[tag=ra.wires.bridge] run function", Bridge.tick)
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
