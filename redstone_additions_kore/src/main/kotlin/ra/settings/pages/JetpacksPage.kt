package ra.settings.pages

import ra.core.Ns

val JetpacksPage = Page(
	"jetpacks", "Jetpacks", Ns.JETPACKS,
	listOf(
		NoteRow("Thruster Kit only — other kits are unaffected"),
		IntRow("jetpack_thrust", "Thruster thrust (% of speed)", Scope.GLOBAL, default = 80, min = 10, max = 300, step = 10),
		IntRow("jetpack_speed_cap", "Thruster speed cap (cm/tick)", Scope.GLOBAL, default = 350, min = 50, max = 2000, step = 25),
		IntRow("jetpack_deadzone", "Thruster deadzone (cm/tick)", Scope.GLOBAL, default = 25, min = 0, max = 200, step = 5),
	),
)
