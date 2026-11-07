package ra.lib

import io.github.ayfri.kore.commands.function
import ra.api.RaLib
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.lib.BlockTags.registerBlockTags
import ra.lib.input.Input
import ra.lib.input.Input.registerInput
import ra.lib.inventory.Inventory
import ra.lib.inventory.Inventory.registerInventory
import ra.lib.light.LightPredicates.registerLightPredicates
import ra.lib.orientation.Orientation
import ra.lib.orientation.Orientation.registerOrientation
import ra.lib.placement.Placement
import ra.lib.placement.Placement.registerPlacement
import ra.lib.redstone.Redstone
import ra.lib.redstone.Redstone.registerRedstone
import ra.lib.skin.Skin.registerSkin
import ra.lib.transport.Transport
import ra.lib.transport.Transport.registerTransport
import ra.lib.util.Util.registerUtil

/**
 * Shared library (`ra_lib`): placement, orientation, redstone reading, inventories, the transport
 * network engine, block skins, typed player input and small utilities. Every module builds on
 * it; its public entry points are the refs in [RaLib].
 */
object RaLibPart : PackPart {
	override fun Ra.register() {
		pack.registerBlockTags()
		registerLightPredicates()
		registerOrientation()
		registerPlacement()
		registerInventory()
		registerRedstone()
		registerSkin()
		registerInput()
		registerTransport()
		registerUtil()

		// Initialize every sub-system. Called on datapack load, from ra:load.
		pack.define(RaLib.init) {
			function(Orientation.init)
			function(Placement.init)
			function(Inventory.init)
			function(Redstone.init)
			function(Input.init)
			function(Transport.init)
		}
	}
}
