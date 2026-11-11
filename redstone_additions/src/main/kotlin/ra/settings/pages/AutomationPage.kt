package ra.settings.pages

import ra.core.Ns
import ra.modules.automation.fakeplayer.FakePlayer

// The Fake Player is disabled (FakePlayer.ENABLED): its rows are left out with it.
val AutomationPage = Page(
	"automation", "Automation", Ns.AUTOMATION,
	listOfNotNull(
		PropRow("uncrafter", "eu_per_uncraft", "Uncrafter EU per item", default = 2000, min = 1, max = 100000, step = 500),
		PropRow("fake_player", "eu_per_mob", "Fake Player EU per mob", default = 250, min = 1, max = 100000, step = 50).takeIf { FakePlayer.ENABLED },
	) + blocks(
		"crafter_controller" to "Crafter Controller",
		"uncrafter" to "Uncrafter",
		"experience_absorber" to "Experience Absorber",
		*listOfNotNull(("fake_player" to "Fake Player").takeIf { FakePlayer.ENABLED }).toTypedArray(),
		"conveyor" to "Conveyor",
	),
)
