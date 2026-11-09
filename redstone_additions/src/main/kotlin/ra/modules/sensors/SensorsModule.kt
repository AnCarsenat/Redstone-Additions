package ra.modules.sensors

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.sensors.EntityDetector.registerEntityDetector
import ra.modules.sensors.LightSensor.registerLightSensor
import ra.modules.sensors.WeatherSensor.registerWeatherSensor
import ra.modules.sensors.HeldItemDetector.registerHeldItemDetector
import ra.modules.sensors.PlayerDetector.registerPlayerDetector
import ra.modules.sensors.SensorPoll.registerSensorPoll
import ra.modules.sensors.SensorPoll.resetFlag
import ra.modules.sensors.TagAdder.registerTagAdder
import ra.modules.sensors.TagRemover.registerTagRemover
import ra.modules.sensors.SensorTags.registerSensorTags
import ra.core.moduleLoaded
import ra.core.raMessage

/** Sensors (`ra_sensors`): entity detection, tag manipulation and world (light, weather) and player (held item, names) sensing blocks. */
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
		registerSensorTags()
		registerEntityDetector()
		registerTagAdder()
		registerTagRemover()
		registerLightSensor()
		registerWeatherSensor()
		registerHeldItemDetector()
		registerPlayerDetector()
		// After every polled block: one slow clock for all of them.
		registerSensorPoll(
			listOf(
				LightSensor.block to LightSensor.update, WeatherSensor.block to WeatherSensor.update,
				HeldItemDetector.block to HeldItemDetector.update, PlayerDetector.block to PlayerDetector.update,
			),
		)

		val blocks = listOf(EntityDetector.block, TagAdder.block, TagRemover.block, LightSensor.block, WeatherSensor.block,
			HeldItemDetector.block, PlayerDetector.block,
		)

		pack.define(load) {
			for (b in blocks) function(b.fn("register_block"))
			// A schedule that did not survive is started again by the next polled block's tick.
			resetFlag()
			moduleLoaded("Sensors loaded!", "aqua")
		}

		// Called every game tick from ra:tick.
		pack.define(tick!!) { for (b in blocks) function(b.tick) }

		pack.define(ref(ns, "items/give_all")) {
			for (b in blocks) function(b.give)
			raMessage("@s", "Given all sensor blocks!", "light_purple")
		}

		bundles += GiveBundle(
			"sensors", "Sensors Bundle", "Entity, tag, light, weather, held item and player sensor blocks", ns,
			listOf(EntityDetector.item, TagAdder.item, TagRemover.item, LightSensor.item, WeatherSensor.item, HeldItemDetector.item, PlayerDetector.item).map { it.stackNbt() },
		)
	}
}
