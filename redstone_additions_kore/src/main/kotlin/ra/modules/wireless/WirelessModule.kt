package ra.modules.wireless

import ra.core.GameplayModule
import ra.core.Ns
import ra.core.Ra

/** Wireless Redstone (`ra_wireless`). Not ported yet. */
object WirelessModule : GameplayModule {
	override val ns = Ns.WIRELESS
	override val title = "Wireless Redstone"

	override fun Ra.register() {
		// TODO(port)
	}
}
