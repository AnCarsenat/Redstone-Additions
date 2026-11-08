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
import ra.modules.ender.TeleportAnchor
import ra.tools.datahandler.DataHandlerSelection.TARGET
import ra.tools.tool

/**
 * A typed list, checked before it reaches the block.
 *
 * The text from the input book is pasted into a command as SNBT -- that is how `["A","B"]` becomes
 * a list. It used to be pasted straight onto the block, so anything that parsed was stored: a
 * compound, a number, a list of the wrong kind. `{a:1}` typed into a Teleport Anchor's `targets`
 * replaced its table, and the anchor's `targets[$(i)]` lookup broke on it. Now the text is parsed
 * into storage ra:dh list_in, and only a list that passes every check below is copied over:
 *   - it must be a list;
 *   - when the property holds a list of text now, every entry must be text;
 *   - a Teleport Anchor's `targets` is fifteen text rows: at most fifteen typed, the rest padded
 *     with "" (no target), as ra_ender:tools/anchor/set_table does.
 */
object DataHandlerList {
	/** `{name, text}`: parse the text into storage ra:dh list_in. Only this line, so a parse failure loses nothing else. */
	private val parse = tool("data_handler/props/write_list")

	/** As the player, with storage ra:dh q = {name, text}. Returns 1 when written, 0 when refused. */
	val apply = tool("data_handler/props/list/apply")

	/** `{name}`: #dh.str = 1 when the property's current first entry is text. */
	private val currentIsText = tool("data_handler/props/list/current_is_text")

	/** Every entry of list_walk is text, or #dh.bad = 3. */
	private val checkText = tool("data_handler/props/list/check_text")

	/** Append "" to list_in until it has fifteen entries. */
	private val pad = tool("data_handler/props/list/pad")

	/** `{name}`: copy list_in onto the block. */
	private val store = tool("data_handler/props/list/store")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val EXAMPLE = """{text:"[\"A\",\"B\"]",color:"yellow"}"""

	fun Ra.registerDataHandlerList() {
		pack.define(parse) { raw("\$data modify storage ra:dh list_in set value \$(text)") }

		pack.define(apply) {
			// 0 fine, 1 not a list (or unreadable), 3 an entry that is not text, 4 too long.
			raw("scoreboard players set #dh.bad $T 0")
			raw("data remove storage ra:dh list_in")
			callWith(parse, "storage ra:dh q")
			raw("execute unless data storage ra:dh list_in run scoreboard players set #dh.bad $T 1")

			// `insert` only works on a list, empty or not, of any element type; the probe entry is
			// removed again straight after.
			raw("execute if score #dh.bad $T matches 0 store success score #dh.ok $T run data modify storage ra:dh list_in insert 0 value 0b")
			raw("execute if score #dh.bad $T matches 0 if score #dh.ok $T matches 0 run scoreboard players set #dh.bad $T 1")
			raw("execute if score #dh.bad $T matches 0 run data remove storage ra:dh list_in[0]")

			// Which entries are allowed: text when the property holds text now, and always for an
			// anchor's table.
			raw("scoreboard players set #dh.str $T 0")
			raw("scoreboard players set #dh.anchor $T 0")
			raw("execute if data storage ra:dh q{name:\"targets\"} if entity ${TARGET.dropLast(1)},tag=${TeleportAnchor.block.tag}] run scoreboard players set #dh.anchor $T 1")
			raw("execute if score #dh.anchor $T matches 1 run scoreboard players set #dh.str $T 1")
			executeRun("if score #dh.bad $T matches 0 if score #dh.str $T matches 0", currentIsText, "with storage ra:dh q")
			raw("data modify storage ra:dh list_walk set from storage ra:dh list_in")
			executeRun("if score #dh.bad $T matches 0 if score #dh.str $T matches 1", checkText)
			raw("data remove storage ra:dh list_walk")

			raw("execute store result score #dh.len $T run data get storage ra:dh list_in")
			raw("execute if score #dh.bad $T matches 0 if score #dh.anchor $T matches 1 if score #dh.len $T matches 16.. run scoreboard players set #dh.bad $T 4")

			raw("""execute if score #dh.bad $T matches 1 run tellraw @s [$PREFIX,{text:"That is not a list. Write it like ",color:"red"},$EXAMPLE]""")
			raw("""execute if score #dh.bad $T matches 3 run tellraw @s [$PREFIX,{text:"Every entry must be text in quotes, like ",color:"red"},$EXAMPLE]""")
			raw("""execute if score #dh.bad $T matches 4 run tellraw @s [$PREFIX,{text:"A Teleport Anchor has 15 targets at most; ",color:"red"},{score:{name:"#dh.len",objective:"$T"},color:"yellow"},{text:" were given.",color:"red"}]""")
			raw("execute unless score #dh.bad $T matches 0 run data remove storage ra:dh list_in")
			raw("execute unless score #dh.bad $T matches 0 run return 0")

			executeRun("if score #dh.anchor $T matches 1", pad)
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
			raw("execute if score #dh.len $T matches 15.. run return 0")
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
