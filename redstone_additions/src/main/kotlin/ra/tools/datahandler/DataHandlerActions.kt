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
import ra.tools.datahandler.props.DataHandlerEntries
import ra.tools.datahandler.props.DataHandlerProps
import ra.tools.datahandler.DataHandlerRegistry
import ra.tools.datahandler.props.DataHandlerTypes
import ra.tools.datahandler.props.DataHandlerUnique
import ra.tools.datahandler.props.DataHandlerVec3
import ra.tools.datahandler.props.DataHandlerFacing
import ra.tools.datahandler.props.DataHandlerColor
import ra.tools.tool

/**
 * What the Data Handler's chat buttons do: `/trigger ra.dh.action set <n>`, handled here.
 *
 *   90..93                    menu actions (internal data, refresh, menu, cancel input)
 *   stamp * 1000 + 100 + i    edit row i of the block's schema (storage ra:dh block_order)
 *   stamp * 1000 + 400 + i    [Set from hand] of row i
 *   stamp * 1000 + 550 + i    [Clear] of item row i
 *   stamp * 1000 + 700 + i    edit an undeclared property, by its index in storage ra:dh known
 *   stamp * 1000 + 1000..     a list entry's buttons (DataHandlerEntries)
 *
 * (1000 stands for DataHandlerRegistry.MUL, which is 10000: room for the list entries.)
 *
 * The stamp is the number of the menu the row was drawn in (see DataHandlerRegistry): a click from
 * any other menu than the player's latest -- an old one scrolled back to, one drawn before a
 * /reload -- is refused and the menu redrawn, so an index always means what was on screen.
 */
object DataHandlerActions {
	val tick = Tools.DataHandler.tick
	val runAction = tool("data_handler/run_action")
	val applyPending = tool("data_handler/apply_pending")
	val genericAction = tool("data_handler/generic_action")
	val genericApply = tool("data_handler/generic_apply")
	val requestNumber = tool("data_handler/request_number")
	val requestText = tool("data_handler/request_text")
	val propertyGone = tool("data_handler/property_gone")

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

			// action = stamp * 1000 + code.
			raw("scoreboard players set #dh.k1000 ${Obj.TEMP} ${DataHandlerRegistry.MUL}")
			raw("scoreboard players operation #dh.code ${Obj.TEMP} = @s ra.dh.action")
			raw("scoreboard players operation #dh.code ${Obj.TEMP} %= #dh.k1000 ${Obj.TEMP}")
			raw("scoreboard players operation #dh.stamp ${Obj.TEMP} = @s ra.dh.action")
			raw("scoreboard players operation #dh.stamp ${Obj.TEMP} /= #dh.k1000 ${Obj.TEMP}")
			raw("scoreboard players set #dh.fresh ${Obj.TEMP} 0")
			raw("execute if score #dh.stamp ${Obj.TEMP} = @s ${DataHandlerRegistry.MENU} run scoreboard players set #dh.fresh ${Obj.TEMP} 1")
			raw("""execute if score #dh.code ${Obj.TEMP} matches 100.. if score #dh.fresh ${Obj.TEMP} matches 0 run tellraw @s [$PREFIX,{text:"That menu is out of date; here is the current one.",color:"gray"}]""")
			executeRun("if score #dh.code ${Obj.TEMP} matches 100.. if score #dh.fresh ${Obj.TEMP} matches 0", refresh)
			// The latest menu, but drawn for another block than the one selected now (the Creative Data
			// Handler, which shares the selection, picked another since): every row button acts on the
			// selected block, so none may run. This one check covers every action, immediate ones (a
			// toggle, a nudge, a face, a colour) and questions alike (#167).
			raw("execute if score #dh.code ${Obj.TEMP} matches 100.. if score #dh.fresh ${Obj.TEMP} matches 1 unless score @s ${DataHandlerRegistry.MENU_SEL} = @s $SEL run scoreboard players set #dh.fresh ${Obj.TEMP} 2")
			raw("""execute if score #dh.fresh ${Obj.TEMP} matches 2 run tellraw @s [$PREFIX,{text:"You selected another block since; nothing was changed. Here is its menu.",color:"red"}]""")
			executeRun("if score #dh.fresh ${Obj.TEMP} matches 2", refresh)

			val main = "if score #dh.fresh ${Obj.TEMP} matches 1"
			executeRun("$main if score #dh.code ${Obj.TEMP} matches ${DataHandlerProps.HAND_BASE}..${DataHandlerProps.CLEAR_BASE - 1}", DataHandlerProps.handAction)
			executeRun("$main if score #dh.code ${Obj.TEMP} matches ${DataHandlerProps.CLEAR_BASE}..699", DataHandlerProps.clearAction)
			executeRun("$main if score #dh.code ${Obj.TEMP} matches 100..399", genericAction)
			executeRun("$main if score #dh.code ${Obj.TEMP} matches 700..999", genericAction)
			executeRun("$main if score #dh.code ${Obj.TEMP} matches 1000..", DataHandlerEntries.entryAction)

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
			raw("data modify storage ra:dh q set value {list:\"block_order\"}")
			raw("execute if score #dh.code ${Obj.TEMP} matches 700.. run data modify storage ra:dh q.list set value \"known\"")
			raw("execute if score #dh.code ${Obj.TEMP} matches 700.. run scoreboard players remove #dh.code ${Obj.TEMP} 600")
			scoreboard.players.remove(literal("#dh.code"), Obj.TEMP, 100)
			raw("execute store result storage ra:dh q.i int 1 run scoreboard players get #dh.code ${Obj.TEMP}")
			callWith(DataHandlerProps.pickName, "storage ra:dh q")
			raw("execute unless data storage ra:dh pending_name run return 0")
			raw("data modify storage ra:dh q.name set from storage ra:dh pending_name")
			callWith(propertyGone, "storage ra:dh q")
			raw("execute if score #dh.gone ${Obj.TEMP} matches 1 run return 0")

			// Its type and what this player may do with it; a dead button stops the click, but the
			// row's action is a /trigger and a player can still type one. This is the refusal that
			// actually holds.
			callWith(DataHandlerProps.probe, "storage ra:dh q")
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute if data storage ra:dh denied run return run data remove storage ra:dh denied")

			// A flag needs no input: flip it and show the result.
			executeRun("if score #dh.type ${Obj.TEMP} matches 2", DataHandlerProps.toggle, "with storage ra:dh q")
			executeRun("if score #dh.type ${Obj.TEMP} matches 2", refresh)
			raw("execute if score #dh.type ${Obj.TEMP} matches 2 run return 0")

			// A vec3: its editor (DataHandlerVec3), opened or closed. #dh.code is the row.
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 6 if score #dh.decl ${Obj.TEMP} matches 1", DataHandlerVec3.openRow)
			// A facing or a colour: the same. A direction has nothing to type: its buttons are on its row.
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 8 if score #dh.decl ${Obj.TEMP} matches 1", DataHandlerFacing.openRow)
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 9 if score #dh.decl ${Obj.TEMP} matches 1", DataHandlerColor.openRow)
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 7", refresh)

			// A compound has no editor: there is nothing safe to type one in with.
			raw("""execute if score #dh.type ${Obj.TEMP} matches 5 run tellraw @s [$PREFIX,{text:"A compound cannot be edited here. Use the Creative Data Handler.",color:"red"}]""")
			raw("execute if score #dh.type ${Obj.TEMP} matches 5 run return 0")

			// Everything else waits for the input form. The kind is remembered so the apply step
			// knows which form answered; the value is re-typed there against the block as it is
			// then. pending_name / pending_kind are only a hand-over: request_* files them under
			// this player's request (pending/save).
			//
			// A declared number gets the number form, bounded by its declaration (#dh.min/#dh.max
			// from probe). A guessed one is typed in the book instead and parsed on the way back,
			// since nothing says what range it may take.
			raw("scoreboard players operation @s ra.dh.pending = @s ra.dh.action")
			raw("data modify storage ra:dh pending_kind set value \"text\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 1 if score #dh.decl ${Obj.TEMP} matches 1 run data modify storage ra:dh pending_kind set value \"number\"")
			raw("execute if score #dh.type ${Obj.TEMP} matches 3 run data modify storage ra:dh pending_kind set value \"list\"")
			executeReturnRun("if data storage ra:dh {pending_kind:\"number\"}", DataHandlerProps.askNumber, "with storage ra:dh q")
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 1", DataHandlerProps.askTypedNumber, "with storage ra:dh q")
			callWith(DataHandlerProps.askText, "storage ra:dh q")
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

			// The checks again, against the block the value is about to land on and the schema as
			// loaded now: read-only, admin-only (does this player still hold ra.admin?), hidden.
			callWith(propertyGone, "storage ra:dh q")
			raw("execute if score #dh.gone ${Obj.TEMP} matches 1 run return 0")
			callWith(DataHandlerProps.probe, "storage ra:dh q")
			callWith(DataHandlerProps.refuse, "storage ra:dh q")
			raw("execute store success score #dh.den ${Obj.TEMP} run data remove storage ra:dh denied")
			raw("execute if score #dh.den ${Obj.TEMP} matches 1 run return 0")

			// One axis of a vec3 (DataHandlerVec3), checked against its range as it is now.
			executeReturnRun("if data storage ra:dh cur{kind:\"vec3\"}", DataHandlerVec3.applyTyped)
			// One part of a facing (DataHandlerFacing), the same way.
			executeReturnRun("if data storage ra:dh cur{kind:\"facing\"}", DataHandlerFacing.applyTyped)
			// The number form's answer, checked against the bounds probe just read.
			executeReturnRun("if data storage ra:dh cur{kind:\"number\"}", DataHandlerProps.writeNumber, "with storage ra:dh q")

			// A typed answer becomes the property's type as it is NOW -- declared, or guessed from the
			// value the block holds at this moment. Text is copied as text, never parsed, so a number
			// typed for a text property stays text; a number, flag or list is parsed and checked,
			// and a refusal leaves the block as it was.
			raw("execute unless data storage ra:input consume.text run return 0")
			raw("data modify storage ra:dh q.text set from storage ra:input consume.text")
			// An answer for one entry of a list (DataHandlerEntries).
			executeReturnRun("if data storage ra:dh cur{kind:\"entry\"}", DataHandlerEntries.applyEntry)
			executeReturnRun("if data storage ra:dh cur{kind:\"append\"}", DataHandlerEntries.applyEntry)
			// A colour, parsed one character at a time (ColorParse), never pasted.
			executeReturnRun("if data storage ra:dh cur{kind:\"color\"}", DataHandlerColor.applyTyped)
			// A direction, facing or colour answered by another form (its type changed with a /reload):
			// typed text is none of them.
			raw("""execute if score #dh.type ${Obj.TEMP} matches 7..9 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is not edited this way any more; nothing was changed.",color:"red"}]""")
			raw("execute if score #dh.type ${Obj.TEMP} matches 7..9 run return 0")
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 1", DataHandlerTypes.writeTypedNumber, "with storage ra:dh q")
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 2", DataHandlerTypes.writeTypedBool, "with storage ra:dh q")
			executeReturnRun("if score #dh.type ${Obj.TEMP} matches 3", DataHandlerList.apply)
			raw("""execute if score #dh.type ${Obj.TEMP} matches 5 run tellraw @s [$PREFIX,{text:"A compound cannot be edited here. Use the Creative Data Handler.",color:"red"}]""")
			raw("execute if score #dh.type ${Obj.TEMP} matches 5 run return 0")
			// Text is written as typed, so it must be in the property's format (TextFormat): it may be
			// pasted into a command later.
			raw("data modify storage ra:safe s set from storage ra:dh q.text")
			raw("execute store result score #dh.fok ${Obj.TEMP} run function ${DataHandlerEntries.checkFormat.asId()}")
			raw("execute if score #dh.fok ${Obj.TEMP} matches 0 run return 0")
			// One per world (a Teleport Anchor's id): refused when another block has it already.
			raw("execute store result score #dh.fok ${Obj.TEMP} run function ${DataHandlerUnique.check.asId()} with storage ra:dh q")
			raw("execute if score #dh.fok ${Obj.TEMP} matches 0 run return 0")
			callWith(DataHandlerProps.writeText, "storage ra:dh q")
			raw("return 1")
		}

		// {name}: #dh.gone = 1, with a message, when the block no longer has the property: there is
		// no value left to take the type of.
		pack.define(propertyGone) {
			raw("scoreboard players set #dh.gone ${Obj.TEMP} 0")
			// Declared but not set (and no default): still editable, the edit writes it.
			raw("\$execute unless data storage ra:dh properties.\$(name) unless data storage ra:dh schema.\$(name).type run scoreboard players set #dh.gone ${Obj.TEMP} 1")
			raw("""${'$'}execute if score #dh.gone ${Obj.TEMP} matches 1 run tellraw @s [$PREFIX,{text:"This block no longer has ",color:"red"},{text:"$(name)",color:"yellow"},{text:"; nothing was changed.",color:"red"}]""")
		}

		// Open an input request for the pending property edit: mode 3 numeric, mode 2 free text.
		// The number form takes the bounds probe left in #dh.min / #dh.max for this property.
		for ((fn, mode) in listOf(requestNumber to 3, requestText to 2))
			pack.define(fn) {
				// Whatever this player was asked before is over: its session is cancelled next.
				function(pendingForget)
				function(RaLib.Input.cancel)
				scoreboard.players.set(self(), "ra.input.mode", mode)
				if (fn == requestNumber) {
					raw("scoreboard players operation @s ra.input.min = #dh.min ${Obj.TEMP}")
					raw("scoreboard players operation @s ra.input.max = #dh.max ${Obj.TEMP}")
				} else {
					scoreboard.players.set(self(), "ra.input.min", 0)
					scoreboard.players.set(self(), "ra.input.max", 120)
				}
				function(RaLib.Input.sessionCreate)
				function(RaLib.Input.routerSelectBackend)
				function(RaLib.Input.routerOpen)
				// Which request is ours, and what it asks.
				raw("scoreboard players operation @s $REQ = @s ra.input.req")
				function(pendingSave)
			}
	}
}
