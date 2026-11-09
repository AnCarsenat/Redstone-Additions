package ra.modules.wires.blocks.water_wheel

import io.github.ayfri.kore.functions.Function
import ra.core.raw
import java.util.Locale

/**
 * The turning wheel: [ARMS] arms of two block displays each (a spoke and a paddle) standing at the
 * marker, so the whole wheel turns about the block centre through `left_rotation` alone.
 *
 * WHY ONLY THE ROTATION MOVES. A display draws its block from its own position outwards
 * (`translation + left_rotation(scale(model))`), so with the translation kept on the axle (only
 * along it, to centre the arm's width) every point of the arm turns on a circle about the axle.
 * The client interpolates the rotation with a slerp, so a step of [STEP_DEGREES] or two spread
 * over [WaterWheel.PERIOD] ticks is a smooth turn; moving the translation instead would cut
 * across the circle in a straight line.
 *
 * WHERE THE PADDLE SITS. The paddle is a top slab: its model fills only the upper half of the
 * unit cube, so scaled to [PADDLE_LENGTH] it runs from half that length out to all of it, a board
 * on the end of the spoke, without a translation that would have to turn with it.
 *
 * Slot `s` is the angle `s * STEP_DEGREES`; arm `i` sits [SLOTS] / [ARMS] slots after arm `i - 1`.
 * Turning is moving every arm on by one or two slots, which [spin] does with one selector per arm.
 */
internal object WaterWheelModel {
	const val ARMS = 8
	const val SLOTS = 16
	const val STEP_DEGREES = 360.0 / SLOTS

	/** Tag on every display of a wheel; `<tag>.arm<i>` picks one arm (its spoke and its paddle). */
	const val TAG = "ra.wires.water_wheel"

	private const val SPOKE = "minecraft:stripped_spruce_log"
	private const val PADDLE = "minecraft:spruce_slab"
	private const val SPOKE_THICK = 0.14
	private const val SPOKE_LENGTH = 0.9
	private const val PADDLE_LENGTH = 1.5
	private const val PADDLE_WIDTH = 0.9
	private const val PADDLE_THICK = 0.1

	/** The axle: `x` for a wheel turning in the Y-Z plane, `z` for one in the X-Y plane. */
	enum class Axis(val vector: String) { X("1f,0f,0f"), Z("0f,0f,1f") }

	private fun f(v: Double) = String.format(Locale.ROOT, "%.4ff", v)

	fun angle(slot: Int) = f(Math.toRadians(STEP_DEGREES * (slot % SLOTS)))

	/** Scale and translation along/across the axle: [along, radial, across]. */
	private fun vec(axis: Axis, along: Double, radial: Double, across: Double) = when (axis) {
		Axis.X -> "[${f(along)},${f(radial)},${f(across)}]"
		Axis.Z -> "[${f(across)},${f(radial)},${f(along)}]"
	}

	private fun summon(axis: Axis, arm: Int, block: String, width: Double, length: Double, thick: Double) =
		"summon block_display ~ ~ ~ {Tags:[\"ra\",\"ra.display\",\"$TAG\",\"$TAG.arm$arm\"]," +
			"brightness:{sky:15,block:0},interpolation_duration:${WaterWheel.PERIOD},block_state:$block," +
			"transformation:{translation:${vec(axis, -width / 2, 0.0, 0.0)},left_rotation:{angle:${angle(arm * SLOTS / ARMS)},axis:[${axis.vector}]}," +
			"scale:${vec(axis, width, length, thick)},right_rotation:[0f,0f,0f,1f]}}"

	/** The whole wheel at slot 0, as the marker, at it. The caller removes any old one first. */
	fun Function.build(axis: Axis) {
		for (arm in 0 until ARMS) {
			raw(summon(axis, arm, "\"$SPOKE[axis=y]\"", SPOKE_THICK, SPOKE_LENGTH, SPOKE_THICK))
			raw(summon(axis, arm, "\"$PADDLE[type=top]\"", PADDLE_WIDTH, PADDLE_LENGTH, PADDLE_THICK))
		}
	}

	/**
	 * Macro {axis}: every arm to its place at wheel position [k] (0 until [SLOTS]), interpolated
	 * over one clock period. A merge into `transformation` keeps its translation and scale: only
	 * the rotation changes. As the marker, at it.
	 */
	fun Function.spin(k: Int) {
		for (arm in 0 until ARMS) {
			raw("\$execute as @e[type=block_display,tag=$TAG.arm$arm,distance=..0.01] run data merge entity @s " +
				"{start_interpolation:0,interpolation_duration:${WaterWheel.PERIOD},transformation:{left_rotation:{angle:${angle(arm * SLOTS / ARMS + k)},axis:[\$(axis)]}}}")
		}
	}
}
