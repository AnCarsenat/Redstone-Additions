package ra.modules.sensors

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
 * The slow clock of the sensors that read the world (Light Sensor, Weather Sensor).
 *
 * Light and weather change slowly, so these sensors are read once a second rather than every
 * tick: [run] is scheduled every [PERIOD] ticks and updates every loaded marker of every polled
 * block, then schedules itself again only while at least one is loaded. With none, nothing is
 * scheduled.
 *
 * `#sn.poll ra.temp` is 1 while a run is scheduled. A polled block's tick ([startIfIdle]) starts
 * the clock when it is 0 and one of its markers is loaded, which covers a sensor placed, a chunk
 * holding one loading, and a world loaded with the schedule lost; load resets the flag to 0 so
 * a schedule that did not survive is started again (`replace` makes a second start harmless).
 */
object SensorPoll {
	const val PERIOD = 20

	val run = ref(Ns.SENSORS, "poll/run")
	private val start = ref(Ns.SENSORS, "poll/start")

	private const val FLAG = "#sn.poll"

	/** In a polled block's tick: one score test while the clock runs. */
	fun Function.startIfIdle(block: CustomBlock) =
		executeRun("unless score $FLAG ${Obj.TEMP} matches 1 if entity ${block.markerSel("limit=1")}", start)

	/** In the module's load. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG ${Obj.TEMP} 0")

	/** [polled]: each polled block with its update function (run as the marker, at it). */
	fun Ra.registerSensorPoll(polled: List<Pair<CustomBlock, FunctionArgument>>) {
		pack.define(start) {
			raw("scoreboard players set $FLAG ${Obj.TEMP} 1")
			schedule(run).replace(PERIOD.ticks)
		}

		pack.define(run) {
			raw("scoreboard players set $FLAG ${Obj.TEMP} 0")
			for ((block, update) in polled) executeRun("as ${block.markerSel()} at @s", update)
			for ((block, _) in polled) executeReturnRun("if entity ${block.markerSel("limit=1")}", start)
		}
	}
}
