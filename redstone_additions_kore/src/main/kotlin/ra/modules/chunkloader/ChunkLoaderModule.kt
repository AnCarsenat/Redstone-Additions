package ra.modules.chunkloader

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.chunkloader.ChunkLoader.registerChunkLoader
import ra.modules.infinite.loadMessage

/** Chunk Loader (`ra_chunk_loader`): one block, the Chunk Loader. */
object ChunkLoaderModule : GameplayModule {
	override val ns = Ns.CHUNK_LOADER
	override val title = "Chunk Loader"

	override fun Ra.register() {
		registerChunkLoader()

		pack.define(ref(ns, "items/give_all")) { function(ChunkLoader.block.give) }

		bundles += GiveBundle(
			id = "chunk_loader",
			title = "Chunk Loader Bundle",
			lore = "Chunk control utility",
			ns = ns,
			items = listOf(ChunkLoader.item.stackNbt()),
		)

		pack.define(load) {
			function(ChunkLoader.registerBlock)
			loadMessage("Chunk Loader loaded!", "aqua")
		}
		pack.define(tick!!) { function(ChunkLoader.block.tick) }
	}
}
