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

/** Shortener: turns a rising edge into a pulse of `pulse` ticks, however long the input stays on. */
object Shortener {
	val block = CustomBlock(Ns.GATES, "shortener", "Shortener", model = "minecraft:quartz_block")
	val process = block.fn("process")

	private const val PULSING = "ra.pulsing"

	fun Ra.registerShortener() {
		block(block)
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.QUARTZ_BLOCK) {
			pattern("   ", " C ", " R ")
			key("C", Items.QUARTZ_BLOCK)
			key("R", Items.REPEATER)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.gateGoggles(block, nameY = 1.0, prop = "pulse", label = "Pulse: ", suffix = "t", y = 0.8)
		val onBreak = pack.timingOnBreak(block, STONE_BREAK_SOUND)
		pack.timingTick(block, Blocks.QUARTZ_BLOCK, onBreak, process)
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		// Default pulse length: 2 ticks.
		data(newMarker(block)) { modify("data.properties.pulse") { set(2) } }
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val powered = self { tag = Tag.POWERED }
		val wasPowered = self { tag = WAS_POWERED }
		val pulsing = self { tag = PULSING }

		execute {
			unlessCondition { data(self(), "data.properties.pulse") }
			run { data(self()) { modify("data.properties.pulse") { set(2) } } }
		}

		detectSwitchIgnoringShell()

		// Rising edge (power goes from 0 to 1+).
		execute {
			unlessCondition { entity(wasPowered) }
			ifCondition { entity(powered) }
			run { tag(self()) { add(PULSING) } }
		}
		execute {
			unlessCondition { entity(wasPowered) }
			ifCondition { entity(powered) }
			run { readProperty("pulse", default = 2, min = 1) }
		}
		raw("execute unless entity @s[tag=$WAS_POWERED] if entity @s[tag=${Tag.POWERED}] store result entity @s data.pulse_remaining int 1 run scoreboard players get #prop ${Obj.TEMP}")

		// Track power state.
		execute {
			ifCondition { entity(powered) }
			run { tag(self()) { add(WAS_POWERED) } }
		}
		execute {
			unlessCondition { entity(powered) }
			run { tag(self()) { remove(WAS_POWERED) } }
		}

		// Output while pulsing.
		execute {
			ifCondition { entity(pulsing) }
			at(self())
			run { outputShell(on = true) }
		}

		// Count the pulse down.
		raw("execute if entity @s[tag=$PULSING] store result score @s ${Obj.TEMP} run data get entity @s data.pulse_remaining")
		execute {
			ifCondition { entity(pulsing) }
			run { scoreboard.players.remove(self(), Obj.TEMP, 1) }
		}
		raw("execute if entity @s[tag=$PULSING] store result storage ra:temp pulse_val int 1 run scoreboard players get @s ${Obj.TEMP}")
		execute {
			ifCondition { entity(pulsing) }
			run { data(self()) { modify("data.pulse_remaining") { set(Storage.TEMP, "pulse_val") } } }
		}

		// Stop pulsing when the timer runs out.
		execute {
			ifCondition { entity(pulsing) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			run { tag(self()) { remove(PULSING) } }
		}
		execute {
			ifCondition { entity(pulsing) }
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			at(self())
			run { outputShell(on = false) }
		}
		execute {
			unlessCondition { entity(pulsing) }
			at(self())
			run { outputShell(on = false) }
		}
	}
}
