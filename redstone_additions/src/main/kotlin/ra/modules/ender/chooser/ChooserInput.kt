package ra.modules.ender.chooser

import io.github.ayfri.kore.commands.function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeStoreRun
import ra.core.raw
import ra.core.ref
import ra.modules.ender.AnchorTerminal
import ra.modules.ender.chooser.Chooser.SRC
import ra.modules.ender.chooser.Chooser.say
import ra.modules.ender.tablet.AnchorTablet

/**
 * [Type a destination]: the anchor id through the shared Input Form book (ra_lib:input), as the
 * Redstone Remote asks for its channel. No operator rights, no `/function` in anyone's chat box.
 *
 * The answer arrives ticks later, so where it applies is checked again then: the player must still
 * stand on an Anchor Terminal, or the tablet must still be in the hotbar slot it was in when the
 * book opened ([AnchorTablet.SLOT]; the book itself takes the hand while it is written).
 */
object ChooserInput {
	private fun fn(path: String) = ref(Ns.ENDER, "chooser/$path")

	/** As a player, from the menu: open the book. */
	val ask = fn("ask")
	/** As a player whose [PEND] is 1: apply a finished book, if it is ours. */
	val answer = fn("answer")

	/** Waiting for a book. */
	const val PEND = "ra.ender.pend"
	/** The ra_lib:input request opened FOR THE MENU, so it never consumes another tool's answer. */
	const val REQ = "ra.ender.req"

	private const val T = Obj.TEMP

	fun Ra.registerChooserInput() {
		pack.define(ask) {
			// Drop any half-finished request before starting a new one.
			function(RaLib.Input.cancel)
			raw("scoreboard players set @s $PEND 1")
			function(RaLib.Input.openText)
			raw("scoreboard players operation @s $REQ = @s ra.input.req")
			raw("""tellraw @s [{text:"Write the id of the anchor to go to (",color:"gray"},{text:"Home",color:"aqua"},{text:", ",color:"gray"},{text:"mine_2",color:"aqua"},{text:"...).",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.bell block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
		}

		pack.define(answer) {
			// Cancelled (the form was dropped): stop waiting. `submit` clears ra.input.active but
			// leaves state at 2, hence the second condition.
			raw("execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return run scoreboard players set @s $PEND 0")
			// Not our request: ours was cancelled when that one opened.
			raw("execute unless score @s $REQ = @s ra.input.req run return run scoreboard players set @s $PEND 0")

			executeStoreRun("", "#ender.st", RaLib.Input.poll)
			raw("execute unless score #ender.st $T matches 2 run return 0")
			executeStoreRun("", "#ender.st", RaLib.Input.consume)
			raw("execute unless score #ender.st $T matches 1 run return 0")
			raw("scoreboard players set @s $PEND 0")

			raw("""execute if data storage ra:input consume{text:""} run return run ${say("No destination entered.")}""")

			// Still where the menu was opened from?
			raw("data modify storage ra:ender menu set value {}")
			raw("scoreboard players set #ender.ctx $T 0")
			raw("execute if score @s $SRC matches 1 store result score #ender.ctx $T run function ${AnchorTerminal.find.asId()}")
			raw("execute if score @s $SRC matches 2 store result score #ender.ctx $T run function ${AnchorTablet.loadSlot.asId()}")
			raw("execute if score #ender.ctx $T matches 0 if score @s $SRC matches 1 run return run ${say("You left the ${AnchorTerminal.block.name}: nobody was sent.", "red")}")
			raw("execute if score #ender.ctx $T matches 0 if score @s $SRC matches 2 run return run ${say("The Anchor Tablet left its hotbar slot: nobody was sent.", "red")}")

			raw("data remove storage ra:ender lookup")
			raw("data modify storage ra:ender lookup.id set from storage ra:input consume.text")
			raw("function ${Chooser.go.asId()}")
		}
	}
}
