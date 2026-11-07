package ra.settings.act

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrInt
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.put
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.Fns
import ra.settings.S
import ra.settings.SettingsObjectives.ACT
import ra.settings.SettingsObjectives.PAGE
import ra.settings.SettingsObjectives.VIEWING
import ra.settings.VIEWING_TICKS
import ra.settings.sCopy
import ra.settings.setTmp
import ra.settings.storeScore
import ra.settings.tmp

/** `ra_settings:act` and `act/` functions: a menu row button was clicked. */
internal object ActFns {
	val at = ref(Ns.SETTINGS, "act/at")
	val ask = ref(Ns.SETTINGS, "act/ask")
	val user = ref(Ns.SETTINGS, "act/user")

	fun Ra.registerAct() {
		pack.define(Fns.act) { act() }
		pack.define(at) { at() }

		// Describe this row as an edit and ask for a value.
		pack.define(ask) {
			data(S) { modify("edit") { set(buildNbtCompound { put("kind", "user") }) } }
			for (field in listOf("obj", "key", "type")) sCopy("edit.$field", "cur.$field")
			for (field in listOf("min", "max")) execute {
				ifCondition { data(S, "cur.$field") }
				run { sCopy("edit.$field", "cur.$field") }
			}
			function(Fns.editStart)
		}

		pack.define(user) { user() }
	}

	/**
	 * The code is the row index plus one, offset by a band that says WHICH button:
	 *   +0     the row's only button, or its "down"      -> direction -1
	 *   +512   the row's "up"                            -> direction +1
	 *   +1024  "Modify", which opens a typed input       -> direction  0
	 * The bands are far past any real row count, so splitting them apart is two comparisons rather
	 * than a second score carried alongside. Plus one because /trigger cannot deliver 0: a trigger
	 * score of 0 is exactly the "not clicked" state the dispatcher waits on.
	 */
	private fun Function.act() {
		val v = tmp("#v")
		execute {
			storeResult { score(v, Obj.SET_TMP) }
			run { scoreboard.players.get(self(), ACT) }
		}
		scoreboard.players.set(self(), ACT, 0)
		scoreboard.players.enable(self(), ACT)
		scoreboard.players.set(self(), VIEWING, VIEWING_TICKS)

		// Highest band first: 1025.. would otherwise also satisfy the 513.. test.
		setTmp("#dir", -1)
		execute {
			ifCondition { score(v, Obj.SET_TMP) matches rangeOrIntStart(1025) }
			run { setTmp("#dir", 0) }
		}
		execute {
			ifCondition { score(v, Obj.SET_TMP) matches rangeOrIntStart(1025) }
			run { scoreboard.players.remove(v, Obj.SET_TMP, 1024) }
		}
		execute {
			ifCondition { score(v, Obj.SET_TMP) matches rangeOrInt(513..1024) }
			run { setTmp("#dir", 1) }
		}
		execute {
			ifCondition { score(v, Obj.SET_TMP) matches rangeOrInt(513..1024) }
			run { scoreboard.players.remove(v, Obj.SET_TMP, 512) }
		}
		scoreboard.players.remove(v, Obj.SET_TMP, 1)

		storeScore("q.p", self(), PAGE)
		storeScore("q.r", v, Obj.SET_TMP)
		function(at, S, "q")
	}

	/**
	 * `act/at {p, r}`: apply row r of page p.
	 *
	 * Only user rows reach here: the generator puts nothing else in the menu registry, so there is
	 * no operator setting on screen for a player to click and be refused. Operator settings are
	 * /function ra_settings:admin/..., gated by the game at permission level 2.
	 */
	private fun Function.at() {
		raw("\$execute unless data storage ra:settings pages[\$(p)].rows[\$(r)] run return 0")
		raw("\$data modify storage ra:settings cur set from storage ra:settings pages[\$(p)].rows[\$(r)]")
		execute {
			unlessCondition { data(S, "cur{scope:\"user\"}") }
			run { returnValue(0) }
		}

		// Typed rows hand over to ra_lib:input and finish on a later tick. The payload is copied
		// field by field rather than substituted, because a row can carry a player-authored string
		// and a macro would break on a quote character.
		val str = "cur{type:\"str\"}"
		val int = "cur{type:\"int\"}"
		execute { ifCondition { data(S, str) }; run { function(ask) } }
		execute { ifCondition { data(S, int) }; ifCondition { score(tmp("#dir"), Obj.SET_TMP) equalTo 0 }; run { function(ask) } }
		execute { ifCondition { data(S, str) }; run { returnValue(0) } }
		execute { ifCondition { data(S, int) }; ifCondition { score(tmp("#dir"), Obj.SET_TMP) equalTo 0 }; run { returnValue(0) } }

		function(user, S, "cur")
		function(Fns.pageOpen)
	}

	/** `act/user {obj, type, default, min, max, step, values}`: change this player's own value. */
	private fun Function.user() {
		val c = "execute if data storage ra:settings cur"
		setTmp("#n", 0)
		raw("\$execute if score @s \$(obj) matches -2147483648.. store result score #n ${Obj.SET_TMP} run scoreboard players get @s \$(obj)")

		// bool: flip it.
		raw("\$$c{type:\"bool\"} if score #n ${Obj.SET_TMP} matches 1.. run scoreboard players set @s \$(obj) 0")
		raw("\$$c{type:\"bool\"} if score #n ${Obj.SET_TMP} matches ..0 run scoreboard players set @s \$(obj) 1")

		// int: step, then clamp. Clamping here rather than refusing keeps the button always doing
		// something visible at the ends of the range.
		raw("\$$c{type:\"int\"} if score #dir ${Obj.SET_TMP} matches 1 run scoreboard players add #n ${Obj.SET_TMP} \$(step)")
		raw("\$$c{type:\"int\"} if score #dir ${Obj.SET_TMP} matches ..-1 run scoreboard players remove #n ${Obj.SET_TMP} \$(step)")
		raw("\$$c{type:\"int\"} run scoreboard players set #lo ${Obj.SET_TMP} \$(min)")
		raw("\$$c{type:\"int\"} run scoreboard players set #hi ${Obj.SET_TMP} \$(max)")
		raw("$c{type:\"int\"} if score #n ${Obj.SET_TMP} < #lo ${Obj.SET_TMP} run scoreboard players operation #n ${Obj.SET_TMP} = #lo ${Obj.SET_TMP}")
		raw("$c{type:\"int\"} if score #n ${Obj.SET_TMP} > #hi ${Obj.SET_TMP} run scoreboard players operation #n ${Obj.SET_TMP} = #hi ${Obj.SET_TMP}")
		raw("\$$c{type:\"int\"} run scoreboard players operation @s \$(obj) = #n ${Obj.SET_TMP}")

		// list: step forward and wrap.
		raw("$c{type:\"list\"} run scoreboard players add #n ${Obj.SET_TMP} 1")
		raw("$c{type:\"list\"} store result score #len ${Obj.SET_TMP} run data get storage ra:settings cur.values")
		raw("$c{type:\"list\"} if score #len ${Obj.SET_TMP} matches 1.. run scoreboard players operation #n ${Obj.SET_TMP} %= #len ${Obj.SET_TMP}")
		raw("\$$c{type:\"list\"} run scoreboard players operation @s \$(obj) = #n ${Obj.SET_TMP}")
	}
}
