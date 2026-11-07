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
 * The first list that writes `result` wins, so the function returns as soon as one matched; end
 * the function with `returnValue(0)`.
 */
fun Function.sacrificeRecipe(inputFilter: String, resultStack: String, chance: Int) {
	raw("execute if data storage ra:enchant input$inputFilter run data modify storage ra:enchant result set value $resultStack")
	execute {
		ifCondition { data(Storage.ENCHANT, "result") }
		run { data(Storage.ENCHANT) { modify("chance") { set(chance) } } }
	}
	execute {
		ifCondition { data(Storage.ENCHANT, "result") }
		run { returnValue(1) }
	}
}
