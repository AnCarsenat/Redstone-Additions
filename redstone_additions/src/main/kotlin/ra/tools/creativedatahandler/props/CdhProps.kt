package ra.tools.creativedatahandler.props

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw
import ra.tools.datahandler.DataHandlerSelection
import ra.tools.datahandler.props.DataHandlerTypes.typeOfProbe
import ra.tools.tool

/**
 * The Creative Data Handler's property rows, one per property the block carries: the block type's
 * declared ones in schema order (storage ra:dh block_order, loaded by
 * ra:tools/data_handler/load_schema), then any it carries without a declaration that some other
 * block declares (storage ra:dh known), flagged. Each row has [Modify] -- suggesting the `/data`
 * command, with an opening quote for text -- and [-] to remove it. Hovering the type shows the
 * whole declaration, flags included. Everything is editable here: this is the operator's tool.
 *
 * It used to be one generated function per property name from a hand-kept list of 18 names --
 * properties no block has, and none of the ones most blocks do.
 */
internal object CdhProps {
	private val next = tool("creative_data_handler/props/next")
	private val nextGuess = tool("creative_data_handler/props/next_guess")
	private val render = tool("creative_data_handler/props/render")
	private val renderGuess = tool("creative_data_handler/props/render_guess")
	private val rowQuoted = tool("creative_data_handler/props/row_quoted")
	private val rowPlain = tool("creative_data_handler/props/row_plain")

	/** `{mid}`, as the player: every row. */
	val list = tool("creative_data_handler/props/list")

	private const val T = Obj.TEMP

	/** The player's block by number, `{mid}`: a suggested command still finds it when typed later. */
	private val BY_MID = DataHandlerSelection.byMid("\$(mid)")

	fun Ra.registerCdhProps(removeProperty: FunctionArgument) {
		pack.define(list) {
			raw("data modify storage ra:cdh rest set from storage ra:cdh properties")
			raw("data modify storage ra:cdh row set value {warn:\"\"}")
			raw("\$data modify storage ra:cdh row.mid set value \$(mid)")
			raw("data modify storage ra:cdh iter set from storage ra:dh block_order")
			function(next)
			raw("data modify storage ra:cdh row.warn set value \"⚠ undeclared \"")
			raw("data modify storage ra:cdh iter set from storage ra:dh known")
			function(nextGuess)
			raw("data remove storage ra:cdh iter")
			raw("data remove storage ra:cdh rest")
		}
		for ((walker, fn) in listOf(next to render, nextGuess to renderGuess))
			pack.define(walker) {
				raw("execute unless data storage ra:cdh iter[0] run return 0")
				raw("data modify storage ra:cdh row.name set from storage ra:cdh iter[0]")
				callWith(fn, "storage ra:cdh row")
				raw("data remove storage ra:cdh iter[0]")
				function(walker)
			}
		pack.define(renderGuess) {
			raw("\$execute unless data storage ra:cdh rest.\$(name) run return 0")
			callWith(render, "storage ra:cdh row")
		}

		// {mid,name,warn}: the row, quoted when the property is text -- by declaration, or, with
		// none, by its value.
		pack.define(render) {
			raw("\$data remove storage ra:cdh rest.\$(name)")
			raw("\$execute unless data storage ra:cdh properties.\$(name) run return 0")
			raw("scoreboard players set #cdh.type $T -1")
			raw("\$execute if data storage ra:dh schema.\$(name).code store result score #cdh.type $T run data get storage ra:dh schema.\$(name).code")
			// A direction is a string too; a facing (a compound) and a colour (an int) are written plain.
			for (code in listOf(ra.core.PropType.TEXT.code, ra.core.PropType.ITEM.code, ra.core.PropType.DIRECTION.code))
				raw("execute if score #cdh.type $T matches $code run return run function ${rowQuoted.asId()} with storage ra:cdh row")
			raw("execute unless score #cdh.type $T matches -1 run return run function ${rowPlain.asId()} with storage ra:cdh row")
			raw("data remove storage ra:dh probe")
			raw("\$data modify storage ra:dh probe set from storage ra:cdh properties.\$(name)")
			typeOfProbe("#cdh.type")
			raw("data remove storage ra:dh probe")
			raw("execute if score #cdh.type $T matches 0 run return run function ${rowQuoted.asId()} with storage ra:cdh row")
			callWith(rowPlain, "storage ra:cdh row")
		}

		for ((fn, quoted) in listOf(rowQuoted to true, rowPlain to false))
			pack.define(fn) { raw("\$tellraw @s ${row(quoted, removeProperty)}") }
	}

	private fun row(quoted: Boolean, removeProperty: FunctionArgument): String {
		val open = if (quoted) "\\\"" else ""
		val suggest = """click_event:{action:"suggest_command",command:"/data modify entity $BY_MID data.properties.$(name) set value $open"}"""
		return listOf(
			"""{text:"  $(name): ",color:"white"}""",
			"""{text:"[",color:"dark_gray"}""",
			"""{nbt:"properties.$(name)",storage:"ra:cdh",color:"green"}""",
			"""{text:"]",color:"dark_gray"}""",
			"""{text:" "}""",
			"""{text:"$(warn)",color:"yellow",hover_event:{action:"show_text",value:"No declaration on this block type; the type is guessed."}}""",
			"""{nbt:"schema.$(name).type",storage:"ra:dh",color:"aqua",hover_event:{action:"show_text",value:[{text:"Declaration: ",color:"gray"},{nbt:"schema.$(name)",storage:"ra:dh",color:"white"}]}}""",
			"""{text:" "}""",
			"""{text:"[Modify]",color:"yellow",$suggest,hover_event:{action:"show_text",value:"Edit with /data"}}""",
			"""{text:" "}""",
			"""{text:"[-]",color:"red",click_event:{action:"run_command",command:"/function ${removeProperty.asId()} {name:\"$(name)\"}"},hover_event:{action:"show_text",value:"Remove property"}}""",
		).joinToString(",", "[", "]")
	}
}
