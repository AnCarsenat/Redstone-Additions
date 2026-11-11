package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.FacingSpec
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.props.PropEditors.act
import ra.tools.datahandler.props.PropEditors.btn
import ra.tools.datahandler.props.PropEditors.line
import ra.tools.datahandler.props.PropEditors.pling
import ra.tools.datahandler.props.PropEditors.rowActions
import ra.tools.datahandler.props.PropEditors.toggleEditor
import ra.tools.tool

/**
 * THE FACING EDITOR: a facing property's row (yaw and pitch, ra.core.DirectionProps), and its buttons.
 *
 * ```
 *   rotation: [yaw 0, pitch 0] facing (degrees, from the block's front) [Edit]
 *     Yaw: 0  [Type] [-] [+]  (-180 to 180)
 *     Pitch: 0  [Type] [-] [+]  (-90 to 90)
 *     Step: [1°] [15°] [45°]
 *     [North] [East] [South] [West]      (a WORLD facing only)
 *     [Reset]
 * ```
 *
 * Compact until [Edit] opens it, like a vec3. Buttons (r in `1000 + 300 i + r`): 0..1 [Type]
 * yaw / pitch (the number form, bounded by the range), 10..13 [-] [+] yaw then pitch by the chosen
 * step (yaw wraps round on a full turn, pitch stops at its ends), 20..23 a step (per player,
 * `ra.dh.v3s`, shared with the vec3 editor), 30..33 a compass facing, 40 [Reset], 199 [Edit].
 * Every value is two scores, `#fc.y` and `#fc.p`; checked against the range and the grid before
 * it is written, as two integers.
 */
object DataHandlerFacing {
	val render = tool("data_handler/props/facing/render")
	val action = tool("data_handler/props/facing/action")
	val openRow = tool("data_handler/props/facing/open_row")
	val applyTyped = tool("data_handler/props/facing/apply_typed")
	private val head = tool("data_handler/props/facing/head")
	private fun axisRow(i: Int) = tool("data_handler/props/facing/axis_$i")
	private fun stepRow(n: Int) = tool("data_handler/props/facing/steps_$n")
	private val compassRow = tool("data_handler/props/facing/compass")
	private val resetRow = tool("data_handler/props/facing/reset_row")
	private val bounds = tool("data_handler/props/facing/bounds")
	private val load = tool("data_handler/props/facing/load")
	private val check = tool("data_handler/props/facing/check")
	private val write = tool("data_handler/props/facing/write")
	private val ask = tool("data_handler/props/facing/ask")
	private val nudge = tool("data_handler/props/facing/nudge")

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET
	private const val STEP = DataHandlerVec3.STEP
	private val PARTS = listOf("yaw" to "y", "pitch" to "p")

	/** #fc.st the player's chosen step, after bounds (the first when theirs is not one this property has). */
	private fun Function.chosenStep() {
		raw("execute unless score @s $STEP matches 0..${FacingSpec.MAX_STEPS - 1} run scoreboard players set @s $STEP 0")
		raw("execute store result score #fc.n $T run data get storage ra:dh spec.fc.steps")
		raw("execute unless score @s $STEP < #fc.n $T run scoreboard players set @s $STEP 0")
		raw("scoreboard players set #fc.st $T 1")
		for (k in 0 until FacingSpec.MAX_STEPS)
			raw("execute if score @s $STEP matches $k store result score #fc.st $T run data get storage ra:dh spec.fc.steps[$k]")
	}

	fun Ra.registerDataHandlerFacing() {
		// {name, note, admin, admin_why, ...}: as the player, after probe, for a declared, unlocked facing.
		pack.define(render) {
			rowActions()
			executeReturnRun("unless score #dh.ri $T matches 0..${DataHandlerEntries.MAX_ROWS - 1}", PropRows.rowCompound, "with storage ra:dh q")
			raw("data modify storage ra:dh fq set value {lc:\"aqua\",lh:\"Open the editor\"}")
			for (k in listOf("name", "note", "admin", "admin_why")) raw("data modify storage ra:dh fq.$k set from storage ra:dh q.$k")
			raw("data modify storage ra:dh fq.frame_label set from storage ra:dh spec.fc.frame_label")
			act("fq", "e", DataHandlerEntries.LIST)
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh fq.lc set value \"gold\"")
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh fq.lh set value \"Close the editor\"")
			function(bounds)
			callWith(load, "storage ra:dh q")
			callWith(head, "storage ra:dh fq")
			raw("execute if score #dh.xp $T matches 0 run return run data remove storage ra:dh fq")

			for (i in 0..1) {
				act("fq", "t$i", i); act("fq", "m$i", 10 + 2 * i); act("fq", "p$i", 11 + 2 * i)
				callWith(axisRow(i), "storage ra:dh fq")
			}
			chosenStep()
			for (k in 0 until FacingSpec.MAX_STEPS) {
				act("fq", "s$k", 20 + k)
				raw("data modify storage ra:dh fq.l$k set from storage ra:dh spec.fc.labels[$k]")
				raw("data modify storage ra:dh fq.c$k set value \"aqua\"")
				raw("execute if score @s $STEP matches $k run data modify storage ra:dh fq.c$k set value \"gold\"")
			}
			for (n in 1..FacingSpec.MAX_STEPS) executeRun("if score #fc.n $T matches $n", stepRow(n), "with storage ra:dh fq")
			for (k in FacingSpec.COMPASS.indices) act("fq", "k$k", 30 + k)
			executeRun("if score #fc.f $T matches 1", compassRow, "with storage ra:dh fq")
			act("fq", "rs", 40)
			callWith(resetRow, "storage ra:dh fq")
			raw("data remove storage ra:dh fq")
		}

		val value = """{text:"yaw ",color:"gray"},{score:{name:"#fc.y",objective:"$T"},color:"green"},{text:", ",color:"dark_gray"},{text:"pitch ",color:"gray"},{score:{name:"#fc.p",objective:"$T"},color:"green"}"""
		line(
			head, """{text:"  "},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},${PropRows.ADMIN_MARK},{text:": ",color:"white"}""",
			"""{text:"[",color:"dark_gray"},$value,{text:"]",color:"dark_gray"}""",
			"""{text:" facing",color:"aqua"},{text:" (degrees, $(frame_label)) ",color:"gray"}""",
			btn("[Edit]", "$(lc)", "e", "$(lh)"),
		)
		for ((i, pair) in PARTS.withIndex()) {
			val (part, s) = pair
			val label = part.replaceFirstChar { it.uppercase() }
			line(
				axisRow(i), """{text:"    $label: ",color:"gray"},{score:{name:"#fc.$s",objective:"$T"},color:"green"},{text:"  "}""",
				btn("[Type]", "yellow", "t$i", "Type the $part in degrees"), PropEditors.SP,
				btn("[-]", "red", "m$i", "Less $part by the chosen step"), PropEditors.SP,
				btn("[+]", "green", "p$i", "More $part by the chosen step"),
				"""{text:"  (",color:"dark_gray"},{score:{name:"#fc.lo$i",objective:"$T"},color:"gray"},{text:" to ",color:"dark_gray"},{score:{name:"#fc.hi$i",objective:"$T"},color:"gray"},{text:")",color:"dark_gray"}""",
			)
		}
		for (n in 1..FacingSpec.MAX_STEPS) line(
			stepRow(n), """{text:"    Step: ",color:"gray"}""",
			*(0 until n).flatMap { k -> listOf(btn("[$(l$k)]", "$(c$k)", "s$k", "Nudge by $(l$k)"), PropEditors.SP) }.toTypedArray(),
		)
		line(
			compassRow, """{text:"    "}""",
			*FacingSpec.COMPASS.withIndex().flatMap { (k, c) -> listOf(btn("[${c.first}]", "aqua", "k$k", "Face ${c.first.lowercase()}, level"), PropEditors.SP) }.toTypedArray(),
		)
		line(resetRow, """{text:"    "}""", btn("[Reset]", "red", "rs", "Back to the default"))

		// The range (#fc.lo0/1, #fc.hi0/1), grid (#fc.g), wrap (#fc.w) and frame (#fc.f) from spec.fc.
		pack.define(bounds) {
			for (i in 0..1) {
				raw("execute store result score #fc.lo$i $T run data get storage ra:dh spec.fc.min[$i]")
				raw("execute store result score #fc.hi$i $T run data get storage ra:dh spec.fc.max[$i]")
			}
			raw("execute store result score #fc.g $T run data get storage ra:dh spec.fc.grid")
			raw("execute unless score #fc.g $T matches 1.. run scoreboard players set #fc.g $T 1")
			raw("execute store result score #fc.w $T run data get storage ra:dh spec.fc.wrap")
			raw("execute store result score #fc.f $T run data get storage ra:dh spec.fc.frame")
		}
		// {name}: the value shown (the block's, or the default) into #fc.y / #fc.p. A value without
		// both parts counts as the declared default.
		pack.define(load) {
			raw("\$execute store success score #fc.has $T if data storage ra:dh properties.\$(name).yaw if data storage ra:dh properties.\$(name).pitch")
			for ((part, s) in PARTS) {
				raw("execute store result score #fc.$s $T run data get storage ra:dh spec.fc.def.$part")
				raw("\$execute if score #fc.has $T matches 1 store result score #fc.$s $T run data get storage ra:dh properties.\$(name).$part")
			}
		}
		// #fc.y / #fc.p against the range and the grid, after bounds. 1 when fine; 0 with the reason.
		pack.define(check) {
			for ((i, pair) in PARTS.withIndex()) {
				val (part, s) = pair
				val why = """[$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" $part must be from ",color:"red"},{score:{name:"#fc.lo$i",objective:"$T"},color:"yellow"},{text:" to ",color:"red"},{score:{name:"#fc.hi$i",objective:"$T"},color:"yellow"},{text:". Nothing was changed.",color:"red"}]"""
				for (cond in listOf("unless score #fc.$s $T >= #fc.lo$i $T", "unless score #fc.$s $T <= #fc.hi$i $T")) {
					raw("execute $cond run tellraw @s $why")
					raw("execute $cond run return run function ${DataHandlerProps.refusedSound.asId()}")
				}
				raw("scoreboard players operation #fc.r $T = #fc.$s $T")
				raw("scoreboard players operation #fc.r $T %= #fc.g $T")
				raw("""execute unless score #fc.r $T matches 0 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" takes multiples of ",color:"red"},{score:{name:"#fc.g",objective:"$T"},color:"yellow"},{text:" degrees only. Nothing was changed.",color:"red"}]""")
				raw("execute unless score #fc.r $T matches 0 run return run function ${DataHandlerProps.refusedSound.asId()}")
			}
			raw("return 1")
		}
		// {name}: #fc.y / #fc.p into the selected block, as two integers.
		pack.define(write) {
			raw("\$data modify entity $TARGET data.properties.\$(name) set value {yaw:0,pitch:0}")
			for ((part, s) in PARTS) raw("\$execute store result entity $TARGET data.properties.\$(name).$part int 1 run scoreboard players get #fc.$s $T")
			pling()
		}

		// As the player, from DataHandlerEntries.entryAction after probe: #dh.r the button, #dh.code the row, q {name}.
		pack.define(action) {
			executeReturnRun("if score #dh.r $T matches ${DataHandlerEntries.LIST}", openRow)
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			function(bounds)
			// A step: only this player's choice, the block is not touched.
			chosenStep()
			raw("scoreboard players operation #fc.k $T = #dh.r $T")
			raw("scoreboard players remove #fc.k $T 20")
			raw("execute if score #dh.r $T matches 20..23 if score #fc.k $T < #fc.n $T run scoreboard players operation @s $STEP = #fc.k $T")
			executeReturnRun("if score #dh.r $T matches 20..23", DataHandler.refresh)
			callWith(load, "storage ra:dh q")
			raw("execute if score #dh.r $T matches 0..1 store result storage ra:dh q.j int 1 run scoreboard players get #dh.r $T")
			executeReturnRun("if score #dh.r $T matches 0..1", ask, "with storage ra:dh q")
			executeRun("if score #dh.r $T matches 10..13", nudge)
			// A compass facing: a WORLD facing only.
			raw("""execute if score #dh.r $T matches 30..33 unless score #fc.f $T matches 1 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" turns from the block's front: it has no compass facing.",color:"red"}]""")
			for ((k, c) in FacingSpec.COMPASS.withIndex()) {
				raw("execute if score #dh.r $T matches ${30 + k} run scoreboard players set #fc.y $T ${c.second}")
				raw("execute if score #dh.r $T matches ${30 + k} run scoreboard players set #fc.p $T 0")
			}
			for ((part, s) in PARTS) raw("execute if score #dh.r $T matches 40 store result score #fc.$s $T run data get storage ra:dh spec.fc.def.$part")
			raw("execute unless score #dh.r $T matches 10..13 unless score #dh.r $T matches 30..33 unless score #dh.r $T matches 40 run return 0")
			raw("execute store result score #fc.ok $T run function ${check.asId()}")
			raw("execute if score #fc.ok $T matches 0 run return 0")
			callWith(write, "storage ra:dh q")
			function(DataHandler.refresh)
		}
		// [Edit], and the row's own action (100 + i, typed by hand): open or close the editor.
		pack.define(openRow) { toggleEditor(DataHandler.refresh) }

		// [-] [+]: #dh.r 10..13, by the chosen step. A full turn of yaw wraps round; pitch stops at its ends.
		pack.define(nudge) {
			for ((i, pair) in PARTS.withIndex()) {
				val s = pair.second
				raw("execute if score #dh.r $T matches ${10 + 2 * i} run scoreboard players operation #fc.$s $T -= #fc.st $T")
				raw("execute if score #dh.r $T matches ${11 + 2 * i} run scoreboard players operation #fc.$s $T += #fc.st $T")
				val moved = "if score #dh.r $T matches ${10 + 2 * i}..${11 + 2 * i}"
				if (i == 0) {
					raw("execute $moved if score #fc.w $T matches 1 if score #fc.y $T > #fc.hi0 $T run scoreboard players remove #fc.y $T 360")
					raw("execute $moved if score #fc.w $T matches 1 if score #fc.y $T < #fc.lo0 $T run scoreboard players add #fc.y $T 360")
				}
				raw("execute $moved if score #fc.$s $T < #fc.lo$i $T run scoreboard players operation #fc.$s $T = #fc.lo$i $T")
				raw("execute $moved if score #fc.$s $T > #fc.hi$i $T run scoreboard players operation #fc.$s $T = #fc.hi$i $T")
			}
		}

		// {name, j}: the number form for yaw (0) or pitch (1), bounded by its range.
		pack.define(ask) {
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_name set from storage ra:dh q.name")
			raw("data modify storage ra:dh pending_kind set value \"facing\"")
			raw("data modify storage ra:dh pending_j set from storage ra:dh q.j")
			for ((i, pair) in PARTS.withIndex()) {
				raw("execute if score #dh.r $T matches $i run scoreboard players operation #dh.min $T = #fc.lo$i $T")
				raw("execute if score #dh.r $T matches $i run scoreboard players operation #dh.max $T = #fc.hi$i $T")
				raw("\$execute if score #dh.r $T matches $i run data modify storage ra:input label set value \"\$(name) ${pair.first}\"")
			}
			function(DataHandlerActions.requestNumber)
		}

		// The number form's answer for one part. As the player, from DataHandlerActions.genericApply
		// after probe and refuse, with q {name}, cur {kind:"facing", j}. Returns 1 when written.
		pack.define(applyTyped) {
			raw("""execute unless score #dh.type $T matches 8 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a facing any more; nothing was changed.",color:"red"}]""")
			raw("execute unless score #dh.type $T matches 8 run return 0")
			function(bounds)
			callWith(load, "storage ra:dh q")
			raw("execute store result score #fc.j $T run data get storage ra:dh cur.j")
			for ((i, pair) in PARTS.withIndex())
				raw("execute if score #fc.j $T matches $i store result score #fc.${pair.second} $T run data get storage ra:input consume.number")
			raw("execute store result score #fc.ok $T run function ${check.asId()}")
			raw("execute if score #fc.ok $T matches 0 run return 0")
			callWith(write, "storage ra:dh q")
			raw("return 1")
		}
	}
}
