package ra.settings.pages

import ra.core.Ns

val WiresPage = Page(
	"wires", "Power & Fluids", Ns.WIRES,
	listOf(
		PropRow("electric_generator", "generation_rate", "Generator EU/tick", default = 60, min = 1, max = 10000, step = 10),
		PropRow("electric_consumer", "cooldown", "Consumer interval", default = 20, min = 1, max = 1200, step = 5),
		PropRow("industrial_light", "eu_use", "Industrial Light EU", default = 10, min = 0, max = 1000, step = 5),
		PropRow("liquid_drain", "speed", "Liquid Drain speed", default = 5, min = 1, max = 100, step = 1),
	) + blocks(
		"electric_generator" to "EU Generator",
		"electric_furnace" to "Electric Furnace",
		"solar_panel" to "Solar Panel",
		"battery" to "Battery",
		"industrial_light" to "Industrial Light",
		"electric_switch" to "EU Switch",
		"electric_breaker" to "EU Breaker",
		"electric_consumer" to "EU Consumer",
		"boiler" to "Boiler",
		"liquid_drain" to "Liquid Drain",
		"liquid_pump" to "Liquid Pump",
		"gas_pump" to "Gas Pump",
		"creative_eu" to "Creative EU Source",
		"creative_fluid" to "Creative Fluid Source",
	),
)
