package ra.modules.wireless

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.wireless.Emitter.registerEmitter
import ra.modules.wireless.Receiver.registerReceiver
import ra.modules.wireless.AnalogEmitter.registerAnalogEmitter
import ra.modules.wireless.AnalogReceiver.registerAnalogReceiver
import ra.modules.wireless.Channel.registerChannel
import ra.modules.wireless.tools.Remote
import ra.modules.wireless.tools.Remote.registerRemote
import ra.core.moduleLoaded
import ra.core.executeRun

/** Wireless Redstone (`ra_wireless`): emitters and receivers (on/off and analog) on string channels, and the Redstone Remote. */
object WirelessModule : GameplayModule {
	override val ns = Ns.WIRELESS
	override val title = "Wireless Redstone"

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerEmitter()
		registerReceiver()
		registerAnalogEmitter()
		registerAnalogReceiver()
		registerChannel()
		registerRemote()

		pack.define(load) {
			// Pulse timer (remote pulse duration).
			scoreboard.objectives.add(Receiver.PULSE_TIMER)
			scoreboard.objectives.add(Remote.PENDING)
			scoreboard.objectives.add(Remote.SLOT)
			scoreboard.objectives.add(Remote.REQ)

			function(Emitter.block.fn("register_block"))
			function(Receiver.block.fn("register_block"))
			function(AnalogEmitter.block.fn("register_block"))
			function(AnalogReceiver.block.fn("register_block"))
			scoreboard.objectives.add(Channel.LEVEL)

			moduleLoaded("Wireless Redstone loaded!", "aqua")
		}

		// Called every game tick from ra:tick.
		pack.define(tick!!) {
			function(Emitter.block.tick)
			function(Receiver.block.tick)
			function(AnalogEmitter.block.tick)
			function(AnalogReceiver.block.tick)

			// Finish channel entry for anyone who has a book request outstanding.
			executeRun("as @a[scores={${Remote.PENDING}=1..}]", Remote.applyPending)
		}

		pack.define(ref(ns, "items/give_all")) {
			function(Emitter.block.give)
			function(Receiver.block.give)
			function(AnalogEmitter.block.give)
			function(AnalogReceiver.block.give)
			function(Remote.give)
		}

		bundles += GiveBundle(
			"wireless", "Wireless Bundle", "Emitters, receivers, and remote", ns,
			listOf(
				Emitter.item.stackNbt(), Receiver.item.stackNbt(), AnalogEmitter.item.stackNbt(),
				AnalogReceiver.item.stackNbt(), Remote.item.stackNbt(),
			),
		)
	}
}
