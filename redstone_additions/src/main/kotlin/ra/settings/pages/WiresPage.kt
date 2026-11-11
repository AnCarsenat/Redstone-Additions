package ra.settings.pages

import ra.core.Ns

val WiresPage = Page(
	"wires", "Power & Fluids", Ns.WIRES,
	listOf(
		PropRow("electric_generator", "generation_rate", "Generator EU/tick", default = 60, min = 1, max = 10000, step = 10),
		PropRow("electric_consumer", "eu_use", "Consumer EU draw", default = 40, min = 1, max = 10000, step = 10),
		PropRow("industrial_light", "eu_use", "Industrial Light EU", default = 10, min = 0, max = 1000, step = 5),
		PropRow("liquid_drain", "rate", "Fluid Drain rate (mL/s)", default = 5000, min = 100, max = 100000, step = 1000),
		PropRow("usage_meter", "every", "Usage Meter units per pulse", default = 1000, min = 1, max = 100000000, step = 1000),
		PropRow("tesla_coil", "damage", "Tesla Coil damage per zap", default = 6, min = 1, max = 40, step = 1),
		PropRow("tesla_coil", "eu_per_zap", "Tesla Coil EU per mob zapped", default = 200, min = 1, max = 100000, step = 50),
		PropRow("water_wheel", "eu_per_block", "Water Wheel EU/tick per flowing block", default = 5, min = 1, max = 1000, step = 1),
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
		"liquid_drain" to "Fluid Drain",
		"liquid_pump" to "Liquid Pump",
		"gas_pump" to "Gas Pump",
		"creative_eu" to "Creative EU Source",
		"creative_fluid" to "Creative Fluid Source",
		"network_gauge" to "Network Gauge",
		"usage_meter" to "Usage Meter",
		"tesla_coil" to "Tesla Coil",
		"spout" to "Spout",
		"water_wheel" to "Water Wheel",
		"air_charger" to "Air Charger",
	),
)
