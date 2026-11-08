package ra.modules.wires.common

import io.github.ayfri.kore.functions.Function
import ra.core.raw

/**
 * 26.3 context int providers, written as the SNBT `/compute` takes, for the wires module's
 * scoreboard arithmetic.
 *
 * One `execute store result score ... run compute default integer {...}` replaces a chain of
 * `scoreboard players operation` lines and the constant fake players (`#two`, `#scale`...) that
 * existed only to be a right-hand side. Kept as text, like the rest of the module's commands, so
 * the generated line reads the same as the Kotlin.
 *
 * Semantics match the scoreboard ops they replace: `/=` floors, so division is [floorDiv], never
 * `div`; a min/max over the inputs is the `if score A > B run ... = B` clamp. An unset score
 * reads as 0 (the provider's default fallback), as it does as an operation source. Division by
 * zero is an error rather than a no-op, so a divisor that can be zero stays behind its guard.
 */
internal object Cip {
	/** A fake player's score: `{type:"score",target:{type:"fixed",name:...},score:...}`. */
	fun score(holder: String, objective: String) =
		"""{type:"score",target:{type:"fixed",name:"$holder"},score:"$objective"}"""

	fun min(vararg inputs: Any) = """{type:"min",inputs:[${inputs.joinToString(",")}]}"""
	fun max(vararg inputs: Any) = """{type:"max",inputs:[${inputs.joinToString(",")}]}"""
	fun mul(vararg inputs: Any) = """{type:"mul",inputs:[${inputs.joinToString(",")}]}"""
	fun sub(left: Any, right: Any) = """{type:"sub",left:$left,right:$right}"""

	/** Floor division: what scoreboard `/=` does. */
	fun floorDiv(left: Any, right: Any) = """{type:"floor_div",left:$left,right:$right}"""
}

/** `execute [<chain> ]store result score <holder> <objective> run compute default integer <provider>`. */
internal fun Function.storeCompute(holder: String, objective: String, provider: String, chain: String = "") =
	raw("execute ${if (chain.isEmpty()) "" else "$chain "}store result score $holder $objective run compute default integer $provider")

/** `execute store result storage <target> <path> int 1 run compute default integer <provider>`. */
internal fun Function.storeComputeStorage(target: String, path: String, provider: String) =
	raw("execute store result storage $target $path int 1 run compute default integer $provider")
