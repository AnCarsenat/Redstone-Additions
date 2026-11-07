package ra.tools

import ra.core.item.ItemSpec

/**
 * One hand-held tool item, described once: its `give`, its recipe result and its give-bundle
 * entry are all derived from [spec], as [ra.core.block.CustomBlock] does for blocks. [id] is the
 * tool's own name (`wrench`, `data_handler`...), used for its recipe, its `using_item`
 * advancement and its `custom_data` flag.
 *
 * [spec]'s components are SNBT text in the order the beet `give` command wrote them; that order
 * is what the generated `give` keeps ([ItemSpec.fromSnbt]).
 *
 * Most tools are a food item with an endless `consumable`: `using_item` is the only advancement
 * trigger that fires reliably on right-click whatever is being looked at, and it only fires for
 * an item you can start using. The consume time is long enough that it never actually finishes.
 */
data class ToolItem(val id: String, val spec: ItemSpec)

/** The endless-use food that makes right-click detectable (see [ToolItem]). */
private val CLICKABLE = arrayOf(
	"food" to "{nutrition:0,saturation:0,can_always_eat:true}",
	"consumable" to """{consume_seconds:1000000,animation:"none",has_consume_particles:false}""",
)

private fun lore(vararg lines: String) = lines.joinToString(",", "[", "]") { """{text:"$it",italic:false,color:"gray"}""" }

private fun clickTool(id: String, item: String, model: String, name: String, lore: String) = ToolItem(
	id, ItemSpec.fromSnbt(
		item,
		"item_model" to "\"$model\"",
		"item_name" to "\"$name\"",
		"lore" to lore,
		"max_stack_size" to "1",
		"custom_data" to "{ra:{$id:1b}}",
		"enchantment_glint_override" to "true",
		*CLICKABLE,
	)
)

object ToolItems {
	val wrench = clickTool(
		"wrench", "minecraft:blaze_rod", "minecraft:orange_dye", "Wrench",
		lore("RMB: Assemble multiblocks", "Shift+RMB: Cycle blocks"),
	)
	val dataHandler = clickTool(
		"data_handler", "minecraft:blaze_rod", "minecraft:light_blue_dye", "Data Handler",
		lore("Shift+RMB: Scan and edit block properties"),
	)
	val creativeDataHandler = clickTool(
		"creative_data_handler", "minecraft:blaze_rod", "minecraft:purple_dye", "Creative Data Handler",
		lore("Shift+RMB: Edit block properties"),
	)
	val inputDataHandler = clickTool(
		"input_data_handler", "minecraft:blaze_rod", "minecraft:book", "Input Data Handler",
		lore("Shift+RMB: Open input menu", "Menu buttons use non-OP triggers"),
	)
	val clipboard = clickTool(
		"clipboard", "minecraft:paper", "minecraft:paper", "Clipboard",
		lore("Shift+RMB a block: set it as the origin", "Shift+RMB others: match them to it", "Shift+RMB at nothing: clear"),
	)
	val multimeter = clickTool(
		"multimeter", "minecraft:clock", "minecraft:clock", "Multimeter",
		lore("RMB a block: read its network", "Stored, capacity, and what this block adds"),
	)

	/** Worn, not used: no food trick. Dyed copper-orange so it reads as goggles. */
	val goggles = ToolItem(
		"goggles", ItemSpec.fromSnbt(
			"minecraft:leather_helmet",
			"item_name" to "\"Goggles\"",
			"max_stack_size" to "1",
			"custom_data" to "{ra:{goggles:1b}}",
			"dyed_color" to "16750848",
			"enchantment_glint_override" to "true",
			"lore" to """[{text:"Reveals custom block info",color:"gray",italic:false},{text:"Wear or hold to scan nearby blocks",color:"dark_gray",italic:false}]""",
		)
	)
}
