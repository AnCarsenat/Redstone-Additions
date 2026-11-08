package ra.modules.gates

import ra.core.block.particleViewersSel
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
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

/** Randomizer: on each rising edge, turns its output on with probability `chance` percent. */
object Randomizer {
	val block = CustomBlock(Ns.GATES, "randomizer", "Randomizer", model = "minecraft:bookshelf")
	val process = block.fn("process")
	val trigger = block.fn("trigger")

	fun Ra.registerRandomizer() {
		block(block)
		properties(block) {
			number("chance", min = 0, max = 100)
		}
		pack.blockGive(block)
		pack.blockRecipe(block, unlockedBy = Items.BOOKSHELF) {
			pattern(" R ", "RBR", " R ")
			key("B", Items.BOOKSHELF)
			key("R", Items.REPEATER)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(process) { process() }
		pack.define(trigger) { trigger() }
		pack.gateGoggles(block, nameY = 1.0, prop = "chance", label = "Chance: ", suffix = "%", y = 0.8)
		val onBreak = pack.timingOnBreak(block, "minecraft:block.wood.break")
		pack.timingTick(block, Blocks.BOOKSHELF, onBreak, process)
	}

	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)
		// Default chance: 50%.
		data(newMarker(block)) { modify("data.properties.chance") { set(50) } }
		finishPlacement(block)
	}

	/** As the marker, at its position. */
	private fun Function.process() {
		val powered = self { tag = Tag.POWERED }

		detectSwitchIgnoringShell()

		// Rising edge.
		execute {
			unlessCondition { entity(self { tag = WAS_POWERED }) }
			ifCondition { entity(powered) }
			run { function(trigger) }
		}

		// Track power state.
		execute {
			ifCondition { entity(powered) }
			run { tag(self()) { add(WAS_POWERED) } }
		}
		execute {
			unlessCondition { entity(powered) }
			run { tag(self()) { remove(WAS_POWERED) } }
		}

		// Turn off when power is removed.
		execute {
			unlessCondition { entity(powered) }
			at(self())
			run { outputShell(on = false) }
		}
	}

	/** Rolls and sets the output. As the marker, at its position. */
	private fun Function.trigger() {
		// Chance, read through the guarded reader: a chance stored as the string "50" used to
		// read as 2, because data get returns a string's length.
		readProperty("chance", default = 50, min = 0)
		raw("scoreboard players operation @s ${Obj.TEMP} = #prop ${Obj.TEMP}")

		// Clamp chance to 0-100.
		execute {
			ifCondition { score(self(), Obj.TEMP, rangeOrIntEnd(0)) }
			run { scoreboard.players.set(self(), Obj.TEMP, 0) }
		}
		execute {
			ifCondition { score(self(), Obj.TEMP, rangeOrIntStart(101)) }
			run { scoreboard.players.set(self(), Obj.TEMP, 100) }
		}

		// Random number 0-99.
		//
		// This used to be stored in @s ra.power -- the redstone library's own output, on this
		// same marker, while the caller was still using it. randomizer/process calls this in the
		// middle of reading its power: the three lines after the call decide the rising-edge latch
		// and whether to clear the output, and they were reading a dice roll instead of a power
		// level. One roll in a hundred came up 0 and made a Randomizer wipe its own output the
		// instant after it fired, and the latch that stops it re-firing while held was decided by
		// chance rather than by redstone.
		//
		// Scratch belongs on a fake player. #rng is not a marker, so nothing else can be reading
		// it, and ra.power now means what the library set it to.
		raw("execute store result score #rng ${Obj.TEMP} run random value 0..99")

		// Output: on if random < chance.
		raw("execute if score #rng ${Obj.TEMP} < @s ${Obj.TEMP} at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		raw("execute unless score #rng ${Obj.TEMP} < @s ${Obj.TEMP} at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")

		// Particle feedback.
		raw("execute if score #rng ${Obj.TEMP} < @s ${Obj.TEMP} run particle minecraft:happy_villager ~ ~1 ~ 0.3 0.3 0.3 0.02 5 normal ${particleViewersSel()}")
		raw("execute unless score #rng ${Obj.TEMP} < @s ${Obj.TEMP} run particle minecraft:smoke ~ ~1 ~ 0.3 0.3 0.3 0.02 5 normal ${particleViewersSel()}")
	}
}
