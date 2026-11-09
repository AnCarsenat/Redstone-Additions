package ra.modules.gates

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
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
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockRecipe
import ra.core.block.blockTick
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
import ra.core.block.markerSel
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
import ra.lib.redstone.Side
import ra.lib.redstone.SideReaders

/**
 * Sequencer: steps 1, 2, ... `steps` (at most 15), one step per input pulse, then back to 1.
 *
 * - Input: redstone on any side but the top; each time it turns on, the step goes up by one.
 * - Reset: redstone on the TOP, as on the Counter. Its rising edge sets the step to 0 (no output),
 *   and while it stays on the input is ignored. The next pulse after a reset is step 1.
 * - Output: the step number as an analog level on every target block in the 3x3x3 cube (see
 *   [analogShell]), so one dust line carries the whole state: a comparator, or a Signal
 *   Calculator set to "equal", picks out one step. Iron blocks in the cube turn to redstone blocks
 *   on the last step, which is the carry for chaining Sequencers.
 *
 * WHY ONE ANALOG OUTPUT AND NOT 15 OUTPUT POSITIONS. Every gate of this module is a plain cube
 * with an output shell around it and no facing; fifteen named positions would need a facing and
 * a fixed footprint the player has to learn. A level per step fits the shell, and decoding a level
 * is what comparators and the Calculator already do.
 *
 * The step is written only on an edge. A readonly property, like the Counter's count.
 */
object Sequencer {
	val block = CustomBlock(Ns.GATES, "sequencer", "Sequencer", model = "minecraft:smooth_quartz")
	val process = block.fn("process")
	private val readInput = block.fn("read_input")
	private val readReset = block.fn("read_reset")
	private val advance = block.fn("advance")
	private val reset = block.fn("reset")
	private val output = block.fn("output")
	val cycleSteps = block.fn("cycle_steps")

	private const val MAX_STEPS = 15
	private const val DEFAULT_STEPS = 4

	/** The top side is powered this tick / was last tick. */
	private const val RESET_IN = "ra.sequencer.reset_in"
	private const val RESET_WAS = "ra.sequencer.reset_was"

	/** The iron shell is lit (the last step). */
	private const val LIT = "ra.sequencer.lit"

	/** Scratch inside [output]: the step is the last one. */
	private const val LAST = "ra.sequencer.last"

	private const val RESYNC = 40

	private const val T = Obj.TEMP
	private const val N = "#seq.n"
	private const val S = "#seq.s"

	private val INPUT_SIDES = listOf(Side.NORTH, Side.SOUTH, Side.WEST, Side.EAST, Side.DOWN)

	fun Ra.registerSequencer() {
		block(block)
		properties(block) {
			number("steps", min = 1, max = MAX_STEPS, default = DEFAULT_STEPS)
			number("step", min = 0, max = MAX_STEPS, readonly = true, default = 0)
		}
		wrenchCycle(block.id, WrenchCycle("Steps", "steps", cycleSteps))
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.REPEATER) {
			pattern(" T ", "RQR")
			key("T", Items.TARGET)
			key("R", Items.REPEATER)
			key("Q", Items.SMOOTH_QUARTZ)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(readInput) {
			for (s in INPUT_SIDES) raw("execute if function ${readSide(s).asId()} run return 1")
			returnValue(0)
		}
		for (s in INPUT_SIDES) pack.define(readSide(s)) { returnRun { function(SideReaders.anySide, s.args()) } }
		pack.define(readReset) { returnRun { function(SideReaders.anySide, Side.UP.args()) } }
		pack.define(advance) { advance() }
		pack.define(reset) {
			data(self()) { modify("data.properties.step") { set(0) } }
			function(output)
		}
		pack.define(output) { output() }
		pack.define(cycleSteps) { cycleSteps() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("step", "Step: ", "aqua", y = 0.8)
			propLine("steps", "Steps: ", "aqua", y = 0.6)
		}
		val onBreak = pack.analogOnBreak(block, "minecraft:block.stone.break")
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.SMOOTH_QUARTZ)) {
			// Rewrite every output once every 40 ticks: a target block placed into the cube since,
			// or a shell edited by hand, picks the step up.
			raw("scoreboard players add #seq.resync $T 1")
			raw("execute if score #seq.resync $T matches $RESYNC.. as ${block.markerSel()} at @s run function ${output.asId()}")
			raw("execute if score #seq.resync $T matches $RESYNC.. run scoreboard players set #seq.resync $T 0")
			forEachMarker(block, process)
		}
	}

	private fun readSide(s: Side) = block.fn("read_${s.id}")

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		data(newMarker(block)) {
			modify("data.properties.steps") { set(DEFAULT_STEPS) }
			modify("data.properties.step") { set(0) }
		}
		finishPlacement(block)
	}

	/** As the marker, at its position. Only edges write anything. */
	private fun Function.process() {
		val p = Tag.POWERED
		raw("tag @s add $IGNORE_BLOCKS")
		raw("tag @s remove $p")
		raw("execute if function ${readInput.asId()} run tag @s add $p")
		raw("tag @s remove $RESET_IN")
		raw("execute if function ${readReset.asId()} run tag @s add $RESET_IN")
		raw("tag @s remove $IGNORE_BLOCKS")

		raw("execute if entity @s[tag=$RESET_IN,tag=!$RESET_WAS] run function ${reset.asId()}")
		raw("execute if entity @s[tag=$p,tag=!$WAS_POWERED,tag=!$RESET_IN] run function ${advance.asId()}")

		raw("tag @s[tag=$RESET_IN] add $RESET_WAS")
		raw("tag @s[tag=!$RESET_IN] remove $RESET_WAS")
		raw("tag @s[tag=$p] add $WAS_POWERED")
		raw("tag @s[tag=!$p] remove $WAS_POWERED")
	}

	/** `#seq.s` = steps, 1-15. */
	private fun Function.readSteps() {
		readProperty("steps", default = DEFAULT_STEPS, min = 1)
		raw("scoreboard players operation $S $T = #prop $T")
		raw("execute if score $S $T matches ${MAX_STEPS + 1}.. run scoreboard players set $S $T $MAX_STEPS")
	}

	/** One rising edge: the next step, wrapping after the last. As the marker, at it. */
	private fun Function.advance() {
		readSteps()
		raw("execute store result score $N $T run data get entity @s data.properties.step")
		raw("scoreboard players add $N $T 1")
		raw("execute if score $N $T > $S $T run scoreboard players set $N $T 1")
		raw("execute store result entity @s data.properties.step int 1 run scoreboard players get $N $T")
		function(output)
	}

	/** Write data.properties.step to the shell. As the marker, at it. */
	private fun Function.output() {
		readSteps()
		raw("execute store result score $N $T run data get entity @s data.properties.step")
		raw("execute if score $N $T matches ${MAX_STEPS + 1}.. run scoreboard players set $N $T $MAX_STEPS")
		raw("execute if score $N $T matches ..-1 run scoreboard players set $N $T 0")
		analogShell(N)

		// The iron shell: lit on the last step only.
		raw("tag @s remove $LAST")
		raw("execute if score $N $T matches 1.. if score $N $T >= $S $T run tag @s add $LAST")
		raw("execute if entity @s[tag=$LAST] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute if entity @s[tag=!$LAST,tag=$LIT] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		raw("tag @s[tag=$LAST] add $LIT")
		raw("tag @s[tag=!$LAST] remove $LIT")
		raw("tag @s remove $LAST")
	}

	/** Wrench: steps + 1, 15 wraps to 2. As the marker, at it. */
	private fun Function.cycleSteps() {
		readSteps()
		raw("scoreboard players add $S $T 1")
		raw("execute if score $S $T matches ${MAX_STEPS + 1}.. run scoreboard players set $S $T 2")
		raw("execute store result entity @s data.properties.steps int 1 run scoreboard players get $S $T")
		// A step past the new last one starts again from 1.
		raw("execute store result score $N $T run data get entity @s data.properties.step")
		raw("execute if score $N $T > $S $T run data modify entity @s data.properties.step set value 1")
		function(output)
		raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{score:{name:"$S",objective:"$T"},color:"aqua"},{text:" steps",color:"aqua"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
	}
}
