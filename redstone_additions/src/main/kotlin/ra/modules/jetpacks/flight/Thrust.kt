package ra.modules.jetpacks.flight

import io.github.ayfri.kore.arguments.enums.DataType
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.compute
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.predicates.providers.NumberProviderScope
import io.github.ayfri.kore.features.predicates.providers.abs
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.max
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.minus
import io.github.ayfri.kore.features.predicates.providers.plus
import io.github.ayfri.kore.features.predicates.providers.score
import io.github.ayfri.kore.features.predicates.providers.times
import io.github.ayfri.kore.features.predicates.providers.unaryMinus
import io.github.ayfri.kore.features.predicates.types.EntityTarget
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.tmp

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

	/** A score of the player (`@s`, the compute's context entity). */
	private fun NumberProviderScope.mine(objective: String) = score(objective, EntityTarget.THIS)

	/** A score of a fake player on `ra.temp`. */
	private fun NumberProviderScope.temp(holder: String) = score(t, holder)

	private fun Function.thrust() {
		raw("execute store result score #jp.px $t run data get entity @s Pos[0] 1000")
		raw("execute store result score #jp.pz $t run data get entity @s Pos[2] 1000")

		// SMOOTHING, BECAUSE RAW DELTAS JITTER
		// Pushing a fraction of last tick's raw movement made the jetpack shake. A single tick's
		// delta is noisy -- it carries the player's own input, collision nudges, and the previous
		// tick's push -- so feeding it straight back turned every wobble into a teleport of a
		// different size, and a teleport of a different size every tick is exactly what the client
		// renders as jitter.
		//
		// So the delta feeds a running average instead of the push directly:
		//     smoothed = floor((smoothed * 3 + (pos - last)) / 4)
		// which is a low-pass filter with a time constant of about four ticks. The player's actual
		// movement still drives it, but the number the push is built from changes gradually, and
		// gradual is what stops it looking like shaking.
		//
		// It also fixes the runaway from the other direction: the smoothed value cannot jump, so
		// the acceleration curve is smooth rather than compounding tick by tick.
		//
		// One /compute per axis. On the first tick after take-off both the last sample and the
		// smoothed speed are ABSENT scores, and the score providers' fallbacks stand in for them:
		// the last sample falls back to this tick's (a delta of zero), the smoothed speed to 0.
		for ((pos, last, v) in listOf(Triple("#jp.px", Jp.X, Jp.VX), Triple("#jp.pz", Jp.Z, Jp.VZ))) {
			execute {
				storeResult { score(self(), v) }
				run {
					compute(self()) {
						(mine(v) * 3 + (temp(pos) - score(last, EntityTarget.THIS, temp(pos)))).floorDiv(4)
					}
				}
			}
		}
		raw("scoreboard players operation @s ${Jp.X} = #jp.px $t")
		raw("scoreboard players operation @s ${Jp.Z} = #jp.pz $t")

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

		// The push is a fraction of the SMOOTHED speed, not of this tick's delta:
		//     push = clamp(floor(smoothed * k / 100), -cap, cap)
		// The cap (0.35 blocks a tick by default) makes amplifying our own amplification settle at
		// a top speed instead of climbing until the player outruns the chunk loader. The negative
		// bound is derived, not configured twice: two numbers that must stay equal and opposite are
		// one number and a sign. k and cap are configurable, but read once per tick in
		// ra_jetpacks:tick rather than here -- this function runs for every player in the air.
		//
		// Each axis is stored twice from one /compute: as the macro argument, in blocks, and as a
		// score for the dead zone below.
		for ((v, m, arg) in listOf(Triple(Jp.VX, "#jp.mx", "push.dx"), Triple(Jp.VZ, "#jp.mz", "push.dz"))) {
			execute {
				storeResult { score(tmp(m), t) }
				storeResult { storage(Storage.JETPACKS, arg, DataType.DOUBLE, 0.001) }
				run {
					compute(self()) {
						max(min((mine(v) * temp("#jp.cfg.k")).floorDiv(100), temp("#jp.cfg.cap")), -temp("#jp.cfg.cap"))
					}
				}
			}
		}

		// Sprinting on the spot still measures a delta of nearly nothing. Below a couple of
		// centimetres a tick the push is not felt, but it IS still a teleport, so under the
		// threshold do nothing at all.
		execute {
			storeResult { score(tmp("#jp.moving"), t) }
			run { compute { max(abs(temp("#jp.mx")), abs(temp("#jp.mz"))) } }
		}
		raw("execute unless score #jp.moving $t > #jp.cfg.dead $t run return 0")

		function(thrustPush, Storage.JETPACKS, "push")
	}
}
