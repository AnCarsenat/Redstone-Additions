package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import ra.api.Goggles
import ra.api.MultiblockLib
import ra.api.MultiblockSpec
import ra.api.multiblock
import ra.api.multiblockSpec
import ra.api.pos
import ra.core.Ns
import ra.core.Ra
import ra.core.Storage
import ra.core.TagRoute
import ra.core.define
import ra.core.hook
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.blastforge.BlastForgeTick.registerBlastForgeTick
import ra.modules.multiblock.forEachMarker
import ra.modules.multiblock.goggles.ShowIoLabel

/**
 * Blast Forge (`ra_multiblock:blast_forge/...`): a 3x3x3 nether brick shell around a blast furnace,
 * assembled on a copper base, that smelts at double yield and recycles gear, fed by heat.
 *
 * A compiled multiblock ([SPEC]): ra_lib_multiblock generates its predicates, validator, periodic
 * check, tagger, debug report and its place in `try_tier {tier:"copper"}`. This file adds the
 * tick and the goggles.
 *
 *   Middle layer, top view, facing north (front = -Z, at the top):
 *
 *     brick  OUT     brick      z = -2    OUT = output_1 barrel
 *     IN1    FURN    IN2        z = -1    IN1 = input_1 barrel (material), IN2 = input_2 (fuel)
 *     brick  BASE    brick      z =  0    FURN = blast furnace, BASE = copper base
 *            (ctrl)             z = +1    ctrl = redstone_in: a redstone block here locks it
 *
 * The layers below (y = -1) and above (y = +1) are full 3x3s of bricks.
 */
object BlastForge {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "blast_forge/$path")

	const val ID = "blast_forge"

	/** `#ra_multiblock:blast_forge_bricks`: any nether brick variant counts. */
	const val BRICKS = "#ra_multiblock:blast_forge_bricks"

	/** North-facing offsets of the named positions the tick uses, relative to the base. */
	val FURNACE = MultiblockSpec.Pos(0, 0, -1)
	val INPUT_1 = MultiblockSpec.Pos(-1, 0, -1)
	val INPUT_2 = MultiblockSpec.Pos(1, 0, -1)
	val OUTPUT_1 = MultiblockSpec.Pos(0, 0, -2)
	val REDSTONE_IN = MultiblockSpec.Pos(0, 0, 1)

	/**
	 * The base itself (0, 0, 0) is not part of the pattern: the base's own break detection
	 * handles it, as it did before the structure was compiled.
	 */
	val SPEC = multiblockSpec(ID, "Blast Forge", "copper") {
		hint = "Blast Forge: 3x3x3 nether bricks + blast furnace + 2 barrels (input) + 1 barrel (output)"
		block(FURNACE.x, FURNACE.y, FURNACE.z, "minecraft:blast_furnace")
		blocks("minecraft:barrel", INPUT_1, INPUT_2, OUTPUT_1)
		// Middle layer: the four corners around the furnace column.
		blocks(BRICKS, pos(-1, 0, 0), pos(1, 0, 0), pos(-1, 0, -2), pos(1, 0, -2))
		// Floor and ceiling: full 3x3s.
		box(-1..1, -1..-1, -2..0, BRICKS)
		box(-1..1, 1..1, -2..0, BRICKS)
		input("input_1", INPUT_1.x, INPUT_1.y, INPUT_1.z)
		input("input_2", INPUT_2.x, INPUT_2.y, INPUT_2.z)
		output("output_1", OUTPUT_1.x, OUTPUT_1.y, OUTPUT_1.z)
		control("redstone_in", REDSTONE_IN.x, REDSTONE_IN.y, REDSTONE_IN.z)
	}

	/** `ra.multiblock.blast_forge`, given by the library's generated setup_type. */
	val TAG = SPEC.tag

	val tickDispatch = fn("tick_dispatch")
	val tick = fn("tick")
	val goggles = fn("goggles")

	fun Ra.registerBlastForge() {
		multiblock(SPEC)

		pack.blockTag("blast_forge_bricks", Ns.MULTIBLOCK.id) {
			add(Blocks.NETHER_BRICKS)
			add(Blocks.RED_NETHER_BRICKS)
			add(Blocks.CRACKED_NETHER_BRICKS)
			add(Blocks.CHISELED_NETHER_BRICKS)
		}

		// No on_break: items stay in the barrels, where players can reach them.
		pack.define(tickDispatch) { forEachMarker(TAG, tick) }
		pack.define(BlastForge.goggles) { goggles() }

		registerBlastForgeTick()

		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
		multiblockGoggles += TagRoute(TAG, BlastForge.goggles)
	}

	/** As the marker, at the base: name, status, and I/O labels above the structure. */
	private fun Function.goggles() {
		// Name and status as one billboard (see ra.tools.goggles.billboard), its base 0.3 above the
		// ceiling: the marker stands on the base block's floor and the ceiling's top face is 2 up.
		raw("""data modify storage ra:temp gg.line set value {text:"Blast Forge",color:"green",bold:true}""")
		function(Goggles.Billboard.append)
		function(Goggles.Status.readStatusMultiblock)
		executeRun("positioned ~ ~2.3 ~", Goggles.Billboard.flushHere)

		// I/O indicators: copy the IO block offset to temp storage, add label + color, and call
		// the shared show_io_label macro.
		ioLabel("inputs.input_1", "Input (Material)", "green")
		ioLabel("inputs.input_2", "Input (Fuel)", "green")
		ioLabel("outputs.output_1", "Output", "red")
		// Control (redstone): outside the structure.
		ioLabel("controls.redstone_in", "Redstone Control", "yellow")
	}

	private fun Function.ioLabel(port: String, label: String, color: String) {
		raw("execute if data entity @s data.$port run data modify storage ra:temp io set from entity @s data.$port")
		raw("execute if data entity @s data.$port run data modify storage ra:temp io.label set value \"$label\"")
		raw("execute if data entity @s data.$port run data modify storage ra:temp io.color set value \"$color\"")
		execute {
			ifCondition { data(self(), "data.$port") }
			run { function(ShowIoLabel.fn, Storage.TEMP, "io") }
		}
	}
}
