package ra.settings.pages

import ra.core.Ns

val WirelessPage = Page(
	"wireless", "Wireless Redstone", Ns.WIRELESS,
	blocks("emitter" to "Emitter", "receiver" to "Receiver"),
)
