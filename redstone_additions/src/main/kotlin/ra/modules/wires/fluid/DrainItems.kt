package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object DrainItems : PackPart {
	val drainItemNext = ref(Ns.WIRES, "fluid/drain_item_next")
	val drainItemTry = ref(Ns.WIRES, "fluid/drain_item_try")
	val drainItems = ref(Ns.WIRES, "fluid/drain_items")
	val drainLoose = ref(Ns.WIRES, "fluid/drain_loose")
	val drainLooseNext = ref(Ns.WIRES, "fluid/drain_loose_next")
	val drainLooseTry = ref(Ns.WIRES, "fluid/drain_loose_try")

	override fun Ra.register() {
		pack.define(DrainItems.drainItemNext) {
			// Internal: walk the held-container registry until one of them empties.
			// Context: as the drain marker, at the drain position.

			raw("execute if score #dr.took ra.wires.tmp matches 1 run return 0")
			raw("execute unless data storage ra:wires iq.queue[0] run return 0")

			data(Storage.WIRES) { modify("iq.cur", Storage.WIRES, "iq.queue[0]") }
			data(Storage.WIRES) { remove("iq.queue[0]") }

			function(DrainItems.drainItemTry, Storage.WIRES, "iq.cur")
			function(DrainItems.drainItemNext)
		}

		pack.define(DrainItems.drainItemTry) {
			// Internal: one registry entry against a nearby sneaking player's main hand.
			// Context: as the drain marker, at the drain position.
			//
			// The candidate is marked rather than matched twice. `if items entity` is the
			// version-stable way to read a held stack in this pack, but it needs the player
			// as @s, and the network offer needs the drain as @s — so the player is tagged in
			// their own context and picked back up by tag in ours.
			//
			// The network is charged BEFORE the item is swapped, and the swap only happens if
			// the offer was accepted in full. Emptying a bucket into a network with room for
			// part of it would have to either destroy the remainder or hand back a partly
			// full bucket, and there is no such item.

			raw("\$execute as @a[distance=..2.5,predicate=ra:is_sneaking,sort=nearest,limit=1] if items entity @s weapon.mainhand \$(item) run tag @s add ra.wires.emptying")
			raw("execute unless entity @a[tag=ra.wires.emptying,limit=1] run return 0")

			// Room for the whole container, or nothing happens.
			function(RaLib.Transport.Net.read)
			raw("\$scoreboard players set #dr.need ra.wires.tmp \$(volume)")
			scoreboard.players.operation(literal("#dr.free"), "ra.wires.tmp", Operation.SET, literal("#net_capacity"), "ra.tr.tmp")
			scoreboard.players.operation(literal("#dr.free"), "ra.wires.tmp", Operation.REMOVE, literal("#net_amount"), "ra.tr.tmp")
			raw("execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value \"network_full\"")
			raw("execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @a[tag=ra.wires.emptying] remove ra.wires.emptying")

			raw("\$execute store result score #dr.got ra.wires.tmp run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
			raw("execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value \"wrong_medium\"")
			raw("execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @a[tag=ra.wires.emptying] remove ra.wires.emptying")

			// A potion is the medium `potion` by volume, but which potion it is has to be
			// remembered before the bottle leaves the player's hand.
			raw("\$execute if data storage ra:wires media.\$(medium).effect run function ${Fluid.potionStore.asId()}")

			// Paid for, so now take the container and leave the empty behind.
			//
			// ONE ITEM OFF THE STACK, NOT THE STACK
			// Buckets do not stack, but experience bottles and potions-in-hand can, and
			// `item replace` overwrites the whole slot: emptying one of sixteen experience
			// bottles used to destroy the other fifteen and hand back a single empty. A
			// stack is decremented and the empty given separately; a single item still goes
			// through `item replace`, which cannot fail on a full inventory.
			raw("\$execute store result score #dr.hand ra.wires.tmp run execute if items entity @p[tag=ra.wires.emptying] weapon.mainhand \$(item)")

			raw("execute if score #dr.hand ra.wires.tmp matches 2.. run item modify entity @p[tag=ra.wires.emptying] weapon.mainhand ra_wires:take_one")
			raw("\$execute if score #dr.hand ra.wires.tmp matches 2.. run give @a[tag=ra.wires.emptying] \$(empty)")
			raw("\$execute if score #dr.hand ra.wires.tmp matches ..1 run item replace entity @a[tag=ra.wires.emptying,limit=1] weapon.mainhand with \$(empty)")

			raw("tag @a[tag=ra.wires.emptying] remove ra.wires.emptying")

			scoreboard.players.set(literal("#dr.took"), "ra.wires.tmp", 1)
			raw("data modify entity @s data.status.drain_state set value \"emptied_container\"")
			raw("\$function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
			raw("playsound minecraft:item.bucket_empty block ${soundListenersSel(8)} ~ ~ ~ 0.7 1.2")
		}

		pack.define(DrainItems.drainItems) {
			// A vertical drain takes filled containers into the network.
			// Context: as the drain marker, at the drain position.
			//
			// WHY VERTICAL
			// A drain lying flat is working on the world: it takes a source block beside it,
			// or puts one back. Stood on end it has nothing sensible to do with the block
			// above or below, so that placement is given the other job — it becomes the point
			// where a base's contents go in. One block, two roles, decided by how you place
			// it, with no extra property to discover.
			//
			// THREE WAYS IN, TRIED IN THAT ORDER
			// 1. a sneaking player's main hand
			// 2. a container sitting on top of the drain
			// 3. loose item entities on top of the drain
			//
			// Only the first needs sneaking. A bucket is also how you pick fluid UP, so
			// without that gate, walking past a drain with a full bucket would quietly empty
			// it. Nothing you have deliberately put in a barrel or dropped on the block is
			// ambiguous in that way, which is why the other two run unattended — and being
			// able to run unattended is the whole point of them: a hopper feeding buckets
			// into a barrel over a drain is how a base loads a network without a player
			// standing there.
			//
			// This used to return here when nobody was sneaking nearby, which is why a barrel
			// of water buckets sat on top of a drain did nothing at all.

			scoreboard.players.set(literal("#dr.took"), "ra.wires.tmp", 0)

			data(Storage.WIRES) { remove("iq") }
			data(Storage.WIRES) { modify("iq.queue", Storage.WIRES, "item_sources") }
			executeRun("if entity @a[distance=..2.5,predicate=ra:is_sneaking,limit=1]", DrainItems.drainItemNext)

			executeRun("if score #dr.took ra.wires.tmp matches 0", Drain.drainAbove)
			executeRun("if score #dr.took ra.wires.tmp matches 0", DrainItems.drainLoose)

			// Nothing to empty, so fall through to the other thing a player standing on a
			// vertical drain can give it: their own experience.
			executeRun("if score #dr.took ra.wires.tmp matches 0", DrainExperience.drainExp)

			raw("execute if score #dr.took ra.wires.tmp matches 0 unless data entity @s data.status{drain_state:\"absorbing_xp\"} unless data entity @s data.status{drain_state:\"network_full\"} unless data entity @s data.status{drain_state:\"wrong_medium\"} run data modify entity @s data.status.drain_state set value \"nothing_to_empty\"")
			data(Storage.WIRES) { remove("iq") }
		}

		pack.define(DrainItems.drainLoose) {
			// Empty one filled container lying loose on top of the drain.
			// Context: as the drain marker, at the drain position.
			//
			// Separate from drain_above because a dropped stack is not a container: it has a
			// count that has to be decremented rather than a slot to swap, and the empty it
			// leaves behind has nowhere to go but the ground.

			raw("execute unless entity @e[type=item,distance=..2.5] run return 0")

			data(Storage.WIRES) { remove("iq") }
			data(Storage.WIRES) { modify("iq.queue", Storage.WIRES, "item_sources") }
			function(DrainItems.drainLooseNext)
		}

		pack.define(DrainItems.drainLooseNext) {
			// Internal: walk the container registry until one of them empties.
			// Context: as the drain marker, at the drain position.

			raw("execute if score #dr.took ra.wires.tmp matches 1 run return 0")
			raw("execute unless data storage ra:wires iq.queue[0] run return 0")

			data(Storage.WIRES) { modify("iq.cur", Storage.WIRES, "iq.queue[0]") }
			data(Storage.WIRES) { remove("iq.queue[0]") }

			function(DrainItems.drainLooseTry, Storage.WIRES, "iq.cur")
			function(DrainItems.drainLooseNext)
		}

		pack.define(DrainItems.drainLooseTry) {
			// Internal: one registry entry against the item entities above the drain.
			// Context: as the drain marker, at the drain position.
			//
			// The candidate is tagged rather than matched twice: the count has to be read and
			// written on the same entity the offer was made for, and re-running the selector
			// between those two steps could land on a different stack.
			//
			// Only the block directly above is searched. A drain should take what has been
			// dropped ON it, not vacuum the floor around it — that is the Magic Crate's job.

			raw("\$execute positioned ~ ~1 ~ as @e[type=item,distance=..1.2,nbt={Item:{id:\"\$(item)\"}},sort=nearest,limit=1] run tag @s add ra.wires.draining_item")
			raw("execute unless entity @e[type=item,tag=ra.wires.draining_item,limit=1] run return 0")

			function(RaLib.Transport.Net.read)
			raw("\$scoreboard players set #dr.need ra.wires.tmp \$(volume)")
			scoreboard.players.operation(literal("#dr.free"), "ra.wires.tmp", Operation.SET, literal("#net_capacity"), "ra.tr.tmp")
			scoreboard.players.operation(literal("#dr.free"), "ra.wires.tmp", Operation.REMOVE, literal("#net_amount"), "ra.tr.tmp")
			raw("execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value \"network_full\"")
			raw("execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item")

			raw("\$execute store result score #dr.got ra.wires.tmp run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(volume),medium:\"\$(medium)\"}")
			raw("execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value \"wrong_medium\"")
			raw("execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item")

			// Paid for. One item off the stack — buckets and potions never stack, but
			// experience bottles do, and killing the whole stack for one bottle's worth of
			// experience would eat the other sixty-three.
			raw("execute store result score #dr.cnt ra.wires.tmp run data get entity @e[type=item,tag=ra.wires.draining_item,limit=1] Item.count")
			scoreboard.players.remove(literal("#dr.cnt"), "ra.wires.tmp", 1)
			raw("execute if score #dr.cnt ra.wires.tmp matches ..0 run kill @e[type=item,tag=ra.wires.draining_item]")
			raw("execute if score #dr.cnt ra.wires.tmp matches 1.. store result entity @e[type=item,tag=ra.wires.draining_item,limit=1] Item.count int 1 run scoreboard players get #dr.cnt ra.wires.tmp")
			raw("tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item")

			raw("\$summon item ~ ~1.2 ~ {Item:{id:\"\$(empty)\",count:1}}")

			scoreboard.players.set(literal("#dr.took"), "ra.wires.tmp", 1)
			raw("data modify entity @s data.status.drain_state set value \"emptied_container\"")
			raw("\$function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
			raw("playsound minecraft:item.bucket_empty block ${soundListenersSel(8)} ~ ~ ~ 0.7 1.2")
		}
	}
}
