package ra.modules.multiblock.blastforge.recipes

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * Blast Forge recipes. Input: `storage ra:temp bf.material` (the item from the material barrel);
 * output: `storage ra:temp bf.result`, absent when nothing matched.
 *
 * To add a category: add a table below and a call in match_all.
 */
object BlastForgeRecipes {
	val matchAll = ref(Ns.MULTIBLOCK, "blast_forge/recipes/match_all")
	val smelting = ref(Ns.MULTIBLOCK, "blast_forge/recipes/smelting")
	val recycling = ref(Ns.MULTIBLOCK, "blast_forge/recipes/recycling")

	/** input id -> (result id, count). */
	private class Recipe(val input: String, val result: String, val count: Int)

	/** Ore smelting: doubles the yield of a vanilla furnace. */
	private val smeltingRecipes = buildList {
		// Raw ores -> 2x ingots.
		add(Recipe("raw_iron", "iron_ingot", 2))
		add(Recipe("raw_gold", "gold_ingot", 2))
		add(Recipe("raw_copper", "copper_ingot", 2))
		// Ore blocks -> 2x ingots.
		add(Recipe("iron_ore", "iron_ingot", 2))
		add(Recipe("deepslate_iron_ore", "iron_ingot", 2))
		add(Recipe("gold_ore", "gold_ingot", 2))
		add(Recipe("deepslate_gold_ore", "gold_ingot", 2))
		add(Recipe("copper_ore", "copper_ingot", 2))
		add(Recipe("deepslate_copper_ore", "copper_ingot", 2))
		// Ancient debris -> 2x netherite scrap.
		add(Recipe("ancient_debris", "netherite_scrap", 2))
		// Sand -> glass.
		add(Recipe("sand", "glass", 1))
		add(Recipe("red_sand", "glass", 1))
		// Cobblestone -> stone.
		add(Recipe("cobblestone", "stone", 1))
		add(Recipe("cobbled_deepslate", "deepslate", 1))
	}

	/**
	 * Equipment recycling: tools and armor back into nuggets, by crafting cost.
	 *   Sword = 2 ingots (18 nuggets), Shovel = 1 (9), Pick/Axe = 3 (27), Hoe = 2 (18),
	 *   Helmet = 5 (45), Chest = 8 (72), Legs = 7 (63), Boots = 4 (36), Horse armor = 6 (54).
	 */
	private val recyclingRecipes = buildList {
		val tools = listOf("sword" to 18, "pickaxe" to 27, "axe" to 27, "shovel" to 9, "hoe" to 18)
		val armor = listOf("helmet" to 45, "chestplate" to 72, "leggings" to 63, "boots" to 36)
		for ((material, nugget) in listOf("iron" to "iron_nugget", "golden" to "gold_nugget")) {
			for ((piece, n) in tools + armor + ("horse_armor" to 54)) add(Recipe("${material}_$piece", nugget, n))
		}
		// Chainmail -> iron nuggets.
		for ((piece, n) in armor) add(Recipe("chainmail_$piece", "iron_nugget", n))
	}

	fun Ra.registerRecipes() {
		pack.define(matchAll) {
			for (category in listOf(smelting, recycling)) {
				function(category)
				raw("execute if data storage ra:temp bf.result run return 1")
			}
			returnValue(0)
		}
		pack.define(smelting) { table(smeltingRecipes) }
		pack.define(recycling) { table(recyclingRecipes) }
	}

	private fun Function.table(recipes: List<Recipe>) {
		for (r in recipes) {
			raw("""execute if data storage ra:temp bf.material{id:"minecraft:${r.input}"} run data modify storage ra:temp bf.result set value {id:"minecraft:${r.result}",count:${r.count},components:{}}""")
			raw("execute if data storage ra:temp bf.result run return 1")
		}
		// No match.
		returnValue(0)
	}
}
