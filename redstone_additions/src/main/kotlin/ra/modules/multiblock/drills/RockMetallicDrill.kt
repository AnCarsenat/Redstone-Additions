package ra.modules.multiblock.drills

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.MultiblockLib
import ra.api.multiblock
import ra.api.multiblockSpec
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.forEachMarker

/**
 * Rock Metallic Drill, copper tier: a compiled multiblock ([SPEC], registered with
 * [ra.api.multiblock]). ra_lib_multiblock generates its predicates, validator, checker and tagger.
 *
 *   Side view, looking east, with the drill facing north (-Z):
 *
 *     B M     B = barrel   (input and output)   M = copper base
 *     C S     C = iron bars (drill shaft)       S = smooth stone
 *
 * A redstone block on the control side switches the drill off.
 */
object RockMetallicDrill {
	const val ID = "rock_metallic_drill"

	val tick = ref(Ns.MULTIBLOCK, "drills/rock_metallic/tick")
	val tickDispatch = ref(Ns.MULTIBLOCK, "drills/rock_metallic/tick_dispatch")

	/**
	 * Offsets are written once for a NORTH-facing structure, relative to the base; the library
	 * derives south/east/west when the pack is generated, so there is no second (or third, or
	 * fourth) coordinate table to keep in step.
	 */
	val SPEC = multiblockSpec(ID, "Rock Metallic Drill", "copper") {
		hint = "Need: barrel in front of the base, iron bars under the barrel, smooth stone under the base"
		block(0, 0, 0, "minecraft:waxed_copper_block")
		block(0, 0, -1, "minecraft:barrel")
		block(0, -1, -1, "minecraft:iron_bars")
		block(0, -1, 0, "minecraft:smooth_stone")
		input("input_1", 0, 0, -1)
		output("output_1", 0, 0, -1)
		control("redstone_in", 0, 0, 1)
	}

	fun Ra.registerRockMetallicDrill() {
		multiblock(SPEC)
		// The ra.multiblock.<id> tag comes from the library's generated spec/setup_type.
		pack.define(tickDispatch) { forEachMarker(SPEC.tag, tick) }
		pack.define(tick) { tick() }
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
	}

	/**
	 * As the multiblock marker, at the base. There is no per-facing copy of this logic: the IO
	 * helpers resolve "output_1" and "redstone_in" from the marker's own IO maps, which the
	 * library already rotated to the assembled facing.
	 */
	private fun Function.tick() {
		// A redstone block on the control side halts the drill.
		execute {
			storeResult { score(literal("#mb_ctrl"), Obj.MULTIBLOCK) }
			run { function(MultiblockLib.Io.isBlock, nbt { this["name"] = "redstone_in"; this["block"] = "minecraft:redstone_block" }) }
		}
		raw("execute if score #mb_ctrl ${Obj.MULTIBLOCK} matches 1 run return 0")

		// One rock every 40 ticks. The original produced one every single tick.
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute unless score @s ${Obj.COOLDOWN} matches 40.. run return 0")
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		raw("particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force ${particleViewersSel()}")
		raw("playsound minecraft:block.stone.break block ${soundListenersSel(12)} ~ ~ ~ 0.4 0.7")

		// Build the item first, then let the library position the insert. Passing an item's
		// components through macro arguments would mean quoting them twice.
		raw("""data modify storage ra:multiblock io_item set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"minecraft:dead_brain_coral_block","minecraft:rarity":"common"}}""")
		// io/insert_here is passed by id, as io/at's `run` argument.
		function(MultiblockLib.Io.at, nbt { this["name"] = "output_1"; this["run"] = MultiblockLib.Io.insertHere.asId() })
	}
}
