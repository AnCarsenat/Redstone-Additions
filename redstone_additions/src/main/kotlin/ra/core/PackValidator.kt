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
