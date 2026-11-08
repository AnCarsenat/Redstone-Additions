package ra.modules.wires.fluid

import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object Pump : PackPart {
	val pumpExtract = ref(Ns.WIRES, "fluid/pump_extract")
	val pumpTick = ref(Ns.WIRES, "fluid/pump_tick")

	override fun Ra.register() {
		pack.define(Pump.pumpExtract) {
			// Internal: move one world source block into the network, if it fits.

			// A liquid pump will not lift a gas, and a gas pump will not lift a liquid.
			// Said on the block, not just skipped: a Gas Pump beside water used to keep whatever
			// state it last had, so it looked as if it were about to work.
			raw("\$execute if entity @s[tag=ra.custom_block.liquid_pump] unless data storage ra:wires media.\$(medium){state:\"liquid\"} run return run data modify entity @s data.status.pump_state set value \"not_a_liquid\"")
			raw("\$execute if entity @s[tag=ra.custom_block.gas_pump] unless data storage ra:wires media.\$(medium){state:\"gas\"} run return run data modify entity @s data.status.pump_state set value \"not_a_gas\"")

			// A source block is all or nothing. Offering a partial amount and then removing
			// the block anyway is how a nearly full network would quietly delete a lake.
			function(RaLib.Transport.Net.read)
			raw("\$scoreboard players set #need ra.wires.tmp \$(volume)")
			computeScore("#free", "ra.wires.tmp", provider { sub(scoreOf("#net_capacity", "ra.tr.tmp"), scoreOf("#net_amount", "ra.tr.tmp")) })

			// A network smaller than one source block can never take one, however long you
			// wait. That is a different problem from a network that happens to be full, and
			// saying so is the difference between "add a tank" and "wait a moment".
			raw("execute if score #net_capacity ra.tr.tmp < #need ra.wires.tmp run data modify entity @s data.status.pump_state set value \"network_too_small\"")
			raw("execute if score #net_capacity ra.tr.tmp < #need ra.wires.tmp run return 0")

			raw("execute if score #free ra.wires.tmp < #need ra.wires.tmp run data modify entity @s data.status.pump_state set value \"network_full\"")
			raw("execute if score #free ra.wires.tmp < #need ra.wires.tmp run return 0")

			raw("\$execute store result score #accept ra.wires.tmp run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")

			raw("execute if score #accept ra.wires.tmp matches ..0 run data modify entity @s data.status.pump_state set value \"wrong_medium\"")
			raw("execute if score #accept ra.wires.tmp matches ..0 run return 0")

			// Small pools drain away; a real body of fluid does not.
			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) run function ${Source.isInfinite.asId()} {match:\"\$(match)\"}")
			// A source marked keep (a lit campfire's smoke) is never used up.
			raw("execute if data storage ra:wires found{keep:1b} run scoreboard players set #src_infinite ra.wires.tmp 1")
			raw("\$execute if score #src_infinite ra.wires.tmp matches 0 positioned ~\$(dx) ~\$(dy) ~\$(dz) run setblock ~ ~ ~ \$(drained)")

			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) run function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
			raw("data modify entity @s data.status.pump_state set value \"pumping\"")
		}

		pack.define(Pump.pumpTick) {
			// Pull a world fluid source into the network.
			// Context: as a pump marker, at the pump position.
			//
			// Runs on a duty cycle rather than every tick: a source block is a whole 1000
			// units, so pulling one per tick per pump both trivialised supply and ran the
			// adjacency scan sixty times a second for no reason.

			scoreboard.players.add(self(), Obj.COOLDOWN, 1)
			raw("execute unless score @s ${Obj.COOLDOWN} matches 20.. run return 0")
			scoreboard.players.set(self(), Obj.COOLDOWN, 0)

			// Sources the pump can lift first. Only when there is none, every source, so a pump with
			// only the wrong kind beside it says so (not_a_gas / not_a_liquid) instead of no_source.
			raw("execute if entity @s[tag=ra.custom_block.liquid_pump] run data modify storage ra:wires probe_state set value \"liquid\"")
			raw("execute if entity @s[tag=ra.custom_block.gas_pump] run data modify storage ra:wires probe_state set value \"gas\"")
			function(Source.scan)
			data(Storage.WIRES) { remove("probe_state") }
			executeRun("unless data storage ra:wires found", Source.scan)

			raw("execute unless data storage ra:wires found run data modify entity @s data.status.pump_state set value \"no_source\"")
			raw("execute unless data storage ra:wires found run return 0")

			function(Pump.pumpExtract, Storage.WIRES, "found")
			data(Storage.WIRES) { remove("found") }
		}
	}
}
