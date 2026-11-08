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
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.datahandler.DataHandler.refresh
import ra.tools.datahandler.DataHandler.showInternalData
import ra.tools.datahandler.DataHandler.showMenu
import ra.tools.datahandler.DataHandler.updateStorage
import ra.tools.datahandler.DataHandlerSelection.REQ
import ra.tools.datahandler.DataHandlerSelection.SEL
import ra.tools.datahandler.DataHandlerSelection.TARGET
import ra.tools.datahandler.DataHandlerSelection.pendingDrop
import ra.tools.datahandler.DataHandlerSelection.pendingForget
import ra.tools.datahandler.DataHandlerSelection.pendingLoad
import ra.tools.datahandler.DataHandlerSelection.pendingSave
import ra.tools.datahandler.DataHandlerSelection.select
import ra.tools.datahandler.props.DataHandlerList
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

	private const val NO_TARGET = "execute unless entity $TARGET"
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
			function(select)
			raw("""$NO_TARGET run tellraw @s [$PREFIX,{text:"No selected target. Shift+RMB a block first.",color:"red"}]""")
			executeRun(NO_TARGET.removePrefix("execute "), pendingDrop)
			raw("$NO_TARGET run scoreboard players set @s ra.dh.action 0")
			raw("$NO_TARGET run scoreboard players enable @s ra.dh.action")
			raw("$NO_TARGET run return 0")

			// What this player's block holds now. storage ra:dh is shared scratch, and the last
			// player to refresh would otherwise decide what every other player's buttons read.
			executeRun("as $TARGET", updateStorage)

			executeRun("if score @s ra.dh.action matches 200..", DataHandlerProps.handAction)
			executeRun("if score @s ra.dh.action matches 100..199", genericAction)

			executeRun("if score @s ra.dh.action matches 90", showInternalData)
			executeRun("if score @s ra.dh.action matches 91", refresh)
			executeRun("if score @s ra.dh.action matches 92", showMenu)

			// Cancel the pending edit / input.
			executeRun("if score @s ra.dh.action matches 93", RaLib.Input.cancel)
			executeRun("if score @s ra.dh.action matches 93", pendingDrop)
			raw("""execute if score @s ra.dh.action matches 93 run tellraw @s [$PREFIX,{text:"Pending edit canceled.",color:"gray"}]""")

			scoreboard.players.set(self(), "ra.dh.action", 0)
			raw("scoreboard players enable @s ra.dh.action")
		}

		// Apply a ready input to the pending property edit. Every stop that ends the wait goes
		// through pending/drop, so the stored edit never outlives it.
		pack.define(applyPending) {
			function(select)
			executeReturnRun(NO_TARGET.removePrefix("execute "), pendingDrop)
			// A cancelled session (the player dropped the form) never becomes ready, so stop waiting
			// for it. `submit` clears ra.input.active but leaves state at 2, hence the second
			// condition -- without it a finished input would be discarded here on the very tick it
			// became available.
			executeReturnRun("unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2", pendingDrop)
			// Not our answer. The settings menu and the Remote share ra_lib:input; consuming their
			// session would hand them an empty result. Ours was cancelled when theirs opened, so it
			// is never coming back.
			executeReturnRun("unless score @s $REQ = @s ra.input.req", pendingDrop)
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.poll)
			raw("execute unless score @s ${Obj.TEMP} matches 2 run return 0")
			executeRun("store result score @s ${Obj.TEMP}", RaLib.Input.consume)
			raw("execute unless score @s ${Obj.TEMP} matches 1 run return 0")

			// The question, into the working copy storage ra:dh cur; the stored one goes now.
			function(pendingLoad)
			function(pendingForget)
			scoreboard.players.set(self(), "ra.dh.pending", 0)

			// The block as it is now: the write below checks against it, not against what it held
			// when the question was asked.
			executeRun("as $TARGET", updateStorage)
			scoreboard.players.set(literal("#dh.ok"), Obj.TEMP, 0)
			executeRun("store result score #dh.ok ${Obj.TEMP}", genericApply)
			raw("""execute if score #dh.ok ${Obj.TEMP} matches 1 run tellraw @s [$PREFIX,{text:"Property updated.",color:"green"}]""")
			raw("data remove storage ra:dh cur")
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
			// not have to probe a value that is about to be replaced. pending_name / pending_kind are
			// only a hand-over: request_* files them under this player's request (pending/save).
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_kind set value \"text\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 1 run data modify storage ra:dh pending_kind set value \"number\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 3 run data modify storage ra:dh pending_kind set value \"list\"")
			executeRun("if score #dh.type ${Obj.TEMP} matches 1", DataHandlerProps.askNumber, "with storage ra:dh q")
			executeRun("unless score #dh.type ${Obj.TEMP} matches 1", DataHandlerProps.askText, "with storage ra:dh q")
		}

		// Write a finished input into the property it was asked for, from storage ra:dh cur
		// {name, kind, mid}. As the player. Returns 1 when written, 0 when refused.
		pack.define(genericApply) {
			raw("execute unless data storage ra:dh cur.name run return 0")

			// Asked about one block, answered while pointing at another: the value was meant for the
			// first, under a property the second may not have or may keep to itself.
			raw("execute store result score #dh.pmid ${Obj.TEMP} run data get storage ra:dh cur.mid")
			raw("""execute unless score #dh.pmid ${Obj.TEMP} = @s $SEL run tellraw @s [$PREFIX,{text:"You selected another block since; the value was not applied.",color:"red"}]""")
			raw("execute unless score #dh.pmid ${Obj.TEMP} = @s $SEL run return 0")

			raw("data modify storage ra:dh q set value {}")
			raw("data modify storage ra:dh q.name set from storage ra:dh cur.name")

			// The read-only check again, against the block the value is about to land on.
			function(DataHandler.collectHidden)
			callWith(DataHandlerProps.deny, "storage ra:dh q")
			raw("execute store success score #dh.den ${Obj.TEMP} run data remove storage ra:dh denied")
			raw("execute if score #dh.den ${Obj.TEMP} matches 1 run return 0")

			executeRun("if data storage ra:dh cur{kind:\"number\"}", DataHandlerProps.writeNumber, "with storage ra:dh q")
			raw("execute if data storage ra:dh cur{kind:\"number\"} run return 1")
			executeRun("if data storage ra:dh cur{kind:\"text\"}", DataHandlerProps.writeText, "with storage ra:dh q")
			raw("execute if data storage ra:dh cur{kind:\"text\"} run return 1")
			// A list is typed rather than quoted; DataHandlerList parses and checks it before the
			// block sees it.
			raw("data modify storage ra:dh q.text set from storage ra:input consume.text")
			executeReturnRun("if data storage ra:dh cur{kind:\"list\"}", DataHandlerList.apply)
			raw("return 0")
		}

		// Open an input request for the pending property edit: mode 3 numeric, mode 2 free text.
		for ((fn, mode, max) in listOf(Triple(requestNumber, 3, 9999), Triple(requestText, 2, 120)))
			pack.define(fn) {
				// Whatever this player was asked before is over: its session is cancelled next.
				function(pendingForget)
				function(RaLib.Input.cancel)
				scoreboard.players.set(self(), "ra.input.mode", mode)
				scoreboard.players.set(self(), "ra.input.min", 0)
				scoreboard.players.set(self(), "ra.input.max", max)
				function(RaLib.Input.sessionCreate)
				function(RaLib.Input.routerSelectBackend)
				function(RaLib.Input.routerOpen)
				// Which request is ours, and what it asks.
				raw("scoreboard players operation @s $REQ = @s ra.input.req")
				function(pendingSave)
			}
	}
}
