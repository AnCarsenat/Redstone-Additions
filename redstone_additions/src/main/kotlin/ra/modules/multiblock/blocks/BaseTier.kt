package ra.modules.multiblock.blocks

import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.Tag

/**
 * One tier of the Multiblock Base: a single block type with five tiers, which is why its give,
 * placement and break are macro functions fed with `storage ra:multiblock tier_data.<tier>`.
 *
 * Every value here ends up in that storage compound, in the base's item and in its recipe, so it
 * is written once.
 */
data class BaseTier(
	val tier: String,
	val tierName: String,
	val level: Int,
	/** The vanilla block placed (and the egg's item_model). */
	val block: BlockArgument,
	val breakSound: String,
	val breakVolume: String,
	val breakPitch: String,
	val placeSound: String,
	val placeVolume: String,
	val placePitch: String,
	val displayBlock: String,
	/** Recipe: a 3x3 ring of [ring] around [core], keyed [ringKey] / [coreKey]. */
	val ring: ItemArgument,
	val core: ItemArgument,
	val ringKey: String,
	val coreKey: String,
	/** The item that unlocks the recipe. */
	val unlockedBy: ItemArgument,
) {
	val blockId get() = block.asId()
	val name get() = "$tierName Multiblock Base"
	val loreText get() = "Tier $level Multiblock Core"

	/** `multiblock_base.<tier>`: marker tag suffix (`ra.custom_block.multiblock_base.<tier>`). */
	val blockTag get() = "multiblock_base.$tier"
	val markerTag get() = Tag.customBlock(blockTag)

	/** `blocks/<tier>_base/handle_placement`. */
	val dir get() = "${tier}_base"

	/** `tier_data.<tier>`, the macro arguments of base_give / base_handle_placement / base_on_break. */
	val path get() = "tier_data.$tier"

	/** The compound stored at [path]. Order and string-typed volumes as in the beet version. */
	fun tierDataSnbt() = "{tier:\"$tier\",tier_name:\"$tierName\",tier_level:$level,block_id:\"$blockId\",block_model:\"$blockId\"," +
		"block_tag:\"$blockTag\",lore_text:\"$loreText\",break_sound:\"$breakSound\",break_volume:\"$breakVolume\",break_pitch:\"$breakPitch\"," +
		"place_sound:\"$placeSound\",place_volume:\"$placeVolume\",place_pitch:\"$placePitch\",display_block:\"$displayBlock\",display_scale:0.65}"

	fun loreNbt() = NbtList(listOf(nbt { this["text"] = loreText; this["color"] = "gray"; this["italic"] = false }))

	fun customDataNbt() = nbt { this["ra"] = nbt { this["multiblock_base"] = tier } }

	fun entityDataNbt() = nbt {
		this["id"] = "minecraft:bat"
		this["Tags"] = NbtList(listOf(Tag.RA, Tag.SPAWNED, Tag.place("multiblock_base"), Tag.place(blockTag)).map(::NbtString))
		this["Silent"] = true
		this["NoAI"] = true
		this["Invulnerable"] = true
	}

	/** The item as an SNBT stack, for the give bundle. */
	fun itemStackNbt(): NbtCompound = nbt {
		this["id"] = "minecraft:bat_spawn_egg"
		this["count"] = 1
		this["components"] = nbt {
			this["minecraft:item_model"] = blockId
			this["minecraft:item_name"] = name
			this["minecraft:lore"] = loreNbt()
			this["minecraft:custom_data"] = customDataNbt()
			this["minecraft:entity_data"] = entityDataNbt()
		}
	}

	companion object {
		val all = listOf(
			BaseTier(
				"copper", "Copper", 1, Blocks.WAXED_COPPER_BLOCK,
				"minecraft:block.copper.break", "1", "0.8", "minecraft:block.copper.place", "1", "0.8", "minecraft:blast_furnace",
				ring = Items.COPPER_BLOCK, core = Items.REDSTONE, ringKey = "C", coreKey = "R", unlockedBy = Items.COPPER_INGOT,
			),
			BaseTier(
				"iron", "Iron", 2, Blocks.IRON_BLOCK,
				"minecraft:block.metal.break", "1", "1", "minecraft:block.anvil.land", "0.5", "1.2", "minecraft:smithing_table",
				ring = Items.IRON_INGOT, core = Items.REDSTONE, ringKey = "I", coreKey = "R", unlockedBy = Items.IRON_INGOT,
			),
			BaseTier(
				"gold", "Gold", 3, Blocks.GOLD_BLOCK,
				"minecraft:block.metal.break", "1", "0.8", "minecraft:entity.experience_orb.pickup", "0.5", "0.8", "minecraft:gold_block",
				ring = Items.GOLD_BLOCK, core = Items.REDSTONE, ringKey = "G", coreKey = "R", unlockedBy = Items.GOLD_INGOT,
			),
			BaseTier(
				"diamond", "Diamond", 4, Blocks.DIAMOND_BLOCK,
				"minecraft:block.amethyst_block.break", "1", "1", "minecraft:block.amethyst_block.chime", "1", "1.2", "minecraft:diamond_block",
				ring = Items.DIAMOND_BLOCK, core = Items.REDSTONE, ringKey = "D", coreKey = "R", unlockedBy = Items.DIAMOND,
			),
			BaseTier(
				"netherite", "Netherite", 5, Blocks.NETHERITE_BLOCK,
				"minecraft:block.netherite_block.break", "0.8", "0.6", "minecraft:block.respawn_anchor.charge", "0.8", "0.6", "minecraft:lodestone",
				ring = Items.IRON_BLOCK, core = Items.NETHERITE_INGOT, ringKey = "I", coreKey = "N", unlockedBy = Items.NETHERITE_INGOT,
			),
		)
	}
}
