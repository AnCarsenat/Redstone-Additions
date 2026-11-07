package ra.modules.infinite

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.modules.enchanting.hookEnchantRecipes
import ra.core.ref
import ra.modules.infinite.InfiniteItems.registerInfiniteItems
import ra.modules.infinite.InfiniteTags.registerInfiniteTags
import ra.modules.infinite.MineralGenerator.registerMineralGenerator
import ra.modules.infinite.NetherGenerator.registerNetherGenerator
import ra.modules.infinite.PoppyGenerator.registerPoppyGenerator
import ra.modules.infinite.debug.PoppyDebug.registerPoppyDebug
import ra.core.moduleLoaded

/** Infinite Generators (`ra_infinite`): generators that grow their own material, forever. */
object InfiniteModule : GameplayModule {
	override val ns = Ns.INFINITE
	override val title = "Infinite Generators"

	private val generators = listOf(MineralGenerator.generator, NetherGenerator.generator, PoppyGenerator.generator)

	override fun Ra.register() {
		pack.registerInfiniteTags()
		registerInfiniteItems()
		registerMineralGenerator()
		registerNetherGenerator()
		registerPoppyGenerator()
		registerPoppyDebug()

		// Ahead of ra_interactive's list, as in the beet tag: see hookEnchantRecipes.
		pack.hookEnchantRecipes(InfiniteItems.enchantRecipes)

		pack.define(ref(ns, "items/give_all")) {
			function(InfiniteItems.giveCasing)
			for (core in InfiniteItems.cores) function(core.give)
			for (g in generators) function(g.block.give)
		}

		// The bundle's cores carry no lore, unlike the ones `give` hands out.
		bundles += GiveBundle(
			id = "infinite",
			title = "Infinite Bundle",
			lore = "Generators that grow their own material",
			ns = ns,
			items = listOf(InfiniteItems.casing.stackNbt()) +
				InfiniteItems.cores.map { it.item.without("lore").stackNbt() } +
				generators.map { it.item.stackNbt() },
		)

		pack.define(load) {
			for (g in generators) function(g.registerBlock)
			moduleLoaded("Infinite generators loaded!", "aqua")
		}
		pack.define(tick!!) {
			for (g in generators) function(g.block.tick)
		}
	}
}
