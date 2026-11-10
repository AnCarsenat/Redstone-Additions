package ra.settings.pages

import ra.core.Ns

val AutomationPage = Page(
	"automation", "Automation", Ns.AUTOMATION,
	listOf(
		PropRow("uncrafter", "eu_per_uncraft", "Uncrafter EU per item", default = 2000, min = 1, max = 100000, step = 500),
	) + blocks(
		"crafter_controller" to "Crafter Controller",
		"uncrafter" to "Uncrafter",
		"experience_absorber" to "Experience Absorber",
	),
)
