package ra.core

/**
 * BLOCK PROPERTY SCHEMAS: what a block type's `data.properties` hold, declared next to the block.
 *
 * ```kotlin
 * properties(block) {
 *     number("delay", min = 1, max = 9999)
 *     bool("inverted")
 *     text("channel")
 *     list("targets", max = 15, pad = true)
 *     item("filter_item")
 *     number("generation_rate", min = 1, readonly = true)
 * }
 * ```
 *
 * Declaration order is menu order. The schema is generated into `storage ra:dh types` at load
 * (`ra:tools/data_handler/init_registry`), one compound per block type, rewritten every load:
 *
 *   types."<type>".<name> = {type:"number",code:1,min:1,max:9999,i:0,readonly:0b,admin_only:0b,visible:1b,goggles:1b}
 *
 * `code` is [PropType.code], so a reader takes the type with one `data get` into a score rather than
 * a compound match per type; the flags are bytes for the same reason.
 *   order."<type>"        = ["delay", ...]
 *
 * so the Data Handler and the wrench answer "what is <name> on this block?" with one compound
 * lookup, and nothing walks a list per tick.
 *
 * Flags:
 *   readonly    the block's own working value. Shown, never editable by the Data Handler, never
 *               cycled by the wrench. The Creative Data Handler still edits it.
 *   adminOnly   only players holding `ra.admin` (server-settings access) may change it. Others
 *               see it locked, with the reason on hover; the write re-checks.
 *   visible     false: not shown to players without `ra.admin`, and not editable by them. Admins
 *               see it greyed "hidden from players" and can edit it.
 *   goggles     whether a `prop_line` of it is drawn on the Goggles billboard. Unset (null) follows
 *               `visible`; true shows it even when hidden from the Data Handler; false keeps a
 *               visible but uninteresting value (a fixed cooldown) off the billboard. Generated as
 *               the resolved byte `goggles:1b|0b`, so the Goggles decide with one compound match.
 *               Not written for a dynamic property: the instance's `property_types` decides.
 *
 * DYNAMIC (allowed, discouraged): a property declared `dynamic(name)` takes its type and flags
 * from the block instance, `data.property_types.<name>` on the marker (same shape as above),
 * instead of from this schema. Every dynamic declaration is a build warning: the type then lives
 * in world data, where nothing checks it.
 */
enum class PropType(val id: String, val code: Int) {
	TEXT("text", 0),
	NUMBER("number", 1),
	BOOL("bool", 2),
	LIST("list", 3),

	/** Text that is an item id; the Data Handler offers [Set from hand]. */
	ITEM("item", 4),
}

data class PropSpec(
	val name: String,
	val type: PropType,
	val min: Int? = null,
	val max: Int? = null,
	/** A list's entry type; only [PropType.TEXT] is checked. */
	val of: PropType? = null,
	/** A list's longest length. */
	val maxLength: Int? = null,
	/** A list padded with "" up to [maxLength] when written. */
	val pad: Boolean = false,
	val readonly: Boolean = false,
	val adminOnly: Boolean = false,
	val visible: Boolean = true,
	val dynamic: Boolean = false,
	/** Goggles billboard: null follows [visible]. See the class comment. */
	val goggles: Boolean? = null,
) {
	init {
		require(name.matches(Regex("[a-z_][a-z0-9_]*"))) { "property name '$name' must be [a-z_][a-z0-9_]*" }
		require(type == PropType.NUMBER || (min == null && max == null)) { "$name: only a number has bounds" }
		require(type == PropType.LIST || (of == null && maxLength == null && !pad)) { "$name: only a list has of / max / pad" }
	}

	/** The registry entry, `{type:"number",min:1,...}`, at menu position [index]. */
	fun snbt(index: Int): String = buildList {
		if (dynamic) add("dynamic:1b") else {
			add("type:\"${type.id}\"")
			add("code:${type.code}")
		}
		min?.let { add("min:$it") }
		max?.let { add("max:$it") }
		of?.let { add("of:\"${it.id}\""); add("of_code:${it.code}") }
		maxLength?.let { add("max_length:$it") }
		if (pad) add("pad:1b")
		add("readonly:${if (readonly) 1 else 0}b")
		add("admin_only:${if (adminOnly) 1 else 0}b")
		add("visible:${if (visible) 1 else 0}b")
		if (!dynamic) add("goggles:${if (goggles ?: visible) 1 else 0}b")
		add("i:$index")
	}.joinToString(",", "{", "}")
}

/** The `properties { }` builder. */
class PropertiesBuilder {
	internal val specs = mutableListOf<PropSpec>()

	private fun add(spec: PropSpec) {
		require(specs.none { it.name == spec.name }) { "property ${spec.name} declared twice" }
		specs += spec
	}

	fun number(name: String, min: Int = 0, max: Int = 9999, readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null) =
		add(PropSpec(name, PropType.NUMBER, min, max, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles))

	fun bool(name: String, readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null) =
		add(PropSpec(name, PropType.BOOL, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles))

	fun text(name: String, readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null) =
		add(PropSpec(name, PropType.TEXT, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles))

	fun item(name: String, readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null) =
		add(PropSpec(name, PropType.ITEM, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles))

	fun list(
		name: String, of: PropType? = null, max: Int? = null, pad: Boolean = false,
		readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null,
	) = add(PropSpec(name, PropType.LIST, of = of, maxLength = max, pad = pad, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles))

	/** Type and flags come from each block instance (`data.property_types.<name>`). Discouraged. */
	fun dynamic(name: String) = add(PropSpec(name, PropType.TEXT, dynamic = true))
}
