package ra.lib.safe

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.Ns

/**
 * `ra_lib:safe/...`: is a text value safe to paste into a command? See [TextFormat] for the
 * formats and why they exist.
 *
 *   check   storage ra:safe {s: <value>, f: <TextFormat.code>} -> returns 1 when [s] is a string in
 *           that format, 0 otherwise. Walks the string one character at a time.
 *   guard   `{p, f}`, as a block's marker: returns 1 when `data.properties.<p>` is safe. The value
 *           last found safe is kept in `data.data.safe.<p>`, so a value that has not changed costs
 *           one comparison and no walk -- this sits in per-tick paths (the Entity Detector).
 *
 * These refuse an unsafe value; [SafeClean] (`clean`, `number`) makes one safe instead, on the
 * same class table and [step].
 *
 * Every macro sink of a player-editable value calls `guard` first and does nothing when it fails.
 * That holds for any way the value got there: the Data Handler (which checks too, to say why),
 * the Clipboard, an old world, a command.
 *
 * HOW A CHARACTER IS CLASSIFIED WITHOUT TRUSTING IT
 * `"`, `\` and line breaks are recognised by plain NBT matches first. Every other character is
 * then looked up as a quoted key in a class table, `storage ra:safe cls."$(c)"` -- the one macro
 * line, safe exactly because those characters were taken out before it. A character the table does
 * not list (anything outside printable ASCII) reads as class 0, which only the inside of a quoted
 * selector string and FREE accept.
 */
object SafeText {
	val check = RaLib.Safe.check
	val guard = RaLib.Safe.guard
	val init = ref(Ns.LIB, "safe/init")
	private val walk = ref(Ns.LIB, "safe/walk")
	internal val classify = ref(Ns.LIB, "safe/class")
	internal val step = ref(Ns.LIB, "safe/step")
	private val stepSel = ref(Ns.LIB, "safe/step_selector")
	private val selKind = ref(Ns.LIB, "safe/selector_kind")
	private val stepCoords = ref(Ns.LIB, "safe/step_coords")
	private val coordStart = ref(Ns.LIB, "safe/coord_start")

	private const val T = Obj.TEMP

	/**
	 * Character classes. 1 letter, 2 `_ +`, 3 `.`, 4 `-`, 5 `: / #`, 6 space, 7 `@`, 8 `[ {`,
	 * 9 `] }`, 10..19 the digits 0..9, 20 `"` (set by the plain match, not the table), 21 `=`,
	 * 22 `,`, 23 `!`, 24 `~`, 25 `^`, 30 other printable punctuation.
	 */
	private val classes: Map<String, Int> = buildMap {
		('a'..'z').forEach { put(it.toString(), 1) }
		('A'..'Z').forEach { put(it.toString(), 1) }
		('0'..'9').forEach { put(it.toString(), 10 + (it - '0')) }
		put("_", 2); put("+", 2); put(".", 3); put("-", 4)
		put(":", 5); put("/", 5); put("#", 5); put(" ", 6); put("@", 7)
		put("[", 8); put("{", 8); put("]", 9); put("}", 9)
		put("=", 21); put(",", 22); put("!", 23); put("~", 24); put("^", 25)
		"'()*$%&;<>?`|".forEach { put(it.toString(), 30) }
	}

	/**
	 * As a block's marker: stop here ([onFail], `return 0` by default) unless `data.properties.<prop>`
	 * is safe in [format]. Put it before every macro call that pastes the property.
	 */
	fun Function.guardProperty(prop: String, format: TextFormat, onFail: String = "return 0") {
		raw("execute store result score #sf.g $T run function ${guard.asId()} {p:\"$prop\",f:${format.code}}")
		raw("execute unless score #sf.g $T matches 1 run $onFail")
	}

	private fun bad() ="scoreboard players set #sf.bad $T 1"

	fun Ra.registerSafeText() {
		// Configuration, rewritten on every load.
		pack.define(init) {
			raw("data modify storage ra:safe cls set value " + classes.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" })
		}

		pack.define(check) {
			raw("execute unless data storage ra:safe s run return 0")
			// A string, and not a number written out as one: `set string` takes a number too, so
			// the copy is put back over itself and must not change (see Kore Gotchas).
			raw("data remove storage ra:safe w")
			raw("execute store success score #sf.ok $T run data modify storage ra:safe w set string storage ra:safe s")
			raw("execute if score #sf.ok $T matches 0 run return 0")
			raw("execute store success score #sf.ok $T run data modify storage ra:safe w set from storage ra:safe s")
			raw("execute if score #sf.ok $T matches 1 run return 0")
			// No format is no answer: a failed read would store 0, which is FREE.
			raw("execute unless data storage ra:safe f run return 0")
			raw("execute store result score #sf.f $T run data get storage ra:safe f")
			raw("execute if score #sf.f $T matches ${TextFormat.FREE.code} run return 1")
			raw("execute store result score #sf.len $T run data get storage ra:safe s")
			raw("execute if score #sf.len $T matches 257.. run return 0")
			for (s in listOf("i", "q", "d", "end", "tk", "tp", "ti", "tf", "bad")) raw("scoreboard players set #sf.$s $T 0")
			raw("scoreboard players set #sf.ts $T 1")
			function(walk)
			raw("data remove storage ra:safe w")
			raw("data remove storage ra:safe c")
			raw("execute if score #sf.bad $T matches 1 run return 0")

			// What only the end of the string can tell.
			raw("data remove storage ra:safe p")
			raw("execute if score #sf.f $T matches ${TextFormat.TAG.code} run data modify storage ra:safe p set string storage ra:safe s 0 3")
			raw("""execute if score #sf.f $T matches ${TextFormat.TAG.code} if data storage ra:safe {p:"ra."} run return 0""")
			raw("data remove storage ra:safe p")
			val sel = "execute if score #sf.f $T matches ${TextFormat.SELECTOR.code}"
			raw("$sel if score #sf.len $T matches ..1 run return 0")
			raw("$sel unless score #sf.q $T matches 0 run return 0")
			raw("$sel if score #sf.len $T matches 3.. unless score #sf.end $T matches 1 run return 0")
			val co = "execute if score #sf.f $T matches ${TextFormat.COORDS.code}"
			raw("$co unless score #sf.tk $T matches 3 run return 0")
			raw("$co if score #sf.ts $T matches 1 run return 0")
			raw("return 1")
		}

		// One character off the front of ra:safe w, classified into #sf.c and judged by the format.
		pack.define(walk) {
			raw("""execute if data storage ra:safe {w:""} run return 0""")
			raw("data modify storage ra:safe c set string storage ra:safe w 0 1")
			raw("data modify storage ra:safe w set string storage ra:safe w 1")
			raw("scoreboard players set #sf.c $T 0")
			raw("""execute if data storage ra:safe {c:"\\"} run return run ${bad()}""")
			raw("""execute if data storage ra:safe {c:"\n"} run return run ${bad()}""")
			raw("""execute if data storage ra:safe {c:"\r"} run return run ${bad()}""")
			raw("""execute if data storage ra:safe {c:"\t"} run return run ${bad()}""")
			raw("""execute if data storage ra:safe {c:"\""} run scoreboard players set #sf.c $T 20""")
			raw("execute if score #sf.c $T matches 0 run function ${classify.asId()} with storage ra:safe")
			function(step)
			raw("execute if score #sf.bad $T matches 1 run return 0")
			raw("scoreboard players add #sf.i $T 1")
			function(walk)
		}
		// {c}: never `"`, `\` or a line break -- walk matched those first.
		pack.define(classify) {
			raw("\$execute store result score #sf.c $T run data get storage ra:safe cls.\"\$(c)\"")
		}

		pack.define(step) {
			fun allow(f: TextFormat, vararg ranges: String) {
				raw("execute if score #sf.f $T matches ${f.code}" + ranges.joinToString("") { " unless score #sf.c $T matches $it" } + " run ${bad()}")
			}
			allow(TextFormat.ID, "1..5", "10..19")
			allow(TextFormat.NAME, "1..6", "10..19")
			allow(TextFormat.TAG, "1..4", "10..19")
			raw("execute if score #sf.f $T matches ${TextFormat.SELECTOR.code} run function ${stepSel.asId()}")
			raw("execute if score #sf.f $T matches ${TextFormat.COORDS.code} run function ${stepCoords.asId()}")
			raw("execute unless score #sf.f $T matches 1..5 run ${bad()}")
		}

		// @<kind>[ ... ]: #sf.d is the bracket depth, #sf.q 1 inside a double-quoted string,
		// #sf.end 1 once the outer bracket has closed (nothing may follow it).
		pack.define(stepSel) {
			raw("execute if score #sf.end $T matches 1 run return run ${bad()}")
			raw("execute if score #sf.i $T matches 0 unless score #sf.c $T matches 7 run return run ${bad()}")
			raw("execute if score #sf.i $T matches 0 run return 0")
			raw("execute if score #sf.i $T matches 1 run return run function ${selKind.asId()}")
			raw("""execute if score #sf.i $T matches 2 unless data storage ra:safe {c:"["} run return run ${bad()}""")
			raw("execute if score #sf.i $T matches 2 run return run scoreboard players set #sf.d $T 1")
			// Inside quotes anything but `"` (which closes them), `\` and line breaks (refused in walk).
			raw("execute if score #sf.q $T matches 1 if score #sf.c $T matches 20 run return run scoreboard players set #sf.q $T 0")
			raw("execute if score #sf.q $T matches 1 run return 0")
			raw("execute if score #sf.c $T matches 20 run return run scoreboard players set #sf.q $T 1")
			raw("execute if score #sf.c $T matches 8 run return run scoreboard players add #sf.d $T 1")
			raw("execute if score #sf.c $T matches 9 run scoreboard players remove #sf.d $T 1")
			raw("execute if score #sf.c $T matches 9 if score #sf.d $T matches 0 run scoreboard players set #sf.end $T 1")
			raw("execute if score #sf.c $T matches 9 run return 0")
			// Bare characters inside the brackets: letters, digits, `_+.-:/#`, space, `=,!~^`.
			raw("execute if score #sf.c $T matches 1..6 run return 0")
			raw("execute if score #sf.c $T matches 10..19 run return 0")
			raw("execute if score #sf.c $T matches 21..25 run return 0")
			raw(bad())
		}
		pack.define(selKind) {
			for (k in "aenprs") raw("""execute if data storage ra:safe {c:"$k"} run return 0""")
			raw(bad())
		}

		// Three tokens `~`/`^`, optional `-`, at most one whole digit, optional `.` and fraction,
		// separated by one space. #sf.ts 1 while a token is expected, #sf.tk tokens started,
		// #sf.tp position in the token, #sf.ti whole digits, #sf.tf 1 after the point.
		pack.define(stepCoords) {
			raw("execute if score #sf.ts $T matches 1 run return run function ${coordStart.asId()}")
			raw("execute if score #sf.c $T matches 6 run return run scoreboard players set #sf.ts $T 1")
			raw("execute if score #sf.c $T matches 4 unless score #sf.tp $T matches 1 run return run ${bad()}")
			raw("execute if score #sf.c $T matches 3 if score #sf.tf $T matches 1 run return run ${bad()}")
			raw("execute if score #sf.c $T matches 3 run scoreboard players set #sf.tf $T 1")
			raw("execute if score #sf.c $T matches 10..19 if score #sf.tf $T matches 0 run scoreboard players add #sf.ti $T 1")
			raw("execute if score #sf.ti $T matches 2.. run return run ${bad()}")
			raw("execute unless score #sf.c $T matches 3..4 unless score #sf.c $T matches 10..19 run return run ${bad()}")
			raw("scoreboard players add #sf.tp $T 1")
		}
		pack.define(coordStart) {
			raw("execute unless score #sf.c $T matches 24..25 run return run ${bad()}")
			raw("scoreboard players set #sf.ts $T 0")
			raw("scoreboard players add #sf.tk $T 1")
			raw("scoreboard players set #sf.tp $T 1")
			raw("scoreboard players set #sf.ti $T 0")
			raw("scoreboard players set #sf.tf $T 0")
		}

		// {p, f}: as a block's marker. 1 when data.properties.<p> is safe in format f.
		pack.define(guard) {
			raw("data remove storage ra:safe s")
			raw("\$data modify storage ra:safe s set from entity @s data.properties.\$(p)")
			raw("execute unless data storage ra:safe s run return 0")
			// Unchanged since it was last judged: the same answer, without walking it again.
			for ((key, answer) in listOf("safe" to 1, "unsafe" to 0)) {
				raw("data remove storage ra:safe was")
				raw("\$data modify storage ra:safe was set from entity @s data.data.$key.\$(p)")
				raw("execute store success score #sf.chg $T run data modify storage ra:safe was set from storage ra:safe s")
				raw("execute if score #sf.chg $T matches 0 run return $answer")
			}
			raw("data remove storage ra:safe was")
			raw("\$data modify storage ra:safe f set value \$(f)")
			raw("execute store result score #sf.r $T run function ${check.asId()}")
			raw("\$execute if score #sf.r $T matches 1 run data modify entity @s data.data.safe.\$(p) set from storage ra:safe s")
			raw("\$execute if score #sf.r $T matches 1 run data remove entity @s data.data.unsafe.\$(p)")
			raw("\$execute if score #sf.r $T matches 0 run data modify entity @s data.data.unsafe.\$(p) set from storage ra:safe s")
			raw("\$execute if score #sf.r $T matches 0 run data remove entity @s data.data.safe.\$(p)")
			raw("return run scoreboard players get #sf.r $T")
		}
	}
}
