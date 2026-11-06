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

	/** `  name: [value] ` -- the part every row shares; [valueColor] is gray on a locked row. */
	private fun head(valueColor: String = "green") =
		"""{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"$valueColor"},{text:"]",color:"dark_gray"},{text:" "}"""

	private fun button(label: String, hover: String, color: String = "yellow", action: String = "action") =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.dh.action set $($action)"},hover_event:{action:"show_text",value:"$hover"}}"""

	private fun Ra.row(fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument, vararg parts: String) =
		pack.define(fn) { raw("\$tellraw @s [" + parts.joinToString(",") + "]") }

	fun Ra.registerPropRows() {
		row(rowNumber, head(), """{text:"number",color:"aqua"}""", """{text:" "}""", button("[Modify]", "Type a new number"))
		row(rowBool, head(), """{text:"bool",color:"aqua"}""", """{text:" "}""", button("[Toggle]", "Flip this flag"))
		row(rowList, head(), """{text:"list",color:"aqua"}""", """{text:" "}""", button("[Edit list]", "Write the whole list, e.g. [\"A\",\"B\"]"))
		row(rowString, head(), """{text:"string",color:"aqua"}""", """{text:" "}""", button("[Modify]", "Write new text in the input book"))

		// The item_name row -- a property whose value is an item id.
		//
		// Two buttons. [Modify] is the ordinary text form, for typing an id out or for setting one
		// while empty-handed. [Set from hand] is the one people will use: it copies the id of
		// whatever the player is holding, which is the whole reason this type exists. Filters are
		// configured by holding up the thing you want filtered, not by remembering how to spell
		// minecraft:polished_blackstone_slab.
		//
		// Its action is 200 + the registry index (in $(hand_action)), a second action space over the
		// same list, so no per-property handler is needed for it either.
		row(
			rowItem, head(), """{text:"item",color:"aqua"}""", """{text:" "}""",
			button("[Modify]", "Type an item id in the input book"), """{text:" "}""",
			button("[Set from hand]", "Copy the id of the item you are holding", color = "aqua", action = "hand_action"),
		)

		// A field the block does not let a survival player change.
		//
		// Shown, not censored. Hiding a field made a block look like it had fewer settings than it
		// has, and left the player guessing why a number they could see in the Goggles was missing
		// here. The value is printed exactly as any other row; only the button is dead, struck
		// through, and says why on hover.
		row(
			rowLocked, head("gray"), """{text:"locked",color:"dark_gray"}""", """{text:" "}""",
			"""{text:"[Modify]",color:"red",strikethrough:true,hover_event:{action:"show_text",value:"This block sets this itself — it cannot be changed here. Use the Creative Data Handler."}}""",
		)
	}
}
