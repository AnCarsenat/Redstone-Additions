package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.function
import ra.core.ColorSpec
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.lib.safe.SafeClean.isNumberAt
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.props.ColorParse.registerColorParse
import ra.tools.datahandler.props.PropEditors.act
import ra.tools.datahandler.props.PropEditors.btn
import ra.tools.datahandler.props.PropEditors.line
import ra.tools.datahandler.props.PropEditors.pling
import ra.tools.datahandler.props.PropEditors.rowActions
import ra.tools.datahandler.props.PropEditors.toggleEditor
import ra.tools.tool

/**
 * THE COLOUR EDITOR: a color property's row (ra.core.ColorProps), and its buttons.
 *
 * ```
 *   background: [■ #CC101010] color (ARGB) [Edit]
 *     ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■          the sixteen chat colours
 *     Opacity: [100%] [75%] [50%] [25%]       ARGB only
 *     [Type] [None] [Reset]                   [None] when the property has one
 * ```
 *
 * Buttons (r in `1000 + 300 i + r`): 0..15 a chat colour (an ARGB value keeps its alpha, or gets
 * the spec's preset alpha when it was none), 40..43 an opacity, 50 [Type] (the input book,
 * parsed by ColorParse: never pasted), 51 [None], 52 [Reset], 199 [Edit]. The value is the score
 * `#col.v`, written as an int.
 */
object DataHandlerColor {
	val render = tool("data_handler/props/color/render")
	val action = tool("data_handler/props/color/action")
	val openRow = tool("data_handler/props/color/open_row")
	val applyTyped = tool("data_handler/props/color/apply_typed")
	private val head = tool("data_handler/props/color/head")
	private val swatches = tool("data_handler/props/color/swatches")
	private val opacity = tool("data_handler/props/color/opacity")
	private val toolsNone = tool("data_handler/props/color/tools_none")
	private val toolsPlain = tool("data_handler/props/color/tools")
	private val specScores = tool("data_handler/props/color/spec")
	private val load = tool("data_handler/props/color/load")
	private val label = tool("data_handler/props/color/label")
	private val write = tool("data_handler/props/color/write")
	private val ask = tool("data_handler/props/color/ask")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET

	private fun hexOf(rgb: Int) = "#%06X".format(rgb)

	fun Ra.registerDataHandlerColor() {
		// {name, note, admin, admin_why, ...}: as the player, after probe, for a declared, unlocked colour.
		pack.define(render) {
			rowActions()
			executeReturnRun("unless score #dh.ri $T matches 0..${DataHandlerEntries.MAX_ROWS - 1}", PropRows.rowNumber, "with storage ra:dh q")
			raw("data modify storage ra:dh cq set value {lc:\"aqua\",lh:\"Open the editor\",kind:\"RGB\"}")
			for (k in listOf("name", "note", "admin", "admin_why")) raw("data modify storage ra:dh cq.$k set from storage ra:dh q.$k")
			function(specScores)
			raw("execute if score #col.a $T matches 1 run data modify storage ra:dh cq.kind set value \"ARGB\"")
			act("cq", "e", DataHandlerEntries.LIST)
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh cq.lc set value \"gold\"")
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh cq.lh set value \"Close the editor\"")
			callWith(load, "storage ra:dh q")
			function(label)
			callWith(head, "storage ra:dh cq")
			raw("execute if score #dh.xp $T matches 0 run return run data remove storage ra:dh cq")

			for (k in ColorSpec.PRESETS.indices) act("cq", "p$k", k)
			callWith(swatches, "storage ra:dh cq")
			for (k in ColorSpec.OPACITIES.indices) act("cq", "o$k", 40 + k)
			executeRun("if score #col.a $T matches 1", opacity, "with storage ra:dh cq")
			act("cq", "t", 50); act("cq", "n", 51); act("cq", "rs", 52)
			executeRun("if score #col.n $T matches 1", toolsNone, "with storage ra:dh cq")
			executeRun("unless score #col.n $T matches 1", toolsPlain, "with storage ra:dh cq")
			raw("data remove storage ra:dh cq")
		}
		line(
			head, """{text:"  "},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},${PropRows.ADMIN_MARK},{text:": ",color:"white"}""",
			"""{text:"[",color:"dark_gray"},{text:"■ ",color:"$(sw)"},{text:"$(lbl)",color:"green"},{text:"]",color:"dark_gray"}""",
			"""{text:" color",color:"aqua"},{text:" ($(kind)) ",color:"gray"}""",
			btn("[Edit]", "$(lc)", "e", "$(lh)"),
		)
		line(
			swatches, """{text:"    "}""",
			*ColorSpec.PRESETS.withIndex().map { (k, p) -> btn("■", hexOf(p.second), "p$k", p.first.replace('_', ' ')) }.toTypedArray(),
		)
		line(
			opacity, """{text:"    Opacity: ",color:"gray"}""",
			*ColorSpec.OPACITIES.withIndex().flatMap { (k, o) -> listOf(btn("[${o.first}]", "aqua", "o$k", "Opacity ${o.first}"), PropEditors.SP) }.toTypedArray(),
		)
		val type = btn("[Type]", "yellow", "t", "Type a colour in the input book: a name, #RRGGBB or a number")
		val reset = btn("[Reset]", "red", "rs", "Back to the default")
		line(toolsNone, """{text:"    "}""", type, PropEditors.SP, btn("[None]", "gray", "n", "No colour"), PropEditors.SP, reset)
		line(toolsPlain, """{text:"    "}""", type, PropEditors.SP, reset)

		// spec.col into #col.a (ARGB), #col.n (has a none), #col.nv (its value), #col.pa, #col.def.
		pack.define(specScores) {
			raw("execute store result score #col.a $T run data get storage ra:dh spec.col.alpha")
			raw("execute store result score #col.n $T run data get storage ra:dh spec.col.none")
			raw("execute store result score #col.nv $T run data get storage ra:dh spec.col.nv")
			raw("execute store result score #col.pa $T run data get storage ra:dh spec.col.pa")
			raw("execute store result score #col.def $T run data get storage ra:dh spec.col.def")
		}
		// {name}: the value shown (the block's, or the default) into #col.v. Not a number: the default.
		pack.define(load) {
			raw("scoreboard players operation #col.v $T = #col.def $T")
			raw("data remove storage ra:dh cprobe")
			raw("\$data modify storage ra:dh cprobe set from storage ra:dh properties.\$(name)")
			isNumberAt("storage ra:dh cprobe", "#col.isn")
			raw("execute if score #col.isn $T matches 1 store result score #col.v $T run data get storage ra:dh cprobe")
			raw("data remove storage ra:dh cprobe")
		}
		// cq.sw (the swatch's colour) and cq.lbl (what is written) for #col.v: "none" for the none value.
		pack.define(label) {
			raw("scoreboard players operation #col.cur $T = #col.v $T")
			raw("function ${ColorParse.hex.asId()}")
			raw("scoreboard players operation #col.v $T = #col.cur $T")
			raw("data modify storage ra:dh cq.sw set from storage ra:temp col.rgb")
			raw("data modify storage ra:dh cq.lbl set from storage ra:temp col.rgb")
			raw("execute if score #col.a $T matches 1 run data modify storage ra:dh cq.lbl set from storage ra:temp col.argb")
			// Only the none value itself is none: `#00FF0000` is a transparent red, shown as such.
			val none = "if score #col.n $T matches 1 if score #col.v $T = #col.nv $T"
			raw("execute $none run data modify storage ra:dh cq.sw set value \"dark_gray\"")
			raw("execute $none run data modify storage ra:dh cq.lbl set value \"none\"")
		}
		// {name}: #col.v into the selected block, as an int.
		pack.define(write) {
			raw("\$execute store result entity $TARGET data.properties.\$(name) int 1 run scoreboard players get #col.v $T")
			pling()
		}

		// As the player, from DataHandlerEntries.entryAction after probe: #dh.r the button, q {name}.
		pack.define(action) {
			executeReturnRun("if score #dh.r $T matches ${DataHandlerEntries.LIST}", openRow)
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			function(specScores)
			callWith(load, "storage ra:dh q")
			executeReturnRun("if score #dh.r $T matches 50", ask, "with storage ra:dh q")
			raw("scoreboard players operation #col.cur $T = #col.v $T")
			raw("function ${ColorParse.hex.asId()}")
			// A chat colour: RGB as it is; ARGB keeps its alpha, or gets the preset alpha when none.
			for ((k, p) in ColorSpec.PRESETS.withIndex()) raw("execute if score #dh.r $T matches $k run scoreboard players set #col.rgb $T ${p.second}")
			raw("execute if score #dh.r $T matches 0..15 if score #col.n $T matches 1 if score #col.cur $T = #col.nv $T run scoreboard players operation #col.al $T = #col.pa $T")
			// An opacity: ARGB only, the colour kept (black when it was none).
			raw("""execute if score #dh.r $T matches 40..43 if score #col.a $T matches 0 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" has no opacity.",color:"red"}]""")
			for ((k, o) in ColorSpec.OPACITIES.withIndex()) raw("execute if score #dh.r $T matches ${40 + k} run scoreboard players set #col.al $T ${o.second}")
			raw("execute if score #dh.r $T matches 0..15 if score #col.a $T matches 0 run scoreboard players operation #col.v $T = #col.rgb $T")
			raw("execute if score #dh.r $T matches 0..15 if score #col.a $T matches 1 run function ${ColorParse.compose.asId()}")
			raw("execute if score #dh.r $T matches 40..43 run function ${ColorParse.compose.asId()}")
			raw("""execute if score #dh.r $T matches 51 if score #col.n $T matches 0 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" always has a colour.",color:"red"}]""")
			raw("execute if score #dh.r $T matches 51 run scoreboard players operation #col.v $T = #col.nv $T")
			raw("execute if score #dh.r $T matches 52 run scoreboard players operation #col.v $T = #col.def $T")
			raw("execute unless score #dh.r $T matches 0..15 unless score #dh.r $T matches 40..43 unless score #dh.r $T matches 51..52 run return 0")
			callWith(write, "storage ra:dh q")
			function(DataHandler.refresh)
		}
		pack.define(openRow) { toggleEditor(DataHandler.refresh) }

		// {name}: the input book. The answer comes back through DataHandlerActions.genericApply.
		pack.define(ask) {
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_name set from storage ra:dh q.name")
			raw("data modify storage ra:dh pending_kind set value \"color\"")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Write the colour for ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" in the input book: a name (red, gold...), #RRGGBB, #AARRGGBB with its opacity, none, or a number.",color:"gray"}]""")
			function(DataHandlerActions.requestText)
		}

		// A typed colour. As the player, from genericApply after probe and refuse, with q {name, text},
		// cur {kind:"color"}. Returns 1 when written.
		pack.define(applyTyped) {
			raw("""execute unless score #dh.type $T matches 9 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a colour any more; nothing was changed.",color:"red"}]""")
			raw("execute unless score #dh.type $T matches 9 run return 0")
			function(specScores)
			callWith(load, "storage ra:dh q")
			raw("scoreboard players operation #col.cur $T = #col.v $T")
			raw("data modify storage ra:temp col set value {}")
			raw("data modify storage ra:temp col.s set from storage ra:dh q.text")
			raw("execute store result score #col.ok $T run function ${ColorParse.parse.asId()}")
			raw("data remove storage ra:temp col")
			raw("""execute if score #col.ok $T matches 0 run tellraw @s [$PREFIX,{nbt:"q.text",storage:"ra:dh",color:"white"},{text:" is not a colour here: write a name (red, gold...), #RRGGBB, #AARRGGBB (with opacity), none, or a number. Nothing was changed.",color:"red"}]""")
			raw("execute if score #col.ok $T matches 0 run return run function ${DataHandlerProps.refusedSound.asId()}")
			// An RGB colour is 0..0xFFFFFF, or its none.
			raw("scoreboard players set #col.rok $T 1")
			raw("execute if score #col.a $T matches 0 unless score #col.v $T matches 0..${0xFFFFFF} run scoreboard players set #col.rok $T 0")
			raw("execute if score #col.a $T matches 0 if score #col.n $T matches 1 if score #col.v $T = #col.nv $T run scoreboard players set #col.rok $T 1")
			raw("""execute if score #col.rok $T matches 0 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" takes a colour from 0 to 16777215 (#FFFFFF). Nothing was changed.",color:"red"}]""")
			raw("execute if score #col.rok $T matches 0 run return run function ${DataHandlerProps.refusedSound.asId()}")
			callWith(write, "storage ra:dh q")
			raw("return 1")
		}

		registerColorParse()
	}
}
