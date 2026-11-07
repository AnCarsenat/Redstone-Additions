package ra.settings.gen

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument

/**
 * Every clickable admin action, in order. A button carries the action's code; the dispatcher
 * looks the path back up in this table (`storage ra:settings actions`) and runs it. Keeping the
 * paths in one list is what lets a button be a /trigger while the same action stays a real
 * autocompleting function.
 *
 * The same path may be registered more than once (each button gets its own code), exactly as the
 * beet generator did, so existing codes in chat history keep meaning the same thing.
 */
internal class ActionTable {
	private val entries = mutableListOf<String>()

	/**
	 * Registers [fn] and returns its code.
	 *
	 * [nav] marks an action that MOVES you somewhere -- opening a page, Back, the uninstall
	 * prompt. Those must not trigger the after-action redraw: Back would draw the index and then
	 * immediately draw the page you had just left on top of it, which is why the menus appeared
	 * twice.
	 *
	 * Codes start at 2. 1 is reserved for "open the index", because a bare
	 * /trigger ra.settings.admin adds 1 -- and somebody typing that means "show me the settings",
	 * not "perform whichever action happens to be first in a generated list". A reserved code
	 * turns a plausible typo into the obvious answer instead of a silent, arbitrary change.
	 */
	fun act(fn: FunctionArgument, nav: Boolean = false): Int {
		entries += if (nav) "{f:\"${fn.asId()}\",nav:1b}" else "{f:\"${fn.asId()}\"}"
		return entries.size + 1
	}

	fun snbt() = entries.joinToString(",", "[", "]")
}
