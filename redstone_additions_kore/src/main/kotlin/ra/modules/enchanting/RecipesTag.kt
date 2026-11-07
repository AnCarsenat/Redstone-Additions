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
 * Adds [fn] to `#ra_enchanting:recipes`, ahead of ra_interactive's list.
 *
 * The order of the tag is observable. `function #ra_enchanting:recipes` runs EVERY list, and a
 * list's `return 1` only ends that list. Each list also ends a recipe with
 * `execute if data storage ra:enchant result run data modify storage ra:enchant chance set value N`,
 * which fires on any result, its own or an earlier list's. So the last list in the tag sets the
 * chance of every match. The beet tag was infinite, jetpacks, interactive, which made every
 * sacrifice roll at 1% (ra_interactive's chance). Modules register in `ra:load` order,
 * ra_interactive first, so plain appending would put ra_jetpacks last and turn every chance into
 * 10%. Inserting this group's lists before ra_interactive's entry restores the beet order whatever
 * order the modules register in.
 */
fun DataPack.hookEnchantRecipes(fn: FunctionArgument) {
	functionTag(EnchantingModule.recipesTag.name, EnchantingModule.recipesTag.namespace) {
		val entry = TagEntry(fn.asString())
		val at = values.indexOfFirst { it.name == interactiveRecipes.asString() }
		values = if (at < 0) values + entry else values.take(at) + entry + values.drop(at)
	}
}
