package ra.tools.inputdatahandler

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.ToolItems
import ra.tools.executeRun
import ra.tools.tool

/**
 * The Input Data Handler: a test bench for ra_lib:input. Shift+RMB opens a menu of `/trigger`
 * buttons (so non-OP players can use it) that open, poll, consume and cancel input sessions.
 */
object InputDataHandler {
	val give = tool("input_data_handler/give")
	val onUse = tool("input_data_handler/on_use")
	val tick = tool("input_data_handler/tick")
	val runAction = tool("input_data_handler/run_action")
	val showMenu = tool("input_data_handler/show_menu")
	val testNumber = tool("input_data_handler/test_number")
	val testText = tool("input_data_handler/test_text")
	val poll = tool("input_data_handler/poll")
	val consume = tool("input_data_handler/consume")

	private const val PREFIX = """{text:"[RA Input] ",color:"gold"}"""

	fun Ra.registerInputDataHandler() {
		pack.define(give) {
			raw("give @s ${ToolItems.inputDataHandler.argument}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Input Data Handler",color:"aqua"}]""")
		}

		// As the player, from the ra:tools/input_data_handler_use advancement.
		pack.define(onUse) {
			advancement { revoke(self(), AdvancementArgument("tools/input_data_handler_use", "ra")) }
			tag(self()) { add("ra.input_handler_clicked") }
			raw("execute if entity @s[tag=ra.input_handler_active] run return fail")
			tag(self()) { add("ra.input_handler_active") }
			// Match Creative Data Handler behavior: only open the menu while sneaking.
			raw("execute unless predicate ra:is_sneaking run return 0")
			function(showMenu)
		}

		// Non-OP trigger actions from the menu.
		pack.define(tick) {
			// Every player may use the menu's trigger buttons.
			raw("scoreboard players enable @a ra.input_handler.action")
			executeRun("as @a[scores={ra.input_handler.action=1..}]", runAction)
		}

		// Run the selected menu action and re-enable the trigger.
		pack.define(runAction) {
			val actions = listOf(testNumber, testText, poll, consume, RaLib.Input.cancel, showMenu)
			actions.forEachIndexed { i, fn -> executeRun("if score @s ra.input_handler.action matches ${i + 1}", fn) }
			scoreboard.players.set(self(), "ra.input_handler.action", 0)
			raw("scoreboard players enable @s ra.input_handler.action")
		}

		pack.define(showMenu) {
			fun button(label: String, color: String, action: Int, hover: String) =
				"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger ra.input_handler.action set $action"},hover_event:{action:"show_text",value:"$hover"}}"""
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Input Data Handler",color:"aqua",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Shift+RMB with this tool to reopen this menu.",color:"gray"}]""")
			raw("tellraw @s \"\"")
			raw("tellraw @s [${button("[Open Number Input]", "gold", 1, "Open numeric input request (1..9)")}]")
			raw("tellraw @s [${button("[Open Text Input]", "light_purple", 2, "Give writable book and capture page 1")}]")
			raw("""tellraw @s [${button("[Poll State]", "yellow", 3, "Check current input session state")},{text:"  "},${button("[Consume Result]", "green", 4, "Consume latest ready result")}]""")
			raw("""tellraw @s [${button("[Cancel Session]", "red", 5, "Cancel current input session")},{text:"  "},${button("[Refresh]", "gray", 6, "Show this menu again")}]""")
		}

		pack.define(testNumber) { function(RaLib.Input.openNumber) }
		// A free-text request backed by writable books.
		pack.define(testText) { function(RaLib.Input.openText) }

		pack.define(poll) {
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.poll)
			raw("""tellraw @s [{text:"[RA Input] state=",color:"gold"},{score:{name:"@s",objective:"${Obj.TEMP}"},color:"yellow"},{text:" (0=idle, 1=waiting, 2=ready)",color:"gray"}]""")
		}
		pack.define(consume) {
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.consume)
			raw("""execute if score @s ${Obj.TEMP} matches 1 run tellraw @s [$PREFIX,{text:"Consumed: ",color:"green"},{nbt:"consume",storage:"ra:input",color:"white"}]""")
			raw("""execute unless score @s ${Obj.TEMP} matches 1 run tellraw @s [$PREFIX,{text:"No ready result.",color:"red"}]""")
		}
	}
}
