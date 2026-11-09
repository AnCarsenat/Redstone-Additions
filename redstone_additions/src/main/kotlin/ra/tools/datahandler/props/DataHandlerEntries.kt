package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.PropType
import ra.core.Ra
import ra.core.TextFormat
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.DataHandlerRegistry
import ra.tools.datahandler.DataHandlerSelection
import ra.tools.datahandler.props.DataHandlerTypes.typeOfProbe
import ra.tools.tool

/**
 * THE LIST EDITOR: one sub-row per entry of a declared list, each with its own buttons.
 *
 * ```
 *   targets: list (15/15) [Edit all] [List]
 *     Strength 1: B   [Choose] [Edit] [Clear]
 *     Strength 2: (empty)   [Choose] [Edit]
 *
 *   tags: list (1/16) [Add] [Edit all] [List]
 * ```
 *
 * A list row is COMPACT by default, its header alone; [List] redraws the menu with that list's
 * entries underneath (above, `targets`), and again collapses it. One list at a time is open, per
 * player: `ra.dh.expand` holds its row index + 1 (0 or unset: none), and is reset when a block is
 * selected with the tool and on load (a schema may be reordered). [Edit all] writes the whole list
 * in the input book; [Add] types one entry for the end.
 *
 * A LOCKED list (read-only, or admin-only for this player) keeps its locked row, with a [List] of
 * its own: open, its entries are shown without buttons.
 *
 * Buttons, as `menu stamp * MUL + code` like every other row (see DataHandlerActions), with
 * i the row's index in the block's schema (0..29) and j the entry (0..98):
 *
 *   1000 + 300 i + j          [Edit]: type a new value for entry j
 *   1000 + 300 i + 99         [Add]: type a new entry for the end (also on an empty list),
 *                             struck through with "Maximum N entries" once `max_length` is reached
 *   1000 + 300 i + 100 + j    [Remove] (or [Clear] on a padded list) entry j
 *   1000 + 300 i + 199        [List]: show or hide the entries (LIST; no [Remove] is 199, j < 99)
 *   1000 + 300 i + 200 + j    [Choose]: pick entry j from the list the schema names (`choices`)
 *   1000 + 300 i + 299        unused
 *
 * A PADDED list (`pad`, a Teleport Anchor's fifteen strengths) has a fixed length and each slot
 * means something by its position, so its [Remove] is [Clear]: the slot becomes "" and the rest
 * stay where they are. Deleting would shift strength 5's target onto strength 4. It has no
 * [Add] either: it is always full.
 *
 * Every write goes through [commit], which re-reads the block and its schema first (flags, the
 * list's length, `max_length`) and checks the new entry against the element type -- `of`, or with
 * none the type of the entry it replaces -- and, for text, against the format (TextFormat).
 * Typed text reaches a command only through DataHandlerTypes.parse, and only for a number entry.
 *
 * The picker ([Choose]) is a menu of its own, with its own stamp: `ra.dh.mkind` 1 says the
 * player's latest menu is a picker, and its choices are kept, as they were drawn, under
 * `storage ra:dh pick.s<stamp>`, so a value registered after the menu was drawn cannot shift what a
 * button picks.
 */
object DataHandlerEntries {
	val render = tool("data_handler/props/list/editor")
	val renderLocked = tool("data_handler/props/list/locked")
	val entryAction = tool("data_handler/props/list/entry_action")
	val applyEntry = tool("data_handler/props/list/apply_entry")
	val pickAction = tool("data_handler/props/list/pick_action")
	val dropPick = tool("data_handler/props/list/drop_pick")
	val waived = tool("data_handler/props/waived")
	val checkFormat = tool("data_handler/props/check_format")
	val formatHintFor5 = tool("data_handler/props/list/format_refused")

	private val entriesNext = tool("data_handler/props/list/entries_next")
	private val count = tool("data_handler/props/list/count")
	private val readLen = tool("data_handler/props/list/read_len")
	private val headPadded = tool("data_handler/props/list/head_padded")
	private val headAppend = tool("data_handler/props/list/head_append")
	private val headFull = tool("data_handler/props/list/head_full")
	private val headLocked = tool("data_handler/props/list/head_locked")
	private val toggle = tool("data_handler/props/list/toggle")
	private val entryShown = tool("data_handler/props/list/entry_shown")
	private val entryShownEmpty = tool("data_handler/props/list/entry_shown_empty")
	private val entryRow = tool("data_handler/props/list/entry")
	private val entryRowChoose = tool("data_handler/props/list/entry_choose")
	private val entryEmpty = tool("data_handler/props/list/entry_empty")
	private val entryEmptyChoose = tool("data_handler/props/list/entry_empty_choose")
	private val ask = tool("data_handler/props/list/ask")
	private val remove = tool("data_handler/props/list/remove")
	private val elemType = tool("data_handler/props/list/elem_type")
	private val commit = tool("data_handler/props/list/commit")
	private val typeOf = tool("data_handler/props/list/type_of")
	private val writeAt =tool("data_handler/props/list/write_at")
	private val writeAppend = tool("data_handler/props/list/write_append")
	private val pickShow = tool("data_handler/props/list/pick_show")
	private val pickLoad = tool("data_handler/props/list/pick_load")
	private val pickSave = tool("data_handler/props/list/pick_save")
	private val pickNext = tool("data_handler/props/list/pick_next")
	private val pickRow = tool("data_handler/props/list/pick_row")
	private val pickRead = tool("data_handler/props/list/pick_read")
	private val pickElem = tool("data_handler/props/list/pick_elem")
	private val dropPickAt = tool("data_handler/props/list/drop_pick_at")
	private val formatHint = tool("data_handler/props/format_hint")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET
	private const val MUL = DataHandlerRegistry.MUL
	private const val KIND = DataHandlerRegistry.KIND
	private const val PSTAMP = DataHandlerRegistry.PSTAMP
	private const val EXPAND = DataHandlerRegistry.EXPAND

	/** The [List] button's offset in a row's entry action space (see the table above). */
	const val LIST = 199

	/** Entries shown, and so editable one by one, per list; longer lists are edited whole. */
	const val MAX_ENTRIES = 99

	/** Rows of a schema whose lists get the editor (the action space holds 30). */
	const val MAX_ROWS = 30

	private fun btn(label: String, color: String, action: String, hover: String) =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.dh.action set $($action)"},hover_event:{action:"show_text",value:"$hover"}}"""

	private const val HEAD = """{text:"  "},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},{text:": ",color:"white"},{text:"list",color:"aqua"},{text:" ($(count)) ",color:"gray"}"""
	private val WHOLE = btn("[Edit all]", "yellow", "whole", "Write the whole list at once, in the input book")
	/** $(lc) and $(lh): aqua "Show the entries" while the list is compact, gold "Hide the entries" open. */
	private val LIST_BTN = btn("[List]", "$(lc)", "list", "$(lh)")
	private const val SP = """{text:" "}"""

	private const val ENTRY = """{text:"    "},{text:"$(label)$(n)",color:"gray"},{text:": ",color:"dark_gray"}"""
	private const val VALUE = """{nbt:"lw[0]",storage:"ra:dh",color:"green"},{text:"  "}"""
	private const val EMPTY = """{text:"(empty)",color:"dark_gray",italic:true},{text:"  "}"""
	private val CHOOSE = btn("[Choose]", "aqua", "c", "Pick from a list") + """,{text:" "}"""
	private val EDIT = btn("[Edit]", "yellow", "e", "Type a new value for this entry")
	private val REMOVE = """{text:" "},""" + btn("$(rm)", "red", "r", "$(rmh)")

	/** `execute <cond> run tellraw @s <json>`, then `execute <cond> run return 0`: a refusal returns 0, not tellraw's 1. */
	private fun Function.fail(cond: String, json: String, macro: Boolean = false) {
		fun line(l: String) = raw(if (macro && "\$(" in l) "\$$l" else l)
		line("execute $cond run tellraw @s $json")
		line("execute $cond run return 0")
	}

	/** storage ra:dh spec.fmt (TEXT's default when absent) into #dh.fmt and storage ra:safe f. */
	fun Function.loadFormat() {
		raw("scoreboard players set #dh.fmt $T ${TextFormat.ID.code}")
		raw("execute if data storage ra:dh spec.fmt store result score #dh.fmt $T run data get storage ra:dh spec.fmt")
		raw("execute store result storage ra:safe f int 1 run scoreboard players get #dh.fmt $T")
	}

	/** #dh.ri: the row's index in the schema, from #dh.act (base + 100 + index). */
	private fun Function.rowIndex() {
		raw("scoreboard players operation #dh.ri $T = #dh.act $T")
		raw("scoreboard players operation #dh.ri $T -= #dh.base $T")
		raw("scoreboard players remove #dh.ri $T 100")
	}

	/**
	 * For row #dh.ri, with q {name, note, action}: storage ra:dh eq, every field a header and its
	 * entries read; #dh.eb the row's entry actions; #dh.lpad, #dh.chs, #dh.llen, #dh.lmax; and
	 * #dh.xp 1 when this player has the list open.
	 */
	private fun Function.prepare() {
		raw("data modify storage ra:dh eq set value {label:\"#\",rm:\"[Remove]\",rmh:\"Delete this entry; the ones after it move up\",lc:\"aqua\",lh:\"Show the entries\"}")
		raw("data modify storage ra:dh eq.name set from storage ra:dh q.name")
		raw("data modify storage ra:dh eq.note set from storage ra:dh q.note")
		raw("data modify storage ra:dh eq.whole set from storage ra:dh q.action")
		raw("execute if data storage ra:dh spec.entry_label run data modify storage ra:dh eq.label set from storage ra:dh spec.entry_label")
		raw("scoreboard players set #dh.lpad $T 0")
		raw("execute if data storage ra:dh spec.pad run scoreboard players set #dh.lpad $T 1")
		raw("execute if score #dh.lpad $T matches 1 run data modify storage ra:dh eq.rm set value \"[Clear]\"")
		raw("execute if score #dh.lpad $T matches 1 run data modify storage ra:dh eq.rmh set value \"Empty this slot; the others keep their place\"")
		raw("scoreboard players set #dh.chs $T 0")
		raw("execute if data storage ra:dh spec.choices run scoreboard players set #dh.chs $T 1")

		// Entry actions: base + 1000 + 300 * row.
		raw("scoreboard players set #dh.k300 $T 300")
		raw("scoreboard players operation #dh.eb $T = #dh.ri $T")
		raw("scoreboard players operation #dh.eb $T *= #dh.k300 $T")
		raw("scoreboard players operation #dh.eb $T += #dh.base $T")
		raw("scoreboard players add #dh.eb $T 1000")
		for ((key, off) in listOf("append" to 99, "list" to LIST)) {
			raw("scoreboard players operation #dh.ap $T = #dh.eb $T")
			raw("scoreboard players add #dh.ap $T $off")
			raw("execute store result storage ra:dh eq.$key int 1 run scoreboard players get #dh.ap $T")
		}

		// Open? ra.dh.expand is the open list's row + 1.
		raw("scoreboard players operation #dh.xv $T = #dh.ri $T")
		raw("scoreboard players add #dh.xv $T 1")
		raw("execute store success score #dh.xp $T if score @s $EXPAND = #dh.xv $T")
		raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh eq.lc set value \"gold\"")
		raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh eq.lh set value \"Hide the entries\"")

		callWith(readLen, "storage ra:dh q")
		raw("execute store result storage ra:dh eq.len int 1 run scoreboard players get #dh.llen $T")
		raw("execute store result storage ra:dh eq.max int 1 run scoreboard players get #dh.lmax $T")
		raw("data modify storage ra:dh eq.count set value \"\"")
		callWith(count, "storage ra:dh eq")
	}

	/** The open list's entries, a row each (#dh.erw 1 with buttons, 0 without), then the cleanup. A macro line: {name}. */
	private fun Function.walk() {
		raw("\$execute if score #dh.xp $T matches 1 run data modify storage ra:dh lw set from storage ra:dh properties.\$(name)")
		raw("scoreboard players set #dh.j $T 0")
		executeRun("if score #dh.xp $T matches 1", entriesNext)
		raw("data remove storage ra:dh lw")
		raw("data remove storage ra:dh eq")
		raw("data remove storage ra:dh ee")
		raw("data remove storage ra:dh le")
	}

	fun Ra.registerDataHandlerEntries() {
		// {admin_setting}: 1 when that GLOBAL setting is on, waiving an admin-only flag for everyone.
		pack.define(waived) {
			raw("\$execute if data storage ra:settings global{\"\$(admin_setting)\":1b} run return 1")
			raw("return 0")
		}

		// storage ra:safe s against the declared format; with storage ra:dh q {name}. Returns 1
		// when fine, 0 (and says why) otherwise.
		pack.define(checkFormat) {
			loadFormat()
			raw("execute store result score #dh.fok $T run function ${RaLib.Safe.check.asId()}")
			raw("execute if score #dh.fok $T matches 1 run return 1")
			callWith(formatHint, "storage ra:dh q")
			raw("return 0")
		}
		// DataHandlerList.apply's refusal 5: an entry of the whole list is not in the format.
		pack.define(formatHintFor5) {
			raw("execute unless score #dh.bad $T matches 5 run return 0")
			callWith(formatHint, "storage ra:dh q")
		}
		// {name}: what the format allows, for #dh.fmt.
		pack.define(formatHint) {
			for (f in TextFormat.entries)
				raw("""${'$'}execute if score #dh.fmt $T matches ${f.code} run tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" takes ${f.hint}. Nothing was changed.",color:"red"}]""")
		}

		// ── Drawing ──────────────────────────────────────────────────────────────────────────

		// {name, note, action, ...} (the row's q), after probe, for a declared, unlocked list.
		pack.define(render) {
			rowIndex()
			executeReturnRun("unless score #dh.ri $T matches 0..${MAX_ROWS - 1}", PropRows.rowList, "with storage ra:dh q")
			prepare()
			executeRun("if score #dh.lpad $T matches 1", headPadded, "with storage ra:dh eq")
			raw("execute if score #dh.lpad $T matches 0 if score #dh.llen $T < #dh.lmax $T run function ${headAppend.asId()} with storage ra:dh eq")
			raw("execute if score #dh.lpad $T matches 0 unless score #dh.llen $T < #dh.lmax $T run function ${headFull.asId()} with storage ra:dh eq")
			raw("scoreboard players set #dh.erw $T 1")
			walk()
		}
		// The same for a declared list this player may not change (#dh.lock 1..2), after probe and
		// with q.reason set: the locked row, a [List] after it, and the entries without buttons.
		pack.define(renderLocked) {
			rowIndex()
			executeReturnRun("unless score #dh.ri $T matches 0..${MAX_ROWS - 1}", PropRows.rowLocked, "with storage ra:dh q")
			prepare()
			for (k in listOf("list", "lc", "lh")) raw("data modify storage ra:dh q.$k set from storage ra:dh eq.$k")
			callWith(headLocked, "storage ra:dh q")
			raw("scoreboard players set #dh.erw $T 0")
			walk()
		}

		// [List]: as the player, from entryAction after probe, #dh.code the row index. Opens this
		// list, or closes it when it is the open one, and redraws the menu.
		pack.define(toggle) {
			raw("execute if score #dh.lock $T matches 3 run function ${DataHandlerProps.refuse.asId()} with storage ra:dh q")
			raw("execute if score #dh.lock $T matches 3 run return run data remove storage ra:dh denied")
			raw("""execute unless score #dh.type $T matches 3 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a list any more.",color:"red"}]""")
			raw("scoreboard players operation #dh.xv $T = #dh.code $T")
			raw("scoreboard players add #dh.xv $T 1")
			raw("execute store success score #dh.xp $T if score @s $EXPAND = #dh.xv $T")
			raw("execute if score #dh.xp $T matches 1 run scoreboard players set @s $EXPAND 0")
			raw("execute if score #dh.xp $T matches 0 run scoreboard players operation @s $EXPAND = #dh.xv $T")
			function(DataHandler.refresh)
		}
		pack.define(entriesNext) {
			raw("execute unless data storage ra:dh lw[0] run return 0")
			raw("""execute if score #dh.j $T matches $MAX_ENTRIES.. if score #dh.erw $T matches 1 run return run tellraw @s [{text:"    … more entries: use [Edit all].",color:"gray",italic:true}]""")
			raw("""execute if score #dh.j $T matches $MAX_ENTRIES.. run return run tellraw @s [{text:"    … more entries.",color:"gray",italic:true}]""")
			raw("data modify storage ra:dh ee set from storage ra:dh eq")
			raw("scoreboard players operation #dh.n $T = #dh.j $T")
			raw("scoreboard players add #dh.n $T 1")
			raw("execute store result storage ra:dh ee.n int 1 run scoreboard players get #dh.n $T")
			for ((key, off) in listOf("e" to 0, "r" to 100, "c" to 200)) {
				raw("scoreboard players operation #dh.n $T = #dh.eb $T")
				raw("scoreboard players operation #dh.n $T += #dh.j $T")
				raw("scoreboard players add #dh.n $T $off")
				raw("execute store result storage ra:dh ee.$key int 1 run scoreboard players get #dh.n $T")
			}
			raw("data modify storage ra:dh le set from storage ra:dh lw[0]")
			raw("scoreboard players set #dh.emp $T 0")
			raw("""execute if score #dh.lpad $T matches 1 if data storage ra:dh {le:""} run scoreboard players set #dh.emp $T 1""")
			for ((emp, chs, fn) in listOf(Triple(0, 0, entryRow), Triple(0, 1, entryRowChoose), Triple(1, 0, entryEmpty), Triple(1, 1, entryEmptyChoose)))
				raw("execute if score #dh.erw $T matches 1 if score #dh.emp $T matches $emp if score #dh.chs $T matches $chs run function ${fn.asId()} with storage ra:dh ee")
			for ((emp, fn) in listOf(0 to entryShown, 1 to entryShownEmpty))
				raw("execute if score #dh.erw $T matches 0 if score #dh.emp $T matches $emp run function ${fn.asId()} with storage ra:dh ee")
			raw("data remove storage ra:dh lw[0]")
			raw("scoreboard players add #dh.j $T 1")
			function(entriesNext)
		}
		// {len, max}: "3/15", or "3" with no declared maximum.
		pack.define(count) {
			raw("\$data modify storage ra:dh eq.count set value \"\$(len)/\$(max)\"")
			raw("execute if score #dh.lmax $T matches 2147483647 run data modify storage ra:dh eq.count set from storage ra:dh eq.len")
			raw("execute if score #dh.lmax $T matches 2147483647 run data modify storage ra:dh eq.count set string storage ra:dh eq.count")
		}
		// {name}: #dh.llen the list's length now, #dh.lmax its declared maximum (int max without).
		pack.define(readLen) {
			raw("\$execute store result score #dh.llen $T run data get storage ra:dh properties.\$(name)")
			raw("scoreboard players set #dh.lmax $T 2147483647")
			raw("execute if data storage ra:dh spec.max_length store result score #dh.lmax $T run data get storage ra:dh spec.max_length")
		}

		fun Ra.line(fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument, vararg parts: String) =
			pack.define(fn) { raw("\$tellraw @s [" + parts.joinToString(",") + "]") }
		line(headPadded, HEAD, WHOLE, SP, LIST_BTN)
		line(headAppend, HEAD, btn("[Add]", "green", "append", "Add an entry at the end"), SP, WHOLE, SP, LIST_BTN)
		line(headFull, HEAD, """{text:"[Add]",color:"dark_gray",strikethrough:true,hover_event:{action:"show_text",value:"Maximum $(max) entries"}}""", SP, WHOLE, SP, LIST_BTN)
		line(headLocked, *PropRows.LOCKED, SP, LIST_BTN)
		line(entryRow, ENTRY, VALUE, EDIT, REMOVE)
		line(entryRowChoose, ENTRY, VALUE, CHOOSE, EDIT, REMOVE)
		line(entryEmpty, ENTRY, EMPTY, EDIT)
		line(entryEmptyChoose, ENTRY, EMPTY, CHOOSE, EDIT)
		line(entryShown, ENTRY, VALUE)
		line(entryShownEmpty, ENTRY, EMPTY)

		// ── Clicks ───────────────────────────────────────────────────────────────────────────

		// As the player, with #dh.code at 1000.. (see the table above). Every check is made again
		// here: a button is only a /trigger, and a player can type any number.
		pack.define(entryAction) {
			raw("scoreboard players remove #dh.code $T 1000")
			raw("scoreboard players set #dh.k300 $T 300")
			raw("scoreboard players operation #dh.r $T = #dh.code $T")
			raw("scoreboard players operation #dh.r $T %= #dh.k300 $T")
			raw("scoreboard players operation #dh.code $T /= #dh.k300 $T")
			raw("data modify storage ra:dh q set value {list:\"block_order\"}")
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.code $T")
			callWith(DataHandlerProps.pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			raw("data remove storage ra:dh pending_name")
			callWith(DataHandlerActions.propertyGone, "storage ra:dh q")
			raw("execute if score #dh.gone $T matches 1 run return 0")
			callWith(DataHandlerProps.probe, "storage ra:dh q")
			// [List] only shows: a locked list may be opened too.
			executeReturnRun("if score #dh.r $T matches $LIST", toggle)
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("""execute unless score #dh.type $T matches 3 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a list any more.",color:"red"}]""")
			callWith(readLen, "storage ra:dh q")
			raw("scoreboard players set #dh.lpad $T 0")
			raw("execute if data storage ra:dh spec.pad run scoreboard players set #dh.lpad $T 1")

			// Which button: j and what to do with it.
			raw("scoreboard players operation #dh.j $T = #dh.r $T")
			raw("execute if score #dh.r $T matches 100..198 run scoreboard players remove #dh.j $T 100")
			raw("execute if score #dh.r $T matches 200..298 run scoreboard players remove #dh.j $T 200")
			raw("execute if score #dh.r $T matches 99 run scoreboard players set #dh.j $T -1")
			raw("execute store result storage ra:dh q.j int 1 run scoreboard players get #dh.j $T")
			raw("scoreboard players operation #dh.n $T = #dh.j $T")
			raw("scoreboard players add #dh.n $T 1")
			raw("execute if score #dh.r $T matches 299 run return 0")
			// An entry that is not there (the list got shorter since the menu was drawn).
			raw("""execute unless score #dh.r $T matches 99 unless score #dh.j $T < #dh.llen $T run return run tellraw @s [$PREFIX,{text:"That entry no longer exists; here is the list as it is now.",color:"red"}]""")
			raw("""execute if score #dh.r $T matches 99 unless score #dh.llen $T < #dh.lmax $T run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is full.",color:"red"}]""")

			executeReturnRun("if score #dh.r $T matches 100..198", remove, "with storage ra:dh q")
			executeReturnRun("if score #dh.r $T matches 200..298", pickShow, "with storage ra:dh q")
			raw("data modify storage ra:dh pending_kind set value \"entry\"")
			raw("execute if score #dh.r $T matches 99 run data modify storage ra:dh pending_kind set value \"append\"")
			callWith(ask, "storage ra:dh q")
		}

		// {name, j}: open the input book for one entry. The question is filed under this player's
		// request with its index (DataHandlerSelection.pendingSave), so the answer lands where it
		// was asked for even if another entry or block is clicked meanwhile.
		pack.define(ask) {
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_name set from storage ra:dh q.name")
			raw("data modify storage ra:dh pending_j set from storage ra:dh q.j")
			raw("""${'$'}execute if score #dh.j $T matches 0.. run tellraw @s [$PREFIX,{text:"Write the new value of ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" entry ",color:"gray"},{score:{name:"#dh.n",objective:"$T"},color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			raw("""${'$'}execute if score #dh.j $T matches -1 run tellraw @s [$PREFIX,{text:"Write the entry to add to ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}

		// {name, j}: remove entry j, or on a padded list clear it to "".
		pack.define(remove) {
			callWith(DataHandlerProps.materialize, "storage ra:dh q")
			raw("\$execute if score #dh.lpad $T matches 1 run data modify entity $TARGET data.properties.\$(name)[\$(j)] set value \"\"")
			raw("\$execute if score #dh.lpad $T matches 0 run data remove entity $TARGET data.properties.\$(name)[\$(j)]")
			raw("""execute if score #dh.lpad $T matches 1 run tellraw @s [$PREFIX,{text:"Entry cleared.",color:"green"}]""")
			raw("""execute if score #dh.lpad $T matches 0 run tellraw @s [$PREFIX,{text:"Entry removed.",color:"green"}]""")
			function(DataHandler.refresh)
		}

		// A typed answer for an entry. As the player, from DataHandlerActions.genericApply, after
		// probe and refuse, with storage ra:dh q {name, text} and cur {kind, j}. Returns 1 when written.
		pack.define(applyEntry) {
			fail("unless score #dh.type $T matches 3", """[$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a list any more; nothing was changed.",color:"red"}]""")
			raw("data modify storage ra:dh q.j set from storage ra:dh cur.j")
			raw("data modify storage ra:dh q.op set from storage ra:dh cur.kind")
			callWith(readLen, "storage ra:dh q")
			callWith(elemType, "storage ra:dh q")
			raw("data remove storage ra:dh elem")
			// Text stays text, never parsed. A number is parsed (the one parse macro) and checked;
			// a flag is compared against its spellings.
			raw("execute if score #dh.et $T matches 0 run data modify storage ra:dh elem set from storage ra:dh q.text")
			raw("execute if score #dh.et $T matches 4 run data modify storage ra:dh elem set from storage ra:dh q.text")
			raw("execute if score #dh.et $T matches 1 run data remove storage ra:dh parsed")
			raw("execute if score #dh.et $T matches 1 run function ${DataHandlerTypes.parse.asId()} with storage ra:dh q")
			raw("execute if score #dh.et $T matches 1 run data modify storage ra:dh elem set from storage ra:dh parsed")
			raw("data remove storage ra:dh parsed")
			raw("scoreboard players set #dh.bool $T -1")
			for ((v, words) in listOf(1 to DataHandlerTypes.TRUE, 0 to DataHandlerTypes.FALSE))
				for (w in words) raw("""execute if score #dh.et $T matches 2 if data storage ra:dh q{text:"$w"} run scoreboard players set #dh.bool $T $v""")
			raw("execute if score #dh.et $T matches 2 if score #dh.bool $T matches 1 run data modify storage ra:dh elem set value 1b")
			raw("execute if score #dh.et $T matches 2 if score #dh.bool $T matches 0 run data modify storage ra:dh elem set value 0b")
			fail("if score #dh.et $T matches 3", """[$PREFIX,{text:"This entry is itself a list or a compound: use [Edit all].",color:"red"}]""")
			fail("if score #dh.et $T matches 5", """[$PREFIX,{text:"This entry is itself a list or a compound: use [Edit all].",color:"red"}]""")
			executeReturnRun("", commit, "with storage ra:dh q")
		}

		// {name, j}: #dh.et, the type an entry must have: the declared `of`, else the type of the
		// entry being replaced (the last one, for an append), else text.
		pack.define(elemType) {
			raw("scoreboard players set #dh.et $T 0")
			raw("execute if data storage ra:dh spec.of_code store result score #dh.et $T run data get storage ra:dh spec.of_code")
			raw("execute if data storage ra:dh spec.of_code run return 0")
			raw("data remove storage ra:dh probe")
			raw("\$data modify storage ra:dh probe set from storage ra:dh properties.\$(name)[\$(j)]")
			raw("execute unless data storage ra:dh probe run return 0")
			typeOfProbe("#dh.et")
			raw("data remove storage ra:dh probe")
		}

		// {name, j, op}: write storage ra:dh elem into the entry, after checking it. Returns 1.
		pack.define(commit) {
			fail("unless data storage ra:dh elem", """[$PREFIX,{text:"That is not a ",color:"red"},{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" entry. Nothing was changed.",color:"red"}]""")
			// Of the right type? An item id is text. Text is told apart with the string test
			// (DataHandlerList.isText), anything else by its type test.
			raw("scoreboard players operation #dh.et2 $T = #dh.et $T")
			raw("execute if score #dh.et2 $T matches ${PropType.ITEM.code} run scoreboard players set #dh.et2 $T 0")
			raw("scoreboard players set #dh.ft $T -1")
			with(DataHandlerList) { isText("elem", "#dh.isx") }
			raw("execute if score #dh.et2 $T matches 0 if score #dh.isx $T matches 1 run scoreboard players set #dh.ft $T 0")
			raw("data modify storage ra:dh probe set from storage ra:dh elem")
			raw("execute unless score #dh.et2 $T matches 0 run function ${typeOf.asId()}")
			raw("data remove storage ra:dh probe")
			fail("unless score #dh.ft $T = #dh.et2 $T", """[$PREFIX,{text:"That is not a ",color:"red"},{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" entry (wrong type). Nothing was changed.",color:"red"}]""")
			// Text: in the declared format.
			raw("data modify storage ra:safe s set from storage ra:dh elem")
			raw("execute if score #dh.ft $T matches 0 store result score #dh.fok $T run function ${checkFormat.asId()}")
			raw("execute if score #dh.ft $T matches 0 if score #dh.fok $T matches 0 run return 0")
			// Still there, and still room -- on the block, which gets the shown default first if it
			// did not carry the list yet (DataHandlerProps.materialize).
			callWith(DataHandlerProps.materialize, "storage ra:dh q")
			raw("\$execute store result score #dh.llen $T run data get entity $TARGET data.properties.\$(name)")
			fail("""if data storage ra:dh q{op:"append"} unless score #dh.llen $T < #dh.lmax $T""", """[$PREFIX,{text:"$(name)",color:"yellow"},{text:" is full; nothing was changed.",color:"red"}]""", macro = true)
			raw("execute store result score #dh.j $T run data get storage ra:dh q.j")
			fail("""unless data storage ra:dh q{op:"append"} unless score #dh.j $T < #dh.llen $T""", """[$PREFIX,{text:"That entry no longer exists; nothing was changed.",color:"red"}]""")
			executeReturnRun("if data storage ra:dh q{op:\"append\"}", writeAppend, "with storage ra:dh q")
			callWith(writeAt, "storage ra:dh q")
			raw("return 1")
		}
		// storage ra:dh probe's type into #dh.ft (DataHandlerTypes.typeOfProbe).
		pack.define(typeOf) { typeOfProbe("#dh.ft") }
		pack.define(writeAt) {
			raw("\$data modify entity $TARGET data.properties.\$(name)[\$(j)] set from storage ra:dh elem")
			raw("data remove storage ra:dh elem")
		}
		pack.define(writeAppend) {
			raw("\$data modify entity $TARGET data.properties.\$(name) append from storage ra:dh elem")
			raw("data remove storage ra:dh elem")
			raw("return 1")
		}

		// ── The picker ───────────────────────────────────────────────────────────────────────

		// {name, j}: a menu of the schema's `choices`, each a button. As the player.
		pack.define(pickShow) {
			function(dropPick)
			val mid = DataHandlerSelection.MID
			raw("scoreboard players add #dh.stamp $mid 1")
			raw("execute unless score #dh.stamp $mid matches 1..${DataHandlerRegistry.STAMP_MAX} run scoreboard players set #dh.stamp $mid 1")
			raw("scoreboard players operation @s ${DataHandlerRegistry.MENU} = #dh.stamp $mid")
			raw("scoreboard players set @s $KIND 1")
			raw("scoreboard players operation @s $PSTAMP = #dh.stamp $mid")

			raw("data modify storage ra:dh pl set value []")
			callWith(pickLoad, "storage ra:dh spec.choices")
			raw("data modify storage ra:dh pk set value {}")
			raw("data modify storage ra:dh pk.name set from storage ra:dh q.name")
			raw("data modify storage ra:dh pk.j set from storage ra:dh q.j")
			raw("data modify storage ra:dh pk.list set from storage ra:dh pl")
			raw("data modify storage ra:dh pk.label set value \"#\"")
			raw("execute if data storage ra:dh spec.entry_label run data modify storage ra:dh pk.label set from storage ra:dh spec.entry_label")
			raw("execute store result storage ra:dh pk.s int 1 run scoreboard players get #dh.stamp $mid")
			callWith(pickSave, "storage ra:dh pk")

			raw("scoreboard players operation #dh.n $T = #dh.j $T")
			raw("scoreboard players add #dh.n $T 1")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══ ",color:"dark_gray"},{text:"Choose ",color:"aqua",bold:true},{nbt:"pk.name",storage:"ra:dh",color:"yellow"},{text:" ",color:"gray"},{nbt:"pk.label",storage:"ra:dh",color:"gray"},{score:{name:"#dh.n",objective:"$T"},color:"gray"},{text:" ══",color:"dark_gray"}]""")
			raw("""execute unless data storage ra:dh pl[0] run tellraw @s [{text:"  Nothing to choose from yet.",color:"gray",italic:true}]""")
			raw("scoreboard players operation #dh.pa $T = #dh.stamp $mid")
			raw("scoreboard players set #dh.kmul $T $MUL")
			raw("scoreboard players operation #dh.pa $T *= #dh.kmul $T")
			raw("scoreboard players add #dh.pa $T 100")
			raw("scoreboard players set #dh.k $T 0")
			function(pickNext)
			raw("""tellraw @s [{text:"[Back]",color:"gray",click_event:{action:"run_command",command:"/trigger ra.dh.action set 91"},hover_event:{action:"show_text",value:"Back to the block's settings"}}]""")
			raw("data remove storage ra:dh pl")
			raw("data remove storage ra:dh pk")
			raw("data remove storage ra:dh pq2")
		}
		// {storage, path}: the schema's own constants.
		pack.define(pickLoad) { raw("\$data modify storage ra:dh pl set from storage \$(storage) \$(path)") }
		// {s}
		pack.define(pickSave) { raw("\$data modify storage ra:dh pick.s\$(s) set from storage ra:dh pk") }
		pack.define(pickNext) {
			raw("execute unless data storage ra:dh pl[0] run return 0")
			raw("""execute if score #dh.k $T matches 200.. run return run tellraw @s [{text:"  … and more.",color:"gray",italic:true}]""")
			raw("execute store result storage ra:dh pq2.a int 1 run scoreboard players get #dh.pa $T")
			callWith(pickRow, "storage ra:dh pq2")
			raw("data remove storage ra:dh pl[0]")
			raw("scoreboard players add #dh.pa $T 1")
			raw("scoreboard players add #dh.k $T 1")
			function(pickNext)
		}
		pack.define(pickRow) {
			raw("""${'$'}tellraw @s [{text:"  ▸ ",color:"dark_gray"},{nbt:"pl[0]",storage:"ra:dh",color:"aqua",click_event:{action:"run_command",command:"/trigger ra.dh.action set $(a)"},hover_event:{action:"show_text",value:"Choose this"}}]""")
		}

		// A picker's button: as the player, #dh.code 100 + k, #dh.stamp the menu it was in.
		pack.define(pickAction) {
			raw("data remove storage ra:dh pk")
			raw("execute store result storage ra:dh pq2.s int 1 run scoreboard players get #dh.stamp $T")
			raw("scoreboard players remove #dh.code $T 100")
			raw("execute store result storage ra:dh pq2.k int 1 run scoreboard players get #dh.code $T")
			callWith(pickRead, "storage ra:dh pq2")
			raw("""execute unless data storage ra:dh pk.elem run tellraw @s [$PREFIX,{text:"That menu is out of date; here is the current one.",color:"gray"}]""")
			executeReturnRun("unless data storage ra:dh pk.elem", DataHandler.refresh)

			raw("data modify storage ra:dh q set value {op:\"entry\"}")
			raw("data modify storage ra:dh q.name set from storage ra:dh pk.name")
			raw("data modify storage ra:dh q.j set from storage ra:dh pk.j")
			raw("data modify storage ra:dh elem set from storage ra:dh pk.elem")
			callWith(DataHandlerActions.propertyGone, "storage ra:dh q")
			raw("execute if score #dh.gone $T matches 0 run function ${pickElem.asId()}")
			function(dropPick)
			raw("data remove storage ra:dh pk")
			raw("data remove storage ra:dh pq2")
			raw("data remove storage ra:dh elem")
			function(DataHandler.refresh)
		}
		// {s, k}
		pack.define(pickRead) {
			raw("\$data modify storage ra:dh pk set from storage ra:dh pick.s\$(s)")
			raw("\$data modify storage ra:dh pk.elem set from storage ra:dh pk.list[\$(k)]")
		}
		pack.define(pickElem) {
			callWith(DataHandlerProps.probe, "storage ra:dh q")
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("""execute unless score #dh.type $T matches 3 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a list any more.",color:"red"}]""")
			callWith(readLen, "storage ra:dh q")
			callWith(elemType, "storage ra:dh q")
			raw("execute store result score #dh.ok $T run function ${commit.asId()} with storage ra:dh q")
			raw("""execute if score #dh.ok $T matches 1 run tellraw @s [$PREFIX,{text:"Property updated.",color:"green"}]""")
		}

		// Forget this player's last picker (its stored choices). As the player.
		pack.define(dropPick) {
			raw("execute unless score @s $PSTAMP matches 1.. run return 0")
			raw("data modify storage ra:dh pq3 set value {}")
			raw("execute store result storage ra:dh pq3.s int 1 run scoreboard players get @s $PSTAMP")
			callWith(dropPickAt, "storage ra:dh pq3")
			raw("data remove storage ra:dh pq3")
			raw("scoreboard players reset @s $PSTAMP")
		}
		pack.define(dropPickAt) { raw("\$data remove storage ra:dh pick.s\$(s)") }
	}
}
