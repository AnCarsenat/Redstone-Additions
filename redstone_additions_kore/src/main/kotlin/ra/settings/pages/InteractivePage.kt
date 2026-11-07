package ra.settings.pages

import ra.core.Ns

val InteractivePage = Page(
	"interactive", "Interactive Blocks", Ns.INTERACTIVE,
	listOf(
		PropRow("magic_crate", "radius", "Magic Crate radius", default = 8, min = 5, max = 32, step = 1),
		PropRow("magic_crate", "cooldown", "Magic Crate interval", default = 20, min = 1, max = 1200, step = 5),
		PropRow("message_block", "range", "Message Block range", default = 16, min = 1, max = 128, step = 4),
	) + blocks(
		"block_breaker" to "Block Breaker",
		"block_placer" to "Block Placer",
		"item_pipe" to "Item Pipe",
		"item_mover" to "Item Mover",
		"magic_crate" to "Magic Crate",
		"message_block" to "Message Block",
		"breeder" to "Breeder",
		"spitter" to "Spitter",
		"infinite_water_cauldron" to "Infinite Water Cauldron",
		"infinite_lava_cauldron" to "Infinite Lava Cauldron",
		"infinite_snow_cauldron" to "Infinite Snow Cauldron",
	),
)
