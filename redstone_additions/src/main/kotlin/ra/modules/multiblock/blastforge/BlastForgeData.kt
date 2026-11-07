package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.functions.Function
import ra.core.raw

/**
 * Blast Forge direction data, written to `storage ra:multiblock bf_dir.<facing>` by
 * ra_multiblock:load and passed to every per-facing macro.
 *
 * All block positions are relative to the base (0,0,0) for each facing. Keys: furnace, in1, in2,
 * out1, ctrl, smoke (string coords for macros); b1-b4 (corner bricks), f1-f9 (floor), c1-c9
 * (ceiling); io_inputs, io_outputs, io_controls (compound metadata for the marker).
 *
 * Kept as the literal compounds the beet version wrote: they are hand-checked coordinates, and
 * the newer ra_lib_multiblock:register path (which derives rotations itself) is what new
 * structures use instead.
 */
object BlastForgeData {
	val facings = listOf("north", "south", "east", "west")

	/** Keys checked against the bricks tag, in check order. */
	val corners = (1..4).map { "b$it" }
	val floor = (1..9).map { "f$it" }
	val ceiling = (1..9).map { "c$it" }

	private val dirs = mapOf(
		"north" to """{facing:"north",furnace:"~ ~ ~-1",in1:"~-1 ~ ~-1",in2:"~1 ~ ~-1",out1:"~ ~ ~-2",ctrl:"~ ~ ~1",smoke:"~ ~0.5 ~-1",b1:"~-1 ~ ~0",b2:"~1 ~ ~0",b3:"~-1 ~ ~-2",b4:"~1 ~ ~-2",f1:"~-1 ~-1 ~-2",f2:"~0 ~-1 ~-2",f3:"~1 ~-1 ~-2",f4:"~-1 ~-1 ~-1",f5:"~0 ~-1 ~-1",f6:"~1 ~-1 ~-1",f7:"~-1 ~-1 ~0",f8:"~0 ~-1 ~0",f9:"~1 ~-1 ~0",c1:"~-1 ~1 ~-2",c2:"~0 ~1 ~-2",c3:"~1 ~1 ~-2",c4:"~-1 ~1 ~-1",c5:"~0 ~1 ~-1",c6:"~1 ~1 ~-1",c7:"~-1 ~1 ~0",c8:"~0 ~1 ~0",c9:"~1 ~1 ~0",io_inputs:{input_1:{x:-1,y:0,z:-1},input_2:{x:1,y:0,z:-1}},io_outputs:{output_1:{x:0,y:0,z:-2}},io_controls:{redstone_in:{x:0,y:0,z:1}}}""",
		"south" to """{facing:"south",furnace:"~ ~ ~1",in1:"~1 ~ ~1",in2:"~-1 ~ ~1",out1:"~ ~ ~2",ctrl:"~ ~ ~-1",smoke:"~ ~0.5 ~1",b1:"~-1 ~ ~0",b2:"~1 ~ ~0",b3:"~-1 ~ ~2",b4:"~1 ~ ~2",f1:"~-1 ~-1 ~0",f2:"~0 ~-1 ~0",f3:"~1 ~-1 ~0",f4:"~-1 ~-1 ~1",f5:"~0 ~-1 ~1",f6:"~1 ~-1 ~1",f7:"~-1 ~-1 ~2",f8:"~0 ~-1 ~2",f9:"~1 ~-1 ~2",c1:"~-1 ~1 ~0",c2:"~0 ~1 ~0",c3:"~1 ~1 ~0",c4:"~-1 ~1 ~1",c5:"~0 ~1 ~1",c6:"~1 ~1 ~1",c7:"~-1 ~1 ~2",c8:"~0 ~1 ~2",c9:"~1 ~1 ~2",io_inputs:{input_1:{x:1,y:0,z:1},input_2:{x:-1,y:0,z:1}},io_outputs:{output_1:{x:0,y:0,z:2}},io_controls:{redstone_in:{x:0,y:0,z:-1}}}""",
		"east" to """{facing:"east",furnace:"~1 ~ ~",in1:"~1 ~ ~-1",in2:"~1 ~ ~1",out1:"~2 ~ ~",ctrl:"~-1 ~ ~",smoke:"~1 ~0.5 ~",b1:"~0 ~ ~-1",b2:"~0 ~ ~1",b3:"~2 ~ ~-1",b4:"~2 ~ ~1",f1:"~0 ~-1 ~-1",f2:"~0 ~-1 ~0",f3:"~0 ~-1 ~1",f4:"~1 ~-1 ~-1",f5:"~1 ~-1 ~0",f6:"~1 ~-1 ~1",f7:"~2 ~-1 ~-1",f8:"~2 ~-1 ~0",f9:"~2 ~-1 ~1",c1:"~0 ~1 ~-1",c2:"~0 ~1 ~0",c3:"~0 ~1 ~1",c4:"~1 ~1 ~-1",c5:"~1 ~1 ~0",c6:"~1 ~1 ~1",c7:"~2 ~1 ~-1",c8:"~2 ~1 ~0",c9:"~2 ~1 ~1",io_inputs:{input_1:{x:1,y:0,z:-1},input_2:{x:1,y:0,z:1}},io_outputs:{output_1:{x:2,y:0,z:0}},io_controls:{redstone_in:{x:-1,y:0,z:0}}}""",
		"west" to """{facing:"west",furnace:"~-1 ~ ~",in1:"~-1 ~ ~1",in2:"~-1 ~ ~-1",out1:"~-2 ~ ~",ctrl:"~1 ~ ~",smoke:"~-1 ~0.5 ~",b1:"~0 ~ ~-1",b2:"~0 ~ ~1",b3:"~-2 ~ ~-1",b4:"~-2 ~ ~1",f1:"~-2 ~-1 ~-1",f2:"~-2 ~-1 ~0",f3:"~-2 ~-1 ~1",f4:"~-1 ~-1 ~-1",f5:"~-1 ~-1 ~0",f6:"~-1 ~-1 ~1",f7:"~0 ~-1 ~-1",f8:"~0 ~-1 ~0",f9:"~0 ~-1 ~1",c1:"~-2 ~1 ~-1",c2:"~-2 ~1 ~0",c3:"~-2 ~1 ~1",c4:"~-1 ~1 ~-1",c5:"~-1 ~1 ~0",c6:"~-1 ~1 ~1",c7:"~0 ~1 ~-1",c8:"~0 ~1 ~0",c9:"~0 ~1 ~1",io_inputs:{input_1:{x:-1,y:0,z:1},input_2:{x:-1,y:0,z:-1}},io_outputs:{output_1:{x:-2,y:0,z:0}},io_controls:{redstone_in:{x:1,y:0,z:0}}}""",
	)

	/** `bf_dir.<facing>`. */
	fun path(facing: String) = "bf_dir.$facing"

	/** Writes all four direction compounds. Part of ra_multiblock:load. */
	fun Function.writeDirections() {
		for (f in facings) raw("data modify storage ra:multiblock ${path(f)} set value ${dirs.getValue(f)}")
	}
}
