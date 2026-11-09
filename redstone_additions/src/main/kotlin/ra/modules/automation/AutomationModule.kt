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
import ra.modules.automation.pattern.Pattern
import ra.modules.automation.pattern.Pattern.registerPattern
import ra.modules.automation.pattern.PatternUse

/**
 * Automation (`ra_automation`): blocks and items that run vanilla machines for you.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/AutomationPage.kt`) and an
 * entry in the advancement tree (`automationTree`, `ra/advancements/AutomationTree.kt`).
 */
object AutomationModule : GameplayModule {
	override val ns = Ns.AUTOMATION
	override val title = "Automation"

	override fun Ra.register() {
		registerPattern()

		pack.define(ref(ns, "items/give_all")) {
			function(Pattern.give)
		}
		bundles += GiveBundle(
			"automation", "Automation Bundle", "Machines that run vanilla blocks for you", ns,
			listOf(Pattern.item.stackNbt()),
		)

		pack.define(load) {
			raw("scoreboard objectives add ${PatternUse.USED} dummy")
			moduleLoaded("Automation loaded!", "gold")
		}
		pack.define(tick!!) {}
	}
}
