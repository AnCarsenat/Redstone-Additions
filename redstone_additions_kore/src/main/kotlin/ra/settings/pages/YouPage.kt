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
	),
)
