package ra.modules.wireless

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
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
import ra.core.block.forEachMarker
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.raw
import ra.core.text
import ra.lib.redstone.Side
import ra.lib.redstone.SideReaders
import ra.modules.wireless.Channel.LEVEL

/**
 * Analog Emitter: sends the strongest redstone level reaching it (0-15) on its `channel`. Every
 * Analog Receiver on the channel outputs the strongest level its emitters send; a Wireless
 * Emitter on the same channel counts as 15, and a Wireless Receiver turns on while the level is
 * above 0 (the emitter carries [Emitter.TRANSMITTING] then).
 *
 * COST. Each side is asked `any_side` first and only a powered side has its level read, as the
 * Signal Calculator does. The level sent last is kept on the marker ([Channel.LEVEL]); the channel
 * is only worked out ([Channel.changed]) on the tick it differs.
 */
object AnalogEmitter {
	val block = CustomBlock(Ns.WIRELESS, "analog_emitter", "Analog Emitter", model = "minecraft:end_stone")
	val item = block.loreItem {}

	private val process = block.fn("process")
	private val update = block.fn("update")

	private const val T = Obj.TEMP

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	fun Ra.registerAnalogEmitter() {
		block(block)
		properties(block) {
			// Compared as a value; pasted only quoted, after ra_lib:safe/guard (the Pager).
			text("channel", TextFormat.NAME)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.COMPARATOR)) {
			pattern(" L ", "ACA", "SSS")
			key("L", Items.LIGHTNING_ROD)
			key("A", Items.AMETHYST_SHARD)
			key("C", Items.COMPARATOR)
			key("S", Items.SMOOTH_STONE_SLAB)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.channel", "\"default\"")
			setOnNew(block, "data.status.level", "0")
			raw("scoreboard players set ${block.newMarkerSel()} $LEVEL 0")
			finishPlacement(block)
		}

		for (s in Side.entries) {
			pack.define(any(s)) { returnRun { function(SideReaders.anySide, s.args()) } }
			pack.define(level(s)) {
				execute {
					storeResult { score(literal("#wl.l"), T) }
					run { function(RaLib.Redstone.side, s.args()) }
				}
				raw("scoreboard players operation #wl.in $T > #wl.l $T")
			}
		}

		// As the marker, at its position.
		pack.define(process) {
			defaultChannel()
			raw("scoreboard players set #wl.in $T 0")
			for (s in Side.entries) raw("execute if function ${any(s).asId()} run function ${level(s).asId()}")
			// 16 is a repeater or comparator pointing in: a full signal.
			raw("execute if score #wl.in $T matches 16.. run scoreboard players set #wl.in $T 15")
			raw("execute if score @s $LEVEL = #wl.in $T run return 0")
			function(update)
		}

		// The level changed: keep it, show it, tell the channel. As the marker, at it.
		pack.define(update) {
			raw("scoreboard players operation #wl.old $T = @s $LEVEL")
			raw("scoreboard players operation #wl.new $T = #wl.in $T")
			raw("scoreboard players operation @s $LEVEL = #wl.in $T")
			raw("execute store result entity @s data.status.level int 1 run scoreboard players get @s $LEVEL")
			raw("tag @s[scores={$LEVEL=1..}] add ${Emitter.TRANSMITTING}")
			raw("tag @s[scores={$LEVEL=..0}] remove ${Emitter.TRANSMITTING}")
			raw("function ${Channel.changed.asId()}")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("channel", "Channel: ", "aqua", y = 0.8)
			dataLine("level", "Level: ", "red", y = 0.6)
		}

		val onBreak = pack.blockOnBreak(
			block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true,
			// A broken emitter stops sending: its channel falls to what the others send.
			cleanup = {
				raw("scoreboard players operation #wl.old $T = @s $LEVEL")
				raw("scoreboard players set #wl.new $T 0")
				raw("execute if score #wl.old $T matches 1.. run function ${Channel.changed.asId()}")
			},
		)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.END_STONE)) { forEachMarker(block, process) }
	}
}
