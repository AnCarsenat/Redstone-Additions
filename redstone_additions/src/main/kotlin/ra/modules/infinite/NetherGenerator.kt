package ra.modules.infinite

import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.item.loreLine

/** Nether Generator: grows one block of nether stone in front of itself. A real dropper, no skin -- see [Generator]. */
object NetherGenerator {
	val generator = Generator(
		"nether_generator", "Nether Generator",
		core = InfiniteItems.netherCore,
		defaultCooldown = 100,
		loreLines = listOf(loreLine("Grows nether stone in front of itself, forever", "gray")),
	)

	/**
	 * 1% ancient debris on a 120-tick period is one scrap's worth every ten minutes or so.
	 * Netherite blocks are deliberately not on the table -- four ingots out of a single cycle
	 * would undo the whole material.
	 */
	private val table = listOf(
		690 to Blocks.NETHERRACK,
		300 to Blocks.MAGMA_BLOCK,
		10 to Blocks.ANCIENT_DEBRIS,
	)

	fun Ra.registerNetherGenerator() {
		with(generator) { registerGenerator() }
		pack.define(generator.process) { process() }
		pack.define(generator.block.goggles) { generatorGoggles(generator) }
	}

	/** As the generator's marker, at the generator, rotated with it. */
	private fun Function.process() {
		cooldownGate()
		raw("execute unless block ^ ^ ^1 $GROWABLE run return 0")

		rollTable(table)

		raw("execute positioned ^ ^ ^1 run particle minecraft:flame ~ ~0.5 ~ 0.3 0.2 0.3 0.01 6")
		raw("execute positioned ^ ^ ^1 run playsound minecraft:block.netherrack.place block @a[distance=..12] ~ ~ ~ 0.4 1.2")
	}
}
