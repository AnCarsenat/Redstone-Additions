package ra.modules.farming

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.raMessage
import ra.core.raw
import ra.core.ref
import ra.modules.farming.Breeder.registerBreeder
import ra.modules.farming.Sprinkler.registerSprinkler
import ra.modules.farming.net.CaptureNet
import ra.modules.farming.net.CaptureNet.registerCaptureNet
import ra.modules.farming.net.CaptureNetUse
import ra.modules.farming.tree.TreeFarm
import ra.modules.farming.tree.TreeFarm.registerTreeFarm

/**
 * Farming (`ra_farming`): blocks that tend crops and animals.
 *
 * THE BREEDER KEEPS ITS OLD IDS. It moved here from Interactive Machines in v6.0.3, but its block
 * is still `CustomBlock(Ns.INTERACTIVE, "breeder", ...)`: its functions (`ra_interactive:blocks/
 * breeder/...`), its recipe (`ra_interactive:breeder`) and its entity tag
 * (`ra_interactive:breedable`) did not change, so placed Breeders and unlocked recipes carry on.
 * Only the module that registers, ticks and hands it out, its docs and its advancement moved.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/FarmingPage.kt`) and an
 * entry in the advancement tree (`farmingTree`, `ra/advancements/FarmingTree.kt`).
 */
object FarmingModule : GameplayModule {
	override val ns = Ns.FARMING
	override val title = "Farming"

	override fun Ra.register() {
		registerBreeder()
		registerSprinkler()
		registerTreeFarm()
		registerCaptureNet()

		val blocks = listOf(Breeder.block to Breeder.item, Sprinkler.block to Sprinkler.item, TreeFarm.block to TreeFarm.item)

		pack.define(load) {
			raw("scoreboard objectives add ${CaptureNetUse.USED} dummy")
			function(Breeder.registerBlock)
			function(Sprinkler.registerBlock)
			function(TreeFarm.registerBlock)
			moduleLoaded("Farming loaded!", "green")
		}
		// The Sprinkler's and the Tree Farm's work runs on the network machines' clock
		// (ra_wires:machine/run); their tick is only the break check.
		pack.define(tick!!) {
			for ((b, _) in blocks) function(b.tick)
		}

		// /function ra_farming:items/give_all
		pack.define(ref(ns, "items/give_all")) {
			for ((b, _) in blocks) function(b.give)
			raMessage("@s", "Given all custom blocks!", "green")
		}

		bundles += GiveBundle(
			"farming", "Farming Bundle", "${blocks.size} blocks and the Capture Nets, for crops, trees and animals", ns,
			blocks.map { it.second.stackNbt() } + listOf(CaptureNet.item.stackNbt(), CaptureNet.masterItem.stackNbt()),
		)
	}
}
