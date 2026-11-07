package ra.modules.multiblock.drills

import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref

/**
 * Rock Metallic Drill, copper tier: the first multiblock on the structure-driven
 * ra_lib_multiblock:register path. Its spec is registered by ra_multiblock:register_types.
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

	/** `ra_lib_multiblock:io/insert_here`, passed by id as io/at's `run` argument. */
	private val insertHere = ref(Ns.LIB_MULTIBLOCK, "io/insert_here")

	/**
	 * The spec handed to ra_lib_multiblock:register. Offsets are written once for a NORTH-facing
	 * structure, relative to the base; ra_lib_multiblock:build derives south/east/west, so there
	 * is no second (or third, or fourth) coordinate table to keep in step.
	 */
	const val SPEC = """{id:"rock_metallic_drill",name:"Rock Metallic Drill",tier:"copper",hint:"Need: barrel in front of the base, iron bars under the barrel, smooth stone under the base",blocks:[{x:0,y:0,z:0,match:"minecraft:waxed_copper_block"},{x:0,y:0,z:-1,match:"minecraft:barrel"},{x:0,y:-1,z:-1,match:"minecraft:iron_bars"},{x:0,y:-1,z:0,match:"minecraft:smooth_stone"}],inputs:[{name:"input_1",x:0,y:0,z:-1}],outputs:[{name:"output_1",x:0,y:0,z:-1}],controls:[{name:"redstone_in",x:0,y:0,z:1}]}"""

	fun Ra.registerRockMetallicDrill() {
		// The tag comes from ra_lib_multiblock:generic/setup_type, derived from the registered
		// id: ra.multiblock.<id>.
		pack.define(tickDispatch) {
			execute {
				asTarget(allEntities { type = EntityTypes.MARKER; tag = Tag.multiblock(ID) })
				at(self())
				run { function(tick) }
			}
		}
		pack.define(tick) { tick() }
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
	}

	/**
	 * As the multiblock marker, at the base. There is no per-facing copy of this logic: the IO
	 * helpers resolve "output_1" and "redstone_in" from the marker's own IO maps, which the
	 * library already rotated to the assembled facing.
	 */
	private fun Function.tick() {
		raw("execute unless data entity @s data.properties{enabled:1b} run return 0")

		// A redstone block on the control side halts the drill.
		execute {
			storeResult { score(io.github.ayfri.kore.arguments.types.literals.literal("#mb_ctrl"), Obj.MULTIBLOCK) }
			run { function(MultiblockLib.Io.isBlock, nbt { this["name"] = "redstone_in"; this["block"] = "minecraft:redstone_block" }) }
		}
		raw("execute if score #mb_ctrl ${Obj.MULTIBLOCK} matches 1 run return 0")

		// One rock every 40 ticks. The original produced one every single tick.
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute unless score @s ${Obj.COOLDOWN} matches 40.. run return 0")
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		raw("particle minecraft:smoke ~ ~1 ~ 0.5 0.2 0.5 0.1 5 force @a[scores={ra.u.par=1..}]")
		raw("playsound minecraft:block.stone.break block @a[distance=..12,scores={ra.u.snd=1..}] ~ ~ ~ 0.4 0.7")

		// Build the item first, then let the library position the insert. Passing an item's
		// components through macro arguments would mean quoting them twice.
		raw("""data modify storage ra:multiblock io_item set value {id:"minecraft:command_block",count:1,components:{"minecraft:item_name":"Rock","minecraft:item_model":"dead_brain_coral_block","minecraft:rarity":"common"}}""")
		function(MultiblockLib.Io.at, nbt { this["name"] = "output_1"; this["run"] = insertHere.asId() })
	}
}
