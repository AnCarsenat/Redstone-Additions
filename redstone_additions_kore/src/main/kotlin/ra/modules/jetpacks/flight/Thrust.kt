package ra.modules.jetpacks.flight

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp

/**
 * Thruster kit: actually make the player go faster horizontally. As the player, at the player.
 *
 * WHY THIS IS NOT AN ATTRIBUTE
 * It was `minecraft:movement_speed`, +120%, and it did nothing you could feel. That attribute
 * governs walking. Once you are off the ground, horizontal movement is driven by a much smaller
 * air-control factor instead, so raising walk speed while flying raises a number that is barely
 * being read. It is not a Minecraft bug, it is the wrong lever.
 *
 * The lever that works is displacement. Measure how far the player actually moved horizontally
 * last tick and add a fraction of it back on top, which means:
 *   - standing still in hover does nothing at all, because the delta is zero
 *   - it pushes whichever way you are ACTUALLY going, so strafing and flying backwards get the
 *     same help as flying forwards, and looking around does not veer you off course the way a
 *     look-direction shove would
 *
 * It feeds on its own output -- next tick's delta includes this tick's push -- so it accelerates
 * rather than jumping to speed, and the cap is what stops that being a runaway. Let go and air
 * drag shrinks the delta, so the boost fades with it instead of having to be switched off.
 */
object Thrust {
	val thrust = ref(Ns.JETPACKS, "flight/thrust")
	/** Macro `{dx, dz}`. */
	val thrustPush = ref(Ns.JETPACKS, "flight/thrust_push")

	private val t = Obj.TEMP

	fun Ra.registerThrust() {
		pack.define(thrust) { thrust() }

		// Move the player by dx/dz, unless that would put them inside a wall.
		//
		// `tp` does not collide. Without these two checks the thruster would post the player
		// through the far side of anything they flew into, which is both a terrible feeling and a
		// way into places a player should have to dig for. Both blocks the player occupies are
		// tested, so clearance at the feet is not enough.
		pack.define(thrustPush) {
			raw("\$execute positioned ~\$(dx) ~ ~\$(dz) unless block ~ ~ ~ #minecraft:air run return 0")
			raw("\$execute positioned ~\$(dx) ~ ~\$(dz) unless block ~ ~1 ~ #minecraft:air run return 0")
			raw("\$tp @s ~\$(dx) ~ ~\$(dz)")
		}
	}

	private fun Function.op(target: String, obj: String, op: String, source: String, srcObj: String) =
		raw("scoreboard players operation $target $obj $op $source $srcObj")

	private fun Function.thrust() {
		raw("execute store result score #jp.px $t run data get entity @s Pos[0] 1000")
		raw("execute store result score #jp.pz $t run data get entity @s Pos[2] 1000")

		// No previous sample on the first tick after take-off. A score that has never been set is
		// ABSENT, and an absent score does not even equal itself.
		raw("execute unless score @s ${Jp.X} = @s ${Jp.X} run scoreboard players operation @s ${Jp.X} = #jp.px $t")
		raw("execute unless score @s ${Jp.Z} = @s ${Jp.Z} run scoreboard players operation @s ${Jp.Z} = #jp.pz $t")

		op("#jp.mx", t, "=", "#jp.px", t)
		op("#jp.mx", t, "-=", "@s", Jp.X)
		op("#jp.mz", t, "=", "#jp.pz", t)
		op("#jp.mz", t, "-=", "@s", Jp.Z)

		op("@s", Jp.X, "=", "#jp.px", t)
		op("@s", Jp.Z, "=", "#jp.pz", t)

		// SMOOTHING, BECAUSE RAW DELTAS JITTER
		// Pushing a fraction of last tick's raw movement made the jetpack shake. A single tick's
		// delta is noisy -- it carries the player's own input, collision nudges, and the previous
		// tick's push -- so feeding it straight back turned every wobble into a teleport of a
		// different size, and a teleport of a different size every tick is exactly what the client
		// renders as jitter.
		//
		// So the delta feeds a running average instead of the push directly:
		//     smoothed = (smoothed * 3 + delta) / 4
		// which is a low-pass filter with a time constant of about four ticks. The player's actual
		// movement still drives it, but the number the push is built from changes gradually, and
		// gradual is what stops it looking like shaking.
		//
		// It also fixes the runaway from the other direction: the smoothed value cannot jump, so
		// the acceleration curve is smooth rather than compounding tick by tick.
		raw("execute unless score @s ${Jp.VX} = @s ${Jp.VX} run scoreboard players set @s ${Jp.VX} 0")
		raw("execute unless score @s ${Jp.VZ} = @s ${Jp.VZ} run scoreboard players set @s ${Jp.VZ} 0")

		raw("scoreboard players set #jp.three $t 3")
		raw("scoreboard players set #jp.four $t 4")

		for ((v, m) in listOf(Jp.VX to "#jp.mx", Jp.VZ to "#jp.mz")) {
			op("@s", v, "*=", "#jp.three", t)
			op("@s", v, "+=", m, t)
			op("@s", v, "/=", "#jp.four", t)
		}

		// The push is a fraction of the SMOOTHED speed, not of this tick's delta.
		op("#jp.mx", t, "=", "@s", Jp.VX)
		op("#jp.mz", t, "=", "@s", Jp.VZ)

		// Configurable, but read once per tick in ra_jetpacks:tick rather than here -- this
		// function runs for every player in the air.
		op("#jp.k", t, "=", "#jp.cfg.k", t)
		raw("scoreboard players set #jp.c100 $t 100")
		op("#jp.mx", t, "*=", "#jp.k", t)
		op("#jp.mx", t, "/=", "#jp.c100", t)
		op("#jp.mz", t, "*=", "#jp.k", t)
		op("#jp.mz", t, "/=", "#jp.c100", t)

		// Cap the push at 0.35 blocks a tick each way, so amplifying our own amplification settles
		// at a top speed instead of climbing until the player outruns the chunk loader.
		op("#jp.cap", t, "=", "#jp.cfg.cap", t)
		// The negative bound is derived, not configured twice: two numbers that must stay equal
		// and opposite are one number and a sign.
		raw("scoreboard players set #jp.neg $t -1")
		op("#jp.ncap", t, "=", "#jp.cap", t)
		op("#jp.ncap", t, "*=", "#jp.neg", t)
		for (m in listOf("#jp.mx", "#jp.mz")) {
			raw("execute if score $m $t > #jp.cap $t run scoreboard players operation $m $t = #jp.cap $t")
			raw("execute if score $m $t < #jp.ncap $t run scoreboard players operation $m $t = #jp.ncap $t")
		}

		// IT ENGAGES ON SPRINT, NOT ON SPEED
		// A data pack cannot set a player's velocity. The only server-side ways to move a player
		// are teleporting, knockback and vehicles -- so a continuous boost has to be a teleport a
		// tick, and that reads as jitter no matter how well the magnitude is smoothed. That is the
		// mechanism, not the tuning. What can be chosen is WHEN it fires.
		//
		// The first attempt was a speed floor: engage above 0.18 blocks a tick. That was the wrong
		// signal twice over. In classic mode you fly by HOLDING SNEAK, and horizontal movement in
		// the air never gets near 0.18 -- air control is a small fraction of walking speed -- so the
		// thruster simply never switched on. And a speed floor cannot tell crossing terrain from
		// lining up a block, because both can be slow.
		//
		// Sprint is the signal, and it is better than a threshold on every count: it is an explicit
		// thing the player does, it means "go fast" in every other context in the game, it works
		// identically in both flight modes, and nobody sprints while placing blocks -- which is
		// exactly where the jitter was unwelcome.
		raw("execute unless predicate ra:is_sprinting run tag @s remove ${Jp.THRUSTING}")
		raw("execute unless predicate ra:is_sprinting run return 0")
		raw("tag @s add ${Jp.THRUSTING}")

		// Sprinting on the spot still measures a delta of nearly nothing. Below a couple of
		// centimetres a tick the push is not felt, but it IS still a teleport, so under the
		// threshold do nothing at all.
		op("#jp.dead", t, "=", "#jp.cfg.dead", t)
		raw("scoreboard players set #jp.neg $t -1")
		op("#jp.ndead", t, "=", "#jp.dead", t)
		op("#jp.ndead", t, "*=", "#jp.neg", t)
		raw("scoreboard players set #jp.moving $t 0")
		for (m in listOf("#jp.mx", "#jp.mz")) {
			raw("execute if score $m $t > #jp.dead $t run scoreboard players set #jp.moving $t 1")
			raw("execute if score $m $t < #jp.ndead $t run scoreboard players set #jp.moving $t 1")
		}
		raw("execute if score #jp.moving $t matches 0 run return 0")

		raw("execute store result storage ra:jetpacks push.dx double 0.001 run scoreboard players get #jp.mx $t")
		raw("execute store result storage ra:jetpacks push.dz double 0.001 run scoreboard players get #jp.mz $t")
		function(thrustPush, Storage.JETPACKS, "push")
	}
}
