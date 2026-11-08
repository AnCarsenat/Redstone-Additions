package ra.tools.wrench

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import ra.api.Tools
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.WrenchCycle
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.tools.tool

/**
 * `ra:tools/wrench/init_registry`: which properties each block can cycle, and what cycles them.
 *
 * ONE LIST, NOT A BRANCH PER BLOCK
 * The wrench used to be a chain of `if entity @s[tag=ra.custom_block.X] run return run function
 * <that block's cycler>`, which meant one cycler per block and no way to offer two. Blocks that
 * wanted a second setting had to borrow the goggles, which is how the Electric Furnace ended up
 * with its output on one tool and its power mode on another.
 *
 * Now a block declares a LIST of cyclable properties (`ra.wrenchCycle`) and the wrench works out
 * what to do: nothing declared and it says so, one declared and it cycles that one immediately,
 * more than one and it opens a menu.
 *
 * Keyed by the block type, which markers carry in data.type as of 5.1.9 -- a data pack cannot ask
 * an entity which of its tags is the interesting one, so the type has to be written down at
 * placement. See ra_migrations for old worlds.
 *
 * There is no `enabled` entry. Every RA Wires block used to carry one, which put an "Enabled" row on
 * blocks that had nothing else to configure and turned a one-setting block into a menu. A second off
 * switch next to redstone is not a setting anyone reaches for -- breaking the wire does the same
 * thing and reads better -- so the property is gone from the module entirely, and the EU Switch,
 * whose only control it was, runs on redstone now.
 */
object WrenchRegistry {
	val initRegistry = Tools.Wrench.initRegistry
	val cycleUniGate = tool("wrench/cycle_uni_gate")
	val cycleUniGateNotify = tool("wrench/cycle_uni_gate_notify")

	/** UNI gate modes in cycle order; the last wraps to the first. */
	private val GATES = listOf("and", "or", "not", "xor", "nand", "nor", "xnor")

	/** The tools own this row: its cycler lives in ra:tools, not in ra_gates. */
	fun Ra.registerToolsWrenchCycles() {
		wrenchCycle("uni_gate", WrenchCycle("Gate type", "gate_type", cycleUniGate))
	}

	fun Ra.registerWrenchRegistry() {
		// Configuration, rewritten on every load. Keys sorted, as the hand-written map was.
		val map = wrenchCycles.toSortedMap().entries.joinToString(",", "{", "}") { (type, rows) ->
			"\"$type\":" + rows.joinToString(",", "[", "]") { """{label:"${it.label}",prop:"${it.prop}",fn:"${it.fn.asId()}"}""" }
		}
		pack.define(initRegistry) { raw("data modify storage ra:wrench cyclables set value $map") }

		// Cycle the UNI gate through its modes. As the gate marker, at its position.
		pack.define(cycleUniGate) {
			raw("execute unless data entity @s data.properties.gate_type run data modify entity @s data.properties.gate_type set value \"and\"")
			// Next mode from the current one, newest first; `return` stops a mode that was just
			// written from matching again further down.
			for (i in GATES.indices.reversed()) {
				val from = GATES[i]
				val to = GATES[(i + 1) % GATES.size]
				raw("execute if data entity @s data.properties{gate_type:\"$from\"} run data modify entity @s data.properties.gate_type set value \"$to\"")
				if (i > 0) executeReturnRun("if data entity @s data.properties{gate_type:\"$from\"}", cycleUniGateNotify)
			}
			function(cycleUniGateNotify)
		}
		pack.define(cycleUniGateNotify) {
			raw("data modify storage ra:temp current_gate set from entity @s data.properties.gate_type")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"UNI Gate mode: ",color:"gray"},{nbt:"current_gate",storage:"ra:temp",color:"light_purple",bold:true}]""")
			raw("playsound minecraft:block.note_block.hat block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 2")
			raw("particle minecraft:happy_villager ~ ~0.5 ~ 0.3 0.3 0.3 0 5 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
			data(Storage.TEMP) { remove("current_gate") }
		}
	}
}
