package ra.lib.redstone

import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompound
import ra.core.Obj

/**
 * One world-space neighbour of a block, as the redstone readers address it.
 *
 * Every reader (`side`, `any_side`, `has_input`) takes the same seven arguments, so they are
 * written down once here instead of being retyped on every call:
 *
 *   dx dy dz  offset to the neighbour
 *   side      name of the direction from us towards it
 *   back      the opposite name, i.e. the direction from it back to us
 *   torch     which torch rule applies on this side: "below", "side" or "none"
 *   dust      which dust rule applies on this side: "side", "above" or "none"
 */
internal enum class Side(
	val dx: Int, val dy: Int, val dz: Int,
	val back: String, val torch: String, val dust: String,
	/** `ra.power.<side>`, written by detect. */
	val objective: String,
) {
	NORTH(0, 0, -1, "south", "side", "side", Obj.POWER_NORTH),
	SOUTH(0, 0, 1, "north", "side", "side", Obj.POWER_SOUTH),
	WEST(-1, 0, 0, "east", "side", "side", Obj.POWER_WEST),
	EAST(1, 0, 0, "west", "side", "side", Obj.POWER_EAST),
	DOWN(0, -1, 0, "up", "below", "none", Obj.POWER_DOWN),
	UP(0, 1, 0, "down", "none", "above", Obj.POWER_UP);

	val id = name.lowercase()

	/** `{dx,dy,dz,side,back,torch,dust}`, the argument block of side / any_side / has_input. */
	fun args(): NbtCompound = nbt {
		this["dx"] = dx
		this["dy"] = dy
		this["dz"] = dz
		this["side"] = id
		this["back"] = back
		this["torch"] = torch
		this["dust"] = dust
	}
}

/** ra.facing values: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. */
internal val facings = listOf(Side.DOWN, Side.UP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST)

/**
 * A look-space side (front, back, left...) and the world side it reads for each ra.facing value
 * (index = facing). [fallback] is for a marker with no facing score at all: it is treated as
 * facing south, the default ra_lib:placement/place seeds when there was no placer to read.
 *
 * A block facing straight up or down has no yaw stored, so left, right, up and down on those two
 * facings are a convention rather than a fact -- see `facing/up`. Only front and back are
 * unambiguous for a vertically placed block.
 */
internal enum class LocalSide(val objective: String, val byFacing: List<Side>, val fallback: Side) {
	FRONT(Obj.POWER_FRONT, listOf(Side.DOWN, Side.UP, Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST), Side.SOUTH),
	BACK(Obj.POWER_BACK, listOf(Side.UP, Side.DOWN, Side.SOUTH, Side.NORTH, Side.EAST, Side.WEST), Side.NORTH),
	LEFT(Obj.POWER_LEFT, listOf(Side.EAST, Side.EAST, Side.WEST, Side.EAST, Side.SOUTH, Side.NORTH), Side.EAST),
	RIGHT(Obj.POWER_RIGHT, listOf(Side.WEST, Side.WEST, Side.EAST, Side.WEST, Side.NORTH, Side.SOUTH), Side.WEST),
	UP(Obj.POWER_LOCAL_UP, listOf(Side.SOUTH, Side.NORTH, Side.UP, Side.UP, Side.UP, Side.UP), Side.UP),
	DOWN(Obj.POWER_LOCAL_DOWN, listOf(Side.NORTH, Side.SOUTH, Side.DOWN, Side.DOWN, Side.DOWN, Side.DOWN), Side.DOWN);

	val id = name.lowercase()

	/** The suffix of `ra.powered.<x>` and `ra.power.<x>`: `local_up` / `local_down` for the two vertical ones. */
	val tagName get() = if (name == "UP" || name == "DOWN") "local_$id" else id
}

/**
 * The look-space mapping `detect_local` writes, per facing: which world score each look-space
 * score copies. Unlike the `local/...` readers this is the OLD mapping, kept as it was: on the
 * two vertical facings it is the mirror convention described in `facing/up`.
 */
internal val lookSpace: Map<Side, Map<LocalSide, Side>> = mapOf(
	Side.DOWN to mapOf(LocalSide.FRONT to Side.DOWN, LocalSide.BACK to Side.UP, LocalSide.LEFT to Side.WEST, LocalSide.RIGHT to Side.EAST, LocalSide.UP to Side.NORTH, LocalSide.DOWN to Side.SOUTH),
	Side.EAST to mapOf(LocalSide.FRONT to Side.EAST, LocalSide.BACK to Side.WEST, LocalSide.LEFT to Side.NORTH, LocalSide.RIGHT to Side.SOUTH, LocalSide.UP to Side.UP, LocalSide.DOWN to Side.DOWN),
	Side.NORTH to mapOf(LocalSide.FRONT to Side.NORTH, LocalSide.BACK to Side.SOUTH, LocalSide.LEFT to Side.WEST, LocalSide.RIGHT to Side.EAST, LocalSide.UP to Side.UP, LocalSide.DOWN to Side.DOWN),
	Side.SOUTH to mapOf(LocalSide.FRONT to Side.SOUTH, LocalSide.BACK to Side.NORTH, LocalSide.LEFT to Side.EAST, LocalSide.RIGHT to Side.WEST, LocalSide.UP to Side.UP, LocalSide.DOWN to Side.DOWN),
	Side.UP to mapOf(LocalSide.FRONT to Side.UP, LocalSide.BACK to Side.DOWN, LocalSide.LEFT to Side.EAST, LocalSide.RIGHT to Side.WEST, LocalSide.UP to Side.NORTH, LocalSide.DOWN to Side.SOUTH),
	Side.WEST to mapOf(LocalSide.FRONT to Side.WEST, LocalSide.BACK to Side.EAST, LocalSide.LEFT to Side.SOUTH, LocalSide.RIGHT to Side.NORTH, LocalSide.UP to Side.UP, LocalSide.DOWN to Side.DOWN),
)
