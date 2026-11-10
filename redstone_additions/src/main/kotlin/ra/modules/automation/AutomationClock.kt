package ra.modules.automation

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.automation.absorber.XpAbsorber
import ra.modules.automation.uncrafter.Uncrafter

/**
 * The once-a-second clock of the Uncrafter and the Experience Absorber.
 *
 * Same shape as the wires' machine clock: [run] is scheduled every [PERIOD] ticks and reschedules
 * itself only while a marker tagged [TAG] is loaded. `#auto.clock ra.temp` is 1 while a run is
 * scheduled; [startIfIdle], in the module tick, starts the clock when it is 0 and such a marker
 * exists (one placed, a chunk loading, a world loaded with the schedule lost), and the module
 * load resets it ([resetClock]).
 *
 * NOTHING PER TICK BUT THE BREAK CHECKS AND ONE SCORE TEST.
 */
object AutomationClock {
	const val PERIOD = 20

	/** On every Uncrafter and Experience Absorber marker, set at placement. */
	const val TAG = "ra.auto.clocked"

	private const val FLAG = "#auto.clock"
	private const val T = Obj.TEMP

	private val start = ref(Ns.AUTOMATION, "clock/start")
	private val run = ref(Ns.AUTOMATION, "clock/run")

	fun Function.resetClock() = raw("scoreboard players set $FLAG $T 0")

	fun Function.startIfIdle() = executeRun("unless score $FLAG $T matches 1 if entity @e[type=minecraft:marker,tag=$TAG,limit=1]", start)

	fun Ra.registerAutomationClock() {
		pack.define(start) {
			raw("scoreboard players set $FLAG $T 1")
			schedule(run).replace(PERIOD.ticks)
		}
		pack.define(run) {
			raw("scoreboard players set $FLAG $T 0")
			raw("execute as @e[type=minecraft:marker,tag=${Uncrafter.block.tag}] at @s run function ${Uncrafter.run.asId()}")
			raw("execute as @e[type=minecraft:marker,tag=${XpAbsorber.block.tag}] at @s run function ${XpAbsorber.run.asId()}")
			executeRun("if entity @e[type=minecraft:marker,tag=$TAG,limit=1]", start)
		}
	}
}
