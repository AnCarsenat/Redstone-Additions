package ra.modules.enchanting

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.features.tags.TagEntry
import io.github.ayfri.kore.features.tags.functionTag
import ra.core.Ns
import ra.core.ref

/** ra_interactive's recipe list. Only its name is used here, to keep it last in the tag. */
private val interactiveRecipes = ref(Ns.INTERACTIVE, "enchant_recipes")

/**
 * Adds [fn] to `#ra_enchanting:recipes`, ahead of ra_interactive's list, which keeps the beet
 * order (infinite, jetpacks, interactive).
 *
 * The order no longer decides any chance: [sacrificeRecipe] makes the first matching recipe win
 * and keep its own chance. It only matters if two lists ever accept the same input, where the
 * earlier list wins.
 */
fun DataPack.hookEnchantRecipes(fn: FunctionArgument) {
	functionTag(EnchantingModule.recipesTag.name, EnchantingModule.recipesTag.namespace) {
		val entry = TagEntry(fn.asString())
		val at = values.indexOfFirst { it.name == interactiveRecipes.asString() }
		values = if (at < 0) values + entry else values.take(at) + entry + values.drop(at)
	}
}
