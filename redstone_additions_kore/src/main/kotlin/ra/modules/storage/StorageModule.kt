package ra.modules.storage

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.infinite.loadMessage
import ra.modules.storage.Boxer.registerBoxer
import ra.modules.storage.StorageBox.registerStorageBox
import ra.modules.storage.Unboxer.registerUnboxer

/**
 * Storage (`ra_storage`): the Boxer packs a container into an Item Crate, the Unboxer empties
 * crates back out. Both move items with the Item Mover's plumbing (ra_interactive).
 */
object StorageModule : GameplayModule {
	override val ns = Ns.STORAGE
	override val title = "Storage"

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerBoxer()
		registerUnboxer()
		registerStorageBox()

		pack.define(load) {
			function(Boxer.registerBlock)
			function(Unboxer.registerBlock)
			loadMessage("Storage blocks loaded!", "aqua")
		}
		pack.define(tick!!) {
			function(Boxer.block.tick)
			function(Unboxer.block.tick)
		}

		// /function ra_storage:items/give_all
		pack.define(ref(ns, "items/give_all")) {
			function(Boxer.block.give)
			function(Unboxer.block.give)
			function(StorageBox.giveStorageBox)
		}

		// The bundle's crate carries no "(empty)" lore, unlike the one give_storage_box hands out.
		bundles += GiveBundle(
			"storage", "Storage Bundle", "Boxing and unboxing utilities", ns,
			listOf(Boxer.item.stackNbt(), Unboxer.item.stackNbt(), StorageBox.crate.without("lore").stackNbt()),
		)
	}
}
