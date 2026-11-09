package ra.settings.pages

import ra.core.Ns
import ra.modules.interactive.Elevator
import ra.modules.interactive.MagicCrate
import ra.modules.interactive.MessageBlock

val InteractivePage = Page(
	"interactive", "Interactive Blocks", Ns.INTERACTIVE,
	listOf(
		// Ranges go to the admin ceiling: only an admin reaches these rows (ra.core.Admin).
		PropRow("magic_crate", "radius", "Magic Crate radius", default = 8, min = 5, max = MagicCrate.ADMIN_MAX_RADIUS, step = 1),
		PropRow("magic_crate", "cooldown", "Magic Crate interval", default = 20, min = 1, max = 1200, step = 5),
		PropRow("block_breaker", "cooldown", "Block Breaker interval", default = 20, min = 1, max = 1200, step = 5),
		PropRow("block_placer", "cooldown", "Block Placer interval", default = 20, min = 1, max = 1200, step = 5),
		PropRow("message_block", "range", "Message Block range", default = 16, min = 1, max = MessageBlock.ADMIN_MAX_RANGE, step = 4),
		PropRow("elevator", "range", "Elevator range", default = Elevator.DEFAULT_RANGE, min = 2, max = Elevator.ADMIN_MAX_RANGE, step = 4),
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
		"elevator" to "Elevator",
	),
)
