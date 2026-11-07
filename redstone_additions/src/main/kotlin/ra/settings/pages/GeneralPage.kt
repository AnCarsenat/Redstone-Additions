package ra.settings.pages

import ra.core.Ns

val GeneralPage = Page(
	"general", "General", Ns.RA,
	listOf(
		BoolRow("welcome", "Show the load message", Scope.GLOBAL, default = 1),
		StrRow("prefix", "Chat prefix", Scope.GLOBAL, default = "RA"),
		IntRow("goggles_range", "Goggles range (blocks)", Scope.GLOBAL, default = 16, min = 4, max = 64, step = 4),
		IntRow("goggles_redraw", "Goggles redraw (ticks)", Scope.GLOBAL, default = 20, min = 5, max = 100, step = 5),
	),
)
