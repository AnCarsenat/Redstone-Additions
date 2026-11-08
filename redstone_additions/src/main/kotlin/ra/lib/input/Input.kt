package ra.lib.input

import io.github.ayfri.kore.arguments.scores.ScoreboardCriteria
import io.github.ayfri.kore.arguments.scores.score
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.dummyObjectives
import ra.lib.fake
import ra.lib.lib
import ra.lib.input.backend.BookBackend
import ra.lib.input.backend.BookBackend.registerBookBackend
import ra.lib.input.backend.TriggerBackend.registerTriggerBackend
import ra.lib.input.router.Router
import ra.lib.input.router.Router.registerRouter
import ra.lib.input.session.Session
import ra.lib.input.session.Session.registerSession
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd

/**
 * `ra_lib:input/...`: typed player input sessions. A caller opens a request (`open_number`,
 * `open_text`, `open_choice`) as the player, polls it, then consumes the result. The router picks a
 * backend per mode: numbers go through `/trigger`, text through a writable book.
 */
object Input {
	val init = lib("input/init")

	/** Mode ids, as stored in ra.input.mode. */
	internal const val MODE_CHOICE = 1
	internal const val MODE_TEXT = 2
	internal const val MODE_NUMBER = 3

	fun Ra.registerInput() {
		// Initialize the input scoreboards and storage.
		pack.define(init) {
			dummyObjectives(
				InputObj.STATE, InputObj.BACKEND, InputObj.MODE, InputObj.MIN, InputObj.MAX, InputObj.RESULT,
				InputObj.TTL, InputObj.REQ, InputObj.NEXT, InputObj.LAST, InputObj.VALUE,
			)
			scoreboard.objectives.add(InputObj.TRIGGER, ScoreboardCriteria.TRIGGER)

			val next = fake("#next")
			scoreboard.players.add(next, InputObj.NEXT, 0)
			execute {
				unlessCondition { score(next, InputObj.NEXT) greaterThanOrEqualTo 1 }
				run { scoreboard.players.set(next, InputObj.NEXT, 1) }
			}

			// Earlier builds left ra.input.trigger enabled after a player's first prompt. Take it
			// off the /trigger list of everyone online without a prompt open (session/cleanup does
			// it for the rest when their prompt ends).
			raw("scoreboard players reset @a[tag=!${InputObj.ACTIVE}] ${InputObj.TRIGGER}")

			data(IN) {
				merge(nbt {
					this["sessions"] = nbt { }
					this["consume"] = nbt { }
				})
			}
		}

		pack.define(RaLib.Input.tick) { tick() }

		// Open a numeric choice request for the executing player. Defaults: range 0..9.
		pack.define(RaLib.Input.openChoice) { open(MODE_CHOICE, 0, 9) }
		// Open a numeric input request for the executing player. Defaults: range 1..9.
		pack.define(RaLib.Input.openNumber) { open(MODE_NUMBER, 1, 9) }
		// Open a free-text request for the executing player. Defaults: max chars 120.
		pack.define(RaLib.Input.openText) { open(MODE_TEXT, 0, 120) }

		// Return the current state for the executing player: 0 = idle, 1 = waiting, 2 = ready.
		pack.define(RaLib.Input.poll) { returnRun { scoreboard.players.get(self(), InputObj.STATE) } }

		pack.define(RaLib.Input.consume) { consume() }

		// Cancel and clean up the current input session for the executing player.
		pack.define(RaLib.Input.cancel) {
			function(Session.cleanup)
			returnValue(1)
		}

		registerSession()
		registerRouter()
		registerTriggerBackend()
		registerBookBackend()
	}

	/** An `open_*` request: mode and default range, then session, backend and UI. The backend is auto-selected by mode. */
	private fun Function.open(mode: Int, min: Int, max: Int) {
		scoreboard.players.set(self(), InputObj.MODE, mode)
		scoreboard.players.set(self(), InputObj.MIN, min)
		scoreboard.players.set(self(), InputObj.MAX, max)

		function(RaLib.Input.sessionCreate)
		function(RaLib.Input.routerSelectBackend)
		function(RaLib.Input.routerOpen)
	}

	/** Tick active input sessions. */
	private fun Function.tick() {
		// An Input Form outside its session is inert: a form put in a chest during a prompt is
		// taken away again the moment a player without an open prompt picks it up, so it never
		// becomes a free writable book. One command; `clear` also reaches the cursor and the
		// crafting grid.
		raw("clear @a[tag=!${InputObj.ACTIVE}] ${BookBackend.ANY_FORM_ITEM}")

		execute {
			asTarget(allPlayers { tag = InputObj.ACTIVE })
			run { function(Router.tick) }
		}

		// A session that just produced a result (state 2) gets a fresh consume window, so a caller
		// that polls less often than every tick cannot lose the answer to an expiry that happens to
		// land on the same tick. The window is capped, so an answer nobody ever consumes still gets
		// collected instead of leaking the session forever.
		execute {
			asTarget(allPlayers { tag = InputObj.ACTIVE; scores { score(InputObj.STATE, 2); score(InputObj.TTL, rangeOrIntEnd(99)) } })
			run { scoreboard.players.set(self(), InputObj.TTL, 100) }
		}

		execute {
			asTarget(allPlayers { tag = InputObj.ACTIVE })
			run { scoreboard.players.remove(self(), InputObj.TTL, 1) }
		}

		// Waiting sessions report the expiry to the player; unconsumed results are dropped silently,
		// since from the player's side the interaction already finished.
		execute {
			asTarget(allPlayers { tag = InputObj.ACTIVE; scores { score(InputObj.STATE, 1); score(InputObj.TTL, rangeOrIntEnd(0)) } })
			run { function(Session.timeout) }
		}
		execute {
			asTarget(allPlayers { tag = InputObj.ACTIVE; scores { score(InputObj.STATE, 2); score(InputObj.TTL, rangeOrIntEnd(0)) } })
			run { function(Session.cleanup) }
		}
	}

	/**
	 * Consume a ready result and write normalized output to storage ra:input consume.
	 * Returns 1 when a result was consumed, 0 otherwise.
	 */
	private fun Function.consume() {
		data(IN) { remove("consume") }

		execute {
			unlessCondition { score(self(), InputObj.STATE) equalTo 2 }
			run { returnValue(0) }
		}

		for ((key, objective) in listOf("request" to InputObj.REQ, "mode" to InputObj.MODE, "backend" to InputObj.BACKEND)) {
			raw("execute store result storage ra:input consume.$key int 1 run scoreboard players get @s $objective")
		}
		data(IN) { modify("consume.player") { set(self(), "UUID") } }

		raw("execute if score @s ${InputObj.MODE} matches $MODE_CHOICE store result storage ra:input consume.choice int 1 run scoreboard players get @s ${InputObj.RESULT}")
		raw("execute if score @s ${InputObj.MODE} matches $MODE_NUMBER store result storage ra:input consume.number int 1 run scoreboard players get @s ${InputObj.RESULT}")
		execute {
			ifCondition { score(self(), InputObj.MODE) equalTo MODE_TEXT }
			run { function(Session.copyTextToConsumePrepare) }
		}

		function(Session.cleanup)
		returnValue(1)
	}
}
