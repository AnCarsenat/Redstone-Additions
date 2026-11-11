package ra.modules.automation

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.automation.absorber.XpAbsorber
import ra.modules.automation.fakeplayer.FakePlayer
import ra.modules.automation.splitter.ItemSplitter
import ra.modules.automation.uncrafter.Uncrafter

/**
 * The once-a-second clock of the Uncrafter, the Experience Absorber, the Item Pipe Splitter and the Fake Player.
 *
 * NOTHING PER TICK. [run] is scheduled every [PERIOD] ticks from the module load ([startClock])
 * and always reschedules itself, so there is no per-tick test for "is one loaded" (a world-wide
 * selector every tick for nobody). With none loaded a run is its three `execute as` selectors and
 * the schedule, once a second; a block placed or a chunk loaded is picked up at the next run.
 *
 * [TAG] is on every Uncrafter, Experience Absorber, Item Pipe Splitter and Fake Player marker, set at placement, so tools and
 * add-ons can find the clocked blocks in one selector.
 */
object AutomationClock {
	const val PERIOD = 20

	/** On every Uncrafter, Experience Absorber, Item Pipe Splitter and Fake Player marker, set at placement. */
	const val TAG = "ra.auto.clocked"

	private val run = ref(Ns.AUTOMATION, "clock/run")

	/** In the module load: (re)start the schedule. `replace`, so a reload never runs it twice. */
	fun Function.startClock() = schedule(run).replace(PERIOD.ticks)

	fun Ra.registerAutomationClock() {
		pack.define(run) {
			raw("execute as @e[type=minecraft:marker,tag=${Uncrafter.block.tag}] at @s run function ${Uncrafter.run.asId()}")
			raw("execute as @e[type=minecraft:marker,tag=${XpAbsorber.block.tag}] at @s run function ${XpAbsorber.run.asId()}")
			raw("execute as @e[type=minecraft:marker,tag=${ItemSplitter.block.tag}] at @s run function ${ItemSplitter.run.asId()}")
			raw("execute as @e[type=minecraft:marker,tag=${FakePlayer.block.tag}] at @s run function ${FakePlayer.run.asId()}")
			// Mobs a Fake Player spawned that none has held for a while go back to the vanilla rules.
			raw("execute if entity @e[tag=${FakePlayer.MOB},limit=1] run function ${FakePlayer.releaseCheck.asId()}")
			schedule(run).replace(PERIOD.ticks)
		}
	}
}
