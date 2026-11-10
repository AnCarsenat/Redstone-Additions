package ra.modules.railworks

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.ref
import ra.modules.railworks.RouteRail.registerRouteRail

/**
 * Railworks (`ra_railworks`): blocks that work with minecarts and their tracks.
 *
 * ADDING A BLOCK. One file per block in this package, with a `fun Ra.register<Block>()` called
 * from [register]. The block's `give`, `register_block` and `tick` go into the module's
 * `items/give_all`, `load` and `tick` below, its item into the bundle. Outside this package: a
 * row on the settings page (`ra/settings/pages/RailworksPage.kt`) and an entry in the advancement
 * tree (`railworksTree`, `ra/advancements/RailworksTree.kt`).
 */
object RailworksModule : GameplayModule {
	override val ns = Ns.RAILWORKS
	override val title = "Railworks"

	override fun Ra.register() {
		registerRouteRail()

		pack.define(ref(ns, "items/give_all")) {
			function(RouteRail.block.give)
		}
		bundles += GiveBundle(
			"railworks", "Railworks Bundle", "Blocks that send each cart where it belongs", ns,
			listOf(RouteRail.item.stackNbt()),
		)

		pack.define(load) {
			function(RouteRail.registerBlock)
			moduleLoaded("Railworks loaded!", "gray")
		}
		pack.define(tick!!) {
			function(RouteRail.block.tick)
		}
	}
}
