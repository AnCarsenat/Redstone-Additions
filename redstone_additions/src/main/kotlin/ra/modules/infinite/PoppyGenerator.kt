package ra.modules.infinite

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import ra.core.Obj
import ra.core.Ra
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedTextLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreLine

/** Poppy Generator: plants one flower in front of itself. A real dropper, no skin -- see [Generator]. */
object PoppyGenerator {
	val generator = Generator(
		"poppy_generator", "Poppy Generator",
		core = InfiniteItems.poppyCore,
		defaultCooldown = 80,
		loreLines = listOf(
			loreLine("Grows flowers in front of itself, forever", "gray"),
			loreLine("Shift+RMB with the wrench: single / patch", "dark_gray"),
		),
	)
	private val block = generator.block

	val single = block.fn("single")
	val growFlower = block.fn("grow_flower")
	val plant = block.fn("plant")
	val checkCell = block.fn("check_cell")
	val checkGround = block.fn("check_ground")

	/** The block in front, then the eight around it: the 3x3 both the planter and the ground check search. */
	private val cells = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)

	private val flowers = listOf(
		Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET,
		Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.BLUE_ORCHID, Blocks.LILY_OF_THE_VALLEY,
	)

	private val ground = literal("#poppy.ground")
	private val done = literal("#poppy.done")

	fun Ra.registerPoppyGenerator() {
		with(generator) {
			registerGenerator {
				// `mode` is gone with the patch. Removed from anything still carrying it so the
				// Data Handler stops offering a setting that decides nothing.
				raw("execute as ${block.markerSel()} run data remove entity @s data.properties.mode")
			}
		}

		// One flower in front, always. The 3x3 patch mode is gone: it was a second code path over
		// the same ground for a block whose whole job is one flower at a time.
		pack.define(generator.process) {
			cooldownGate(generator.defaultCooldown)
			execute {
				positioned(vec3(0, 0, 1).local)
				run { function(single) }
			}
		}
		pack.define(single) { single() }
		pack.define(growFlower) { growFlower() }
		pack.define(plant) { plant() }
		pack.define(checkCell) { checkCell() }
		pack.define(checkGround) { searchCells(checkGround = true) }
		pack.define(block.goggles) { goggles() }
	}

	/**
	 * Plant one flower somewhere in front of the generator. At the block in front of it.
	 *
	 * The block dead ahead is tried first, then the eight around it, and the first spot that takes
	 * a flower wins -- `plant` sets #poppy.done, and every later cell is skipped once it is set.
	 *
	 * Insisting on the one block dead ahead is what made this look broken: a generator that faces
	 * slightly off, or whose soil sits beside rather than in front of it, planted nothing and said
	 * nothing.
	 */
	private fun Function.single() {
		scoreboard.players.set(done, Obj.TEMP, 0)
		searchCells(checkGround = false)
	}

	/**
	 * The 3x3 walk. For the planter each cell after the first is skipped once something was
	 * planted; for the ground check (`check_ground`) every cell is checked -- same 3x3 the planter
	 * searches, so the Goggles readout cannot drift from what the generator actually does.
	 */
	private fun Function.searchCells(checkGround: Boolean) {
		val target = if (checkGround) checkCell else growFlower
		function(target)
		for ((dx, dz) in cells) execute {
			if (!checkGround) ifCondition { score(done, Obj.TEMP) equalTo 0 }
			positioned(vec3(dx, 0, dz).relative)
			run { function(target) }
		}
	}

	/**
	 * Plant one flower at, above or below the current position -- the block in front of the
	 * generator.
	 *
	 * A flower only survives on `#ra_infinite:flower_ground` -- the vanilla dirt family plus
	 * farmland -- and providing that is the player's job. The generator plants; it does not
	 * terraform.
	 *
	 * Three spots are tried, because all three are what "in front of it" means in practice, and
	 * which one applies depends only on how the generator was placed:
	 *
	 *   1. the block in front, ground one below it -- generator level with the soil;
	 *   2. one block up, when the block in front *is* the soil -- a generator aimed straight at a
	 *      grass block, which is how most people set this up;
	 *   3. one block down, ground two below -- a generator on a pedestal, planting on the ground it
	 *      overlooks.
	 *
	 * Not tried: anything further away, and anything at all when the generator faces up, since the
	 * only thing under that target is the generator itself.
	 */
	private fun Function.growFlower() {
		execute {
			ifCondition { block(vec3(), InfiniteTags.growable) }
			ifCondition { block(vec3(0, -1, 0).relative, InfiniteTags.flowerGround) }
			run { returnRun(plant) }
		}
		execute {
			ifCondition { block(vec3(), InfiniteTags.flowerGround) }
			ifCondition { block(vec3(0, 1, 0).relative, InfiniteTags.growable) }
			positioned(vec3(0, 1, 0).relative)
			run { returnRun(plant) }
		}
		execute {
			ifCondition { block(vec3(0, -1, 0).relative, InfiniteTags.growable) }
			ifCondition { block(vec3(0, -2, 0).relative, InfiniteTags.flowerGround) }
			positioned(vec3(0, -1, 0).relative)
			run { function(plant) }
		}
	}

	/** Put one random flower here. At a spot already checked for ground. */
	private fun Function.plant() {
		raw("execute store result score #gen.roll ${Obj.TEMP} run random value 1..${flowers.size}")
		flowers.forEachIndexed { i, flower ->
			raw("execute if score #gen.roll ${Obj.TEMP} matches ${i + 1} run setblock ~ ~ ~ ${flower.asId()}")
		}

		raw("particle minecraft:happy_villager ~ ~0.4 ~ 0.3 0.3 0.3 0.01 4 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.grass.place block ${soundListenersSel(12)} ~ ~ ~ 0.3 1.3")

		// Single mode stops searching once something has been planted.
		scoreboard.players.set(done, Obj.TEMP, 1)
	}

	/** One cell of the ground check -- the same three heights grow_flower tries. Sets #poppy.ground to 1 if a flower would take. */
	private fun Function.checkCell() {
		raw("execute if block ~ ~ ~ $GROWABLE if block ~ ~-1 ~ $FLOWER_GROUND run scoreboard players set #poppy.ground ${Obj.TEMP} 1")
		raw("execute if block ~ ~ ~ $FLOWER_GROUND if block ~ ~1 ~ $GROWABLE run scoreboard players set #poppy.ground ${Obj.TEMP} 1")
		raw("execute if block ~ ~-1 ~ $GROWABLE if block ~ ~-2 ~ $FLOWER_GROUND run scoreboard players set #poppy.ground ${Obj.TEMP} 1")
	}

	/** The billboard lays the lines out itself, top first, growing upward from above the block. */
	private fun Function.goggles() {
		gogglesHeader(block)
		// Drawn only if the schema lets the goggles show it; the generators' fixed cooldown is
		// `goggles = false` (Generator.kt), so this is a Data Handler value, not a billboard line.
		stackedPropLine("cooldown", "Cooldown: ", "aqua", suffix = "t")

		// Whether the spot it plants into has anything a flower can stand on. Without this a
		// generator aimed at bare stone or at its own top face looks broken rather than unplanted
		// -- providing dirt or grass is the player's job, but knowing that it is missing should not
		// be. The check has to move to the target, the line has to be drawn back at the block, so
		// the verdict travels as a score.
		scoreboard.players.set(ground, Obj.TEMP, 0)
		execute {
			rotatedAs(self())
			positioned(vec3(0, 0, 1).local)
			run { function(checkGround) }
		}

		execute {
			ifCondition { score(ground, Obj.TEMP) equalTo 1 }
			run { stackedTextLine("Ground: ", "ok", "green") }
		}
		execute {
			ifCondition { score(ground, Obj.TEMP) equalTo 0 }
			run { stackedTextLine("Ground: ", "none", "red") }
		}
	}
}
