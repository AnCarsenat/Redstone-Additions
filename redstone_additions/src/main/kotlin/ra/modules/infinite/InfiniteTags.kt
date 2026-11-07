package ra.modules.infinite

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.tagged.BlockTagArgument
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import ra.core.Ns

/** Block tags of ra_infinite. */
object InfiniteTags {
	/** What a generator may grow into: air and the plants a player would not miss. */
	val growable = BlockTagArgument("growable", Ns.INFINITE.id)

	/** What a flower survives on: the vanilla dirt family plus farmland. */
	val flowerGround = BlockTagArgument("flower_ground", Ns.INFINITE.id)

	fun DataPack.registerInfiniteTags() {
		blockTag("growable", Ns.INFINITE.id) {
			for (b in listOf(
				Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
				Blocks.FERN, Blocks.LARGE_FERN, Blocks.DEAD_BUSH, Blocks.SNOW,
			)) add(b)
		}
		blockTag("flower_ground", Ns.INFINITE.id) {
			for (b in listOf(
				Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PODZOL,
				Blocks.MYCELIUM, Blocks.MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK, Blocks.MUD,
				Blocks.MUDDY_MANGROVE_ROOTS, Blocks.FARMLAND,
			)) add(b)
		}
	}
}

/** [InfiniteTags.growable] as command text (`#ra_infinite:growable`), for the raw block tests. */
val GROWABLE = InfiniteTags.growable.asId()

/** [InfiniteTags.flowerGround] as command text (`#ra_infinite:flower_ground`). */
val FLOWER_GROUND = InfiniteTags.flowerGround.asId()
