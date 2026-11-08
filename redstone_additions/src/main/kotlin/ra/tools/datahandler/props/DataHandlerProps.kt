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
import ra.core.Tag
import ra.tools.datahandler.PropType
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.macroCall
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.props.DataHandlerList.registerDataHandlerList
import ra.tools.datahandler.props.DataHandlerTypes.registerDataHandlerTypes
import ra.tools.datahandler.props.DataHandlerTypes.typeOfProbe
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
	val askTypedNumber = tool("data_handler/props/ask_typed_number")
	val writeNumber = tool("data_handler/props/write_number")
	val writeText = tool("data_handler/props/write_text")

	/** #dh.type -> the word a guessed row and the debug line use. */
	private val GUESS = listOf(0 to "text", 1 to "number", 2 to "bool", 3 to "list", 5 to "compound")

	internal const val TARGET = ra.tools.datahandler.DataHandlerSelection.TARGET
	internal const val PREFIX = """{text:"[Data Handler] ",color:"gold"}"""

	fun Ra.registerDataHandlerProps() {
		// Render one registry entry, then the rest. As the player.
		pack.define(next) {
			raw("execute unless data storage ra:dh iter[0] run return 0")
			// warn / guess are filled in by render for a guessed type; every row reads them.
			raw("data modify storage ra:dh q set value {warn:\"\",guess:\"\"}")
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

		// {name}: what type is this property? Result in #dh.type ra.temp:
		//   0 text   1 number   2 bool   3 list   4 item id   5 compound (guessed only)
		// #dh.decl is 1 when the type is declared (storage ra:dh types, from init_registry) and 0
		// when it was guessed from the value. A declaration always wins: it is the one source that
		// an earlier bad write cannot have corrupted -- a delay that somehow became the string "5"
		// is still a number. For a declared number, #dh.min / #dh.max are its bounds (0..9999 when
		// it declares none).
		pack.define(probe) {
			val t = Obj.TEMP
			raw("scoreboard players set #dh.type $t 0")
			raw("scoreboard players set #dh.min $t 0")
			raw("scoreboard players set #dh.max $t 9999")
			raw("\$execute store success score #dh.decl $t if data storage ra:dh types.\"\$(name)\".type")
			for (type in PropType.entries)
				raw("\$execute if data storage ra:dh types.\"\$(name)\"{type:\"${type.id}\"} run scoreboard players set #dh.type $t ${type.code}")
			raw("\$execute if data storage ra:dh types.\"\$(name)\".min store result score #dh.min $t run data get storage ra:dh types.\"\$(name)\".min")
			raw("\$execute if data storage ra:dh types.\"\$(name)\".max store result score #dh.max $t run data get storage ra:dh types.\"\$(name)\".max")
			raw("execute if score #dh.decl $t matches 1 run return 1")

			// No declaration: guess from the value, as DataHandlerTypes describes. Its tests are
			// typed -- `data modify ... set string` is not one, since it accepts a number too.
			raw("data remove storage ra:dh probe")
			raw("\$data modify storage ra:dh probe set from storage ra:dh properties.\$(name)")
			typeOfProbe("#dh.type")
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
		// A number the registry does not declare: typed in the book, so it may be negative, large or
		// a decimal, and parsed and checked on the way back (DataHandlerTypes.writeTypedNumber).
		pack.define(askTypedNumber) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write a number for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}
		pack.define(askText) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write the new value for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}

		// {name}: the number-form and text writes. Text is copied, never parsed, so a number typed
		// for a text property stays text. A list goes through DataHandlerList, a number or flag typed
		// in the book through DataHandlerTypes; both check the value first.
		pack.define(writeNumber) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.number") }
		pack.define(writeText) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.text") }

		registerRender()
		registerDataHandlerList()
		registerDataHandlerTypes()
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

		// No declared type: the row shows its guess behind a yellow warning sign, and debug players
		// are told which property on which block it was. A type is declared in DataHandlerRegistry.
		raw("execute if score #dh.decl ${Obj.TEMP} matches 0 run data modify storage ra:dh q.warn set value \"⚠ \"")
		for ((code, label) in GUESS)
			raw("execute if score #dh.decl ${Obj.TEMP} matches 0 if score #dh.type ${Obj.TEMP} matches $code run data modify storage ra:dh q.guess set value \"$label\"")
		raw("""execute if score #dh.decl ${Obj.TEMP} matches 0 run tellraw @s[tag=${Tag.DEBUG}] [{text:"[RA debug] ",color:"dark_gray"},{text:"Data Handler: ",color:"gray"},{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" on ",color:"gray"},{nbt:"block_type",storage:"ra:dh",color:"gold"},{text:" has no declared type; guessed ",color:"gray"},{nbt:"q.guess",storage:"ra:dh",color:"aqua"},{text:".",color:"gray"}]""")

		val rows = listOf(1 to PropRows.rowNumber, 2 to PropRows.rowBool, 3 to PropRows.rowList, 0 to PropRows.rowString, 5 to PropRows.rowCompound)
		for ((type, row) in rows)
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
