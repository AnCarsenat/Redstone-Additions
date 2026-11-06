package ra.core.block

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.generated.arguments.types.RecipeArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.give
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.generated.Tags
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.summon
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.Tag
import ra.core.define
import ra.core.raw

/**
 * The functions and resources every placeable block has, generated from its [CustomBlock].
 * A block's own file adds only what is particular to it: what happens on placement, what it
 * does each tick and what it shows on the goggles.
 */

/** `ns:blocks/<dir>/give`: gives the block's item to `@s`. */
fun DataPack.blockGive(block: CustomBlock) = define(block.give) {
	give(self(), block.item)
}

/**
 * Drops the block's item at `~ ~ ~`, tagged `ra.drop_temp`. Run at the marker when the block is
 * broken, so the player gets back exactly the item they placed.
 */
fun Function.dropBlockItem(block: CustomBlock) {
	summon(EntityTypes.ITEM, vec3(), nbt {
		this["Item"] = block.itemStackNbt()
		this["Tags"] = NbtList(listOf(NbtString(Tag.RA), NbtString(Tag.DROP_TEMP)))
	})
}

/**
 * Removes the vanilla item the physical block dropped when it was broken, so only the custom item
 * from [dropBlockItem] is left. Nearest one within 2 blocks: the break happened this tick, here.
 */
fun Function.killVanillaDrop(itemId: String) {
	raw("kill @e[type=item,nbt={Item:{id:\"$itemId\"}},distance=..2,limit=1,sort=nearest]")
}

/**
 * The crafting recipe `ns:<id>` producing the block's item, plus the advancement
 * `ns:recipes/unlock_<id>` that teaches it once the player holds [unlockedBy] (or already knows
 * it, the vanilla convention).
 *
 * [unlockedBy] is the ingredient that makes the recipe discoverable, `minecraft:` id.
 */
fun DataPack.blockRecipe(
	block: CustomBlock,
	unlockedBy: ItemArgument,
	category: RecipeCategory? = RecipeCategory.REDSTONE,
	shape: CraftingShaped.() -> Unit,
): RecipeArgument {
	val recipe = recipesBuilder.craftingShaped(block.id, block.ns.id) {
		this.category = category
		showNotification = true
		shape()
		result(Items.BAT_SPAWN_EGG, 1) { with(block) { blockItemComponents() } }
	}
	recipeUnlock(block.id, block.ns.id, recipe, unlockedBy)
	return recipe
}

/** `ns:recipes/unlock_<name>`, the vanilla-shaped recipe advancement for any recipe. */
fun DataPack.recipeUnlock(name: String, namespace: String, recipe: RecipeArgument, unlockedBy: ItemArgument): AdvancementArgument {
	val trigger = "has_${unlockedBy.asId().substringAfter(':')}"
	return advancement("recipes/unlock_$name") {
		this.namespace = namespace
		parent = AdvancementArgument("recipes/root", "minecraft")
		criteria {
			recipeUnlocked("has_the_recipe", recipe)
			inventoryChanged(trigger) {
				item { items = listOf(unlockedBy) }
			}
		}
		requirements(listOf("has_the_recipe", trigger))
		rewards { recipes(recipe) }
	}
}

/**
 * `ns:blocks/<dir>/on_break`, run as a broken block's marker at its position: [cleanup] (the
 * block's own displays, outputs, stored items), then the vanilla drop is swapped for the custom
 * item and the marker removed.
 */
fun DataPack.blockOnBreak(block: CustomBlock, cleanup: Function.() -> Unit = {}) = define(block.fn("on_break")) {
	cleanup()
	killVanillaDrop(block.physical)
	dropBlockItem(block)
	kill(self())
}

/**
 * `ns:blocks/<dir>/tick`, the per-type tick: every marker whose block is gone runs [onBreak],
 * then [body] runs (typically `execute as <markers> at @s run function <process>`).
 *
 * The beet version spelled break handling as one `execute as @e[tag=ra.broken,...]` line per
 * cleanup step. Routing each broken marker through one `on_break` function does the same work
 * once per marker and keeps the cleanup in one readable place.
 */
fun DataPack.blockTick(block: CustomBlock, onBreak: FunctionArgument, body: Function.() -> Unit) = define(block.tick) {
	execute {
		asTarget(markers(block))
		at(self())
		ifCondition { block(vec3(), Tags.Block.AIR) }
		run { function(onBreak) }
	}
	body()
}
