package ra.settings.pages

import ra.core.Ns
import ra.modules.farming.Sprinkler

val FarmingPage = Page(
	"farming", "Farming", Ns.FARMING,
	listOf(
		// Ranges go to the admin ceiling: only an admin reaches these rows (ra.core.Admin).
		PropRow("sprinkler", "radius", "Sprinkler radius", default = Sprinkler.DEFAULT_RADIUS, min = 1, max = Sprinkler.ADMIN_MAX_RADIUS, step = 1),
	) + blocks(
		"breeder" to "Breeder",
		"sprinkler" to "Sprinkler",
	),
)
