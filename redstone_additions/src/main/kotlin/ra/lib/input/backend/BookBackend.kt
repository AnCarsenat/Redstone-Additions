package ra.lib.input.backend

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.fake
import ra.lib.input.IN
import ra.lib.input.InputObj
import ra.lib.input.inputMessage
import ra.lib.input.storeReq
import ra.lib.lib
import ra.core.macroCall

/**
 * `ra_lib:input/backend/writable_book/...`: free text, written on page 1 of an "Input Form"
 * writable book. The book carries `custom_data={ra:{input_book:1b,input_req:<req>}}`, which is how
 * the session finds it again.
 */
object BookBackend {
	val openText = lib("input/backend/writable_book/open_text")
	val giveBookSafe = lib("input/backend/writable_book/give_book_safe")
	val tickScan = lib("input/backend/writable_book/tick_scan")
	val tickScanReq = lib("input/backend/writable_book/tick_scan_req")
	val submit = lib("input/backend/writable_book/submit")
	val cancelDropped = lib("input/backend/writable_book/cancel_dropped")
	val killDroppedReq = lib("input/backend/writable_book/kill_dropped_req")
	val clearBookReq = lib("input/backend/writable_book/clear_book_req")
	val cancelGone = lib("input/backend/writable_book/cancel_gone")

	/** `storage ra:input <SCAN>`: the written form, copied out of whichever slot holds it. */
	private const val SCAN = "scan_form"

	/** The form's custom_data, as an NBT compound in an Items filter. `$(req)` is a macro argument. */
	private const val FORM_DATA = "\"minecraft:custom_data\":{ra:{input_book:1b,input_req:\$(req)}}"

	/** This request's form once something is written on it, as an NBT compound filter. */
	private const val FORM_WRITTEN = "{id:\"minecraft:writable_book\",components:{$FORM_DATA,\"minecraft:writable_book_content\":{pages:[{}]}}}"

	/** This request's form, as an item predicate (`execute if items`, `clear`). */
	private const val FORM_ITEM = "minecraft:writable_book[custom_data={ra:{input_book:1b,input_req:\$(req)}}]"

	/** Any Input Form, whatever its request. */
	internal const val ANY_FORM_ITEM = "minecraft:writable_book[custom_data~{ra:{input_book:1b}}]"

	/**
	 * Where the player can have the form while it is still theirs: inventory, offhand, the item
	 * held on the cursor with a screen open, and the 2x2 crafting grid. Slots of an open chest,
	 * bundle contents and lecterns are not the player's: a form put there is gone.
	 */
	private val heldSlots = listOf("container.*", "weapon.offhand", "player.cursor", "player.crafting.*")

	/** Book sessions expire this long after the player last held the form (5 minutes). */
	const val BOOK_TTL = 6000

	/** Ticks the form may be missing (a creative-mode inventory shuffle) before the session is cancelled. */
	private const val GONE_GRACE = 40

	/** Main inventory and hotbar slots give_book_safe checks for room. */
	private val playerSlots = 0..35

	fun Ra.registerBookBackend() {
		// Prompt the player to write text in a temporary writable book.
		pack.define(openText) {
			// Writing takes longer than typing a number; tick_scan_req keeps refreshing this while
			// the form is in hand.
			scoreboard.players.set(self(), InputObj.TTL, BOOK_TTL)

			storeReq()
			function(giveBookSafe, IN, "tmp")
			data(IN) { remove("tmp") }

			raw("tellraw @s \"\"")
			inputMessage("Text input requested.", "aqua")
			raw("""tellraw @s [{text:"Write on page 1 of the Input Form book.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Close the book when done. Capture runs automatically.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Drop the book or put it away to cancel.",color:"dark_gray"}]""")
		}

		// `give_book_safe {req}`: hand out the form, but only into an empty slot.
		pack.define(giveBookSafe) {
			// ra.temp: 1 when at least one main inventory/hotbar slot is empty.
			scoreboard.players.set(self(), Obj.TEMP, 0)
			for (slot in playerSlots) execute {
				unlessCondition { data(self(), "Inventory[{Slot:${slot}b}]") }
				run { scoreboard.players.set(self(), Obj.TEMP, 1) }
			}

			raw("\$execute if score @s ${Obj.TEMP} matches 1 run give @s minecraft:writable_book[item_name=\"Input Form\",lore=[{text:\"Write text on page 1\",italic:false,color:\"gray\"},{text:\"Close the book to submit\",italic:false,color:\"gray\"}],custom_data={ra:{input_book:1b,input_req:\$(req)}}]")

			// The form is handed out ONCE per session. From then on a missing form means the player
			// put it away (chest, bundle, lectern) or lost it, and the session is cancelled instead
			// of re-giving: re-giving made a duplicate each time, and the copy left in the chest
			// survived the session as a free writable book.
			raw("\$execute if score @s ${Obj.TEMP} matches 1 run data modify storage ra:input sessions.req_\$(req).form_given set value 1b")
			raw("\$execute if score @s ${Obj.TEMP} matches 1 run data remove storage ra:input sessions.req_\$(req).inventory_full_warned")
			raw("\$execute if score @s ${Obj.TEMP} matches 0 unless data storage ra:input sessions.req_\$(req).inventory_full_warned run tellraw @s [{text:\"[RA Input] \",color:\"gold\"},{text:\"Inventory full. Free one slot for Input Form.\",color:\"red\"}]")
			raw("\$execute if score @s ${Obj.TEMP} matches 0 run data modify storage ra:input sessions.req_\$(req).inventory_full_warned set value 1b")
		}

		// Poll the player's inventory for the matching writable input book.
		pack.define(tickScan) {
			storeReq()
			function(tickScanReq, IN, "tmp")
			data(IN) { remove("tmp") }
		}

		// `tick_scan_req {req}`.
		pack.define(tickScanReq) {
			val dropped = fake("#input_dropped")

			// Dropping the form cancels the request. This has to be tested before anything else
			// touches the item: the previous order killed the dropped book and then handed a fresh
			// one straight back, which left no way out of a request short of waiting for the timeout.
			scoreboard.players.set(dropped, Obj.TEMP, 0)
			raw("\$execute at @s as @e[type=item,distance=..8] if data entity @s {Item:{id:\"minecraft:writable_book\",components:{$FORM_DATA}}} run scoreboard players set #input_dropped ${Obj.TEMP} 1")

			execute {
				ifCondition { score(dropped, Obj.TEMP) equalTo 1 }
				run { function(cancelDropped) }
			}
			execute {
				ifCondition { score(dropped, Obj.TEMP) equalTo 1 }
				run { returnValue(0) }
			}

			// Is the form still the player's? Anywhere in heldSlots counts, so moving it to the
			// offhand (F) or carrying it on the cursor between slots does not cancel anything.
			val has = fake("#input_has")
			scoreboard.players.set(has, Obj.TEMP, 0)
			for (slot in heldSlots) raw("\$execute if items entity @s $slot $FORM_ITEM run scoreboard players set #input_has ${Obj.TEMP} 1")
			// A form found before form_given existed (session opened by an older build) is the given one.
			raw("\$execute if score #input_has ${Obj.TEMP} matches 1 run data modify storage ra:input sessions.req_\$(req).form_given set value 1b")

			// Not handed out yet (inventory was full): keep trying, without touching the hand.
			// #input_has 2 = still to be handed out.
			raw("\$execute unless data storage ra:input sessions.req_\$(req).form_given run scoreboard players set #input_has ${Obj.TEMP} 2")
			raw("\$execute if score #input_has ${Obj.TEMP} matches 2 run ${macroCall(giveBookSafe, "{req:\$(req)}")}")
			raw("execute if score #input_has ${Obj.TEMP} matches 2 run return 0")

			// Handed out and gone: cancel after a short grace, like dropping it. VALUE is the
			// trigger backend's scratch score; the book backend never reads it otherwise.
			execute {
				ifCondition { score(has, Obj.TEMP) equalTo 1 }
				run { scoreboard.players.set(self(), InputObj.VALUE, 0) }
			}
			execute {
				ifCondition { score(has, Obj.TEMP) equalTo 0 }
				run { scoreboard.players.add(self(), InputObj.VALUE, 1) }
			}
			raw("execute if score #input_has ${Obj.TEMP} matches 0 if score @s ${InputObj.VALUE} matches $GONE_GRACE.. run ${macroCall(cancelGone)}")
			raw("execute if score #input_has ${Obj.TEMP} matches 0 run return 0")

			// Holding the form (selected or offhand) is writing it: the session only runs out once
			// the player has left it alone for BOOK_TTL. A fixed 30 s from opening cut people off
			// mid-sentence.
			raw("\$execute if items entity @s weapon.* $FORM_ITEM run scoreboard players set @s ${InputObj.TTL} $BOOK_TTL")

			// Written on: submit, from the inventory or the offhand (26.3 keeps the offhand in
			// equipment.offhand, not in Inventory, so a form written in the left hand was never read).
			data(IN) { remove(SCAN) }
			raw("\$execute if data entity @s Inventory[$FORM_WRITTEN] run data modify storage ra:input $SCAN set from entity @s Inventory[$FORM_WRITTEN]")
			raw("\$execute unless data storage ra:input $SCAN if data entity @s equipment.offhand$FORM_WRITTEN run data modify storage ra:input $SCAN set from entity @s equipment.offhand")
			raw("\$execute if data storage ra:input $SCAN run ${macroCall(submit, "{req:\$(req)}")}")
			data(IN) { remove(SCAN) }
		}

		// `submit {req}`: commit the text of the form copied to storage ra:input scan_form into the session result.
		pack.define(submit) {
			raw("\$data remove storage ra:input sessions.req_\$(req).result.text")

			raw("\$data modify storage ra:input sessions.req_\$(req).result.text set from storage ra:input $SCAN.components.\"minecraft:writable_book_content\".pages[0].raw")
			raw("\$execute unless data storage ra:input sessions.req_\$(req).result.text run data modify storage ra:input sessions.req_\$(req).result.text set from storage ra:input $SCAN.components.\"minecraft:writable_book_content\".pages[0]")
			raw("\$execute unless data storage ra:input sessions.req_\$(req).result.text run data modify storage ra:input sessions.req_\$(req).result.text set value \"\"")

			raw("\$data modify storage ra:input sessions.req_\$(req).state set value \"ready\"")

			raw("\$" + macroCall(killDroppedReq, "{req:\$(req)}"))
			raw("\$" + macroCall(clearBookReq, "{req:\$(req)}"))

			scoreboard.players.set(self(), InputObj.RESULT, 1)
			scoreboard.players.set(self(), InputObj.STATE, 2)
			tag(self()) { remove(InputObj.ACTIVE) }

			inputMessage("Text captured.", "green")
		}

		// The form left the player's inventory (chest, bundle, lectern, thrown far). Same outcome
		// as dropping it. Context: as the player.
		pack.define(cancelGone) {
			inputMessage("Input Form put away: input cancelled.", "gray")
			raw("playsound minecraft:block.note_block.bass block @s ~ ~ ~ 0.5 0.8")
			function(RaLib.Input.cancel)
		}

		// The player threw the Input Form away. Treat that as "never mind" and tear the session
		// down. Context: as the player.
		//
		// ra_lib:input/cancel runs session/cleanup, which for this backend kills the dropped item
		// and clears any copy still in the inventory, so nothing is left lying on the floor.
		pack.define(cancelDropped) {
			inputMessage("Input cancelled.", "gray")
			raw("playsound minecraft:block.note_block.bass block @s ~ ~ ~ 0.5 0.8")

			function(RaLib.Input.cancel)
		}

		// `kill_dropped_req {req}`: kill this request's form lying nearby. Only this request's: a
		// catch-all for any form also killed another player's dropped form before their own scan
		// saw it, so their cancel never happened.
		pack.define(killDroppedReq) {
			raw("\$execute at @s as @e[type=item,distance=..8,nbt={Item:{id:\"minecraft:writable_book\"}}] if data entity @s {Item:{components:{$FORM_DATA}}} run kill @s")
		}

		// `clear_book_req {req}`: remove this request's form from the player's inventory, and any
		// older form they fetched back from a chest: a player only ever has one session.
		pack.define(clearBookReq) {
			raw("\$clear @s $FORM_ITEM")
			raw("clear @s $ANY_FORM_ITEM")
		}
	}
}
