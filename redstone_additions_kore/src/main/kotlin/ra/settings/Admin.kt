package ra.settings

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.SettingsObjectives.ADMIN
import ra.settings.SettingsObjectives.ADMIN_TAG
import ra.settings.SettingsObjectives.APAGE
import ra.settings.SettingsObjectives.PEND

/** The server-settings dispatcher: a /trigger button press becomes the admin function it names. */
internal fun Ra.registerAdmin() {
	val run = ref(Ns.SETTINGS, "admin_run")
	val refreshAt = ref(Ns.SETTINGS, "admin_refresh_at")

	pack.define(Fns.adminDispatch) { dispatch(run) }
	pack.define(run) { run() }

	// `admin_call {f}`: run the action. One macro rather than a comparison per action.
	pack.define(Fns.adminCall) { raw("\$function \$(f)") }

	// Redraw the settings page this player is looking at.
	//
	// Changing a value used to print one line and leave the page above it showing the old numbers,
	// so the only way to see what you had done was to reopen the page. Every action redraws
	// instead. Nothing happens if the player is not on a page -- an action run by typing its
	// /function path directly has no page to return to, and inventing one would be worse than the
	// single confirmation line it already prints.
	pack.define(Fns.adminRefresh) {
		execute {
			unlessCondition { score(self(), APAGE) matches rangeOrIntStart(0) }
			run { returnValue(0) }
		}
		storeScore("q.a", self(), APAGE)
		function(refreshAt, S, "q")
	}

	// `admin_refresh_at {a}`: redraw page a.
	pack.define(refreshAt) {
		sRemove("back")
		raw("\$execute if data storage ra:settings apages[\$(a)] run data modify storage ra:settings back.f set from storage ra:settings apages[\$(a)]")
		execute {
			ifCondition { data(S, "back.f") }
			run { function(Fns.adminCall, S, "back") }
		}
		sRemove("back")
	}

	// Give this player lasting server-settings access.
	//
	// WHY A TAG AT ALL. The buttons on the settings pages fire /trigger rather than /function,
	// because a run_command click event raises a confirmation prompt every time. /trigger does not
	// prompt -- but it also does not check permissions, so the check has to live somewhere, and
	// this tag is it. It can only be handed out by something that already needs permission level
	// 2: this function, reached through ra_settings:admin/show, or /tag typed by an operator.
	//
	// It PERSISTS. That is the point -- a tagged player opens the panel directly, today and after
	// the next reload. It is a role rather than a session: removing someone's operator status does
	// not remove this, and ra_settings:admin/revoke is how you take it back.
	pack.define(Fns.adminGrant) {
		tag(self()) { add(ADMIN_TAG) }
		scoreboard.players.enable(self(), ADMIN)
	}

	// Give the nearest player lasting server-settings access. Running any function needs
	// permission level 2, so this cannot be used by a player to promote themselves. It is the
	// deliberate way to hand access to somebody who should have it without making them a full
	// operator.
	pack.define(ref(Ns.SETTINGS, "admin/grant")) {
		function(Fns.adminGrant)
		raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"You can now open server settings directly with the button, or ",color:"green"},{text:"/trigger ra.settings.open set 2",color:"yellow",click_event:{action:"suggest_command",command:"/trigger ra.settings.open set 2"}}]""")
	}

	// Take server-settings access off the nearest player: the counterpart to grant, and the reason
	// the tag being permanent is safe to offer. The trigger is reset as well as the tag. Enabling
	// is per-player state that outlives the tag, and while the dispatcher refuses an untagged
	// player anyway, leaving a trigger armed that can never do anything is untidy.
	pack.define(ref(Ns.SETTINGS, "admin/revoke")) {
		raw("tag @p remove $ADMIN_TAG")
		raw("scoreboard players reset @p $ADMIN")
		raw("""tellraw @p [{text:"[Settings] ",color:"gold"},{text:"Server settings access removed. Your own preferences still work.",color:"gray"}]""")
	}
}

/**
 * A server-settings button was clicked.
 *
 * The tag is re-checked here even though the objective is only enabled for players who hold it.
 * Enabling is per-player state that OUTLIVES the tag: a player whose access was revoked still has
 * an armed trigger until something resets it, and this is what stops that last armed click.
 */
private fun Function.dispatch(run: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
	val a = tmp("#a")
	execute {
		storeResult { score(a, Obj.SET_TMP) }
		run { scoreboard.players.get(self(), ADMIN) }
	}
	scoreboard.players.set(self(), ADMIN, 0)

	raw("""execute unless entity @s[tag=$ADMIN_TAG] run return run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"You do not have server-settings access.",color:"red"}]""")

	scoreboard.players.enable(self(), ADMIN)

	// 1 means "open the index" -- what a bare /trigger ra.settings.admin produces.
	execute {
		ifCondition { score(a, Obj.SET_TMP) equalTo 1 }
		run { returnRun { function(Settings.Admin.show) } }
	}

	// Everything else is an action, numbered from 2 so that 1 could be reserved.
	scoreboard.players.remove(a, Obj.SET_TMP, 2)
	storeScore("q.a", a, Obj.SET_TMP)
	function(run, S, "q")
}

/** `admin_run {a}`: look action a up in the generated table and run it. */
private fun Function.run() {
	sRemove("call")
	raw("\$execute if data storage ra:settings actions[\$(a)] run data modify storage ra:settings call set from storage ra:settings actions[\$(a)]")

	// Whether this action navigates is read BEFORE it runs. The action itself writes to this
	// storage -- a page show overwrites `call` on its way through -- so asking afterwards would be
	// asking about whatever replaced it.
	setTmp("#nav", 0)
	execute {
		ifCondition { data(S, "call{nav:1b}") }
		run { setTmp("#nav", 1) }
	}

	execute {
		ifCondition { data(S, "call.f") }
		run { function(Fns.adminCall, S, "call") }
	}
	sRemove("call")

	// Redraw, so the page shows what the click just did. Not after a navigation: Back already drew
	// the index, and redrawing would put the page you just left straight back on top of it --
	// which is exactly why the menus appeared twice. Not while an input form is open either: the
	// value has not been typed yet, and a redraw would bury the instructions.
	execute {
		ifCondition { score(tmp("#nav"), Obj.SET_TMP) equalTo 0 }
		unlessCondition { score(self(), PEND) matches rangeOrIntStart(1) }
		run { function(Fns.adminRefresh) }
	}
}
