package ra.modules.wireless

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
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
import ra.modules.infinite.breakCheck
import ra.modules.infinite.loreItem
import ra.modules.infinite.registerBlockMessage
import ra.modules.sensors.batGuard
import ra.modules.sensors.clearNew
import ra.modules.sensors.exec
import ra.modules.sensors.plainOnBreak
import ra.modules.sensors.setOnNew
import ra.modules.sensors.shapedRecipe
import ra.modules.sensors.shell

/**
 * Wireless Receiver: powers its shell while an emitter on its `channel` string transmits, or for a
 * few ticks after a Redstone Remote pulses its channel. The wrench (plain RMB) toggles `enabled`.
 */
object Receiver {
	val block = CustomBlock(Ns.WIRELESS, "receiver", "Wireless Receiver", model = "minecraft:purpur_block")
	val item = block.loreItem {}

	/** Pulse from a remote in progress; `ra.pulse_timer` counts it down. */
	const val PULSING = "ra.pulsing"
	const val PULSE_TIMER = "ra.pulse_timer"

	/** On the receiver while it scans emitters, so a matching emitter can tag it [RECEIVING]. */
	private const val CHECKING = "ra.checking"
	private const val RECEIVING = "ra.receiving"

	private val process = block.fn("process")
	private val checkEmitterChannel = block.fn("check_emitter_channel")
	val toggle = block.fn("toggle")
	val registerBlock = block.fn("register_block")

	fun Ra.registerReceiver() {
		block(block)
		wrenchToggles += TagRoute(block.tag, toggle)
		pack.registerBlockMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item, unlockedBy = Items.PURPUR_BLOCK) {
			pattern("GPG", "PRP", "GPG")
			key("G", Items.GOLD_INGOT)
			key("P", Items.PURPUR_BLOCK)
			key("R", Items.REDSTONE)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			batGuard(block)
			// Purpur block with a marker.
			placeBlock(block)
			// Default channel (a string) and enabled state.
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.enabled", "1b")
			clearNew(block)
			raw("tag @s remove ${block.placeTag}")
			raw("return 1")
		}

		pack.define(process) { process() }

		// As an emitter: does its channel match the receiver's (in storage ra:temp receiver_channel)?
		pack.define(checkEmitterChannel) {
			// Copy the emitter's channel to temp for comparison.
			raw("data modify storage ra:temp check_channel set from entity @s data.properties.channel")

			// Try to overwrite it with the receiver's channel: success 0 (nothing changed) means
			// the two are the same.
			raw("execute store success score @s ${Obj.TEMP} run data modify storage ra:temp check_channel set from storage ra:temp receiver_channel")

			// Channels match: tag the receiver as receiving.
			raw("execute if score @s ${Obj.TEMP} matches 0 run tag @e[type=marker,tag=$CHECKING] add $RECEIVING")
		}

		// Toggle enabled (called by the wrench, as the marker).
		pack.define(toggle) { toggleEnabled("Receiver", "light_purple") }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("channel", "Channel: ", "aqua", y = 0.8)
		}

		// A broken receiver puts its shell out, or whatever it touched stays powered.
		val onBreak = pack.plainOnBreak(block, item, particle = true, afterKill = { shell(on = false) })
		pack.define(block.tick) {
			// Break detection: the receiver is a purpur block, anything else means it is gone.
			breakCheck(block, Blocks.PURPUR_BLOCK, onBreak)
			execute {
				asTarget(markers(block))
				at(self())
				run { function(process) }
			}
		}
	}

	/** As the marker, at its position. */
	private fun io.github.ayfri.kore.functions.Function.process() {
		defaultsChannelEnabled()

		// Only receive if enabled.
		raw("execute unless data entity @s data.properties{enabled:1b} at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		raw("execute unless data entity @s data.properties{enabled:1b} run return 0")

		// Pulsing from a remote.
		raw("execute if entity @s[tag=$PULSING] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if entity @s[tag=$PULSING] run scoreboard players remove @s $PULSE_TIMER 1")
		raw("execute if entity @s[tag=$PULSING] if score @s $PULSE_TIMER matches ..0 run tag @s remove $PULSING")

		// If pulsing, skip the emitter check.
		raw("execute if entity @s[tag=$PULSING] run return 1")

		// Is any emitter on the same channel transmitting? Our channel goes to storage for the comparison.
		raw("data modify storage ra:temp receiver_channel set from entity @s data.properties.channel")
		tag(self()) { add(CHECKING) }

		exec("as @e[type=marker,tag=${Emitter.block.tag},tag=${Emitter.TRANSMITTING}]", checkEmitterChannel)

		// Output based on receiving state.
		raw("execute if entity @s[tag=$RECEIVING] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute unless entity @s[tag=$RECEIVING] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")

		// Cleanup.
		tag(self()) { remove(CHECKING) }
		tag(self()) { remove(RECEIVING) }
	}
}
