package ra.modules.gates

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
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

/** Extender: keeps its output on for `extend` ticks after its input drops. */
object Extender {
	val block = CustomBlock(Ns.GATES, "extender", "Extender", model = "minecraft:quartz_block")
	val process = block.fn("process")

	private const val EXTENDING = "ra.extending"

	fun Ra.registerExtender() {
		block(block)
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.QUARTZ_BLOCK) {
			pattern(" R ", " C ", "   ")
			key("C", Items.QUARTZ_BLOCK)
			key("R", Items.REPEATER)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.gateGoggles(block, nameY = 1.0, prop = "extend", label = "Extend: ", suffix = "t", y = 0.8)
		val onBreak = pack.timingOnBreak(block, STONE_BREAK_SOUND)
		pack.timingTick(block, Blocks.QUARTZ_BLOCK, onBreak, process)
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		// Default extend: 20 ticks = 1 second.
		data(newMarker(block)) { modify("data.properties.extend") { set(20) } }
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val powered = self { tag = Tag.POWERED }
		val wasPowered = self { tag = WAS_POWERED }
		val extending = self { tag = EXTENDING }

		execute {
			unlessCondition { data(self(), "data.properties.extend") }
			run { data(self()) { modify("data.properties.extend") { set(20) } } }
		}

		detectSwitchIgnoringShell()

		// Rising edge (power goes from 0 to 1+).
		execute {
			unlessCondition { entity(wasPowered) }
			ifCondition { entity(powered) }
			run { tag(self()) { add(EXTENDING) } }
		}
		execute {
			unlessCondition { entity(wasPowered) }
			ifCondition { entity(powered) }
			run { readProperty("extend", default = 20, min = 1) }
		}
		raw("execute unless entity @s[tag=$WAS_POWERED] if entity @s[tag=${Tag.POWERED}] store result entity @s data.extend_remaining int 1 run scoreboard players get #prop ${Obj.TEMP}")

		// Track power state.
		execute {
			ifCondition { entity(powered) }
			run { tag(self()) { add(WAS_POWERED) } }
		}
		execute {
			unlessCondition { entity(powered) }
			run { tag(self()) { remove(WAS_POWERED) } }
		}

		// While powered, keep resetting the timer.
		execute {
			ifCondition { entity(powered) }
			run { readProperty("extend", default = 20, min = 1) }
		}
		raw("execute if entity @s[tag=${Tag.POWERED}] store result entity @s data.extend_remaining int 1 run scoreboard players get #prop ${Obj.TEMP}")

		// Output while extending or powered.
		execute {
			ifCondition { entity(extending) }
			at(self())
			run { outputShell(on = true) }
		}
		execute {
			ifCondition { entity(powered) }
			at(self())
			run { outputShell(on = true) }
		}

		// Count down when not powered.
		raw("execute unless entity @s[tag=${Tag.POWERED}] if entity @s[tag=$EXTENDING] store result score @s ${Obj.TEMP} run data get entity @s data.extend_remaining")
		execute {
			unlessCondition { entity(powered) }
			ifCondition { entity(extending) }
			run { scoreboard.players.remove(self(), Obj.TEMP, 1) }
		}
		raw("execute unless entity @s[tag=${Tag.POWERED}] if entity @s[tag=$EXTENDING] store result storage ra:temp extend_val int 1 run scoreboard players get @s ${Obj.TEMP}")
		execute {
			unlessCondition { entity(powered) }
			ifCondition { entity(extending) }
			run { data(self()) { modify("data.extend_remaining") { set(Storage.TEMP, "extend_val") } } }
		}

		// Stop extending when the timer runs out.
		execute {
			unlessCondition { entity(powered) }
			ifCondition { entity(extending) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			run { tag(self()) { remove(EXTENDING) } }
		}
		execute {
			unlessCondition { entity(powered) }
			ifCondition { entity(extending) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			at(self())
			run { outputShell(on = false) }
		}
		execute {
			unlessCondition { entity(powered) }
			unlessCondition { entity(extending) }
			at(self())
			run { outputShell(on = false) }
		}
	}
}
