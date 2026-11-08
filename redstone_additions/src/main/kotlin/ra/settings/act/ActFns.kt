package ra.settings.act

import ra.core.scoreOf
import ra.core.snbt
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
	val userBool = ref(Ns.SETTINGS, "act/user/bool")
	val userInt = ref(Ns.SETTINGS, "act/user/int")
	val userList = ref(Ns.SETTINGS, "act/user/list")

	fun Ra.registerAct() {
		pack.define(Fns.act) { act() }
		pack.define(at) { at() }

		// Describe this row as an edit and ask for a value.
		pack.define(ask) {
			data(S) { modify("edit") { set(buildNbtCompound { put("kind", "user") }) } }
			for (field in listOf("obj", "key", "type")) sCopy("edit.$field", "cur.$field")
			for (field in listOf("min", "max", "label")) execute {
				ifCondition { data(S, "cur.$field") }
				run { sCopy("edit.$field", "cur.$field") }
			}
			function(Fns.editStart)
		}

		pack.define(user) { user() }
		pack.define(userBool) { userBool() }
		pack.define(userInt) { userInt() }
		pack.define(userList) { userList() }
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

		function(user)
		function(Fns.pageOpen)
	}

	/**
	 * `act/user`: change this player's own value for the row in storage ra:settings cur.
	 *
	 * ONE MACRO PER TYPE. A macro call fails outright when any `$(name)` it uses is missing from
	 * the compound, and a bool row carries no min, max or step. The single `act/user` macro this
	 * replaces used all three, so every bool click was dropped before a command ran: personal
	 * toggles never flipped, and the menu, redrawn from the unchanged score, showed the old value.
	 * Each branch below only names the fields its own row type always has.
	 */
	private fun Function.user() {
		val c = "storage ra:settings cur"
		setTmp("#n", 0)
		raw("execute if data $c{type:\"bool\"} run function ${userBool.asId()} with $c")
		raw("execute if data $c{type:\"int\"} run function ${userInt.asId()} with $c")
		raw("execute if data $c{type:\"list\"} run function ${userList.asId()} with $c")
	}

	/** Reads the player's current value into #n; an unset score stays 0. */
	private fun Function.readCurrent() {
		raw("\$execute if score @s \$(obj) matches -2147483648.. store result score #n ${Obj.SET_TMP} run scoreboard players get @s \$(obj)")
	}

	/** `act/user/bool {obj}`: flip it. */
	private fun Function.userBool() {
		readCurrent()
		raw("\$execute if score #n ${Obj.SET_TMP} matches 1.. run return run scoreboard players set @s \$(obj) 0")
		raw("\$scoreboard players set @s \$(obj) 1")
	}

	/**
	 * `act/user/int {obj, min, max, step}`: step, then clamp. Clamping here rather than refusing
	 * keeps the button always doing something visible at the ends of the range.
	 */
	private fun Function.userInt() {
		readCurrent()
		raw("\$execute if score #dir ${Obj.SET_TMP} matches 1 run scoreboard players add #n ${Obj.SET_TMP} \$(step)")
		raw("\$execute if score #dir ${Obj.SET_TMP} matches ..-1 run scoreboard players remove #n ${Obj.SET_TMP} \$(step)")
		// The bounds are macro text, so the provider is too: min(max(n, min), max).
		val n = scoreOf("#n", Obj.SET_TMP).snbt()
		val clamp = "{type:\"minecraft:min\",inputs:[{type:\"minecraft:max\",inputs:[$n,\$(min)]},\$(max)]}"
		raw("\$execute store result score @s \$(obj) run compute default integer $clamp")
	}

	/** `act/user/list {obj}`: step forward through cur.values and wrap. */
	private fun Function.userList() {
		readCurrent()
		scoreboard.players.add(tmp("#n"), Obj.SET_TMP, 1)
		raw("execute store result score #len ${Obj.SET_TMP} run data get storage ra:settings cur.values")
		raw("execute if score #len ${Obj.SET_TMP} matches 1.. run scoreboard players operation #n ${Obj.SET_TMP} %= #len ${Obj.SET_TMP}")
		raw("\$scoreboard players operation @s \$(obj) = #n ${Obj.SET_TMP}")
	}
}
