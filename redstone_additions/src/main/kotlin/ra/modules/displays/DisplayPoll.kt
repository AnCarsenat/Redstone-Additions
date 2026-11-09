package ra.modules.displays

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.markerSel
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

/**
 * The slow clock of the Displays module, the same shape as the sensors' (`SensorPoll`).
 *
 * What a display shows changes rarely (an item put in a hologram, a colour typed into the Data
 * Handler, a display killed by hand), so it is looked at once a second rather than every tick:
 * [run] is scheduled every [PERIOD] ticks, runs every loaded marker of every polled block's
 * `poll` function, then schedules itself again only while at least one is loaded. With none,
 * nothing is scheduled and the module costs one score test per block type per tick.
 *
 * `#dsp.poll ra.temp` is 1 while a run is scheduled. A polled block's tick ([startIfIdle]) starts
 * the clock when it is 0 and one of its markers is loaded: a block placed, a chunk holding one
 * loading, a world loaded with the schedule lost. Load resets the flag ([resetFlag]), and
 * `replace` makes a second start harmless.
 */
object DisplayPoll {
	/** Ticks between two runs. The Item Hologram's spin step lasts exactly this long. */
	const val PERIOD = 20

	val run = ref(Ns.DISPLAYS, "poll/run")
	private val start = ref(Ns.DISPLAYS, "poll/start")

	private const val FLAG = "#dsp.poll"

	/** In a polled block's tick: one score test while the clock runs. */
	fun Function.startIfIdle(block: CustomBlock) =
		executeRun("unless score $FLAG ${Obj.TEMP} matches 1 if entity ${block.markerSel("limit=1")}", start)

	/** In the module's load. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG ${Obj.TEMP} 0")

	/** [polled]: each polled block with its poll function (run as the marker, at it). */
	fun Ra.registerDisplayPoll(polled: List<Pair<CustomBlock, FunctionArgument>>) {
		pack.define(start) {
			raw("scoreboard players set $FLAG ${Obj.TEMP} 1")
			schedule(run).replace(PERIOD.ticks)
		}

		pack.define(run) {
			raw("scoreboard players set $FLAG ${Obj.TEMP} 0")
			for ((block, poll) in polled) executeRun("as ${block.markerSel()} at @s", poll)
			for ((block, _) in polled) executeReturnRun("if entity ${block.markerSel("limit=1")}", start)
		}
	}
}
