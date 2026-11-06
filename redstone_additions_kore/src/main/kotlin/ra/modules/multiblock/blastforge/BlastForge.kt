package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrInt
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.TagRoute
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref
import ra.modules.multiblock.blastforge.BlastForgeData.ceiling
import ra.modules.multiblock.blastforge.BlastForgeData.corners
import ra.modules.multiblock.blastforge.BlastForgeData.facings
import ra.modules.multiblock.blastforge.BlastForgeData.floor
import ra.modules.multiblock.blastforge.BlastForgeData.path
import ra.modules.multiblock.blastforge.BlastForgeTick.registerBlastForgeTick
import ra.modules.multiblock.goggles.ShowIoLabel

/**
 * Blast Forge (`ra_multiblock:blast_forge/...`): a 3x3x3 nether brick shell around a blast furnace,
 * assembled on a copper base, that smelts at double yield and recycles gear, fed by heat.
 *
 * It uses its own hand-written validator hooked into the #ra_lib_multiblock tags rather than the
 * newer ra_lib_multiblock:register path. It works, so it was left as it is.
 */
object BlastForge {
	private fun fn(path: String) = ref(Ns.MULTIBLOCK, "blast_forge/$path")

	const val TAG = "ra.multiblock.blast_forge"

	val checkBlocks = fn("check_blocks")
	val checkStructure = fn("check_structure")
	val validate = fn("validate")
	val setupType = fn("setup_type")
	val onBreak = fn("on_break")
	val tickDispatch = fn("tick_dispatch")
	val tick = fn("tick")
	val storeAssemblyData = fn("store_assembly_data")
	val goggles = fn("goggles")

	/** `#ra_multiblock:blast_forge_bricks`: any nether brick variant counts. */
	const val BRICKS = "#ra_multiblock:blast_forge_bricks"

	fun Ra.registerBlastForge() {
		pack.blockTag("blast_forge_bricks", Ns.MULTIBLOCK.id) {
			add(Blocks.NETHER_BRICKS)
			add(Blocks.RED_NETHER_BRICKS)
			add(Blocks.CRACKED_NETHER_BRICKS)
			add(Blocks.CHISELED_NETHER_BRICKS)
		}

		pack.define(checkBlocks) { checkBlocks() }
		pack.define(checkStructure) { checkStructure() }
		pack.define(validate) { validate() }
		pack.define(storeAssemblyData) {
			raw("\$data modify storage ra:multiblock facing set value \"$(dir)\"")
			raw("\$data modify storage ra:multiblock inputs set from storage ra:multiblock bf_dir.$(dir).io_inputs")
			raw("\$data modify storage ra:multiblock outputs set from storage ra:multiblock bf_dir.$(dir).io_outputs")
			raw("\$data modify storage ra:multiblock controls set from storage ra:multiblock bf_dir.$(dir).io_controls")
		}
		// Adds the type tag to a newly created multiblock marker. As the new marker.
		pack.define(setupType) {
			raw("""execute if data entity @s data{type:"blast_forge"} run tag @s add $TAG""")
		}
		pack.define(onBreak) {
			raw("execute unless entity @s[tag=$TAG] run return 0")
			// No cleanup needed: items stay in the barrels, where players can reach them.
		}
		pack.define(tickDispatch) {
			execute {
				asTarget(io.github.ayfri.kore.arguments.types.literals.allEntities {
					type = io.github.ayfri.kore.generated.EntityTypes.MARKER
					tag = TAG
				})
				at(self())
				run { function(tick) }
			}
		}
		pack.define(BlastForge.goggles) { goggles() }

		registerBlastForgeTick()
		BlastForgeDebug.register(pack)

		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.VALIDATE, validate)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.SETUP_TYPE, setupType)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.TICK, tickDispatch)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.CHECK_STRUCTURE, checkStructure)
		pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.ON_BREAK, onBreak)
		multiblockGoggles += TagRoute(TAG, BlastForge.goggles)
	}

	/**
	 * The block pattern, the single source of truth for both assembly and the periodic check.
	 * Macro: `bf_dir.<facing>`. At the base. Returns 1 if every block is right, 0 otherwise, and
	 * has no side effects.
	 */
	private fun Function.checkBlocks() {
		// Functional blocks.
		raw("\$execute unless block $(furnace) minecraft:blast_furnace run return 0")
		for (k in listOf("in1", "in2", "out1")) raw("\$execute unless block $($k) minecraft:barrel run return 0")
		// Middle layer (Y=0) corner bricks, then the 9-block floor (Y-1) and ceiling (Y+1).
		for (k in corners + floor + ceiling) raw("\$execute unless block $($k) $BRICKS run return 0")
		returnValue(1)
	}

	/**
	 * Periodic integrity check. As the multiblock marker, at the base. Sets @s ra.multiblock to 1
	 * if the structure is still valid.
	 */
	private fun Function.checkStructure() {
		raw("execute unless entity @s[tag=$TAG] run return 0")

		raw("""execute if entity @a[tag=ra.debug,limit=1] unless data entity @s data.facing run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"ERROR: No facing data on marker!",color:"red"},{text:" data="},{nbt:"data",entity:"@s",color:"yellow"}]""")

		// store result sets the score straight from check_blocks' return value.
		for (f in facings) execute {
			ifCondition { data(self(), """data{facing:"$f"}""") }
			storeResult { score(self(), Obj.MULTIBLOCK) }
			run { function(checkBlocks, Storage.MULTIBLOCK, path(f)) }
		}

		// Score still 0 with a facing set: a block check failed.
		raw("""execute if entity @a[tag=ra.debug,limit=1] if score @s ${Obj.MULTIBLOCK} matches 0 if data entity @s data.facing run tellraw @a[tag=ra.debug] [{text:"[MB Debug] ",color:"gold"},{text:"Structure check FAILED for facing: ",color:"red"},{nbt:"data.facing",entity:"@s",color:"yellow"}]""")
	}

	/**
	 * Assembly validation: tries the four facings. At the base. Sets #mb_result ra.multiblock to
	 * 1 if valid in any direction, and stores the facing + IO for the marker.
	 */
	private fun Function.validate() {
		raw("""execute unless data storage ra:multiblock {type:"blast_forge"} run return 0""")
		val result = literal("#mb_result")
		for (f in facings) {
			execute {
				storeResult { score(result, Obj.MULTIBLOCK) }
				run { function(checkBlocks, Storage.MULTIBLOCK, path(f)) }
			}
			execute {
				ifCondition { score(result, Obj.MULTIBLOCK, rangeOrInt(1)) }
				run { function(storeAssemblyData, nbt { this["dir"] = f }) }
			}
			execute {
				ifCondition { score(result, Obj.MULTIBLOCK, rangeOrInt(1)) }
				run { returnValue(1) }
			}
		}
	}

	/** As the marker, at the base: name, status, and I/O labels above the structure. */
	private fun Function.goggles() {
		// Name billboard, above the ceiling.
		raw("""summon text_display ~ ~2.5 ~ {Tags:["ra","ra.billboard","ra.display"],billboard:"center",text:{text:"Blast Forge",color:"green",bold:true},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}""")

		function(Goggles.Status.readStatusMultiblock)

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
