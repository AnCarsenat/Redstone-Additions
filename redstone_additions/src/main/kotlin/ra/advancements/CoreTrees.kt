package ra.advancements

import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.generated.Items
import ra.advancements.Flag.ONE
import ra.advancements.Flag.TRUE

/** A module's tab entry under `ra_advancements:ra`: any item of [item] unlocks it. */
internal fun category(tree: String, icon: ItemArgument, title: String, description: String, criterion: String, item: ItemArgument) =
	Unlock(tree, "ra", icon, title, description, criterion, item, announce = null)

/** The tab itself and the core tools. */
internal val coreTree = listOf(
	Unlock(
		"root", null, Items.CRAFTER, "Redstone Additions", "The world of redstone at the palm of your hand.",
		"on_game_load", Items.REDSTONE, toast = null, announce = null, background = "minecraft:block/redstone_ore",
	),
	Unlock(
		"ra", "root", Items.REDSTONE, "Redstone Additions Core", "Discover the core features of Redstone Additions.",
		"get_core_item", Items.REDSTONE, announce = null,
	),
	egg("ra", "clipboard", "Clipboard", Items.PAPER, ONE, item = Items.PAPER),
	egg("ra", "multimeter", "Multimeter", Items.CLOCK, ONE, item = Items.CLOCK),
)

/** The example multiblock: a path rather than a fan, each step the parent of the next. */
internal val exampleTree = listOf(
	Unlock(
		"ra_example", "ra", Items.BLAST_FURNACE, "Example Multiblocks", "Learn the example multiblock.",
		"start_example", Items.BAT_SPAWN_EGG, Match.Exact(raData("example_copper_base", TRUE)),
	),
	Unlock(
		"ra_example/get_blast_forge_base", "ra_example", Items.BLAST_FURNACE, "Blast Forge Base",
		"Obtain the example Blast Forge core and place it in the world.",
		"get_blast_forge_base", Items.BAT_SPAWN_EGG, Match.Exact(raData("example_copper_base", TRUE)),
	),
)

internal val gatesTree = listOf(
	category("ra_gates", Items.COMPARATOR, "Redstone Gates", "Unlock the power of logic gates.", "get_gate_item", Items.COMPARATOR),
	egg("ra_gates", "uni_gate", "Uni Gate", Items.SMOOTH_STONE_SLAB, ONE, file = "get_gate"),
	egg("ra_gates", "delayer", "Delayer", Items.CHISELED_STONE_BRICKS, TRUE),
	egg("ra_gates", "extender", "Extender", Items.MOSSY_COBBLESTONE, TRUE),
	egg("ra_gates", "randomizer", "Randomizer", Items.BOOKSHELF, TRUE),
	egg("ra_gates", "shortener", "Shortener", Items.MOSSY_STONE_BRICKS, TRUE),
	egg("ra_gates", "counter", "Counter", Items.CHISELED_QUARTZ_BLOCK, TRUE),
	egg("ra_gates", "edge_detector", "Edge Detector", Items.QUARTZ_PILLAR, TRUE),
	egg("ra_gates", "signal_calculator", "Signal Calculator", Items.QUARTZ_BRICKS, TRUE),
)

internal val sensorsTree = listOf(
	category("ra_sensors", Items.OBSERVER, "Redstone Sensors", "Utilize sensors to detect the world around you.", "get_sensor_item", Items.OBSERVER),
	egg("ra_sensors", "entity_detector", "Entity Detector", Items.SCULK_SENSOR, TRUE),
	egg("ra_sensors", "tag_adder", "Tag Adder", Items.GREEN_GLAZED_TERRACOTTA, ONE, "Obtain the Tag Adder item."),
	egg("ra_sensors", "tag_remover", "Tag Remover", Items.RED_GLAZED_TERRACOTTA, ONE, "Obtain the Tag Remover item."),
)

internal val wirelessTree = listOf(
	category("ra_wireless", Items.END_STONE_BRICKS, "Redstone Wireless", "Control redstone signals wirelessly.", "get_wireless_item", Items.GOLD_INGOT),
	egg("ra_wireless", "emitter", "Wireless Emitter", Items.END_STONE_BRICKS, TRUE),
	egg("ra_wireless", "receiver", "Wireless Receiver", Items.PURPUR_BLOCK, TRUE),
	egg("ra_wireless", "remote", "Redstone Remote", Items.RED_DYE, TRUE, item = Items.WARPED_FUNGUS_ON_A_STICK),
)

/** `criterion` is `get_sensor_item` here too: the tab was copied from Sensors and kept the name. */
internal val interactiveTree = listOf(
	category("ra_interactive", Items.PISTON, "Redstone Interactive", "Utilize blocks to interact with the world around you.", "get_sensor_item", Items.OBSERVER),
	egg("ra_interactive", "big_torch", "Big Torch", Items.TORCH, ONE, "Obtain the Big Torch item."),
	egg("ra_interactive", "block_breaker", "Block Breaker", Items.IRON_PICKAXE, ONE, "Obtain the Block Breaker item."),
	egg("ra_interactive", "block_placer", "Block Placer", Items.DISPENSER, ONE, "Obtain the Block Placer item."),
	egg("ra_interactive", "breeder", "Breeder", Items.DISPENSER, ONE, "Obtain the Breeder item."),
	egg("ra_interactive", "infinite_lava_cauldron", "Infinite Lava Cauldron", Items.CAULDRON, TRUE),
	egg("ra_interactive", "infinite_snow_cauldron", "Infinite Snow Cauldron", Items.CAULDRON, TRUE),
	egg("ra_interactive", "infinite_water_cauldron", "Infinite Water Cauldron", Items.CAULDRON, TRUE),
	egg("ra_interactive", "item_mover", "Item Mover", Items.DISPENSER, ONE, "Obtain the Item Mover item."),
	egg("ra_interactive", "item_pipe", "Item Pipe", Items.DISPENSER, TRUE),
	egg("ra_interactive", "magic_crate", "Magic Crate", Items.BARREL, ONE, "Obtain the Magic Crate item."),
	egg("ra_interactive", "message_block", "Message Block", Items.NOTE_BLOCK, TRUE),
	egg("ra_interactive", "spitter", "Spitter", Items.DROPPER, TRUE),
)
