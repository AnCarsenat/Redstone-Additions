package ra.hub.uninstall

import io.github.ayfri.kore.DataPack

/**
 * What the generated pack leaves in a world, read back from its own functions: every
 * `scoreboard objectives add`, and every top-level key written under a `storage <ns:path>` of a
 * pack namespace.
 *
 * WHY READ IT BACK. The uninstall used to be a hand-written list, and by 6.0.0 it had missed
 * more than it caught: the transport, jetpack, ender, enchanting and hold-timer objectives, and
 * eight storages. Reading the pack's own commands cannot fall behind it. It runs after every part
 * has registered ([ra.core.Ra.afterAllParts]), so it sees the whole pack.
 */
internal object PackFootprint {
	private val objectiveAdd = Regex("""scoreboard objectives add ([^\s$]+)""")

	// Only the storage id is matched: a line can name two storages (`set from storage ...`), and
	// what follows each one is read from the line directly.
	private val storageUse = Regex("""storage ([a-z0-9_.-]+:[a-z0-9_./-]+) (?=\S)""")

	/** Every objective the pack adds whose name starts with `ra.`, in first-seen order. */
	fun objectives(pack: DataPack): List<String> = lines(pack)
		.flatMap { line -> objectiveAdd.findAll(line).map { it.groupValues[1] } }
		.filter { it.startsWith("ra.") }
		.distinct()
		.toList()

	/**
	 * Storage id → the top-level keys the pack writes under it, both sorted. Only storages in a
	 * pack namespace (`ra`, `ra_*`). A key built from a macro variable is skipped: only the game
	 * knows it, and every such path in the pack today sits below a fixed top-level key anyway.
	 * Reads are collected along with writes; removing a key that is not there does nothing.
	 */
	fun storageKeys(pack: DataPack): Map<String, List<String>> {
		val keys = sortedMapOf<String, MutableSet<String>>()
		for (line in lines(pack)) for (m in storageUse.findAll(line)) {
			val id = m.groupValues[1]
			val ns = id.substringBefore(':')
			if (ns != "ra" && !ns.startsWith("ra_")) continue
			val rest = line.substring(m.range.last + 1)
			val found = if (rest.startsWith("{")) compoundKeys(rest) else listOfNotNull(firstSegment(rest))
			keys.getOrPut(id) { sortedSetOf() } += found.filter { it.isNotEmpty() && "$(" !in it }
		}
		return keys.mapValues { it.value.toList() }
	}

	private fun lines(pack: DataPack) =
		(pack.functions + pack.generatedFunctions).asSequence().flatMap { it.lines.asSequence() }

	/** `a.b[0]` → `a`; `"x y".z` → `"x y"`. */
	private fun firstSegment(path: String): String? {
		if (path.startsWith("\"")) {
			val end = path.indexOf('"', 1)
			return if (end > 0) path.substring(0, end + 1) else null
		}
		return path.takeWhile { it != '.' && it != '[' && it != '{' && !it.isWhitespace() }.ifEmpty { null }
	}

	/** The top-level keys of the SNBT compound [snbt] starts with (`data merge storage x {a:1,b:{}}` → a, b). */
	private fun compoundKeys(snbt: String): List<String> {
		val keys = mutableListOf<String>()
		var depth = 0
		var quote: Char? = null
		var expectKey = false
		var i = 0
		while (i < snbt.length) {
			val c = snbt[i]
			when {
				quote != null -> if (c == '\\') i++ else if (c == quote) quote = null
				depth == 1 && expectKey && !c.isWhitespace() && c != '}' -> {
					val end = snbt.indexOf(':', i)
					if (end < 0) return keys
					keys += snbt.substring(i, end).trim()
					expectKey = false
					i = end
				}
				c == '"' || c == '\'' -> quote = c
				c == '{' || c == '[' -> { depth++; expectKey = depth == 1 && c == '{' }
				c == '}' || c == ']' -> { depth--; if (depth == 0) return keys }
				depth == 1 && c == ',' -> expectKey = true
			}
			i++
		}
		return keys
	}
}
