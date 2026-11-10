package ra.modules.automation.uncrafter

/**
 * What the [Uncrafter] takes apart, and into what: the vanilla crafting recipe of each item, run
 * backwards. Written into `storage ra_automation:uncraft map` at load, keyed by item id:
 *
 * ```snbt
 * {"minecraft:iron_door":{n:3,out:[{id:"minecraft:iron_ingot",count:6}]}}
 * ```
 *
 * `n` is how many the recipe makes (an Uncrafter waits until a stack holds that many), `out` the
 * ingredients it used.
 *
 * WHAT IS IN IT, AND WHY ONLY THAT. A recipe is in the table only when running it backwards can
 * never be worth more than running it forwards:
 *  - Exactly the vanilla crafting recipe, never more. Iron block to nine ingots and nine ingots to
 *    an iron block cost the same both ways, so the loop gives nothing for free.
 *  - One recipe, one set of ingredients. A recipe taking "any planks" or "sand or red sand" is left
 *    out: there is no right answer to which one comes back.
 *  - Nothing a stonecutter makes more cheaply than the crafting table (stairs: the crafting table
 *    gives 4 for 6 blocks, the stonecutter 1 for 1, so uncrafting 4 stairs would turn 4 blocks
 *    into 6).
 *  - Nothing that returns a container item (cake, honey block: their buckets and bottles came
 *    back when they were crafted).
 *  - Nothing found in bulk in the world whose ingredients are scarce (amethyst blocks, melons).
 * Tool materials are vanilla tags holding a single item (`#iron_tool_materials` is the iron
 * ingot), so tools are in.
 *
 * Damaged, enchanted, renamed or otherwise changed items are never taken apart: the Uncrafter
 * passes any stack with components on unchanged (see [UncrafterRun]).
 *
 * Every row here was checked against the 26.3 recipe files.
 */
internal object UncraftTable {
	/** [id] made [n] at a time from [ingredients] (id to count). Ids without a namespace are vanilla. */
	data class Row(val id: String, val n: Int, val ingredients: List<Pair<String, Int>>)

	private fun r(id: String, n: Int, vararg ingredients: Pair<String, Int>) = Row(id, n, ingredients.toList())

	private fun tools(material: String, prefix: String) = listOf(
		r("${prefix}_sword", 1, material to 2, "stick" to 1),
		r("${prefix}_pickaxe", 1, material to 3, "stick" to 2),
		r("${prefix}_axe", 1, material to 3, "stick" to 2),
		r("${prefix}_shovel", 1, material to 1, "stick" to 2),
		r("${prefix}_hoe", 1, material to 2, "stick" to 2),
		r("${prefix}_helmet", 1, material to 5),
		r("${prefix}_chestplate", 1, material to 8),
		r("${prefix}_leggings", 1, material to 7),
		r("${prefix}_boots", 1, material to 4),
	)

	val rows: List<Row> = listOf(
		// Storage blocks.
		r("iron_block", 1, "iron_ingot" to 9),
		r("gold_block", 1, "gold_ingot" to 9),
		r("diamond_block", 1, "diamond" to 9),
		r("emerald_block", 1, "emerald" to 9),
		r("lapis_block", 1, "lapis_lazuli" to 9),
		r("redstone_block", 1, "redstone" to 9),
		r("coal_block", 1, "coal" to 9),
		r("copper_block", 1, "copper_ingot" to 9),
		r("netherite_block", 1, "netherite_ingot" to 9),
		r("raw_iron_block", 1, "raw_iron" to 9),
		r("raw_gold_block", 1, "raw_gold" to 9),
		r("raw_copper_block", 1, "raw_copper" to 9),
		r("quartz_block", 1, "quartz" to 4),
		r("bone_block", 1, "bone_meal" to 9),
		r("slime_block", 1, "slime_ball" to 9),
		r("hay_block", 1, "wheat" to 9),
		r("dried_kelp_block", 1, "dried_kelp" to 9),
		r("snow_block", 1, "snowball" to 4),
		r("glowstone", 1, "glowstone_dust" to 4),
		r("clay", 1, "clay_ball" to 4),
		r("bricks", 1, "brick" to 4),
		r("netherite_ingot", 1, "netherite_scrap" to 4, "gold_ingot" to 4),
	) + tools("iron_ingot", "iron") + tools("gold_ingot", "golden") + tools("diamond", "diamond") + listOf(
		r("leather_helmet", 1, "leather" to 5),
		r("leather_chestplate", 1, "leather" to 8),
		r("leather_leggings", 1, "leather" to 7),
		r("leather_boots", 1, "leather" to 4),
		r("turtle_helmet", 1, "turtle_scute" to 5),
		r("bow", 1, "stick" to 3, "string" to 3),
		r("crossbow", 1, "stick" to 3, "string" to 2, "iron_ingot" to 1, "tripwire_hook" to 1),
		r("shears", 1, "iron_ingot" to 2),
		r("compass", 1, "iron_ingot" to 4, "redstone" to 1),
		r("clock", 1, "gold_ingot" to 4, "redstone" to 1),
		r("spyglass", 1, "copper_ingot" to 2, "amethyst_shard" to 1),

		// Redstone and machines.
		r("hopper", 1, "iron_ingot" to 5, "chest" to 1),
		r("observer", 1, "cobblestone" to 6, "redstone" to 2, "quartz" to 1),
		r("dispenser", 1, "cobblestone" to 7, "bow" to 1, "redstone" to 1),
		r("dropper", 1, "cobblestone" to 7, "redstone" to 1),
		r("repeater", 1, "stone" to 3, "redstone_torch" to 2, "redstone" to 1),
		r("comparator", 1, "stone" to 3, "redstone_torch" to 3, "quartz" to 1),
		r("crafter", 1, "iron_ingot" to 5, "crafting_table" to 1, "redstone" to 2, "dropper" to 1),
		r("target", 1, "hay_block" to 1, "redstone" to 4),
		r("heavy_weighted_pressure_plate", 1, "iron_ingot" to 2),
		r("light_weighted_pressure_plate", 1, "gold_ingot" to 2),
		r("iron_door", 3, "iron_ingot" to 6),
		r("iron_trapdoor", 1, "iron_ingot" to 4),
		r("iron_bars", 16, "iron_ingot" to 6),
		r("minecart", 1, "iron_ingot" to 5),
		r("bucket", 1, "iron_ingot" to 3),
		r("cauldron", 1, "iron_ingot" to 7),
		r("anvil", 1, "iron_block" to 3, "iron_ingot" to 4),
		r("lantern", 1, "iron_nugget" to 8, "torch" to 1),

		// Late game.
		r("enchanting_table", 1, "book" to 1, "diamond" to 2, "obsidian" to 4),
		r("ender_chest", 1, "obsidian" to 8, "ender_eye" to 1),
		r("ender_eye", 1, "ender_pearl" to 1, "blaze_powder" to 1),
		r("beacon", 1, "glass" to 5, "nether_star" to 1, "obsidian" to 3),
		r("conduit", 1, "nautilus_shell" to 8, "heart_of_the_sea" to 1),
		r("lodestone", 1, "chiseled_stone_bricks" to 8, "iron_ingot" to 1),
		r("respawn_anchor", 1, "crying_obsidian" to 6, "glowstone" to 3),
	)

	private fun ns(id: String) = if (':' in id) id else "minecraft:$id"

	init {
		require(rows.map { it.id }.toSet().size == rows.size) { "UncraftTable: an item is listed twice" }
		for (row in rows) {
			require(row.n in 1..64 && row.ingredients.isNotEmpty()) { "UncraftTable ${row.id}: bad row" }
			// One output slot per ingredient at most: the free-slot check in UncrafterRun relies on it.
			require(row.ingredients.all { it.second in 1..16 }) { "UncraftTable ${row.id}: an ingredient count must fit one stack" }
		}
	}

	/** The most distinct ingredients a row has: how many free output slots an uncraft can need. */
	val maxOutputs = rows.maxOf { it.ingredients.size }

	/** The whole table as one SNBT compound, for `data modify storage ... set value`. */
	fun snbt(): String = rows.joinToString(",", "{", "}") { row ->
		val out = row.ingredients.joinToString(",", "[", "]") { (id, count) -> "{id:\"${ns(id)}\",count:$count}" }
		"\"${ns(row.id)}\":{n:${row.n},out:$out}"
	}
}
