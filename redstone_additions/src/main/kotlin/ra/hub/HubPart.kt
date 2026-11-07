package ra.hub

import io.github.ayfri.kore.commands.function
import ra.api.Settings
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.hub.items.registerBundles
import ra.hub.placement.registerPlacement
import ra.hub.uninstall.registerUninstall

/**
 * Core orchestration in the `ra` namespace: `ra:load`, `ra:tick`, the placement router, the give
 * bundles and uninstall. Runs last, so every module and the tools have registered the handlers,
 * bundles and load/tick functions it builds these from.
 */
object HubPart : PackPart {
	override fun Ra.register() {
		registerLoad()
		registerTick()
		registerPlacement()
		registerBundles()
		pack.registerUninstall()

		// `/function ra:settings`: open the server settings, the short way in. ra_settings:admin/show
		// is the real entry point and does the work; this exists because that path is four segments
		// deep and an operator wanting the panel should not have to tab through the tree to reach
		// its front door. `ra:settings` is two tokens and sorts next to the other top-level functions.
		pack.define(ref(Ns.RA, "settings")) { function(Settings.Admin.show) }
	}
}
