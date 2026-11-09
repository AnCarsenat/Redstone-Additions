package ra.settings.pages

import ra.core.Ns

val GatesPage = Page(
	"gates", "Logic Gates", Ns.GATES,
	listOf(
		PropRow("clock", "cooldown", "Clock interval", default = 20, min = 2, max = 1200, step = 2),
		PropRow("delayer", "delay", "Delayer delay", default = 20, min = 1, max = 1200, step = 5),
		PropRow("extender", "extend", "Extender duration", default = 20, min = 1, max = 1200, step = 5),
		PropRow("shortener", "pulse", "Shortener pulse", default = 2, min = 1, max = 200, step = 1),
		PropRow("randomizer", "chance", "Randomizer chance %", default = 50, min = 0, max = 100, step = 5),
		PropRow("counter", "target", "Counter target", default = 10, min = 1, max = 1000, step = 1),
		PropRow("edge_detector", "pulse", "Edge Detector pulse", default = 2, min = 1, max = 200, step = 1),
	) + blocks(
		"uni_gate" to "Universal Gate",
		"clock" to "Clock",
		"delayer" to "Delayer",
		"extender" to "Extender",
		"shortener" to "Shortener",
		"randomizer" to "Randomizer",
		"counter" to "Counter",
		"edge_detector" to "Edge Detector",
	),
)
