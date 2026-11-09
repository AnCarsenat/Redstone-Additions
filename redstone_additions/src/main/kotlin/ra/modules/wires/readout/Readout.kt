package ra.modules.wires.readout

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.network_gauge.NetworkGauge
import ra.modules.wires.blocks.usage_meter.UsageMeter

/**
 * The slow clock of the blocks that read a network without being part of it (the Network Gauge,
 * the Usage Meter).
 *
 * WHY A CLOCK AND NOT A "CHANGED" SIGNAL
 * A network record (`storage ra:transport nets.n<id>`) keeps no change counter: the only writers
 * are net/offer_write and net/take_write, the hottest lines in the transport engine, and a flag
 * there would be one more write on every transfer of every network for the sake of a handful of
 * gauges. A gauge reading its network every [PERIOD] ticks costs a few commands per gauge and
 * nothing at all while none is loaded.
 *
 * Same shape as the sensors' SensorPoll: [run] is scheduled every [PERIOD] ticks and reschedules
 * itself only while one of these blocks is loaded. `#rd.poll ra.wires.tmp2` is 1 while a run is
 * scheduled; the per-tick check in [tick] starts the clock when it is 0 and a readout marker is
 * loaded (a block placed, a chunk loading, a world loaded with the schedule lost), and the module
 * load resets it so a schedule that did not survive is started again.
 */
object Readout : PackPart {
	/** Ticks between two reads. Twice a second: a pulse train or a fill bar still looks live. */
	const val PERIOD = 10

	/** On every Network Gauge and Usage Meter marker: what the clock runs over. */
	const val TAG = "ra.wires.readout"

	val tick = ref(Ns.WIRES, "readout/tick")
	val run = ref(Ns.WIRES, "readout/run")
	private val start = ref(Ns.WIRES, "readout/start")

	/** As a transport node: record its network and class, and read it (net/read). */
	val node = ref(Ns.WIRES, "readout/node")

	private const val FLAG = "#rd.poll"

	/** In the module load. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG ra.wires.tmp2 0")

	/**
	 * As a readout marker, at it: find the network it reads. `#rd.net ra.wires.tmp` is the
	 * network id (0 for none), `#rd.class` its class, and net/read has filled `#net_amount` /
	 * `#net_capacity` and `storage ra:transport cur`.
	 *
	 * BEHIND FIRST. A readout faces the player who placed it (dir_type 3, like the Light
	 * Sensor), so the block it was placed against is behind it: that network wins. With no node
	 * there, the first neighbour that is one, in the engine's neighbour order, so a gauge set
	 * down beside a pipe rather than on it still finds it. A node on no network (an EU Switch
	 * without redstone) is passed over.
	 */
	fun Function.findNetwork() {
		raw("scoreboard players set #rd.net ra.wires.tmp 0")
		raw("scoreboard players set #rd.class ra.wires.tmp 0")
		raw("scoreboard players set #net_amount ra.tr.tmp 0")
		raw("scoreboard players set #net_capacity ra.tr.tmp 0")
		executeRun("positioned ^ ^ ^-1 as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] if score @s ra.tr.net matches 1..", node)
		for (side in listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~"))
			executeRun("if score #rd.net ra.wires.tmp matches 0 positioned $side as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] if score @s ra.tr.net matches 1..", node)
	}

	/**
	 * After [findNetwork]: `data.status.network` (0 for none) and `data.status.unit`, `EU` on an
	 * electric grid and `mL` on anything else.
	 */
	fun Function.storeNetworkStatus() {
		raw("execute store result entity @s data.status.network int 1 run scoreboard players get #rd.net ra.wires.tmp")
		raw("data modify entity @s data.status.unit set value \"mL\"")
		raw("execute store result score #rd.eu ra.wires.tmp run data get storage ra:transport classes.electric")
		raw("execute if score #rd.class ra.wires.tmp = #rd.eu ra.wires.tmp run data modify entity @s data.status.unit set value \"EU\"")
	}

	override fun Ra.register() {
		pack.define(tick) {
			// Per tick: the break checks (a target block's own drop must be caught within a few
			// ticks, see killVanillaDrop) and one score test while the clock runs.
			executeRun("as @e[type=marker,tag=ra.custom_block.network_gauge] at @s unless block ~ ~ ~ minecraft:target", NetworkGauge.onBreak)
			executeRun("as @e[type=marker,tag=ra.custom_block.usage_meter] at @s unless block ~ ~ ~ minecraft:target", UsageMeter.onBreak)
			executeRun("unless score $FLAG ra.wires.tmp2 matches 1 if entity @e[type=marker,tag=$TAG,limit=1]", start)
		}

		pack.define(start) {
			raw("scoreboard players set $FLAG ra.wires.tmp2 1")
			schedule(run).replace(PERIOD.ticks)
		}

		pack.define(run) {
			raw("scoreboard players set $FLAG ra.wires.tmp2 0")
			executeRun("as @e[type=marker,tag=ra.custom_block.network_gauge] at @s", NetworkGauge.update)
			// A meter that reaches its count lights its target and sets #rd.lit; one schedule
			// puts every pulse of this pass out together.
			raw("scoreboard players set #rd.lit ra.wires.tmp 0")
			executeRun("as @e[type=marker,tag=ra.custom_block.usage_meter] at @s", UsageMeter.update)
			executeRun("if score #rd.lit ra.wires.tmp matches 1", UsageMeter.pulseLater)
			executeReturnRun("if entity @e[type=marker,tag=$TAG,limit=1]", start)
		}

		pack.define(node) {
			raw("scoreboard players operation #rd.net ra.wires.tmp = @s ra.tr.net")
			raw("scoreboard players operation #rd.class ra.wires.tmp = @s ra.tr.class")
			function(RaLib.Transport.Net.read)
		}
	}
}
