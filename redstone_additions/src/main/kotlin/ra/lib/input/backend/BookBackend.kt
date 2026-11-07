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

	/** The form's custom_data, as an NBT compound in an Items filter. `$(req)` is a macro argument. */
	private const val FORM_DATA = "\"minecraft:custom_data\":{ra:{input_book:1b,input_req:\$(req)}}"

	/** The player's Inventory entry holding this request's form. */
	private const val FORM_SLOT = "Inventory[{id:\"minecraft:writable_book\",components:{$FORM_DATA}}]"

	/** Main inventory and hotbar slots give_book_safe checks for room. */
	private val playerSlots = 0..35

	fun Ra.registerBookBackend() {
		// Prompt the player to write text in a temporary writable book.
		pack.define(openText) {
			storeReq()
			function(giveBookSafe, IN, "tmp")
			data(IN) { remove("tmp") }

			raw("tellraw @s \"\"")
			inputMessage("Text input requested.", "aqua")
			raw("""tellraw @s [{text:"Write on page 1 of the Input Form book.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Close the book when done. Capture runs automatically.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Drop the book to cancel.",color:"dark_gray"}]""")
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

			// Keep the request book available while waiting without overwriting the current hand.
			raw("\$execute unless data entity @s $FORM_SLOT run ${macroCall(giveBookSafe, "{req:\$(req)}")}")

			raw("\$execute if data entity @s Inventory[{id:\"minecraft:writable_book\",components:{$FORM_DATA,\"minecraft:writable_book_content\":{pages:[{}]}}}] run ${macroCall(submit, "{req:\$(req)}")}")
		}

		// `submit {req}`: commit the text from the matching writable book into the session result.
		pack.define(submit) {
			raw("\$data remove storage ra:input sessions.req_\$(req).result.text")

			raw("\$data modify storage ra:input sessions.req_\$(req).result.text set from entity @s $FORM_SLOT.components.\"minecraft:writable_book_content\".pages[0].raw")
			raw("\$execute unless data storage ra:input sessions.req_\$(req).result.text run data modify storage ra:input sessions.req_\$(req).result.text set from entity @s $FORM_SLOT.components.\"minecraft:writable_book_content\".pages[0]")
			raw("\$execute unless data storage ra:input sessions.req_\$(req).result.text run data modify storage ra:input sessions.req_\$(req).result.text set value \"\"")

			raw("\$data modify storage ra:input sessions.req_\$(req).state set value \"ready\"")

			raw("\$" + macroCall(killDroppedReq, "{req:\$(req)}"))
			raw("\$" + macroCall(clearBookReq, "{req:\$(req)}"))

			scoreboard.players.set(self(), InputObj.RESULT, 1)
			scoreboard.players.set(self(), InputObj.STATE, 2)
			tag(self()) { remove(InputObj.ACTIVE) }

			inputMessage("Text captured.", "green")
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

		// `kill_dropped_req {req}`: kill this request's form lying nearby, and any stray form.
		pack.define(killDroppedReq) {
			raw("\$execute at @s as @e[type=item,distance=..8,nbt={Item:{id:\"minecraft:writable_book\"}}] if data entity @s {Item:{components:{$FORM_DATA}}} run kill @s")
			raw("execute at @s as @e[type=item,distance=..8,nbt={Item:{id:\"minecraft:writable_book\"}}] if data entity @s {Item:{components:{\"minecraft:custom_data\":{ra:{input_book:1b}}}}} run kill @s")
		}

		// `clear_book_req {req}`: remove this request's form from the player's inventory.
		pack.define(clearBookReq) {
			raw("\$clear @s minecraft:writable_book[custom_data={ra:{input_book:1b,input_req:\$(req)}}]")
		}
	}
}
