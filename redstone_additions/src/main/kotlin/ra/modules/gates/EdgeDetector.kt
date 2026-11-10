package ra.modules.gates

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockRecipe
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarker
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.raw

/**
 * Edge Detector: a pulse of `pulse` ticks each time its input turns on (`mode` "rising"), turns
 * off ("falling"), or either ("both").
 *
 * Not the sensors' EdgeTrigger (ra/modules/sensors/EdgeTrigger.kt): that is the internal helper
 * the Tag Adder and Tag Remover use to act once per rising edge, not a block. This is the
 * placeable gate, and the only block that reacts to a falling edge. The Shortener is the closest
 * neighbour: it does what "rising" does, but cannot see the input turning off.
 */
object EdgeDetector {
	val block = CustomBlock(Ns.GATES, "edge_detector", "Edge Detector", model = "minecraft:quartz_pillar")
	val process = block.fn("process")
	private val fire = block.fn("fire")
	val cycleMode = block.fn("cycle_mode")

	/** In cycle order; the last wraps to the first. Anything else in `mode` reads as "rising". */
	private val MODES = listOf("rising" to "green", "falling" to "red", "both" to "light_purple")

	fun Ra.registerEdgeDetector() {
		block(block)
		properties(block) {
			text("mode", default = "rising")
			number("pulse", min = 1, max = 200, default = 2)
		}
		wrenchCycle(block.id, WrenchCycle("Mode", "mode", cycleMode))
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.OBSERVER) {
			pattern("ROR", "SSS")
			key("R", Items.REPEATER)
			key("O", Items.OBSERVER)
			key("S", Items.STONE)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(fire) {
			readProperty("pulse", default = 2, min = 1)
			pulseStart()
		}
		pack.define(cycleMode) { cycleMode() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("mode", "Mode: ", "aqua", y = 0.8)
			propLine("pulse", "Pulse: ", "aqua", y = 0.6, suffix = "t")
		}
		val onBreak = pack.timingOnBreak(block, "minecraft:block.stone.break")
		pack.timingTick(block, Blocks.QUARTZ_PILLAR, onBreak, process)
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		data(newMarker(block)) {
			modify("data.properties.mode") { set("rising") }
			modify("data.properties.pulse") { set(2) }
		}
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val p = Tag.POWERED
		pulseCountdown()
		detectSwitchIgnoringShell()

		// Edges only: a steady input, on or off, runs none of these.
		raw("""execute if entity @s[tag=$p,tag=!$WAS_POWERED] unless data entity @s data.properties{mode:"falling"} run function ${fire.asId()}""")
		raw("""execute if entity @s[tag=!$p,tag=$WAS_POWERED] if data entity @s data.properties{mode:"falling"} run function ${fire.asId()}""")
		raw("""execute if entity @s[tag=!$p,tag=$WAS_POWERED] if data entity @s data.properties{mode:"both"} run function ${fire.asId()}""")

		raw("tag @s[tag=$p] add $WAS_POWERED")
		raw("tag @s[tag=!$p] remove $WAS_POWERED")
		pulseFinish()
	}

	/** Wrench: rising -> falling -> both -> rising. As the marker, at it. */
	private fun Function.cycleMode() {
		val m = "#edge.mode ${Obj.TEMP}"
		raw("scoreboard players set $m 0")
		MODES.forEachIndexed { i, (mode, _) ->
			if (i > 0) raw("""execute if data entity @s data.properties{mode:"$mode"} run scoreboard players set $m $i""")
		}
		MODES.forEachIndexed { i, _ ->
			val (next, color) = MODES[(i + 1) % MODES.size]
			raw("""execute if score $m matches $i run data modify entity @s data.properties.mode set value "$next"""")
			raw("""execute if score $m matches $i run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"$next",color:"$color"}]""")
		}
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
	}
}
