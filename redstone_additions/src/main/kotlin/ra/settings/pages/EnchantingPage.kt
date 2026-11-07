package ra.settings.pages

import ra.core.Ns

val EnchantingPage = Page(
	"enchanting", "Enchant Crafting", Ns.ENCHANTING,
	listOf(BoolRow("enchanting", "Enchant crafting", Scope.GLOBAL, default = 1)),
)
