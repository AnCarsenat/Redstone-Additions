package ra.lib.input.router

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.input.IN
import ra.lib.input.Input.MODE_CHOICE
import ra.lib.input.Input.MODE_NUMBER
import ra.lib.input.Input.MODE_TEXT
import ra.lib.input.InputObj
import ra.lib.input.backend.BookBackend
import ra.lib.input.backend.TriggerBackend
import ra.lib.input.storeReq
import ra.lib.lib

/** `ra_lib:input/router/...`: picks a backend for the session's mode and forwards to it. */
object Router {
	val tick = lib("input/router/tick")
	val storeBackend = lib("input/router/store_backend")

	const val TRIGGER = 1
	const val BOOK = 2

	fun Ra.registerRouter() {
		// Open the selected backend's UI for the current mode.
		pack.define(RaLib.Input.routerOpen) {
			for ((backend, mode, fn) in listOf(
				Triple(TRIGGER, MODE_CHOICE, TriggerBackend.openChoice),
				Triple(TRIGGER, MODE_NUMBER, TriggerBackend.openChoice),
				Triple(BOOK, MODE_TEXT, BookBackend.openText),
			)) execute {
				ifCondition { score(self(), InputObj.BACKEND) equalTo backend }
				ifCondition { score(self(), InputObj.MODE) equalTo mode }
				run { function(fn) }
			}
			// The caller's label (InputRange) is for this prompt only.
			raw("data remove storage ra:input label")
		}

		// Select the backend for the current mode. Auto strategy: numeric -> trigger, text -> writable_book.
		pack.define(RaLib.Input.routerSelectBackend) {
			scoreboard.players.set(self(), InputObj.BACKEND, 0)

			for ((mode, backend) in listOf(MODE_CHOICE to TRIGGER, MODE_NUMBER to TRIGGER, MODE_TEXT to BOOK)) execute {
				ifCondition { score(self(), InputObj.MODE) equalTo mode }
				run { scoreboard.players.set(self(), InputObj.BACKEND, backend) }
			}

			execute {
				unlessCondition { score(self(), InputObj.BACKEND) matches TRIGGER..BOOK }
				run { scoreboard.players.set(self(), InputObj.BACKEND, TRIGGER) }
			}

			storeReq()
			raw("execute store result storage ra:input tmp.backend int 1 run scoreboard players get @s ${InputObj.BACKEND}")
			function(storeBackend, IN, "tmp")
			data(IN) { remove("tmp") }
		}

		// Macro storage shape: {req, backend}.
		pack.define(storeBackend) { raw("\$data modify storage ra:input sessions.req_\$(req).backend set value \$(backend)") }

		// Tick the selected backend for the current player's session.
		pack.define(tick) {
			execute {
				ifCondition { score(self(), InputObj.BACKEND) equalTo TRIGGER }
				run { function(TriggerBackend.tick) }
			}
			execute {
				ifCondition { score(self(), InputObj.BACKEND) equalTo BOOK }
				run { function(BookBackend.tickScan) }
			}
		}
	}
}
