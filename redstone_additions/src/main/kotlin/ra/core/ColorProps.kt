package ra.core

import io.github.ayfri.kore.functions.Function

/**
 * COLOR PROPERTIES: a colour as one int.
 *
 *   RGB   (`alpha = false`)  `RED << 16 | GREEN << 8 | BLUE`, 0..16777215 (0xFFFFFF). With
 *         `none = true`, -1 means "no colour".
 *   ARGB  (`alpha = true`)   `ALPHA << 24 | RGB` as a signed int, the form a text display's
 *         `background` takes. Alpha 0 is fully transparent, and 0 is "none".
 *
 * ```kotlin
 * properties(block) { color("background", ColorSpec(alpha = true, none = true, default = 0)) }
 * // use: readColor("background", spec) -> score #col.v ra.temp, then
 * //      execute store result entity <display> background int 1 run scoreboard players get #col.v ra.temp
 * ```
 *
 * A player picks one of the sixteen chat colours ([PRESETS], which keep the alpha the value has,
 * or [ColorSpec.presetAlpha] when it was none), an opacity (ARGB), [None], or types one in the
 * input book: a chat colour's name, `none`, `#RRGGBB`, `#AARRGGBB` (ARGB only) or a decimal
 * number. The text is read one character at a time and turned into a number by score arithmetic
 * (tools/datahandler/props/ColorParse.kt): nothing typed is ever part of a command.
 *
 * Generated into the schema as `col:{alpha:1b|0b,none:1b|0b,def:<int>,pa:<int>,nv:<int>}`, nv being
 * the "none" value; the editor is tools/datahandler/props/DataHandlerColor.kt.
 */
class ColorSpec(
	val alpha: Boolean = false,
	val none: Boolean = false,
	val default: Int = 0xFFFFFF,
	/** The opacity a preset gets when the value was none (ARGB only), 0..255. */
	val presetAlpha: Int = 255,
) {
	init {
		require(presetAlpha in 1..255) { "color presetAlpha must be 1..255" }
		require(alpha || default in 0..0xFFFFFF || (none && default == NONE_RGB)) { "an RGB color's default must be 0..0xFFFFFF (or -1 for none)" }
	}

	/** What "none" is stored as. */
	val noneValue get() = if (alpha) 0 else NONE_RGB

	fun snbtFields(): String =
		"col:{alpha:${if (alpha) 1 else 0}b,none:${if (none) 1 else 0}b,def:$default,pa:$presetAlpha,nv:$noneValue}"

	companion object {
		const val NONE_RGB = -1

		/** The sixteen chat colours, by name, with their RGB. Preset button k sets the k-th. */
		val PRESETS = listOf(
			"black" to 0x000000, "dark_blue" to 0x0000AA, "dark_green" to 0x00AA00, "dark_aqua" to 0x00AAAA,
			"dark_red" to 0xAA0000, "dark_purple" to 0xAA00AA, "gold" to 0xFFAA00, "gray" to 0xAAAAAA,
			"dark_gray" to 0x555555, "blue" to 0x5555FF, "green" to 0x55FF55, "aqua" to 0x55FFFF,
			"red" to 0xFF5555, "light_purple" to 0xFF55FF, "yellow" to 0xFFFF55, "white" to 0xFFFFFF,
		)

		/** The opacity buttons of an ARGB colour: label to alpha. */
		val OPACITIES = listOf("100%" to 255, "75%" to 191, "50%" to 128, "25%" to 64)
	}
}

/** A color property: see [ColorSpec]. */
fun PropertiesBuilder.color(
	name: String, spec: ColorSpec,
	readonly: Boolean = false, adminOnly: Boolean = false, visible: Boolean = true, goggles: Boolean? = null,
) {
	require(specs.none { it.name == name }) { "property $name declared twice" }
	specs += PropSpec(name, PropType.COLOR, readonly = readonly, adminOnly = adminOnly, visible = visible, goggles = goggles, default = spec.default.toString(), color = spec)
}

/**
 * Use time, as the block's marker: `data.properties.<name>` into the score `#col.v` (ra.temp).
 * Missing, it is the default. An RGB value outside 0..0xFFFFFF (other than its "none") reads as the
 * default; any int is a valid ARGB. Whatever the property holds, the result is an integer.
 */
fun Function.readColor(name: String, spec: ColorSpec) {
	val t = Obj.TEMP
	val p = "data.properties.$name"
	raw("scoreboard players set #col.v $t ${spec.default}")
	raw("execute if data entity @s $p store result score #col.v $t run data get entity @s $p")
	if (!spec.alpha) {
		val keep = if (spec.none) " unless score #col.v $t matches ${ColorSpec.NONE_RGB}" else ""
		raw("execute unless score #col.v $t matches 0..${0xFFFFFF}$keep run scoreboard players set #col.v $t ${spec.default}")
	}
}
