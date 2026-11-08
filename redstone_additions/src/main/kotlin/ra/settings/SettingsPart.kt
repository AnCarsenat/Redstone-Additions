package ra.settings

import ra.core.PackPart
import ra.core.Ra
import ra.settings.act.ActFns.registerAct
import ra.settings.apply.ApplyFns.registerApply
import ra.settings.gen.SettingsGenerator
import ra.settings.page.PageFns.registerPage
import ra.settings.pages.SettingsPages
import ra.settings.placement.PlacementFns.registerPlacement
import ra.settings.purge.Purge.registerPurge
import ra.settings.row.RowFns.registerRows

/**
 * Settings (`ra_settings`): pack-wide (global) and per-player (user) settings.
 *
 * The runtime -- menu, dispatchers, typed input, the read API -- is hand-written in this package.
 * Everything that depends on WHICH settings exist (the menu registry, defaults, the operator
 * function tree, uninstall) is generated from [SettingsPages] by [SettingsGenerator], which
 * replaces the beet build's `tools/settings_gen.py`.
 */
object SettingsPart : PackPart {
	override fun Ra.register() {
		registerLifecycle()
		registerRead()
		registerMenu()
		registerPage()
		registerRows()
		registerAct()
		registerEdit()
		registerApply()
		registerApplyStr()
		registerAdmin()
		registerDisabled()
		registerPlacement()
		registerPurge()
		SettingsGenerator(pack, SettingsPages.all).generate()
	}
}
