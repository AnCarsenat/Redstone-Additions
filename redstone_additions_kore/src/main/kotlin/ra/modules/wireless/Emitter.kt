package ra.modules.wireless

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Ra
import ra.core.TagRoute
import ra.core.block.CustomBlock
import ra.core.block.blockGive
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
import ra.core.block.breakCheck
import ra.core.blockRegisterMessage
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.clearNewTag

/**
 * Wireless Emitter: transmits its redstone input to every receiver on the same `channel` string.
 * It only tags itself `ra.transmitting`; receivers look for transmitting emitters on their channel.
 * The wrench (plain RMB) toggles `enabled`.
 */
object Emitter {
	val block = CustomBlock(Ns.WIRELESS, "emitter", "Wireless Emitter", model = "minecraft:end_stone_bricks")
	val item = block.loreItem {}

	/** On an enabled, powered emitter. Receivers match against it. */
	const val TRANSMITTING = "ra.transmitting"

	private val process = block.fn("process")
	val toggle = block.fn("toggle")
	val registerBlock = block.fn("register_block")

	fun Ra.registerEmitter() {
		block(block)
		wrenchToggles += TagRoute(block.tag, toggle)
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.END_STONE_BRICKS)) {
			pattern("GEG", "ERE", "GEG")
			key("G", Items.GOLD_INGOT)
			key("E", Items.END_STONE_BRICKS)
			key("R", Items.REDSTONE)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// End stone bricks with a marker.
			placeBlock(block)
			// Default channel (a string) and enabled state.
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.enabled", "1b")
			clearNewTag(block)
			// Remove the placement tag.
			tag(self()) { remove(block.placeTag) }
			returnValue(1)
		}

		// As the marker, at its position.
		pack.define(process) {
			defaultsChannelEnabled()

			// Detect redstone state from all supported sources.
			function(RaLib.Redstone.detectSwitch)

			// Only transmit if enabled.
			raw("execute unless data entity @s data.properties{enabled:1b} run return 0")

			// If powered, tag as transmitting.
			raw("execute if entity @s[tag=ra.powered] run tag @s add $TRANSMITTING")
			raw("execute unless entity @s[tag=ra.powered] run tag @s remove $TRANSMITTING")
		}

		// Toggle enabled (called by the wrench, as the marker).
		pack.define(toggle) { toggleEnabled("Emitter", "yellow") }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("channel", "Channel: ", "aqua", y = 0.8)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.define(block.tick) {
			// Break detection: the emitter is end stone bricks, anything else means it is gone.
			breakCheck(block, Blocks.END_STONE_BRICKS, onBreak)
			execute {
				asTarget(markers(block))
				at(self())
				run { function(process) }
			}
		}
	}
}
