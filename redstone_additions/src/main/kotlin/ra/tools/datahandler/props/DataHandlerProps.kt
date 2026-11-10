package ra.tools.datahandler.props

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.core.Admin
import ra.core.Admin.overPlayerMax
import ra.core.Admin.raiseMaxForAdmin
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.Tag
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.props.DataHandlerList.registerDataHandlerList
import ra.tools.datahandler.props.DataHandlerEntries.registerDataHandlerEntries
import ra.core.executeReturnRun
import ra.tools.datahandler.props.DataHandlerTypes.registerDataHandlerTypes
import ra.tools.datahandler.props.DataHandlerTypes.typeOfProbe
import ra.tools.datahandler.props.PropRows.registerPropRows
import ra.tools.datahandler.props.DataHandlerUnique.registerDataHandlerUnique
import ra.tools.tool

/**
 * The Data Handler's per-property machinery: walking the selected block's schema, typing a value,
 * editing it.
 *
 * The block's schema is copied into storage ra:dh by update_storage when its data is read:
 *   schema       types."<type>" -- name -> {type, min, max, readonly, admin_only, visible, ...}
 *   block_order  order."<type>" -- the names, in menu order
 *   inst_types   the marker's own data.property_types, for properties declared dynamic
 */
object DataHandlerProps {
	val next = tool("data_handler/props/next")
	val nextGuess = tool("data_handler/props/next_guess")
	val render = tool("data_handler/props/render")
	val renderGuess = tool("data_handler/props/render_guess")
	val redact = tool("data_handler/props/redact")
	val redactNext = tool("data_handler/props/redact_next")
	val redactOne = tool("data_handler/props/redact_one")
	val probe = tool("data_handler/props/probe")
	val pickName = tool("data_handler/props/pick_name")
	val refuse = tool("data_handler/props/refuse")
	val handAction = tool("data_handler/props/hand_action")
	val setFromHand = tool("data_handler/props/set_from_hand")
	val clearAction = tool("data_handler/props/clear_action")
	val clearItem = tool("data_handler/props/clear_item")

	/** {name}: write the shown default into the block when it does not carry the property yet. */
	val materialize = tool("data_handler/props/materialize")

	/** Row index offsets of the item row's two extra buttons: [Set from hand] and [Clear]. */
	const val HAND_BASE = 400
	const val CLEAR_BASE = 550
	val toggle = tool("data_handler/props/toggle")
	val askNumber = tool("data_handler/props/ask_number")
	val askText = tool("data_handler/props/ask_text")
	val askTypedNumber = tool("data_handler/props/ask_typed_number")
	val writeNumber = tool("data_handler/props/write_number")
	val writeText = tool("data_handler/props/write_text")

	/** #dh.type -> the word a guessed row and the debug line use. */
	private val GUESS = listOf(0 to "text", 1 to "number", 2 to "bool", 3 to "list", 5 to "compound")

	/** #dh.lock -> why the row is locked, for its hover and for the refusal. */
	internal val LOCK_REASON = mapOf(
		1 to "This block sets this itself — it cannot be changed here.",
		2 to Admin.ONLY_REASON,
	)

	internal const val TARGET = ra.tools.datahandler.DataHandlerSelection.TARGET
	internal const val PREFIX = """{text:"[Data Handler] ",color:"gold"}"""

	private const val T = Obj.TEMP

	fun Ra.registerDataHandlerProps() {
		// Render the block's declared properties in order, then the rest. As the player.
		// #dh.act holds the action number of the current row: menu stamp * 1000 + 100 + index.
		pack.define(next) {
			raw("execute unless data storage ra:dh iter[0] run return 0")
			newRow("iter[0]")
			raw("scoreboard players operation #dh.hand $T = #dh.act $T")
			scoreboard.players.add(literal("#dh.hand"), T, 300)
			raw("execute store result storage ra:dh q.hand_action int 1 run scoreboard players get #dh.hand $T")
			scoreboard.players.add(literal("#dh.hand"), T, CLEAR_BASE - HAND_BASE)
			raw("execute store result storage ra:dh q.clear_action int 1 run scoreboard players get #dh.hand $T")
			callWith(render, "storage ra:dh q")
			raw("data remove storage ra:dh iter[0]")
			scoreboard.players.add(literal("#dh.act"), T, 1)
			function(next)
		}

		// The same over `known` (every name some block declares), for the ones this block carries
		// WITHOUT declaring them -- whatever is still in storage ra:dh rest. Action 700 + index.
		pack.define(nextGuess) {
			raw("execute unless data storage ra:dh iter[0] run return 0")
			newRow("iter[0]")
			raw("data modify storage ra:dh q.hand_action set value 0")
			raw("data modify storage ra:dh q.clear_action set value 0")
			callWith(renderGuess, "storage ra:dh q")
			raw("data remove storage ra:dh iter[0]")
			scoreboard.players.add(literal("#dh.act"), T, 1)
			function(nextGuess)
		}
		pack.define(renderGuess) {
			raw("\$execute unless data storage ra:dh rest.\$(name) run return 0")
			callWith(render, "storage ra:dh q")
		}

		// Build storage ra:dh display_props, the summary line: everything the block carries (stored,
		// not the defaults shown for what it lacks), less what this player may not see (visible:0b,
		// for a player without ra.admin).
		pack.define(redact) {
			raw("data remove storage ra:dh display_props")
			raw("data modify storage ra:dh display_props set from storage ra:dh stored")
			raw("execute if entity @s[tag=${ADMIN}] run return 0")
			raw("data modify storage ra:dh iter set from storage ra:dh block_order")
			function(redactNext)
		}
		pack.define(redactNext) {
			raw("execute unless data storage ra:dh iter[0] run return 0")
			raw("data modify storage ra:dh rq set value {}")
			raw("data modify storage ra:dh rq.name set from storage ra:dh iter[0]")
			callWith(redactOne, "storage ra:dh rq")
			raw("data remove storage ra:dh iter[0]")
			function(redactNext)
		}
		pack.define(redactOne) {
			raw("scoreboard players set #dh.vis $T 1")
			raw("\$execute if data storage ra:dh schema.\$(name).visible store result score #dh.vis $T run data get storage ra:dh schema.\$(name).visible")
			raw("\$execute if score #dh.vis $T matches 0 run data remove storage ra:dh display_props.\$(name)")
		}

		// {name}: what is this property, and what may this player do with it?
		//   #dh.type  0 text  1 number  2 bool  3 list  4 item id  5 compound (guessed only)  6 vec3
		//   #dh.decl  1 declared (the block type's schema, or the instance for a dynamic one),
		//             0 guessed from the value
		//   #dh.lock  0 editable  1 read-only  2 admin only, and this player is not an admin
		//             3 hidden from this player
		//   #dh.hid   1 when declared invisible (an admin still sees it, marked)
		//   #dh.min / #dh.max  a declared number's bounds (the max is admin_max for an admin, when
		//             declared); the whole int range otherwise
		//   #dh.ao    1 when only an admin may change it (adminOnly, not waived by its setting)
		// and storage ra:dh spec, the declaration itself (absent when guessed).
		//
		// A declaration always wins over the value: it is the one source an earlier bad write
		// cannot have corrupted -- a delay that became the string "5" is still a number.
		pack.define(probe) {
			for (s in listOf("#dh.type", "#dh.lock", "#dh.hid")) raw("scoreboard players set $s $T 0")
			raw("scoreboard players set #dh.min $T -2147483648")
			raw("scoreboard players set #dh.max $T 2147483647")
			raw("execute store success score #dh.adm $T if entity @s[tag=$ADMIN]")
			data(Storage.DH) { remove("spec") }
			raw("\$data modify storage ra:dh spec set from storage ra:dh schema.\$(name)")
			// Dynamic: the instance carries its own declaration. Missing there, it counts as none.
			raw("\$execute if data storage ra:dh spec.dynamic run data modify storage ra:dh spec set from storage ra:dh inst_types.\$(name)")
			raw("execute store success score #dh.decl $T if data storage ra:dh spec.type")
			// One `data get` per field, into scores: the declaration stores its type as a number
			// (code, PropType.code) and its flags as bytes for exactly this.
			raw("execute if data storage ra:dh spec.code store result score #dh.type $T run data get storage ra:dh spec.code")
			raw("execute if data storage ra:dh spec.min store result score #dh.min $T run data get storage ra:dh spec.min")
			raw("execute if data storage ra:dh spec.max store result score #dh.max $T run data get storage ra:dh spec.max")
			// Admins bypass a range's player maximum, up to its admin_max (ra.core.Admin).
			raiseMaxForAdmin("storage ra:dh spec", "#dh.max", T, "if score #dh.adm $T matches 1")
			raw("scoreboard players set #dh.vis $T 1")
			raw("execute if data storage ra:dh spec.visible store result score #dh.vis $T run data get storage ra:dh spec.visible")
			raw("execute if score #dh.vis $T matches 0 run scoreboard players set #dh.hid $T 1")
			raw("execute store result score #dh.ro $T run data get storage ra:dh spec.readonly")
			raw("execute store result score #dh.ao $T run data get storage ra:dh spec.admin_only")
			raw("execute if score #dh.ro $T matches 1 run scoreboard players set #dh.lock $T 1")
			// Admin-only unless a server setting lets every player edit it (adminOnly(setting = ...)).
			raw("scoreboard players set #dh.wv $T 0")
			raw("execute if score #dh.ao $T matches 1 if data storage ra:dh spec.admin_setting store result score #dh.wv $T run function ${DataHandlerEntries.waived.asId()} with storage ra:dh spec")
			raw("execute if score #dh.wv $T matches 1 run scoreboard players set #dh.ao $T 0")
			raw("execute if score #dh.ao $T matches 1 if score #dh.adm $T matches 0 run scoreboard players set #dh.lock $T 2")
			raw("execute if score #dh.hid $T matches 1 if score #dh.adm $T matches 0 run scoreboard players set #dh.lock $T 3")
			raw("execute if score #dh.decl $T matches 1 run return 1")

			// No declaration: guess from the value, with tests only that type passes (see
			// DataHandlerTypes). `data modify ... set string` is not one: it accepts a number too.
			raw("data remove storage ra:dh probe")
			raw("\$data modify storage ra:dh probe set from storage ra:dh properties.\$(name)")
			typeOfProbe("#dh.type")
			data(Storage.DH) { remove("probe") }
		}

		// {list, i}: resolve a button's index to the property name it stands for.
		pack.define(pickName) {
			data(Storage.DH) { remove("pending_name") }
			raw("\$data modify storage ra:dh pending_name set from storage ra:dh \$(list)[\$(i)]")
		}

		// {name}, after probe: refuse an edit #dh.lock forbids, and say why. Sets storage ra:dh
		// denied when it refuses, which the caller checks. A dead button stops the click, but a row's
		// action is a /trigger and a player can still type one: this is the refusal that holds.
		pack.define(refuse) {
			raw("execute if score #dh.lock $T matches 0 run return 0")
			data(Storage.DH) { modify("denied", net.benwoodworth.knbt.NbtByte(1)) }
			for ((code, reason) in LOCK_REASON)
				raw("""${'$'}execute if score #dh.lock $T matches $code run tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:": $reason",color:"gray"}]""")
			raw("""execute if score #dh.lock $T matches 3 run tellraw @s [$PREFIX,{text:"This block has no such setting.",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
		}

		// A [Set from hand] button: as the player, with #dh.code at 400 + index.
		//
		// The mirror of generic_action for the second action space. Same index lookup, same
		// refusal, and then a straight write rather than an input form -- the answer is in the
		// player's hand.
		pack.define(handAction) {
			raw("data modify storage ra:dh q set value {list:\"block_order\"}")
			scoreboard.players.remove(literal("#dh.code"), T, HAND_BASE)
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.code $T")
			callWith(pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			callWith(probe, "storage ra:dh q")
			callWith(refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("execute unless score #dh.type $T matches 4 run return 0")
			callWith(setFromHand, "storage ra:dh q")
			function(ra.tools.datahandler.DataHandler.refresh)
		}

		// An item row's [Clear], with #dh.code at 550 + index: the same lookup and refusal as
		// [Set from hand], then the item property is written back to "" -- the "no item" value
		// (an Item Pipe with an empty filter_item filters nothing). Before this there was no way
		// back: [Set from hand] needs something in the hand, and the typed form refuses "".
		pack.define(clearAction) {
			raw("data modify storage ra:dh q set value {list:\"block_order\"}")
			scoreboard.players.remove(literal("#dh.code"), T, CLEAR_BASE)
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.code $T")
			callWith(pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			callWith(probe, "storage ra:dh q")
			callWith(refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("execute unless score #dh.type $T matches 4 run return 0")
			callWith(clearItem, "storage ra:dh q")
			function(ra.tools.datahandler.DataHandler.refresh)
		}

		// {name}: empty an item property. As the player.
		pack.define(clearItem) {
			raw("\$data modify storage ra:dh properties.\$(name) set value \"\"")
			raw("\$data modify entity $TARGET data.properties.\$(name) set value \"\"")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" cleared.",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.0")
		}

		// {name}: copy the held item's id into an item property. As the player.
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

		// {name}: a declared property the block lacks is shown with its default (DataHandler.updateStorage);
		// an edit that changes part of the value -- one list entry, a flag -- starts from that default,
		// so it is written to the block first. A whole-value write needs none of this: it creates the key.
		pack.define(materialize) {
			raw("\$execute if data entity $TARGET data.properties.\$(name) run return 0")
			raw("\$execute if data storage ra:dh properties.\$(name) run data modify entity $TARGET data.properties.\$(name) set from storage ra:dh properties.\$(name)")
		}

		// {name}: flip a byte property on the selected block.
		//
		// The old value is latched into a tag before the first write. Testing the property again on
		// the next line would read what that write just stored, so the pair undid itself and
		// [Toggle] appeared to do nothing.
		pack.define(toggle) {
			callWith(materialize, "storage ra:dh q")
			tag(self()) { remove("ra.dh.was_set") }
			raw("\$execute if data entity $TARGET {data:{properties:{\$(name):1b}}} run tag @s add ra.dh.was_set")
			raw("\$execute if entity @s[tag=ra.dh.was_set] run data modify entity $TARGET data.properties.\$(name) set value 0b")
			raw("\$execute unless entity @s[tag=ra.dh.was_set] run data modify entity $TARGET data.properties.\$(name) set value 1b")
			tag(self()) { remove("ra.dh.was_set") }
		}

		// {name}: ask for the new value, then open the matching input form.
		// The prompt itself ("Enter a number for <name> (min–max)") is ra_lib:input's, named by the label.
		pack.define(askNumber) {
			raw("\$data modify storage ra:input label set value \"\$(name)\"")
			function(DataHandlerActions.requestNumber)
		}
		// A number nothing declares: typed in the book, so it may be negative, large or a decimal,
		// and parsed and checked on the way back (DataHandlerTypes.writeTypedNumber).
		pack.define(askTypedNumber) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write a number for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}
		pack.define(askText) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write the new value for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}

		// {name}: the number-form and text writes. The number form's answer is checked against the
		// bounds again (probe, just before): a /reload may have narrowed them since it was asked.
		// Text is copied, never parsed, so a number typed for a text property stays text. A list
		// goes through DataHandlerList, a number or flag typed in the book through DataHandlerTypes;
		// both check the value first.
		pack.define(writeNumber) {
			raw("execute store result score #dh.v $T run data get storage ra:input consume.number")
			raw("""execute unless score #dh.v $T >= #dh.min $T run return run function ${DataHandlerTypes.outOfRange.asId()}""")
			raw("""execute unless score #dh.v $T <= #dh.max $T run return run function ${DataHandlerTypes.outOfRange.asId()}""")
			raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.number")
			raw("return 1")
		}
		pack.define(writeText) { raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:input consume.text") }

		registerRender()
		registerDataHandlerList()
		registerDataHandlerEntries()
		registerDataHandlerTypes()
		registerPropRows()
		registerDataHandlerUnique()
		with(DataHandlerVec3) { registerDataHandlerVec3() }
	}

	/** On a player: server-settings access. */
	internal const val ADMIN = ra.core.Admin.TAG

	/** storage ra:dh q for one row: the name at [from], its action, and every field a row reads. */
	private fun io.github.ayfri.kore.functions.Function.newRow(from: String) {
		raw("data modify storage ra:dh q set value {warn:\"\",guess:\"\",note:\"\",reason:\"\",admin:\"\",admin_why:\"\"}")
		raw("data modify storage ra:dh q.name set from storage ra:dh $from")
		raw("execute store result storage ra:dh q.action int 1 run scoreboard players get #dh.act $T")
	}

	/**
	 * q.admin / q.admin_why for the row, after probe ({name} in q): marked " (admin)" when only an
	 * admin may change the property, or when its value is above the players' maximum (only an
	 * admin could have set it). For every player: an admin sees what is theirs alone, a player
	 * sees why. See ra.core.Admin and PropRows.ADMIN_MARK.
	 */
	private fun io.github.ayfri.kore.functions.Function.adminMark() {
		raw("execute if score #dh.ao $T matches 1 run data modify storage ra:dh q.admin set value \"${Admin.MARK}\"")
		raw("execute if score #dh.ao $T matches 1 run data modify storage ra:dh q.admin_why set value \"${LOCK_REASON.getValue(2)}\"")
		raw("data remove storage ra:dh cur")
		raw("\$data modify storage ra:dh cur set from storage ra:dh properties.\$(name)")
		overPlayerMax("storage ra:dh spec", "storage ra:dh cur", "#dh.aov", T)
		raw("data remove storage ra:dh cur")
		raw("execute if score #dh.aov $T matches 1 run data modify storage ra:dh q.admin set value \"${Admin.MARK}\"")
		raw("execute if score #dh.aov $T matches 1 run data modify storage ra:dh q.admin_why set value \"${Admin.OVER_REASON}\"")
	}

	// {name,action,hand_action,warn,guess,note,reason,admin,admin_why}: one property row, if the selected block
	// has that property. As the player.
	private fun Ra.registerRender() = pack.define(render) {
		raw("\$data remove storage ra:dh rest.\$(name)")
		// Missing from the block: a declared (typed) property still gets its row, with its default
		// or "(not set)"; anything else has nothing to show.
		raw("\$execute unless data storage ra:dh properties.\$(name) unless data storage ra:dh schema.\$(name).type run return 0")
		callWith(probe, "storage ra:dh q")

		// Hidden from this player: no row at all.
		raw("execute if score #dh.lock $T matches 3 run return 0")
		raw("execute if score #dh.hid $T matches 1 run data modify storage ra:dh q.note set value \" (hidden from players)\"")
		adminMark()
		// Not on the block yet: the value shown is the schema's default, written on the first edit --
		// or there is none, and the first edit writes what is typed.
		for ((state, cond) in listOf("default" to "if", "not set" to "unless")) {
			val c = "\$execute unless data storage ra:dh stored.\$(name) $cond data storage ra:dh properties.\$(name)"
			raw("$c run data modify storage ra:dh q.note set value \" ($state)\"")
			raw("$c if score #dh.hid $T matches 1 run data modify storage ra:dh q.note set value \" ($state, hidden from players)\"")
		}

		// No declared type: the row shows its guess behind a yellow warning sign, and debug players
		// are told which property on which block it was.
		raw("execute if score #dh.decl $T matches 0 run data modify storage ra:dh q.warn set value \"⚠ \"")
		for ((code, label) in GUESS)
			raw("execute if score #dh.decl $T matches 0 if score #dh.type $T matches $code run data modify storage ra:dh q.guess set value \"$label\"")
		raw("""execute if score #dh.decl $T matches 0 run tellraw @s[tag=${Tag.DEBUG}] [{text:"[RA debug] ",color:"dark_gray"},{text:"Data Handler: ",color:"gray"},{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" on ",color:"gray"},{nbt:"block_type",storage:"ra:dh",color:"gold"},{text:" has no declared type; guessed ",color:"gray"},{nbt:"q.guess",storage:"ra:dh",color:"aqua"},{text:".",color:"gray"}]""")

		// Locked: read-only for everyone, or admin-only for this player. Shown with a dead button
		// rather than withheld -- a censored row makes a block look like it has fewer settings than
		// it does. The Creative Data Handler edits anything.
		for ((code, reason) in LOCK_REASON)
			raw("execute if score #dh.lock $T matches $code run data modify storage ra:dh q.reason set value \"$reason\"")
		// A declared list keeps its locked row, with a [List] to look at its entries.
		executeReturnRun("if score #dh.lock $T matches 1..2 if score #dh.type $T matches 3 if score #dh.decl $T matches 1", DataHandlerEntries.renderLocked, "with storage ra:dh q")
		raw("execute if score #dh.lock $T matches 1..2 run return run function ${PropRows.rowLocked.asId()} with storage ra:dh q")

		// A declared vec3 gets its editor (DataHandlerVec3).
		executeReturnRun("if score #dh.type $T matches 6 if score #dh.decl $T matches 1", DataHandlerVec3.render, "with storage ra:dh q")
		// A declared list gets the per-entry editor (DataHandlerEntries); a guessed one the whole-list row.
		executeReturnRun("if score #dh.type $T matches 3 if score #dh.decl $T matches 1", DataHandlerEntries.render, "with storage ra:dh q")
		val rows = listOf(1 to PropRows.rowNumber, 2 to PropRows.rowBool, 3 to PropRows.rowList, 0 to PropRows.rowString, 4 to PropRows.rowItem, 5 to PropRows.rowCompound)
		for ((type, row) in rows)
			executeRun("if score #dh.type $T matches $type", row, "with storage ra:dh q")
	}
}
