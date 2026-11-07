package ra.core

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function

/**
 * Calls to the pack's own functions behind a condition written as vanilla text.
 *
 * Many calls sit behind a chain of block-state, item and NBT-selector tests
 * (`anchored eyes positioned ^ ^ ^2 unless data storage ...`) that the typed `execute { }`
 * builder writes three times longer than vanilla. These helpers keep the chain as text but take
 * the called function as a [FunctionArgument], so the call is still a checked value and never a
 * string: a typo is a compile error and a missing definition fails `PackValidator`.
 *
 * Arguments ([args]) are written after the function id: `{k:v}`, or `with storage ra:x path`.
 */

private fun withArgs(fn: FunctionArgument, args: String) = "function ${fn.asId()}" + if (args.isEmpty()) "" else " $args"

private fun chainPrefix(chain: String) = if (chain.isEmpty()) "" else "$chain "

/** `execute <chain> run function <fn>[ <args>]`. An empty [chain] writes `execute run function ...`. */
fun Function.executeRun(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute ${chainPrefix(chain)}run ${withArgs(fn, args)}")

/** `execute <chain> run return run function <fn>[ <args>]`. */
fun Function.executeReturnRun(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute ${chainPrefix(chain)}run return run ${withArgs(fn, args)}")

/**
 * `execute [<chain> ]store result score <holder> <objective> run function <fn>[ <args>]`:
 * the function's return value into a score. [objective] defaults to `ra.temp`.
 */
fun Function.executeStoreRun(chain: String, holder: String, fn: FunctionArgument, args: String = "", objective: String = Obj.TEMP) =
	executeRun("${chainPrefix(chain)}store result score $holder $objective", fn, args)

/**
 * `function <fn> with <source>`, the source as vanilla text: `storage ra:temp place_item`,
 * `entity @s data.properties`... Written as text so the beet spelling of the source stays.
 */
fun Function.callWith(fn: FunctionArgument, source: String) = raw("function ${fn.asId()} with $source")

/** `return run function <fn>[ <args>]`. */
fun Function.returnRun(fn: FunctionArgument, args: String = "") = raw("return run ${withArgs(fn, args)}")

/**
 * The text a macro line uses to call [ref] with a literal argument block -- for the lines that
 * must stay macros (`$function ra_lib:redstone/dust/$(dust) {...}`), where the typed `function()`
 * cannot be used. Built from the ref so a rename still reaches the call. Does not write anything.
 */
fun macroCall(ref: FunctionArgument, args: String = "") = withArgs(ref, args)
