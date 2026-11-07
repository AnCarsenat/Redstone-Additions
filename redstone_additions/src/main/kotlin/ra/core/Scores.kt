package ra.core

import io.github.ayfri.kore.functions.Function

/**
 * `execute unless score <holder> <objective> matches -2147483648.. run scoreboard players set
 * <holder> <objective> <value>`: gives [holder] a starting score only when it has none.
 *
 * A score that has never been set reads as ABSENT, and every comparison against an absent score
 * is false. `matches -2147483648..` is true for any value at all, so `unless` on it is the test
 * for "no score yet".
 */
fun Function.ensureScore(holder: String, objective: String, value: Int = 0) {
	raw("execute unless score $holder $objective matches -2147483648.. run scoreboard players set $holder $objective $value")
}
