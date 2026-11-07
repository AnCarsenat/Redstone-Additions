package ra.lib.redstone

import ra.core.Ra
import ra.lib.redstone.Detect.registerDetect
import ra.lib.redstone.Local.registerLocal
import ra.lib.redstone.RedstoneTags.registerRedstoneTags
import ra.lib.redstone.Rules.registerRules
import ra.lib.redstone.SideReaders.registerSideReaders

/**
 * `ra_lib:redstone/...`: the redstone power readers. Cheapest first: `any` < `detect_switch` <
 * `local/<side>` < `side` < `detect` < `detect_local`; `count_inputs` asks presence, not power.
 */
object Redstone {
	val init = RedstoneTags.init

	fun Ra.registerRedstone() {
		registerRedstoneTags()
		registerRules()
		registerSideReaders()
		registerDetect()
		registerLocal()
	}
}
