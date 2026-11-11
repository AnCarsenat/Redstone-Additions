package ra.modules.automation

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.raw
import ra.core.ref
import ra.modules.automation.crafter.CrafterController
import ra.modules.automation.crafter.CrafterController.registerCrafterController
import ra.modules.automation.crafter.CrafterControllerRun.resetFlag
import ra.modules.automation.pattern.Pattern
import ra.modules.automation.pattern.Pattern.registerPattern
import ra.modules.automation.pattern.PatternUse
import ra.modules.automation.splitter.ItemSplitter
import ra.modules.automation.splitter.ItemSplitter.registerItemSplitter
import ra.modules.automation.AutomationClock.registerAutomationClock
import ra.modules.automation.AutomationClock.startClock
import ra.modules.automation.absorber.XpAbsorber
import ra.modules.automation.absorber.XpAbsorber.registerXpAbsorber
import ra.modules.automation.conveyor.Conveyor
import ra.modules.automation.conveyor.Conveyor.registerConveyor
import ra.modules.automation.fakeplayer.FakePlayer
import ra.modules.automation.fakeplayer.FakePlayer.registerFakePlayer
import ra.modules.automation.fakeplayer.FakePlayer.registerFakePlayerLeftovers
import ra.modules.automation.fakeplayer.FakePlayerSpawner
import ra.modules.automation.uncrafter.UncraftTable
import ra.modules.automation.uncrafter.Uncrafter
import ra.modules.automation.uncrafter.Uncrafter.registerUncrafter
import ra.modules.automation.uncrafter.UncrafterRun

/**
 * Automation (`ra_automation`): blocks and items that run vanilla machines for you.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/AutomationPage.kt`) and an
 * entry in the advancement tree (`automationTree`, `ra/advancements/AutomationTree.kt`).
 *
 * The Uncrafter, the Experience Absorber and the Item Pipe Splitter run on [AutomationClock]: a block of theirs tags its
 * marker [AutomationClock.TAG] at placement and adds itself to the clock's run.
 *
 * The [Pattern] item is shared: a block that takes Patterns adds itself to [Pattern.targets].
 *
 * The Fake Player is disabled ([FakePlayer.ENABLED]): its code is kept, and only its break check
 * and mob sweep are registered, for worlds that already have some.
 */
object AutomationModule : GameplayModule {
	override val ns = Ns.AUTOMATION
	override val title = "Automation"

	override fun Ra.register() {
		registerPattern()
		registerCrafterController()
		registerUncrafter()
		registerXpAbsorber()
		registerItemSplitter()
		// Disabled: see FakePlayer.ENABLED. Only the break check and the mob sweep stay.
		if (FakePlayer.ENABLED) registerFakePlayer() else registerFakePlayerLeftovers()
		registerConveyor()
		registerAutomationClock()

		pack.define(ref(ns, "items/give_all")) {
			function(CrafterController.block.give)
			function(Pattern.give)
			function(Uncrafter.block.give)
			function(XpAbsorber.block.give)
			function(ItemSplitter.block.give)
			if (FakePlayer.ENABLED) function(FakePlayer.block.give)
			function(Conveyor.block.give)
		}
		bundles += GiveBundle(
			"automation", "Automation Bundle", "Machines that run vanilla blocks for you", ns,
			listOfNotNull(
				CrafterController.item.stackNbt(), Pattern.item.stackNbt(),
				Uncrafter.item.stackNbt(), XpAbsorber.item.stackNbt(), ItemSplitter.item.stackNbt(),
				FakePlayer.item.stackNbt().takeIf { FakePlayer.ENABLED }, Conveyor.item.stackNbt(),
			),
		)

		pack.define(load) {
			raw("scoreboard objectives add ${PatternUse.USED} dummy")
			if (FakePlayer.ENABLED) function(FakePlayerSpawner.load)
			resetFlag()
			startClock()
			raw("data modify storage ${UncrafterRun.TABLE} map set value ${UncraftTable.snbt()}")
			function(CrafterController.registerBlock)
			function(Uncrafter.registerBlock)
			function(XpAbsorber.registerBlock)
			function(ItemSplitter.registerBlock)
			if (FakePlayer.ENABLED) function(FakePlayer.registerBlock)
			function(Conveyor.registerBlock)
			moduleLoaded("Automation loaded!", "gold")
		}
		pack.define(tick!!) {
			function(CrafterController.block.tick)
			function(Uncrafter.block.tick)
			function(XpAbsorber.block.tick)
			function(ItemSplitter.block.tick)
			function(FakePlayer.block.tick)
			function(Conveyor.block.tick)
		}
	}
}
