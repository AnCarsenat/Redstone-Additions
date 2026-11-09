package ra.modules.security

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.ref
import ra.modules.security.keypad.Keypad
import ra.modules.security.keypad.Keypad.registerKeypad
import ra.modules.security.keypad.KeypadUse.resetFlag

/**
 * Security (`ra_security`): blocks that keep a build closed to whoever lacks the code.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package for a larger one), with a
 * `fun Ra.register<Block>()` called from [register]. The block's `give`, `register_block` and
 * `tick` go into the module's `items/give_all`, `load` and `tick` below, its item into the bundle.
 * Outside this package: a row on the settings page (`ra/settings/pages/SecurityPage.kt`) and an
 * entry in the advancement tree (`securityTree`, `ra/advancements/SecurityTree.kt`).
 *
 * Codes are Automation's Pattern items (`ra.modules.automation.pattern.Pattern`).
 */
object SecurityModule : GameplayModule {
	override val ns = Ns.SECURITY
	override val title = "Security"

	override fun Ra.register() {
		registerKeypad()

		pack.define(ref(ns, "items/give_all")) {
			function(Keypad.block.give)
		}
		bundles += GiveBundle(
			"security", "Security Bundle", "Blocks that open only for the right code", ns,
			listOf(Keypad.item.stackNbt()),
		)

		pack.define(load) {
			// A schedule that did not survive is started again by the next timed keypad's tick.
			resetFlag()
			function(Keypad.registerBlock)
			moduleLoaded("Security loaded!", "dark_red")
		}
		pack.define(tick!!) {
			function(Keypad.block.tick)
		}
	}
}
