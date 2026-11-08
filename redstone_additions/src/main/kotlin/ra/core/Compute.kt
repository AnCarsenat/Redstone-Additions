package ra.core

import io.github.ayfri.kore.arguments.Argument
import io.github.ayfri.kore.features.predicates.providers.ContextIntProvider
import io.github.ayfri.kore.features.predicates.providers.NumberProviderScope
import io.github.ayfri.kore.features.predicates.providers.fixedScore
import io.github.ayfri.kore.features.predicates.providers.intStorage
import io.github.ayfri.kore.features.predicates.providers.score
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
 */

/** Kore's own scope object is internal; the provider functions only need a receiver. */
private object Scope : NumberProviderScope

/** Builds a provider with Kore's provider functions in scope: `provider { min(a, b) }`. */
fun provider(block: NumberProviderScope.() -> ContextIntProvider): ContextIntProvider = Scope.block()

/** The score of a fake player, `{type:"minecraft:score",target:{type:"minecraft:fixed",name:"#x"},score:"<objective>"}`. */
fun scoreOf(holder: String, objective: String): ContextIntProvider = Scope.score(objective, fixedScore(holder))

/** An int read from storage, `{type:"minecraft:storage",storage:"ra:x",path:"<path>"}`. [path] may hold macro arguments. */
fun storageOf(storage: Argument, path: String): ContextIntProvider = Scope.intStorage(storage.asString(), path)

/**
 * The provider as the SNBT text a command takes. Rendered here rather than through Kore's
 * `compute { }`, which may hoist an inline provider into a registry file -- and a provider whose
 * path holds `$(id)` must stay inline in the macro line that substitutes it.
 */
fun ContextIntProvider.snbt(): String = encodeToDataPackSnbt(ContextIntProvider.serializer(), this)

private fun Function.line(text: String) = raw(if ("\$(" in text) "\$$text" else text)

/** `compute default integer <provider>`, the tail both commands below end with. */
private fun computeCommand(value: ContextIntProvider) = "compute default integer ${value.snbt()}"

/**
 * `execute store result score <holder> <objective> [<more stores> ]run compute default integer <value>`.
 * [alsoStore] adds further `store ...` clauses (`"store result storage ra:x path int 1"`), so one
 * evaluation can land in a score and in storage at once.
 */
fun Function.computeScore(holder: String, objective: String, value: ContextIntProvider, vararg alsoStore: String) {
	val stores = (listOf("store result score $holder $objective") + alsoStore).joinToString(" ")
	line("execute $stores run ${computeCommand(value)}")
}

/** `data modify storage <storage> <path> set compute default integer <value>`. [path] may hold macro arguments. */
fun Function.computeStorage(storage: Argument, path: String, value: ContextIntProvider) =
	line("data modify storage ${storage.asString()} $path set ${computeCommand(value)}")

/** `store result storage <storage> <path> int 1`, for [computeScore]'s `alsoStore`. */
fun storeInt(storage: Argument, path: String) = "store result storage ${storage.asString()} $path int 1"
