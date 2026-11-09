package ra.settings.pages

import ra.core.Ns
import ra.modules.enchanting.BookPrinter
import ra.modules.enchanting.BookUpgrader
import ra.modules.enchanting.Disenchanter

val EnchantingPage = Page(
	"enchanting", "Enchant Crafting", Ns.ENCHANTING,
	listOf(
		BoolRow("enchanting", "Enchant crafting", Scope.GLOBAL, default = 1),
		IntRow(Disenchanter.COST_KEY, "Disenchanter mL of experience per enchantment", Scope.GLOBAL, default = Disenchanter.DEFAULT_COST, min = 0, max = 100000, step = 100),
		IntRow(BookUpgrader.BASE_KEY, "Book Upgrader base price in mL (x2 per level)", Scope.GLOBAL, default = BookUpgrader.DEFAULT_BASE, min = 1, max = 1000, step = 50),
		IntRow(BookUpgrader.CAP_KEY, "Book Upgrader highest level", Scope.GLOBAL, default = BookUpgrader.DEFAULT_CAP, min = 1, max = BookUpgrader.MAX_CAP, step = 1),
		IntRow(BookPrinter.CHANCE_KEY, "Book Printer chance of a Schematic (%)", Scope.GLOBAL, default = BookPrinter.DEFAULT_CHANCE, min = 0, max = 100, step = 1),
		IntRow(BookPrinter.COST_KEY, "Book Printer mL of experience per level printed", Scope.GLOBAL, default = BookPrinter.DEFAULT_COST, min = 0, max = 100000, step = 100),
	) + blocks(
		"disenchanter" to "Disenchanter",
		"book_upgrader" to "Book Upgrader",
		"book_printer" to "Book Printer",
	),
)
