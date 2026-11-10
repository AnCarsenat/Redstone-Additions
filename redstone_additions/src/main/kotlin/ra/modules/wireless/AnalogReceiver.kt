package ra.modules.wireless

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockGive
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.outputShell
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.targetPowerFill
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.raw
import ra.core.text
import ra.modules.wireless.Channel.LEVEL

/**
 * Analog Receiver: outputs its channel's level, 0-15 -- the strongest an Analog Emitter on the
 * channel sends, or 15 while a Wireless Emitter on it is powered. Every target block in the 3x3x3
 * cube gets the level as its `power` (see [targetPowerFill]), and iron blocks there turn into
 * redstone blocks while it is above 0, as on the Signal Calculator.
 *
 * NOTHING RUNS WHILE THE CHANNEL IS STILL. The emitters push a new level ([Channel.push]) on the
 * tick it changes. A receiver works its level out itself ([resync]) only when it is placed and
 * once every [RESYNC] ticks, which picks up a channel edited in the Data Handler, a target block
 * placed next to it since, and an emitter that vanished without breaking (a /kill).
 */
object AnalogReceiver {
	val block = CustomBlock(Ns.WIRELESS, "analog_receiver", "Analog Receiver", model = "minecraft:amethyst_block")
	val item = block.loreItem {}

	const val RESYNC = 40

	/** The output matches the channel. Cleared on every receiver each [RESYNC] ticks. */
	private const val SYNCED = "ra.wl.synced"

	private val resync = block.fn("resync")
	private val apply = block.fn("apply")
	private val output = block.fn("output")

	private const val T = Obj.TEMP

	fun Ra.registerAnalogReceiver() {
		block(block)
		properties(block) {
			// Compared as a value, never pasted into a command.
			text("channel", TextFormat.NAME)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.COMPARATOR)) {
			pattern(" L ", "CAC", "SSS")
			key("L", Items.LIGHTNING_ROD)
			key("C", Items.COMPARATOR)
			key("A", Items.AMETHYST_SHARD)
			key("S", Items.SMOOTH_STONE_SLAB)
		}

		// As bat, at bat position. The first tick's resync gives it its level.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.status.level", "0")
			finishPlacement(block)
		}

		// From scratch: the strongest level on our channel. As the marker, at it.
		pack.define(resync) {
			defaultChannel()
			raw("data modify storage ra:temp wl_channel set from entity @s data.properties.channel")
			raw("scoreboard players set #wl.other $T 0")
			raw("function ${Channel.collect.asId()}")
			raw("scoreboard players operation #wl.level $T = #wl.other $T")
			raw("function ${apply.asId()}")
			raw("tag @s add $SYNCED")
		}

		// From an emitter: as the marker, at it, the channel already matched.
		pack.define(Channel.push) {
			raw("execute if score @s $LEVEL = #wl.level $T run return 0")
			raw("function ${apply.asId()}")
		}

		// Show #wl.level. As the marker, at it.
		pack.define(apply) {
			raw("scoreboard players operation @s $LEVEL = #wl.level $T")
			raw("execute store result entity @s data.status.level int 1 run scoreboard players get @s $LEVEL")
			raw("execute store result storage ra:temp wl_out.level int 1 run scoreboard players get @s $LEVEL")
			raw("function ${output.asId()} with storage ra:temp wl_out")
			raw("execute if score @s $LEVEL matches 1.. run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
			raw("execute if score @s $LEVEL matches ..0 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		}

		// Macro {level}: only ever the integer [apply] stored from a score.
		pack.define(output) { raw("\$" + targetPowerFill(1, "\$(level)")) }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("channel", "Channel: ", "aqua", y = 0.8)
			dataLine("level", "Output: ", "red", y = 0.6)
		}

		// A broken receiver puts its outputs out, or whatever it touched stays powered.
		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true,
			afterVanillaKill = {
				outputShell(on = false)
				raw(targetPowerFill(1, "0"))
			},
		)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.AMETHYST_BLOCK)) {
			// One counter for every receiver, as the Signal Calculator does.
			raw("scoreboard players add #wl.resync $T 1")
			raw("execute if score #wl.resync $T matches $RESYNC.. run tag ${block.markerSel()} remove $SYNCED")
			raw("execute if score #wl.resync $T matches $RESYNC.. run scoreboard players set #wl.resync $T 0")
			raw("execute as ${block.markerSel("tag=!$SYNCED")} at @s run function ${resync.asId()}")
		}
	}
}
