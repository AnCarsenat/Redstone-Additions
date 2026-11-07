package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Tags
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.PARTICLE_VIEWERS
import ra.modules.jetpacks.Jp.tmp
import ra.modules.jetpacks.Kits
import ra.modules.jetpacks.mode.Modes

/**
 * Hover mode: sneak steers up or down by where you look, and letting go holds.
 *
 * Descending is NOT levitation. Amplifier 255 is stored as 255, not as -1, so the old wrap-around
 * trick does not sink a player -- it throws them upward at about twelve blocks a second. Sinking
 * hands gravity back with slow falling on top, which keeps the descent gentle and eats the fall
 * damage.
 *
 * Holding station is a servo, not an absence of gravity: the vertical speed is measured from one
 * tick to the next and the gravity attribute is aimed against it. There is no command that clears
 * a player's velocity -- /tp used to, but MC-275455 was fixed in the 1.21.2 snapshots and relative
 * teleports now keep motion, so an in-place tp does nothing at all. Two thruster strengths against
 * a small dead zone kill a levitation coast in two or three ticks, which is what makes hover feel
 * abrupt instead of icy.
 *
 * Pitch: -90 is straight up, +90 is straight down.
 */
object Hover {
	val hover = ref(Ns.JETPACKS, "flight/hover")
	val grounded = ref(Ns.JETPACKS, "flight/grounded")
	val hold = ref(Ns.JETPACKS, "flight/hold")
	val gravity = ref(Ns.JETPACKS, "flight/gravity")

	private val t = Obj.TEMP
	private val dir = tmp("#jp.dir")
	private val tier = tmp("#jp.tier")

	/**
	 * Thruster tiers written onto the gravity attribute. Player gravity has a base value of 0.08
	 * blocks per tick squared, so an add_value modifier of -0.08 lands on exactly zero and anything
	 * between -0.16 and 0.0 is a thruster of the given strength. Tier 9 means "hands off": the
	 * modifier comes away and vanilla gravity applies.
	 *
	 *   -2  push up hard     -1  push up gently   0  hold
	 *    1  pull down gently  2  pull down hard   9  vanilla
	 */
	private val gravityTiers = listOf(-2 to "-0.16", -1 to "-0.1", 0 to "-0.08", 1 to "-0.06", 2 to "0.0")

	fun Ra.registerHover() {
		pack.define(hover) { hover() }
		pack.define(grounded) { grounded() }
		pack.define(hold) { hold() }
		pack.define(gravity) { gravity() }
	}

	/** As the player wearing the jetpack, at the player. */
	private fun Function.hover() {
		// Vertical speed, in thousandths of a block per tick, from the change in Pos[1].
		raw("execute store result score #jp.y $t run data get entity @s Pos[1] 1000")
		raw("execute unless score @s ${Jp.Y} = @s ${Jp.Y} run scoreboard players operation @s ${Jp.Y} = #jp.y $t")
		raw("scoreboard players operation #jp.dy $t = #jp.y $t")
		raw("scoreboard players operation #jp.dy $t -= @s ${Jp.Y}")
		raw("scoreboard players operation @s ${Jp.Y} = #jp.y $t")

		raw("execute store result score @s ${Obj.PITCH} run data get entity @s Rotation[1] 1")

		// -1 sink, 0 hold, 1 climb. The dead zone is wide on purpose: a narrow one made every small
		// look-around flip the state.
		scoreboard.players.set(dir, t, 0)
		raw("execute if predicate ra:is_sneaking if score @s ${Obj.PITCH} matches ..-30 run scoreboard players set #jp.dir $t 1")
		raw("execute if predicate ra:is_sneaking if score @s ${Obj.PITCH} matches 30.. run scoreboard players set #jp.dir $t -1")

		// Standing on something hands the player straight back to vanilla: walking around with
		// gravity switched off feels wrong, and the servo has nothing to hold up. A block below the
		// feet at ~-0.1 means grounded -- with the feet at y=64.0 that reads block 63, and one tick
		// off the floor it reads air. Water and lava count as grounded too, which is fine: swimming
		// should be swimming. Sneak plus look up is exempt, or hover could never leave the ground.
		//
		// ra.jp.air records that the player has actually been airborne, so flight/grounded can tell
		// a landing from someone who is simply standing there.
		raw("execute if block ~ ~-0.1 ~ #minecraft:air run scoreboard players set @s ${Jp.AIR} 1")
		execute {
			unlessCondition { block(vec3(0.0, -0.1, 0.0).relative, Tags.Block.AIR) }
			ifCondition { score(dir, t) lessThanOrEqualTo 0 }
			run { function(grounded) }
		}
		raw("execute unless block ~ ~-0.1 ~ #minecraft:air if score #jp.dir $t matches ..0 run return 0")

		// Sinking: vanilla gravity back, capped by slow falling.
		raw("execute if score #jp.dir $t matches -1 run scoreboard players set #jp.tier $t 9")
		raw("execute if score #jp.dir $t matches -1 run effect clear @s minecraft:levitation")
		raw("execute if score #jp.dir $t matches -1 unless entity @s[tag=${Kits.lift.active}] run effect give @s minecraft:slow_falling 1 0 true")
		// Lift kit sinks faster by simply not cushioning the fall as much. Slow falling is what makes
		// the descent gentle, so the upgrade drops it and leans on the hover servo's gravity handling
		// instead.
		raw("execute if score #jp.dir $t matches -1 if entity @s[tag=${Kits.lift.active}] run effect clear @s minecraft:slow_falling")

		// Climbing: levitation drives the ascent, so gravity only has to stay out of the way.
		// Amplifier 2 is three blocks a second, matching the descent.
		raw("execute if score #jp.dir $t matches 1 run scoreboard players set #jp.tier $t 0")
		raw("execute if score #jp.dir $t matches 1 run effect clear @s minecraft:slow_falling")
		raw("execute if score #jp.dir $t matches 1 unless entity @s[tag=${Kits.lift.active}] run effect give @s minecraft:levitation 1 2 true")
		// Amplifier 5 is about six blocks a second, double the stock climb.
		raw("execute if score #jp.dir $t matches 1 if entity @s[tag=${Kits.lift.active}] run effect give @s minecraft:levitation 1 5 true")

		// Holding: the servo picks the tier.
		raw("execute if score #jp.dir $t matches 0 run effect clear @s minecraft:slow_falling")
		raw("execute if score #jp.dir $t matches 0 run effect clear @s minecraft:levitation")
		execute {
			ifCondition { score(dir, t) equalTo 0 }
			run { function(hold) }
		}

		function(gravity)

		// Idle wash: the thrusters are holding the player's weight even standing still, so the
		// smoke never stops. Count 0 turns the delta into a velocity, which is the only way to make
		// campfire smoke fall instead of rise.
		raw("particle minecraft:campfire_cosy_smoke ~0.15 ~-0.2 ~0.15 0 -1 0 0.08 0 normal $PARTICLE_VIEWERS")
		raw("particle minecraft:campfire_cosy_smoke ~-0.15 ~-0.2 ~-0.15 0 -1 0 0.08 0 normal $PARTICLE_VIEWERS")

		// Under thrust: the same smoke, more of it and pushed harder. No end rods -- the white
		// streaks read as glitch geometry rather than exhaust.
		raw("execute unless score #jp.dir $t matches 0 run particle minecraft:campfire_cosy_smoke ~0.15 ~-0.25 ~-0.15 0 -1 0 0.22 0")
		raw("execute unless score #jp.dir $t matches 0 run particle minecraft:campfire_cosy_smoke ~-0.15 ~-0.25 ~0.15 0 -1 0 0.22 0")
		raw("execute unless score #jp.dir $t matches 0 run particle minecraft:campfire_cosy_smoke ~ ~-0.25 ~ 0 -1 0 0.3 0")
		raw("execute unless score #jp.dir $t matches 0 run particle minecraft:smoke ~ ~-0.15 ~ 0.12 0.02 0.12 0.02 2")

		// The engine runs the whole time hover is holding the player up, hold included -- that is
		// the thrusters carrying their weight.
		function(Engine.sound)

		// Holding station costs fuel too -- the jetpack is carrying the player's weight. Sinking is free.
		execute {
			ifCondition { score(dir, t) greaterThanOrEqualTo 0 }
			run { function(Engine.fuel) }
		}
	}

	/**
	 * Hover mode with a block under the feet. As the player, at the player.
	 *
	 * Two different situations land here, and telling them apart is the whole point:
	 *
	 *   standing, never took off   -> vanilla physics, thrusters idle, jetpack stays on so sneak
	 *                                 plus look up can still lift off
	 *   just landed (ra.jp.air 1)  -> switch off, the same as /trigger ra.jp.power
	 *
	 * Switching off for merely standing made hover unusable: the jetpack turned itself off the
	 * tick after the mode was selected, so there was never an armed jetpack to take off with.
	 */
	private fun Function.grounded() {
		// Vanilla gravity while on the ground, and no leftover flight effects or engine.
		scoreboard.players.set(tier, t, 9)
		function(gravity)
		raw("effect clear @s minecraft:levitation")
		raw("effect clear @s minecraft:slow_falling")
		function(Engine.soundOff)

		// Forget the sampled height, so the first airborne tick measures a fresh speed instead of a
		// jump's worth of it.
		scoreboard.players.reset(self(), Jp.Y)

		// Touchdown. power_toggle *toggles*, so it is only called while the jetpack is on.
		execute {
			ifCondition { score(self(), Jp.AIR) equalTo 1 }
			unlessCondition { score(self(), Jp.OFF) equalTo 1 }
			run { function(Modes.powerToggle) }
		}
		scoreboard.players.set(self(), Jp.AIR, 0)
	}

	/**
	 * Pick a thruster tier that cancels the player's vertical speed. Reads #jp.dy ra.temp
	 * (thousandths of a block per tick), writes #jp.tier ra.temp for [gravity].
	 *
	 * Dead zone is 0.006 blocks a tick, about a tenth of a block a second -- below what anyone can
	 * see, and wide enough that the tier is not flapping every tick. The gentle tier changes speed
	 * by 0.02 a tick and the hard tier by 0.08, so a levitation coast (0.15 a tick) is gone in two
	 * ticks and small residues settle without overshooting into a visible bob.
	 */
	private fun Function.hold() {
		scoreboard.players.set(tier, t, 0)
		raw("execute if score #jp.dy $t matches 6..60 run scoreboard players set #jp.tier $t 1")
		raw("execute if score #jp.dy $t matches 61.. run scoreboard players set #jp.tier $t 2")
		raw("execute if score #jp.dy $t matches -60..-6 run scoreboard players set #jp.tier $t -1")
		raw("execute if score #jp.dy $t matches ..-61 run scoreboard players set #jp.tier $t -2")
	}

	/**
	 * Set the player's gravity to the tier in #jp.tier ra.temp. As the player.
	 *
	 * Only touched when the tier actually changes -- re-adding a modifier that is already there
	 * fails, so every write is a remove followed by an add.
	 */
	private fun Function.gravity() {
		raw("execute if score @s ${Jp.GRAV} = #jp.tier $t run return 0")
		raw("scoreboard players operation @s ${Jp.GRAV} = #jp.tier $t")

		raw("execute if entity @s[tag=${Jp.HOVER_ON}] run attribute @s minecraft:gravity modifier remove ${Jp.HOVER_MODIFIER}")
		raw("tag @s remove ${Jp.HOVER_ON}")

		raw("execute if score #jp.tier $t matches 9 run return 0")

		for ((tierValue, amount) in gravityTiers) {
			raw("execute if score #jp.tier $t matches $tierValue run attribute @s minecraft:gravity modifier add ${Jp.HOVER_MODIFIER} $amount add_value")
		}
		raw("tag @s add ${Jp.HOVER_ON}")
	}
}
