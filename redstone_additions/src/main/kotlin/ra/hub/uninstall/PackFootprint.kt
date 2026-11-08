package ra.hub.uninstall

import io.github.ayfri.kore.DataPack

/**
 * What the generated pack leaves in a world, read back from its own functions: every
 * `scoreboard objectives add`, and every top-level key written under a `storage <ns:path>` of a
 * pack namespace.
 *
 * WHY READ IT BACK. The uninstall used to be a hand-written list, and by 6.0.0 it had missed
 * more than it caught: the transport, jetpack, ender, enchanting and hold-timer objectives, and
 * eight storages. Reading the pack's own commands cannot fall behind it. It runs once the pack is
 * complete ([ra.core.Ra.whenPackComplete]), after every part and every deferred step, and
 * [uncovered] re-checks the finished pack, so a miss fails the build instead of shipping.
 */
internal object PackFootprint {
	private val objectiveAdd = Regex("""scoreboard objectives add ([^\s$]+)""")

	// Only the storage id is matched: a line can name two storages (`set from storage ...`), and
	// what follows each one is read from the line directly.
	private val storageUse = Regex("""storage ([a-z0-9_.-]+:[a-z0-9_./-]+) (?=\S)""")

	/**
	 * Every objective the pack adds, in first-seen order. Not only `ra.*`: whatever the pack adds
	 * is the pack's to remove, and a name outside the convention is no reason to leave it behind.
	 */
	fun objectives(pack: DataPack): List<String> = lines(pack)
		.flatMap { line -> objectiveAdd.findAll(line).map { it.groupValues[1] } }
		.distinct()
		.toList()

	private val macroObjectiveAdd = Regex("""scoreboard objectives add \S*\$\(""")
	// Writes only: reading a key named by a macro (`... from storage ra:dh $(list)[$(i)]`,
	// `data get`, `if data`) leaves nothing behind.
	private val macroStorage = Regex("""(?<!from |get |if data |unless data )storage (\S*\$\(|ra[a-z0-9_]*:\S+ \$\()""")
	private val objectiveRemove = Regex("""^scoreboard objectives remove (\S+)$""")
	private val storageRemove = Regex("""^data remove storage (\S+) (\S+)$""")
	private val functionCall = Regex("""^function (\S+)$""")

	/**
	 * What the finished pack creates that the function [uninstall] (with every function it calls)
	 * does not remove, one line per problem; empty when the uninstall is complete. Run on the
	 * finished pack, so it catches a function defined after the uninstall read the pack, and the
	 * one thing the scan cannot read: an objective or storage whose NAME comes from a macro,
	 * known only in game.
	 */
	fun uncovered(pack: DataPack, uninstall: String): List<String> {
		val byId = (pack.functions + pack.generatedFunctions).associateBy { it.asId() }
		val removes = mutableListOf<String>()
		val seen = mutableSetOf<String>()
		fun collect(id: String) {
			if (!seen.add(id)) return
			for (line in byId[id]?.lines.orEmpty()) {
				removes += line
				functionCall.find(line)?.let { collect(it.groupValues[1]) }
			}
		}
		collect(uninstall)
		val removedObjectives = removes.mapNotNull { objectiveRemove.find(it)?.groupValues?.get(1) }.toSet()
		val removedKeys = removes.mapNotNull { storageRemove.find(it)?.groupValues }.map { it[1] to it[2] }.toSet()

		val problems = mutableListOf<String>()
		objectives(pack).filter { it !in removedObjectives }.forEach { problems += "objective $it is never removed" }
		for ((id, keys) in storageKeys(pack)) for (key in keys) {
			if (id to key !in removedKeys) problems += "storage $id key $key is never removed"
		}
		for (line in lines(pack)) {
			if (macroObjectiveAdd.containsMatchIn(line)) problems += "objective named by a macro, which the uninstall cannot know: $line"
			if (macroStorage.containsMatchIn(line)) problems += "storage named by a macro, which the uninstall cannot know: $line"
		}
		return problems.distinct()
	}

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
