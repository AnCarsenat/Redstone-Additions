package ra.tools

import ra.core.GiveBundle
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.tools.clipboard.Clipboard.registerClipboard
import ra.tools.creativedatahandler.CreativeDataHandler.registerCreativeDataHandler
import ra.tools.datahandler.DataHandler.registerDataHandler
import ra.tools.datahandler.DataHandlerRegistry.registerDataHandlerRegistry
import ra.tools.goggles.GogglesTool.registerGoggles
import ra.tools.inputdatahandler.InputDataHandler.registerInputDataHandler
import ra.tools.multimeter.Multimeter.registerMultimeter
import ra.tools.readonly.Readonly.registerReadonly
import ra.tools.wrench.Wrench.registerWrench
import ra.tools.wrench.WrenchMenu.registerWrenchMenu
import ra.tools.wrench.WrenchMultiblock.registerWrenchMultiblock
import ra.tools.wrench.WrenchRegistry.registerToolsWrenchCycles
import ra.tools.wrench.WrenchRegistry.registerWrenchRegistry

/**
 * Tools (`ra:tools/...`): wrench, goggles, the three data handlers, clipboard, multimeter.
 *
 * Runs after every module, so the registries it builds shared functions from (goggles dispatch,
 * wrench cycles and toggles, read-only properties) are complete when it reads them.
 */
object ToolsPart : PackPart {
	override fun Ra.register() {
		// The tools' own registry rows go in before anything is built from the registry.
		registerToolsWrenchCycles()

		pack.registerToolResources()
		registerBlockName()
		registerGoggles()
		registerWrench()
		registerWrenchMenu()
		registerWrenchMultiblock()
		registerWrenchRegistry()
		registerReadonly()
		registerDataHandlerRegistry()
		registerDataHandler()
		registerCreativeDataHandler()
		registerInputDataHandler()
		registerClipboard()
		registerMultimeter()

		// ra:items/bundles/give_tools_bundle (built by the hub). The Input Data Handler is a
		// debugging tool and was never in the bundle.
		bundles += GiveBundle(
			id = "tools",
			title = "RA Tools Bundle",
			lore = "Wrench, data handlers, goggles",
			ns = Ns.RA,
			items = listOf(
				ToolItems.multimeter, ToolItems.clipboard, ToolItems.wrench,
				ToolItems.creativeDataHandler, ToolItems.dataHandler, ToolItems.goggles,
			).map { it.spec.stackNbt() },
		)
	}
}
