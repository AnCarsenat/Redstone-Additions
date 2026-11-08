package ra.lib.input.session

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import net.benwoodworth.knbt.NbtString
import ra.api.RaLib
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.fake
import ra.lib.input.IN
import ra.lib.input.InputObj
import ra.lib.input.backend.BookBackend
import ra.lib.input.inputMessage
import ra.lib.input.storeReq
import ra.lib.lib

/** `ra_lib:input/session/...`: one player's session, its scores and its `sessions.req_<id>` bucket. */
object Session {
	val cleanup = lib("input/session/cleanup")
	val timeout = lib("input/session/timeout")
	val storageCreate = lib("input/session/storage_create")
	val storageRemove = lib("input/session/storage_remove")
	val copyTextToConsume = lib("input/session/storage_copy_text_to_consume")
	val copyTextToConsumePrepare = lib("input/session/storage_copy_text_to_consume_prepare")

	fun Ra.registerSession() {
		// Create a new input session for the executing player.
		pack.define(RaLib.Input.sessionCreate) {
			tag(self()) { add(InputObj.ACTIVE) }

			scoreboard.players.set(self(), InputObj.STATE, 1)
			scoreboard.players.set(self(), InputObj.RESULT, -1)
			scoreboard.players.set(self(), InputObj.BACKEND, 0)
			scoreboard.players.set(self(), InputObj.TTL, 600)
			scoreboard.players.set(self(), InputObj.VALUE, 0)

			// Use an int-min sentinel to detect changes from /trigger, including submissions of 0.
			// Setting a score does not enable it: the trigger backend's open_choice does, so a text
			// prompt does not put ra.input.trigger in the player's /trigger list.
			scoreboard.players.set(self(), InputObj.TRIGGER, InputObj.SENTINEL)
			scoreboard.players.set(self(), InputObj.LAST, InputObj.SENTINEL)

			// A player only ever has one session, so any Input Form already on them is left over
			// from an earlier one (fetched back out of a chest, say). It must not become a free book.
			raw("clear @s ${BookBackend.ANY_FORM_ITEM}")

			scoreboard.players.operation(self(), InputObj.REQ, Operation.SET, fake("#next"), InputObj.NEXT)
			scoreboard.players.add(fake("#next"), InputObj.NEXT, 1)

			storeReq()
			raw("execute store result storage ra:input tmp.mode int 1 run scoreboard players get @s ${InputObj.MODE}")
			function(storageCreate, IN, "tmp")
			data(IN) { remove("tmp") }
		}

		// Clean all state for the executing player's input session.
		pack.define(cleanup) {
			tag(self()) { remove(InputObj.ACTIVE) }

			storeReq()
			execute {
				ifCondition { score(self(), InputObj.BACKEND) equalTo 2 }
				run { function(BookBackend.killDroppedReq, IN, "tmp") }
			}
			execute {
				ifCondition { score(self(), InputObj.BACKEND) equalTo 2 }
				run { function(BookBackend.clearBookReq, IN, "tmp") }
			}
			function(storageRemove, IN, "tmp")
			data(IN) { remove("tmp") }

			for ((objective, value) in listOf(
				InputObj.STATE to 0, InputObj.BACKEND to 0, InputObj.MODE to 0, InputObj.MIN to 0, InputObj.MAX to 0,
				InputObj.RESULT to -1, InputObj.TTL to 0, InputObj.REQ to 0, InputObj.LAST to InputObj.SENTINEL,
				InputObj.VALUE to 0,
			)) scoreboard.players.set(self(), objective, value)

			// RESET, not a sentinel: a reset score is no longer enabled, so ra.input.trigger leaves
			// the player's /trigger list once the prompt is over. Setting it kept it listed forever
			// after a player's first prompt. sessionCreate sets the sentinel again before any read.
			scoreboard.players.reset(self(), InputObj.TRIGGER)
		}

		// Expire an active session that exceeded its TTL.
		pack.define(timeout) {
			inputMessage("Input request expired.", "red")
			function(cleanup)
		}

		// Macro storage shape: {req, mode}.
		pack.define(storageCreate) {
			raw("\$data modify storage ra:input sessions.req_\$(req) set value {mode:\$(mode),backend:0,state:\"waiting\",result:{}}")
			raw("\$data modify storage ra:input sessions.req_\$(req).owner set from entity @s UUID")
		}

		// Macro storage shape: {req}.
		pack.define(storageRemove) { raw("\$data remove storage ra:input sessions.req_\$(req)") }

		// Macro storage shape: {req}.
		pack.define(copyTextToConsume) {
			raw("\$data modify storage ra:input consume.text set from storage ra:input sessions.req_\$(req).result.text")
		}

		// Copy text from the player's session bucket into storage ra:input consume.text.
		pack.define(copyTextToConsumePrepare) {
			storeReq()
			function(copyTextToConsume, IN, "tmp")
			execute {
				unlessCondition { data(IN, "consume.text") }
				run { data(IN) { modify("consume.text") { set(NbtString("")) } } }
			}
			data(IN) { remove("tmp") }
		}
	}
}
