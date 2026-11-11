package ra.tools.datahandler.props

import ra.core.ColorSpec
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * A COLOUR AS TEXT, BOTH WAYS, WITH NO TYPED CHARACTER EVER IN A COMMAND (ra.core.ColorProps).
 *
 * [parse]  storage ra:temp col.s (the typed text) -> score `#col.v`, returns 1; or 0 when the text
 *          is no colour. Accepted: `none` (#col.n 1: the property has a none), a chat colour's
 *          name, `#RRGGBB`, `#AARRGGBB` (#col.a 1: ARGB), a decimal integer with an optional `-`.
 *          The words are NBT matches against constants; every other text is read one character at
 *          a time (`set string ... i i+1`), each compared with the digits by NBT match ([digit]),
 *          and the number built with score arithmetic. A `#RRGGBB` for an ARGB colour is opaque;
 *          a name keeps the alpha in `#col.cur` (the value before), or `#col.pa` when that was none
 *          (exactly the none value: `#00FF0000` is a transparent red, not none).
 *
 * [hex]    score `#col.v` -> storage ra:temp col.rgb `"#RRGGBB"` and col.argb `"#AARRGGBB"`, scores
 *          `#col.rgb` (0..0xFFFFFF) and `#col.al` (0..255). The string is joined by one macro line
 *          whose arguments are hex digits this function wrote from a constant table.
 */
internal object ColorParse {
	val parse = tool("data_handler/props/color/parse")
	val hex = tool("data_handler/props/color/hex")
	/** score #col.al (0..255) and #col.rgb -> #col.v. */
	val compose = tool("data_handler/props/color/compose")
	private val digit = tool("data_handler/props/color/digit")
	private val hexJoin = tool("data_handler/props/color/hex_join")
	private val parseHex = tool("data_handler/props/color/parse_hex")
	private val parseDec = tool("data_handler/props/color/parse_dec")

	private const val T = Obj.TEMP
	private const val DIGITS = "0123456789ABCDEF"
	private const val K24 = 16777216
	/** Decimal digits read at most: an int has ten. */
	private const val MAX_DEC = 10

	/** `{col:{ch:"x"}}`, a root match (the only kind every tool handles). */
	private fun ch(c: Char) = """{col:{ch:"$c"}}"""

	fun Ra.registerColorParse() {
		pack.define(hex) {
			raw("scoreboard players set #col.k24 $T $K24")
			raw("scoreboard players set #col.k16 $T 16")
			raw("scoreboard players operation #col.rgb $T = #col.v $T")
			raw("scoreboard players operation #col.rgb $T %= #col.k24 $T")
			// Whatever the sign rule of %=, the low 24 bits end up in 0..0xFFFFFF.
			raw("execute if score #col.rgb $T matches ..-1 run scoreboard players add #col.rgb $T $K24")
			raw("scoreboard players operation #col.al $T = #col.v $T")
			raw("scoreboard players operation #col.al $T -= #col.rgb $T")
			raw("scoreboard players operation #col.al $T /= #col.k24 $T")
			raw("execute if score #col.al $T matches ..-1 run scoreboard players add #col.al $T 256")
			raw("scoreboard players operation #col.x $T = #col.rgb $T")
			for (i in 0..7) {
				if (i == 6) raw("scoreboard players operation #col.x $T = #col.al $T")
				raw("scoreboard players operation #col.d $T = #col.x $T")
				raw("scoreboard players operation #col.d $T %= #col.k16 $T")
				raw("scoreboard players operation #col.x $T /= #col.k16 $T")
				for ((n, c) in DIGITS.withIndex()) raw("execute if score #col.d $T matches $n run data modify storage ra:temp col.h$i set value \"$c\"")
			}
			raw("function ${hexJoin.asId()} with storage ra:temp col")
		}
		pack.define(hexJoin) {
			raw("\$data modify storage ra:temp col.rgb set value \"#\$(h5)\$(h4)\$(h3)\$(h2)\$(h1)\$(h0)\"")
			raw("\$data modify storage ra:temp col.argb set value \"#\$(h7)\$(h6)\$(h5)\$(h4)\$(h3)\$(h2)\$(h1)\$(h0)\"")
		}
		pack.define(compose) {
			raw("scoreboard players set #col.k24 $T $K24")
			raw("scoreboard players operation #col.v $T = #col.al $T")
			// Wraps past 127 into the sign bit: exactly the ARGB int.
			raw("scoreboard players operation #col.v $T *= #col.k24 $T")
			raw("scoreboard players operation #col.v $T += #col.rgb $T")
		}

		// The digit value of storage ra:temp col.ch, 0..15; 16 for anything else.
		pack.define(digit) {
			for ((n, c) in DIGITS.withIndex()) {
				raw("execute if data storage ra:temp ${ch(c)} run return $n")
				if (c.isLetter()) raw("execute if data storage ra:temp ${ch(c.lowercaseChar())} run return $n")
			}
			raw("return 16")
		}

		pack.define(parse) {
			for (w in listOf("none", "None", "NONE"))
				raw("""execute if score #col.n $T matches 1 if data storage ra:temp {col:{s:"$w"}} store result score #col.v $T run data get storage ra:dh spec.col.nv""")
			for (w in listOf("none", "None", "NONE")) raw("""execute if score #col.n $T matches 1 if data storage ra:temp {col:{s:"$w"}} run return 1""")
			// A chat colour by name: the alpha the value had, as a preset button does.
			raw("scoreboard players set #col.nm $T 0")
			for ((name, rgb) in ColorSpec.PRESETS)
				raw("""execute if data storage ra:temp {col:{s:"$name"}} run scoreboard players set #col.rgb $T $rgb""")
			for ((name, _) in ColorSpec.PRESETS)
				raw("""execute if data storage ra:temp {col:{s:"$name"}} run scoreboard players set #col.nm $T 1""")
			raw("execute if score #col.nm $T matches 1 if score #col.a $T matches 0 run scoreboard players operation #col.v $T = #col.rgb $T")
			raw("execute if score #col.nm $T matches 1 if score #col.a $T matches 0 run return 1")
			raw("execute if score #col.nm $T matches 1 run scoreboard players operation #col.keep $T = #col.rgb $T")
			raw("execute if score #col.nm $T matches 1 run scoreboard players operation #col.v $T = #col.cur $T")
			raw("execute if score #col.nm $T matches 1 run function ${hex.asId()}")
			raw("execute if score #col.nm $T matches 1 if score #col.n $T matches 1 if score #col.cur $T = #col.nv $T run scoreboard players operation #col.al $T = #col.pa $T")
			raw("execute if score #col.nm $T matches 1 run scoreboard players operation #col.rgb $T = #col.keep $T")
			raw("execute if score #col.nm $T matches 1 run function ${compose.asId()}")
			raw("execute if score #col.nm $T matches 1 run return 1")

			raw("execute store result score #col.len $T run data get storage ra:temp col.s")
			raw("data modify storage ra:temp col.ch set string storage ra:temp col.s 0 1")
			raw("execute if data storage ra:temp ${ch('#')} run return run function ${parseHex.asId()}")
			raw("return run function ${parseDec.asId()}")
		}

		// `#` and six digits, or eight for an ARGB colour.
		pack.define(parseHex) {
			raw("execute unless score #col.len $T matches 7 unless score #col.len $T matches 9 run return 0")
			raw("execute if score #col.len $T matches 9 if score #col.a $T matches 0 run return 0")
			raw("scoreboard players set #col.v $T 0")
			raw("scoreboard players set #col.k16 $T 16")
			for (i in 1..8) {
				// `#RRGGBB` read: opaque for an ARGB colour (+ 0xFF000000, as a signed int).
				if (i == 7) raw("execute if score #col.len $T matches 7 if score #col.a $T matches 1 run scoreboard players remove #col.v $T $K24")
				if (i == 7) raw("execute if score #col.len $T matches 7 run return 1")
				raw("data modify storage ra:temp col.ch set string storage ra:temp col.s $i ${i + 1}")
				raw("execute store result score #col.d $T run function ${digit.asId()}")
				raw("execute if score #col.d $T matches 16 run return 0")
				raw("scoreboard players operation #col.v $T *= #col.k16 $T")
				raw("scoreboard players operation #col.v $T += #col.d $T")
			}
			raw("return 1")
		}

		// A decimal integer: an optional `-`, then one to ten digits, refused past the int range.
		pack.define(parseDec) {
			raw("scoreboard players set #col.v $T 0")
			raw("scoreboard players set #col.neg $T 0")
			raw("scoreboard players set #col.bad $T 0")
			raw("execute if data storage ra:temp ${ch('-')} run scoreboard players set #col.neg $T 1")
			raw("scoreboard players operation #col.nd $T = #col.len $T")
			raw("scoreboard players operation #col.nd $T -= #col.neg $T")
			raw("execute unless score #col.nd $T matches 1..$MAX_DEC run return 0")
			raw("scoreboard players set #col.k10 $T 10")
			for (i in 0..MAX_DEC) {
				// Position i is a digit, unless it is the sign (position 0 of a negative number).
				val sign = if (i == 0) " unless score #col.neg $T matches 1" else ""
				raw("execute if score #col.len $T matches ${i + 1}..$sign run function ${decStep(i).asId()}")
				raw("execute if score #col.bad $T matches 1 run return 0")
			}
			raw("return 1")
		}
		for (i in 0..MAX_DEC) pack.define(decStep(i)) {
			raw("data modify storage ra:temp col.ch set string storage ra:temp col.s $i ${i + 1}")
			raw("execute store result score #col.d $T run function ${digit.asId()}")
			raw("execute unless score #col.d $T matches 0..9 run return run scoreboard players set #col.bad $T 1")
			// v * 10 + d must stay an int. Negative numbers are built negative, so -2147483648 fits.
			raw("execute if score #col.neg $T matches 0 if score #col.v $T matches 214748365.. run return run scoreboard players set #col.bad $T 1")
			raw("execute if score #col.neg $T matches 0 if score #col.v $T matches 214748364 if score #col.d $T matches 8.. run return run scoreboard players set #col.bad $T 1")
			raw("execute if score #col.neg $T matches 1 if score #col.v $T matches ..-214748365 run return run scoreboard players set #col.bad $T 1")
			raw("execute if score #col.neg $T matches 1 if score #col.v $T matches -214748364 if score #col.d $T matches 9.. run return run scoreboard players set #col.bad $T 1")
			raw("scoreboard players operation #col.v $T *= #col.k10 $T")
			raw("execute if score #col.neg $T matches 0 run scoreboard players operation #col.v $T += #col.d $T")
			raw("execute if score #col.neg $T matches 1 run scoreboard players operation #col.v $T -= #col.d $T")
		}
	}

	private fun decStep(i: Int) = tool("data_handler/props/color/dec_$i")
}
