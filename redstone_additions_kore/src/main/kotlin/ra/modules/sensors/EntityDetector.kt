package ra.modules.sensors

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockTick
import ra.core.block.gogglesHeader
import ra.core.block.markers
import ra.core.block.placeBlock
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.block.shapedRecipe
import ra.core.block.recipeResult
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.STONE_BREAK_SOUND
import ra.core.blockRegisterMessage
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.clearNewTag
import ra.core.block.outputShell

/**
 * Entity Detector: powers its shell while any entity matches its `entity_selector` property.
 * Properties: entity_selector (default `@e[type=minecraft:pig,distance=..5]`).
 */
object EntityDetector {
	val block = CustomBlock(Ns.SENSORS, "entity_detector", "Entity Detector", model = "minecraft:waxed_chiseled_copper")
	val item = block.loreItem {}

	/** Set on the detector while its selector runs, so matches can score it. */
	private const val CHECKING = "ra.checking"

	private val detect = block.fn("detect")
	private val detectMacro = block.fn("detect_macro")
	val registerBlock = block.fn("register_block")

	fun Ra.registerEntityDetector() {
		block(block)
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.SCULK_SENSOR), count = null) {
			pattern("CCC", "ROR", "CCC")
			key("C", Items.COBBLESTONE)
			key("R", Items.REDSTONE)
			key("O", Items.OBSERVER)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Waxed chiseled copper with a marker (dir_type 0 = non-directional).
			placeBlock(block)
			// Initialize default properties first.
			setOnNew(block, "data.properties", """{entity_selector:"@e[type=minecraft:pig,distance=..5]"}""")
			// Remove ra.new now that setup is complete.
			clearNewTag(block)
			returnValue(1)
		}

		// Detect entities in range. As the marker, at its position: the selector is stored for the macro.
		pack.define(detect) {
			raw("data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			function(detectMacro, Storage.TEMP)
		}

		// Macro {entity_selector}. As the marker, at its position.
		pack.define(detectMacro) {
			// Tag self to reference later, reset score.
			tag(self()) { add(CHECKING) }
			scoreboard.players.set(self(), Obj.TEMP, 0)

			// Find matching entities, exclude custom blocks, set the score on the checking marker.
			raw("\$execute at @s as \$(entity_selector) unless entity @s[tag=ra.custom_block] run scoreboard players set @e[type=marker,tag=$CHECKING,limit=1] ${Obj.TEMP} 1")

			tag(self()) { remove(CHECKING) }

			// Output redstone signal based on detection.
			outputShell(on = false)
			raw("execute if score @s ${Obj.TEMP} matches 1 at @s run return run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		}

		pack.define(block.goggles) { goggles() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) {
			// Check for entities in range.
			execute {
				asTarget(markers(block))
				at(self())
				run { function(detect) }
			}
		}
	}

	/** Goggles readout; also the block's display name (see ra:tools/block_name). */
	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.0)
		propLine("entity_selector", "Target: ", "aqua", y = 0.8)
	}
}
