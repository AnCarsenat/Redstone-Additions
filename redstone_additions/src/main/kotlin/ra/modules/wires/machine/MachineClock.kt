package ra.modules.wires.machine

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.tesla_coil.TeslaCoil

/**
 * The once-a-second clock of the network machines that act on the world around them in steps:
 * the Tesla Coil (one zap a second).
 *
 * Same shape as the readout clock (ra_wires:readout) and the sensors' SensorPoll: [run] is
 * scheduled every [PERIOD] ticks and reschedules itself only while a marker tagged [TAG] is
 * loaded. `#mc.poll ra.wires.tmp2` is 1 while a run is scheduled; the per-tick check in [tick]
 * starts the clock when it is 0 and such a marker exists (one placed, a chunk loading, a world
 * loaded with the schedule lost), and the module load resets it.
 *
 * NOTHING PER TICK BUT THE BREAK CHECKS. A coil with no mob near it costs a handful of commands once a second, and nothing at all when none is loaded.
 */
object MachineClock : PackPart {
	/** Ticks between two runs. */
	const val PERIOD = 20

	/** On every Tesla Coil marker: what the clock runs over. */
	const val TAG = "ra.wires.clocked"

	val tick = ref(Ns.WIRES, "machine/tick")
	val run = ref(Ns.WIRES, "machine/run")
	private val start = ref(Ns.WIRES, "machine/start")

	private const val FLAG = "#mc.poll"

	/** In the module load. */
	fun Function.resetClock() = raw("scoreboard players set $FLAG ra.wires.tmp2 0")

	override fun Ra.register() {
		pack.define(MachineClock.tick) {
			// Break checks: the vanilla drop must be caught within a few ticks (killVanillaDrop).
			executeRun("as @e[type=marker,tag=ra.custom_block.${TeslaCoil.ID}] at @s unless block ~ ~ ~ ${TeslaCoil.BLOCK}", TeslaCoil.onBreak)
			executeRun("unless score $FLAG ra.wires.tmp2 matches 1 if entity @e[type=marker,tag=$TAG,limit=1]", start)
		}

		pack.define(start) {
			raw("scoreboard players set $FLAG ra.wires.tmp2 1")
			schedule(run).replace(PERIOD.ticks)
		}

		pack.define(MachineClock.run) {
			raw("scoreboard players set $FLAG ra.wires.tmp2 0")
			executeRun("as @e[type=marker,tag=ra.custom_block.${TeslaCoil.ID}] at @s", TeslaCoil.run)
			executeReturnRun("if entity @e[type=marker,tag=$TAG,limit=1]", start)
		}
	}
}
