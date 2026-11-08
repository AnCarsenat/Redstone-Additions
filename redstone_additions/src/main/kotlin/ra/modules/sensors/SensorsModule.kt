package ra.modules.sensors

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.sensors.EntityDetector.registerEntityDetector
import ra.modules.sensors.TagAdder.registerTagAdder
import ra.modules.sensors.TagRemover.registerTagRemover
import ra.core.moduleLoaded
import ra.core.raMessage

/** Sensors (`ra_sensors`): entity detection and tag manipulation blocks. */
object SensorsModule : GameplayModule {
	override val ns = Ns.SENSORS
	override val title = "Sensors"

	/**
	 * The GLOBAL setting (Sensors page) that lets players without server-settings access edit
	 * entity selectors and tags. Off by default: see TextFormat for what a selector can reach.
	 */
	const val SELECTOR_SETTING = "player_selectors"

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerEntityDetector()
		registerTagAdder()
		registerTagRemover()

		val blocks = listOf(EntityDetector.block, TagAdder.block, TagRemover.block)

		pack.define(load) {
			for (b in blocks) function(b.fn("register_block"))
			moduleLoaded("Sensors loaded!", "aqua")
		}

		// Called every game tick from ra:tick.
		pack.define(tick!!) { for (b in blocks) function(b.tick) }

		pack.define(ref(ns, "items/give_all")) {
			for (b in blocks) function(b.give)
			raMessage("@s", "Given all sensor blocks!", "light_purple")
		}

		bundles += GiveBundle(
			"sensors", "Sensors Bundle", "Entity detector and tag blocks", ns,
			listOf(EntityDetector.item, TagAdder.item, TagRemover.item).map { it.stackNbt() },
		)
	}
}
