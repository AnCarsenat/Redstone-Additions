package ra.modules.automation.pattern

import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.item.SBool
import ra.core.item.SInt
import ra.core.item.SList
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref

/**
 * Recipe Pattern: a [Pattern] that also tells the Crafter Controller to fill the crafter itself.
 *
 * DATA FORMAT. The Pattern's, plus one flag:
 *
 * ```
 * custom_data: {ra:{pattern:{slots:[...nine ids...],recipe:1b}}}
 * ```
 *
 * So it IS a Pattern ([Pattern.HELD] matches it): it is recorded and cleared the same way, every
 * block in [Pattern.targets] takes it, and a reader that only knows `slots` reads it as a plain
 * Pattern. The recording rewrites only `ra.pattern.slots`, so the flag stays. Recipe Patterns do
 * not stack with Patterns (their custom_data differs).
 *
 * The result is not stored: a crafter works it out from its grid, and a data pack cannot ask the
 * game for it. The grid is the recipe.
 *
 * Who reads the flag: the Crafter Controller. With a Recipe Pattern loaded it takes the
 * ingredients from the container on its other side and puts them in the crafter's slots, then
 * fires it ([ra.modules.automation.crafter.CrafterFill]).
 */
object RecipePattern {
	val give = ref(Ns.AUTOMATION, "pattern/give_recipe")

	/** An item test for a Recipe Pattern, for `if items ... $HELD`. */
	const val HELD = """*[minecraft:custom_data~{ra:{pattern:{recipe:1b}}}]"""

	val item = itemSpec("minecraft:paper") {
		model("minecraft:globe_banner_pattern")
		name("Recipe Pattern")
		add("max_stack_size", SInt(Pattern.MAX_STACK))
		customData(raData("pattern" to comp("slots" to SList(Pattern.BLANK_SLOTS.map { SStr(it) }), "recipe" to SBool(true))))
		lore(*blankLore().toTypedArray())
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	/** The lore of a blank Recipe Pattern, as SNBT compounds. */
	fun blankLore() = Pattern.blankLore() + loreLine("A Crafter Controller fills the crafter from it", "dark_gray")

	/** The hint lines under a recorded grid. */
	fun hintLore() = listOf(
		loreLine("Right-click a Crafter Controller to load it:", "dark_gray"),
		loreLine("it fills the crafter from the container behind it", "dark_gray"),
		loreLine("Sneak + right-click air to clear", "dark_gray"),
	)

	fun Ra.registerRecipePattern() {
		pack.define(give) { raw("give @s ${item.giveArg()}") }

		// The Pattern's layout, with a hopper for the redstone: it is the one that fetches.
		pack.shapedRecipe(Ns.AUTOMATION, "recipe_pattern", item.recipeResult(Items.PAPER), Unlock(Items.CRAFTER), RecipeCategory.MISC, count = 3) {
			pattern(" H ", "PPP")
			key("H", Items.HOPPER)
			key("P", Items.PAPER)
		}
	}
}
