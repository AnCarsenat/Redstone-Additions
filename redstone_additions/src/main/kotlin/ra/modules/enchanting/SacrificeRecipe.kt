package ra.modules.enchanting

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.core.Storage
import ra.core.raw

/**
 * One sacrifice recipe inside an addon's `enchant_recipes` function (see [EnchantingModule]):
 * when `storage ra:enchant input` matches [inputFilter] (an SNBT compound filter such as
 * `{id:"minecraft:stone"}`), produce [resultStack] (an SNBT item stack) with [chance] percent.
 *
 * The first recipe that matches wins, across every list in `#ra_enchanting:recipes`:
 * `function #tag` runs EVERY list, and a list's `return 1` only ends that list, so each recipe
 * first steps aside when an earlier one (in this list or an earlier list) already wrote `result`.
 * Without that guard, the chance line below fired on any result, its own or an earlier list's,
 * and the last list in the tag (ra_interactive, 1%) decided the chance of every sacrifice.
 *
 * End the list function with `returnValue(0)`.
 *
 * [sacrificeName] names an input matched on more than its id (custom_data): it is written as a
 * `# Sacrifice: <name>` comment above the recipe, which is where tools/recipe_render/atlas.py
 * reads it for the Recipe Atlas. Inputs matched by id alone are named from the id.
 *
 * [won] and [lost] are optional messages for the player who threw the item (its `Thrower`, reached
 * with `execute on origin`), sent on a hit and on a miss. Plain text without double quotes. A rare
 * result is worth telling the player about; a 1% core rolled every second is not.
 */
fun Function.sacrificeRecipe(
	inputFilter: String,
	resultStack: String,
	chance: Int,
	sacrificeName: String? = null,
	won: String? = null,
	lost: String? = null,
) {
	require(won?.contains('"') != true && lost?.contains('"') != true) { "sacrifice messages must not contain double quotes" }
	raw("execute if data storage ra:enchant result run return 0")
	// Directly above the matching line: the atlas reads the comment just above it.
	if (sacrificeName != null) comment("Sacrifice: $sacrificeName")
	raw("execute if data storage ra:enchant input$inputFilter run data modify storage ra:enchant result set value $resultStack")
	execute {
		ifCondition { data(Storage.ENCHANT, "result") }
		run { data(Storage.ENCHANT) { modify("chance") { set(chance) } } }
	}
	if (won != null) raw("execute if data storage ra:enchant result run data modify storage ra:enchant won set value \"$won\"")
	if (lost != null) raw("execute if data storage ra:enchant result run data modify storage ra:enchant lost set value \"$lost\"")
	execute {
		ifCondition { data(Storage.ENCHANT, "result") }
		run { returnValue(1) }
	}
}
