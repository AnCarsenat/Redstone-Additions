package ra.tools

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.conditions.entityProperties
import io.github.ayfri.kore.features.predicates.predicate
import io.github.ayfri.kore.features.predicates.sub.flags
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.core.Ns
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.tools.clipboard.Clipboard
import ra.tools.creativedatahandler.CreativeDataHandler
import ra.tools.datahandler.DataHandler
import ra.tools.goggles.lens.GogglesLens
import ra.tools.inputdatahandler.InputDataHandler
import ra.tools.multimeter.Multimeter
import ra.tools.wrench.Wrench

/**
 * The tools' data files: crafting recipes with their unlock advancements, the `using_item`
 * advancements that turn a right-click into a function call, and the two player predicates.
 */
internal fun DataPack.registerToolResources() {
	toolRecipe(ToolItems.wrench, Items.BLAZE_ROD, RecipeCategory.REDSTONE, Items.BLAZE_ROD) {
		pattern("I I", " S ", " S ")
		key("S", Items.STICK); key("I", Items.IRON_INGOT)
	}
	toolRecipe(ToolItems.dataHandler, Items.BLAZE_ROD, RecipeCategory.REDSTONE, Items.BLAZE_ROD) {
		pattern("T T", "ISI", "ISI")
		key("T", Items.REDSTONE_TORCH); key("I", Items.IRON_INGOT); key("S", Items.STICK)
	}
	// Goggles kept their own trigger name from before the others were made uniform.
	toolRecipe(ToolItems.goggles, Items.LEATHER_HELMET, RecipeCategory.EQUIPMENT, Items.COPPER_INGOT, trigger = "has_copper") {
		pattern(" H ", "PGP")
		key("H", Items.COPPER_INGOT); key("P", Items.GLASS_PANE); key("G", Items.GOLD_INGOT)
	}
	// The lens is the expensive way to the same view: the Goggles' gold and glass, made precious.
	// A recipe ingredient cannot ask for the Goggles item itself (it would take any leather helmet).
	toolRecipe(ToolItems.gogglesLens, Items.FIREWORK_STAR, RecipeCategory.EQUIPMENT, Items.NETHERITE_INGOT) {
		pattern("TDT", "GNG", "TDT")
		key("T", Items.TINTED_GLASS); key("D", Items.DIAMOND); key("G", Items.GOLD_INGOT); key("N", Items.NETHERITE_INGOT)
	}
	toolRecipe(ToolItems.clipboard, Items.PAPER, RecipeCategory.EQUIPMENT, Items.PAPER) {
		pattern("III", "PPP", "IPI")
		key("I", Items.IRON_NUGGET); key("P", Items.PAPER)
	}
	toolRecipe(ToolItems.multimeter, Items.CLOCK, RecipeCategory.EQUIPMENT, Items.QUARTZ) {
		pattern(" C ", "RQR", " I ")
		key("C", Items.COPPER_INGOT); key("R", Items.REDSTONE); key("Q", Items.QUARTZ); key("I", Items.IRON_INGOT)
	}

	useAdvancement(ToolItems.clipboard, Items.PAPER, Clipboard.onUse)
	useAdvancement(ToolItems.creativeDataHandler, Items.BLAZE_ROD, CreativeDataHandler.onUse)
	useAdvancement(ToolItems.gogglesLens, Items.FIREWORK_STAR, GogglesLens.onUse)
	useAdvancement(ToolItems.dataHandler, Items.BLAZE_ROD, DataHandler.onUse)
	useAdvancement(ToolItems.inputDataHandler, Items.BLAZE_ROD, InputDataHandler.onUse)
	useAdvancement(ToolItems.multimeter, Items.CLOCK, Multimeter.onUse)
	useAdvancement(ToolItems.wrench, Items.BLAZE_ROD, Wrench.onUse)

	predicate("is_sneaking") {
		namespace = Ns.RA.id
		entityProperties { flags { isSneaking = true } }
	}
	predicate("is_sprinting") {
		namespace = Ns.RA.id
		entityProperties { flags { isSprinting = true } }
	}
}

/** `ra:<id>` and `ra:recipes/unlock_<id>`, unlocked by holding [unlockedBy] (criterion [trigger]). */
private fun DataPack.toolRecipe(
	tool: ToolItem,
	resultItem: ItemArgument,
	category: RecipeCategory,
	unlockedBy: ItemArgument,
	trigger: String = "has_ingredient",
	shape: CraftingShaped.() -> Unit,
) = shapedRecipe(Ns.RA, tool.id, tool.spec.recipeResult(resultItem), Unlock(unlockedBy, trigger), category, shape = shape)

/** `ra:tools/<id>_use`: right-click with the tool runs [onUse] as the player. It revokes itself. */
private fun DataPack.useAdvancement(tool: ToolItem, item: ItemArgument, onUse: FunctionArgument) {
	advancement("tools/${tool.id}_use") {
		namespace = Ns.RA.id
		criteria {
			usingItem("requirement") {
				this.item = itemStackPredicate(item) {
					predicates { customData { this["ra"] = nbt { this[tool.id] = true } } }
				}
			}
		}
		rewards { function = onUse }
	}
}
