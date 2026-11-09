package ra.modules.gates

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.ref
import ra.modules.gates.Clock.registerClock
import ra.modules.gates.Counter.registerCounter
import ra.modules.gates.Delayer.registerDelayer
import ra.modules.gates.EdgeDetector.registerEdgeDetector
import ra.modules.gates.Extender.registerExtender
import ra.modules.gates.Randomizer.registerRandomizer
import ra.modules.gates.Sequencer.registerSequencer
import ra.modules.gates.Shortener.registerShortener
import ra.modules.gates.SignalCalculator.registerSignalCalculator
import ra.modules.gates.UniGate.registerUniGate
import ra.modules.gates.debug.ClockDebug.registerClockDebug

/**
 * Logic Gates (`ra_gates`): the UNI Gate and the timing blocks (Clock, Delayer, Extender,
 * Randomizer, Shortener, Counter, Edge Detector) and the
 * analog blocks (Signal Calculator, Sequencer). Each block lives in its own file; this one
 * wires them together.
 */
object GatesModule : GameplayModule {
	override val ns = Ns.GATES
	override val title = "Logic Gates"

	/** Every block, in the order load, tick, give_all and the bundle list them. */
	private val gateBlocks = listOf(UniGate.block, Clock.block, Delayer.block, Extender.block, Randomizer.block, Shortener.block, Counter.block, EdgeDetector.block, SignalCalculator.block, Sequencer.block)

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerUniGate()
		registerClock()
		registerDelayer()
		registerExtender()
		registerRandomizer()
		registerShortener()
		registerCounter()
		registerEdgeDetector()
		registerSignalCalculator()
		registerSequencer()
		pack.defineAnalogOutput()
		registerClockDebug()

		pack.define(load) {
			for (b in gateBlocks) function(b.fn("register_block"))
			moduleLoaded("Logic Gates loaded!", "aqua")
		}
		pack.define(tick!!) {
			// Per-block redstone detection runs in each block's process function.
			for (b in gateBlocks) function(b.tick)
		}

		// /function ra_gates:items/give_all
		pack.define(ref(ns, "items/give_all")) {
			for (b in gateBlocks) function(b.give)
		}

		// kept: beet contract. Deprecated compatibility entry point: gates now run per-entity
		// redstone detection directly in their process functions. Kept because other packs may
		// still call it.
		pack.define(ref(ns, "check_signals")) { returnValue(1) }

		bundles += GiveBundle("gates", "Gates Bundle", "UNI Gate + timing utility blocks", ns, gateBlocks.map { it.itemStackNbt() })
	}
}
