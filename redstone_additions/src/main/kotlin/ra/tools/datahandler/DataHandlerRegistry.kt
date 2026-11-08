package ra.tools.datahandler

import ra.api.Tools
import ra.tools.datahandler.PropDecl.Companion.bool
import ra.tools.datahandler.PropDecl.Companion.item
import ra.tools.datahandler.PropDecl.Companion.list
import ra.tools.datahandler.PropDecl.Companion.number
import ra.tools.datahandler.PropDecl.Companion.text
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * `ra:tools/data_handler/init_registry`: the property names the Handler knows how to show, in
 * display order (`storage ra:dh registry`), and the declared type of each (`storage ra:dh types`,
 * `{<name>:{type:"number",min:1,max:9999}, ...}`).
 *
 * A data pack cannot iterate the keys of a compound, which is why this list exists at all -- it is
 * the only hand-maintained part left. Adding a property to a block means adding its declaration here --
 * name and type -- and nothing else: the type picks the row and the editor. Position matters
 * only in that it decides the action id a row's button carries (100 + index).
 *
 * CALLED UNCONDITIONALLY FROM ra:load, LIKE THE WRENCH AND READONLY REGISTRIES
 * This is configuration, not world state: it is the same on every world and it must match the
 * build that is running. It used to be seeded lazily instead, `unless data storage ra:dh numeric`,
 * from three call sites -- and that guard is exactly what broke the two filter properties. A world
 * upgraded from an earlier version already had `numeric`, so the guard passed, this function never
 * ran again, and the world kept an old `registry` with no filter_item or filter_medium in it and no
 * item_names list at all. Neither filter had a row in the editor, on any block, for ever.
 *
 * So: never guard this on the presence of one of its own lists. Every list is rewritten on every
 * load, and a name added here reaches an existing world on the next /reload.
 *
 * APPEND, DO NOT INSERT
 * A row's button carries 100 + its index, and those ids are baked into the chat message a player is
 * looking at. Inserting a name in the middle renumbers every row after it, so a menu drawn before a
 * /reload would edit the wrong property when clicked. New names go on the end.
 */
object DataHandlerRegistry {
	val initRegistry = Tools.DataHandler.initRegistry

	/**
	 * Menu order, each with its declared type. Append only (see above).
	 *
	 * Every property gets a type here, so the editor never has to guess one from the value sitting
	 * in it. Guessing is a fallback for names another pack appends to `storage ra:dh registry` at
	 * load without a type, and those rows are flagged in the menu.
	 *
	 * `channel`, `anchor_id`, `tag` and `entity_selector` are text on purpose: they are identifiers
	 * that blocks compare, and a channel called "5" must stay the text 5 -- turning one into a number
	 * is the v5.1.6 bug that stopped a vault matching its partner. Number bounds are the smallest
	 * value the block itself accepts (its `ra_lib:util/property` read clamps to the same `min`).
	 */
	private val PROPERTIES = listOf(
		bool("inverted"), text("mode"), text("channel"), text("target"), text("tag"), text("entity_selector"),
		text("message_block"), text("gate"), text("gate_type"), text("output"),
		number("range", min = 1), number("radius", min = 1), number("delay", min = 1), number("cooldown", min = 1),
		number("power", max = 15), number("distance"), number("extend", min = 1), number("pulse", min = 1),
		number("chance", max = 100), number("transfer_rate", min = 1), number("generation_rate", min = 1, max = 10000),
		number("eu_use"), text("tier"), number("tier_level"), text("input1"), text("output1"), text("anchor_id"),
		list("targets"), number("rate", min = 1), item("filter_item"), text("filter_medium"), text("medium"), bool("enabled"),
	)

	/**
	 * Properties blocks carry that have no row in the menu, declared all the same so the build's
	 * undeclared-property check knows them: a multiblock's `speed`, and `interval`, the old name
	 * of a drain's `cooldown` that its tick folds into the new one.
	 */
	private val UNLISTED = listOf(number("speed"), number("interval", min = 1))

	/**
	 * Names that appear in `data.properties` but are not block properties: the block-state
	 * `properties` of a skin's block_display. Never reported.
	 */
	private val NOT_PROPERTIES = setOf("facing", "lit")

	/** The built-in declaration of [name], if any. */
	internal fun declOf(name: String): PropDecl? = (PROPERTIES + UNLISTED).firstOrNull { it.name == name }

	fun Ra.registerDataHandlerRegistry() {
		// The tools port this list centrally, in its original order. Anything a module registered
		// on top goes on the end, which is the only place a new name may go.
		val decls = linkedMapOf<String, PropDecl>()
		for (p in PROPERTIES) decls[p.name] = p
		for ((name, p) in dataHandlerProperties) decls.putIfAbsent(name, p)
		dataHandlerProperties.clear()
		dataHandlerProperties += decls
		val names = decls.keys.toList()
		val types = decls.values + UNLISTED.filter { it.name !in decls }

		pack.define(initRegistry) {
			raw("data modify storage ra:dh registry set value ${names.snbt()}")
			raw("data modify storage ra:dh types set value ${types.joinToString(",", "{", "}") { "${it.name}:${it.snbt()}" }}")
			// Lists of v6.0.1 and earlier, read by nothing now.
			raw("data remove storage ra:dh numeric")
			raw("data remove storage ra:dh item_names")
		}

		val declared = types.map { it.name }.toSet()
		afterAllParts { warnUndeclared(declared) }
	}

	private fun List<String>.snbt() = joinToString(",", "[", "]") { "\"$it\"" }

	/** `data.properties.<name>`, `data.properties{<name>:`, and the keys of a property compound. */
	private val path = Regex("""data\.properties\.([a-z_][a-z0-9_]*)""")
	private val compound = Regex("""(?:data\.properties(?: set value | merge value |)|properties:|props:)\{([^{}]*)\}""")
	private val key = Regex("""(?:^|,)\s*([a-z_][a-z0-9_]*)\s*:""")

	/**
	 * Every property name a generated function writes or reads on a block that has no declared
	 * type, printed with the functions that use it. A warning, not a failure: the scan reads
	 * command text, and a heuristic should not be able to stop a build.
	 */
	private fun Ra.warnUndeclared(declared: Set<String>) {
		val seen = sortedMapOf<String, MutableSet<String>>()
		for (fn in pack.functions + pack.generatedFunctions) for (line in fn.lines) {
			if (line.trimStart().startsWith("#")) continue
			val names = path.findAll(line).map { it.groupValues[1] } +
				compound.findAll(line).flatMap { m -> key.findAll(m.groupValues[1]).map { it.groupValues[1] } }
			for (n in names) if (n !in declared && n !in NOT_PROPERTIES) seen.getOrPut(n) { sortedSetOf() } += where(fn.asId())
		}
		if (seen.isEmpty()) return
		println("[ra] warning: ${seen.size} block propert${if (seen.size == 1) "y has" else "ies have"} no declared Data Handler type (DataHandlerRegistry):")
		for ((n, at) in seen) println("  - $n, on ${at.joinToString(", ")}")
	}

	/** `ra_gates:blocks/clock/tick` -> `block ra_gates:clock`; anything else as is. */
	private fun where(id: String): String {
		val (ns, p) = id.split(':', limit = 2)
		return if (p.startsWith("blocks/")) "block $ns:${p.removePrefix("blocks/").substringBefore('/')}" else id
	}
}
