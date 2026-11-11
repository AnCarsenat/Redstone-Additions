package ra.settings.pages

import ra.core.Ns
import ra.modules.displays.DisplayStyle

val DisplaysPage = Page(
	"displays", "Displays", Ns.DISPLAYS,
	listOf(
		// Scale in percent; the rows go to the admin ceiling (ra.core.Admin).
		PropRow("gauge_bar", "scale", "Gauge Bar scale (%)", default = 100, min = DisplayStyle.MIN_SCALE, max = DisplayStyle.ADMIN_MAX_SCALE, step = 25),
		PropRow("item_hologram", "scale", "Item Hologram scale (%)", default = 100, min = DisplayStyle.MIN_SCALE, max = DisplayStyle.ADMIN_MAX_SCALE, step = 25),
		PropRow("text_screen", "scale", "Text Screen scale (%)", default = 100, min = DisplayStyle.MIN_SCALE, max = DisplayStyle.ADMIN_MAX_SCALE, step = 25),
	) + blocks(
		"gauge_bar" to "Gauge Bar",
		"item_hologram" to "Item Hologram",
		"text_screen" to "Text Screen",
	),
)
