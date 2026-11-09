package ra.settings.pages

import ra.core.Ns
import ra.core.Obj

/** The only page with user rows, and therefore the only page in the /trigger menu. */
val YouPage = Page(
	"you", "Your Preferences", Ns.RA,
	listOf(
		BoolRow("snd", "Machine sounds", Scope.USER, default = 1, obj = Obj.USER_SOUND),
		BoolRow("par", "Machine particles", Scope.USER, default = 1, obj = Obj.USER_PARTICLES),
		BoolRow("dbg", "Debug messages", Scope.USER, default = 0, obj = Obj.USER_DEBUG),
		BoolRow(
			"fprev", "Show filter previews", Scope.USER, default = 1, obj = Obj.USER_FILTER_PREVIEW,
			hint = "Goggles: the item an Item Pipe filters for, floating above its readout. " +
				"Billboards are shared by everyone, so a pipe shows its preview while ANY goggles " +
				"wearer near it has this on -- turn it off and you stop asking for previews, but " +
				"you still see one drawn for a nearby player who wants it. " +
				"The server can also turn previews off for everyone.",
		),
		BoolRow(
			"jpfuel", "Jetpack fuel gauge", Scope.USER, default = 1, obj = Obj.USER_JETPACK_GAUGE,
			hint = "A fuel bar on the actionbar while you fly a coal-burning jetpack: " +
				"how much of the burning piece is left, and how much coal you carry.",
		),
		BoolRow(
			"airg", "Air tank gauge", Scope.USER, default = 1, obj = Obj.USER_AIR_GAUGE,
			hint = "An air bar on the actionbar while you wear an Air Tank with your head under water, " +
				"or stand on an Air Charger: how full the tank is, and the minutes of air left.",
		),
	),
)
