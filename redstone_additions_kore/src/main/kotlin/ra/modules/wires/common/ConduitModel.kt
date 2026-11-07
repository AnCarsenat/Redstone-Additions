package ra.modules.wires.common

import io.github.ayfri.kore.functions.Function
import ra.core.raw
import java.math.BigDecimal

/**
 * The block-display model of a pipe or wire: a cube core and, per connected side, a bar from the
 * core out to the block boundary (never across it -- see common/update_pipe_displays_copper).
 *
 * Both looks are the same shape at two sizes, so they are generated from [half], the core's
 * half-width: the core is `2*half` wide and a link is `0.5 - half` long.
 */
data class ConduitModel(val display: String, val node: String, val block: String, val half: String)

val PIPE = ConduitModel("ra.wires.pipe_display", "ra.wires.fluid_node", "copper_block", "0.28")
val WIRE = ConduitModel("ra.wires.wire_display", "ra.wires.electric_node", "orange_concrete", "0.13")

/** A neighbour direction: where it is, the way back, and which axis (0 x, 1 y, 2 z) it lies on. */
data class Side(val offset: String, val back: String, val axis: Int, val positive: Boolean)

/** The six neighbours, in the order every per-side list in this module uses. */
val SIDES = listOf(
	Side("~1 ~ ~", "~-1 ~ ~", 0, true),
	Side("~-1 ~ ~", "~1 ~ ~", 0, false),
	Side("~ ~ ~1", "~ ~ ~-1", 2, true),
	Side("~ ~ ~-1", "~ ~ ~1", 2, false),
	Side("~ ~1 ~", "~ ~-1 ~", 1, true),
	Side("~ ~-1 ~", "~ ~1 ~", 1, false),
)

private fun f(v: BigDecimal) = v.stripTrailingZeros().toPlainString() + "f"

private fun ConduitModel.summon(kind: String, translation: List<BigDecimal>, scale: List<BigDecimal>) =
	// `block_state:{id:...}` is the 26.3 shape (what overlay_121 shipped).
	"summon block_display ~ ~ ~ {Tags:[\"ra\",\"$display\",\"$display.$kind\"],block_state:{id:\"minecraft:$block\"}," +
		"transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]," +
		"translation:[${translation.joinToString(",") { f(it) }}],scale:[${scale.joinToString(",") { f(it) }}]}}"

/** The core cube, centred on the block. */
fun Function.conduitCore(model: ConduitModel) {
	val h = BigDecimal(model.half)
	raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 run " + model.summon("center", List(3) { h.negate() }, List(3) { h + h }))
}

/** One half-connector per side that has a node of the same kind next to it. */
fun Function.conduitLinks(model: ConduitModel) {
	val h = BigDecimal(model.half)
	val len = BigDecimal("0.5") - h
	for (side in SIDES) {
		val translation = List(3) { if (it == side.axis) (if (side.positive) h else BigDecimal("-0.5")) else h.negate() }
		val scale = List(3) { if (it == side.axis) len else h + h }
		raw("execute positioned ${side.offset} if entity @e[type=marker,tag=${model.node},distance=..0.75,limit=1,sort=nearest] " +
			"positioned ${side.back} align xyz positioned ~0.5 ~0.5 ~0.5 run " + model.summon("link", translation, scale))
	}
}
