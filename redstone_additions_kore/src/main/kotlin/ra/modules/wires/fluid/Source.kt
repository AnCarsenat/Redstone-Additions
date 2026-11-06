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
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line

object Source : PackPart {
	val isInfinite = ref(Ns.WIRES, "fluid/source/is_infinite")
	val probe = ref(Ns.WIRES, "fluid/source/probe")
	val probeNext = ref(Ns.WIRES, "fluid/source/probe_next")
	val probeTest = ref(Ns.WIRES, "fluid/source/probe_test")
	val scan = ref(Ns.WIRES, "fluid/source/scan")

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

			data(Storage.WIRES) { modify("found", Storage.WIRES, "probe.cur") }
			data(Storage.WIRES) { modify("found.dx", Storage.WIRES, "probe.dx") }
			data(Storage.WIRES) { modify("found.dy", Storage.WIRES, "probe.dy") }
			data(Storage.WIRES) { modify("found.dz", Storage.WIRES, "probe.dz") }
		}

		pack.define(Source.scan) {
			// Find a drainable world source in one of the six adjacent blocks.
			// Context: at the node position.
			// Output: storage ra:wires found = {match,medium,volume,drained,dx,dy,dz}
			// cleared when nothing is adjacent.
			//
			// The old pump only ever looked at ^ ^ ^1 — local coordinates, on a marker placed
			// with dir_type:0 whose rotation was therefore always the default. In practice
			// that meant a pump could only ever see the block to its south, which is why
			// pumps looked like they did nothing. Six-way adjacency removes the dependency on
			// a facing the block never really had.

			data(Storage.WIRES) { remove("found") }

			line("execute positioned ~1 ~ ~ run function", Source.probe, "{dx:1,dy:0,dz:0}")
			line("execute positioned ~-1 ~ ~ run function", Source.probe, "{dx:-1,dy:0,dz:0}")
			line("execute positioned ~ ~ ~1 run function", Source.probe, "{dx:0,dy:0,dz:1}")
			line("execute positioned ~ ~ ~-1 run function", Source.probe, "{dx:0,dy:0,dz:-1}")
			line("execute positioned ~ ~-1 ~ run function", Source.probe, "{dx:0,dy:-1,dz:0}")
			line("execute positioned ~ ~1 ~ run function", Source.probe, "{dx:0,dy:1,dz:0}")
		}
	}
}
