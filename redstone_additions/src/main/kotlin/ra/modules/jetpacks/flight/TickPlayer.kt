package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.tmp
import ra.modules.jetpacks.Kits

/** One player wearing a jetpack. As the player, at the player. */
object TickPlayer {
	val tickPlayer = ref(Ns.JETPACKS, "flight/tick_player")

	private val t = Obj.TEMP

	fun Ra.registerTickPlayer() {
		pack.define(tickPlayer) { tickPlayer() }
	}

	/** Drops the Thruster's smoothing state: x/z samples, smoothed speed, throttle and the thrusting tag. */
	private fun Function.resetThrust(condition: String) {
		for (obj in listOf(Jp.X, Jp.Z, Jp.VX, Jp.VZ, Jp.RAMP)) raw("execute if score $condition run scoreboard players reset @s $obj")
		raw("execute if score $condition run tag @s remove ${Jp.THRUSTING}")
	}

	private fun Function.tickPlayer() {
		for (trigger in listOf(Jp.MODE, Jp.SOUND, Jp.POWER, Jp.KITS)) scoreboard.players.enable(self(), trigger)

		// Switched off with /trigger ra.jp.power: hand anything left over back to vanilla and stay
		// out of the way. The triggers above still work, so it can be switched on.
		execute {
			ifCondition { score(self(), Jp.OFF) equalTo 1 }
			ifCondition { entity(self { tag = Jp.ON }) }
			run { function(Engine.stop) }
		}
		raw("execute if score @s ${Jp.OFF} matches 1 run return 0")

		tag(self()) { add(Jp.ON) }

		// THE CHESTPLATE IS THE RECORD; THESE TAGS ARE A CACHE OF IT
		// Re-derived every tick rather than set once when a kit is fitted, so taking the jetpack
		// off, handing it to someone else or dying with it all do the right thing with no extra
		// bookkeeping. It also self-heals worlds where the upgrades were briefly stored on the
		// player: those tags are cleared here the first time a chestplate that does not carry the
		// flag is worn.
		for (u in Kits.upgrades) {
			val fitted = "items entity @s armor.chest *[minecraft:custom_data~{ra:{${u.key}:1b}}]"
			raw("execute if $fitted unless entity @s[tag=${u.mute}] run tag @s add ${u.active}")
			raw("execute unless $fitted run tag @s remove ${u.active}")
			raw("execute if entity @s[tag=${u.mute}] run tag @s remove ${u.active}")
		}

		// Both modes are driven by holding sneak, and sneaking normally costs 70% of walking speed
		// -- which is why classic mode felt like wading. A jetpack pins sneaking_speed (base 0.3)
		// back up to a full 1.0 while it is worn.
		raw("execute unless entity @s[tag=${Jp.SPEED_ON}] run attribute @s minecraft:sneaking_speed modifier add ${Jp.SNEAK_MODIFIER} 0.7 add_value")
		tag(self()) { add(Jp.SPEED_ON) }

		// Airborne, computed once and read by both the Thruster and the ground-reset below. This
		// lives here rather than inside flight/thrust because more than one thing needs it and the
		// test is a block read.
		function(Scorch.airborne)

		// Thruster kit: the horizontal upgrade, and ONLY while off the ground. It used to apply on
		// the ground too, which made a fitted jetpack a permanent sprint boost you could not take
		// off without removing the chestplate.
		//
		// It also used to be an attribute modifier, which is the wrong lever entirely -- see
		// flight/thrust for why movement_speed does almost nothing once you are in the air.
		resetThrust("#jp.airborne $t matches 0")

		// A jetpack that ran dry comes back the moment there is fuel again.
		for (fuel in listOf("minecraft:coal", "minecraft:charcoal")) for (slot in listOf("container.*", "weapon.offhand")) {
			raw("execute if entity @s[tag=${Jp.DRY}] if items entity @s $slot $fuel run tag @s remove ${Jp.DRY}")
		}
		raw("execute if entity @s[tag=${Jp.DRY}] run return 0")

		execute {
			ifCondition { score(self(), Jp.STATE) equalTo 1 }
			run { function(Hover.hover) }
		}
		execute {
			unlessCondition { score(self(), Jp.STATE) equalTo 1 }
			run { function(Classic.classic) }
		}

		// IS THE JETPACK ACTUALLY RUNNING?
		// The upgrade kits used to fire on "wearing the kit and off the ground", which is not the
		// same thing at all. In normal mode the jetpack only runs while you sneak -- flight/classic
		// returns early otherwise -- so jump, sprint, jump handed you the thruster for free, with
		// the pack switched off. In hover the jetpack IS holding you up whenever you are airborne,
		// so there the two really do coincide. (Scorch: standing in a village with the kit fitted
		// should not set fire to the villagers.)
		scoreboard.players.set(tmp("#jp.firing"), t, 0)
		raw("execute if score #jp.airborne $t matches 1 if score @s ${Jp.STATE} matches 1 run scoreboard players set #jp.firing $t 1")
		raw("execute if score #jp.airborne $t matches 1 unless score @s ${Jp.STATE} matches 1 if predicate ${Classic.SNEAKING} run scoreboard players set #jp.firing $t 1")

		// Below the fuel check on purpose. Both kits used to run above it, so they kept working on
		// an empty tank -- the one state where the jetpack is unambiguously off. Not firing means
		// the smoothing has to be dropped, not merely paused. thrust measures how far you moved
		// since it last looked; leave that reading behind while the pack is off and the next burst
		// opens with a position delta covering the whole glide, which is a lurch nobody asked for.
		// Cleared, it re-seeds on the next tick with a delta of zero -- the same thing that happens
		// on landing.
		resetThrust("#jp.firing $t matches 0")

		execute {
			ifCondition { entity(self { tag = Kits.speed.active }) }
			ifCondition { score(tmp("#jp.firing"), t) equalTo 1 }
			run { function(Thrust.thrust) }
		}
		execute {
			ifCondition { entity(self { tag = Kits.scorch.active }) }
			ifCondition { score(tmp("#jp.firing"), t) equalTo 1 }
			run { function(Scorch.scorch) }
		}
	}
}
