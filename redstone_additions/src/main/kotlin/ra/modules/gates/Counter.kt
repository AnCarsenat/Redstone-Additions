package ra.modules.gates

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
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
import ra.core.block.outputShell
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.block.textLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.raw
import ra.lib.redstone.Side
import ra.lib.redstone.SideReaders

/**
 * Counter: counts rising edges on its input and fires when the count reaches `target`.
 *
 * - Input: redstone on any side but the top. Each time it turns on, `count` goes up by one.
 * - Reset: redstone on the TOP (a lever, button or dust on it, or a powered block above). Turning
 *   it on sets the count to 0 and puts the output out; while it stays on, input edges are ignored.
 * - `hold` off (default): on reaching `target` the count wraps to 0 and the output pulses for
 *   [PULSE] ticks, so the Counter divides a pulse train by `target`.
 * - `hold` on: on reaching `target` the output turns on and stays on, and the count stays at
 *   `target`, until the reset input clears it.
 *
 * The count is written only on an edge, so a Counter with a steady input reads its sides and
 * nothing else. It is a readonly property: the Data Handler and the Goggles show it, and only the
 * Creative Data Handler can set it by hand.
 */
object Counter {
	val block = CustomBlock(Ns.GATES, "counter", "Counter", model = "minecraft:chiseled_quartz_block")
	val process = block.fn("process")
	private val readInput = block.fn("read_input")
	private val readReset = block.fn("read_reset")
	private val count = block.fn("count")
	private val reset = block.fn("reset")
	private val holdOn = block.fn("hold_on")
	val toggleHold = block.fn("toggle_hold")

	/** Output pulse length, in ticks, when `hold` is off: the Shortener's default. */
	private const val PULSE = 2

	/** The top side is powered this tick. */
	private const val RESET_IN = "ra.counter.reset_in"

	/** The top side was powered last tick. */
	private const val RESET_WAS = "ra.counter.reset_was"

	private const val N = "#ctr.n ${Obj.TEMP}"
	private const val T = "#ctr.t ${Obj.TEMP}"

	/** The sides that count; the top is the reset. */
	private val INPUT_SIDES = listOf(Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST, Side.DOWN)

	fun Ra.registerCounter() {
		block(block)
		properties(block) {
			number("target", min = 1, max = 1000, default = 10)
			bool("hold", default = false)
			number("count", min = 0, max = 1000, readonly = true, default = 0)
		}
		wrenchCycle(block.id, WrenchCycle("Hold", "hold", toggleHold))
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.COMPARATOR) {
			pattern(" C ", "RHR", "SSS")
			key("C", Items.COMPARATOR)
			key("H", Items.HOPPER)
			key("R", Items.REPEATER)
			key("S", Items.STONE)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(readInput) { readInput() }
		for (s in INPUT_SIDES) pack.define(block.fn("read_${s.id}")) { returnRun { function(SideReaders.anySide, s.args()) } }
		pack.define(readReset) { returnRun { function(SideReaders.anySide, Side.UP.args()) } }
		pack.define(count) { count() }
		pack.define(holdOn) {
			raw("execute store result entity @s data.properties.count int 1 run scoreboard players get $T")
			outputShell(on = true)
		}
		pack.define(reset) {
			data(self()) { modify("data.properties.count") { set(0) } }
			pulseCancel()
			outputShell(on = false)
		}
		pack.define(toggleHold) { toggleHold() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("count", "Count: ", "aqua", y = 0.8)
			propLine("target", "Target: ", "aqua", y = 0.6)
			execute {
				ifCondition { data(self(), "data.properties{hold:1b}") }
				run { textLine("Output: ", "held until reset", "gold", y = 0.4) }
			}
		}
		val onBreak = pack.timingOnBreak(block, "minecraft:block.stone.break")
		pack.timingTick(block, Blocks.CHISELED_QUARTZ_BLOCK, onBreak, process)
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		data(newMarker(block)) {
			modify("data.properties.target") { set(10) }
			modify("data.properties.hold") { set(false) }
			modify("data.properties.count") { set(0) }
		}
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val p = Tag.POWERED
		pulseCountdown()

		// Read both inputs with the output shell ignored, so a lit shell does not count or reset.
		raw("tag @s add $IGNORE_BLOCKS")
		raw("tag @s remove $p")
		raw("execute if function ${readInput.asId()} run tag @s add $p")
		raw("tag @s remove $RESET_IN")
		raw("execute if function ${readReset.asId()} run tag @s add $RESET_IN")
		raw("tag @s remove $IGNORE_BLOCKS")

		// Reset on its rising edge; counting is blocked for as long as it is held.
		raw("execute if entity @s[tag=$RESET_IN,tag=!$RESET_WAS] run function ${reset.asId()}")
		raw("execute if entity @s[tag=$p,tag=!$WAS_POWERED,tag=!$RESET_IN] run function ${count.asId()}")

		raw("tag @s[tag=$RESET_IN] add $RESET_WAS")
		raw("tag @s[tag=!$RESET_IN] remove $RESET_WAS")
		raw("tag @s[tag=$p] add $WAS_POWERED")
		raw("tag @s[tag=!$p] remove $WAS_POWERED")
		pulseFinish()
	}

	/** Returns 1 at the first powered input side, 0 when none is. As the marker, at it. */
	private fun Function.readInput() {
		for (s in INPUT_SIDES) raw("execute if function ${readSide(s)} run return 1")
		returnValue(0)
	}

	private fun readSide(s: Side) = block.fn("read_${s.id}").asId()

	/** One rising edge. As the marker, at it. */
	private fun Function.count() {
		readProperty("target", default = 10, min = 1)
		raw("scoreboard players operation $T = #prop ${Obj.TEMP}")
		raw("execute store result score $N run data get entity @s data.properties.count")

		// Held at the target: nothing to do until the reset.
		raw("execute if data entity @s data.properties{hold:1b} if score $N >= $T run return 0")

		raw("scoreboard players add $N 1")
		raw("execute if score $N < $T run return run execute store result entity @s data.properties.count int 1 run scoreboard players get $N")

		// Reached the target.
		raw("execute if data entity @s data.properties{hold:1b} run return run function ${holdOn.asId()}")
		raw("data modify entity @s data.properties.count set value 0")
		raw("scoreboard players set #prop ${Obj.TEMP} $PULSE")
		pulseStart()
	}

	/** Wrench: hold on / off. As the marker, at it. */
	private fun Function.toggleHold() {
		val h = "#ctr.hold ${Obj.TEMP}"
		raw("execute store success score $h if data entity @s data.properties{hold:1b}")
		raw("execute if score $h matches 1 run data modify entity @s data.properties.hold set value 0b")
		raw("execute if score $h matches 0 run data modify entity @s data.properties.hold set value 1b")
		raw("""execute if score $h matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"pulse on target",color:"green"}]""")
		raw("""execute if score $h matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"hold until reset",color:"gold"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
	}
}
