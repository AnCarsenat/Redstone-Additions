package ra.settings.pages

import ra.core.Ns
import ra.modules.ender.tablet.AnchorTablet

val EnderPage = Page(
	"ender", "Ender Vaults", Ns.ENDER,
	listOf(
		PropRow("ender_power_vault", "transfer_rate", "Power Vault EU/tick", default = 80, min = 1, max = 10000, step = 10),
		PropRow("ender_fluid_vault", "transfer_rate", "Fluid Vault mB/tick", default = 200, min = 1, max = 10000, step = 50),
		IntRow(AnchorTablet.COST_KEY, "Anchor Tablet ender pearls per trip", Scope.GLOBAL, default = AnchorTablet.DEFAULT_COST, min = 0, max = 16, step = 1),
		IntRow(AnchorTablet.COOLDOWN_KEY, "Anchor Tablet recharge (seconds)", Scope.GLOBAL, default = AnchorTablet.DEFAULT_COOLDOWN, min = 0, max = 600, step = 5),
	) + blocks(
		"ender_item_vault" to "Ender Item Vault",
		"ender_fluid_vault" to "Ender Fluid Vault",
		"ender_power_vault" to "Ender Power Vault",
		"teleport_anchor" to "Teleport Anchor",
		"anchor_terminal" to "Anchor Terminal",
	),
)
