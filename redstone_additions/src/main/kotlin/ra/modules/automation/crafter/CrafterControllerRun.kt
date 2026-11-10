package ra.modules.automation.crafter

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.automation.crafter.CrafterFill.registerCrafterFill

/**
 * When and how a [CrafterController] looks at its crafter.
 *
 * NO IDLE COST. A controller with a loaded, valid Pattern carries [ARMED]. Only armed controllers
 * are looked at, every [PERIOD] ticks, by [run], which reschedules itself while one is loaded;
 * with none, nothing is scheduled ([startIfIdle] restarts it, as the sensors' slow clock does).
 * One look ([check]) stops at the first of: switched off, no crafter next to it, an empty
 * crafter, a crafter already powered, contents unchanged since the last look (`data.data.last`,
 * one NBT comparison). Only then are the nine slots compared.
 *
 * THE COMPARISON pastes the Pattern's ids into `execute if items block ~ ~ ~ container.<n> <id>`
 * ([match], a macro). Every id was checked by `ra_lib:safe/check` (format ID) when the Pattern was
 * loaded ([arm]), into `data.data.match`, `minecraft:air` standing for an empty slot; when
 * `data.properties.pattern` no longer equals `data.data.checked` (the Creative Data Handler
 * edited it), the look re-arms first.
 */
object CrafterControllerRun {
	/** `storage ra_automation:crafter`: scratch. */
	const val STORE = "ra_automation:crafter"

	const val PERIOD = 10
	const val PULSE_TICKS = 2

	/** On a controller with a valid, non-blank Pattern loaded. */
	const val ARMED = "ra.auto.cc_armed"
	/** On a controller powering itself this moment. */
	private const val PULSE = "ra.auto.cc_pulse"
	private const val FLAG = "#auto.cc_poll"
	private const val T = Obj.TEMP
	private const val EMPTY = "minecraft:air"

	private fun fn(path: String) = CrafterController.block.fn(path)

	/** As a controller: validate `data.properties.pattern` into `data.data.match`, tag [ARMED] or not. */
	val arm = fn("arm")
	/** `storage ra_automation:crafter slots` -> how many are non-empty strings (0 for a list that is not nine long). */
	val count = fn("count")
	private val start = fn("poll/start")
	private val run = fn("poll/run")
	private val check = fn("check")
	private val atCrafter = fn("at_crafter")
	/** As the controller, at its crafter: compare with the last look, match, fire. Shared with [CrafterFill]. */
	val compareAndFire = fn("compare_and_fire")
	private val match = fn("match")
	private val unpulse = fn("unpulse")
	private val armSlot = fn("arm_slot")

	private fun armedSel(extra: String = "") = "@e[type=minecraft:marker,tag=${CrafterController.block.tag},tag=$ARMED$extra]"

	/** In the block's tick: one score test while the clock runs. */
	fun Function.startIfIdle() = executeRun("unless score $FLAG $T matches 1 if entity ${armedSel(",limit=1")}", start)

	/** In the module's load: a schedule that did not survive is started again by [startIfIdle]. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG $T 0")

	/** Up first (a controller under its crafter), then the sides, then down. */
	val directions = listOf("~ ~1 ~", "~ ~ ~-1", "~ ~ ~1", "~1 ~ ~", "~-1 ~ ~", "~ ~-1 ~")

	fun Ra.registerCrafterControllerRun() {
		pack.define(start) {
			raw("scoreboard players set $FLAG $T 1")
			schedule(run).replace(PERIOD.ticks)
		}
		pack.define(run) {
			raw("scoreboard players set $FLAG $T 0")
			raw("execute as ${armedSel()} at @s run function ${check.asId()}")
			raw("data remove storage $STORE d")
			executeRun("if entity ${armedSel(",limit=1")}", start)
		}

		pack.define(count) {
			raw("execute unless data storage $STORE slots[8] run return 0")
			raw("execute if data storage $STORE slots[9] run return 0")
			raw("scoreboard players set #cc.n $T 0")
			for (n in 0 until 9) {
				raw("data modify storage $STORE cur set from storage $STORE slots[$n]")
				raw("execute unless data storage $STORE {cur:\"\"} run scoreboard players add #cc.n $T 1")
			}
			raw("data remove storage $STORE cur")
			raw("return run scoreboard players get #cc.n $T")
		}

		pack.define(arm) { arm() }
		registerCrafterFill()
		pack.define(armSlot) { armSlot() }

		// As an armed controller, at it.
		pack.define(check) {
			raw("execute if entity @s[tag=$PULSE] run function ${unpulse.asId()}")
			raw("data modify storage $STORE d set from entity @s data")
			raw("execute if data storage $STORE d.properties{enabled:0b} run return run data modify entity @s data.status.state set value \"switched off\"")
			raw("execute store success score #cc.chg $T run data modify storage $STORE d.data.checked set from storage $STORE d.properties.pattern")
			raw("execute if score #cc.chg $T matches 1 run return run function ${arm.asId()}")
			raw("execute if entity @s[tag=${CrafterFill.FILL}] run return run function ${CrafterFill.check.asId()}")
			for (d in directions) raw("execute if block $d minecraft:crafter positioned $d run return run function ${atCrafter.asId()}")
			raw("data modify entity @s data.status.state set value \"no crafter next to it\"")
		}

		// As the controller, at the crafter.
		pack.define(atCrafter) {
			raw("execute unless items block ~ ~ ~ container.* * run data remove entity @s data.data.last")
			raw("execute unless items block ~ ~ ~ container.* * run return run data modify entity @s data.status.state set value \"crafter empty\"")
			raw("execute if block ~ ~ ~ minecraft:crafter[triggered=true] run return run data modify entity @s data.status.state set value \"crafter powered\"")
			function(compareAndFire)
		}

		// As the controller, at the crafter, which is not empty and not powered.
		pack.define(compareAndFire) {
			raw("execute store success score #cc.chg $T run data modify storage $STORE d.data.last set from block ~ ~ ~ Items")
			raw("execute if score #cc.chg $T matches 0 run return 0")
			raw("data modify entity @s data.data.last set from storage $STORE d.data.last")
			raw("data modify storage $STORE m set from storage $STORE d.data.match")
			raw("execute store result score #cc.ok $T run function ${match.asId()} with storage $STORE m")
			raw("data remove storage $STORE m")
			raw("execute unless score #cc.ok $T matches 1 run return run data modify entity @s data.status.state set value \"waiting for the ingredients\"")
			// The edge: the controller powers itself, and the crafter beside it crafts.
			raw("execute at @s if block ~ ~ ~ minecraft:target run setblock ~ ~ ~ minecraft:target[power=15]")
			raw("tag @s add $PULSE")
			schedule(unpulse).replace(PULSE_TICKS.ticks)
			raw("data modify entity @s data.status.state set value \"crafting\"")
			raw("execute store result score #cc.n $T run data get entity @s data.status.crafts")
			raw("execute store result entity @s data.status.crafts int 1 run scoreboard players add #cc.n $T 1")
		}

		// {s0..s8}: ids that passed ra_lib:safe/check in format ID, or minecraft:air for an empty slot.
		// As the controller, at the crafter.
		pack.define(match) {
			for (n in 0 until 9) {
				raw("execute if data storage $STORE m{s$n:\"$EMPTY\"} if items block ~ ~ ~ container.$n * run return 0")
				raw("\$execute unless data storage $STORE m{s$n:\"$EMPTY\"} unless items block ~ ~ ~ container.$n \$(s$n) run return 0")
			}
			raw("return 1")
		}

		// Scheduled, or from a look that finds the tag left by an unloaded chunk.
		pack.define(unpulse) {
			raw("execute as @e[type=minecraft:marker,tag=$PULSE] at @s if block ~ ~ ~ minecraft:target run setblock ~ ~ ~ minecraft:target[power=0]")
			raw("tag @e[type=minecraft:marker,tag=$PULSE] remove $PULSE")
		}
	}

	private fun Function.arm() {
		raw("tag @s remove $ARMED")
		raw("tag @s remove ${CrafterFill.FILL}")
		raw("data remove entity @s data.data.match")
		raw("data remove entity @s data.data.last")
		raw("data modify entity @s data.data.checked set from entity @s data.properties.pattern")
		raw("data modify storage $STORE slots set from entity @s data.properties.pattern")
		raw("execute store result score #cc.n $T run function ${count.asId()}")
		raw("execute if score #cc.n $T matches ..0 unless data entity @s data.data.item run data modify entity @s data.status.state set value \"no pattern\"")
		raw("execute if score #cc.n $T matches ..0 if data entity @s data.data.item run data modify entity @s data.status.state set value \"pattern unreadable\"")
		raw("execute if score #cc.n $T matches ..0 run return run data remove storage $STORE slots")

		raw("data modify storage $STORE m set value {}")
		raw("scoreboard players set #cc.bad $T 0")
		for (n in 0 until 9) {
			raw("data modify storage $STORE cur set from storage $STORE slots[$n]")
			raw("function ${armSlot.asId()}")
			raw("data modify storage $STORE m.s$n set from storage $STORE cur")
		}
		raw("data remove storage $STORE slots")
		raw("data remove storage $STORE cur")
		raw("execute if score #cc.bad $T matches 1 run data modify entity @s data.status.state set value \"pattern unreadable\"")
		raw("execute if score #cc.bad $T matches 1 run return run data remove storage $STORE m")
		raw("data modify entity @s data.data.match set from storage $STORE m")
		raw("data remove storage $STORE m")
		raw("tag @s add $ARMED")
		// A Recipe Pattern (the flag on the item it holds): it fills the crafter itself.
		raw("scoreboard players set #cc.rcp $T 0")
		raw("execute store result score #cc.rcp $T run data get entity @s data.data.item.components.\"minecraft:custom_data\".ra.pattern.recipe")
		raw("execute if score #cc.rcp $T matches 1 run tag @s add ${CrafterFill.FILL}")
		raw("data modify entity @s data.status.state set value \"waiting for the ingredients\"")
	}

	/** `storage cur`: "" becomes [EMPTY]; anything else must be an id in format ID, or `#cc.bad` is set. */
	private fun Function.armSlot() {
		raw("execute if data storage $STORE {cur:\"\"} run return run data modify storage $STORE cur set value \"$EMPTY\"")
		raw("data remove storage ra:safe s")
		raw("data modify storage ra:safe s set from storage $STORE cur")
		raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
		raw("execute store result score #cc.ok $T run function ${RaLib.Safe.check.asId()}")
		raw("execute unless score #cc.ok $T matches 1 run scoreboard players set #cc.bad $T 1")
		raw("data remove storage ra:safe s")
		raw("data remove storage ra:safe f")
	}
}
