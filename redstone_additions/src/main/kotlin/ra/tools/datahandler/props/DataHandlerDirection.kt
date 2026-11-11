package ra.tools.datahandler.props

import ra.core.DIRECTIONS
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.props.PropEditors.act
import ra.tools.datahandler.props.PropEditors.btn
import ra.tools.datahandler.props.PropEditors.line
import ra.tools.datahandler.props.PropEditors.pling
import ra.tools.datahandler.props.PropEditors.rowActions
import ra.tools.tool

/**
 * THE DIRECTION EDITOR: a direction property's row (ra.core.DirectionProps), one line.
 *
 * ```
 *   facing: [south] direction [Down] [Up] [North] [South] [West] [East]
 * ```
 *
 * The current face is gold. Button r (0..5, in [DIRECTIONS] order) writes that face, one of six
 * constants: nothing typed. Its actions are the entry action space's (`1000 + 300 i + r`, see
 * PropEditors); the row's own action (100 + i, typed by hand) only redraws the menu.
 */
object DataHandlerDirection {
	val render = tool("data_handler/props/direction/render")
	val action = tool("data_handler/props/direction/action")
	private val row = tool("data_handler/props/direction/row")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET

	private fun label(face: String) = face.replaceFirstChar { it.uppercase() }

	fun Ra.registerDataHandlerDirection() {
		// {name, note, admin, admin_why, ...}: as the player, after probe, for a declared, unlocked direction.
		pack.define(render) {
			rowActions()
			executeReturnRun("unless score #dh.ri $T matches 0..${DataHandlerEntries.MAX_ROWS - 1}", PropRows.rowString, "with storage ra:dh q")
			raw("data modify storage ra:dh dq set value {}")
			for (k in listOf("name", "note", "admin", "admin_why")) raw("data modify storage ra:dh dq.$k set from storage ra:dh q.$k")
			for ((k, f) in DIRECTIONS.withIndex()) {
				act("dq", "a$k", k)
				raw("data modify storage ra:dh dq.c$k set value \"aqua\"")
				raw("\$execute if data storage ra:dh properties{\$(name):\"$f\"} run data modify storage ra:dh dq.c$k set value \"gold\"")
			}
			callWith(row, "storage ra:dh dq")
			raw("data remove storage ra:dh dq")
		}
		line(
			row, """{text:"  "},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},${PropRows.ADMIN_MARK},{text:": ",color:"white"}""",
			"""{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"},{text:"]",color:"dark_gray"},{text:" direction ",color:"aqua"}""",
			*DIRECTIONS.withIndex().flatMap { (k, f) -> listOf(btn("[${label(f)}]", "$(c$k)", "a$k", "Point it ${if (f == "up" || f == "down") f else "to the $f"}"), PropEditors.SP) }.toTypedArray(),
		)

		// {name}: as the player, from DataHandlerEntries.entryAction after probe: #dh.r the button.
		pack.define(action) {
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("execute unless score #dh.r $T matches 0..${DIRECTIONS.size - 1} run return 0")
			for ((k, f) in DIRECTIONS.withIndex()) {
				raw("\$execute if score #dh.r $T matches $k run data modify entity $TARGET data.properties.\$(name) set value \"$f\"")
				raw("""${'$'}execute if score #dh.r $T matches $k run tellraw @s [$PREFIX,{text:"$(name)",color:"yellow"},{text:" set to ",color:"gray"},{text:"$f",color:"green"}]""")
			}
			pling()
			raw("function ${DataHandler.refresh.asId()}")
		}
	}
}
