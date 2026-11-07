package ra.core.block

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.components.ComponentsScope
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.predicates.sub.ItemStackPredicate
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.generated.arguments.types.RecipeArgument
import ra.core.Ns
import ra.core.item.ItemSpec

/**
 * Crafting recipes and the vanilla-shaped advancements that unlock them.
 *
 * Every recipe `ns:<name>` comes with `ns:recipes/unlock_<name>`, parented to
 * `minecraft:recipes/root`, granted once the player holds the [Unlock] item (or already knows the
 * recipe), rewarding the recipe.
 */

/**
 * What a recipe produces: the result [item] and the components written on it. Components go in
 * as raw NBT (`CustomComponent`), because Kore's typed item_name / custom_data / entity_data only
 * serialize to SNBT and throw in a recipe's JSON.
 */
class RecipeResult(val item: ItemArgument, val components: ComponentsScope.() -> Unit)

/** The block's egg, with exactly the components of [CustomBlock.item]. */
fun CustomBlock.recipeResult() = RecipeResult(Items.BAT_SPAWN_EGG) { blockItemComponents() }

/** [item] (a bat spawn egg unless told otherwise) carrying this spec's components. */
fun ItemSpec.recipeResult(item: ItemArgument = Items.BAT_SPAWN_EGG) = RecipeResult(item) { applyTo(this) }

/**
 * The unlock condition: holding [item] (optionally narrowed by [match], e.g. on custom data).
 *
 * [criterion] keeps the name each beet file gave it (`has_pearl`, `has_ingredient`, `has_lava`
 * for a lava bucket...): a criterion name is how an advancement is granted or revoked by command,
 * and a renamed criterion drops the progress saved under the old name. Default `has_<item path>`.
 */
class Unlock(
	val item: ItemArgument,
	val criterion: String = "has_${item.asId().substringAfter(':')}",
	val match: ItemStackPredicate.() -> Unit = {},
)

/**
 * The shaped recipe `ns:<name>` producing [result] and, when [unlock] is given, its
 * `ns:recipes/unlock_<name>` advancement.
 *
 * [count] null leaves the count out of the JSON (vanilla defaults to 1), as several modules had
 * it. [notify] false leaves `show_notification` out. [category] null leaves the category out.
 */
fun DataPack.shapedRecipe(
	ns: Ns,
	name: String,
	result: RecipeResult,
	unlock: Unlock?,
	category: RecipeCategory? = RecipeCategory.REDSTONE,
	count: Short? = 1,
	notify: Boolean = true,
	shape: CraftingShaped.() -> Unit,
): RecipeArgument {
	val recipe = recipesBuilder.craftingShaped(name, ns.id) {
		this.category = category
		if (notify) showNotification = true
		shape()
		result(result.item, count) { result.components(this) }
	}
	if (unlock != null) recipeUnlock(name, ns, recipe, unlock)
	return recipe
}

/** `ns:recipes/unlock_<name>`, the vanilla-shaped recipe advancement for any [recipe]. */
fun DataPack.recipeUnlock(name: String, ns: Ns, recipe: RecipeArgument, unlock: Unlock): AdvancementArgument =
	advancement("recipes/unlock_$name") {
		namespace = ns.id
		parent = AdvancementArgument("recipes/root", "minecraft")
		criteria {
			recipeUnlocked("has_the_recipe", recipe)
			inventoryChanged(unlock.criterion) {
				item {
					items = listOf(unlock.item)
					unlock.match(this)
				}
			}
		}
		requirements(listOf("has_the_recipe", unlock.criterion))
		rewards { recipes(recipe) }
	}

/** [recipeUnlock] with the default criterion `has_<unlockedBy path>`. */
fun DataPack.recipeUnlock(name: String, namespace: String, recipe: RecipeArgument, unlockedBy: ItemArgument): AdvancementArgument =
	recipeUnlock(name, Ns.entries.first { it.id == namespace }, recipe, Unlock(unlockedBy))

/**
 * The crafting recipe `ns:<id>` producing the block's item, plus the advancement
 * `ns:recipes/unlock_<id>` that teaches it once the player holds [unlockedBy] (or already knows
 * it, the vanilla convention). [unlockedBy] is the ingredient that makes the recipe discoverable.
 */
fun DataPack.blockRecipe(
	block: CustomBlock,
	unlockedBy: ItemArgument,
	category: RecipeCategory? = RecipeCategory.REDSTONE,
	shape: CraftingShaped.() -> Unit,
): RecipeArgument = shapedRecipe(block.ns, block.id, block.recipeResult(), Unlock(unlockedBy), category, shape = shape)
