package ra.tools.wrench

import io.github.ayfri.kore.commands.data
import net.benwoodworth.knbt.NbtByte
import ra.api.MultiblockLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.tools.executeReturnRun
import ra.tools.executeRun
import ra.tools.tool

/** Wrench RMB on multiblocks: assemble a structure on its base, or toggle an assembled one. */
object WrenchMultiblock {
	val checkBlastForge = tool("wrench/check_blast_forge")
	val handleMultiblockBase = tool("wrench/handle_multiblock_base")
	val toggleMultiblock = tool("wrench/toggle_multiblock")
	val toggleMultiblockEntity = tool("wrench/toggle_multiblock_entity")
	val tryMultiblock = tool("wrench/try_multiblock")

	/**
	 * One base block per tier. [named] is a structure with its own hand-written validator, tried by
	 * name before the registry; [hints] are the structure descriptions shown when nothing matches.
	 */
	private class Tier(
		val name: String,
		val block: String,
		val named: String? = null,
		val hints: List<String> = emptyList(),
	) {
		val tryAssemble = tool("wrench/try_assemble_$name")
	}

	private val tiers = listOf(
		Tier(
			"copper", "waxed_copper_block", named = "blast_forge", hints = listOf(
				"Blast Forge: 3x3x3 nether bricks + blast furnace + 2 barrels (input) + 1 barrel (output)",
				"Rock Metallic Drill: barrel beside the base, iron bars under the barrel, smooth stone under the base",
			)
		),
		Tier("iron", "iron_block"),
		Tier("gold", "gold_block"),
		Tier("diamond", "diamond_block"),
		Tier(
			"netherite", "netherite_block", named = "upgrade_platform",
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
			executeReturnRun("align xyz positioned ~0.5 ~ ~0.5 $ASSEMBLED", toggleMultiblock)
			raw("data modify storage ra:multiblock type set value \"blast_forge\"")
			// Player rotation, for a directional multiblock.
			raw("execute store result storage ra:multiblock rotation int 1 positioned as @a[tag=ra.wrench_active,distance=..8,limit=1,sort=nearest] run data get entity @s Rotation[0]")
			executeRun("store result score #mb_assembled ${Obj.TEMP}", MultiblockLib.tryAssemble)
			raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"Invalid Blast Forge structure. Need: 3x3 brick wall + blast furnace behind.",color:"red"}]""")
		}

		// As the multiblock base marker, at its position.
		pack.define(handleMultiblockBase) {
			// align xyz normalizes the position, distance=..0.5 matches only THIS block's marker.
			executeReturnRun("at @s align xyz positioned ~0.5 ~ ~0.5 $ASSEMBLED", toggleMultiblock)
			// Not assembled yet - try to assemble based on tier.
			for (t in tiers) executeRun("if entity @s[tag=ra.custom_block.multiblock_base.${t.name}]", t.tryAssemble)
			// Legacy compatibility: older bases only had tier in NBT properties.
			for (t in tiers) executeRun(
				"unless entity @s[tag=ra.custom_block.multiblock_base.${t.name}] if data entity @s data.properties{tier:\"${t.name}\"}",
				t.tryAssemble,
			)
		}

		// At the multiblock position (block-aligned centre).
		pack.define(toggleMultiblock) {
			executeRun("as @e[type=marker,tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest]", toggleMultiblockEntity)
		}
		pack.define(toggleMultiblockEntity) {
			raw("execute if data entity @s data.properties{enabled:1b} run data modify entity @s data.properties.enabled set value 0b")
			raw("execute if data entity @s data.properties{enabled:0b} run data modify entity @s data.properties.enabled set value 1b")
			raw("""execute if data entity @s data.properties{enabled:1b} at @s run tellraw @a[distance=..16] [{text:"[Wrench] ",color:"gold"},{text:"Multiblock ",color:"gray"},{text:"enabled",color:"green"}]""")
			raw("""execute if data entity @s data.properties{enabled:0b} at @s run tellraw @a[distance=..16] [{text:"[Wrench] ",color:"gold"},{text:"Multiblock ",color:"gray"},{text:"disabled",color:"red"}]""")
			raw("playsound minecraft:block.lever.click block @a[distance=..8,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1")
		}

		// At the multiblock base position.
		for (t in tiers) pack.define(t.tryAssemble) {
			if (t.named != null) {
				// A structure with its own hand-written validator is tried by name first...
				raw("data modify storage ra:multiblock type set value \"${t.named}\"")
				executeRun("store result score #mb_assembled ${Obj.TEMP}", MultiblockLib.tryAssemble)
				// ...then anything registered for this tier. Adding a structure means registering it
				// in ra_multiblock:register_types -- this file does not change.
				executeRun(
					"if score #mb_assembled ra.temp matches 0 store result score #mb_assembled ra.temp",
					MultiblockLib.tryTier, "{tier:\"${t.name}\"}",
				)
				raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No ${t.name}-tier multiblock matches the structure here.",color:"red"}]""")
				for (hint in t.hints)
					raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"",color:"gray"},{text:"$hint",color:"gray"}]""")
				raw("execute if score #mb_assembled ${Obj.TEMP} matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.5")
			} else {
				// No hand-written multiblock exists for this tier; ask the registry.
				executeRun("store result score #mb_assembled ${Obj.TEMP}", MultiblockLib.tryTier, "{tier:\"${t.name}\"}")
				raw("""execute if score #mb_assembled ${Obj.TEMP} matches 0 run tellraw @a[distance=..8] [{text:"[Wrench] ",color:"gold"},{text:"No ${t.name}-tier multiblock matches the structure here.",color:"gray"}]""")
				raw("execute if score #mb_assembled ${Obj.TEMP} matches 0 run playsound minecraft:block.note_block.bass block @a[distance=..8] ~ ~ ~ 0.5 0.8")
			}
		}

		// No custom block found by the wrench: as the player, at the looked-at position.
		// Fallback path: if the marker raycast misses, allow direct block-based interaction.
		pack.define(tryMultiblock) {
			executeReturnRun(ASSEMBLED, toggleMultiblock)
			for (t in tiers) executeReturnRun("if block ~ ~ ~ minecraft:${t.block}", t.tryAssemble)
		}
	}
}
