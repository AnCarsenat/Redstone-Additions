package ra.lib.inventory

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import net.benwoodworth.knbt.buildNbtList
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.lib.BlockTags
import ra.lib.fake
import ra.lib.inventory.Inventory.here
import ra.lib.inventory.Inventory.len
import ra.lib.inventory.Inventory.max
import ra.lib.lib

/** `insert`, `insert_or_drop` and the free-slot search they rely on. */
internal object InsertOrDrop {
	val dropItem = lib("inventory/drop_item")
	val findFreeSlotStep = lib("inventory/find_free_slot/step")
	val findFreeSlotTest = lib("inventory/find_free_slot/test")

	private val INV = Storage.INVENTORY
	private val slot = fake("#inv_slot")
	private val probe = fake("#inv_probe")

	fun Ra.registerInsertOrDrop() {
		// `insert {id, count, components}`: insert an item into the container at the current
		// position using /loot insert. Stacking is handled by the game.
		//
		// FALLBACK PATH. Prefer ra_lib:inventory/move_slot, which moves a whole stack with /item
		// replace: no loot table to parse, and the stack crosses verbatim instead of being rebuilt
		// from an id plus a components blob. Use this only when the destination has no free slot
		// and the items have to merge into partial stacks, which /item cannot express.
		//
		// Caveat: whatever does not fit is DESTROYED, and the return value cannot be used to work
		// out how much that was. `loot insert` reports the number of item entries it handled, not
		// the number of items, so a 64-stack reports far less than 64 even when all of it went in.
		// insert_or_drop originally subtracted this from the requested count and dropped the
		// difference -- inserting the stack AND dropping almost all of it again.
		//
		// Only safe with a count of 1, where "handled" and "inserted" coincide and the result is a
		// plain success flag. For anything larger use insert_or_drop, which picks a slot explicitly
		// and never consults a loot table.
		//
		// Input, as macro arguments: $(id) item id, $(count), $(components) ({} or a component map).
		// Output: returns the number of items inserted (0 if the container is full).
		// Example: function ra_lib:inventory/insert with storage ra:temp pipe_item
		pack.define(RaLib.Inventory.insert) {
			raw("\$return run loot insert ~ ~ ~ loot {pools:[{rolls:1,entries:[{type:\"item\",name:\"\$(id)\",modifier:[{type:\"set_count\",count:\$(count)},{type:\"set_components\",components:\$(components)}]}]}]}")
		}

		pack.define(RaLib.Inventory.insertOrDrop) { insertOrDrop() }

		// `drop_item`: drop storage ra:inventory item on the floor at the current position.
		//
		// Summoned with a placeholder and then overwritten from storage, so the item's components
		// cross verbatim instead of being rebuilt from a macro literal.
		pack.define(dropItem) {
			raw("summon item ~ ~0.5 ~ {Item:{id:\"minecraft:stone\",count:1},PickupDelay:20,Tags:[\"ra\",\"ra.inv.overflow\"]}")
			raw("data modify entity @e[type=item,tag=ra.inv.overflow,distance=..2,limit=1,sort=nearest] Item set from storage ra:inventory item")
			raw("tag @e[type=item,tag=ra.inv.overflow,distance=..2] remove ra.inv.overflow")
		}

		pack.define(RaLib.Inventory.findFreeSlot) { findFreeSlot() }

		// Internal: test one slot index, then move on.
		pack.define(findFreeSlotStep) {
			returnIfFound()
			execute {
				ifCondition { score(probe, Obj.TEMP) greaterThanOrEqualTo score(max, Obj.TEMP) }
				run { returnValue(0) }
			}

			testProbe()

			returnIfFound()
			scoreboard.players.add(probe, Obj.TEMP, 1)
			function(findFreeSlotStep)
		}

		// `test {n}`, internal: claim slot n if nothing occupies it.
		pack.define(findFreeSlotTest) {
			raw("\$execute unless data block ~ ~ ~ Items[{Slot:\$(n)b}] run scoreboard players operation #inv_slot ${Obj.TEMP} = #inv_probe ${Obj.TEMP}")
		}

		// `has_free_slot`: does the container at the current position have a completely empty
		// slot? Returns 1 or 0.
		pack.define(RaLib.Inventory.hasFreeSlot) {
			function(RaLib.Inventory.containerSize)

			scoreboard.players.set(len, Obj.TEMP, 0)
			storeItemsLength()

			execute {
				ifCondition { score(len, Obj.TEMP) lessThan score(max, Obj.TEMP) }
				run { returnValue(1) }
			}
			returnValue(0)
		}
	}

	private fun Function.returnIfFound() = execute {
		ifCondition { score(slot, Obj.TEMP) greaterThanOrEqualTo 0 }
		run { returnValue(0) }
	}

	/** Write #inv_probe into slotq.n and test that slot. */
	private fun Function.testProbe() {
		raw("execute store result storage ra:inventory slotq.n int 1 run scoreboard players get #inv_probe ${Obj.TEMP}")
		function(findFreeSlotTest, INV, "slotq")
	}

	/**
	 * `find_free_slot`: index of an empty slot in the container at the current position.
	 * Output: #inv_slot ra.temp, or -1 when the container is full.
	 *
	 * Items is a list of the stacks that exist, each carrying its own Slot number, so the list
	 * index is not the slot index and a container can be non-contiguous. The number of stacks
	 * present is tried first because containers usually do fill from slot 0 upward, which makes
	 * the common case a single check; only when that guess is taken does the linear scan run.
	 */
	private fun Function.findFreeSlot() {
		function(RaLib.Inventory.containerSize)

		scoreboard.players.set(len, Obj.TEMP, 0)
		storeItemsLength()

		scoreboard.players.set(slot, Obj.TEMP, -1)
		execute {
			ifCondition { score(len, Obj.TEMP) greaterThanOrEqualTo score(max, Obj.TEMP) }
			run { returnValue(-1) }
		}

		// Fast guess.
		scoreboard.players.operation(probe, Obj.TEMP, Operation.SET, len, Obj.TEMP)
		testProbe()
		execute {
			ifCondition { score(slot, Obj.TEMP) greaterThanOrEqualTo 0 }
			run { returnRun { scoreboard.players.get(slot, Obj.TEMP) } }
		}

		// Otherwise walk the slots.
		scoreboard.players.set(probe, Obj.TEMP, 0)
		function(findFreeSlotStep)

		data(INV) { remove("slotq") }
		returnRun { scoreboard.players.get(slot, Obj.TEMP) }
	}

	/**
	 * `insert_or_drop`: put storage ra:inventory item into the container at the current position,
	 * or drop it as an item entity if it will not fit. Returns the number inserted.
	 *
	 * Caller contract:
	 *   data modify storage ra:inventory item set from <your item>
	 *   function ra_lib:inventory/insert_or_drop
	 *
	 * The item is passed through storage rather than as macro arguments on purpose: an item's
	 * components can carry a player-authored name, and substituting that into a command literal
	 * breaks on a quote character.
	 *
	 * This does NOT use `loot insert`. That command's return value is the number of item ENTRIES
	 * it handled, not the number of items, so subtracting it from the requested count reported
	 * almost a whole stack as "left over" -- and the stack was then inserted AND dropped. The slot
	 * is chosen explicitly and the stack is appended to the container's Items list instead, which
	 * is exact and cannot double up.
	 *
	 * Trade-off: this claims a free slot rather than topping up a matching partial stack. A
	 * container with no free slot drops, even if the items would have merged. That is
	 * predictable, and never duplicates.
	 */
	private fun Function.insertOrDrop() {
		val put = fake("#inv_put")
		val amount = fake("#inv_amount")

		scoreboard.players.set(put, Obj.TEMP, 0)
		data(INV) { remove("item.Slot") }

		raw("execute store result score #inv_amount ${Obj.TEMP} run data get storage ra:inventory item.count")
		execute {
			ifCondition { score(amount, Obj.TEMP) lessThanOrEqualTo 0 }
			run { returnValue(0) }
		}

		// Not a container at all: nothing to insert into.
		execute {
			unlessCondition { block(here, BlockTags.containers) }
			run { function(dropItem) }
		}
		execute {
			unlessCondition { block(here, BlockTags.containers) }
			run { returnValue(0) }
		}

		execute {
			storeResult { score(slot, Obj.TEMP) }
			run { function(RaLib.Inventory.findFreeSlot) }
		}
		execute {
			ifCondition { score(slot, Obj.TEMP) lessThanOrEqualTo -1 }
			run { function(dropItem) }
		}
		execute {
			ifCondition { score(slot, Obj.TEMP) lessThanOrEqualTo -1 }
			run { returnValue(0) }
		}

		// A container that has never held anything may have no Items list at all.
		execute {
			unlessCondition { data(here, "Items") }
			run { data(here) { modify("Items") { set(buildNbtList<Nothing> { }) } } }
		}

		data(INV) { modify("put") { set(INV, "item") } }
		raw("execute store result storage ra:inventory put.Slot byte 1 run scoreboard players get #inv_slot ${Obj.TEMP}")
		data(here) { modify("Items") { append(INV, "put") } }
		data(INV) { remove("put") }

		scoreboard.players.operation(put, Obj.TEMP, Operation.SET, amount, Obj.TEMP)
		returnRun { scoreboard.players.get(put, Obj.TEMP) }
	}
}
