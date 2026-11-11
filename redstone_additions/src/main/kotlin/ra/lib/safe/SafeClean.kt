package ra.lib.safe

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * THE CORE SANITISER: turn player input into a value that can be pasted into a command.
 *
 * [SafeText]'s `check` and `guard` answer "is this value safe?" and refuse it when it is not.
 * These functions make it safe instead, so a caller always has something to paste:
 *
 *   clean   storage ra:safe {s: <value>, f: <TextFormat.code>} -> storage ra:safe out (a string).
 *           Returns 1 when out is usable, 0 (out removed) when nothing usable is left.
 *             FREE      `"` becomes `'`, `\` becomes `/`, a line break or tab a space. The result
 *                       can sit inside a double-quoted string of a macro line (a text component's
 *                       `{text:"$(value)"}`) and never closes it.
 *             ID, NAME, TAG   every character the format does not accept is dropped (`My Tag!`
 *                       as TAG gives `MyTag`). A TAG still starting with `ra.` is refused.
 *             SELECTOR, COORDS   structure cannot be repaired: copied when valid, refused when not.
 *           A number is cleaned as its digits; a compound or list is refused. At most 256
 *           characters are read. Whatever comes out passes `check` in the same format: that is the
 *           last step, so the two can never disagree.
 *   number  storage ra:safe {s: <value>} -> storage ra:safe n (int) and score #sf.n ra.temp.
 *           Returns 1 when s is a number of any numeric type (a decimal keeps its whole part),
 *           0 for a string, compound or list -- "5" in quotes is text, not a number. A value that
 *           went through here is an int in storage or on a scoreboard: it can never be text.
 *
 * HOW CLEAN BUILDS THE STRING
 * One character at a time off the front of a copy (`data modify ... set string ... 0 1`), each
 * classified exactly as `check` does (the shared class table and `step`), then appended with the
 * one macro line `data modify storage ra:safe out set value "$(out)$(c)"`. Both arguments are
 * already free of `"`, `\` and line breaks when it runs, so the line can only ever write a string.
 */
object SafeClean {
	val clean = RaLib.Safe.clean
	val number = RaLib.Safe.number
	private val walk = ref(Ns.LIB, "safe/clean/walk")
	private val char = ref(Ns.LIB, "safe/clean/char")
	private val append = ref(Ns.LIB, "safe/clean/append")

	private const val T = Obj.TEMP

	/** Longest input read, the same cap `check` puts on a value. */
	private const val MAX = 256

	/** The score [clean] leaves its answer in, for [cleanFrom]. */
	const val OK = "#sf.cl"

	/**
	 * Clean the value at [source] (`storage ra:temp x.y`, `entity @s data.properties.z`) in
	 * [format]: [OK] is 1 and `storage ra:safe out` holds the result, or [OK] is 0.
	 */
	fun Function.cleanFrom(source: String, format: TextFormat) {
		raw("data remove storage ra:safe s")
		raw("data modify storage ra:safe s set from $source")
		raw("data modify storage ra:safe f set value ${format.code}")
		raw("execute store result score $OK $T run function ${clean.asId()}")
	}

	/** [into] (a score holder in ra.temp) = 1 when the value at [source] is a number, else 0. */
	fun Function.isNumberAt(source: String, into: String) {
		raw("data remove storage ra:safe s")
		raw("data modify storage ra:safe s set from $source")
		raw("execute store result score $into $T run function ${number.asId()}")
	}

	fun Ra.registerSafeClean() {
		pack.define(clean) {
			raw("data remove storage ra:safe out")
			raw("execute unless data storage ra:safe s run return 0")
			raw("execute unless data storage ra:safe f run return 0")
			// A compound or a list has no one-line text form: refused, whatever the format.
			raw("execute if data storage ra:safe s{} run return 0")
			raw("data modify storage ra:safe cw set from storage ra:safe s")
			raw("execute store success score #sf.lst $T run data modify storage ra:safe cw insert 0 value 0b")
			raw("data remove storage ra:safe cw")
			raw("execute if score #sf.lst $T matches 1 run return 0")
			// A number is read as its digits.
			raw("data modify storage ra:safe cw set string storage ra:safe s")
			raw("execute store result score #sf.f $T run data get storage ra:safe f")
			raw("data modify storage ra:safe out set value \"\"")
			raw("scoreboard players set #sf.i $T 0")
			// SELECTOR and COORDS are not filtered: a dropped character would change what they mean.
			val whole = "execute if score #sf.f $T matches ${TextFormat.SELECTOR.code}..${TextFormat.COORDS.code}"
			raw("$whole run data modify storage ra:safe out set from storage ra:safe cw")
			raw("$whole run data modify storage ra:safe cw set value \"\"")
			function(walk)
			raw("data remove storage ra:safe cw")
			raw("data remove storage ra:safe c")

			// The last word is check's, in the same format.
			raw("""execute if data storage ra:safe {out:""} run return run data remove storage ra:safe out""")
			raw("data modify storage ra:safe s set from storage ra:safe out")
			raw("execute store result score #sf.r $T run function ${RaLib.Safe.check.asId()}")
			raw("execute if score #sf.r $T matches 0 run data remove storage ra:safe out")
			raw("return run scoreboard players get #sf.r $T")
		}

		// One character off the front of ra:safe cw, kept, replaced or dropped by char.
		pack.define(walk) {
			raw("""execute if data storage ra:safe {cw:""} run return 0""")
			raw("execute if score #sf.i $T matches $MAX.. run return 0")
			raw("data modify storage ra:safe c set string storage ra:safe cw 0 1")
			raw("data modify storage ra:safe cw set string storage ra:safe cw 1")
			function(char)
			raw("scoreboard players add #sf.i $T 1")
			function(walk)
		}

		// ra:safe c, for format #sf.f.
		pack.define(char) {
			val free = "execute if score #sf.f $T matches ${TextFormat.FREE.code}"
			raw("""$free if data storage ra:safe {c:"\\"} run data modify storage ra:safe c set value "/"""")
			raw("""$free if data storage ra:safe {c:"\""} run data modify storage ra:safe c set value "'"""")
			for (lb in listOf("\\n", "\\r", "\\t"))
				raw("""$free if data storage ra:safe {c:"$lb"} run data modify storage ra:safe c set value " """")
			raw("$free run return run function ${append.asId()} with storage ra:safe")
			// ID, NAME, TAG: what classify cannot be trusted with is dropped first, then step judges.
			raw("""execute if data storage ra:safe {c:"\\"} run return 0""")
			raw("""execute if data storage ra:safe {c:"\""} run return 0""")
			for (lb in listOf("\\n", "\\r", "\\t")) raw("""execute if data storage ra:safe {c:"$lb"} run return 0""")
			raw("scoreboard players set #sf.c $T 0")
			raw("scoreboard players set #sf.bad $T 0")
			raw("function ${SafeText.classify.asId()} with storage ra:safe")
			raw("function ${SafeText.step.asId()}")
			raw("execute if score #sf.bad $T matches 1 run return 0")
			raw("function ${append.asId()} with storage ra:safe")
		}
		// {out, c}: never `"`, `\` or a line break -- char replaced or dropped those, and out is
		// only ever built here, from such characters.
		pack.define(append) {
			raw("\$data modify storage ra:safe out set value \"\$(out)\$(c)\"")
		}

		// The DataHandler's number test, moved here: a storage number provider read twice with two
		// fallbacks. A number reads the same both times; anything else falls back to each.
		pack.define(number) {
			fun provider(fallback: Int) = """{type:"minecraft:storage",storage:"ra:safe",path:"s",fallback:$fallback}"""
			raw("data remove storage ra:safe n")
			raw("execute store result score #sf.n0 $T run compute default integer ${provider(0)}")
			raw("execute store result score #sf.n1 $T run compute default integer ${provider(1)}")
			raw("execute unless score #sf.n0 $T = #sf.n1 $T run return 0")
			raw("scoreboard players operation #sf.n $T = #sf.n0 $T")
			raw("execute store result storage ra:safe n int 1 run scoreboard players get #sf.n $T")
			raw("return 1")
		}
	}
}
