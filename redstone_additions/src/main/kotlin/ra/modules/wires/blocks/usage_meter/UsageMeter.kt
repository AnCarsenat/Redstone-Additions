package ra.modules.wires.blocks.usage_meter

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.add
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.sub
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.TagRoute
import ra.core.WrenchCycle
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.propLine
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.block.targetPowerFill
import ra.core.block.textLine
import ra.core.computeScore
import ra.core.define
import ra.core.executeRun
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.readout.Readout
import ra.modules.wires.readout.Readout.findNetwork
import ra.modules.wires.readout.Readout.storeNetworkStatus
import ra.core.block.WRENCH_READOUT

/**
 * Usage Meter: a redstone pulse every `every` units consumed (taken out) or produced (put in)
 * on the network behind it, and a running `total` for bills.
 *
 * COUNTING. The meter flags the network record (`meter:1b`); from then on net/offer_write and
 * net/take_write add each transfer to the record's `produced` / `consumed` (ra/lib/transport/net/
 * Flow.kt), and networks no meter reads pay one `if data` per transfer. The counts are shared by
 * every meter on a network, so a meter never resets them: it keeps the last count it read
 * (`data.data.last`) and adds the difference to its own total and progress.
 *
 * EPOCHS. A rebuild of the network writes a fresh record, without the flag or the counts. The
 * first meter to see it flags it again with a new `epoch` (`#mt.epoch ra.wires.tmp2`, counted up
 * once per flagging) and counts from 0. A meter whose remembered epoch differs knows the count
 * restarted, and starts from 0 too. A new meter, or one whose mode changed, starts from the
 * count as it stands, so it never bills what happened before it was there.
 *
 * WRAPPING. A count reaching [WRAP] is taken down by [WRAP] by the meter that reads it. Any
 * other meter then reads less than it last did on the same epoch, which can only be that wrap,
 * and adds [WRAP] back. Totals and progress stop at [WRAP], which keeps every sum here in 32 bits.
 *
 * PULSES. At most one per read (every Readout.PERIOD ticks), 2 ticks long, on the meter's own
 * target block. More than one due waits in `progress` for the next reads, so none is lost.
 */
object UsageMeter : PackPart {
	const val ID = "usage_meter"
	const val DEFAULT_EVERY = 1000
	const val MAX_EVERY = 100_000_000
	private const val WRAP = 1_000_000_000
	private const val LIT = "ra.wires.meter_lit"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val update = ref(Ns.WIRES, "blocks/$ID/update")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	val cycleMode = ref(Ns.WIRES, "blocks/$ID/cycle_mode")
	val reset = ref(Ns.WIRES, "blocks/$ID/reset")
	val pulseLater = ref(Ns.WIRES, "blocks/$ID/pulse_later")
	val pulseOff = ref(Ns.WIRES, "blocks/$ID/pulse_off")
	private val count = ref(Ns.WIRES, "blocks/$ID/count")
	private val flag = ref(Ns.WIRES, "blocks/$ID/flag")
	private val pulse = ref(Ns.WIRES, "blocks/$ID/pulse")
	private val unlit = ref(Ns.WIRES, "blocks/$ID/unlit")

	override fun Ra.register() {
		properties(ID) {
			text("mode", default = "consumed")
			number("every", min = 1, max = MAX_EVERY, default = DEFAULT_EVERY)
			// The meter's own reading; the wrench (plain right-click) sets it back to 0.
			number("total", min = 0, max = WRAP, readonly = true, default = 0)
		}
		wrenchCycle(ID, WrenchCycle("Mode", "mode", cycleMode))
		wrenchToggles += TagRoute("ra.custom_block.$ID", reset)

		pack.define(UsageMeter.update) { update() }
		pack.define(count) { count() }
		pack.define(flag) {
			raw("scoreboard players add #mt.epoch ra.wires.tmp2 1")
			raw("\$data modify storage ra:transport nets.n\$(id) merge value {meter:1b,produced:0,consumed:0}")
			raw("\$execute store result storage ra:transport nets.n\$(id).epoch int 1 run scoreboard players get #mt.epoch ra.wires.tmp2")
		}
		pack.define(pulse) {
			raw("scoreboard players operation #mt.p ra.wires.tmp -= #prop ra.temp")
			raw("execute store result entity @s data.data.progress int 1 run scoreboard players get #mt.p ra.wires.tmp")
			raw(targetPowerFill(0, "15"))
			tag(self()) { add(LIT) }
			raw("scoreboard players set #rd.lit ra.wires.tmp 1")
		}
		pack.define(pulseLater) { schedule(pulseOff).replace(2.ticks) }
		pack.define(pulseOff) { executeRun("as @e[type=marker,tag=$LIT] at @s", unlit) }
		pack.define(unlit) {
			raw(targetPowerFill(0, "0"))
			tag(self()) { remove(LIT) }
		}

		pack.define(cycleMode) {
			// Shift+RMB with the Wrench. As the marker, at it. The new mode reads the other
			// count, so the meter starts from it as it stands, as a new meter would.
			raw("execute store success score #mt.m ra.wires.tmp if data entity @s data.properties{mode:\"produced\"}")
			raw("execute if score #mt.m ra.wires.tmp matches 1 run data modify entity @s data.properties.mode set value \"consumed\"")
			raw("execute if score #mt.m ra.wires.tmp matches 0 run data modify entity @s data.properties.mode set value \"produced\"")
			data(self()) { remove("data.data.epoch") }
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"Usage Meter: ",color:"gray"},{nbt:"data.properties.mode",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(reset) {
			// Plain RMB with the Wrench: a new billing period. As the marker.
			raw("data modify entity @s data.properties.total set value 0")
			raw("data modify entity @s data.data.progress set value 0")
			raw("data modify entity @s data.status.progress set value 0")
			raw("""execute at @s run tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"Usage Meter: ",color:"gray"},{text:"total reset to 0",color:"green"}]""")
			raw("execute at @s run playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(UsageMeter.goggles) {
			gogglesHeader("Usage Meter", nameY = 1.0)
			propLine("mode", "Counts: ", "aqua", y = 0.8)
			unitLines("total", "Total: ", "gold", prop = true)
			unitLines("every", "Pulse every: ", "aqua", prop = true)
			unitLines("progress", "Since last pulse: ", "gray", prop = false)
			execute {
				ifCondition { data(self(), "data.status{network:0}") }
				run { textLine("Network: ", "None beside it", "gray", y = 0.2) }
			}
			execute {
				unlessCondition { data(self(), "data.status{network:0}") }
				run { dataLine("network", "Network: ", "aqua", y = 0.2) }
			}
		}

		pack.define(UsageMeter.onBreak) {
			// As the marker, at it. Ends by killing the marker. The network keeps its flag
			// until its next rebuild; another meter may still be reading it.
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(Readout.TAG) }
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:target")
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** A property ([prop]) or status value with " mL" or " EU" after it, by the network's class. */
	private fun Function.unitLines(key: String, label: String, color: String, prop: Boolean) {
		for (unit in listOf("EU", "mL")) execute {
			ifCondition { data(self(), "data.status{unit:\"$unit\"}") }
			run { if (prop) propLine(key, label, color, y = 0.4, suffix = " $unit") else dataLine(key, label, color, y = 0.4, suffix = " $unit") }
		}
	}

	/** As the marker, at it, from the readout clock. */
	private fun Function.update() {
		// A pulse whose put-out was lost with an unloaded chunk.
		executeRun("if entity @s[tag=$LIT]", unlit)

		findNetwork()
		storeNetworkStatus()
		raw("execute if score #rd.net ra.wires.tmp matches 0 run return 0")

		raw("data modify storage ra:wires mtq set value {key:\"consumed\"}")
		raw("execute if data entity @s data.properties{mode:\"produced\"} run data modify storage ra:wires mtq.key set value \"produced\"")
		raw("execute store result storage ra:wires mtq.id int 1 run scoreboard players get #rd.net ra.wires.tmp")
		function(count, ra.core.Storage.WIRES, "mtq")
		data(ra.core.Storage.WIRES) { remove("mtq") }
	}

	/** `count {id, key}`: read the network's count and move the meter on. As the marker, at it. */
	private fun Function.count() {
		val t = "ra.wires.tmp"
		raw("\$execute unless data storage ra:transport nets.n\$(id) run return 0")
		raw("\$execute unless data storage ra:transport nets.n\$(id).meter run function ${flag.asId()} {id:\$(id)}")
		raw("\$execute store result score #mt.cur $t run data get storage ra:transport nets.n\$(id).\$(key)")
		raw("\$execute store result score #mt.ep $t run data get storage ra:transport nets.n\$(id).epoch")

		raw("execute store result score #mt.last $t run data get entity @s data.data.last")
		raw("execute store result score #mt.my $t run data get entity @s data.data.epoch")
		raw("execute unless score #mt.ep $t = #mt.my $t run scoreboard players set #mt.last $t 0")
		raw("execute unless data entity @s data.data.epoch run scoreboard players operation #mt.last $t = #mt.cur $t")

		computeScore("#mt.d", t, provider { sub(scoreOf("#mt.cur", t), scoreOf("#mt.last", t)) })
		raw("execute if score #mt.d $t matches ..-1 run scoreboard players add #mt.d $t $WRAP")
		raw("execute if score #mt.d $t matches ..-1 run scoreboard players set #mt.d $t 0")
		raw("execute if score #mt.d $t matches ${WRAP + 1}.. run scoreboard players set #mt.d $t $WRAP")

		raw("scoreboard players set #mt.w $t 0")
		raw("execute if score #mt.cur $t matches $WRAP.. store success score #mt.w $t run scoreboard players remove #mt.cur $t $WRAP")
		raw("\$execute if score #mt.w $t matches 1 store result storage ra:transport nets.n\$(id).\$(key) int 1 run scoreboard players get #mt.cur $t")

		raw("execute store result entity @s data.data.last int 1 run scoreboard players get #mt.cur $t")
		raw("execute store result entity @s data.data.epoch int 1 run scoreboard players get #mt.ep $t")

		raw("execute store result score #mt.t $t run data get entity @s data.properties.total")
		computeScore("#mt.t", t, provider { min(add(scoreOf("#mt.t", t), scoreOf("#mt.d", t)), constant(WRAP)) },
			"store result entity @s data.properties.total int 1")
		raw("execute store result score #mt.p $t run data get entity @s data.data.progress")
		computeScore("#mt.p", t, provider { min(add(scoreOf("#mt.p", t), scoreOf("#mt.d", t)), constant(WRAP)) },
			"store result entity @s data.data.progress int 1")

		readProperty("every", default = DEFAULT_EVERY, min = 1)
		executeRun("if score #mt.p $t >= #prop ra.temp", pulse)
		raw("execute store result entity @s data.status.progress int 1 run scoreboard players get #mt.p $t")
	}
}
