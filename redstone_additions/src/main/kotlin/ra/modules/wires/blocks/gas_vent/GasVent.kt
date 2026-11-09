package ra.modules.wires.blocks.gas_vent

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.api.RaLib
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.WrenchCycle
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.particleViewersSel
import ra.core.block.propLine
import ra.core.block.readProperty
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.common.line
import ra.modules.wires.machine.MachineClock

/**
 * Gas Vent: while powered by redstone, once a second, lets up to `rate` mL of its network's gas
 * out into the area, and applies that gas's effect to every living entity within `radius`.
 *
 * WHAT IT RELEASES. The network's primary medium, when the media registry says it is a gas; a
 * liquid is left in the pipes (`not_a_gas`). Like a powered Liquid Drain pouring a medium with no
 * world form, the gas really leaves the network and is shown as its `pour` particle.
 *
 * WHAT IT DOES. `storage ra:wires gas_effects.<medium>` = `{effect, seconds, amplifier}`
 * (ra_wires:media/init): oxygen gives water breathing, toxic gas withers. A gas with no entry is
 * only vented. Another pack adds its gas there after ra_wires:load.
 *
 * NO COST WHILE OFF. Unpowered, a vent is one redstone test a second.
 */
object GasVent : PackPart {
	const val ID = "gas_vent"
	const val BLOCK = "minecraft:waxed_weathered_copper_grate"

	const val DEFAULT_RADIUS = 4
	const val MAX_RADIUS = 8
	const val ADMIN_MAX_RADIUS = 16
	const val DEFAULT_RATE = 1000
	const val MIN_RATE = 100
	const val MAX_RATE = 100_000
	const val CAPACITY = 1000

	private const val T = "ra.wires.tmp"
	private const val S = "storage ra:wires gv"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val run = ref(Ns.WIRES, "blocks/$ID/run")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	private val vent = ref(Ns.WIRES, "blocks/$ID/vent")
	private val apply = ref(Ns.WIRES, "blocks/$ID/apply")
	private val show = ref(Ns.WIRES, "blocks/$ID/show")
	val cycleRadius = ref(Ns.WIRES, "blocks/$ID/cycle_radius")

	override fun Ra.register() {
		properties(ID) {
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
			// Throughput, so admin only, like the Liquid Drain's rate.
			number("rate", min = MIN_RATE, max = MAX_RATE, adminOnly = true, default = DEFAULT_RATE)
		}
		wrenchCycle(ID, WrenchCycle("Radius", "radius", cycleRadius))

		// Shift+RMB with the Wrench: 2, 4, 6, 8 and round. As the marker, at it.
		pack.define(cycleRadius) {
			readProperty("radius", default = DEFAULT_RADIUS, min = 1)
			raw("scoreboard players set #gv.r $T 2")
			for (r in listOf(2, 4, 6)) raw("execute if score #prop ra.temp matches $r run scoreboard players set #gv.r $T ${r + 2}")
			raw("execute store result entity @s data.properties.radius int 1 run scoreboard players get #gv.r $T")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Gas Vent radius: ",color:"gray"},{nbt:"data.properties.radius",entity:"@s",color:"aqua"},{text:" blocks",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(GasVent.run) { run() }

		// Macro {medium, r, rate}. As the marker, at it.
		pack.define(vent) {
			raw("\$execute unless data storage ra:wires media.\$(medium){state:\"gas\"} run return run data modify entity @s data.status.vent_state set value \"not_a_gas\"")
			raw("\$execute store result score #gv.got $T run function ${RaLib.Transport.Net.take.asId()} {amount:\$(rate),medium:\"\$(medium)\"}")
			raw("execute if score #gv.got $T matches ..0 run return run data modify entity @s data.status.vent_state set value \"network_empty\"")
			raw("data modify entity @s data.status.vent_state set value \"venting\"")
			raw("execute store result entity @s data.status.vented int 1 run scoreboard players get #gv.got $T")

			// The effect entry, with the radius beside it for the macro.
			raw("\$data modify $S.e set from storage ra:wires gas_effects.\$(medium)")
			raw("execute if data $S.e run data modify $S.e.r set from $S.r")
			raw("execute if data $S.e run function ${apply.asId()} with $S.e")

			raw("\$execute if data storage ra:wires media.\$(medium).pour run return run function ${show.asId()} with storage ra:wires media.\$(medium)")
		}

		// Macro {effect, seconds, amplifier, r}. Living entities only take it; the rest of @e is
		// skipped by `effect give` itself.
		pack.define(apply) {
			raw("\$effect give @e[distance=..\$(r)] \$(effect) \$(seconds) \$(amplifier)")
		}

		// Macro: a media registry entry ({pour,...}).
		pack.define(show) {
			raw("\$particle \$(pour) ~ ~0.8 ~ 0.8 0.5 0.8 0.02 20 normal ${particleViewersSel(32)}")
		}

		pack.define(GasVent.goggles) {
			gogglesHeader("Gas Vent", nameY = 1.0)
			dataLine("vent_state", "Vent: ", "yellow", y = 0.8)
			dataLine("medium", "Gas: ", "aqua", y = 0.6)
			dataLine("amount", "Amount: ", "aqua", y = 0.4, suffix = " mL")
			propLine("radius", "Radius: ", "aqua", y = 0.2)
		}

		pack.define(GasVent.onBreak) {
			// As the marker, at it. Ends by killing the marker.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(MachineClock.TAG) }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop(BLOCK)
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** As the marker, at it, once a second from the machine clock. */
	private fun Function.run() {
		line("execute unless function", RaLib.Redstone.any, "run return run data modify entity @s data.status.vent_state set value \"off\"")
		raw("execute unless score @s ra.tr.net matches 1.. run return run data modify entity @s data.status.vent_state set value \"no_network\"")
		function(RaLib.Transport.Net.read)
		raw("execute unless data storage ra:transport cur.medium run return run data modify entity @s data.status.vent_state set value \"network_empty\"")

		raw("data remove $S")
		raw("data modify $S.medium set from storage ra:transport cur.medium")
		readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
		raw("execute store result $S.r int 1 run scoreboard players get #prop ra.temp")
		readProperty("rate", default = DEFAULT_RATE, min = MIN_RATE)
		raw("execute if score #prop ra.temp matches ${MAX_RATE + 1}.. run scoreboard players set #prop ra.temp $MAX_RATE")
		raw("execute store result $S.rate int 1 run scoreboard players get #prop ra.temp")
		function(vent, Storage.WIRES, "gv")
		raw("data remove $S")
	}
}
