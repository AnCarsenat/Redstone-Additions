package ra.core

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.functions.function
import io.github.ayfri.kore.features.tags.functionTag

/**
 * Function references and definitions.
 *
 * A function is named ONCE, as a [FunctionArgument] value (usually a `val` in an `*Api` object
 * or a module file), and both its definition and every call site use that value. Renaming a
 * path is then one edit, and calling a function nobody defines is caught by
 * `PackValidator` at generation time instead of failing silently in game.
 */

/** A reference to `ns:path`. Does not create anything. */
fun ref(ns: Ns, path: String): FunctionArgument = FunctionArgument(path, ns.id)

/** Defines the body of [ref]. Returns [ref] so it can be stored or chained. */
fun DataPack.define(ref: FunctionArgument, block: Function.() -> Unit): FunctionArgument {
	function(ref.name, ref.namespace, ref.directory, block)
	return ref
}

/** Declares and defines `ns:path` in one go, for functions nothing outside the file calls. */
fun DataPack.fn(ns: Ns, path: String, block: Function.() -> Unit): FunctionArgument = define(ref(ns, path), block)

/** Same as [DataPack.fn], callable while building another function (for local helpers). */
fun Function.fn(ns: Ns, path: String, block: Function.() -> Unit): FunctionArgument = datapack.fn(ns, path, block)

/**
 * A command written as text.
 *
 * Use the typed Kore builders by default. `raw` is for the cases the DSL cannot express or
 * expresses worse than vanilla: macro lines (`$...`), long SNBT literals copied from the game,
 * and commands with no builder. Every `raw` line must be a complete, valid 26.3 command.
 */
fun Function.raw(line: String) {
	require('\n' !in line) { "raw() takes one command; got several lines in ${datapack.name}: $line" }
	addLine(line)
}

/**
 * Adds [fn] to the function tag `#ns:tag`. Tags are shared: every part appends to the same
 * file, in registration order, so a hook is registered by the part that owns the hooked function.
 */
fun DataPack.hook(ns: Ns, tag: String, fn: FunctionArgument) {
	functionTag(tag, ns.id) { add(fn) }
}
