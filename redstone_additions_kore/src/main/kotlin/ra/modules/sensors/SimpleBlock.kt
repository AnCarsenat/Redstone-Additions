package ra.modules.sensors

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.fill
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.generated.arguments.types.RecipeArgument
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Ns
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.killVanillaDrop
import ra.core.define
import ra.core.raw
import ra.modules.infinite.ItemSpec

/**
 * The block lifecycle as `ra_sensors`, `ra_wireless` and `ra_ender` wrote it, shared by the three.
 *
 * These blocks keep the beet placement shape, which differs from the core helpers in two ways that
 * matter at runtime: their handlers have no "a marker is already here" guard, and their break drop
 * is summoned WITHOUT `ra.drop_temp` (some interactive blocks overwrite the first `ra.drop_temp`
 * item they find with their container contents, so a tagged drop lying nearby could be replaced).
 *
 * Function calls stay values: [exec] takes the called function as a [FunctionArgument] and only
 * the execute chain as vanilla text, where the typed builder would be several times longer.
 */

/** `execute <chain> run function <fn>[ <args>]`. [args] is `{k:v}` or `with storage <id> [path]`. */
fun Function.exec(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute $chain run function ${fn.asId()}${if (args.isEmpty()) "" else " $args"}")

/** `execute <chain> run return run function <fn>[ <args>]`. */
fun Function.execReturn(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute $chain run return run function ${fn.asId()}${if (args.isEmpty()) "" else " $args"}")

/** `return run function <fn>`. */
fun Function.returnRunFn(fn: FunctionArgument) = raw("return run function ${fn.asId()}")

/** `@e[type=marker,tag=<block tag>]` as text, for chains written as vanilla text. */
fun CustomBlock.sel(vararg extra: String) =
	"@e[type=marker,tag=$tag${extra.joinToString("") { ",$it" }}]"

/** The fresh marker, as the beet handlers spelled it. */
fun CustomBlock.newSel() = "@e[type=marker,tag=$tag,tag=${Tag.NEW},distance=..0.5,sort=nearest,limit=1]"

/** First line of every handler: not our bat -> return 0. */
fun Function.batGuard(block: CustomBlock) = raw("execute unless entity @s[tag=${block.placeTag}] run return 0")

/** `data modify entity <fresh marker> <path> set value <snbt>`. */
fun Function.setOnNew(block: CustomBlock, path: String, snbt: String) =
	raw("data modify entity ${block.newSel()} $path set value $snbt")

/** Drops `ra.new` from the fresh marker (every marker of the type within 0.5). */
fun Function.clearNew(block: CustomBlock) = tag(allEntities {
	type = EntityTypes.MARKER
	tag = block.tag
	tag = Tag.NEW
	distance = rangeOrDoubleEnd(0.5)
}) { remove(Tag.NEW) }

/** Every marker of the type missing `data.properties.<key>` gets [snbt]. */
fun Function.defaultProp(block: CustomBlock, key: String, snbt: String) =
	raw("execute as ${block.sel()} unless data entity @s data.properties.$key run data modify entity @s data.properties.$key set value $snbt")

/** The 3x3x3 output shell: iron blocks become redstone blocks ([on]) or back. Run `at` the block. */
fun Function.shell(on: Boolean) = if (on)
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.REDSTONE_BLOCK, Blocks.IRON_BLOCK)
else
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.IRON_BLOCK, Blocks.REDSTONE_BLOCK)

/**
 * The shaped recipe `ns:<name>` producing [result], and its `ns:recipes/unlock_<name>` advancement
 * (criterion [trigger], granted once the player holds [unlockedBy]). [count] null leaves the
 * count out of the JSON, as the sensors' recipes had it (vanilla defaults to 1).
 */
fun DataPack.shapedRecipe(
	ns: Ns,
	name: String,
	result: ItemSpec,
	unlockedBy: ItemArgument,
	resultItem: ItemArgument = Items.BAT_SPAWN_EGG,
	category: RecipeCategory = RecipeCategory.REDSTONE,
	count: Short? = 1,
	trigger: String = "has_${unlockedBy.asId().substringAfter(':')}",
	shape: CraftingShaped.() -> Unit,
): RecipeArgument {
	val recipe = recipesBuilder.craftingShaped(name, ns.id) {
		this.category = category
		shape()
		result(resultItem, count) { result.applyTo(this) }
		showNotification = true
	}
	advancement("recipes/unlock_$name") {
		namespace = ns.id
		parent = AdvancementArgument("recipes/root", "minecraft")
		criteria {
			recipeUnlocked("has_the_recipe", recipe)
			inventoryChanged(trigger) { item { items = listOf(unlockedBy) } }
		}
		requirements(listOf("has_the_recipe", trigger))
		rewards { recipes(recipe) }
	}
	return recipe
}

/**
 * `ns:blocks/<dir>/on_break`, as the broken marker at its block: [cleanup], the vanilla drop
 * swapped for [drop] ([afterKill] runs between the two), the stone break sound, a puff of cloud
 * when [particle], then the marker is removed.
 */
fun DataPack.plainOnBreak(
	block: CustomBlock,
	drop: ItemSpec,
	particle: Boolean,
	cleanup: Function.() -> Unit = {},
	afterKill: Function.() -> Unit = {},
): FunctionArgument = define(block.fn("on_break")) {
	cleanup()
	killVanillaDrop(block.physical)
	afterKill()
	raw("summon item ~ ~ ~ {Item:${drop.stackSnbt()}}")
	raw("playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1")
	if (particle) raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5")
	kill(self())
}
