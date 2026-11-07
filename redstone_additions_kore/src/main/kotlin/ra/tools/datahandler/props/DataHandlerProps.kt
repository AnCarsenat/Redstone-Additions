package ra.tools.datahandler.props

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import net.benwoodworth.knbt.NbtByte
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.macroCall
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.props.PropRows.registerPropRows
import ra.tools.tool

/** The Data Handler's per-property machinery: walking the registry, typing a value, editing it. */
object DataHandlerProps {
	val next = tool("data_handler/props/next")
	val render = tool("data_handler/props/render")
	val redact = tool("data_handler/props/redact")
	val probe = tool("data_handler/props/probe")
	val pickName = tool("data_handler/props/pick_name")
	val deny = tool("data_handler/props/deny")
	val handAction = tool("data_handler/props/hand_action")
	val setFromHand = tool("data_handler/props/set_from_hand")
	val toggle = tool("data_handler/props/toggle")
	val askNumber = tool("data_handler/props/ask_number")
	val askText = tool("data_handler/props/ask_text")
	val writeNumber = tool("data_handler/props/write_number")
	val writeText = tool("data_handler/props/write_text")
	val writeList = tool("data_handler/props/write_list")

	internal const val TARGET = "@e[type=marker,tag=ra.dh_target,limit=1]"
	internal const val PREFIX = """{text:"[Data Handler] ",color:"gold"}"""

	fun Ra.registerDataHandlerProps() {
		// Render one registry entry, then the rest. As the player.
		pack.define(next) {
			raw("execute unless data storage ra:dh iter[0] run return 0")
			raw("data modify storage ra:dh q set value {}")
			raw("data modify storage ra:dh q.name set from storage ra:dh iter[0]")
			raw("execute store result storage ra:dh q.action int 1 run scoreboard players get #dh.act ${Obj.TEMP}")
			callWith(render, "storage ra:dh q")
			raw("data remove storage ra:dh iter[0]")
			scoreboard.players.add(literal("#dh.act"), Obj.TEMP, 1)
			function(next)
		}

		// Build storage ra:dh display_props: everything the block carries.
		// Nothing is removed. Locked fields are still listed in the Properties summary and still get
		// a row -- see row_locked. This is kept as the one place that decides what the summary shows,
		// so a future rule has somewhere to live.
		pack.define(redact) { raw("data modify storage ra:dh display_props set from storage ra:dh properties") }

		// {name}: what type is this property? Result in #dh.type ra.temp.
		//   0 string   1 number   2 bool   3 list   4 item id
		//
		// There is no "type of" operator, so each answer is a test only that type passes.
		//
		// The obvious test -- does `data get` work on it -- is worthless: `data get` succeeds on a
		// string too, returning its *length*. Reading it as success-means-number classified every
		// string as a number, so the editor asked for a number and wrote one, which is how channels
		// ended up as ints that no string comparison could match.
		pack.define(probe) {
			scoreboard.players.set(literal("#dh.type"), Obj.TEMP, 1)
			// `data modify ... set string` is the real test: it only accepts a string source and
			// fails on anything else.
			raw("\$execute store success score #dh.probe ${Obj.TEMP} run data modify storage ra:dh probe set string storage ra:dh properties.\$(name)")
			raw("execute if score #dh.probe ${Obj.TEMP} matches 1 run scoreboard players set #dh.type ${Obj.TEMP} 0")
			// A list answers to [0]; a string does not, so this cannot catch one by mistake.
			raw("\$execute if data storage ra:dh properties.\$(name)[0] run scoreboard players set #dh.type ${Obj.TEMP} 3")
			// Bytes last: not a string, no [0], so whatever it is has fallen through to number.
			raw("\$execute if data storage ra:dh properties{\$(name):0b} run scoreboard players set #dh.type ${Obj.TEMP} 2")
			raw("\$execute if data storage ra:dh properties{\$(name):1b} run scoreboard players set #dh.type ${Obj.TEMP} 2")
			// A name the registry calls numeric is numeric, whatever is currently sitting in it. This
			// is the only source here that cannot have been corrupted by an earlier bad write; see
			// init_registry for why the list lives there and what is kept off it on purpose.
			raw("\$execute if data storage ra:dh {numeric:[\"\$(name)\"]} run scoreboard players set #dh.type ${Obj.TEMP} 1")
			// An item id last, so it wins over the string it would otherwise probe as. Same reasoning
			// as `numeric`: the registry knows what the field means, the value sitting in it does not.
			raw("\$execute if data storage ra:dh {item_names:[\"\$(name)\"]} run scoreboard players set #dh.type ${Obj.TEMP} 4")
			data(Storage.DH) { remove("probe") }
		}

		// {i}: resolve a row index to the property name it stands for.
		pack.define(pickName) {
			data(Storage.DH) { remove("pending_name") }
			raw("\$data modify storage ra:dh pending_name set from storage ra:dh registry[\$(i)]")
		}

		// {name}: refuse an edit to a field the block keeps to itself. Sets storage ra:dh denied
		// when it refuses, which the caller checks.
		pack.define(deny) {
			raw("\$execute unless data storage ra:dh hidden.\$(name) run return 0")
			data(Storage.DH) { modify("denied", NbtByte(1)) }
			raw("""${'$'}tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" is set by the block and cannot be changed here.",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
		}

		// A [Set from hand] button: as the player, with ra.dh.action at 200 + index.
		//
		// The mirror of generic_action for the second action space. Same index lookup, same refusal
		// for a field the block keeps to itself, and then a straight write rather than an input form
		// -- there is nothing to ask for, the answer is in the player's hand.
		pack.define(handAction) {
			raw("scoreboard players operation #dh.idx ${Obj.TEMP} = @s ra.dh.action")
			scoreboard.players.remove(literal("#dh.idx"), Obj.TEMP, 200)
			raw("data modify storage ra:dh q set value {}")
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.idx ${Obj.TEMP}")
			callWith(pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			function(DataHandler.collectHidden)
			callWith(deny, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			callWith(setFromHand, "storage ra:dh q")
			function(DataHandler.refresh)
		}

		// {name}: copy the held item's id into an item_name property. As the player.
		//
		// SelectedItem is absent when the hand is empty, so the guard is a real one and not
		// politeness -- writing an absent source would leave the old value standing while the player
		// was told it had changed.
		pack.define(setFromHand) {
			raw("""execute unless data entity @s SelectedItem run return run tellraw @s [$PREFIX,{text:"Hold the item you want to filter for, then press it again.",color:"red"}]""")
			raw("data modify storage ra:dh hand set from entity @s SelectedItem.id")
			raw("\$data modify storage ra:dh properties.\$(name) set from storage ra:dh hand")
			raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:dh hand")
			data(Storage.DH) { remove("hand") }
			raw("""${'$'}tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" set to ",color:"gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"}]""")
			raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.4")
		}

		// {name}: flip a byte property on the selected block.
		//
		// The old value is latched into a tag before the first write. Testing the property again on
		// the next line would read what that write just stored, so the pair undid itself and
		// [Toggle] appeared to do nothing.
		pack.define(toggle) {
			tag(self()) { remove("ra.dh.was_set") }
			raw("\$execute if data entity $TARGET {data:{properties:{\$(name):1b}}} run tag @s add ra.dh.was_set")
			raw("\$execute if entity @s[tag=ra.dh.was_set] run data modify entity $TARGET data.properties.\$(name) set value 0b")
			raw("\$execute unless entity @s[tag=ra.dh.was_set] run data modify entity $TARGET data.properties.\$(name) set value 1b")
			tag(self()) { remove("ra.dh.was_set") }
		}

		// {name}: ask for the new value, then open the matching input form.
		pack.define(askNumber) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Enter a new number for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:".",color:"gray"}]""")
			function(DataHandlerActions.requestNumber)
		}
		pack.define(askText) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write the new value for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}

		// {name} / {name,text}: the three writes, one per input kind.
		pack.define(writeNumber) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.number") }
		pack.define(writeText) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.text") }
		pack.define(writeList) { raw("\$data modify entity $TARGET data.properties.\$(name) set value \$(text)") }

		registerRender()
		registerPropRows()
	}

	// {name,action}: one property row, if the selected block has that property. As the player.
	private fun Ra.registerRender() = pack.define(render) {
		raw("\$execute unless data storage ra:dh properties.\$(name) run return 0")

		// Fields the block declared as its own, in ra:tools/readonly/init_registry. Shown with a
		// dead button rather than withheld: a censored row makes a block look like it has fewer
		// settings than it does, and leaves the player wondering why a value the Goggles happily
		// display is missing here.
		//
		// No gamemode test. Creative players use the Creative Data Handler, which shows and edits
		// everything -- so this tool does not need a second mode.
		raw("\$execute if data storage ra:dh hidden.\$(name) run scoreboard players add #dh.shown ${Obj.TEMP} 1")
		raw("\$execute if data storage ra:dh hidden.\$(name) run return run ${macroCall(PropRows.rowLocked, "with storage ra:dh q")}")

		scoreboard.players.add(literal("#dh.shown"), Obj.TEMP, 1)
		callWith(probe, "storage ra:dh q")

		for ((type, row) in listOf(1 to PropRows.rowNumber, 2 to PropRows.rowBool, 3 to PropRows.rowList, 0 to PropRows.rowString))
			executeRun("if score #dh.type ${Obj.TEMP} matches $type", row, "with storage ra:dh q")

		// The item row carries two buttons, and a macro is substituted from one compound, so the
		// second action id is worked out here rather than in the row. 200 + index is the same
		// registry index in a second action space -- see hand_action. On a scratch holder, not on
		// #dh.act: that one is the row iterator and the rest of the list is still counting on it.
		raw("scoreboard players operation #dh.hand ${Obj.TEMP} = #dh.act ${Obj.TEMP}")
		scoreboard.players.add(literal("#dh.hand"), Obj.TEMP, 100)
		raw("execute if score #dh.type ${Obj.TEMP} matches 4 store result storage ra:dh q.hand_action int 1 run scoreboard players get #dh.hand ${Obj.TEMP}")
		executeRun("if score #dh.type ${Obj.TEMP} matches 4", PropRows.rowItem, "with storage ra:dh q")
	}
}
