package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object Source : PackPart {
	val isInfinite = ref(Ns.WIRES, "fluid/source/is_infinite")
	val probe = ref(Ns.WIRES, "fluid/source/probe")
	val probeNext = ref(Ns.WIRES, "fluid/source/probe_next")
	val probeTest = ref(Ns.WIRES, "fluid/source/probe_test")
	val scan = ref(Ns.WIRES, "fluid/source/scan")
	val scanBank = ref(Ns.WIRES, "fluid/source/scan_bank")
	val flowing = ref(Ns.WIRES, "fluid/source/flowing")

	override fun Ra.register() {
		pack.define(Source.isInfinite) {
			// Decide whether the source block at the current position belongs to a body big
			// enough to count as inexhaustible.
			// Output: #src_infinite ra.wires.tmp (1 = leave the block alone, 0 = consume it).
			//
			// The rule is nine or more matching source blocks within two blocks of this one.
			// That is an approximation of connectivity rather than a true flood fill: every
			// position in the template is visited exactly once, so there is no frontier and
			// no visited set to carry, and the check costs a fixed 25 block tests instead of
			// an unbounded search. A real pool clears the threshold easily; a handful of
			// placed source blocks does not, so small pools genuinely empty out.

			scoreboard.players.set(literal("#src_count"), "ra.wires.tmp", 0)

			raw("\$execute if block ~-2 ~ ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~-1 ~-1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~-1 ~ ~-1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~-1 ~ ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~-1 ~ ~1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~-1 ~1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~-2 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~-1 ~-1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~-1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~-1 ~1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~ ~-2 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~ ~-1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~ ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~ ~1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~ ~2 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~1 ~-1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~1 ~1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~ ~2 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~1 ~-1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~1 ~ ~-1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~1 ~ ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~1 ~ ~1 \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~1 ~1 ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")
			raw("\$execute if block ~2 ~ ~ \$(match) run scoreboard players add #src_count ra.wires.tmp 1")

			scoreboard.players.set(literal("#src_infinite"), "ra.wires.tmp", 0)
			raw("execute if score #src_count ra.wires.tmp matches 9.. run scoreboard players set #src_infinite ra.wires.tmp 1")
		}

		pack.define(Source.probe) {
			// Internal: test the block at the current position against the source registry.
			// Stops at the first match found across the whole scan.

			raw("execute if data storage ra:wires found run return 0")

			data(Storage.WIRES) { modify("probe.queue", Storage.WIRES, "source_blocks") }
			raw("\$data modify storage ra:wires probe.dx set value \$(dx)")
			raw("\$data modify storage ra:wires probe.dy set value \$(dy)")
			raw("\$data modify storage ra:wires probe.dz set value \$(dz)")

			function(Source.probeNext)
			data(Storage.WIRES) { remove("probe") }
		}

		pack.define(Source.probeNext) {
			// Internal: walk the source registry until one entry matches this block.

			raw("execute if data storage ra:wires found run return 0")
			raw("execute unless data storage ra:wires probe.queue[0] run return 0")

			data(Storage.WIRES) { modify("probe.cur", Storage.WIRES, "probe.queue[0]") }
			data(Storage.WIRES) { remove("probe.queue[0]") }

			function(Source.probeTest, Storage.WIRES, "probe.cur")
			function(Source.probeNext)
		}

		pack.define(Source.probeTest) {
			// Internal: record the entry if the block here matches it.

			raw("\$execute unless block ~ ~ ~ \$(match) run return 0")
			// A scan asked for one state (storage ra:wires probe_state) skips the others, so a Gas
			// Pump beside water and a campfire takes the smoke, and a Liquid Pump or Drain in the
			// same spot takes the water instead of stopping at the campfire.
			raw("\$execute if data storage ra:wires probe_state unless data storage ra:wires {probe_state:\"\$(state)\"} run return 0")

			data(Storage.WIRES) { modify("found", Storage.WIRES, "probe.cur") }
			data(Storage.WIRES) { modify("found.dx", Storage.WIRES, "probe.dx") }
			data(Storage.WIRES) { modify("found.dy", Storage.WIRES, "probe.dy") }
			data(Storage.WIRES) { modify("found.dz", Storage.WIRES, "probe.dz") }
		}

		pack.define(Source.scan) {
			// Find a drainable world source in one of the six adjacent blocks.
			// Context: at the node position.
			// Input (optional): storage ra:wires probe_state, "liquid" or "gas" -- only sources of
			// that state count.
			// Output: storage ra:wires found = {match,medium,state,volume,drained,[keep],dx,dy,dz}
			// cleared when nothing is adjacent.
			//
			// The old pump only ever looked at ^ ^ ^1 — local coordinates, on a marker placed
			// with dir_type:0 whose rotation was therefore always the default. In practice
			// that meant a pump could only ever see the block to its south, which is why
			// pumps looked like they did nothing. Six-way adjacency removes the dependency on
			// a facing the block never really had.

			data(Storage.WIRES) { remove("found") }

			executeRun("positioned ~1 ~ ~", Source.probe, "{dx:1,dy:0,dz:0}")
			executeRun("positioned ~-1 ~ ~", Source.probe, "{dx:-1,dy:0,dz:0}")
			executeRun("positioned ~ ~ ~1", Source.probe, "{dx:0,dy:0,dz:1}")
			executeRun("positioned ~ ~ ~-1", Source.probe, "{dx:0,dy:0,dz:-1}")
			executeRun("positioned ~ ~-1 ~", Source.probe, "{dx:0,dy:-1,dz:0}")
			executeRun("positioned ~ ~1 ~", Source.probe, "{dx:0,dy:1,dz:0}")
		}

		pack.define(Source.scanBank) {
			// Find a drainable world source one block down and one block out: the four blocks
			// diagonally below the node's sides.
			// Context: at the node position. Input and output as ra_wires:fluid/source/scan; run
			// it only after that scan found nothing, since it leaves `found` alone once set.
			//
			// WHY
			// A block put down beside a pool stands ON the bank, and the bank's top is level with
			// the water's surface. So the pool is never next to the block -- it is next to the
			// block underneath it, one down and one out -- and a six-way scan saw only air and the
			// bank. That is how a Liquid Drain placed beside a lake said `no_source` forever.

			executeRun("positioned ~1 ~-1 ~", Source.probe, "{dx:1,dy:-1,dz:0}")
			executeRun("positioned ~-1 ~-1 ~", Source.probe, "{dx:-1,dy:-1,dz:0}")
			executeRun("positioned ~ ~-1 ~1", Source.probe, "{dx:0,dy:-1,dz:1}")
			executeRun("positioned ~ ~-1 ~-1", Source.probe, "{dx:0,dy:-1,dz:-1}")
		}

		pack.define(Source.flowing) {
			// Is there liquid that is not a source block in any of the places the two scans look?
			// Context: at the node position, after both scans found nothing.
			// Returns 1 when there is.
			//
			// A pool that has just had the block beside a drain taken out of it fills that gap
			// with flowing water, which no scan takes. It may well turn back into a source in a
			// moment (two sources beside it make one), so the drain says it is waiting for one
			// rather than claiming there is nothing there. No source is in these places by now,
			// so any water or lava found is flowing.

			for (pos in listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~-1 ~", "~ ~1 ~", "~1 ~-1 ~", "~-1 ~-1 ~", "~ ~-1 ~1", "~ ~-1 ~-1")) {
				raw("execute if block $pos minecraft:water run return 1")
				raw("execute if block $pos minecraft:lava run return 1")
			}
			raw("return 0")
		}
	}
}
