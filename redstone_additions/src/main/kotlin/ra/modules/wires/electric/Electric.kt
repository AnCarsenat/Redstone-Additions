package ra.modules.wires.electric

import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.creative_eu.CreativeEu
import ra.modules.wires.blocks.creative_fluid.CreativeFluid
import ra.modules.wires.blocks.electric_furnace.ElectricFurnace
import ra.modules.wires.blocks.electric_generator.ElectricGenerator
import ra.modules.wires.blocks.industrial_light.IndustrialLight
import ra.modules.wires.blocks.solar_panel.SolarPanel
import ra.modules.wires.common.Common
import ra.modules.wires.common.line

object Electric : PackPart {
	val adopt = ref(Ns.WIRES, "electric/adopt")
	val consumerTick = ref(Ns.WIRES, "electric/consumer_tick")
	val tick = ref(Ns.WIRES, "electric/tick")
	val updateStatus = ref(Ns.WIRES, "electric/update_status")

	override fun Ra.register() {
		pack.define(Electric.adopt) {
			// Enrol an electric node that is not on a transport network yet.
			// Context: as the node marker.
			//
			// Two callers in practice. New blocks are joined at placement by
			// ra_wires:blocks/place_join, so they never reach here. Everything placed before
			// electric moved onto the network engine does — those markers carry
			// ra.wires.electric_node but no ra.tr.node, and would otherwise sit inert on a
			// grid that cannot see them.
			//
			// Capacity is the node's contribution to its network's total, and the numbers
			// match the specs in ra_wires:blocks/handle_placement.
			//
			// Wires, switches and the vault contribute NOTHING. A grid stores what its
			// batteries store and no more, so a run of wire is not secretly a tank: with no
			// battery on it, whatever a generator makes has to be spent on the same tick or
			// it is gone. Generators, consumers and panels keep 50 only so they can function
			// standing alone.

			executeRun("if entity @s[tag=ra.custom_block.electric_wire]", RaLib.Transport.Net.join, """{class:"electric",capacity:0}""")
			executeRun("if entity @s[tag=ra.custom_block.electric_generator]", RaLib.Transport.Net.join, """{class:"electric",capacity:50}""")
			executeRun("if entity @s[tag=ra.custom_block.electric_consumer]", RaLib.Transport.Net.join, """{class:"electric",capacity:50}""")
			executeRun("if entity @s[tag=ra.custom_block.solar_panel]", RaLib.Transport.Net.join, """{class:"electric",capacity:50}""")
			executeRun("if entity @s[tag=ra.custom_block.electric_switch]", RaLib.Transport.Net.join, """{class:"electric",capacity:0}""")
			executeRun("if entity @s[tag=ra.custom_block.battery]", RaLib.Transport.Net.join, """{class:"electric",capacity:10000}""")
			executeRun("if entity @s[tag=ra.custom_block.ender_power_vault]", RaLib.Transport.Net.join, """{class:"electric",capacity:0}""")

			// The per-node buffer is dead weight now: the charge lives on the network. Left
			// behind it would show up in the Data Handler as an editable field that changes
			// nothing.
			// transfer_rate steered the old per-node handover and steers nothing now -- except
			// on the Ender Power Vault, where it is still the rate its channel moves EU at.
			raw("execute unless entity @s[tag=ra.custom_block.ender_power_vault] run data remove entity @s data.properties.transfer_rate")
			data(self()) { remove("data.data.eu") }
			data(self()) { remove("data.data.capacity") }
		}

		pack.define(Electric.consumerTick) {
			// Draw this tick's EU off the grid and expose whether it ran.
			// Context: as the consumer marker, at its position.
			//
			// All or nothing: a consumer that cannot afford a full tick's draw takes nothing
			// rather than half. Taking a partial amount would let a starved grid drain into
			// a row of consumers that all report themselves as off, which is how you get a
			// generator running flat out and nothing on the base working.

			raw("execute unless data entity @s data.properties.eu_use run data modify entity @s data.properties.eu_use set value 40")

			// Guarded: an unguarded read of a missing eu_use writes zero, and a consumer
			// that costs nothing runs for free off an empty grid.
			function(RaLib.Util.property, nbt { this["name"] = "eu_use"; this["default"] = 40; this["min"] = 1 })
			scoreboard.players.operation(literal("#use"), "ra.wires.tmp2", Operation.SET, literal("#prop"), "ra.temp")

			// Check the grid can cover the whole draw before taking any of it.
			function(RaLib.Transport.Net.read)
			raw("execute if score #net_amount ra.tr.tmp < #use ra.wires.tmp2 run data modify entity @s data.status.active set value 0b")
			raw("execute if score #net_amount ra.tr.tmp < #use ra.wires.tmp2 run return 0")

			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #use ra.wires.tmp2")
			executeRun("store result score #got ra.wires.tmp", EuApi.takeEu, "with storage ra:wires eu")

			raw("execute if score #got ra.wires.tmp < #use ra.wires.tmp2 run data modify entity @s data.status.active set value 0b")
			raw("execute if score #got ra.wires.tmp < #use ra.wires.tmp2 run return 0")

			raw("data modify entity @s data.status.active set value 1b")
			raw("particle minecraft:electric_spark ~ ~0.9 ~ 0.25 0.25 0.25 0.01 2 normal ${particleViewersSel()}")
		}

		pack.define(Electric.tick) {
			// Tick the electric grid.
			//
			// WHAT CHANGED
			// Electric used to be the last system still moving charge node to node: every
			// wire held its own buffer and, once a tick, handed half the difference to one
			// neighbour that had less. That model cannot deliver power, only level it out.
			// Charge crawled one block per tick, a generator with six neighbours fed exactly
			// one of them, and once a run had evened out to within 1 EU everywhere the
			// transfer guard stopped it moving at all — so a consumer at the end of a line
			// was fed by whatever leaked past the wires in front of it, if anything.
			//
			// It now uses the same network engine the fluid side has used since v5.1.4, on
			// the `electric` class the engine already reserved. Adjacent nodes are flood
			// filled into a grid, the charge belongs to the grid rather than to any block,
			// and capacity is the sum of what each node contributes. A generator offers its
			// output to the grid and a consumer takes from it, both in the same tick, however
			// far apart they are. There is no propagation left to go wrong, no order
			// dependence, and wires cost nothing per tick because there is nothing for them
			// to do but conduct.

			// Keep electric wire conduits non-waterlogged.
			raw("execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]")

			// Self-heal missing wire displays without forcing per-tick full rebuilds.
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.wire_display.center,distance=..0.6,limit=1]", Common.updateModelLocalAndNeighbors)

			// Break detection — straight to the cleanup handler, no ra.broken round-trip.
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_wire] at @s unless block ~ ~ ~ conduit", ElectricBreak.wire)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_generator] at @s unless block ~ ~ ~ barrel", ElectricBreak.generator)
			executeRun("as @e[type=marker,tag=ra.custom_block.creative_eu] at @s unless block ~ ~ ~ beacon", ElectricBreak.creativeEu)
			executeRun("as @e[type=marker,tag=ra.custom_block.creative_fluid] at @s unless block ~ ~ ~ beacon", ElectricBreak.creativeFluid)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_furnace] at @s unless block ~ ~ ~ barrel", ElectricBreak.electricFurnace)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s unless block ~ ~ ~ observer", ElectricBreak.consumer)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_switch] at @s unless block ~ ~ ~ waxed_cut_copper", ElectricBreak.switch)
			executeRun("as @e[type=marker,tag=ra.custom_block.solar_panel] at @s unless block ~ ~ ~ daylight_detector", ElectricBreak.solarPanel)
			executeRun("as @e[type=marker,tag=ra.custom_block.battery] at @s unless block ~ ~ ~ waxed_copper_grate", ElectricBreak.battery)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_breaker] at @s unless block ~ ~ ~ waxed_copper_bulb", ElectricBreak.breaker)
			executeRun("as @e[type=marker,tag=ra.custom_block.industrial_light] at @s unless block ~ ~ ~ sea_lantern", ElectricBreak.industrialLight)

			// Membership. Every one of these lines only fires when a node's membership
			// disagrees with what it should be, which matters more than it looks: joining and
			// leaving both mark the network dirty, so a line that fired unconditionally would
			// force a rebuild every single tick for ever.
			//
			// Repaint a generator's furnace skin if it has gone missing. Anchored to the block
			// centre, matching where ra_lib:skin/spawn stands its displays.
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_generator] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.electric_generator,distance=..0.4,limit=1]", ElectricGenerator.refreshDisplay)

			// Enrols anything placed before electric moved onto the network engine, and costs
			// one tag test per node on every tick after that.
			executeRun("as @e[type=marker,tag=ra.wires.electric_node,tag=!ra.tr.node,tag=!ra.custom_block.electric_switch] at @s", Electric.adopt)

			// A switch is the exception: its enabled state decides whether it conducts, so it
			// joins and leaves as that changes, however it was changed — wrench, Data
			// Handler toggle or a raw /data edit. Leaving severs the run at this block and
			// the two halves rebuild as separate grids, which is what the old model could
			// never really do: a switch there just declined to hand charge on, while still
			// holding a buffer either side could pull from.
			//
			// Only `enabled` is allowed to keep a node off the grid. A disabled consumer or
			// generator stops drawing or producing but still conducts, exactly as before —
			// cutting a line is the switch's job alone.
			// THE SWITCH IS REDSTONE NOW, NOT A PROPERTY
			// It was the one block whose only control was `enabled`, which meant the only way
			// to work a switch was to open a menu -- in a redstone pack. Powered conducts,
			// unpowered cuts, the same convention the EU Breaker and the valves already use,
			// so a lever wired to it does what a lever obviously should.
			line("execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=!ra.tr.node] at @s if function", RaLib.Redstone.any, "run function", Electric.adopt)
			line("execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.tr.node] at @s unless function", RaLib.Redstone.any, "run function", RaLib.Transport.Net.leave)

			// Production, then draw. Order matters only in that a generator's output is
			// spendable on the same tick it is made; nothing depends on which generator or
			// which consumer the selector reaches first, because they all talk to the grid
			// rather than to each other.
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_generator] at @s", Generator.generatorTick)
			executeRun("as @e[type=marker,tag=ra.custom_block.solar_panel] at @s", SolarPanel.tick)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s", Electric.consumerTick)
			// Orphan sweep for the Industrial Light. A light that goes out clears its own
			// beam on that tick; this is only for beams whose marker vanished with a chunk,
			// which have nothing left to notice. Once every ten seconds.
			scoreboard.players.add(literal("#il.sweep"), "ra.wires.tmp2", 1)
			raw("execute if score #il.sweep ra.wires.tmp2 matches 200.. run scoreboard players set #il.sweep ra.wires.tmp2 0")

			executeRun("as @e[type=marker,tag=ra.custom_block.electric_furnace] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.electric_furnace,distance=..0.4,limit=1]", ElectricFurnace.refreshDisplay)
			executeRun("as @e[type=marker,tag=ra.custom_block.creative_eu] at @s", CreativeEu.tick)
			executeRun("as @e[type=marker,tag=ra.custom_block.creative_fluid] at @s", CreativeFluid.tick)
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_furnace] at @s", ElectricFurnace.tick)
			executeRun("as @e[type=marker,tag=ra.custom_block.industrial_light] at @s", IndustrialLight.tick)

			// Status refresh. Wires and switches do no other work.
			executeRun("as @e[type=marker,tag=ra.wires.electric_node] at @s", Electric.updateStatus)
		}

		pack.define(Electric.updateStatus) {
			// Publish the grid's charge, and which way it is going, on this node.
			// Context: as an electric node marker.
			//
			// The figures are the whole network's, because that is where the charge lives.
			// Every node on a grid reports the same numbers — the honest answer, since a wire
			// does not hold its own pocket of electricity.
			//
			// WHY A TREND AND NOT AN `enabled` FLAG
			// Wires used to show "Enabled: on", which was true of every wire that had ever
			// been placed and told you nothing. What you actually want to know standing in
			// front of a wire is whether the grid is filling or draining, and that is one
			// subtraction against what the same node read a second ago.

			function(RaLib.Transport.Net.read)

			raw("execute store result entity @s data.status.available_eu int 1 run scoreboard players get #net_amount ra.tr.tmp")
			raw("execute store result entity @s data.status.capacity int 1 run scoreboard players get #net_capacity ra.tr.tmp")

			// Trend against this node's own last reading.
			raw("execute unless data entity @s data.data.last_eu run data modify entity @s data.data.last_eu set value 0")
			raw("execute store result score #eu.prev ra.wires.tmp run data get entity @s data.data.last_eu 1")
			computeScore(
				"#eu.delta", "ra.wires.tmp",
				provider { sub(scoreOf("#net_amount", "ra.tr.tmp"), scoreOf("#eu.prev", "ra.wires.tmp")) },
				chain = "store result entity @s data.status.trend int 1",
			)
			raw("execute store result entity @s data.data.last_eu int 1 run scoreboard players get #net_amount ra.tr.tmp")

			// One line that says what the grid is doing, in the order that matters: not being
			// on a grid beats being full, and being full beats which way the needle moved.
			raw("execute if score #eu.delta ra.wires.tmp matches 1.. run data modify entity @s data.status.grid set value \"Charging\"")
			raw("execute if score #eu.delta ra.wires.tmp matches ..-1 run data modify entity @s data.status.grid set value \"Draining\"")
			raw("execute if score #eu.delta ra.wires.tmp matches 0 run data modify entity @s data.status.grid set value \"Steady\"")

			raw("execute if score #net_amount ra.tr.tmp matches 1.. if score #net_amount ra.tr.tmp >= #net_capacity ra.tr.tmp run data modify entity @s data.status.grid set value \"Grid full\"")
			raw("execute if score #net_amount ra.tr.tmp matches ..0 run data modify entity @s data.status.grid set value \"Empty\"")

			// No capacity anywhere on the run: wires and switches store nothing, so a grid
			// with no battery on it cannot hold a thing between ticks. Worth saying outright,
			// because it looks identical to an empty grid otherwise.
			raw("execute if score #net_capacity ra.tr.tmp matches 0 run data modify entity @s data.status.grid set value \"No storage — add a Battery\"")

			// A node on no grid at all: a switch someone turned off, or a wire with nothing
			// adjacent. That beats every other reading.
			raw("execute if score @s ra.tr.net matches ..0 run data modify entity @s data.status.grid set value \"Disconnected\"")
		}
	}
}
