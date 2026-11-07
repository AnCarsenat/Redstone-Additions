package ra.settings

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.api.Settings
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.settings.SettingsObjectives.ADMIN_TAG
import ra.settings.SettingsObjectives.OPEN
import ra.settings.SettingsObjectives.PAGE
import ra.settings.SettingsObjectives.VIEWING
import ra.settings.page.PageFns

/** The player's menu and the `/trigger ra.settings.open` entry point. */
internal fun Ra.registerMenu() {
	pack.define(Fns.menu) { menu() }
	pack.define(Fns.openClick) { openClick() }
	pack.define(Fns.serverOpen) { serverOpen() }
}

/** Ticks a drawn menu keeps its buttons enabled. Drawing a menu is what makes its buttons usable; see ra_settings:init. */
internal const val VIEWING_TICKS = 1200

/**
 * The player's own preferences, reached with /trigger ra.settings.open set 1 -- so it works for
 * someone with no permissions at all, which is the entire reason this menu exists separately from
 * the operator settings. Only user-scope rows are here: world settings are absent because a
 * button a player is only going to be refused is worse than no button.
 */
private fun Function.menu() {
	scoreboard.players.set(self(), VIEWING, VIEWING_TICKS)

	raw("""tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{text:"Your Redstone Additions Preferences",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]""")

	// With a single page there is nothing to choose between, so a menu whose only content is one
	// button is a click that teaches the player nothing. Open it.
	execute {
		unlessCondition { data(S, "pages[1]") }
		run { scoreboard.players.set(self(), PAGE, 0) }
	}
	execute {
		unlessCondition { data(S, "pages[1]") }
		run { returnRun { function(Fns.pageOpen) } }
	}

	setTmp("#idx", 0)
	sCopy("scan", "pages")
	function(PageFns.list)
	sRemove("scan")

	raw("""tellraw @s [{text:"These affect only you.",color:"dark_gray"}]""")
	// Shown to everyone, because nothing can tell operators apart: there is no permission-level
	// selector, and @s[level=...] is EXPERIENCE level. It RUNS a trigger rather than suggesting a
	// function: suggesting /function is useless to somebody who cannot run one, whereas the
	// trigger works for everybody and lands in ra_settings:server_open.
	raw("""tellraw @s [{text:"  "},{text:"[ Server settings ]",color:"gray",hover_event:{action:"show_text",value:"World-wide settings — needs operator permission"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 2"}}]""")
}

/**
 * A page button was clicked, or the player typed /trigger ra.settings.open.
 *
 * 1 is the player's own menu -- and also what a bare /trigger produces, since /trigger with no
 * argument adds 1 to a score the dispatcher has just zeroed. 2 is the server settings, which go
 * through server_open because whether they may be opened at all depends on the player. 3 is the
 * disabled-blocks list, which anyone may look at. Page N arrives as N+4: 0 cannot be delivered
 * through a trigger, and 1, 2 and 3 are taken.
 *
 * These ride the existing objective rather than getting their own. Every extra trigger is another
 * thing to enable every tick for everybody, so a fixed code on a trigger that already exists
 * beats a new objective for every new entry point.
 */
private fun Function.openClick() {
	val v = tmp("#v")
	execute {
		storeResult { score(v, Obj.SET_TMP) }
		run { scoreboard.players.get(self(), OPEN) }
	}
	scoreboard.players.set(self(), OPEN, 0)
	scoreboard.players.enable(self(), OPEN)

	execute {
		ifCondition { score(v, Obj.SET_TMP) matches rangeOrIntEnd(1) }
		run { returnRun { function(Fns.menu) } }
	}
	execute {
		ifCondition { score(v, Obj.SET_TMP) equalTo 2 }
		run { returnRun { function(Fns.serverOpen) } }
	}
	execute {
		ifCondition { score(v, Obj.SET_TMP) equalTo 3 }
		run { returnRun { function(Fns.disabled) } }
	}

	scoreboard.players.remove(v, Obj.SET_TMP, 4)
	scoreboard.players.operation(self(), PAGE, Operation.SET, v, Obj.SET_TMP)
	function(Fns.pageOpen)
}

/**
 * Try to open the server settings, and if that is not this player's to do, put the command where
 * they can read it.
 *
 * WHY THIS IS NOT JUST A LINK TO THE FUNCTION. A button that runs /function raises a confirmation
 * dialog, and for a player without permission it then fails anyway. A button that runs a trigger
 * does neither -- but a trigger handler is datapack code, which runs at permission level 2, so it
 * COULD open the panel for anybody. That would hand server settings to every player on the
 * server. So the handler opens the panel only for a player who already holds ra.admin, meaning
 * the game already checked them once. Everyone else is given the command as a SUGGESTION: it
 * lands in the chat box unrun, and pressing enter puts the permission check back on the game.
 */
private fun Function.serverOpen() {
	execute {
		ifCondition { entity(self { tag = ADMIN_TAG }) }
		run { returnRun { function(Settings.Admin.show) } }
	}

	raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Server settings need operator permission.",color:"gray"}]""")
	raw("""tellraw @s [{text:"  "},{text:"/function ${Settings.Admin.show.asId()}",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Puts this in your chat box — press enter to run it. It will only work if you are an operator."},click_event:{action:"suggest_command",command:"/function ${Settings.Admin.show.asId()}"}}]""")
}
