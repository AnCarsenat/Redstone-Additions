package ra.modules.gates

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockRecipe
import ra.core.block.finishPlacement
import ra.core.block.newMarker
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.core.blockRegisterMessage
import ra.core.block.outputShell
import ra.core.block.readProperty
import ra.core.block.placementGuard
import ra.core.block.STONE_BREAK_SOUND

/** Delayer: turns its output on `delay` ticks after its input, and off as soon as the input drops. */
object Delayer {
	val block = CustomBlock(Ns.GATES, "delayer", "Delayer", model = "minecraft:quartz_block")
	val process = block.fn("process")

	private const val DELAYING = "ra.delaying"

	fun Ra.registerDelayer() {
		block(block)
		properties(block) {
			number("delay", min = 1, max = 1200)
		}
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.QUARTZ_BLOCK) {
			pattern("   ", " CR", "   ")
			key("C", Items.QUARTZ_BLOCK)
			key("R", Items.REPEATER)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.gateGoggles(block, nameY = 1.0, prop = "delay", label = "Delay: ", suffix = "t", y = 0.8)
		val onBreak = pack.timingOnBreak(block, STONE_BREAK_SOUND)
		pack.timingTick(block, Blocks.QUARTZ_BLOCK, onBreak, process)
	}

	/** As the spawned bat, at the block centre. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		// Default delay: 20 ticks = 1 second.
		data(newMarker(block)) { modify("data.properties.delay") { set(20) } }
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val powered = self { tag = Tag.POWERED }
		val delaying = self { tag = DELAYING }

		execute {
			unlessCondition { data(self(), "data.properties.delay") }
			run { data(self()) { modify("data.properties.delay") { set(20) } } }
		}

		detectSwitchIgnoringShell()

		// Powered and not counting yet: start the countdown.
		execute {
			ifCondition { entity(powered) }
			unlessCondition { entity(delaying) }
			run { tag(self()) { add(DELAYING) } }
		}
		execute {
			ifCondition { entity(delaying) }
			unlessCondition { data(self(), "data.delay_current") }
			run { readProperty("delay", default = 20, min = 1) }
		}
		raw("execute if entity @s[tag=$DELAYING] unless data entity @s data.delay_current store result entity @s data.delay_current int 1 run scoreboard players get #prop ${Obj.TEMP}")

		// Count down while powered.
		raw("execute if entity @s[tag=$DELAYING] store result score @s ${Obj.TEMP} run data get entity @s data.delay_current")
		execute {
			ifCondition { entity(delaying) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntStart(1)) }
			run { scoreboard.players.remove(self(), Obj.TEMP, 1) }
		}
		raw("execute if entity @s[tag=$DELAYING] store result storage ra:temp delay_val int 1 run scoreboard players get @s ${Obj.TEMP}")
		execute {
			ifCondition { entity(delaying) }
			run { data(self()) { modify("data.delay_current") { set(Storage.TEMP, "delay_val") } } }
		}

		// Output when the countdown reaches 0.
		execute {
			ifCondition { entity(delaying) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			at(self())
			run { outputShell(on = true) }
		}
		execute {
			ifCondition { entity(delaying) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntStart(1)) }
			at(self())
			run { outputShell(on = false) }
		}

		// Reset when power is removed.
		execute {
			unlessCondition { entity(powered) }
			ifCondition { entity(delaying) }
			run { tag(self()) { remove(DELAYING) } }
		}
		execute {
			unlessCondition { entity(powered) }
			run { data(self()) { remove("data.delay_current") } }
		}
		execute {
			unlessCondition { entity(powered) }
			at(self())
			run { outputShell(on = false) }
		}
	}
}
