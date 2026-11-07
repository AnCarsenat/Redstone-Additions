package ra.modules.chunkloader

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.recipeUnlock
import ra.core.block.textLine
import ra.core.define
import ra.core.raw
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage

/**
 * Chunk Loader: force-loads its own chunk while it is powered.
 *
 * A lodestone. The chunk is added on the rising edge of power and removed on the falling edge
 * (or when the block is broken); `ra.was_powered` remembers last tick's state to find the edges.
 */
object ChunkLoader {
	val block = CustomBlock(Ns.CHUNK_LOADER, "chunk_loader", "Chunk Loader", model = "minecraft:lodestone")

	val item = block.loreItem { lore(loreLine("Loads its chunk while powered", "gray")) }

	private const val WAS_POWERED = "ra.was_powered"

	val registerBlock = block.fn("register_block")
	private val onUnpowered = block.fn("on_unpowered")

	fun Ra.registerChunkLoader() {
		block(block)
		pack.blockRegisterMessage(block)

		pack.define(block.give) { raw("give @s ${item.giveArg()}") }

		val recipe = pack.recipesBuilder.craftingShaped(block.id, block.ns.id) {
			category = RecipeCategory.REDSTONE
			pattern("ISI", "RLR", "IRI")
			key("I", Items.IRON_INGOT)
			key("R", Items.REDSTONE)
			key("L", Items.LODESTONE)
			key("S", Items.NETHER_STAR)
			result(Items.BAT_SPAWN_EGG, 1) { item.applyTo(this) }
			showNotification = true
		}
		pack.recipeUnlock(block.id, block.ns.id, recipe, Items.LODESTONE)

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) { goggles() }

		// Remove force-load from the chunk containing this marker. As the marker, at its position.
		pack.define(onUnpowered) { raw("execute at @s run forceload remove ~ ~") }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true) { function(onUnpowered) }

		pack.define(block.tick) {
			breakCheck(block, Blocks.LODESTONE, onBreak, skipNew = true)

			// Redstone power detection
			execute {
				asTarget(markers(skipNew = true))
				at(self())
				run { function(RaLib.Redstone.detectSwitch) }
			}

			// Load chunk on rising edge
			raw("execute as @e[type=marker,tag=${block.tag},tag=!$WAS_POWERED,tag=${Tag.POWERED}] at @s run execute at @s run forceload add ~ ~")

			// Unload chunk on falling edge
			execute {
				asTarget(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = WAS_POWERED; tag = "!${Tag.POWERED}" })
				at(self())
				run { function(onUnpowered) }
			}

			// Track power state
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = Tag.POWERED }) { add(WAS_POWERED) }
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = "!${Tag.POWERED}" }) { remove(WAS_POWERED) }
		}
	}

	private fun markers(skipNew: Boolean) = allEntities {
		type = EntityTypes.MARKER
		tag = block.tag
		if (skipNew) tag = "!${Tag.NEW}"
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		raw("execute unless entity @s[tag=${block.placeTag}] run return 0")
		// Lodestone with a marker (dir_type 0 = no direction).
		placeBlock(block)
		finishPlacement(block, removePlaceTag = false, withinBlock = false)
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.0)
		execute {
			ifCondition { entity(self { tag = Tag.POWERED }) }
			run { textLine("State: ", "ON", "green", y = 0.8) }
		}
		execute {
			unlessCondition { entity(self { tag = Tag.POWERED }) }
			run { textLine("State: ", "OFF", "red", y = 0.8) }
		}
	}
}
