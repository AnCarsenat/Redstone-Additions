package ra.settings.gen

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument

/**
 * Text pieces of the generated settings functions: storage paths, chat components and buttons.
 * Kept as text because each one is a fragment of a `tellraw` SNBT literal, built at generation
 * time from literals: nothing here has to survive a player-authored quote character being
 * substituted into a command.
 */
internal object Text {
	/** A page header bar. */
	fun bar(title: String) =
		"""[{text:"─── ",color:"dark_gray"},{text:"$title",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]"""

	/**
	 * Storage path for a global key. Always quoted -- keys contain dots, and an unquoted dot is a
	 * path separator, so `wires.rate` would silently become a nested compound instead of one key.
	 */
	fun gpath(key: String) = "global.\"$key\""

	fun ppath(block: String, prop: String) = "global.props.\"$block\".\"$prop\""

	/**
	 * A path being embedded INSIDE a quoted SNBT string. Keys are quoted in the path itself (they
	 * contain dots), and those quotes have to be escaped again once the whole path becomes the
	 * value of `nbt:"..."`. Without this the component ends early and the rest of the path leaks
	 * out as stray tokens -- valid-looking text that parses as garbage.
	 */
	fun esc(path: String) = path.replace("\"", "\\\"")

	/** `{nbt:"<path>",storage:"ra:settings",color:"aqua"}`. */
	fun value(path: String) = """{nbt:"${esc(path)}",storage:"ra:settings",color:"aqua"}"""

	/** "[Settings] <label> is now <value><extra>". */
	fun report(label: String, path: String, extra: String = "") =
		"""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"$label",color:"white"},{text:" is now ",color:"gray"},${value(path)}$extra]"""

	const val NEW_BLOCKS_ONLY = """,{text:" (new blocks only)",color:"dark_gray"}"""

	const val SPACE = """,{text:" "},"""

	/**
	 * A function-path segment. Only [a-z0-9_] survives, because a resource location rejects
	 * spaces, capitals and punctuation outright.
	 */
	fun slug(text: String): String {
		var out = text.lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
		while ("__" in out) out = out.replace("__", "_")
		return out.trim('_')
	}

	/**
	 * A menu button.
	 *
	 * Fires /trigger rather than the action's /function. Both reach the same code -- the
	 * function tree still exists and still autocompletes -- but a run_command click event raises
	 * a confirmation prompt every single time, and an operator stepping a value ten times should
	 * not answer ten dialogs. /trigger is exempt because the game already knows a trigger is
	 * safe: it only writes a score the player was explicitly enabled for.
	 *
	 * The permission check moves with it. See ra_settings:admin_dispatch.
	 */
	fun btn(text: String, fn: FunctionArgument, color: String, hover: String?, code: Int?): String {
		val shown = hover ?: "/function ${fn.asId()}"
		val cmd = if (code != null) "/trigger ra.settings.admin set $code" else "/function ${fn.asId()}"
		return """{text:"[$text]",color:"$color",bold:true,hover_event:{action:"show_text",value:"$shown"},click_event:{action:"run_command",command:"$cmd"}}"""
	}

	/** `tellraw @s [{text:"  "},<button>]`: a button on a line of its own. */
	fun buttonLine(button: String) = """tellraw @s [{text:"  "},$button]"""

	/** JSON-to-SNBT for a page's menu entry: bare keys, double-quoted strings. */
	fun snbt(value: Any?): String = when (value) {
		is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) -> "$k:${snbt(v)}" }
		is List<*> -> value.joinToString(",", "[", "]") { snbt(it) }
		is Boolean -> if (value) "1b" else "0b"
		is String -> jsonString(value)
		else -> value.toString()
	}

	/** A string as Python's `json.dumps` wrote it: escaped quotes and backslashes, non-ASCII as `\uXXXX`. */
	private fun jsonString(s: String) = buildString {
		append('"')
		for (c in s) when {
			c == '"' -> append("\\\"")
			c == '\\' -> append("\\\\")
			c.code > 127 -> append("\\u%04x".format(c.code))
			else -> append(c)
		}
		append('"')
	}
}
