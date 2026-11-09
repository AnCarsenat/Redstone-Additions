package ra.tools.datahandler.props

import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * One chat row per property type, `{name,action}`: name, current value, type, and a button that
 * runs `/trigger ra.dh.action set <action>` (100 + the registry index).
 */
internal object PropRows {
	val rowNumber = tool("data_handler/props/row_number")
	val rowBool = tool("data_handler/props/row_bool")
	val rowList = tool("data_handler/props/row_list")
	val rowString = tool("data_handler/props/row_string")
	val rowItem = tool("data_handler/props/row_item")
	val rowLocked = tool("data_handler/props/row_locked")
	val rowCompound = tool("data_handler/props/row_compound")

	/**
	 * `  name: [value] ` -- the part every row shares; [valueColor] is gray on a locked row. A row
	 * whose type was guessed rather than declared starts with a yellow `⚠ ` ($(warn), "" otherwise)
	 * that names the guess ($(guess)) on hover. $(note) is " (hidden from players)" on a row only an
	 * admin sees, "" otherwise.
	 */
	private fun head(valueColor: String = "green") =
		"""{text:"  "},{text:"$(warn)",color:"yellow",hover_event:{action:"show_text",value:[{text:"Undeclared type",color:"yellow"},{text:" — guessed $(guess) from the value.\nDeclare it in the block's properties { }.",color:"gray"}]}},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},{text:": ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"$valueColor"},{text:"]",color:"dark_gray"},{text:" "}"""

	private fun button(label: String, hover: String, color: String = "yellow", action: String = "action") =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.dh.action set $($action)"},hover_event:{action:"show_text",value:"$hover"}}"""

	private fun Ra.row(fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument, vararg parts: String) =
		pack.define(fn) { raw("\$tellraw @s [" + parts.joinToString(",") + "]") }

	/** The locked row's parts; a declared list's adds a [List] (DataHandlerEntries.renderLocked). */
	val LOCKED = arrayOf(
		head("gray"), """{text:"locked",color:"dark_gray"}""", """{text:" "}""",
		"""{text:"[Modify]",color:"red",strikethrough:true,hover_event:{action:"show_text",value:"$(reason)"}}""",
	)

	fun Ra.registerPropRows() {
		row(rowNumber, head(), """{text:"number",color:"aqua"}""", """{text:" "}""", button("[Modify]", "Type a new number"))
		row(rowBool, head(), """{text:"bool",color:"aqua"}""", """{text:" "}""", button("[Toggle]", "Flip this flag"))
		row(rowList, head(), """{text:"list",color:"aqua"}""", """{text:" "}""", button("[Edit all]", "Write the whole list, e.g. [\\\"A\\\",\\\"B\\\"]"))
		// (The quotes are escaped for the SNBT string they sit in. Unescaped, they ended the hover
		// text early, the game refused the whole tellraw, and no list row was ever shown.)
		row(rowString, head(), """{text:"string",color:"aqua"}""", """{text:" "}""", button("[Modify]", "Write new text in the input book"))

		// The item_name row -- a property whose value is an item id.
		//
		// Two buttons. [Modify] is the ordinary text form, for typing an id out or for setting one
		// while empty-handed. [Set from hand] is the one people will use: it copies the id of
		// whatever the player is holding, which is the whole reason this type exists. Filters are
		// configured by holding up the thing you want filtered, not by remembering how to spell
		// minecraft:polished_blackstone_slab.
		//
		// Its action is 400 + the row index (in $(hand_action)), a second action space over the
		// same list, so no per-property handler is needed for it either. [Clear] ($(clear_action),
		// 550 + the row index) empties it again.
		row(
			rowItem, head(), """{text:"item",color:"aqua"}""", """{text:" "}""",
			button("[Modify]", "Type an item id in the input book"), """{text:" "}""",
			button("[Set from hand]", "Copy the id of the item you are holding", color = "aqua", action = "hand_action"), """{text:" "}""",
			button("[Clear]", "Set no item", color = "red", action = "clear_action"),
		)

		// A compound: shown, with no editor. There is no safe way to type one in (see DataHandlerList
		// for what a typed list needs checking for), so it is left to the Creative Data Handler.
		// The macro line needs a variable, and $(name) is in head().
		row(
			rowCompound, head(), """{text:"compound",color:"aqua"}""", """{text:" "}""",
			"""{text:"[Modify]",color:"dark_gray",strikethrough:true,hover_event:{action:"show_text",value:"A compound cannot be edited here. Use the Creative Data Handler."}}""",
		)

		// A field this player may not change: read-only, or admin-only and they are not an admin.
		// $(reason) says which, on hover.
		//
		// Shown, not censored. Hiding a field made a block look like it had fewer settings than it
		// has, and left the player guessing why a number they could see in the Goggles was missing
		// here. The value is printed exactly as any other row; only the button is dead, struck
		// through, and says why on hover.
		row(rowLocked, *LOCKED)
	}
}
