package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.Common
import ra.modules.wires.liquid.Liquid
import ra.modules.wires.liquid.LiquidBreak
import ra.modules.wires.media.Media

object Fluid : PackPart {
	val particles = ref(Ns.WIRES, "fluid/particles")
	val particlesRun = ref(Ns.WIRES, "fluid/particles_run")
	val potionStore = ref(Ns.WIRES, "fluid/potion_store")
	val potionStoreRun = ref(Ns.WIRES, "fluid/potion_store_run")
	val refreshStatus = ref(Ns.WIRES, "fluid/refresh_status")
	val refreshStatusAll = ref(Ns.WIRES, "fluid/refresh_status_all")
	val tick = ref(Ns.WIRES, "fluid/tick")

	override fun Ra.register() {
		pack.define(Fluid.particles) {
			// Show the particle a medium is registered with, at the current position.

			raw("\$execute if data storage ra:wires media.\$(medium).particle run function ${Fluid.particlesRun.asId()} with storage ra:wires media.\$(medium)")
		}

		pack.define(Fluid.particlesRun) {
			// Internal: the medium's own particle, so water splashes, lava sparks and powder
			// snow puffs snowflakes rather than everything emitting the same grey smoke.

			raw("\$particle \$(particle) ~ ~0.6 ~ 0.25 0.25 0.25 0.02 8 normal ${particleViewersSel()}")
		}

		pack.define(Fluid.potionStore) {
			// Remember which potion a network is holding.
			// Context: as the drain marker, with the emptying player still tagged.
			//
			// WHY THE NETWORK REMEMBERS AND THE MEDIUM DOES NOT
			// Every potion is the medium `potion` as far as volume is concerned -- they mix,
			// they share the run's capacity, and none of that needs to know which one it is.
			// What does need to know is the drain at the far end, and it needs the whole
			// `potion_contents` component rather than a name, because a brewed-with-commands
			// potion carries its effects on the item and nothing else knows them.
			//
			// FIRST POTION IN WINS
			// A network already holding one potion keeps its identity when a second,
			// different one is poured in. The alternative is refusing the second pour, which
			// is the single-medium behaviour this release exists to remove, or averaging two
			// effect lists, which means nothing. Mixing potions is a player decision and this
			// is the documented consequence of it.

			raw("execute if score @s ra.tr.net matches ..0 run return 0")

			data(Storage.WIRES) { remove("pot") }
			raw("execute store result storage ra:wires pot.id int 1 run scoreboard players get @s ra.tr.net")
			raw("data modify storage ra:wires pot.contents set from entity @p[tag=ra.wires.emptying] SelectedItem.components.\"minecraft:potion_contents\"")
			raw("execute unless data storage ra:wires pot.contents run return 0")

			function(Fluid.potionStoreRun, Storage.WIRES, "pot")
		}

		pack.define(Fluid.potionStoreRun) {
			// Internal: the dynamic-name half of potion_store.

			raw("\$execute unless data storage ra:transport nets.n\$(id).potion run data modify storage ra:transport nets.n\$(id).potion set from storage ra:wires pot.contents")
		}

		pack.define(Fluid.refreshStatus) {
			// Copy this node's network state onto the marker for the goggles to read.
			// Context: as a fluid node marker.

			function(Media.label)

			raw("execute store result entity @s data.status.amount int 1 run scoreboard players get #net_amount ra.tr.tmp")
			raw("execute store result entity @s data.status.capacity int 1 run scoreboard players get #net_capacity ra.tr.tmp")
			raw("execute store result entity @s data.status.network int 1 run scoreboard players get @s ra.tr.net")
		}

		pack.define(Fluid.refreshStatusAll) {
			// Periodic status refresh for every fluid node.

			scoreboard.players.set(literal("#status_timer"), "ra.wires.tmp", 0)
			executeRun("as @e[type=marker,tag=ra.wires.fluid_node]", Fluid.refreshStatus)
		}

		pack.define(Fluid.tick) {
			// Tick the merged fluid and gas system.
			//
			// Pipes, tanks and valves do no per-tick work. They contribute capacity and
			// connectivity, and the contents belong to the network, so a hundred-block pipe
			// run costs nothing to keep running. Only the blocks that actually move fluid
			// between the world and the network are ticked.
			//
			// The previous design gave every node its own buffer and had each one push at its
			// six neighbours every tick, guarded by a one-move-per-tick latch. Fluid crawled
			// one block per tick, the result depended on which node the entity selector
			// happened to reach first, and every node re-read and rewrote its NBT each tick
			// whether anything was flowing or not.

			// --- Pipe visuals ---
			// Displays are only rebuilt on the tick a conduit appears or disappears at a pipe
			// marker, or when a pipe's display entity has gone missing. They are never
			// rebuilt on a tick where nothing changed.
			raw("execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]")
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit", Liquid.pipeAppeared)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit", Liquid.pipeVanished)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.pipe_display.center,distance=..0.6,limit=1]", Common.updateModelLocalAndNeighbors)

			// --- Break detection ---
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s unless block ~ ~ ~ conduit", LiquidBreak.pipe)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_tank] at @s unless block ~ ~ ~ waxed_copper_block", LiquidBreak.tank)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s unless block ~ ~ ~ dispenser", LiquidBreak.pump)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_valve] at @s unless block ~ ~ ~ waxed_cut_copper", LiquidBreak.valve)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_filter] at @s unless block ~ ~ ~ waxed_exposed_cut_copper", LiquidBreak.filter)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s unless block ~ ~ ~ dropper", LiquidBreak.drain)
			executeRun("as @e[type=marker,tag=ra.custom_block.gas_tank] at @s unless block ~ ~ ~ iron_block", LiquidBreak.gasTank)
			executeRun("as @e[type=marker,tag=ra.custom_block.gas_pump] at @s unless block ~ ~ ~ smoker", LiquidBreak.gasPump)
			executeRun("as @e[type=marker,tag=ra.custom_block.gas_valve] at @s unless block ~ ~ ~ smooth_basalt", LiquidBreak.gasValve)
			executeRun("as @e[type=marker,tag=ra.custom_block.boiler] at @s unless block ~ ~ ~ furnace", LiquidBreak.boiler)

			// Valves are bridges now: they belong to no network and pump between the two they
			// sit between. Two things have to be true of a valve built by an older version.
			//
			// It must stop being a member -- a valve that stayed in a network would merge the
			// very runs it is supposed to keep apart, and the bridge would then find the same
			// network on both sides and refuse to move anything.
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.tr.node]", RaLib.Transport.Net.leave)
			executeRun("as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.tr.node]", RaLib.Transport.Net.leave)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_filter,tag=ra.tr.node]", RaLib.Transport.Net.leave)

			// And it must be tagged as a bridge so ra_wires:tick drives it at all. Idempotent,
			// so this costs one tag write on a set that is almost always empty.
			raw("tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=!ra.wires.bridge] add ra.wires.bridge")
			raw("tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=!ra.wires.bridge] add ra.wires.bridge")
			raw("tag @e[type=marker,tag=ra.custom_block.liquid_filter,tag=!ra.wires.bridge] add ra.wires.bridge")

			// --- World <-> network ---
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s", Pump.pumpTick)
			executeRun("as @e[type=marker,tag=ra.custom_block.gas_pump] at @s", Pump.pumpTick)
			executeRun("as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s", Drain.drainTick)
			executeRun("as @e[type=marker,tag=ra.custom_block.boiler] at @s", FluidBoiler.boilerTick)

			// --- Status for the goggles, refreshed every 20 ticks ---
			// The goggles only re-read billboards every 40 ticks, so writing these values on
			// every node on every tick was pure waste.
			scoreboard.players.add(literal("#status_timer"), "ra.wires.tmp", 1)
			executeRun("if score #status_timer ra.wires.tmp matches 20..", Fluid.refreshStatusAll)
		}
	}
}
