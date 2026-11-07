package ra.modules.wireless

import io.github.ayfri.kore.arguments.types.literals.self
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
import ra.core.block.outputShell
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.finishPlacement
import ra.core.block.blockTick
import ra.core.block.BreakTest
import ra.core.block.forEachMarker
import io.github.ayfri.kore.functions.Function
import ra.core.block.markerSel

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

	fun Ra.registerReceiver() {
		block(block)
		wrenchToggles += TagRoute(block.tag, toggle)
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.PURPUR_BLOCK)) {
			pattern("GPG", "PRP", "GPG")
			key("G", Items.GOLD_INGOT)
			key("P", Items.PURPUR_BLOCK)
			key("R", Items.REDSTONE)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Purpur block with a marker.
			placeBlock(block)
			// Default channel (a string) and enabled state.
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.properties.enabled", "1b")
			finishPlacement(block)
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
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true, afterVanillaKill = { outputShell(on = false) })
		// Break detection: the receiver is a purpur block, anything else means it is gone.
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.PURPUR_BLOCK)) { forEachMarker(block, process) }
	}

	/** As the marker, at its position. */
	private fun Function.process() {
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

		executeRun("as ${Emitter.block.markerSel("tag=${Emitter.TRANSMITTING}")}", checkEmitterChannel)

		// Output based on receiving state.
		raw("execute if entity @s[tag=$RECEIVING] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute unless entity @s[tag=$RECEIVING] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")

		// Cleanup.
		tag(self()) { remove(CHECKING) }
		tag(self()) { remove(RECEIVING) }
	}
}
