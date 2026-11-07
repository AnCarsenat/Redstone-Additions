package ra.settings.pages

import ra.core.Ns

val SensorsPage = Page(
	"sensors", "Sensors", Ns.SENSORS,
	blocks(
		"entity_detector" to "Entity Detector",
		"tag_adder" to "Tag Adder",
		"tag_remover" to "Tag Remover",
	),
)
