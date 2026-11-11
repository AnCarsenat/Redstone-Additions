package ra.modules.displays

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.readFacing
import ra.core.ref
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A DISPLAY'S TILT: the `rotation` property (a facing, [DisplayStyle.ROTATION]) as a quaternion
 * and a pivot correction, computed once per restyle with scoreboard arithmetic.
 *
 * The game has no sine, so [trigInit] writes a table at load, `storage ra:displays trig`:
 * `hs`/`hc` the sine and cosine of half the angle, `s`/`c` of the angle, x 10000, one entry per
 * whole degree from -180 to 180 (index = angle + 180). [readTilt] reads eight entries through one
 * macro ([trigAt]) whose two arguments are the yaw and pitch as clamped integers, stored with
 * `store result ... int`: no text, typed or not, reaches it.
 *
 * Q = Ry(-yaw) * Rx(pitch) = (cy sp, -sy cp, sy sp, cy cp), with cy = cos(yaw/2) and so on: into
 * storage ra:temp dsp.rr, the right_rotation of every tilted part. A text turns about its line's
 * centre c = (0, h, 0) instead of its origin: its translation gets L S (c - Qc), and
 * c - Qc = h (sin p sin y, 1 - cos p, -sin p cos y). [readTilt] leaves that vector, x 10000 and
 * times the scale percent / 100, in `#dsp.e0..2`; [pivotShift] is the per-part matrix L S h that
 * [DisplayStyle]'s form multiplies it by. An untilted display (0, 0) skips all of it.
 */
internal object DisplayTilt {
	val trigInit = ref(Ns.DISPLAYS, "config/trig_init")
	private val trigAt = ref(Ns.DISPLAYS, "config/trig_at")

	private const val T = Obj.TEMP
	private const val K = "#dsp.k"
	private const val TRIG = "storage ra:displays trig"

	private fun table(f: (Double) -> Double) =
		(-180..180).joinToString(",", "[I;", "]") { (f(Math.toRadians(it.toDouble())) * 10000).roundToInt().toString() }

	fun Ra.registerDisplayTilt() {
		pack.define(trigInit) {
			raw("data modify $TRIG set value {hs:${table { sin(it / 2) }},hc:${table { cos(it / 2) }},s:${table { sin(it) }},c:${table { cos(it) }}}")
		}
		// {y, p}: the table indexes of the yaw and the pitch.
		pack.define(trigAt) {
			for ((a, i) in listOf("y" to "y", "p" to "p")) for (k in listOf("hs", "hc", "s", "c"))
				raw("\$execute store result score #dsp.$k$a $T run data get $TRIG.$k[\$($i)]")
		}
	}

	/** In a restyle's read, as the marker, after the scale is in [K]. */
	fun Function.readTilt() {
		readFacing("rotation", DisplayStyle.ROTATION)
		raw("data modify storage ra:temp dsp.rr set value [0f,0f,0f,1f]")
		for (j in 0..2) raw("scoreboard players set #dsp.e$j $T 0")
		raw("execute if score #fc.yaw $T matches 0 if score #fc.pitch $T matches 0 run return 0")
		raw("scoreboard players add #fc.yaw $T 180")
		raw("scoreboard players add #fc.pitch $T 180")
		raw("data modify storage ra:temp dsp.ang set value {y:180,p:180}")
		raw("execute store result storage ra:temp dsp.ang.y int 1 run scoreboard players get #fc.yaw $T")
		raw("execute store result storage ra:temp dsp.ang.p int 1 run scoreboard players get #fc.pitch $T")
		raw("function ${trigAt.asId()} with storage ra:temp dsp.ang")
		// Q, x 1e8.
		fun product(into: String, a: String, b: String) {
			raw("scoreboard players operation $into $T = $a $T")
			raw("scoreboard players operation $into $T *= $b $T")
		}
		product("#dsp.q0", "#dsp.hcy", "#dsp.hsp")
		product("#dsp.q1", "#dsp.hsy", "#dsp.hcp")
		raw("scoreboard players set #dsp.m1 $T -1")
		raw("scoreboard players operation #dsp.q1 $T *= #dsp.m1 $T")
		product("#dsp.q2", "#dsp.hsy", "#dsp.hsp")
		product("#dsp.q3", "#dsp.hcy", "#dsp.hcp")
		for (i in 0..3) raw("execute store result storage ra:temp dsp.rr[$i] float 0.00000001 run scoreboard players get #dsp.q$i $T")
		// c - Qc over h, x 1e4, then times the scale percent / 100.
		raw("scoreboard players set #dsp.k1e4 $T 10000")
		raw("scoreboard players set #dsp.k100 $T 100")
		product("#dsp.e0", "#dsp.sp", "#dsp.sy")
		raw("scoreboard players operation #dsp.e0 $T /= #dsp.k1e4 $T")
		raw("scoreboard players set #dsp.e1 $T 10000")
		raw("scoreboard players operation #dsp.e1 $T -= #dsp.cp $T")
		product("#dsp.e2", "#dsp.sp", "#dsp.cy")
		raw("scoreboard players operation #dsp.e2 $T /= #dsp.k1e4 $T")
		raw("scoreboard players operation #dsp.e2 $T *= #dsp.m1 $T")
		for (j in 0..2) {
			raw("scoreboard players operation #dsp.e$j $T *= $K $T")
			raw("scoreboard players operation #dsp.e$j $T /= #dsp.k100 $T")
		}
	}

	/**
	 * The matrix that turns `#dsp.e0..2` into a translation in 1e-7 block for [f]: its roll L times
	 * its scale S times its pivot height, x 1000; null when it has no pivot. Entry [i][j] is how much
	 * of e_j goes into translation axis i.
	 */
	fun pivotShift(f: Form): List<List<Int>>? {
		if (f.pivot == 0.0) return null
		val r = Math.toRadians(f.roll.toDouble())
		val roll = listOf(listOf(cos(r), -sin(r), 0.0), listOf(sin(r), cos(r), 0.0), listOf(0.0, 0.0, 1.0))
		return (0..2).map { i -> (0..2).map { j -> (roll[i][j] * f.s[j] * f.pivot * 1000).roundToInt() } }
	}
}
