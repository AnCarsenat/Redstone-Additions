package ra.settings.pages

import ra.core.Ns

val GeneralPage = Page(
	"general", "General", Ns.RA,
	listOf(
		BoolRow("welcome", "Show the load message", Scope.GLOBAL, default = 1),
		StrRow("prefix", "Chat prefix", Scope.GLOBAL, default = "RA"),
	),
)
