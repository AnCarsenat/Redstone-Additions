package ra.tools.wrench

import io.github.ayfri.kore.commands.data
import net.benwoodworth.knbt.NbtByte
import ra.api.MultiblockLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.tools.tool

/**
 * Wrench RMB on multiblocks: assemble a structure on its base. On one that is already assembled it
 * only says so -- until v6.0.1 it flipped a hidden `enabled` property there, which could leave a
 * structure switched off with nothing on it to show why. Its redstone control is the off switch.
 */
object WrenchMultiblock {
	val checkBlastForge = tool("wrench/check_blast_forge")
	val handleMultiblockBase = tool("wrench/handle_multiblock_base")
	val alreadyAssembled = tool("wrench/already_assembled")
	val tryMultiblock = tool("wrench/try_multiblock")

	/**
	 * One base block per tier. Every structure is assembled through `try_tier`, which tries the
	 * tier's compiled specs and then its storage-registered types; [hints] are the structure
	 * descriptions shown when nothing matches.
	 */
	private class Tier(
		val name: String,
		val block: String,
		val hints: List<String> = emptyList(),
	) {
		val tryAssemble = tool("wrench/try_assemble_$name")
	}

	private val tiers = listOf(
		Tier(
			"copper", "waxed_copper_block", hints = listOf(
				"Blast Forge: 3x3x3 nether bricks + blast furnace + 2 barrels (input) + 1 barrel (output)",
				"Rock Metallic Drill: barrel beside the base, iron bars under the barrel, smooth stone under the base",
			)
		),
		Tier("iron", "iron_block"),
		Tier("gold", "gold_block"),
		Tier("diamond", "diamond_block"),
		Tier(
			"netherite", "netherite_block",
			hints = listOf("Upgrade Platform: 3x3 smooth stone platform with netherite base in center"),
		),
	)

	/** At the block-aligned centre of a position: is an assembled multiblock's marker right here? */
	private const val ASSEMBLED = "if entity @e[type=marker,tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest]"

	fun Ra.registerWrenchMultiblock() {
		// Check if looking at a valid blast forge structure. At the hopper position.
		pack.define(checkBlastForge) {
			data(Storage.TEMP) { modify("wrench_found", NbtByte(1)) }
			// Already a multiblock? Aligned position + tight distance.
			executeReturnRun("align xyz positioned ~0.5 ~ ~0.5 $ASSEMBLED", alreadyAssembled)
			raw("data modify storage ra:multiblock type set value \"blast_forge\"")
			// Player rotation, for a directional multiblock.
			raw("execute store result storage ra:multiblock rotation int 1 positioned as @a[tag=ra.wrench_active,distance=..8,limit=1,sort=nearest] run data get entity @s Rotation[0]")
			executeRun("store result score #mb_assembled ${Obj.TEMP}", MultiblockLib.tryAssemble)
			raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"Invalid Blast Forge structure. Need: 3x3 brick wall + blast furnace behind.",color:"red"}]""")
		}

		// As the multiblock base marker, at its position.
		pack.define(handleMultiblockBase) {
			// align xyz normalizes the position, distance=..0.5 matches only THIS block's marker.
			executeReturnRun("at @s align xyz positioned ~0.5 ~ ~0.5 $ASSEMBLED", alreadyAssembled)
			// Not assembled yet - try to assemble based on tier.
			for (t in tiers) executeRun("if entity @s[tag=ra.custom_block.multiblock_base.${t.name}]", t.tryAssemble)
			// Legacy compatibility: older bases only had tier in NBT properties.
			for (t in tiers) executeRun(
				"unless entity @s[tag=ra.custom_block.multiblock_base.${t.name}] if data entity @s data.properties{tier:\"${t.name}\"}",
				t.tryAssemble,
			)
		}

		// At the multiblock position (block-aligned centre). Returns 1 so the callers stop here
		// instead of trying to assemble the structure a second time.
		pack.define(alreadyAssembled) {
			raw("""tellraw @a[tag=ra.wrench_active,distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"This multiblock is already assembled.",color:"gray"}]""")
			raw("return 1")
		}

		// At the multiblock base position.
		for (t in tiers) pack.define(t.tryAssemble) {
			// Anything registered for this tier. Adding a structure means registering its spec
			// (Ra.multiblock) -- this file does not change.
			executeRun("store result score #mb_assembled ${Obj.TEMP}", MultiblockLib.tryTier, "{tier:\"${t.name}\"}")
			if (t.hints.isNotEmpty()) {
				raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No ${t.name}-tier multiblock matches the structure here.",color:"red"}]""")
				for (hint in t.hints)
					raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"$hint",color:"gray"}]""")
				raw("execute if score #mb_assembled ${Obj.TEMP} matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5")
			} else {
				// No structure of this tier ships with the pack: a quieter message.
				raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No ${t.name}-tier multiblock matches the structure here.",color:"gray"}]""")
				raw("execute if score #mb_assembled ${Obj.TEMP} matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8")
			}
		}

		// No custom block found by the wrench: as the player, at the looked-at position.
		// Fallback path: if the marker raycast misses, allow direct block-based interaction.
		pack.define(tryMultiblock) {
			executeReturnRun(ASSEMBLED, alreadyAssembled)
			for (t in tiers) executeReturnRun("if block ~ ~ ~ minecraft:${t.block}", t.tryAssemble)
		}
	}
}
