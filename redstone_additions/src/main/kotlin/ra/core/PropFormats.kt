package ra.core

/**
 * WHAT A TEXT PROPERTY MAY HOLD.
 *
 * Several blocks paste a text property straight into a command through a macro: the sensors'
 * `entity_selector` into `execute as $(entity_selector) ...`, the Boxer's `input1` into
 * `execute positioned $(input1) ...`, a vault's `channel` into `{channel:"$(channel)"}`. A macro
 * substitutes text, not a value, so whatever the property holds becomes part of the command: a
 * selector `@s if function ns:any run say` turns the Tag Adder into "run any function, as
 * anybody". A format says which characters a value may use so that it can only ever be the one
 * argument it is pasted as. It is checked by `ra_lib:safe/check` (ra/lib/safe/SafeText.kt):
 *
 *   FREE      anything. Only for a value no command ever pastes (the Message Block's text).
 *   ID        letters, digits and `_ . + - : / #`. No space, no quote: one bare word. The default
 *             for text, item ids and list entries.
 *   NAME      ID plus spaces. For a value that is only ever compared, or pasted inside quotes.
 *   TAG       an entity tag: letters, digits and `_ . + -`, and never `ra.`-prefixed (the pack's
 *             own tags, `ra.admin` among them, are not something a block may hand out).
 *   SELECTOR  `@a`/`@e`/`@n`/`@p`/`@r`/`@s`, optionally one `[...]`. Outside double quotes: no
 *             quote but `"`, no backslash, spaces only inside the brackets, nothing after the
 *             closing bracket. A command can only be extended past an argument by a space at
 *             argument level, which this leaves no way to reach.
 *   COORDS    three relative coordinates (`~` or `^`, a sign, at most one whole digit and a
 *             fraction): `^ ^ ^-1`, `~ ~1.5 ~`. Nine blocks away at most, and no letters at all.
 *
 * [code] is what the schema stores (`fmt`) and the checker reads.
 */
enum class TextFormat(val code: Int, val hint: String) {
	FREE(0, "any text"),
	ID(1, "letters, digits and _ . + - : / # only, without spaces"),
	NAME(2, "letters, digits, spaces and _ . + - : / # only"),
	TAG(3, "letters, digits and _ . + - only, not starting with ra."),
	SELECTOR(4, "a selector like @e[type=pig,distance=..5]: no backslash, and nothing after the closing ]"),
	COORDS(5, "three relative coordinates like ^ ^ ^1, each within 9 blocks"),
}

private fun PropertiesBuilder.put(spec: PropSpec) {
	require(specs.none { it.name == spec.name }) { "property ${spec.name} declared twice" }
	specs += spec
}

/**
 * A text property with an explicit [format]. [adminSetting] (with [adminOnly]) names a GLOBAL bool
 * setting that, when on, lets players without server-settings access edit it too.
 */
fun PropertiesBuilder.text(
	name: String, format: TextFormat, adminOnly: Boolean = false, adminSetting: String? = null,
	readonly: Boolean = false, visible: Boolean = true, default: String? = null,
) = put(PropSpec(name, PropType.TEXT, readonly = readonly, adminOnly = adminOnly, visible = visible, format = format, adminSetting = adminSetting, default = default?.let(::snbtString)))

/**
 * A list of text, each entry checked against [format]. [choices] is a `storage` / `path` pair the
 * Data Handler offers as a pick list for each entry; [entryLabel] names an entry ("Strength 3").
 */
fun PropertiesBuilder.textList(
	name: String, max: Int? = null, pad: Boolean = false, format: TextFormat = TextFormat.ID,
	choices: Pair<String, String>? = null, entryLabel: String? = null,
	readonly: Boolean = false, adminOnly: Boolean = false, adminSetting: String? = null, visible: Boolean = true,
	default: List<String>? = null,
) = put(
	PropSpec(
		name, PropType.LIST, of = PropType.TEXT, maxLength = max, pad = pad, readonly = readonly, adminOnly = adminOnly,
		visible = visible, format = format, adminSetting = adminSetting, choices = choices, entryLabel = entryLabel,
		default = default?.joinToString(",", "[", "]", transform = ::snbtString),
	),
)
