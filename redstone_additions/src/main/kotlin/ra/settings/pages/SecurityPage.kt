package ra.settings.pages

import ra.core.Ns

val SecurityPage = Page(
	"security", "Security", Ns.SECURITY,
	listOf(
		PropRow("keypad", "pulse", "Keypad pulse", default = 20, min = 1, max = 200, step = 5),
		PropRow("keypad", "max_attempts", "Keypad tries before lockout", default = 3, min = 1, max = 20, step = 1),
		PropRow("keypad", "lockout", "Keypad lockout (s)", default = 30, min = 1, max = 3600, step = 10),
	) + blocks("keypad" to "Keypad"),
)
