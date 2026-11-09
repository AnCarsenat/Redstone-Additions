package ra.lib

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.tagged.BlockTagArgument
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Tags
import ra.core.Ns

/**
 * `#ra_lib:containers`, `#ra_lib:passable`, `#ra_lib:ray_passable` and `#ra_lib:unbreakable`, used
 * across modules. The
 * redstone source tags live with the readers, in [ra.lib.redstone.RedstoneTags].
 */
internal object BlockTags {
	val containers = BlockTagArgument("containers", Ns.LIB.id)

	/** Blocks the tools' look ray goes through: see [registerBlockTags]. */
	val rayPassable = BlockTagArgument("ray_passable", Ns.LIB.id)

	/** Shulker box colours, in the order the beet tag listed them. */
	private val shulkerColours = listOf(
		"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
		"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
	)

	fun DataPack.registerBlockTags() {
		val ns = Ns.LIB.id

		// Every block the inventory helpers treat as a container.
		blockTag("containers", ns) {
			for (b in listOf(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER, Blocks.SHULKER_BOX)) add(b)
			for (c in shulkerColours) add("minecraft:${c}_shulker_box")
		}

		// Blocks a moving thing may pass through or replace.
		blockTag("passable", ns) {
			for (b in listOf(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER, Blocks.LAVA)) add(b)
			for (t in listOf(Tags.Block.SIGNS, Tags.Block.WALL_SIGNS, Tags.Block.FLOWERS, Tags.Block.SAPLINGS)) add(t)
			for (b in listOf(
				Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.DEAD_BUSH, Blocks.SEAGRASS,
				Blocks.TALL_SEAGRASS, Blocks.VINE, Blocks.SNOW, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.TORCH, Blocks.WALL_TORCH,
				Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
			)) add(b)
			add(Tags.Block.RAILS)
		}

		// Blocks the tools' look ray (ra.tools.raycast) goes through on its way to a custom block:
		// everything replaceable, plus the small and non-colliding blocks a player aims over --
		// redstone components, rails, carpets, plants, signs, ladders, torches... Not in it: any
		// block that fills (most of) its cell, which is what a player sees under the crosshair.
		blockTag("ray_passable", ns) {
			for (t in listOf(
				Tags.Block.REPLACEABLE, Tags.Block.RAILS, Tags.Block.BUTTONS, Tags.Block.PRESSURE_PLATES,
				Tags.Block.WOOL_CARPETS, Tags.Block.SMALL_FLOWERS, Tags.Block.SAPLINGS, Tags.Block.ALL_SIGNS,
				Tags.Block.BANNERS, Tags.Block.CANDLES, Tags.Block.FIRE, Tags.Block.CROPS, Tags.Block.CORALS,
				Tags.Block.WALL_CORALS, Tags.Block.FLOWER_POTS, Tags.Block.CAVE_VINES, Tags.Block.SKULLS,
				Tags.Block.LIGHTNING_RODS, Tags.Block.CHAINS,
			)) add(t)
			for (b in listOf(
				Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR, Blocks.LEVER, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK,
				Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.REDSTONE_TORCH,
				Blocks.REDSTONE_WALL_TORCH, Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH, Blocks.END_ROD, Blocks.LADDER,
				Blocks.MOSS_CARPET, Blocks.PALE_MOSS_CARPET, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
				Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.SUGAR_CANE, Blocks.NETHER_WART,
				Blocks.SWEET_BERRY_BUSH, Blocks.COBWEB, Blocks.LILY_PAD, Blocks.SUNFLOWER, Blocks.LILAC, Blocks.PEONY,
				Blocks.ROSE_BUSH, Blocks.PITCHER_PLANT, Blocks.PINK_PETALS, Blocks.WILDFLOWERS, Blocks.LEAF_LITTER,
				Blocks.SPORE_BLOSSOM, Blocks.CACTUS_FLOWER, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
				Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS, Blocks.HANGING_ROOTS, Blocks.PALE_HANGING_MOSS,
				Blocks.FIREFLY_BUSH, Blocks.SMALL_DRIPLEAF, Blocks.SEA_PICKLE, Blocks.KELP, Blocks.KELP_PLANT,
				Blocks.SCULK_VEIN, Blocks.PLAYER_WALL_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.ZOMBIE_WALL_HEAD,
				Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.DRAGON_WALL_HEAD,
				Blocks.PIGLIN_WALL_HEAD,
			)) add(b)
		}

		// Blocks nothing in the pack may break or replace.
		blockTag("unbreakable", ns) {
			for (b in listOf(
				Blocks.BEDROCK, Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
				Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.MOVING_PISTON,
			)) add(b)
		}
	}
}
