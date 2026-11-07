package ra.tools.datahandler

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.api.Tools
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandler.refresh
import ra.tools.datahandler.DataHandler.showInternalData
import ra.tools.datahandler.DataHandler.showMenu
import ra.tools.datahandler.props.DataHandlerProps
import ra.tools.tool

/**
 * What the Data Handler's chat buttons do: `/trigger ra.dh.action set <n>`, handled here.
 *
 *   90..93      menu actions (internal data, refresh, menu, cancel input)
 *   100 + i     the button of registry row i (edit)
 *   200 + i     the [Set from hand] button of registry row i
 *
 * Property rows carry their registry index rather than a per-property constant, so one handler
 * serves every property instead of a branch per name.
 */
object DataHandlerActions {
	val tick = Tools.DataHandler.tick
	val runAction = tool("data_handler/run_action")
	val applyPending = tool("data_handler/apply_pending")
	val genericAction = tool("data_handler/generic_action")
	val genericApply = tool("data_handler/generic_apply")
	val requestNumber = tool("data_handler/request_number")
	val requestText = tool("data_handler/request_text")

	private const val NO_TARGET = "execute unless entity @e[type=marker,tag=ra.dh_target,limit=1]"
	private const val PREFIX = """{text:"[Data Handler] ",color:"gold"}"""

	fun Ra.registerDataHandlerActions() {
		// Non-OP trigger actions and pending input for data handler users.
		// The trigger is handed out in ra:tick, to players holding the Data Handler -- see there.
		// Re-enabling it for everybody here would undo that.
		pack.define(tick) {
			executeRun("as @a[scores={ra.dh.action=1..}]", runAction)
			executeRun("as @a[scores={ra.dh.pending=1..}]", applyPending)
		}

		pack.define(runAction) {
			raw("""$NO_TARGET run tellraw @s [$PREFIX,{text:"No selected target. Shift+RMB a block first.",color:"red"}]""")
			raw("$NO_TARGET run scoreboard players set @s ra.dh.pending 0")
			raw("$NO_TARGET run scoreboard players set @s ra.dh.action 0")
			raw("$NO_TARGET run scoreboard players enable @s ra.dh.action")
			raw("$NO_TARGET run return 0")

			executeRun("if score @s ra.dh.action matches 200..", DataHandlerProps.handAction)
			executeRun("if score @s ra.dh.action matches 100..199", genericAction)

			executeRun("if score @s ra.dh.action matches 90", showInternalData)
			executeRun("if score @s ra.dh.action matches 91", refresh)
			executeRun("if score @s ra.dh.action matches 92", showMenu)

			// Cancel the pending edit / input.
			executeRun("if score @s ra.dh.action matches 93", RaLib.Input.cancel)
			raw("execute if score @s ra.dh.action matches 93 run scoreboard players set @s ra.dh.pending 0")
			raw("""execute if score @s ra.dh.action matches 93 run tellraw @s [$PREFIX,{text:"Pending edit canceled.",color:"gray"}]""")

			scoreboard.players.set(self(), "ra.dh.action", 0)
			raw("scoreboard players enable @s ra.dh.action")
		}

		// Apply a ready input to the pending property edit.
		pack.define(applyPending) {
			raw("$NO_TARGET run scoreboard players set @s ra.dh.pending 0")
			raw("$NO_TARGET run return 0")
			// A cancelled session (the player dropped the form) never becomes ready, so stop waiting
			// for it. `submit` clears ra.input.active but leaves state at 2, hence the second
			// condition -- without it a finished input would be discarded here on the very tick it
			// became available.
			val gone = "execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2"
			raw("$gone run scoreboard players set @s ra.dh.pending 0")
			raw("$gone run return 0")
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.poll)
			raw("execute unless score @s ${Obj.TEMP} matches 2 run return 0")
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.consume)
			raw("execute unless score @s ${Obj.TEMP} matches 1 run return 0")
			// One write for every property: the pending id is 100 + the registry index, and storage
			// ra:dh pending_name / pending_kind carry the rest.
			executeRun("if score @s ra.dh.pending matches 100..", genericApply)
			scoreboard.players.set(self(), "ra.dh.pending", 0)
			raw("""tellraw @s [$PREFIX,{text:"Property updated.",color:"green"}]""")
			function(refresh)
		}

		// A property row's button, as the player, with ra.dh.action at 100 + row index.
		pack.define(genericAction) {
			raw("scoreboard players operation #dh.idx ${Obj.TEMP} = @s ra.dh.action")
			scoreboard.players.remove(literal("#dh.idx"), Obj.TEMP, 100)
			raw("data modify storage ra:dh q set value {}")
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.idx ${Obj.TEMP}")
			callWith(DataHandlerProps.pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")

			// A dead button stops the click, but the row's action is a /trigger and a player can
			// still type one. This is the refusal that actually holds.
			function(DataHandler.collectHidden)
			callWith(DataHandlerProps.deny, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")

			callWith(DataHandlerProps.probe, "storage ra:dh q")

			// A flag needs no input: flip it and show the result.
			executeRun("if score #dh.type ${Obj.TEMP} matches 2", DataHandlerProps.toggle, "with storage ra:dh q")
			executeRun("if score #dh.type ${Obj.TEMP} matches 2", refresh)
			raw("execute if score #dh.type ${Obj.TEMP} matches 2 run return 0")

			// Everything else waits for the input form. The kind is remembered so the apply step does
			// not have to probe a value that is about to be replaced.
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_kind set value \"text\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 1 run data modify storage ra:dh pending_kind set value \"number\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 3 run data modify storage ra:dh pending_kind set value \"list\"")
			executeRun("if score #dh.type ${Obj.TEMP} matches 1", DataHandlerProps.askNumber, "with storage ra:dh q")
			executeRun("unless score #dh.type ${Obj.TEMP} matches 1", DataHandlerProps.askText, "with storage ra:dh q")
		}

		// Write a finished input into the property it was asked for. As the player.
		pack.define(genericApply) {
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q set value {}")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			executeRun("if data storage ra:dh {pending_kind:\"number\"}", DataHandlerProps.writeNumber, "with storage ra:dh q")
			executeRun("if data storage ra:dh {pending_kind:\"text\"}", DataHandlerProps.writeText, "with storage ra:dh q")
			// A list is typed rather than quoted, so the text is pasted into the command as SNBT. Bad
			// syntax fails the macro and leaves the property alone.
			raw("data modify storage ra:dh q.text set from storage ra:input consume.text")
			executeRun("if data storage ra:dh {pending_kind:\"list\"}", DataHandlerProps.writeList, "with storage ra:dh q")
		}

		// Open an input request for the pending property edit: mode 3 numeric, mode 2 free text.
		for ((fn, mode, max) in listOf(Triple(requestNumber, 3, 9999), Triple(requestText, 2, 120)))
			pack.define(fn) {
				function(RaLib.Input.cancel)
				scoreboard.players.set(self(), "ra.input.mode", mode)
				scoreboard.players.set(self(), "ra.input.min", 0)
				scoreboard.players.set(self(), "ra.input.max", max)
				function(RaLib.Input.sessionCreate)
				function(RaLib.Input.routerSelectBackend)
				function(RaLib.Input.routerOpen)
			}
	}
}
