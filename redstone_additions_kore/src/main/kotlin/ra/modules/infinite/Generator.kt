package ra.modules.infinite

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDouble
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShapeless
import io.github.ayfri.kore.features.recipes.types.ingredient
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.markers
import ra.core.block.newMarker
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw

/**
 * What every Infinite generator shares: a real DROPPER, the item, the shapeless casing + core
 * recipe, placement, the per-type tick and the cooldown gate at the top of `process`.
 *
 * A real dropper, with no block_display skin over it. A generator needs a `facing` state so you
 * can see where it will grow, and a dropper's front is unmistakable. It was a skinned barrel until
 * a skin's own darkness gave it away: a block_display samples the light inside the opaque block it
 * covers, which is zero, so every skin rendered black.
 *
 * Ejection is not a concern the way it is for the storage blocks: a generator never puts anything
 * in its own inventory and never reads redstone.
 */
class Generator(
	id: String,
	name: String,
	val core: InfiniteItems.Core,
	/** `data.properties.cooldown` a new (or pre-cooldown) generator gets. */
	val defaultCooldown: Int,
	val loreLines: List<SComp>,
) {
	val block = CustomBlock(Ns.INFINITE, id, name, model = "minecraft:dropper", dirType = 2)

	val item = block.loreItem {
		rarity("epic")
		glint()
		lore(*loreLines.toTypedArray())
	}

	val process = block.fn("process")
	val registerBlock = block.fn("register_block")

	/** Registers everything but `process` and `goggles`, which are the generator's own. [extraTick] runs after the cooldown default. */
	fun Ra.registerGenerator(extraTick: Function.() -> Unit = {}) {
		block(block)
		readonly(block.id, "cooldown")
		pack.registerBlockMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }

		val recipe = pack.recipesBuilder.craftingShapeless(block.id, block.ns.id) {
			category = RecipeCategory.REDSTONE
			ingredient(Items.REPEATING_COMMAND_BLOCK)
			ingredient(core.base)
			result(Items.BAT_SPAWN_EGG, 1) { item.applyTo(this) }
			showNotification = true
		}

		// Unlocked by holding the core, matched on its custom data: the base item alone is a
		// command block anyone in creative has.
		pack.advancement("recipes/unlock_${block.id}") {
			namespace = block.ns.id
			parent = AdvancementArgument("recipes/root", "minecraft")
			criteria {
				recipeUnlocked("has_the_recipe", recipe)
				inventoryChanged("has_core") {
					item {
						items = listOf(core.base)
						predicates { customData { this["ra"] = nbt { this[core.key] = true } } }
					}
				}
			}
			requirements(listOf("has_the_recipe", "has_core"))
			rewards { recipes(recipe) }
		}

		pack.define(block.handlePlacement) {
			raw("execute unless entity @s[tag=${block.placeTag}] run return 0")
			placeBlock(block)
			data(newMarker(block)) { modify("data.properties.cooldown") { set(defaultCooldown) } }
			finishLorePlacement(block)
		}

		val onBreak = pack.loreBlockOnBreak(block, item)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)

			// The property is the authority for the period; the score is the countdown.
			raw("execute as @e[type=marker,tag=${block.tag}] unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value $defaultCooldown")
			extraTick()

			// One block, one marker. A placement that ran twice -- a reload mid-place, or a
			// duplicate bat -- leaves two markers stacked on the same block, which ticks the
			// generator twice and draws the goggles readout twice on top of itself.
			execute {
				asTarget(markers(block))
				at(self())
				run {
					kill(allEntities(limitToOne = true) {
						type = EntityTypes.MARKER
						tag = block.tag
						distance = rangeOrDouble(0.01, 0.9)
						sort = Sort.NEAREST
					})
				}
			}

			scoreboard.players.add(markers(block), Obj.COOLDOWN, 1)
			execute {
				asTarget(markers(block))
				at(self())
				run { function(process) }
			}
		}
	}
}

/**
 * Wait out the configured period. A missing cooldown used to read as zero here, and zero means
 * "long enough" to the comparison below -- the generator then ran every tick.
 */
fun Function.cooldownGate() {
	function(RaLib.Util.property, nbt { this["name"] = "cooldown"; this["default"] = 100; this["min"] = 1 })
	raw("execute unless score @s ${Obj.COOLDOWN} >= #prop ${Obj.TEMP} run return 0")
	scoreboard.players.set(self(), Obj.COOLDOWN, 0)
}

/**
 * Rolls `#gen.roll ra.temp` in 1..total and sets the block in front to the matching entry of
 * [table] (weight, block), weights read as consecutive ranges.
 */
fun Function.rollTable(table: List<Pair<Int, BlockArgument>>, at: String = "^ ^ ^1") {
	val total = table.sumOf { it.first }
	raw("execute store result score #gen.roll ${Obj.TEMP} run random value 1..$total")
	var from = 1
	for ((weight, block) in table) {
		val to = from + weight - 1
		val range = if (from == to) "$from" else "$from..$to"
		raw("execute if score #gen.roll ${Obj.TEMP} matches $range run setblock $at ${block.asId()}")
		from = to + 1
	}
}

/** Reference to the growable tag, used by every generator and the debug report. */
const val GROWABLE = "#ra_infinite:growable"
const val FLOWER_GROUND = "#ra_infinite:flower_ground"
