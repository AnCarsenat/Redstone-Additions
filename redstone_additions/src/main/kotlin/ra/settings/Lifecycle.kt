package ra.settings

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.scores.ScoreboardCriteria
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.scores.score
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.tags.functionTag
import io.github.ayfri.kore.functions.Function
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.settings.SettingsObjectives.ACT
import ra.settings.SettingsObjectives.ADMIN
import ra.settings.SettingsObjectives.ADMIN_TAG
import ra.settings.SettingsObjectives.DBG_WAS
import ra.settings.SettingsObjectives.OPEN
import ra.settings.SettingsObjectives.PEND
import ra.settings.SettingsObjectives.VIEWING

/** `init`, the two halves of the tick, `hooks`, and the `api` documentation function. */
internal fun Ra.registerLifecycle() {
	pack.define(Settings.init) { init() }

	// The half of the settings tick that must run EARLY, called from ra:tick before the module
	// ticks. Only seeding: the module ticks below this emit sounds and particles filtered on
	// per-player scores, and a player with no score is excluded by those filters -- so the
	// scores have to exist before anything tries to play a sound. Everything else lives in
	// input_tick, which runs AFTER ra_lib:input/tick.
	pack.define(Settings.tick) { function(Fns.sync) }

	pack.define(Settings.inputTick) { inputTick() }
	pack.define(Fns.hooks) { hooks() }

	// Not runnable: how a module gets settings, kept in the game files where a pack author looks.
	pack.define(Fns.api) {
		for (line in API_DOC) comment(line)
	}
}

private val API_DOC = listOf(
	"/ra_settings:api",
	"Not runnable. How a module gets settings.",
	"",
	"Add a settings page in ra.settings.pages (Kotlin). Nothing else: the generator turns it into",
	"the menu registry, the defaults, and the operator function tree at build time.",
	"",
	"READING A SETTING",
	"  function ra_settings:get  {key:\"...\",default:N}            -> #setting ra.set.tmp",
	"  function ra_settings:prop {block:\"...\",prop:\"...\",default:N} -> #setting ra.set.tmp",
	"  function ra_settings:user {obj:\"...\",default:N}              -> #setting ra.set.tmp  (as the player)",
	"  function ra_settings:enabled {block:\"...\"}                   -> 1 when placeable",
)

/**
 * Bring the settings system up. Called once from ra:load.
 *
 * WHAT THIS IS. A place for values that are not a property of one placed block. Per-block
 * properties already have a home -- the marker's data.properties, edited with the wrench or the
 * Data Handler. What had nowhere to live was the other two kinds: pack-wide balance an admin
 * wants to tune once, and per-player preferences that should differ between two people standing
 * in the same room. See [ra.settings.pages.Scope] for why the two are reached differently.
 */
private fun Function.init() {
	scoreboard.objectives.add(OPEN, ScoreboardCriteria.TRIGGER)
	scoreboard.objectives.add(ACT, ScoreboardCriteria.TRIGGER)

	// `open` is the way IN, so everybody always has it -- it is the one trigger that has to be
	// there before anything has happened.
	scoreboard.players.enable(allPlayers(), OPEN)

	// `act` is only meaningful while a menu is on screen, so it is enabled while that is true and
	// reset when it stops being true. An enabled trigger shows up in everyone's /trigger
	// completion whether it can do anything or not, and a list of names that do nothing is worse
	// than a shorter list.
	scoreboard.objectives.add(VIEWING)

	// Server-settings buttons. Only ever enabled for a player holding ra.admin, so a hand-typed
	// /trigger from anyone else is refused by the game before it reaches the dispatcher.
	scoreboard.objectives.add(ADMIN, ScoreboardCriteria.TRIGGER)

	// ra.admin IS THE PERMISSION, AND IT PERSISTS. It is deliberately not cleared here. A server
	// owner tags the people who should have settings access -- with /tag, or by opening the panel
	// once through ra_settings:admin/show -- and that holds across reloads. This is a role, not a
	// session: somebody who is de-opped keeps settings access until the tag is taken off them.
	// Granting it needs permission level 2, so nobody can give it to themselves; taking it back
	// is a deliberate act (ra_settings:admin/revoke).

	scoreboard.objectives.add(SettingsObjectives.PAGE)
	scoreboard.objectives.add(SettingsObjectives.APAGE)

	// Sessions are shared with every other tool that asks for typed input, so consuming one
	// without checking whose it is takes the Data Handler's answer away from it.
	scoreboard.objectives.add(SettingsObjectives.REQ)

	// Mirrors ra.dh.pending, for the same reason: the answer arrives some ticks after the click,
	// and something has to remember what the question was.
	scoreboard.objectives.add(PEND)

	// Scratch for the renderer and the dispatcher.
	scoreboard.objectives.add(Obj.SET_TMP)

	// Created here rather than in hooks: hooks runs every tick, and re-adding an existing
	// objective logs a complaint every one of them.
	scoreboard.objectives.add(DBG_WAS)

	// Rebuilds the menu registry and seeds any setting that has no value yet. Values already
	// chosen are never overwritten -- see the generated ra_settings:defaults.
	function(datapack.functionTag("register", Ns.SETTINGS.id))
}

/**
 * Menu clicks, the admin dispatcher, and finished typed input. Called from ra:tick directly
 * after ra_lib:input/tick, beside the Data Handler.
 *
 * WHY EVERY CLICK IS HANDLED HERE AND NOT IN ra_settings:tick. A click can open an input session,
 * and where in the tick that happens decides whether ra_lib:input sees the session on the tick
 * it was born. ra_lib:input/tick runs partway through ra:tick. The Data Handler's dispatcher sits
 * after it, so a session the Handler opens is first scanned on the NEXT tick. This dispatcher used
 * to sit before it, so a session opened here was scanned in the same tick it was created --
 * against a brand new form, by a scan written on the assumption that a tick had passed. So the
 * click handling moved to where the Handler's is; the seeding half stayed early, because it has
 * to precede the module sounds it feeds.
 */
private fun Function.inputTick() {
	scoreboard.players.enable(allPlayers(), OPEN)

	// Only for players with a menu open. Resetting removes the score AND the enabled flag with it,
	// which is the only way to take a trigger back off somebody.
	val viewing = allPlayers { scores { score(VIEWING, rangeOrIntStart(1)) } }
	val expired = allPlayers { scores { score(VIEWING, rangeOrIntEnd(0)) } }
	execute { asTarget(viewing); run { scoreboard.players.remove(self(), VIEWING, 1) } }
	scoreboard.players.enable(viewing, ACT)
	execute { asTarget(expired); run { scoreboard.players.reset(self(), ACT) } }
	execute { asTarget(expired); run { scoreboard.players.reset(self(), VIEWING) } }

	dispatch(OPEN, Fns.openClick)
	dispatch(ACT, Fns.act)

	scoreboard.players.enable(allPlayers { tag = ADMIN_TAG }, ADMIN)
	dispatch(ADMIN, Fns.adminDispatch)

	dispatch(PEND, Fns.applyPending)

	// After the dispatchers, so a preference changed by a click applies on the tick it was
	// pressed rather than the one after.
	function(Fns.hooks)
}

/** `execute as @a[scores={<objective>=1..}] at @s run function <fn>`. */
private fun Function.dispatch(objective: String, fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) = execute {
	asTarget(allPlayers { scores { score(objective, rangeOrIntStart(1)) } })
	at(self())
	run { function(fn) }
}

/**
 * Apply user settings that are not enforced at their own call sites. Called once per tick from
 * input_tick, AFTER the click dispatcher. Sounds and particles are gated in the commands
 * themselves -- every playsound and particle in the pack carries the matching scores= filter.
 *
 * DEBUG IS MIRRORED, NOT OWNED. The pack gates its debug messages on the ra.debug tag, and that
 * tag predates this system: /tag @s add ra.debug is still the documented way to see the
 * multiblock debug reports (ra_lib_multiblock:spec/debug). An earlier version set the
 * tag from the score in both directions, which quietly stripped the tag from anyone who had added
 * it by hand -- the setting fought the documented command and won, every tick. So the score only
 * ever ADDS; turning the setting off removes the tag once, on the tick it changes.
 */
private fun Function.hooks() {
	tag(allPlayers { scores { score(Obj.USER_DEBUG, rangeOrIntStart(1)) } }) { add(Tag.DEBUG) }

	// The falling edge only: on for a player whose setting was on last tick and is off now. A
	// player who never used the setting has both at 0 and is untouched.
	execute {
		asTarget(allPlayers { scores { score(Obj.USER_DEBUG, rangeOrIntEnd(0)) } })
		ifCondition { score(self(), DBG_WAS) matches rangeOrIntStart(1) }
		run { tag(self()) { remove(Tag.DEBUG) } }
	}

	execute {
		asTarget(allPlayers())
		run { scoreboard.players.operation(self(), DBG_WAS, Operation.SET, self(), Obj.USER_DEBUG) }
	}
}
