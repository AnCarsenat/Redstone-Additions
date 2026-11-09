package ra.settings.pages

import ra.core.Ns
import ra.modules.enchanting.Disenchanter

val EnchantingPage = Page(
	"enchanting", "Enchant Crafting", Ns.ENCHANTING,
	listOf(
		BoolRow("enchanting", "Enchant crafting", Scope.GLOBAL, default = 1),
		IntRow(Disenchanter.COST_KEY, "Disenchanter mL of experience per enchantment", Scope.GLOBAL, default = Disenchanter.DEFAULT_COST, min = 0, max = 100000, step = 100),
	) + blocks(
		"disenchanter" to "Disenchanter",
	),
)
