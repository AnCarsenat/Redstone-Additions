package ra.multiblocklib.build

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.arguments.enums.DataType
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.arguments.types.ScoreHolderArgument
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.multiblocklib.MB
import ra.multiblocklib.mb
import ra.multiblocklib.mbCopy
import ra.multiblocklib.mbRemove
import ra.multiblocklib.returnUnlessMb

/**
 * `ra_lib_multiblock:build/` functions: derives the south, east and west variants of every registered
 * structure from its north-facing spec. Runs once per load, straight after
 * `#ra_lib_multiblock:load`.
 *
 * This is the part that used to be manual. The Blast Forge carried four hand-written
 * coordinate tables of 26 positions each -- 104 offsets rotated by hand, every one of them a
 * chance to transpose a sign. (It is a compiled spec now, rotated in Kotlin instead.)
 */
object Build {
	val all = ref(Ns.LIB_MULTIBLOCK, "build/all")
	val next = ref(Ns.LIB_MULTIBLOCK, "build/next")
	val type = ref(Ns.LIB_MULTIBLOCK, "build/type")
	val facing = ref(Ns.LIB_MULTIBLOCK, "build/facing")
	val entryNext = ref(Ns.LIB_MULTIBLOCK, "build/entry_next")
	val rotate = ref(Ns.LIB_MULTIBLOCK, "build/rotate")
	val writeBlock = ref(Ns.LIB_MULTIBLOCK, "build/write_block")
	val writeIo = ref(Ns.LIB_MULTIBLOCK, "build/write_io")

	/** Facings in `#mb_rot` order: 0 north, 1 south, 2 east, 3 west. */
	private val FACINGS = listOf("north", "south", "east", "west")

	/** The IO maps, each written by its own pass over the work queue. */
	private val IO_KINDS = listOf("inputs", "outputs", "controls")

	fun Ra.registerBuild() {
		pack.define(all) {
			mbCopy("build_q", "registry_runtime")
			function(next)
			mbRemove("build_q", "build", "work_q", "work", "work_cur")
		}

		// The queue is a sibling of `build` rather than a field inside it: `build` is handed to
		// macro functions as their argument compound, and every field in such a compound is
		// stringified on each call.
		pack.define(next) {
			returnUnlessMb("build_q[0]")
			mbCopy("build.type", "build_q[0]")
			mbRemove("build_q[0]")
			function(type, MB, "build")
			function(next)
		}

		// A structure flagged rotates:0b is rotationally symmetric, so all four facings reuse the
		// authored offsets and no rotation maths runs.
		pack.define(type) {
			data(MB) { modify("build.symmetric") { set(0) } }
			raw("\$execute if data storage ra:multiblock types.\$(type){rotates:0b} run data modify storage ra:multiblock build.symmetric set value 1")
			FACINGS.forEachIndexed { rot, dir ->
				data(MB) { modify("build.facing") { set(dir) } }
				scoreboard.players.set(mb("#mb_rot"), Obj.MULTIBLOCK, rot)
				if (rot > 0) execute {
					ifCondition { data(MB, "build{symmetric:1}") }
					run { scoreboard.players.set(mb("#mb_rot"), Obj.MULTIBLOCK, 0) }
				}
				function(facing, MB, "build")
			}
		}

		pack.define(facing) { facing() }
		pack.define(entryNext) { entryNext() }
		pack.define(rotate) { rotate() }

		// The position is baked into the relative-coordinate string the structure checker feeds
		// to `if block`.
		pack.define(writeBlock) {
			raw("\$data modify storage ra:multiblock types.\$(type).facings.\$(facing).blocks append value {pos:\"~\$(x) ~\$(y) ~\$(z)\",match:\"\$(match)\"}")
		}

		// work.kind selects which of the three maps receives the slot.
		pack.define(writeIo) {
			for (kind in IO_KINDS) {
				raw("\$execute if data storage ra:multiblock work{kind:\"$kind\"} run data modify storage ra:multiblock types.\$(type).facings.\$(facing).$kind.\$(name) set value {x:\$(x),y:\$(y),z:\$(z)}")
			}
		}
	}

	/** `build/facing {type, facing}`: rotate one structure's block list and IO lists into one facing. */
	private fun Function.facing() {
		mbCopy("work.type", "build.type")
		mbCopy("work.facing", "build.facing")

		// Block pattern.
		raw("\$data modify storage ra:multiblock types.\$(type).facings.\$(facing).blocks set value []")
		raw("\$data modify storage ra:multiblock work_q set from storage ra:multiblock types.\$(type).blocks")
		data(MB) { modify("work.kind") { set("blocks") } }
		function(entryNext)

		// IO maps. These end up as compounds keyed by name, which is the shape setup_marker copies
		// onto the marker and the shape the io/ helpers read.
		for (kind in IO_KINDS) raw("\$data modify storage ra:multiblock types.\$(type).facings.\$(facing).$kind set value {}")
		for (kind in IO_KINDS) {
			raw("\$data modify storage ra:multiblock work_q set from storage ra:multiblock types.\$(type).$kind")
			data(MB) { modify("work.kind") { set(kind) } }
			function(entryNext)
		}
	}

	/** Rotate one entry off the work queue and write it into the destination `work.kind` selects, then recurse. */
	private fun Function.entryNext() {
		returnUnlessMb("work_q[0]")

		mbCopy("work_cur", "work_q[0]")
		mbRemove("work_q[0]")

		for (axis in "xyz") execute {
			storeResult { score(mb("#mb_$axis"), Obj.MULTIBLOCK) }
			run { data(MB) { get("work_cur.$axis") } }
		}
		function(rotate)

		for (axis in "xyz") execute {
			storeResult { storage(MB, "work_cur.$axis", DataType.INT, 1.0) }
			run { scoreboard.players.get(mb("#mb_r$axis"), Obj.MULTIBLOCK) }
		}

		// The writer is a macro, so the type and facing travel with the entry.
		mbCopy("work_cur.type", "work.type")
		mbCopy("work_cur.facing", "work.facing")

		execute {
			ifCondition { data(MB, "work{kind:\"blocks\"}") }
			run { function(writeBlock, MB, "work_cur") }
		}
		execute {
			unlessCondition { data(MB, "work{kind:\"blocks\"}") }
			run { function(writeIo, MB, "work_cur") }
		}

		function(entryNext)
	}

	/**
	 * Rotate (#mb_x,#mb_y,#mb_z) about the vertical axis into (#mb_rx,#mb_ry,#mb_rz), for the
	 * facing selected by #mb_rot.
	 *
	 * Offsets are authored for a north-facing structure, where "in front of the base" is -Z.
	 * The three other facings are the quarter turns of that:
	 *   north (0)  ( x, y,  z)
	 *   south (1)  (-x, y, -z)
	 *   east  (2)  (-z, y,  x)
	 *   west  (3)  ( z, y, -x)
	 * Height never changes, so ry is always y.
	 */
	private fun Function.rotate() {
		op(mb("#mb_ry"), Operation.SET, mb("#mb_y"))

		// Per facing: (target, source, negate) in the order the operations run.
		val steps = listOf(
			listOf(Triple("rx", "x", false), Triple("rz", "z", false)),
			listOf(Triple("rx", "x", true), Triple("rz", "z", true)),
			listOf(Triple("rx", "z", true), Triple("rz", "x", false)),
			listOf(Triple("rx", "z", false), Triple("rz", "x", true)),
		)
		steps.forEachIndexed { rot, ops ->
			for ((to, from, negate) in ops) {
				onRot(rot) { op(mb("#mb_$to"), Operation.SET, mb("#mb_$from")) }
				if (negate) onRot(rot) { op(mb("#mb_$to"), Operation.MULTIPLY, mb("#mb_neg")) }
			}
		}
	}

	private fun Function.onRot(rot: Int, block: Function.() -> Unit) = execute {
		ifCondition { score(mb("#mb_rot"), Obj.MULTIBLOCK) equalTo rot }
		run(block)
	}

	private fun Function.op(target: ScoreHolderArgument, operation: Operation, source: ScoreHolderArgument) =
		scoreboard.players.operation(target, Obj.MULTIBLOCK, operation, source, Obj.MULTIBLOCK)
}
