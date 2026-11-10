package ra.modules.jetpacks.flight

import io.github.ayfri.kore.features.predicates.providers.ContextIntProvider
import io.github.ayfri.kore.features.predicates.providers.coerceAtLeast
import io.github.ayfri.kore.features.predicates.providers.coerceIn
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.minus
import io.github.ayfri.kore.features.predicates.providers.plus
import io.github.ayfri.kore.features.predicates.providers.times
import io.github.ayfri.kore.features.predicates.providers.unaryMinus
import ra.core.Ns
import ra.core.namedProvider
import ra.core.provider

/**
 * THE THRUSTER'S EASING CURVE, IN ONE PLACE
 *
 * The Thruster used to be a switch: sprint and the full push arrived on the next tick, let go and
 * it vanished on the next. [Jp.RAMP][ra.modules.jetpacks.Jp.RAMP] is a 0..100 throttle that eases
 * towards its target instead, and the push is scaled by it.
 *
 * Each tick the throttle covers a quarter of the remaining gap, and never less than [MIN_STEP],
 * so it settles instead of creeping forever:
 *
 *     up:   0 -> 25 -> 43 -> 57 -> 67 -> 75 -> 81 -> 87 -> 93 -> 99 -> 100   (half a second)
 *     down: 100 -> 75 -> 57 -> 43 -> 33 -> 25 -> 19 -> 13 -> 7 -> 1 -> 0
 *
 * Fast at first and gentle at the end on both ways: the push is felt at once but never lands as a
 * step. [up] and [down] are the whole formula; applied to the player's own throttle they are the
 * named providers [UP] and [DOWN], which is what the Thruster runs.
 */
object ThrustRamp {
	/** Named provider `ra_jetpacks:flight/ramp_up`: [up] of the player's own `ra.jp.ramp` (`compute entity @s`). */
	val UP = namedProvider(Ns.JETPACKS, "flight/ramp_up")

	/** Named provider `ra_jetpacks:flight/ramp_down`: [down] of the player's own `ra.jp.ramp`. */
	val DOWN = namedProvider(Ns.JETPACKS, "flight/ramp_down")

	/** Full throttle. */
	const val FULL = 100

	/** Smallest change per tick, so the ease reaches 0 and [FULL] in a handful of ticks. */
	const val MIN_STEP = 6

	/** The throttle one tick closer to [FULL]. [cur] is the current throttle (absent reads as 0). */
	fun up(cur: ContextIntProvider): ContextIntProvider = provider {
		(cur + (-cur + FULL).floorDiv(4).coerceAtLeast(MIN_STEP)).coerceIn(0, FULL)
	}

	/** The throttle one tick closer to 0. */
	fun down(cur: ContextIntProvider): ContextIntProvider = provider {
		(cur - cur.floorDiv(4).coerceAtLeast(MIN_STEP)).coerceIn(0, FULL)
	}

	/** [push] (thousandths of a block) scaled by the throttle [ramp]. */
	fun scaled(push: ContextIntProvider, ramp: ContextIntProvider): ContextIntProvider = provider {
		(push * ramp).floorDiv(FULL)
	}
}
