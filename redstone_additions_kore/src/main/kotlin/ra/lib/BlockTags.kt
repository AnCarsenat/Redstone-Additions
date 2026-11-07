package ra.lib

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.tagged.BlockTagArgument
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Tags
import ra.core.Ns

/**
 * `#ra_lib:containers`, `#ra_lib:passable` and `#ra_lib:unbreakable`, used across modules. The
 * redstone source tags live with the readers, in [ra.lib.redstone.RedstoneTags].
 */
internal object BlockTags {
	val containers = BlockTagArgument("containers", Ns.LIB.id)

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

		// Blocks nothing in the pack may break or replace.
		blockTag("unbreakable", ns) {
			for (b in listOf(
				Blocks.BEDROCK, Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
				Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.MOVING_PISTON,
			)) add(b)
		}
	}
}
