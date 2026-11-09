package ra.core

import io.github.ayfri.kore.DataPack

/**
 * Generation-time checks Kore cannot do on its own, run just before the pack is written.
 *
 * The beet build had `lint_macros.py` and nothing else; a call to a function that did not exist
 * only showed up as a line in the game log after `/reload`. Here every `function ns:path` and
 * `schedule function ns:path` in a pack namespace must resolve to a function (or, for `#ns:path`,
 * a function tag) declared in this pack, or generation fails with the caller's path.
 */
object PackValidator {
	// `$` and parentheses are captured so a macro inside the path (`give_$(kit)_kit`) is seen
	// whole and skipped, rather than read as a call to `give_`.
	private val call = Regex("""function (#?)([a-z0-9_.-]+):([a-z0-9_./$()-]+)""")
	private val ownNamespaces = Ns.entries.filter { it != Ns.MINECRAFT }.map { it.id }.toSet()

	// Minecraft accepts names that start with a digit (`$(1a)`), so no letter-first rule here.
	private val macroVariable = Regex("""\$\([A-Za-z0-9_]+\)""")

	/**
	 * A line starting with `$` must contain at least one `$(name)`. One that does not raises
	 * "No variables in macro" at load time and Minecraft rejects the WHOLE function, not just the
	 * line -- that is how the Big Torch once shipped without its torch (a macro's only variable
	 * was replaced by a constant and the leftover `$` took the function down). Always fatal,
	 * strict mode or not: unlike a missing callee, nothing later in the port can fix it.
	 */
	fun macroLinesWithoutVariables(pack: DataPack): List<String> =
		(pack.functions + pack.generatedFunctions).flatMap { fn ->
			fn.lines.withIndex()
				.filter { (_, line) -> line.trimStart().startsWith("$") && !macroVariable.containsMatchIn(line) }
				.map { (i, line) -> "${fn.asId()}:${i + 1}: macro line with no \$(variable): ${line.trim().take(120)}" }
		}

	private val oldDispatchKey = Regex("""\{\s*(function|condition)\s*:\s*\\?"(minecraft:)?[a-z_]+""")
	private val oldBlockState = Regex("""block_state\s*:\s*\{\s*Name\s*:""")
	private val playsound = Regex("""\bplaysound \S+ \S+ \S+ \S+ \S+ \S+ \S+ ([0-9.]+)""")

	/**
	 * 26.3 renamed the dispatch key of inline item modifiers (`function`) and predicates
	 * (`condition`) to `type`. A line still using the old key fails to parse and Minecraft drops
	 * the WHOLE function -- and a function tag listing it then fails too: one Keypad placement
	 * handler written `{function:"minecraft:fill_player_head"}` took `#ra:placement_handlers`
	 * down and with it every block's placement. Also catches a `playsound` pitch above 2.0, which
	 * fails the same way, and a display's `block_state:{Name:...}`, which 26.3 cannot decode (the
	 * command succeeds, the display shows air; only the game log says why). Always fatal.
	 */
	fun preTwentySixThreeSyntax(pack: DataPack): List<String> =
		(pack.functions + pack.generatedFunctions).flatMap { fn ->
			fn.lines.withIndex().mapNotNull { (i, line) ->
				if (line.trimStart().startsWith("#")) return@mapNotNull null
				val where = "${fn.asId()}:${i + 1}"
				oldDispatchKey.find(line)?.let { return@mapNotNull "$where: inline `${it.groupValues[1]}:` key, 26.3 uses `type:`: ${line.trim().take(120)}" }
				// Not fatal to parse -- the command succeeds -- but the display decodes to air.
				if (oldBlockState.containsMatchIn(line)) return@mapNotNull "$where: block_state {Name:...}, 26.3 reads {id:...} (the display shows air): ${line.trim().take(120)}"
				playsound.find(line)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { it > 2.0 }
					?.let { "$where: playsound pitch $it above 2.0: ${line.trim().take(120)}" }
			}
		}

	private val itemSummon = Regex("""\bsummon (minecraft:)?item\b(?!_)""")

	/**
	 * An item entity summoned without an `Item` (or with one filled in afterwards) holds an empty
	 * stack, and the game discards it before adding it: "Tried to add entity minecraft:item but it
	 * was marked as removed already". The follow-up `data modify ... Item set from` then finds no
	 * entity -- or, worse, the nearest OTHER item carrying the same tag -- so the item is lost or
	 * overwritten. A Magic Crate looping on exactly this flooded the log with ~1900 warnings a
	 * tick and hit the command limit every tick. Always fatal: summon with a placeholder
	 * (`{Item:{id:"minecraft:stone",count:1}}`) and overwrite it, as `ra_lib:inventory/drop_item`
	 * does.
	 */
	fun itemSummonsWithoutItem(pack: DataPack): List<String> =
		(pack.functions + pack.generatedFunctions).flatMap { fn ->
			fn.lines.withIndex()
				.filter { (_, line) -> !line.trimStart().startsWith("#") && itemSummon.containsMatchIn(line) && "Item:" !in line.substringAfter("summon") }
				.map { (i, line) -> "${fn.asId()}:${i + 1}: item summoned without an Item: ${line.trim().take(120)}" }
		}

	/**
	 * Every `tellraw` a function reachable from `#minecraft:load` runs must be one of:
	 * - gated on the "Show the load message" setting ([loadMessage] / [LoadMessages.GATE]);
	 * - a deliberate [loadNotice] (a warning shown whatever the setting);
	 * - for `@a[tag=ra.debug]` only: those players asked for debug lines;
	 * - for `@s`: at load that is the server console, not anybody's chat.
	 *
	 * v6.0.1 gated the hub's greeting but every module printed its own "loaded!" line before it,
	 * ungated, so turning the setting off still left eleven lines in chat. Always fatal.
	 *
	 * Calls are followed through `function` and function tags; `schedule` is not followed (a
	 * scheduled function does not run during load), nor is any `function` inside a tellraw, which
	 * is a click event, not a call.
	 */
	fun ungatedLoadMessages(pack: DataPack): List<String> {
		val byId = (pack.functions + pack.generatedFunctions).associateBy { it.asId() }
		val tags = pack.tags.filter { it.type == "function" }.associate { "${it.namespace}:${it.fileName}" to it.values.map { v -> v.name } }
		val seen = mutableSetOf<String>()
		val queue = ArrayDeque(tags["minecraft:load"].orEmpty())
		val bad = mutableListOf<String>()
		while (queue.isNotEmpty()) {
			val ref = queue.removeFirst()
			if (!seen.add(ref)) continue
			if (ref.startsWith("#")) { queue += tags[ref.removePrefix("#")].orEmpty(); continue }
			val fn = byId[ref] ?: continue
			for ((i, raw) in fn.lines.withIndex()) {
				val line = raw.trimStart()
				if (line.startsWith("#")) continue
				if ("tellraw" !in line) {
					if ("schedule " in line) continue
					call.findAll(line).forEach { m -> m.destructured.let { (t, ns, p) -> if ("$" !in p) queue += "$t$ns:$p" } }
					continue
				}
				val ok = line.startsWith(LoadMessages.GATE) || raw in LoadMessages.alwaysShown ||
					Regex("""tellraw @a\[tag=ra\.debug] """).containsMatchIn(line) || Regex("""tellraw @s """).containsMatchIn(line)
				if (!ok) bad += "$ref:${i + 1}: tellraw at load not gated on the load-message setting: ${line.take(120)}"
			}
		}
		return bad
	}

	/** Fails generation listing every unresolved call. */
	fun validate(pack: DataPack) {
		val problems = problems(pack)
		check(problems.isEmpty()) {
			"Pack validation failed (${problems.size}):\n" + problems.joinToString("\n") { "  - $it" }
		}
	}

	/** Prints unresolved calls without failing; for partially ported trees (`-Pra.strict=false`). */
	fun report(pack: DataPack) {
		val problems = problems(pack)
		if (problems.isEmpty()) return
		println("[ra] ${problems.size} unresolved call(s), strict mode off:")
		problems.forEach { println("  - $it") }
	}

	fun problems(pack: DataPack): List<String> {
		val all = pack.functions + pack.generatedFunctions
		val defined = all.map { it.asId() }.toSet()
		val tags = pack.tags.filter { it.type == "function" }.map { "${it.namespace}:${it.fileName}" }.toSet()

		val problems = buildList {
			// Function tag entries: a missing entry fails the whole tag at load.
			for (tag in pack.tags.filter { it.type == "function" }) for (entry in tag.values) {
				val id = entry.name.removePrefix("#")
				val ns = id.substringBefore(':')
				if (ns !in ownNamespaces || entry.required == false) continue
				val ok = if (entry.name.startsWith("#")) id in tags else id in defined
				if (!ok) add("#${tag.namespace}:${tag.fileName}: lists ${entry.name}, which this pack never defines")
			}
			// Advancement rewards.
			for (adv in pack.advancements) {
				val fn = adv.rewards?.function ?: continue
				if (fn.namespace in ownNamespaces && fn.asId() !in defined)
					add("advancement ${adv.namespace ?: pack.name}:${adv.fileName}: reward function ${fn.asId()} is never defined")
			}
			for (fn in all) for (line in fn.lines) {
				if (line.trimStart().startsWith("#")) continue
				for (m in call.findAll(line)) {
					val (isTag, ns, path) = m.destructured
					if (ns !in ownNamespaces || "$" in path) continue
					val id = "$ns:$path"
					val ok = if (isTag.isNotEmpty()) id in tags else id in defined
					if (!ok) add("${fn.asId()}: calls ${isTag}$id, which this pack never defines")
				}
			}
		}

		return problems.distinct()
	}
}
