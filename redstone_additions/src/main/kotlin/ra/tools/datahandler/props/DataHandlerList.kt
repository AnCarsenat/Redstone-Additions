package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandlerSelection.TARGET
import ra.tools.tool

/**
 * A typed list, checked before it reaches the block.
 *
 * The text from the input book is pasted into a command as SNBT -- that is how `["A","B"]` becomes
 * a list. It used to be pasted straight onto the block, so anything that parsed was stored: a
 * compound, a number, a list of the wrong kind. `{a:1}` typed into a Teleport Anchor's `targets`
 * replaced its table, and the anchor's `targets[$(i)]` lookup broke on it. Now the text is parsed
 * (by DataHandlerTypes.parse, the one place typed text meets SNBT) into storage ra:dh list_in, and only a list that passes every check below is copied over:
 *   - it must be a list;
 *   - every entry must be text when the property is declared `list(of = TEXT)`, or, with no
 *     declaration, when it holds a list of text now;
 *   - at most `max` entries when one is declared, padded with "" up to it when declared `pad`
 *     (a Teleport Anchor's `targets`: fifteen rows, "" for no target, as
 *     ra_ender:tools/anchor/set_table does).
 * The declaration is storage ra:dh spec, left there by props/probe just before.
 */
object DataHandlerList {
	/** As the player, with storage ra:dh q = {name, text}. Returns 1 when written, 0 when refused. */
	val apply = tool("data_handler/props/list/apply")

	/** `{name}`: #dh.str = 1 when the property's current first entry is text. */
	private val currentIsText = tool("data_handler/props/list/current_is_text")

	/** Every entry of list_walk is text, or #dh.bad = 3. */
	private val checkText = tool("data_handler/props/list/check_text")

	/** Append "" to list_in until it has #dh.lmax entries. */
	private val pad = tool("data_handler/props/list/pad")

	/** `{name}`: copy list_in onto the block. */
	private val store = tool("data_handler/props/list/store")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val EXAMPLE = """{text:"[\"A\",\"B\"]",color:"yellow"}"""

	fun Ra.registerDataHandlerList() {
		pack.define(apply) {
			// 0 fine, 1 not a list (or unreadable), 3 an entry that is not text, 4 too long.
			raw("scoreboard players set #dh.bad $T 0")
			raw("data remove storage ra:dh list_in")
			// The shared parse (DataHandlerTypes.parse): one line, so a bad text fails only that call.
			raw("data remove storage ra:dh parsed")
			callWith(DataHandlerTypes.parse, "storage ra:dh q")
			raw("data modify storage ra:dh list_in set from storage ra:dh parsed")
			raw("data remove storage ra:dh parsed")
			raw("execute unless data storage ra:dh list_in run scoreboard players set #dh.bad $T 1")

			// `insert` only works on a list, empty or not, of any element type; the probe entry is
			// removed again straight after.
			raw("execute if score #dh.bad $T matches 0 store success score #dh.ok $T run data modify storage ra:dh list_in insert 0 value 0b")
			raw("execute if score #dh.bad $T matches 0 if score #dh.ok $T matches 0 run scoreboard players set #dh.bad $T 1")
			raw("execute if score #dh.bad $T matches 0 run data remove storage ra:dh list_in[0]")

			// Which entries are allowed: text when declared so, or -- undeclared -- when the property
			// holds text now.
			raw("scoreboard players set #dh.str $T 0")
			raw("execute if data storage ra:dh spec.of_code store result score #dh.of $T run data get storage ra:dh spec.of_code")
			raw("execute if data storage ra:dh spec.of_code if score #dh.of $T matches ${ra.core.PropType.TEXT.code} run scoreboard players set #dh.str $T 1")
			executeRun("if score #dh.bad $T matches 0 if score #dh.str $T matches 0 unless data storage ra:dh spec.type", currentIsText, "with storage ra:dh q")
			raw("data modify storage ra:dh list_walk set from storage ra:dh list_in")
			executeRun("if score #dh.bad $T matches 0 if score #dh.str $T matches 1", checkText)
			raw("data remove storage ra:dh list_walk")

			raw("execute store result score #dh.len $T run data get storage ra:dh list_in")
			raw("scoreboard players set #dh.lmax $T 2147483647")
			raw("execute if data storage ra:dh spec.max_length store result score #dh.lmax $T run data get storage ra:dh spec.max_length")
			raw("execute if score #dh.bad $T matches 0 if score #dh.len $T > #dh.lmax $T run scoreboard players set #dh.bad $T 4")

			raw("""execute if score #dh.bad $T matches 1 run tellraw @s [$PREFIX,{text:"That is not a list. Write it like ",color:"red"},$EXAMPLE]""")
			raw("""execute if score #dh.bad $T matches 3 run tellraw @s [$PREFIX,{text:"Every entry must be text in quotes, like ",color:"red"},$EXAMPLE]""")
			raw("""execute if score #dh.bad $T matches 4 run tellraw @s [$PREFIX,{text:"At most ",color:"red"},{score:{name:"#dh.lmax",objective:"$T"},color:"yellow"},{text:" entries; ",color:"red"},{score:{name:"#dh.len",objective:"$T"},color:"yellow"},{text:" were given.",color:"red"}]""")
			raw("execute unless score #dh.bad $T matches 0 run data remove storage ra:dh list_in")
			raw("execute unless score #dh.bad $T matches 0 run return 0")

			executeRun("if data storage ra:dh spec.pad if data storage ra:dh spec.max_length", pad)
			callWith(store, "storage ra:dh q")
			raw("data remove storage ra:dh list_in")
			returnValue(1)
		}

		// {name}: is the property's first entry text?
		pack.define(currentIsText) { isText("properties.\$(name)[0]", "#dh.str", macro = true) }

		pack.define(checkText) {
			raw("execute unless data storage ra:dh list_walk[0] run return 0")
			isText("list_walk[0]", "#dh.ok")
			raw("execute if score #dh.ok $T matches 0 run return run scoreboard players set #dh.bad $T 3")
			raw("data remove storage ra:dh list_walk[0]")
			function(checkText)
		}

		pack.define(pad) {
			raw("execute store result score #dh.len $T run data get storage ra:dh list_in")
			raw("execute if score #dh.len $T >= #dh.lmax $T run return 0")
			raw("data modify storage ra:dh list_in append value \"\"")
			function(pad)
		}

		pack.define(store) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:dh list_in") }
	}

	/**
	 * [into] = 1 when storage ra:dh [path] is a string, else 0.
	 *
	 * `set string` alone cannot tell: it accepts any plain value and writes a number as its digits,
	 * so `[1,2]` would pass as text. The entry is therefore copied back over that copy as well,
	 * which changes nothing only when it was a string already.
	 */
	private fun Function.isText(path: String, into: String, macro: Boolean = false) {
		val m = if (macro) "\$" else ""
		raw("data remove storage ra:dh probe")
		raw("scoreboard players set #dh.chg $T 0")
		raw("${m}execute store success score $into $T run data modify storage ra:dh probe set string storage ra:dh $path")
		raw("${m}execute if score $into $T matches 1 store success score #dh.chg $T run data modify storage ra:dh probe set from storage ra:dh $path")
		raw("execute if score #dh.chg $T matches 1 run scoreboard players set $into $T 0")
		raw("data remove storage ra:dh probe")
	}
}
