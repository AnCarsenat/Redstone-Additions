package ra.hub

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.scores.greaterThanOrEqualTo
import io.github.ayfri.kore.arguments.scores.lessThanOrEqualTo
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.RedstoneAdditions
import ra.api.Goggles
import ra.api.ModulesApi
import ra.api.RaLib
import ra.api.Settings
import ra.api.Tools
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.hub.placement.detectBats

/**
 * Tools whose "in use" tag is refreshed every tick by their click advancement. A tag that was
 * not refreshed this tick means the player stopped clicking. (The debug-only input handler's
 * pair stays disabled, as in beet.)
 */
private val ACTIVE_TOOLS = listOf("cdh", "dh", "wrench", "clip", "meter", "remote")

internal fun Ra.registerTick() {
	pack.define(tick) {
		// RESCHEDULE FIRST. A tick that runs past maxCommandChainLength is cut off where it
		// stands, and when this line sat near the end, the cut took the reschedule with it: the
		// whole pack stopped until somebody ran /reload. Up here it has already happened. Nothing
		// below depends on it running late -- `replace` only moves the next run, and the next run
		// is a tick away either way.
		schedule(tick).replace(1.ticks)

		toolTagCleanup()

		// Custom blocks saved by an older version, migrated before anything below reads them --
		// including blocks in a chunk that has just loaded. One selector when there are none.
		// See ra.migrations.MigrationsPart.
		function(ModulesApi.Migrations.entities)

		// Settings: seed per-player defaults and collect menu clicks. Early, because the module
		// ticks below emit sounds and particles that are filtered on those scores.
		function(Settings.tick)

		// Rebuild transport networks if any were placed or broken. Debounced, so this is a single
		// storage check on a tick where nothing changed.
		function(RaLib.Transport.tick)

		function(RaLib.Input.tick)

		// Non-OP input data handler actions.
		function(Tools.DataHandler.tick)

		// Settings typed input, collected in the same place and for the same reason: this is after
		// ra_lib:input/tick, so a form finished this tick is seen this tick.
		function(Settings.inputTick)

		function(detectBats)

		// Markers are NOT re-centred here any more. This used to teleport every custom block
		// marker in the world onto its block centre, every tick; a marker has no physics and
		// nothing in the pack moves one, and placement summons it at the centre already
		// (#ra:placement_handlers runs at `align xyz positioned ~0.5 ~0.5 ~0.5`). A marker an
		// older version left off-centre is straightened once by the 6.0.1 migration.

		for (module in RedstoneAdditions.modules) module.tick?.let { function(it) }

		function(Goggles.tick)

		menuTriggers()
	}
}

private fun Function.toolTagCleanup() {
	for (tool in ACTIVE_TOOLS) tag(allPlayers { tag = "ra.${tool}_active"; tag = !"ra.${tool}_clicked" }) { remove("ra.${tool}_active") }
	for (tool in ACTIVE_TOOLS) tag(allPlayers { tag = "ra.${tool}_clicked" }) { remove("ra.${tool}_clicked") }
}

/**
 * TOOL MENU TRIGGERS, HANDED OUT WHILE THE TOOL IS HELD.
 *
 * `ra.wrench` and `ra.dh.action` carry which row of a menu was clicked. They are no use to
 * somebody who is not holding the tool that prints that menu, and an enabled trigger shows up in
 * everyone's /trigger completion regardless -- so they are enabled while the tool is in hand and
 * reset when the grace window runs out.
 *
 * The window exists because putting the tool away to read the menu it just printed is a normal
 * thing to do, and disarming the buttons the instant a player switches slots would make the
 * menus feel broken.
 *
 * Resetting the score is what actually takes a trigger back: the enabled flag is stored with the
 * score, so removing one removes the other.
 */
private fun Function.menuTriggers() {
	raw("execute as @a if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{wrench:1b}}] run scoreboard players set @s $HOLD_WRENCH 200")
	raw("execute as @a if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{data_handler:1b}}] run scoreboard players set @s $HOLD_DH 200")

	for (hold in listOf(HOLD_WRENCH, HOLD_DH)) execute {
		asTarget(allPlayers { scores { hold greaterThanOrEqualTo 1 } })
		run { scoreboard.players.remove(self(), hold, 1) }
	}

	val holdingWrench = allPlayers { scores { HOLD_WRENCH greaterThanOrEqualTo 1 } }
	val holdingDh = allPlayers { scores { HOLD_DH greaterThanOrEqualTo 1 } }
	scoreboard.players.enable(holdingWrench, WRENCH)
	scoreboard.players.enable(holdingDh, DH_ACTION)
	scoreboard.players.enable(holdingDh, EDIT_TYPE)

	for ((hold, trigger) in listOf(HOLD_WRENCH to WRENCH, HOLD_DH to DH_ACTION, HOLD_DH to EDIT_TYPE)) execute {
		asTarget(allPlayers { scores { hold lessThanOrEqualTo 0 } })
		run { scoreboard.players.reset(self(), trigger) }
	}

	execute {
		asTarget(allPlayers { scores { WRENCH greaterThanOrEqualTo 1 } })
		at(self())
		run { function(Tools.Wrench.menuAction) }
	}
}
