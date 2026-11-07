package ra.settings.pages

import ra.core.Ns

private val generators = listOf("mineral" to "Mineral", "nether" to "Nether", "poppy" to "Poppy")

val InfinitePage = Page(
	"infinite", "Infinite Generators", Ns.INFINITE,
	generators.map { (id, name) ->
		PropRow("${id}_generator", "cooldown", "$name interval", default = 100, min = 1, max = 1200, step = 10)
	} + generators.map { (id, name) -> BlockRow("${id}_generator", "$name Generator") },
)
