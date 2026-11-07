package ra.modules.wires.electric

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref

object Fuel : PackPart {
	val scan = ref(Ns.WIRES, "electric/fuel/scan")
	val slotNext = ref(Ns.WIRES, "electric/fuel/slot_next")
	val slotTry = ref(Ns.WIRES, "electric/fuel/slot_try")
	val take = ref(Ns.WIRES, "electric/fuel/take")

	override fun Ra.register() {
		pack.define(Fuel.scan) {
			// Light the next piece of fuel in the generator's own inventory.
			// Context: as the generator marker, at its block. Only called when nothing is
			// currently burning.
			//
			// THIS WALKS THE BARREL, NOT THE REGISTRY
			// Two earlier versions of this asked the registry first -- "is there coal in
			// there? is there charcoal in there?" -- sixteen questions per attempt, first
			// against Items[0] and then with `execute if items ... container.*`. Both were a
			// way of testing a container's contents indirectly, and when the generator went
			// on reporting no fuel with a full stack of coal in it, there was no way to tell
			// from the outside which of the two halves was lying.
			//
			// So it now reads the container the way the rest of the pack reads containers:
			// `data ... Items`, the same access drop_items, the Data Handler and the item
			// pipes have always used. Whatever is in slot n is looked up in fuel_map by its
			// own id -- one lookup, no search -- and taken out by index. Nothing here depends
			// on a command whose behaviour I could not verify from the outside.
			//
			// It is also cheaper. Before: sixteen tests whatever the barrel held. Now: one
			// lookup per stack actually present, which for a generator is nearly always one.

			raw("execute unless block ~ ~ ~ minecraft:barrel run return run data modify entity @s data.status.fuel set value \"Old block - break and replace\"")
			raw("execute unless data storage ra:wires fuel_map run return run data modify entity @s data.status.fuel set value \"Fuel registry missing - /reload\"")
			raw("execute unless data block ~ ~ ~ Items[0] run return 0")

			data(Storage.WIRES) { remove("fq") }
			raw("data modify storage ra:wires fq.scan set from block ~ ~ ~ Items")
			scoreboard.players.set(literal("#gen.i"), "ra.wires.tmp", 0)
			function(Fuel.slotNext)
			data(Storage.WIRES) { remove("fq") }
		}

		pack.define(Fuel.slotNext) {
			// Internal: walk the stacks in the barrel until one of them is a fuel.
			// Context: as the generator marker, at its block.
			//
			// fq.scan is a COPY of the container's Items. The copy is consumed from the front
			// while #gen.i counts the matching index in the real container, because the real
			// one must not be touched until something is actually taken from it -- removing
			// an entry mid-walk would shift every index after it.

			raw("execute if data entity @s data.data.burn run return 0")
			raw("execute unless data storage ra:wires fq.scan[0] run return 0")

			data(Storage.WIRES) { modify("fq.q.id", Storage.WIRES, "fq.scan[0].id") }
			raw("execute store result storage ra:wires fq.q.n int 1 run scoreboard players get #gen.i ra.wires.tmp")
			function(Fuel.slotTry, Storage.WIRES, "fq.q")

			data(Storage.WIRES) { remove("fq.scan[0]") }
			scoreboard.players.add(literal("#gen.i"), "ra.wires.tmp", 1)
			function(Fuel.slotNext)
		}

		pack.define(Fuel.slotTry) {
			// Internal: is the stack at index n a fuel? Context: as the marker, at the block.
			//
			// One keyed lookup. An id that is not a fuel simply has no entry, and `unless
			// data` on the result is the whole test -- no comparison, no search.

			raw("\$data modify storage ra:wires fq.cur set from storage ra:wires fuel_map.\"\$(id)\"")
			raw("execute unless data storage ra:wires fq.cur run return 0")

			raw("\$data modify storage ra:wires fq.cur.n set value \$(n)")
			function(Fuel.take, Storage.WIRES, "fq.cur")
		}

		pack.define(Fuel.take) {
			// Internal: burn one item out of slot index n. Context: as the marker, at the block.
			//
			// Taking one off a stack, by index, on the real container. The count is read
			// first so a stack of one is removed outright rather than left behind as a
			// zero-count entry, which renders as a ghost item and cannot be picked up.
			//
			// The burn is only set after the item is gone, so a failed take cannot hand out
			// free power.

			raw("\$execute store result score #gen.cnt ra.wires.tmp run data get block ~ ~ ~ Items[\$(n)].count")
			raw("execute if score #gen.cnt ra.wires.tmp matches ..0 run return 0")

			raw("\$execute if score #gen.cnt ra.wires.tmp matches 1 run data remove block ~ ~ ~ Items[\$(n)]")
			scoreboard.players.remove(literal("#gen.cnt"), "ra.wires.tmp", 1)
			raw("\$execute if score #gen.cnt ra.wires.tmp matches 1.. store result block ~ ~ ~ Items[\$(n)].count int 1 run scoreboard players get #gen.cnt ra.wires.tmp")

			raw("\$data modify entity @s data.data.burn set value \$(ticks)")
			raw("\$data modify entity @s data.status.fuel set value \"\$(name)\"")

			raw("playsound minecraft:block.furnace.fire_crackle block @a[distance=..12,scores={ra.u.snd=1..}] ~ ~ ~ 0.4 1.2")
		}
	}
}
