package ra.modules.audio

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.raw
import ra.core.ref
import ra.modules.audio.Alarm.registerAlarm
import ra.modules.audio.AudioPlayer.registerAudioPlayer
import ra.modules.audio.Doorbell.registerDoorbell
import ra.modules.audio.jukebox.JukeboxController
import ra.modules.audio.jukebox.JukeboxController.registerJukeboxController
import ra.modules.audio.jukebox.JukeboxOps.resetPoll

/**
 * Audio (`ra_audio`): blocks that make sound on a redstone signal. The Alarm and the Doorbell play
 * sounds from a fixed list ([AudioSounds]); the Jukebox Controller runs a vanilla jukebox.
 *
 * ADDING A BLOCK. One file per block in this package (or a sub-package), with a
 * `fun Ra.register<Block>()` called from [register]. Its `give`, `register_block` and `tick` go
 * into the lists below, its item into the bundle. Outside this package: a row on the settings page
 * (`ra/settings/pages/AudioPage.kt`) and an entry in the advancement tree (`audioTree`,
 * `ra/advancements/AudioTree.kt`).
 */
object AudioModule : GameplayModule {
	override val ns = Ns.AUDIO
	override val title = "Audio"

	override fun Ra.register() {
		registerAudioPlayer()
		registerAlarm()
		registerDoorbell()
		registerJukeboxController()

		val blocks = listOf(Alarm.block, Doorbell.block, JukeboxController.block)

		pack.define(ref(ns, "items/give_all")) {
			for (b in blocks) function(b.give)
		}
		bundles += GiveBundle(
			"audio", "Audio Bundle", "Alarms, doorbells and a jukebox controller", ns,
			listOf(Alarm.item, Doorbell.item, JukeboxController.item).map { it.stackNbt() },
		)

		pack.define(load) {
			raw("scoreboard objectives add ${AudioPlayer.STEP} dummy")
			// A schedule that did not survive is started again by the next playing controller's tick.
			resetPoll()
			for (b in blocks) function(b.fn("register_block"))
			moduleLoaded("Audio loaded!", "light_purple")
		}
		pack.define(tick!!) {
			for (b in blocks) function(b.tick)
		}
	}
}
