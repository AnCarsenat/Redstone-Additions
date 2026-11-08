package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * WHAT TYPE IS A VALUE, AND HOW TYPED TEXT BECOMES ONE.
 *
 * A data pack has no "type of" operator, so each type is a test that only that type passes. The
 * value under test is first copied to storage ra:dh probe, so none of the tests below needs a
 * macro path:
 *
 *   list      `data modify storage ra:dh probe insert 0 value 0b` succeeds. `insert` works on
 *             any list, empty or not; on anything else it fails. (`probe[]` is no test: an empty
 *             list has no element to match.)
 *   compound  `execute if data storage ra:dh probe{}`.
 *   number    two `/compute`s of `{type:"minecraft:storage",storage:"ra:dh",path:"probe",fallback:F}`,
 *             one with F = 0 and one with F = 1. The storage provider returns its fallback when
 *             the value is absent or is not a number (a string -- even "5" -- a list, a compound),
 *             and the number itself otherwise, so the two agree exactly when it is a number. Two
 *             fallbacks rather than one "impossible" sentinel: every int is a possible value.
 *   bool      a number that is the byte 0b or 1b: `execute if data storage ra:dh {probe:1b}`
 *             (an NBT match is typed, so the int 1 does not pass).
 *   string    anything else.
 *
 * What does NOT work, and what this replaces: `data modify ... set string` succeeds on a number
 * too (it writes the digits), so using it as "is a string" called every number property a string,
 * and only names on the `numeric` registry list ever got the number editor.
 *
 * TYPED TEXT
 * The input book's text reaches a command only through [parse], a function whose single line is
 * `$data modify storage ra:dh parsed set value $(text)`. A malformed text fails that one call and
 * leaves `parsed` absent; nothing else runs with it. The parsed value is then type-tested like any
 * other. Text meant to be text is never parsed: it is copied with `set from storage ra:input consume.text`.
 */
object DataHandlerTypes {
	/** `{text}`: parse the text as SNBT into storage ra:dh parsed. The only line, on purpose. */
	val parse = tool("data_handler/props/parse")

	/** `{name}` and storage ra:dh q.text: write the text as a number. Returns 1 when written. */
	val writeTypedNumber = tool("data_handler/props/write_typed_number")

	/** Tell the player the value is outside #dh.min..#dh.max. Returns 0 (nothing written). */
	val outOfRange = tool("data_handler/props/out_of_range")

	/** `{name}` and storage ra:dh q.text: write the text as a flag. Returns 1 when written. */
	val writeTypedBool = tool("data_handler/props/write_typed_bool")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET

	/** Accepted spellings of a flag, as typed. */
	private val TRUE = listOf("true", "on", "1").flatMap { it.spellings() }
	private val FALSE = listOf("false", "off", "0").flatMap { it.spellings() }
	private fun String.spellings() = listOf(this, replaceFirstChar { it.uppercase() }, uppercase()).distinct()

	/**
	 * The type of storage ra:dh probe into [into]: 0 string, 1 number, 2 bool, 3 list, 5 compound.
	 * `probe` may be changed (a list gains an entry); the caller removes it.
	 */
	fun Function.typeOfProbe(into: String) {
		raw("scoreboard players set $into $T 0")
		isNumber("probe", "#dh.num")
		raw("execute if score #dh.num $T matches 1 run scoreboard players set $into $T 1")
		raw("execute if data storage ra:dh {probe:0b} run scoreboard players set $into $T 2")
		raw("execute if data storage ra:dh {probe:1b} run scoreboard players set $into $T 2")
		raw("execute if data storage ra:dh probe{} run scoreboard players set $into $T 5")
		// Last: it changes probe.
		raw("execute store success score #dh.lst $T run data modify storage ra:dh probe insert 0 value 0b")
		raw("execute if score #dh.lst $T matches 1 run scoreboard players set $into $T 3")
	}

	/** [into] = 1 when storage ra:dh [path] holds a number (any numeric type), else 0. */
	fun Function.isNumber(path: String, into: String) {
		fun provider(fallback: Int) = """{type:"minecraft:storage",storage:"ra:dh",path:"$path",fallback:$fallback}"""
		raw("execute store result score #dh.n0 $T run compute default integer ${provider(0)}")
		raw("execute store result score #dh.n1 $T run compute default integer ${provider(1)}")
		raw("scoreboard players set $into $T 0")
		raw("execute if score #dh.n0 $T = #dh.n1 $T run scoreboard players set $into $T 1")
	}

	fun Ra.registerDataHandlerTypes() {
		pack.define(parse) { raw("\$data modify storage ra:dh parsed set value \$(text)") }

		// {name}: q.text parsed, checked to be a number, then written as parsed (5 stays an int,
		// 2.5 a double). "5" in quotes parses as text and is refused like any other word.
		pack.define(writeTypedNumber) {
			raw("data remove storage ra:dh parsed")
			callWith(parse, "storage ra:dh q")
			isNumber("parsed", "#dh.ok")
			raw("""${'$'}execute if score #dh.ok $T matches 0 run tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" takes a number, and ",color:"red"},{nbt:"q.text",storage:"ra:dh",color:"white"},{text:" is not one. Nothing was changed.",color:"red"}]""")
			raw("execute if score #dh.ok $T matches 0 run data remove storage ra:dh parsed")
			raw("execute if score #dh.ok $T matches 0 run return 0")
			// Bounds: #dh.min / #dh.max from probe, the declared range, or the whole int range when
			// nothing declares one. A decimal is judged by its whole part, as the blocks read it.
			raw("execute store result score #dh.v $T run data get storage ra:dh parsed")
			raw("execute unless score #dh.v $T >= #dh.min $T run data remove storage ra:dh parsed")
			raw("execute unless score #dh.v $T <= #dh.max $T run data remove storage ra:dh parsed")
			raw("execute unless data storage ra:dh parsed run return run function ${outOfRange.asId()}")

			raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:dh parsed")
			raw("data remove storage ra:dh parsed")
			returnValue(1)
		}

		pack.define(outOfRange) {
			raw("""tellraw @s [$PREFIX,{text:"Out of range: ",color:"red"},{text:"pick a number from ",color:"red"},{score:{name:"#dh.min",objective:"$T"},color:"white"},{text:" to ",color:"red"},{score:{name:"#dh.max",objective:"$T"},color:"white"},{text:". Nothing was changed.",color:"red"}]""")
			raw("return 0")
		}

		// {name}: q.text compared, never parsed, against the accepted spellings.
		pack.define(writeTypedBool) {
			raw("scoreboard players set #dh.bool $T -1")
			for (s in TRUE) raw("""execute if data storage ra:dh q{text:"$s"} run scoreboard players set #dh.bool $T 1""")
			for (s in FALSE) raw("""execute if data storage ra:dh q{text:"$s"} run scoreboard players set #dh.bool $T 0""")
			raw("""${'$'}execute if score #dh.bool $T matches -1 run tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" is on or off: write true, false, on, off, 1 or 0. Nothing was changed.",color:"red"}]""")
			raw("execute if score #dh.bool $T matches -1 run return 0")
			raw("\$execute if score #dh.bool $T matches 1 run data modify entity $TARGET data.properties.\$(name) set value 1b")
			raw("\$execute if score #dh.bool $T matches 0 run data modify entity $TARGET data.properties.\$(name) set value 0b")
			returnValue(1)
		}
	}
}
