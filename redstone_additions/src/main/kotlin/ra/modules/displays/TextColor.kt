package ra.modules.displays

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * `ra_displays:text_color/...`: the text colour a display may use, checked before it is used.
 *
 * A text component's `color` is a name ([NAMES]) or `#RRGGBB`. Anything else would make the
 * component fail to decode, and the display would lose its text, so a colour property is
 * checked here, never pasted: [applyColor] copies the property, when valid, into the marker's
 * `data.data.color` (the block's default otherwise), and the text is built with
 * `data modify ... color set from` that copy. Runs only on a restyle.
 *
 * A hex colour is checked one character at a time, each compared with plain NBT matches
 * ([hexDigit]): no macro, so no character of it is ever part of a command.
 */
internal object TextColor {
	val NAMES = listOf(
		"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
		"dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
	)
	private const val HEX = "0123456789abcdefABCDEF"

	/** In: storage ra:temp dsp.c. Returns 1 when it is a colour, 0 otherwise. */
	val check = ref(Ns.DISPLAYS, "text_color/check")
	private val hexDigit = ref(Ns.DISPLAYS, "text_color/hex_digit")

	private const val T = Obj.TEMP

	fun Ra.registerTextColor() {
		pack.define(check) {
			for (n in NAMES) raw("""execute if data storage ra:temp {dsp:{c:"$n"}} run return 1""")
			// `#` and six hex digits: seven characters, `data get` of a string being its length.
			raw("execute store result score #dsp.len $T run data get storage ra:temp dsp.c")
			raw("execute unless score #dsp.len $T matches 7 run return 0")
			raw("data modify storage ra:temp dsp.ch set string storage ra:temp dsp.c 0 1")
			raw("""execute unless data storage ra:temp {dsp:{ch:"#"}} run return 0""")
			for (i in 1..6) {
				raw("data modify storage ra:temp dsp.ch set string storage ra:temp dsp.c $i ${i + 1}")
				raw("execute unless function ${hexDigit.asId()} run return 0")
			}
			raw("return 1")
		}
		pack.define(hexDigit) {
			for (c in HEX) raw("""execute if data storage ra:temp {dsp:{ch:"$c"}} run return 1""")
			raw("return 0")
		}
	}

	/**
	 * As the marker: `data.data.<into>` = `data.properties.<prop>` when it is a colour, else
	 * [default] (a constant). A string the property is not is read as nothing.
	 */
	fun Function.applyColor(prop: String, default: String, into: String = "color") {
		raw("""data modify entity @s data.data.$into set value "$default"""")
		raw("data remove storage ra:temp dsp.c")
		raw("data modify storage ra:temp dsp.c set from entity @s data.properties.$prop")
		raw("execute store result score #dsp.cok $T run function ${check.asId()}")
		raw("execute if score #dsp.cok $T matches 1 run data modify entity @s data.data.$into set from storage ra:temp dsp.c")
	}
}
