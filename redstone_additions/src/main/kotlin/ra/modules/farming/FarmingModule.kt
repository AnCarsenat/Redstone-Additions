package ra.modules.farming

import ra.core.GameplayModule
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.ref

/**
 * Farming (`ra_farming`): blocks that tend crops and animals.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/FarmingPage.kt`) and an
 * entry in the advancement tree (`farmingTree`, `ra/advancements/FarmingTree.kt`).
 */
object FarmingModule : GameplayModule {
	override val ns = Ns.FARMING
	override val title = "Farming"

	override fun Ra.register() {
		pack.define(ref(ns, "items/give_all")) {}

		pack.define(load) {
			moduleLoaded("Farming loaded!", "green")
		}
		pack.define(tick!!) {}
	}
}
