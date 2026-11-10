package ra.tools.datahandler.props

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Admin
import ra.core.Obj
import ra.core.Ra
import ra.core.Vec3Spec
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandler
import ra.tools.datahandler.DataHandlerActions
import ra.tools.datahandler.DataHandlerRegistry
import ra.tools.datahandler.props.Vec3Ops.commit
import ra.tools.datahandler.props.Vec3Ops.registerVec3Ops
import ra.tools.tool

/**
 * THE VEC3 EDITOR: a vec3 property's row (ra.core.Vec3Props), and what its buttons do.
 *
 * ```
 *   target: [x 0, y 1, z 0] vec3 (blocks, from the block) [Edit]
 *     X: 0  [Type] [-X] [+X]  (-16 to 16)
 *     Y: 1  [Type] [-Y] [+Y]  (-16 to 16)
 *     Z: 0  [Type] [-Z] [+Z]  (-16 to 16)
 *     Step: [1] [4] [16]
 *     [Pick] [Reset]
 * ```
 *
 * The row is compact until [Edit] opens it, one open row per player like a list's [List]
 * (`ra.dh.expand`). While it is open, and for a few seconds after each change, the block the
 * value points at is outlined (Vec3Ops.show); a display's offset (frame 0) needs no outline, the
 * display itself moves within a second.
 *
 *   [Type]       the number form (ra_lib:input), bounded by the axis' range for this player
 *   [-X] [+X]    move by the chosen step, stopping at the range's end
 *   Step         the nudge sizes the property declares; the choice is per player (`ra.dh.v3s`)
 *   [Pick]       the block the player is looking at, up to Vec3Ops.PICK_REACH blocks
 *   [Reset]      the declared default
 *
 * Buttons share a list's action space (DataHandlerEntries): `menu stamp * MUL + 1000 + 300 i + r`
 * with i the row, and r: 0..2 [Type] x/y/z, 10..15 [-X] [+X] [-Y] [+Y] [-Z] [+Z], 20..23 a step,
 * 30 [Pick], 31 [Reset], 199 [Edit] (open / close). Every click is checked again (probe, refuse,
 * range): a button is only a /trigger. Admins get the declared admin range, and a value outside
 * the players' range is marked " (admin)" in gold, as a number above its maximum is.
 */
object DataHandlerVec3 {
	val render = tool("data_handler/props/vec3/render")
	val action = tool("data_handler/props/vec3/action")
	val openRow = tool("data_handler/props/vec3/open_row")
	val applyTyped = tool("data_handler/props/vec3/apply_typed")
	private val toggle = tool("data_handler/props/vec3/toggle")
	private val head = tool("data_handler/props/vec3/head")
	private fun axisRow(i: Int) = tool("data_handler/props/vec3/axis_$i")
	private fun stepRow(n: Int) = tool("data_handler/props/vec3/steps_$n")
	private val toolsPick = tool("data_handler/props/vec3/tools_pick")
	private val toolsFace = tool("data_handler/props/vec3/tools_face")
	private val ask = tool("data_handler/props/vec3/ask")
	private val nudge = tool("data_handler/props/vec3/nudge")
	private val reset = tool("data_handler/props/vec3/reset")

	/** On a player: the index of the nudge step they chose. */
	const val STEP = "ra.dh.v3s"

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val EXPAND = DataHandlerRegistry.EXPAND
	private val AXES = listOf("x", "y", "z")

	private fun btn(label: String, color: String, action: String, hover: String) =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.dh.action set $($action)"},hover_event:{action:"show_text",value:"$hover"}}"""

	private const val SP = """{text:" "}"""

	/** #dh.ri, the row's schema index (from #dh.act); #dh.eb its entry actions; #dh.xp 1 when open. */
	private fun Function.rowActions() {
		raw("scoreboard players operation #dh.ri $T = #dh.act $T")
		raw("scoreboard players operation #dh.ri $T -= #dh.base $T")
		raw("scoreboard players remove #dh.ri $T 100")
		raw("scoreboard players set #dh.k300 $T 300")
		raw("scoreboard players operation #dh.eb $T = #dh.ri $T")
		raw("scoreboard players operation #dh.eb $T *= #dh.k300 $T")
		raw("scoreboard players operation #dh.eb $T += #dh.base $T")
		raw("scoreboard players add #dh.eb $T 1000")
		raw("scoreboard players operation #dh.xv $T = #dh.ri $T")
		raw("scoreboard players add #dh.xv $T 1")
		raw("execute store success score #dh.xp $T if score @s $EXPAND = #dh.xv $T")
	}

	/** storage ra:dh vq.<key> = #dh.eb + [off]. */
	private fun Function.act(key: String, off: Int) {
		raw("scoreboard players operation #dh.ap $T = #dh.eb $T")
		raw("scoreboard players add #dh.ap $T $off")
		raw("execute store result storage ra:dh vq.$key int 1 run scoreboard players get #dh.ap $T")
	}

	/** #v3.st, the player's chosen step (the first when the choice is not one this property has). */
	private fun Function.chosenStep() {
		raw("execute unless score @s $STEP matches 0..${Vec3Spec.MAX_STEPS - 1} run scoreboard players set @s $STEP 0")
		raw("execute store result score #v3.n $T run data get storage ra:dh spec.v3.steps")
		raw("execute unless score @s $STEP < #v3.n $T run scoreboard players set @s $STEP 0")
	}

	fun Ra.registerDataHandlerVec3() {
		// {name, note, admin, admin_why, action, ...}: as the player, after probe, for a declared,
		// unlocked vec3.
		pack.define(render) {
			rowActions()
			executeReturnRun("unless score #dh.ri $T matches 0..${DataHandlerEntries.MAX_ROWS - 1}", PropRows.rowCompound, "with storage ra:dh q")
			raw("data modify storage ra:dh vq set value {lc:\"aqua\",lh:\"Open the editor\"}")
			for (k in listOf("name", "note", "admin", "admin_why")) raw("data modify storage ra:dh vq.$k set from storage ra:dh q.$k")
			for (k in listOf("unit_label", "frame_label")) raw("data modify storage ra:dh vq.$k set from storage ra:dh spec.v3.$k")
			act("e", DataHandlerEntries.LIST)
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh vq.lc set value \"gold\"")
			raw("execute if score #dh.xp $T matches 1 run data modify storage ra:dh vq.lh set value \"Close the editor\"")
			// Outside the players' range: only an admin could have set it.
			raw("scoreboard players set #v3.adm $T 0")
			function(Vec3Ops.bounds)
			callWith(Vec3Ops.load, "storage ra:dh q")
			raw("scoreboard players set #v3.over $T 0")
			for ((i, a) in AXES.withIndex()) {
				raw("execute unless score #v3.$a $T >= #v3.lo$i $T run scoreboard players set #v3.over $T 1")
				raw("execute unless score #v3.$a $T <= #v3.hi$i $T run scoreboard players set #v3.over $T 1")
			}
			raw("execute if score #v3.over $T matches 1 run data modify storage ra:dh vq.admin set value \"${Admin.MARK}\"")
			raw("execute if score #v3.over $T matches 1 run data modify storage ra:dh vq.admin_why set value \"${Admin.OVER_REASON}\"")
			callWith(head, "storage ra:dh vq")
			raw("execute if score #dh.xp $T matches 0 run return run data remove storage ra:dh vq")

			// Open: this player's range, every button, the outline.
			raw("scoreboard players operation #v3.adm $T = #dh.adm $T")
			function(Vec3Ops.bounds)
			for (i in 0..2) {
				act("t$i", i); act("m$i", 10 + 2 * i); act("p$i", 11 + 2 * i)
				callWith(axisRow(i), "storage ra:dh vq")
			}
			chosenStep()
			for (k in 0 until Vec3Spec.MAX_STEPS) {
				act("s$k", 20 + k)
				raw("data modify storage ra:dh vq.l$k set from storage ra:dh spec.v3.labels[$k]")
				raw("data modify storage ra:dh vq.c$k set value \"aqua\"")
				raw("execute if score @s $STEP matches $k run data modify storage ra:dh vq.c$k set value \"gold\"")
			}
			for (n in 1..Vec3Spec.MAX_STEPS) executeRun("if score #v3.n $T matches $n", stepRow(n), "with storage ra:dh vq")
			act("pk", 30); act("rs", 31)
			executeRun("if score #v3.f $T matches 0", toolsFace, "with storage ra:dh vq")
			executeRun("unless score #v3.f $T matches 0", toolsPick, "with storage ra:dh vq")
			raw("data remove storage ra:dh vq")
			function(Vec3Ops.show)
		}

		fun Ra.line(fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument, vararg parts: String) =
			pack.define(fn) { raw("\$tellraw @s [" + parts.joinToString(",") + "]") }
		val value = AXES.mapIndexed { i, a ->
			(if (i == 0) "" else """{text:", ",color:"dark_gray"},""") + """{text:"$a ",color:"gray"},{nbt:"properties.$(name).$a",storage:"ra:dh",color:"green"}"""
		}.joinToString(",")
		line(
			head, """{text:"  "},{text:"$(name)",color:"white"},{text:"$(note)",color:"dark_gray",italic:true},${PropRows.ADMIN_MARK},{text:": ",color:"white"}""",
			"""{text:"[",color:"dark_gray"},$value,{text:"]",color:"dark_gray"}""",
			"""{text:" vec3",color:"aqua"},{text:" ($(unit_label), $(frame_label)) ",color:"gray"}""",
			btn("[Edit]", "$(lc)", "e", "$(lh)"),
		)
		for ((i, a) in AXES.withIndex()) {
			val A = a.uppercase()
			line(
				axisRow(i), """{text:"    $A: ",color:"gray"},{nbt:"properties.$(name).$a",storage:"ra:dh",color:"green"},{text:"  "}""",
				btn("[Type]", "yellow", "t$i", "Type a number for $A"), SP,
				btn("[-$A]", "red", "m$i", "Move by the chosen step toward -$A"), SP,
				btn("[+$A]", "green", "p$i", "Move by the chosen step toward +$A"),
				"""{text:"  (",color:"dark_gray"},{score:{name:"#v3.lo$i",objective:"$T"},color:"gray"},{text:" to ",color:"dark_gray"},{score:{name:"#v3.hi$i",objective:"$T"},color:"gray"},{text:")",color:"dark_gray"}""",
			)
		}
		for (n in 1..Vec3Spec.MAX_STEPS) line(
			stepRow(n), """{text:"    Step: ",color:"gray"}""",
			*(0 until n).flatMap { k -> listOf(btn("[$(l$k)]", "$(c$k)", "s$k", "Nudge by $(l$k)"), SP) }.toTypedArray(),
			"""{text:"($(unit_label))",color:"dark_gray"}""",
		)
		line(toolsPick, """{text:"    "}""", btn("[Pick]", "aqua", "pk", "Set it to the block you are looking at"), SP, btn("[Reset]", "red", "rs", "Back to the default"))
		line(toolsFace, """{text:"    "}""", btn("[Reset]", "red", "rs", "Back to the default"))

		// As the player, from DataHandlerEntries.entryAction after probe: #dh.r the button, #dh.code
		// the row, q {name}.
		pack.define(action) {
			executeReturnRun("if score #dh.r $T matches ${DataHandlerEntries.LIST}", toggle)
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")
			raw("scoreboard players operation #v3.adm $T = #dh.adm $T")
			function(Vec3Ops.bounds)
			// [Type]
			raw("execute if score #dh.r $T matches 0..2 store result storage ra:dh q.j int 1 run scoreboard players get #dh.r $T")
			executeReturnRun("if score #dh.r $T matches 0..2", ask, "with storage ra:dh q")
			// A step.
			chosenStep()
			raw("scoreboard players operation #v3.k $T = #dh.r $T")
			raw("scoreboard players remove #v3.k $T 20")
			raw("execute if score #dh.r $T matches 20..23 if score #v3.k $T < #v3.n $T run scoreboard players operation @s $STEP = #v3.k $T")
			executeReturnRun("if score #dh.r $T matches 20..23", DataHandler.refresh)
			callWith(Vec3Ops.load, "storage ra:dh q")
			executeRun("if score #dh.r $T matches 10..15", nudge)
			raw("execute if score #dh.r $T matches 30 store result score #v3.ok $T run function ${Vec3Ops.pick.asId()}")
			raw("execute if score #dh.r $T matches 30 if score #v3.ok $T matches 0 run return 0")
			executeRun("if score #dh.r $T matches 31", reset)
			raw("execute unless score #dh.r $T matches 10..15 unless score #dh.r $T matches 30..31 run return 0")
			commit()
			function(DataHandler.refresh)
		}

		// [Edit]: open this row's editor, or close it when it is the open one. #dh.code the row.
		pack.define(toggle) {
			raw("execute if score #dh.lock $T matches 3 run function ${DataHandlerProps.refuse.asId()} with storage ra:dh q")
			raw("execute if score #dh.lock $T matches 3 run return run data remove storage ra:dh denied")
			raw("scoreboard players operation #dh.xv $T = #dh.code $T")
			raw("scoreboard players add #dh.xv $T 1")
			raw("execute store success score #dh.xp $T if score @s $EXPAND = #dh.xv $T")
			raw("execute if score #dh.xp $T matches 1 run scoreboard players set @s $EXPAND 0")
			raw("execute if score #dh.xp $T matches 1 run function ${Vec3Ops.hide.asId()}")
			raw("execute if score #dh.xp $T matches 0 run scoreboard players operation @s $EXPAND = #dh.xv $T")
			function(DataHandler.refresh)
		}
		// The row's own action (100 + i, typed by hand: the row's button is [Edit]): the same.
		pack.define(openRow) { function(toggle) }

		// [-X] .. [+Z]: #dh.r 10..15, the step #v3.st, stopping at the range's end. Only the moved
		// axis is clamped: another one an admin set out of the players' range is refused by check,
		// not quietly changed.
		pack.define(nudge) {
			raw("scoreboard players set #v3.st $T 1")
			for (k in 0 until Vec3Spec.MAX_STEPS)
				raw("execute if score @s $STEP matches $k store result score #v3.st $T run data get storage ra:dh spec.v3.steps[$k]")
			for ((i, a) in AXES.withIndex()) {
				raw("execute if score #dh.r $T matches ${10 + 2 * i} run scoreboard players operation #v3.$a $T -= #v3.st $T")
				raw("execute if score #dh.r $T matches ${11 + 2 * i} run scoreboard players operation #v3.$a $T += #v3.st $T")
				val moved = "if score #dh.r $T matches ${10 + 2 * i}..${11 + 2 * i}"
				raw("execute $moved if score #v3.$a $T < #v3.lo$i $T run scoreboard players operation #v3.$a $T = #v3.lo$i $T")
				raw("execute $moved if score #v3.$a $T > #v3.hi$i $T run scoreboard players operation #v3.$a $T = #v3.hi$i $T")
			}
		}
		// [Reset]: the declared default; for a world position, the default from the block.
		pack.define(reset) {
			for ((i, a) in AXES.withIndex()) {
				raw("execute store result score #v3.$a $T run data get storage ra:dh spec.v3.def.$a")
				raw("scoreboard players operation #v3.$a $T += #v3.b$i $T")
			}
		}

		// {name, j}: the number form for one axis, bounded by this player's range for it.
		pack.define(ask) {
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_name set from storage ra:dh q.name")
			raw("data modify storage ra:dh pending_kind set value \"vec3\"")
			raw("data modify storage ra:dh pending_j set from storage ra:dh q.j")
			for ((i, a) in AXES.withIndex()) {
				raw("execute if score #dh.r $T matches $i run scoreboard players operation #dh.min $T = #v3.lo$i $T")
				raw("execute if score #dh.r $T matches $i run scoreboard players operation #dh.max $T = #v3.hi$i $T")
				raw("\$execute if score #dh.r $T matches $i run data modify storage ra:input label set value \"\$(name) ${a.uppercase()}\"")
			}
			function(DataHandlerActions.requestNumber)
		}

		// The number form's answer for one axis. As the player, from DataHandlerActions.genericApply
		// after probe and refuse, with q {name}, cur {kind:"vec3", j}. Returns 1 when written.
		pack.define(applyTyped) {
			raw("""execute unless score #dh.type $T matches 6 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not a vec3 any more; nothing was changed.",color:"red"}]""")
			raw("execute unless score #dh.type $T matches 6 run return 0")
			raw("scoreboard players operation #v3.adm $T = #dh.adm $T")
			function(Vec3Ops.bounds)
			callWith(Vec3Ops.load, "storage ra:dh q")
			raw("execute store result score #v3.j $T run data get storage ra:dh cur.j")
			for ((i, a) in AXES.withIndex())
				raw("execute if score #v3.j $T matches $i store result score #v3.$a $T run data get storage ra:input consume.number")
			raw("execute store result score #v3.ok $T run function ${Vec3Ops.check.asId()}")
			raw("execute if score #v3.ok $T matches 0 run return 0")
			callWith(Vec3Ops.write, "storage ra:dh q")
			function(Vec3Ops.show)
			raw("return 1")
		}

		registerVec3Ops()
	}
}
