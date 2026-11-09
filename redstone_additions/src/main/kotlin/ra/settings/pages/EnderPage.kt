package ra.settings.pages

import ra.core.Ns

val EnderPage = Page(
	"ender", "Ender Vaults", Ns.ENDER,
	listOf(
		PropRow("ender_power_vault", "transfer_rate", "Power Vault EU/tick", default = 80, min = 1, max = 10000, step = 10),
		PropRow("ender_fluid_vault", "transfer_rate", "Fluid Vault mB/tick", default = 200, min = 1, max = 10000, step = 50),
	) + blocks(
		"ender_item_vault" to "Ender Item Vault",
		"ender_fluid_vault" to "Ender Fluid Vault",
		"ender_power_vault" to "Ender Power Vault",
		"teleport_anchor" to "Teleport Anchor",
		"anchor_terminal" to "Anchor Terminal",
	),
)
