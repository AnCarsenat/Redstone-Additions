package ra.lib.input

import io.github.ayfri.kore.functions.Function
import ra.core.raw

/**
 * A number's allowed range, written the same way wherever one is asked for or refused:
 * `(1–1200)`, `(≥ 1)` with no upper bound, `(≤ 64)` with no lower one, nothing with neither. A
 * missing bound is the int extreme, as ra.input.min / ra.input.max hold it.
 *
 * Callers that ask through ra_lib:input name what is being asked for by writing
 * `storage ra:input label` (a string) before `router/open`; the number prompt then reads
 * "Enter a number for <label> (1–1200)". The router removes the label once the prompt is shown.
 */
object InputRange {
	const val LOW = Int.MIN_VALUE
	const val HIGH = Int.MAX_VALUE

	private fun score(holder: String, obj: String, color: String) =
		"""{score:{name:"$holder",objective:"$obj"},color:"$color"}"""

	/**
	 * One `tellraw [target] [before, <range>, after]` line per bound case. [before] and [after]
	 * are comma-joined JSON text parts (may be empty); [condition] is an extra `execute` chain
	 * ("if data storage ...") every line must also pass.
	 */
	fun Function.tellRange(
		target: String, before: String, after: String,
		minHolder: String, minObj: String, maxHolder: String, maxObj: String,
		condition: String = "", color: String = "white",
	) {
		val lo = "score $minHolder $minObj matches $LOW"
		val hi = "score $maxHolder $maxObj matches $HIGH"
		val cases = listOf(
			"unless $lo unless $hi" to """{text:" (",color:"gray"},${score(minHolder, minObj, color)},{text:"–",color:"gray"},${score(maxHolder, maxObj, color)},{text:")",color:"gray"}""",
			"unless $lo if $hi" to """{text:" (≥ ",color:"gray"},${score(minHolder, minObj, color)},{text:")",color:"gray"}""",
			"if $lo unless $hi" to """{text:" (≤ ",color:"gray"},${score(maxHolder, maxObj, color)},{text:")",color:"gray"}""",
			"if $lo if $hi" to "",
		)
		val cond = if (condition.isEmpty()) "" else "$condition "
		for ((test, range) in cases) {
			val parts = listOf(before, range, after).filter { it.isNotEmpty() }.joinToString(",")
			raw("execute $cond$test run tellraw $target [$parts]")
		}
	}
}
