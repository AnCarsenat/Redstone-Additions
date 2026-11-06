package ra.modules.gates

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.modules.gates.Clock.registerClock

/**
 * Logic Gates (`ra_gates`): the UNI Gate and the timing blocks (Clock, Delayer, Extender,
 * Randomizer, Shortener). Each block lives in its own file; this one wires them together.
 */
object GatesModule : GameplayModule {
	override val ns = Ns.GATES
	override val title = "Logic Gates"

	override fun Ra.register() {
		registerClock()
		// TODO(port): uni_gate, delayer, extender, randomizer, shortener, items/give_all, debug/*, check_signals.

		pack.define(load) {
			function(Clock.block.fn("register_block"))
			raw("""tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Logic Gates loaded!",color:"aqua"}]""")
		}
		pack.define(tick!!) {
			// Per-block redstone detection runs in each block's process function.
			function(Clock.block.tick)
		}
	}
}
