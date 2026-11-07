package ra.modules.interactive

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.hook
import ra.api.ModulesApi
import ra.modules.enchanting.EnchantingModule
import ra.modules.enchanting.sacrificeRecipe
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Enchanted Coal and the Enchanted Coal Block, the way to the Big Torch: coal sacrificed at an
 * enchanting table becomes Enchanted Coal, nine make a block, and the block with a stick makes a
 * Big Torch.
 *
 * Enchanted Coal sits on minecraft:structure_void for the same reason the generator cores sit on
 * their own odd blocks: a vanilla crafting recipe matches ingredients by id alone, so the
 * 9-into-a-block recipe needs an id that nothing else in the game or the pack uses. See
 * ra_infinite:enchant_recipes. The block is a barrier for the same reason.
 */
object EnchantedCoal {
	val coal = itemSpec("minecraft:structure_void") {
		model("minecraft:coal")
		name("Enchanted Coal")
		rarity("rare")
		glint()
		lore(loreLine("Nine of these make a block", "gray"))
		customData(raData(flag("enchanted_coal")))
	}

	val coalBlock = itemSpec("minecraft:barrier") {
		model("minecraft:coal_block")
		name("Enchanted Coal Block")
		rarity("rare")
		glint()
		lore(loreLine("Craft with a stick to make a Big Torch", "gray"))
		customData(raData(flag("enchanted_coal_block")))
	}

	/** Sacrifice recipes for this module. Input: storage ra:enchant input (see ra_enchanting). */
	val enchantRecipes = ModulesApi.Interactive.enchantRecipes

	fun Ra.registerEnchantedCoal() {
		pack.define(enchantRecipes) {
			// Coal -> Enchanted Coal, 1%.
			sacrificeRecipe("{id:\"minecraft:coal\"}", coal.stackSnbt(), chance = 1)
			returnValue(0)
		}
		// Listed last in #ra_enchanting:recipes; ra_infinite and ra_jetpacks insert theirs ahead of
		// it (see ra.modules.enchanting.hookEnchantRecipes for why the order is observable).
		pack.hook(Ns.ENCHANTING, EnchantingModule.recipesTag.name, enchantRecipes)

		pack.shapedRecipe(
			Ns.INTERACTIVE, "enchanted_coal_block", coalBlock.recipeResult(Items.BARRIER), unlock = null,
			RecipeCategory.BUILDING, notify = false,
		) {
			pattern("CCC", "CCC", "CCC")
			key("C", listOf(Items.STRUCTURE_VOID))
		}
	}
}
