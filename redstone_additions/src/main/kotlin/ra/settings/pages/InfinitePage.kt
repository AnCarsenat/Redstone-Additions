package ra.settings.pages

import ra.core.Ns

/** id, name, default interval: must match each generator's own `defaultCooldown`. */
private val generators = listOf(Triple("mineral", "Mineral", 100), Triple("nether", "Nether", 100), Triple("poppy", "Poppy", 80))

val InfinitePage = Page(
	"infinite", "Infinite Generators", Ns.INFINITE,
	generators.map { (id, name, default) ->
		PropRow("${id}_generator", "cooldown", "$name interval", default = default, min = 1, max = 1200, step = 10)
	} + generators.map { (id, name, _) -> BlockRow("${id}_generator", "$name Generator") },
)
