package ra.lib.input.backend

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.input.IN
import ra.lib.input.InputObj
import ra.lib.input.inputMessage
import ra.lib.input.storeReq
import ra.lib.input.InputRange.tellRange
import ra.lib.lib

/** `ra_lib:input/backend/trigger/...`: numbers, submitted with `/trigger ra.input.trigger set <n>`. */
object TriggerBackend {
	val openChoice = lib("input/backend/trigger/open_choice")
	val storeChoice = lib("input/backend/trigger/store_choice")
	val submit = lib("input/backend/trigger/submit")
	val tick = lib("input/backend/trigger/tick")

	fun Ra.registerTriggerBackend() {
		// Prompt the player to submit numeric input using /trigger.
		pack.define(openChoice) {
			// Enabled here, for number prompts only; session/cleanup resets it when the prompt ends.
			scoreboard.players.enable(self(), InputObj.TRIGGER)

			raw("tellraw @s \"\"")
			inputMessage("Numeric input requested.", "aqua")
			// "Enter a number for <label> (1–1200)", the label from the caller (InputRange).
			val mm = arrayOf("@s", InputObj.MIN, "@s", InputObj.MAX)
			tellRange(
				"@s", """{text:"Enter a number for ",color:"gray"},{nbt:"label",storage:"ra:input",color:"yellow"}""", "",
				mm[0], mm[1], mm[2], mm[3], condition = "if data storage ra:input label", color = "yellow",
			)
			tellRange("@s", """{text:"Enter a number",color:"gray"}""", "", mm[0], mm[1], mm[2], mm[3], condition = "unless data storage ra:input label", color = "yellow")
			tellRange(
				"@s",
				"""{text:"Use ",color:"gray"},{text:"/trigger ${InputObj.TRIGGER} set <n>",color:"yellow",hover_event:{action:"show_text",value:"Enter a number in the shown range"},click_event:{action:"suggest_command",command:"/trigger ${InputObj.TRIGGER} set "}}""",
				"", mm[0], mm[1], mm[2], mm[3], color = "yellow",
			)
		}

		// Macro storage shape: {req, value}.
		pack.define(storeChoice) {
			raw("\$data modify storage ra:input sessions.req_\$(req).result.choice set value \$(value)")
			raw("\$data modify storage ra:input sessions.req_\$(req).state set value \"ready\"")
		}

		// Capture and validate the submitted trigger value.
		pack.define(submit) {
			scoreboard.players.operation(self(), InputObj.VALUE, Operation.SET, self(), InputObj.TRIGGER)

			// Reset the trigger to the sentinel and re-enable it for future submissions.
			scoreboard.players.set(self(), InputObj.TRIGGER, InputObj.SENTINEL)
			scoreboard.players.set(self(), InputObj.LAST, InputObj.SENTINEL)
			scoreboard.players.enable(self(), InputObj.TRIGGER)

			// Refused with the range, so the player knows what to type instead.
			raw("scoreboard players set #in.oor ${ra.core.Obj.TEMP} 0")
			raw("execute unless score @s ${InputObj.VALUE} >= @s ${InputObj.MIN} run scoreboard players set #in.oor ${ra.core.Obj.TEMP} 1")
			raw("execute unless score @s ${InputObj.VALUE} <= @s ${InputObj.MAX} run scoreboard players set #in.oor ${ra.core.Obj.TEMP} 1")
			tellRange(
				"@s", """{text:"[RA Input] ",color:"gold"},{text:"Out of range: enter a number",color:"red"}""", """{text:".",color:"red"}""",
				"@s", InputObj.MIN, "@s", InputObj.MAX, condition = "if score #in.oor ${ra.core.Obj.TEMP} matches 1", color = "yellow",
			)
			raw("execute if score #in.oor ${ra.core.Obj.TEMP} matches 1 run return 0")

			scoreboard.players.operation(self(), InputObj.RESULT, Operation.SET, self(), InputObj.VALUE)
			scoreboard.players.set(self(), InputObj.STATE, 2)
			tag(self()) { remove(InputObj.ACTIVE) }
			// Answered: nothing more to submit, so take the trigger off the /trigger list now
			// rather than when the caller consumes.
			scoreboard.players.reset(self(), InputObj.TRIGGER)

			storeReq()
			raw("execute store result storage ra:input tmp.value int 1 run scoreboard players get @s ${InputObj.VALUE}")
			function(storeChoice, IN, "tmp")
			data(IN) { remove("tmp") }

			inputMessage("Input captured.", "green")
		}

		// Detect trigger submissions -- any change from the last value -- and validate them.
		pack.define(tick) {
			execute {
				ifCondition { score(self(), InputObj.TRIGGER) greaterThan score(self(), InputObj.LAST) }
				run { function(submit) }
			}
			execute {
				ifCondition { score(self(), InputObj.TRIGGER) lessThan score(self(), InputObj.LAST) }
				run { function(submit) }
			}
		}
	}
}
