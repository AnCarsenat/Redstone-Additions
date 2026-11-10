package ra.modules.infinite

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import ra.core.Ns
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.sacrificeRecipe
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.item.flag

/**
 * The Generator Casing and the three cores, the crafting items behind the generators.
 *
 * Vanilla recipe ingredients match on item id alone -- they cannot ask for custom_data -- so the
 * casing and each core are built on block items no survival player can obtain or place
 * (GameMasterBlockItem: placing one needs creative *and* permission level 2). That is what keeps
 * a bare netherite scrap from standing in for a casing, with no advancement guard in the way. The
 * Item Crate uses a plain command block, so these four ids are deliberately different from it.
 */
object InfiniteItems {
	val casing = itemSpec("minecraft:repeating_command_block") {
		model("minecraft:copper_grate")
		name("Generator Casing")
		rarity("uncommon")
		glint()
		lore(loreLine("Craft with a core to build a generator", "gray"))
		customData(raData(flag("generator_casing")))
	}

	/**
	 * A generator core. In survival each comes out of an enchanting table: sacrifice [sacrifice],
	 * 1% a piece (see `enchant_recipes`). Each sits on its own unplaceable block item, which is
	 * what lets the three generator recipes tell each other apart.
	 */
	class Core(val key: String, val base: ItemArgument, val model: String, val name: String, val generator: String, val sacrifice: String) {
		val item = itemSpec(base.asId()) {
			model(model)
			this.name(name)
			rarity("epic")
			glint()
			lore(loreLine("Craft with a Generator Casing", "gray"), loreLine("to build a $generator", "gray"))
			customData(raData(flag(key)))
		}

		val give = ref(Ns.INFINITE, "items/give_$key")
	}

	val mineralCore = Core("mineral_core", Items.JIGSAW, "minecraft:deepslate_diamond_ore", "Mineral Core", "Mineral Generator", "minecraft:stone")
	val netherCore = Core("nether_core", Items.STRUCTURE_BLOCK, "minecraft:ancient_debris", "Nether Core", "Nether Generator", "minecraft:netherrack")
	val poppyCore = Core("poppy_core", Items.CHAIN_COMMAND_BLOCK, "minecraft:flowering_azalea", "Poppy Core", "Poppy Generator", "minecraft:poppy")
	val cores = listOf(mineralCore, netherCore, poppyCore)

	val giveCasing = ref(Ns.INFINITE, "items/give_generator_casing")
	val enchantRecipes = ref(Ns.INFINITE, "enchant_recipes")

	fun Ra.registerInfiniteItems() {
		pack.define(giveCasing) { raw("give @s ${casing.giveArg()}") }
		for (core in cores) pack.define(core.give) { raw("give @s ${core.item.giveArg()}") }

		pack.shapedRecipe(
			Ns.INFINITE, "generator_casing", casing.recipeResult(Items.REPEATING_COMMAND_BLOCK),
			Unlock(Items.NETHERITE_INGOT), RecipeCategory.MISC,
		) {
			pattern("GGG", "GNG", "GGG")
			key("G", Items.COPPER_GRATE)
			key("N", Items.NETHERITE_INGOT)
		}

		// Sacrifice recipes for the cores, listed in #ra_enchanting:recipes. Input: storage
		// ra:enchant input. The first recipe that writes `result` wins, so each returns once matched.
		pack.define(enchantRecipes) {
			for (core in cores) sacrificeRecipe("{id:\"${core.sacrifice}\"}", core.item.stackSnbt(), chance = 1)
			returnValue(0)
		}
	}
}
