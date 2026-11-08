package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.Cip
import ra.modules.wires.common.storeCompute

object Drain : PackPart {
	val drainAbove = ref(Ns.WIRES, "fluid/drain_above")
	val drainAboveNext = ref(Ns.WIRES, "fluid/drain_above_next")
	val drainAboveTry = ref(Ns.WIRES, "fluid/drain_above_try")
	val drainTick = ref(Ns.WIRES, "fluid/drain_tick")

	override fun Ra.register() {
		pack.define(Drain.drainAbove) {
			// Empty one filled container out of the block sitting on top of the drain.
			// Context: as the drain marker, at the drain position.
			//
			// One container per cycle, so the drain's cooldown still governs throughput —
			// emptying a whole barrel of buckets in a single tick would make the setting
			// meaningless.

			raw("execute unless data block ~ ~1 ~ Items[0] run return 0")

			data(Storage.WIRES) { remove("iq") }
			data(Storage.WIRES) { modify("iq.queue", Storage.WIRES, "item_sources") }
			function(Drain.drainAboveNext)
		}

		pack.define(Drain.drainAboveNext) {
			// Internal: walk the container registry until one of them empties.
			// Context: as the drain marker, at the drain position.

			raw("execute if score #dr.took ra.wires.tmp matches 1 run return 0")
			raw("execute unless data storage ra:wires iq.queue[0] run return 0")

			data(Storage.WIRES) { modify("iq.cur", Storage.WIRES, "iq.queue[0]") }
			data(Storage.WIRES) { remove("iq.queue[0]") }

			function(Drain.drainAboveTry, Storage.WIRES, "iq.cur")
			function(Drain.drainAboveNext)
		}

		pack.define(Drain.drainAboveTry) {
			// Internal: one registry entry against the container above the drain.
			// Context: as the drain marker, at the drain position.
			//
			// `container.*` rather than a slot, so it does not matter where in the barrel the
			// bucket sits — the same reason the EU Generator stopped reading Items[0].
			//
			// The network is charged BEFORE the container is swapped, and the swap only
			// happens if the offer was accepted in full. Emptying a bucket into a network
			// with room for part of it would have to either destroy the remainder or hand
			// back a partly full bucket, and there is no such item.

			raw("\$execute unless items block ~ ~1 ~ container.* \$(item) run return 0")

			function(RaLib.Transport.Net.read)
			raw("\$scoreboard players set #dr.need ra.wires.tmp \$(volume)")
			storeCompute("#dr.free", "ra.wires.tmp", Cip.sub(Cip.score("#net_capacity", "ra.tr.tmp"), Cip.score("#net_amount", "ra.tr.tmp")))
			raw("execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run data modify entity @s data.status.drain_state set value \"network_full\"")

			raw("\$execute store result score #dr.got ra.wires.tmp run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
			raw("execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run data modify entity @s data.status.drain_state set value \"wrong_medium\"")

			// Paid for, so now take the full container and leave the empty behind.
			//
			// The removal is CHECKED. `if items` sees the whole container, ra_lib:inventory
			// /remove reads the `Items` list, and the two do not agree on every block that
			// accepts items -- and when the removal quietly failed, the empty was handed out
			// anyway. That is one empty bucket per cycle, forever, out of a full one that
			// never left the container, with the network being charged for water each time.
			// Nothing is minted unless the full container actually came out.
			raw("\$execute positioned ~ ~1 ~ store result score #dr.rm ra.wires.tmp run function ${RaLib.Inventory.remove.asId()} {id:\"\$(item)\",count:1}")
			raw("\$execute if score #dr.rm ra.wires.tmp matches ..0 run function ${RaLib.Transport.Net.take.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
			raw("execute if score #dr.rm ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value \"nothing_to_empty\"")

			raw("\$execute positioned ~ ~1 ~ store result score #dr.put ra.wires.tmp run function ${RaLib.Inventory.insert.asId()} {id:\"\$(empty)\",count:1,components:{}}")

			// Removing one item does not guarantee a slot for the empty: the full stack may
			// have been a partial one that merged, leaving the container still full of other
			// things. Whatever will not fit is dropped rather than destroyed.
			raw("\$execute if score #dr.put ra.wires.tmp matches ..0 run summon item ~ ~1.2 ~ {Item:{id:\"\$(empty)\",count:1}}")

			scoreboard.players.set(literal("#dr.took"), "ra.wires.tmp", 1)
			raw("data modify entity @s data.status.drain_state set value \"emptied_container\"")
			raw("\$function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
			raw("playsound minecraft:item.bucket_empty block ${soundListenersSel(8)} ~ ~ ~ 0.7 1.2")
		}

		pack.define(Drain.drainTick) {
			// Move fluid between the world and the network.
			// Context: as a drain marker, at the drain position.
			//
			// The drain has three roles. Two are chosen with the wrench:
			// "drain" — take a world source into the network, same as a pump but slower
			// "place" — spend network contents putting the medium back into the world
			//
			// The third is chosen by how you place the block: stood vertically it stops
			// working on the world entirely and becomes the hand-loading point, where a
			// player empties a held container into the network. See ra_wires:fluid/drain_items.
			//
			// Mode "place" is what makes a fluid network worth building: it can carry lava
			// from a pool to wherever you want it, or refill cauldrons on the far side of a
			// base, instead of the contents only ever being a number on a billboard.

			raw("execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value \"drain\"")
			// Migration: this knob was briefly called `interval`. One name across the pack.
			raw("execute if data entity @s data.properties.interval unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set from entity @s data.properties.interval")
			data(self()) { remove("data.properties.interval") }
			raw("execute unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 20")

			// Throughput is the cooldown between actions, cycled through three steps with the
			// wrench. Compared as a score rather than baked into the selector, so the setting
			// takes effect the moment it is changed.
			scoreboard.players.add(self(), Obj.COOLDOWN, 1)
			function(RaLib.Util.property, nbt { this["name"] = "cooldown"; this["default"] = 20; this["min"] = 1 })
			scoreboard.players.operation(literal("#dr.cooldown"), "ra.wires.tmp", Operation.SET, literal("#prop"), "ra.temp")
			raw("execute if score @s ${Obj.COOLDOWN} < #dr.cooldown ra.wires.tmp run return 0")
			scoreboard.players.set(self(), Obj.COOLDOWN, 0)

			// ra.facing 0 is down and 1 is up: a drain on its end is the hand-loading point,
			// whatever mode it is set to.
			executeReturnRun("if score @s ra.facing matches 0..1", DrainItems.drainItems)

			executeRun("""if data entity @s data.properties{mode:"place"}""", DrainExperience.drainPlace)
			raw("""execute if data entity @s data.properties{mode:"place"} run return 0""")

			function(Source.scan)

			raw("execute unless data storage ra:wires found run data modify entity @s data.status.drain_state set value \"no_source\"")
			raw("execute unless data storage ra:wires found run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 4")
			raw("execute unless data storage ra:wires found run return 0")

			function(Pump.pumpExtract, Storage.WIRES, "found")
			data(self()) { modify("data.status.drain_state", self(), "data.status.pump_state") }
			data(Storage.WIRES) { remove("found") }
		}
	}
}
