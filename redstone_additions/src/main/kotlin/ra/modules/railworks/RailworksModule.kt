package ra.modules.railworks

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.raw
import ra.core.ref
import ra.modules.railworks.RouteRail.registerRouteRail
import ra.modules.railworks.carts.CartTracker
import ra.modules.railworks.carts.CartTracker.stampNow
import ra.modules.railworks.carts.RailBuilder
import ra.modules.railworks.carts.RailBuilder.registerRailBuilder
import ra.modules.railworks.carts.RamUpgrade
import ra.modules.railworks.carts.RamUpgrade.registerRamUpgrade

/**
 * Railworks (`ra_railworks`): blocks that work with minecarts and their tracks, and carts of our own.
 *
 * ADDING A BLOCK. One file per block in this package, with a `fun Ra.register<Block>()` called
 * from [register]. The block's `give`, `register_block` and `tick` go into the module's
 * `items/give_all`, `load` and `tick` below, its item into the bundle. Outside this package: a
 * row on the settings page (`ra/settings/pages/RailworksPage.kt`) and an entry in the advancement
 * tree (`railworksTree`, `ra/advancements/RailworksTree.kt`).
 *
 * CARTS (`carts/`). The [RamUpgrade] fits onto a minecart, the [RailBuilder] is one. Both are
 * followed by the [CartTracker], which makes a broken cart drop what it carried.
 */
object RailworksModule : GameplayModule {
	override val ns = Ns.RAILWORKS
	override val title = "Railworks"

	override fun Ra.register() {
		registerRouteRail()
		registerRamUpgrade()
		registerRailBuilder()
		CartTracker.register(this, RamUpgrade.item.stackSnbt(), RailBuilder.item.stackSnbt())
		wrenchEntityHooks += RamUpgrade.wrenchHook

		pack.define(ref(ns, "items/give_all")) {
			function(RouteRail.block.give)
			function(RamUpgrade.give)
			function(RailBuilder.give)
		}
		bundles += GiveBundle(
			"railworks", "Railworks Bundle", "Blocks and carts for your tracks", ns,
			listOf(RouteRail.item.stackNbt(), RamUpgrade.item.stackNbt(), RailBuilder.item.stackNbt()),
		)

		pack.define(load) {
			function(RouteRail.registerBlock)
			for (o in CartTracker.objectives + RailBuilder.objectives + RamUpgrade.USED) raw("scoreboard objectives add $o dummy")
			moduleLoaded("Railworks loaded!", "gray")
		}
		pack.define(tick!!) {
			function(RouteRail.block.tick)
			// Builders first (they move), then every tracked cart (its tracker follows; its upgrades
			// act), then the trackers whose cart is gone.
			stampNow()
			function(RailBuilder.pass)
			raw("execute as @e[type=${CartTracker.CARTS},tag=${CartTracker.TRACKED}] at @s run function ${CartTracker.cartTick.asId()}")
			function(CartTracker.lostPass)
		}
	}
}
