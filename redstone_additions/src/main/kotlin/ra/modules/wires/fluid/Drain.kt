package ra.modules.wires.fluid

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
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object Drain : PackPart {
	val drainAbove = ref(Ns.WIRES, "fluid/drain_above")
	val drainAboveNext = ref(Ns.WIRES, "fluid/drain_above_next")
	val drainAboveTry = ref(Ns.WIRES, "fluid/drain_above_try")
	val drainTick = ref(Ns.WIRES, "fluid/drain_tick")

	/** Default `speed`: litres per second, i.e. one 5 L source block a second (20 ticks). */
	const val DEFAULT_SPEED = 5

	/** Ticks between actions at speed 1; the interval is this divided by `speed`. */
	const val TICKS_AT_SPEED_1 = 100

	override fun Ra.register() {
		pack.define(Drain.drainAbove) {
			// Empty one filled container out of the block sitting on top of the drain.
			// Context: as the drain marker, at the drain position.
			//
			// one container per cycle, so the drain's speed still governs throughput —
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
			computeScore("#dr.free", "ra.wires.tmp", provider { sub(scoreOf("#net_capacity", "ra.tr.tmp"), scoreOf("#net_amount", "ra.tr.tmp")) })
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
			raw("\$function ${Fluid.drip.asId()} {medium:\"\$(medium)\"}")
			raw("playsound minecraft:item.bucket_empty block ${soundListenersSel(8)} ~ ~ ~ 0.7 1.2")
		}

		pack.define(Drain.drainTick) {
			// Move fluid between the world and the network.
			// Context: as a drain marker, at the drain position.
			//
			// WHAT IT DOES, IN ORDER
			// 1. Powered by redstone: pour out -- spend network contents putting the medium
			//    back into the world (a source block, orbs, or a potion over nearby players).
			// 2. Stood on end (facing up or down): be a loading point first -- empty a sneaking
			//    player's held container, a container on top, or dropped containers into the
			//    network, or take a sneaking player's experience. See ra_wires:fluid/drain_items.
			// 3. Otherwise -- and also when a vertical drain had nothing to load -- take an
			//    adjacent world source in, like a pump.
			//
			// WHY THIS REPLACED THE `mode` PROPERTY (6.0.1)
			// A drain picks its facing from where the player looks, and placing one on the
			// ground beside a pool means looking down: it came out vertical. A vertical drain
			// used to be ONLY a loading point and returned before the world scan, so it never
			// took in a single block of the pool beside it, and the wrench's drain/place mode
			// was never even read. Pouring out is now the redstone signal, which a player can
			// see and change, and a vertical drain still drains the world when it has nothing
			// to load.

			// Throughput: one action every 100 / speed ticks, so speed is litres per second at
			// 5 L a source block (5 = one block a second, the old default of 20 ticks).
			// Compared as a score rather than baked into the selector, so a change applies at once.
			scoreboard.players.add(self(), Obj.COOLDOWN, 1)
			function(RaLib.Util.property, nbt { this["name"] = "speed"; this["default"] = DEFAULT_SPEED; this["min"] = 1 })
			scoreboard.players.set(literal("#dr.ticks"), "ra.wires.tmp", TICKS_AT_SPEED_1)
			scoreboard.players.operation(literal("#dr.ticks"), "ra.wires.tmp", Operation.DIVIDE, literal("#prop"), "ra.temp")
			raw("execute if score @s ${Obj.COOLDOWN} < #dr.ticks ra.wires.tmp run return 0")
			scoreboard.players.set(self(), Obj.COOLDOWN, 0)

			// 1. Redstone pours out.
			executeReturnRun("if function ${RaLib.Redstone.any.asId()}", DrainExperience.drainPlace)

			// 2. A vertical drain loads containers and experience first. Anything other than
			// "nothing_to_empty" means it acted, or has something to say (network full, wrong
			// medium) that a world-scan status must not overwrite.
			raw("execute if score @s ra.facing matches 0..1 run data modify entity @s data.status.drain_state set value \"idle\"")
			executeRun("if score @s ra.facing matches 0..1", DrainItems.drainItems)
			raw("execute if score @s ra.facing matches 0..1 unless data entity @s data.status{drain_state:\"nothing_to_empty\"} run return 0")

			// 3. Take a world source in.
			function(Source.scan)

			raw("execute unless data storage ra:wires found run data modify entity @s data.status.drain_state set value \"no_source\"")
			raw("execute unless data storage ra:wires found unless score @s ra.facing matches 0..1 run particle minecraft:smoke ~ ~0.8 ~ 0.25 0.25 0.25 0.01 4 normal ${particleViewersSel()}")
			raw("execute unless data storage ra:wires found run return 0")

			data(self()) { remove("data.status.pump_state") }
			function(Pump.pumpExtract, Storage.WIRES, "found")
			data(self()) { modify("data.status.drain_state", self(), "data.status.pump_state") }
			executeRun("if data entity @s data.status{pump_state:\"pumping\"}", Fluid.drip, "with storage ra:wires found")
			raw("execute if data entity @s data.status{pump_state:\"pumping\"} run playsound minecraft:item.bucket_fill block ${soundListenersSel(8)} ~ ~ ~ 0.5 1.2")
			data(Storage.WIRES) { remove("found") }
		}
	}
}
