package ra.core.block

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.give
import ra.core.define

/**
 * The functions and resources every placeable block has, generated from its [CustomBlock].
 * A block's own file adds only what is particular to it: what happens on placement, what it
 * does each tick and what it shows on the goggles.
 *
 * Grouped by concern: breaking in `BlockBreak.kt` ([blockTick], [blockOnBreak]), placement in
 * `Placement.kt`, recipes in `Recipes.kt` ([blockRecipe]), goggles in `GogglesLines.kt`, the
 * debug line in `ra.core.Messages` ([ra.core.blockRegisterMessage]).
 */

/** `ns:blocks/<dir>/give`: gives the block's item to `@s`. */
fun DataPack.blockGive(block: CustomBlock) = define(block.give) {
	give(self(), block.item)
}
