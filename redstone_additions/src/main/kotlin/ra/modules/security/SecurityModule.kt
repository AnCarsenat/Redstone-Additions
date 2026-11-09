package ra.modules.security

import ra.core.GameplayModule
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.ref

/**
 * Security (`ra_security`): blocks that keep a build closed to whoever lacks the code.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/SecurityPage.kt`) and an
 * entry in the advancement tree (`securityTree`, `ra/advancements/SecurityTree.kt`).
 */
object SecurityModule : GameplayModule {
	override val ns = Ns.SECURITY
	override val title = "Security"

	override fun Ra.register() {
		pack.define(ref(ns, "items/give_all")) {}

		pack.define(load) {
			moduleLoaded("Security loaded!", "dark_red")
		}
		pack.define(tick!!) {}
	}
}
