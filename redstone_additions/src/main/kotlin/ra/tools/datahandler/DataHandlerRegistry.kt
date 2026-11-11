package ra.tools.datahandler

import ra.api.Tools
import ra.core.PropSpec
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * `ra:tools/data_handler/init_registry`: every block type's property schema, from the
 * `properties(type) { ... }` declarations next to each block (see ra/core/Properties.kt).
 *
 *   storage ra:dh types."<type>".<name>  {type:"number",min:1,max:9999,i:0,readonly:0b,admin_only:0b,visible:1b}
 *   storage ra:dh order."<type>"         the names in declaration (menu) order
 *   storage ra:dh known                  every name declared on any type, for guessing rows of
 *                                        properties a block carries without declaring them
 *   storage ra:dh defaults."<type>"      name -> the value shown for a declared property the block
 *                                        does not carry yet (PropSpec.default), so a block
 *                                        placed before the property existed still gets its row
 *
 * Every lookup is one compound key -- "what is <name> on this block?" -- so nothing walks a list
 * per tick; the menu walks its own block's list when it is drawn.
 *
 * CALLED UNCONDITIONALLY FROM ra:load, LIKE THE WRENCH REGISTRY
 * This is configuration, not world state: it is the same on every world and it must match the
 * build that is running. An earlier version seeded its lists lazily, `unless data storage ra:dh
 * numeric`, and an upgraded world kept an old list with the filter properties missing for good.
 * Never guard this on the presence of one of its own entries.
 *
 * MENU BUTTONS AND /reload
 * A row's button carries `<menu stamp> * 1000 + <code>`. The stamp is the number of the menu the
 * player was last shown (ra.dh.menu); a click from any older menu -- scrolled back to, or drawn
 * before a /reload that may have reordered a schema -- is refused and the menu redrawn. Loading
 * resets every stamp, so a schema can be reordered freely.
 */
object DataHandlerRegistry {
	val initRegistry = Tools.DataHandler.initRegistry

	/** On a player: the stamp of the last Data Handler menu they were shown. */
	const val MENU = "ra.dh.menu"

	/** A button's action is `stamp * MUL + code`; stamps run 1..[STAMP_MAX], so it stays an int. */
	const val MUL = 10000
	const val STAMP_MAX = 200000

	/** On a player: the row index + 1 of the list they have open in the menu (DataHandlerEntries); 0 or unset, none. */
	const val EXPAND = "ra.dh.expand"

	/** On a player: the ra.dh.mid of the block their last menu showed (DataHandler.showMenu). */
	const val MENU_SEL = "ra.dh.msel"

	fun Ra.registerDataHandlerRegistry() {
		val schemas = blockProperties.toSortedMap()
		val known = schemas.values.flatten().filter { !it.dynamic }.map { it.name }.distinct().sorted()

		pack.define(initRegistry) {
			// One line per type, each replacing that type whole, rather than one `set` of the whole
			// compound: an add-on pack's own types (see Extension Examples) survive our load,
			// whichever pack loads first.
			for ((type, specs) in schemas) {
				raw("data modify storage ra:dh types.\"$type\" set value " + specs.withIndex().joinToString(",", "{", "}") { (i, p) -> "${p.name}:${p.snbt(i)}" })
				raw("data modify storage ra:dh order.\"$type\" set value ${specs.map { it.name }.snbt()}")
				val defaults = specs.mapNotNull { p -> p.default?.takeUnless { p.dynamic }?.let { "${p.name}:$it" } }
				raw(if (defaults.isEmpty()) "data remove storage ra:dh defaults.\"$type\""
					else "data modify storage ra:dh defaults.\"$type\" set value " + defaults.joinToString(",", "{", "}"))
			}
			raw("data modify storage ra:dh known set value ${known.snbt()}")
			// Lists of v6.0.1 and earlier, read by nothing now.
			for (old in listOf("registry", "numeric", "item_names", "readonly")) raw("data remove storage ra:dh $old")
			// Menus drawn before this load may name rows of a schema that changed: void them all.
			raw("scoreboard objectives add $MENU dummy")
			raw("scoreboard objectives add $EXPAND dummy")
			raw("scoreboard objectives add ${ra.tools.datahandler.props.DataHandlerVec3.STEP} dummy")
			raw("scoreboard objectives add $MENU_SEL dummy")
			// Vec3 points left by an editor when the world was saved: their clock did not survive.
			raw("function ${ra.tools.datahandler.props.Vec3Ops.expire.asId()}")
			raw("scoreboard players reset * $MENU")
			// Row indexes of a schema that may have changed.
			raw("scoreboard players reset * $EXPAND")
		}

		afterAllParts { checkSchemas(schemas) }
	}

	private fun List<String>.snbt() = joinToString(",", "[", "]") { "\"$it\"" }

	/** `data.properties.<name>`, and the keys of a property compound. */
	private val path = Regex("""data\.properties\.([a-z_][a-z0-9_]*)""")
	private val compound = Regex("""(?:data\.properties(?: set value | merge value |)|properties:|props:)\{([^{}]*)\}""")
	private val key = Regex("""(?:^|,)\s*([a-z_][a-z0-9_]*)\s*:""")

	/** Block-state properties of a skin's block_display, which share the word. Never reported. */
	private val NOT_PROPERTIES = setOf("facing", "lit")

	/**
	 * Build warnings, never failures (the scan reads command text, and a heuristic should not be
	 * able to stop a build):
	 *   - each `dynamic` declaration, which is allowed but discouraged;
	 *   - each property name a generated function reads or writes on a block without declaring it.
	 *     In a function under `blocks/<dir>/`, the name must be in that block's schema; anywhere
	 *     else, in some schema. Migration steps (`ra_migrations:`) are not scanned.
	 */
	private fun Ra.checkSchemas(schemas: Map<String, List<PropSpec>>) {
		for ((type, specs) in schemas) for (p in specs.filter { it.dynamic })
			println("[ra] warning: ${type}.${p.name} is declared dynamic: its type lives on each block instance (data.property_types), where nothing checks it. Declare a type if you can.")

		val dirToType = allBlocks.associate { "${it.ns.id}:${it.dir}" to it.id } +
			schemas.keys.associateBy { it } // wires markers: dir == type
		val anywhere = schemas.values.flatten().map { it.name }.toSet()
		val seen = sortedMapOf<String, MutableSet<String>>()
		for (fn in pack.functions + pack.generatedFunctions) {
			val id = fn.asId()
			val (ns, p) = id.split(':', limit = 2)
			// Migrations read and drop retired properties (a Tag Adder's old `tag`) by design.
			if (ns == "ra_migrations") continue
			val dir = if (p.startsWith("blocks/")) p.removePrefix("blocks/").substringBefore('/') else null
			val type = dir?.let { dirToType["$ns:$it"] ?: dirToType[it] }
			val allowed = type?.let { t -> schemas[t]?.map { it.name }?.toSet() } ?: anywhere
			for (line in fn.lines) {
				if (line.trimStart().startsWith("#")) continue
				val names = path.findAll(line).map { it.groupValues[1] } +
					compound.findAll(line).flatMap { m -> key.findAll(m.groupValues[1]).map { it.groupValues[1] } }
				for (n in names) if (n !in allowed && n !in NOT_PROPERTIES)
					seen.getOrPut(if (type != null) "$type.$n" else n) { sortedSetOf() } += id
			}
		}
		if (seen.isEmpty()) return
		println("[ra] warning: ${seen.size} block propert${if (seen.size == 1) "y is" else "ies are"} used without a declaration (properties(type) { ... }):")
		for ((n, at) in seen) println("  - $n, in ${at.take(3).joinToString(", ")}${if (at.size > 3) " (+${at.size - 3})" else ""}")
	}
}
