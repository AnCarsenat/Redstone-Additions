package ra.modules.interactive

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.Tags
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.generated.arguments.types.RecipeArgument
import ra.core.Obj
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.markers
import ra.core.define
import ra.core.raw
import ra.modules.infinite.ItemSpec

/**
 * What the ra_interactive and ra_storage blocks share: the give / drop item, the pieces of their
 * `on_break`, the placement guard, the recipe + unlock pair, and the double-chest partner table.
 *
 * Their break handling differs from block to block in which steps it has (a container dump, a
 * skin to clear, a network to leave, a quieter sound) and in their order, so `on_break` is
 * composed from the steps below in each block's own file rather than hidden behind one helper.
 */

/** `ns:blocks/<dir>/give`: the block's full item, lore included. */
fun DataPack.specGive(block: CustomBlock, item: ItemSpec) = define(block.give) { raw("give @s ${item.giveArg()}") }

/**
 * The custom item, summoned at `~ ~ ~`. Without `ra.drop_temp` unless [dropTemp]: blocks that
 * spill their container overwrite the first `ra.drop_temp` item they find, so a tagged custom
 * drop lying nearby could be replaced by a stack of cobblestone.
 */
fun Function.dropSpec(item: ItemSpec, dropTemp: Boolean = false) {
	val tags = if (dropTemp) """,Tags:["${Tag.RA}","${Tag.DROP_TEMP}"]""" else ""
	raw("summon item ~ ~ ~ {Item:${item.stackSnbt()}$tags}")
}

/** Break sound for everyone in 16 blocks, or only those who asked for block sounds ([listenersOnly]). */
fun Function.breakSound(sound: String = "minecraft:block.stone.break", listenersOnly: Boolean = false) {
	val who = if (listenersOnly) "@a[distance=..16,scores={${Obj.USER_SOUND}=1..}]" else "@a[distance=..16]"
	raw("playsound $sound block $who ~ ~ ~ 1 1")
}

/** The puff of cloud every broken block leaves. */
fun Function.breakCloud() = raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5")

/**
 * Spills the container's FIRST stack (Items[0]) half a block up. Only the first: this is the
 * written-out form the beet blocks used, and the Magic Crate's recursive `drop_all` is the one
 * that empties a whole container.
 */
fun Function.dropFirstSlot() {
	raw("execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:[\"${Tag.RA}\",\"${Tag.DROP_TEMP}\"]}")
	raw("execute if entity @e[type=item,tag=${Tag.DROP_TEMP},limit=1] run data modify entity @e[type=item,tag=${Tag.DROP_TEMP},limit=1] Item set from block ~ ~ ~ Items[0]")
	raw("execute as @e[type=item,tag=${Tag.DROP_TEMP}] run tag @s remove ${Tag.DROP_TEMP}")
}

/**
 * `execute <chain> run function <fn>[ <args>]`. Most of these blocks' calls sit behind a chain of
 * block-state, item and NBT-selector tests that the typed `execute { }` builder writes three
 * times longer than vanilla (the same trade the tools made, see ra.tools.ToolsDsl). The chain
 * stays vanilla text; the called function stays a checked value, never a string.
 * [args] is `{k:v}` or `with storage ra:x path`.
 */
fun Function.executeRun(chain: String, fn: FunctionArgument, args: String = "") =
	raw("execute ${if (chain.isEmpty()) "" else "$chain "}run function ${fn.asId()}${if (args.isEmpty()) "" else " $args"}")

/** `execute [<chain>] store result score <holder> ra.temp run function <fn>[ <args>]`. */
fun Function.executeStoreRun(chain: String, holder: String, fn: FunctionArgument, args: String = "") =
	executeRun("${if (chain.isEmpty()) "" else "$chain "}store result score $holder ${Obj.TEMP}", fn, args)

/** `function <fn> with <source>`: `storage ra:temp place_item`, `entity @s data.properties`... */
fun Function.callWith(fn: FunctionArgument, source: String) = raw("function ${fn.asId()} with $source")

/** First line of every handler: not this block's bat -> `return 0`. */
fun Function.placeGuard(block: CustomBlock) = raw("execute unless entity @s[tag=${block.placeTag}] run return 0")

/**
 * Every marker of [block] standing in air runs [onBreak]. The cauldrons test for air rather than
 * "not my block": a cauldron is emptied by a bucket into a plain `cauldron`, which is still the
 * block, only waiting for its refill.
 */
fun Function.airBreakCheck(block: CustomBlock, onBreak: FunctionArgument) = execute {
	asTarget(markers(block))
	at(self())
	ifCondition { block(vec3(), Tags.Block.AIR) }
	run { function(onBreak) }
}

/** `execute as <every marker of block> at @s run function <fn>`. */
fun Function.forEachMarker(block: CustomBlock, fn: FunctionArgument) = execute {
	asTarget(markers(block))
	at(self())
	run { function(fn) }
}

/** The hopper-rate gate: a missing score starts at 0, then the block acts every 4th tick. */
fun Function.hopperCooldown() {
	// A score that has never been set reads as ABSENT, and every comparison against an absent
	// score is false. `matches -2147483648..` is true for any value at all, so `unless` on it is
	// the test for "no score yet".
	raw("execute unless score @s ${Obj.COOLDOWN} matches -2147483648.. run scoreboard players set @s ${Obj.COOLDOWN} 0")
	raw("scoreboard players add @s ${Obj.COOLDOWN} 1")
	raw("execute if score @s ${Obj.COOLDOWN} matches ..3 run return 0")
}

/**
 * The shaped recipe `ns:<name>` and, when [unlock] is given, the vanilla-shaped advancement
 * `ns:recipes/unlock_<name>`. The unlock criterion keeps the name each beet file gave it
 * (`has_lava` for a lava bucket...): a criterion name is how an advancement is granted or revoked
 * by command, so it is part of the contract.
 */
fun DataPack.shapedRecipe(
	ns: String,
	name: String,
	result: ItemSpec,
	resultItem: ItemArgument = Items.BAT_SPAWN_EGG,
	pattern: List<String>,
	keys: List<Pair<String, List<ItemArgument>>>,
	category: RecipeCategory = RecipeCategory.REDSTONE,
	count: Short? = null,
	notify: Boolean = true,
	unlock: Pair<String, ItemArgument>? = null,
): RecipeArgument {
	val recipe = recipesBuilder.craftingShaped(name, ns) {
		this.category = category
		pattern(*pattern.toTypedArray())
		for ((k, items) in keys) key(k, items)
		result(resultItem, count) { result.applyTo(this) }
		if (notify) showNotification = true
	}
	if (unlock != null) {
		val (trigger, unlockItem) = unlock
		advancement("recipes/unlock_$name") {
			namespace = ns
			parent = AdvancementArgument("recipes/root", "minecraft")
			criteria {
				recipeUnlocked("has_the_recipe", recipe)
				inventoryChanged(trigger) { item { items = listOf(unlockItem) } }
			}
			requirements(listOf("has_the_recipe", trigger))
			rewards { recipes(recipe) }
		}
	}
	return recipe
}

/** `has_<item path>`, the criterion name most unlock files used. */
fun has(item: ItemArgument) = "has_${item.asId().substringAfter(':')}" to item

/**
 * One half of a double chest: when the block here is [block] facing [facing] with [type], its
 * other half is at [offset]. A double chest is two blocks that each know only their own side, so
 * which half is which depends on facing AND type.
 */
data class ChestHalf(val block: String, val facing: String, val type: String, val offset: String) {
	/** `if block ~ ~ ~ <this half> positioned <the other half>`, an execute fragment. */
	val toPartner get() = "if block ~ ~ ~ minecraft:$block[facing=$facing,type=$type] positioned $offset"
}

/** Every chest half, chests first then trapped chests, in the order the beet files tested them. */
val CHEST_HALVES: List<ChestHalf> = listOf("chest", "trapped_chest").flatMap { b ->
	listOf(
		ChestHalf(b, "north", "left", "~1 ~ ~"),
		ChestHalf(b, "north", "right", "~-1 ~ ~"),
		ChestHalf(b, "south", "left", "~-1 ~ ~"),
		ChestHalf(b, "south", "right", "~1 ~ ~"),
		ChestHalf(b, "east", "left", "~ ~ ~1"),
		ChestHalf(b, "east", "right", "~ ~ ~-1"),
		ChestHalf(b, "west", "left", "~ ~ ~-1"),
		ChestHalf(b, "west", "right", "~ ~ ~1"),
	)
}
