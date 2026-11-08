package ra.modules.wireless

import ra.core.TextFormat
import ra.core.text
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.gogglesHeader
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
import ra.core.block.finishPlacement
import ra.core.Tag
import ra.core.block.blockTick
import ra.core.block.BreakTest
import ra.core.block.forEachMarker

/**
 * Wireless Emitter: transmits its redstone input to every receiver on the same `channel` string.
 * It only tags itself `ra.transmitting`; receivers look for transmitting emitters on their channel.
 *
 * No on/off switch: its redstone input and its channel are the controls. Until v6.0.1 it also had an
 * `enabled` property the wrench toggled, invisible on the block, which left an emitter silently
 * dead. Markers placed before then may still carry `enabled`; nothing reads it, and the schema
 * declares it hidden and read-only so the Data Handler does not list it as undeclared.
 */
object Emitter {
	val block = CustomBlock(Ns.WIRELESS, "emitter", "Wireless Emitter", model = "minecraft:end_stone_bricks")
	val item = block.loreItem {}

	/** On a powered emitter. Receivers match against it. */
	const val TRANSMITTING = "ra.transmitting"

	private val process = block.fn("process")

	fun Ra.registerEmitter() {
		block(block)
		properties(block) {
			// Compared as a value, never pasted into a command.
			text("channel", TextFormat.NAME)
			// Retired in v6.0.1, still on markers placed before it. Nothing reads it.
			bool("enabled", readonly = true, visible = false)
		}
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
			// Default channel (a string).
			setOnNew(block, "data.properties.channel", "\"default\"")
			// Remove ra.new and the placement tag.
			finishPlacement(block)
		}

		// As the marker, at its position.
		pack.define(process) {
			defaultChannel()

			// Detect redstone state from all supported sources.
			function(RaLib.Redstone.detectSwitch)

			// If powered, tag as transmitting.
			raw("execute if entity @s[tag=${Tag.POWERED}] run tag @s add $TRANSMITTING")
			raw("execute unless entity @s[tag=${Tag.POWERED}] run tag @s remove $TRANSMITTING")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("channel", "Channel: ", "aqua", y = 0.8)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		// Break detection: the emitter is end stone bricks, anything else means it is gone.
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.END_STONE_BRICKS)) { forEachMarker(block, process) }
	}
}
