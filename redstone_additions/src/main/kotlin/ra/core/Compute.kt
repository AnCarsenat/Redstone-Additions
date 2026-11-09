package ra.core

import io.github.ayfri.kore.arguments.Argument
import io.github.ayfri.kore.features.predicates.providers.ContextFloatProvider
import io.github.ayfri.kore.features.predicates.providers.ContextIntProvider
import io.github.ayfri.kore.features.predicates.providers.NumberProviderScope
import io.github.ayfri.kore.features.predicates.providers.fixedScore
import io.github.ayfri.kore.features.predicates.providers.intStorage
import io.github.ayfri.kore.features.predicates.providers.score
import io.github.ayfri.kore.features.predicates.types.EntityTarget
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.encodeToDataPackSnbt

/**
 * Arithmetic in one command, with 26.3's `/compute` and `data modify ... set compute`.
 *
 * Before 26.3 every storage update was read -> scoreboard operation -> write, three commands,
 * and every clamp was a copy plus an `execute if score ... run ... =`. A context int provider
 * does the whole sum in one evaluation: `{type:"minecraft:add",inputs:[<storage>,<score>]}`.
 *
 * Providers are Kore's typed ones ([ContextIntProvider], combined with `min`, `sub`, `add`... in
 * a [NumberProviderScope] or with Kore's operators); this file only names the two leaves every
 * caller needs and writes the two commands that consume a provider. The provider is rendered to
 * its SNBT text, so it can sit in a macro line: a storage path holding `$(id)` is just a string
 * field, and the `$` prefix is added whenever the line contains a `$(`.
 *
 * WHAT A MISSING VALUE READS AS
 * Both leaves read as 0 when there is nothing there -- an unset score, an absent storage path,
 * a value that is not a number -- because their `fallback` defaults to 0. That is the same 0 a
 * failed `data get` stored, which is what lets "first arrival of a medium" keep working.
 *
 * WHAT DOES FAIL
 * The arithmetic itself: division by zero, and any intermediate result outside 32 bits, cancel
 * the evaluation instead of wrapping the way a scoreboard operation does. A failed `/compute`
 * under `execute store result` stores 0; a failed `data modify ... set compute` writes nothing.
 * Guard a divisor that can be 0 before computing with it.
 *
 * Scoreboard `/=` and `%=` round towards negative infinity: their equivalents are `floorDiv`
 * and `floorMod`, not `div` and `mod`.
 *
 * A product that can leave 32 bits (a ratio scaled up before dividing) belongs in a float
 * provider instead: [floatProvider] and [computeScoreScaled].
 */

/** Kore's own scope object is internal; the provider functions only need a receiver. */
private object Scope : NumberProviderScope

/** Builds a provider with Kore's provider functions in scope: `provider { min(a, b) }`. */
fun provider(block: NumberProviderScope.() -> ContextIntProvider): ContextIntProvider = Scope.block()

/** Builds a float provider the same way: `floatProvider { div(fromInt(a), fromInt(b)) }`. */
fun floatProvider(block: NumberProviderScope.() -> ContextFloatProvider): ContextFloatProvider = Scope.block()

/**
 * The score of a fake player, `{type:"minecraft:score",target:{type:"minecraft:fixed",name:"#x"},score:"<objective>"}`.
 * [fallback] is what an absent score reads as (0 when omitted).
 */
fun scoreOf(holder: String, objective: String, fallback: ContextIntProvider? = null): ContextIntProvider =
	Scope.score(objective, fixedScore(holder), fallback)

/** [scoreOf] with a constant fallback. */
fun scoreOf(holder: String, objective: String, fallback: Int): ContextIntProvider = Scope.score(objective, fixedScore(holder), fallback)

/**
 * The score of the entity the compute runs for (`compute entity <e>`, see [COMPUTE_SELF]),
 * `{type:"minecraft:score",target:{type:"minecraft:context",target:this},score:"<objective>"}`.
 */
fun thisScoreOf(objective: String, fallback: ContextIntProvider? = null): ContextIntProvider =
	Scope.score(objective, EntityTarget.THIS, fallback)

/** An int read from storage, `{type:"minecraft:storage",storage:"ra:x",path:"<path>"}`. [path] may hold macro arguments. */
fun storageOf(storage: Argument, path: String): ContextIntProvider = Scope.intStorage(storage.asString(), path)

/**
 * The provider as the SNBT text a command takes. Rendered here rather than through Kore's
 * `compute { }`, which may hoist an inline provider into a registry file -- and a provider whose
 * path holds `$(id)` must stay inline in the macro line that substitutes it.
 */
fun ContextIntProvider.snbt(): String = encodeToDataPackSnbt(ContextIntProvider.serializer(), this)

/** The float provider as SNBT, see [ContextIntProvider.snbt]. */
fun ContextFloatProvider.snbt(): String = encodeToDataPackSnbt(ContextFloatProvider.serializer(), this)

private fun Function.line(text: String) = raw(if ("\$(" in text) "\$$text" else text)

/** What the compute evaluates for: `default` (no entity), or `entity @s` for [thisScoreOf]. */
const val COMPUTE_DEFAULT = "default"
const val COMPUTE_SELF = "entity @s"

/**
 * `execute [<chain> ]<stores> run compute <source> integer <value>`. [chain] is whatever goes
 * before the stores: a condition (`if score #cap x matches 1..`) or a further `store` clause.
 */
private fun Function.computeLine(stores: List<String>, value: String, source: String, chain: String) {
	val head = if (chain.isEmpty()) "" else "$chain "
	line("execute $head${stores.joinToString(" ")} run compute $source $value")
}

/** `store result score <holder> <objective>`. */
fun storeScore(holder: String, objective: String) = "store result score $holder $objective"

/** `store result storage <storage> <path> int 1`, for [computeScore]'s `alsoStore` or [computeInto]. */
fun storeInt(storage: Argument, path: String) = "store result storage ${storage.asString()} $path int 1"

/** `store result storage <storage> <path> double <scale>`: an integer result written as a fixed-point double. */
fun storeDouble(storage: Argument, path: String, scale: Double) = "store result storage ${storage.asString()} $path double $scale"

/**
 * `execute [<chain> ]store result score <holder> <objective> [<more stores> ]run compute <source> integer <value>`.
 * [alsoStore] adds further `store ...` clauses ([storeInt]), so one evaluation can land in a
 * score and in storage at once. [source] is [COMPUTE_DEFAULT] or [COMPUTE_SELF].
 */
fun Function.computeScore(
	holder: String,
	objective: String,
	value: ContextIntProvider,
	vararg alsoStore: String,
	source: String = COMPUTE_DEFAULT,
	chain: String = "",
) = computeLine(listOf(storeScore(holder, objective)) + alsoStore, "integer ${value.snbt()}", source, chain)

/** `execute [<chain> ]<store> run compute <source> integer <value>`, for a result that only goes to [store] ([storeInt]). */
fun Function.computeInto(store: String, value: ContextIntProvider, source: String = COMPUTE_DEFAULT, chain: String = "") =
	computeLine(listOf(store), "integer ${value.snbt()}", source, chain)

/**
 * `execute [<chain> ]store result score <holder> <objective> run compute <source> float <value> <scale>`.
 * The float result times [scale] becomes the command's integer result, which is what the
 * score store captures. This keeps a scaled ratio such as `amount * 10000 / capacity` in range:
 * the division happens in floats, so no intermediate product has to fit in 32 bits.
 */
fun Function.computeScoreScaled(
	holder: String,
	objective: String,
	value: ContextFloatProvider,
	scale: Int,
	source: String = COMPUTE_DEFAULT,
	chain: String = "",
) = computeLine(listOf(storeScore(holder, objective)), "float ${value.snbt()} $scale", source, chain)

/**
 * `[execute <chain> run ]data modify storage <storage> <path> set compute default integer <value>`.
 * [path] may hold macro arguments; [chain] is a condition such as `if data storage ...`.
 */
fun Function.computeStorage(storage: Argument, path: String, value: ContextIntProvider, chain: String = "") {
	val head = if (chain.isEmpty()) "" else "execute $chain run "
	line("${head}data modify storage ${storage.asString()} $path set compute $COMPUTE_DEFAULT integer ${value.snbt()}")
}
