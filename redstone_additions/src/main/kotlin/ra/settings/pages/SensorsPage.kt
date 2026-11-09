package ra.settings.pages

import ra.core.Ns

val SensorsPage = Page(
	"sensors", "Sensors", Ns.SENSORS,
	listOf(
		// Off: only admins may edit an entity selector or a tag (the Tag Adder can reach any entity
		// and hand out any tag). See the Data Handler's adminOnly(setting = ...).
		BoolRow("player_selectors", "Players may edit entity selectors and tags", Scope.GLOBAL, default = 0),
	) + blocks(
		"entity_detector" to "Entity Detector",
		"tag_adder" to "Tag Adder",
		"tag_remover" to "Tag Remover",
		"light_sensor" to "Light Sensor",
		"weather_sensor" to "Weather Sensor",
	),
)
