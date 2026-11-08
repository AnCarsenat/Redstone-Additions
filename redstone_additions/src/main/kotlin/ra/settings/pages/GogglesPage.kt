package ra.settings.pages

import ra.core.Ns

/**
 * Goggles, a subcategory of [GeneralPage]. The keys are the ones the General page used to hold,
 * so values already chosen carry over; only the function paths moved (`admin/goggles/...`).
 */
val GogglesPage = Page(
	"goggles", "Goggles", Ns.RA,
	listOf(
		IntRow("goggles_range", "Goggles range (blocks)", Scope.GLOBAL, default = 16, min = 4, max = 64, step = 4),
		IntRow("goggles_redraw", "Goggles redraw (ticks)", Scope.GLOBAL, default = 20, min = 5, max = 100, step = 5),
		BoolRow("goggles_filter_preview", "Filter previews", Scope.GLOBAL, default = 1),
		NoteRow("Filter previews: the item an Item Pipe filters for, drawn above its readout."),
	),
	parent = "general",
)
