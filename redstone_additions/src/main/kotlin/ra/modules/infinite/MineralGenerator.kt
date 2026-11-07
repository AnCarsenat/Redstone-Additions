package ra.modules.infinite

import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import ra.core.Ra
import ra.core.block.gogglesHeader
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreLine

/** Mineral Generator: grows one block of ore in front of itself every `cooldown` ticks. */
object MineralGenerator {
	val generator = Generator(
		"mineral_generator", "Mineral Generator",
		core = InfiniteItems.mineralCore,
		defaultCooldown = 100,
		loreLines = listOf(loreLine("Grows ore in front of itself, forever", "gray")),
	)

	/**
	 * Per mille, so the rare end of the table can be tuned finely. At the default 100-tick period
	 * that is a diamond roughly every seven minutes and an emerald roughly every half hour -- a
	 * generator is a trickle, not a quarry.
	 */
	private val table = listOf(
		570 to Blocks.STONE,
		200 to Blocks.COAL_ORE,
		140 to Blocks.IRON_ORE,
		75 to Blocks.REDSTONE_ORE,
		12 to Blocks.DIAMOND_ORE,
		3 to Blocks.EMERALD_ORE,
	)

	fun Ra.registerMineralGenerator() {
		with(generator) { registerGenerator() }
		pack.define(generator.process) { process() }
		pack.define(generator.block.goggles) { generatorGoggles(generator) }
	}

	/** As the generator's marker, at the generator, rotated with it. */
	private fun Function.process() {
		cooldownGate()

		// Somebody has to take the last one away first.
		raw("execute unless block ^ ^ ^1 $GROWABLE run return 0")

		rollTable(table)

		raw("execute positioned ^ ^ ^1 run particle minecraft:crit ~ ~0.5 ~ 0.3 0.2 0.3 0.05 6")
		raw("execute positioned ^ ^ ^1 run playsound minecraft:block.stone.place block @a[distance=..12] ~ ~ ~ 0.4 1.4")
	}
}

/** Mineral and Nether Generator readout: the name and the cooldown. */
fun Function.generatorGoggles(generator: Generator) {
	gogglesHeader(generator.block, nameY = 1.0)
	propLine("cooldown", "Cooldown: ", "aqua", y = 0.75, suffix = "t")
}
