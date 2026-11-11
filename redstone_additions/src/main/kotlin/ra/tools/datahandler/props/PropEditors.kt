package ra.tools.datahandler.props

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * What the direction, facing and colour editors share with each other (and with the vec3 one,
 * whose copies of these predate them): a row's actions in the entry action space, the buttons,
 * one tellraw per line.
 *
 * A row's buttons are `menu stamp * MUL + 1000 + 300 i + r` (DataHandlerEntries), i the row's
 * index in the schema and r the button; r = [DataHandlerEntries.LIST] (199) is a row's [Edit],
 * which opens or closes its editor, one open row per player (`ra.dh.expand`, row index + 1).
 */
internal object PropEditors {
	private const val T = Obj.TEMP

	/** A button: `$(<action>)` is its action number, from the row's storage. */
	fun btn(label: String, color: String, action: String, hover: String) =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.dh.action set $($action)"},hover_event:{action:"show_text",value:"$hover"}}"""

	const val SP = """{text:" "}"""

	/** As the player, rendering: #dh.ri the row's schema index, #dh.eb its entry actions, #dh.xp 1 when its editor is open. */
	fun Function.rowActions() {
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
		raw("execute store success score #dh.xp $T if score @s ${ra.tools.datahandler.DataHandlerRegistry.EXPAND} = #dh.xv $T")
	}

	/** `storage ra:dh <into>.<key>` = #dh.eb + [off]: a button's action number. */
	fun Function.act(into: String, key: String, off: Int) {
		raw("scoreboard players operation #dh.ap $T = #dh.eb $T")
		raw("scoreboard players add #dh.ap $T $off")
		raw("execute store result storage ra:dh $into.$key int 1 run scoreboard players get #dh.ap $T")
	}

	/** One chat line, a macro tellraw of [parts]. */
	fun Ra.line(fn: FunctionArgument, vararg parts: String) =
		pack.define(fn) { raw("\$tellraw @s [" + parts.joinToString(",") + "]") }

	/**
	 * [Edit]: open this row's editor, or close it when it is the open one. As the player, #dh.code
	 * the row. A row hidden from this player (#dh.lock 3) is refused.
	 */
	fun Function.toggleEditor(after: FunctionArgument) {
		raw("execute if score #dh.lock $T matches 3 run function ${DataHandlerProps.refuse.asId()} with storage ra:dh q")
		raw("execute if score #dh.lock $T matches 3 run return run data remove storage ra:dh denied")
		raw("scoreboard players operation #dh.xv $T = #dh.code $T")
		raw("scoreboard players add #dh.xv $T 1")
		val expand = ra.tools.datahandler.DataHandlerRegistry.EXPAND
		raw("execute store success score #dh.xp $T if score @s $expand = #dh.xv $T")
		raw("execute if score #dh.xp $T matches 1 run scoreboard players set @s $expand 0")
		raw("execute if score #dh.xp $T matches 0 run scoreboard players operation @s $expand = #dh.xv $T")
		raw("function ${after.asId()}")
	}

	/** The change sound, for a player who has sounds on. */
	fun Function.pling() = raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.4")

	/** The refusal sound. */
	fun Function.bass() = raw("playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
}
