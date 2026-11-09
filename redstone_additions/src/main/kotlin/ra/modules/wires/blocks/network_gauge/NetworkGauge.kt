package ra.modules.wires.blocks.network_gauge

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.ceil
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.div
import io.github.ayfri.kore.features.predicates.providers.fromInt
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.targetPowerFill
import ra.core.block.textLine
import ra.core.computeScoreScaled
import ra.core.define
import ra.core.floatProvider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.readout.Readout
import ra.modules.wires.readout.Readout.findNetwork

/**
 * Network Gauge: a redstone signal, 0 to 15, from how full a network is.
 *
 * ONE BLOCK FOR EVERY CLASS
 * It reads whichever network it is attached to (see [Readout.findNetwork]) and is not part of it,
 * like a valve: the fill ratio is the same question on a fluid run and an EU grid, so a Tank Gauge
 * and a Grid Gauge would be the same block twice, and a gauge that joined a network would have to
 * pick a class at placement and could never be moved from a pipe to a wire. Not being a node also
 * means placing or breaking one never triggers a rebuild.
 *
 * THE LEVEL is `ceil(15 * amount / capacity)`, as a comparator reads a container: any amount
 * above zero gives at least 1, and only a full network gives 15. One /compute in floats, the
 * product taken before the division so an exact fraction stays exact (5000 of 15000 is 5, not 6
 * from 0.33333334 * 15), and nothing has to fit in 32 bits on the way. A network with no capacity
 * (a wire run with no battery) reads 0.
 *
 * OUTPUT. The gauge IS a target block, as the Light Sensor is: its `power` state is the signal
 * it gives every neighbour, written on every read.
 */
object NetworkGauge : PackPart {
	const val ID = "network_gauge"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val update = ref(Ns.WIRES, "blocks/$ID/update")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	private val output = ref(Ns.WIRES, "blocks/$ID/output")

	override fun Ra.register() {
		pack.define(NetworkGauge.update) { update() }

		// Macro {level}: only ever the integer stored just before the call.
		pack.define(output) { raw("\$" + targetPowerFill(0, "\$(level)")) }

		pack.define(NetworkGauge.goggles) {
			gogglesHeader("Network Gauge", nameY = 1.0)
			dataLine("level", "Output: ", "red", y = 0.8)
			execute {
				ifCondition { data(self(), "data.status{network:0}") }
				run { textLine("Network: ", "None beside it", "gray", y = 0.6) }
			}
			execute {
				unlessCondition { data(self(), "data.status{network:0}") }
				run { dataLine("network", "Network: ", "aqua", y = 0.6) }
			}
			unitLine("amount", "Holds: ", "aqua")
			unitLine("capacity", "Of: ", "gray")
		}

		pack.define(NetworkGauge.onBreak) {
			// As the marker, at it. Ends by killing the marker. Not a node, so there is no
			// network to leave; the pipe or wire beside it redraws without its connector.
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(Readout.TAG) }
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:target")
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** `data.status.<key>` with " mL" or " EU" after it, by the network's class. */
	private fun Function.unitLine(key: String, label: String, color: String) {
		execute {
			ifCondition { data(self(), "data.status{unit:\"EU\"}") }
			run { dataLine(key, label, color, y = 0.4, suffix = " EU") }
		}
		execute {
			ifCondition { data(self(), "data.status{unit:\"mL\"}") }
			run { dataLine(key, label, color, y = 0.4, suffix = " mL") }
		}
	}

	/** As the marker, at it, from the readout clock. */
	private fun Function.update() {
		findNetwork()

		raw("scoreboard players set #rd.level ra.wires.tmp 0")
		computeScoreScaled(
			"#rd.level", "ra.wires.tmp",
			floatProvider {
				ceil(div(mul(constant(15f), fromInt(scoreOf("#net_amount", "ra.tr.tmp"))), fromInt(scoreOf("#net_capacity", "ra.tr.tmp"))))
			},
			scale = 1,
			chain = "if score #net_capacity ra.tr.tmp matches 1..",
		)
		// Overfull for the moment a shrunken network waits for its clamp; a negative amount
		// never stands, but a level outside 0-15 is not a target state.
		raw("execute if score #rd.level ra.wires.tmp matches 16.. run scoreboard players set #rd.level ra.wires.tmp 15")
		raw("execute if score #rd.level ra.wires.tmp matches ..-1 run scoreboard players set #rd.level ra.wires.tmp 0")

		raw("execute store result entity @s data.status.network int 1 run scoreboard players get #rd.net ra.wires.tmp")
		raw("execute store result entity @s data.status.amount int 1 run scoreboard players get #net_amount ra.tr.tmp")
		raw("execute store result entity @s data.status.capacity int 1 run scoreboard players get #net_capacity ra.tr.tmp")
		raw("data modify entity @s data.status.unit set value \"mL\"")
		raw("execute store result score #rd.eu ra.wires.tmp run data get storage ra:transport classes.electric")
		raw("execute if score #rd.class ra.wires.tmp = #rd.eu ra.wires.tmp run data modify entity @s data.status.unit set value \"EU\"")

		// Written every read, not only on a change: a fill to the state already there changes
		// and updates nothing, and an arrow that pulsed the target gets its level back.
		raw("execute store result entity @s data.status.level int 1 run scoreboard players get #rd.level ra.wires.tmp")
		raw("function ${output.asId()} with entity @s data.status")
	}
}
