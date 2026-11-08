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
	val mergeNext = lib("inventory/insert_or_drop/merge_next")
	val mergeOne = lib("inventory/insert_or_drop/merge_one")
	val normalizeProbe = lib("inventory/normalize_probe")
	val canAcceptPartial = lib("inventory/can_accept_partial")
	val canAcceptNext = lib("inventory/can_accept_partial/next")
	val canAcceptCheck = lib("inventory/can_accept_partial/check")
	val placeNext = lib("inventory/insert_or_drop/place_next")
	val stackSize = lib("inventory/stack_size")
	val writeCount = lib("inventory/insert_or_drop/write_count")

	private val INV = Storage.INVENTORY
	private val slot = fake("#inv_slot")
	private val probe = fake("#inv_probe")

	private const val NORM_TAG = "ra.inv.norm"
	private const val NORM_SEL = "@e[type=minecraft:item_display,tag=$NORM_TAG,distance=..0.01,limit=1]"

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
		pack.define(mergeNext) { mergeNext() }
		pack.define(mergeOne) { mergeOne() }
		pack.define(placeNext) { placeNext() }
		pack.define(normalizeProbe) { normalizeProbe() }
		pack.define(canAcceptPartial) { canAcceptPartial() }
		pack.define(canAcceptNext) { canAcceptNext() }
		pack.define(canAcceptCheck) { canAcceptCheck() }

		// `stack_size {n}`: the most items slot n of the container here can hold of what is in it,
		// into #inv_stack ra.temp. Read off the stack itself, so it is exact for any item: vanilla
		// stacks are 1, 16 or 64, and anything else carries an explicit max_stack_size component
		// (which also overrides the item's default). The slot must hold an item.
		pack.define(stackSize) {
			raw("scoreboard players set #inv_stack ${Obj.TEMP} 64")
			raw("\$execute if items block ~ ~ ~ container.\$(n) *[minecraft:max_stack_size=16] run scoreboard players set #inv_stack ${Obj.TEMP} 16")
			raw("\$execute if items block ~ ~ ~ container.\$(n) *[minecraft:max_stack_size=1] run scoreboard players set #inv_stack ${Obj.TEMP} 1")
			raw("\$execute if data block ~ ~ ~ Items[{Slot:\$(n)b}].components.\"minecraft:max_stack_size\" store result score #inv_stack ${Obj.TEMP} run data get block ~ ~ ~ Items[{Slot:\$(n)b}].components.\"minecraft:max_stack_size\"")
		}

		// `write_count {n}`: slot n's count becomes #inv_c ra.temp.
		pack.define(writeCount) {
			raw("\$execute store result block ~ ~ ~ Items[{Slot:\$(n)b}].count int 1 run scoreboard players get #inv_c ${Obj.TEMP}")
		}

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
	 * dropping as an item entity whatever will not fit. Returns the number inserted.
	 *
	 * Caller contract:
	 *   data modify storage ra:inventory item set from <your item>
	 *   function ra_lib:inventory/insert_or_drop
	 *
	 * The item is passed through storage rather than as macro arguments on purpose: an item's
	 * components can carry a player-authored name, and substituting that into a command literal
	 * breaks on a quote character.
	 *
	 * Same order as a player shift-clicking a stack in:
	 *   1. top up every partial stack of the same item (same id AND same components), in Items
	 *      order, up to that item's stack size;
	 *   2. put what is left into free slots, a full stack each;
	 *   3. drop the rest.
	 * So [1,1,1,_,_,_] + 1 cobblestone is [2,1,1,_,_,_], + 70 is [64,8,1,_,_,_]. Up to v6.0.1
	 * step 1 was missing (it had been dropped along with `loot insert`), and every delivery took
	 * a new slot.
	 *
	 * This does NOT use `loot insert`. Its return value is the number of item ENTRIES it handled,
	 * not the number of items, so the leftover cannot be worked out from it, and an earlier
	 * version inserted a stack AND dropped almost all of it again. Here every count is written
	 * explicitly, so nothing is duplicated or lost.
	 *
	 * "Same item" is tested without macros: a copy of each stack and a copy of the incoming item,
	 * both stripped of Slot and count, are compared with `data modify ... set from`, which fails
	 * when the value would not change -- exactly when the two are equal, components included.
	 * The incoming copy is first put in the form the container stores items in (normalize_probe):
	 * compared raw, an item built from a hand-written literal never matched its own stacks.
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

		// A container that has never held anything may have no Items list at all.
		execute {
			unlessCondition { data(here, "Items") }
			run { data(here) { modify("Items") { set(buildNbtList<Nothing> { }) } } }
		}

		// 1. Top up matching partial stacks.
		raw("data modify storage ra:inventory probe set from storage ra:inventory item")
		function(normalizeProbe)
		raw("data modify storage ra:inventory scan set from block ~ ~ ~ Items")
		function(mergeNext)
		raw("data remove storage ra:inventory scan")
		raw("data remove storage ra:inventory cmp")
		raw("data remove storage ra:inventory probe")

		// 2. Free slots.
		function(placeNext)
		data(INV) { remove("slotq") }

		// 3. Drop the rest, as one entity carrying the remaining count.
		raw("execute if score #inv_amount ${Obj.TEMP} matches 1.. store result storage ra:inventory item.count int 1 run scoreboard players get #inv_amount ${Obj.TEMP}")
		raw("execute if score #inv_amount ${Obj.TEMP} matches 1.. run function ${dropItem.asId()}")

		returnRun { scoreboard.players.get(put, Obj.TEMP) }
	}

	/**
	 * `normalize_probe`: turn storage ra:inventory probe (an item, any count) into the exact form
	 * a container stores it in, minus Slot and count, so it can be compared with `data modify ...
	 * set from` against a stack read off Items. Run positioned at the container.
	 *
	 * A container does not keep the NBT it is handed: every stack goes through the item codec on
	 * the way in and is written back out in canonical form. An item built from a hand-written
	 * literal can differ from that while being the same item to the game -- an `item_model` of
	 * `dead_brain_coral_block` is stored as `minecraft:dead_brain_coral_block`, components equal
	 * to the item's defaults are dropped, text is re-encoded. That is why the Rock Metallic
	 * Drill's rocks never matched the rock already in its barrel and took a new slot each time.
	 *
	 * So the probe takes the same round trip: it is written onto a throwaway item_display, which
	 * decodes it with the same codec, and read back. No macro is involved, so player-authored
	 * names are safe, and the display only ever holds a copy: the real item stays in storage
	 * ra:inventory item. The display is summoned empty, so if the game refuses the item there is
	 * nothing to read back and the probe is kept as it was, never replaced by something else.
	 */
	private fun Function.normalizeProbe() {
		raw("data remove storage ra:inventory probe.Slot")
		raw("data modify storage ra:inventory probe.count set value 1")
		// The codec drops an empty components map too; done here as well so the comparison
		// holds wherever the round trip is not available.
		raw("execute store result score #inv_clen ${Obj.TEMP} run data get storage ra:inventory probe.components")
		raw("execute if score #inv_clen ${Obj.TEMP} matches 0 run data remove storage ra:inventory probe.components")

		raw("data remove storage ra:inventory norm")
		raw("summon minecraft:item_display ~ ~ ~ {Tags:[\"ra\",\"$NORM_TAG\"]}")
		raw("data modify entity $NORM_SEL item set from storage ra:inventory probe")
		raw("data modify storage ra:inventory norm set from entity $NORM_SEL item")
		raw("kill @e[type=minecraft:item_display,tag=$NORM_TAG,distance=..0.01]")
		raw("execute if data storage ra:inventory norm.id run data modify storage ra:inventory probe set from storage ra:inventory norm")
		raw("data remove storage ra:inventory norm")

		raw("data remove storage ra:inventory probe.count")
	}

	/**
	 * `can_accept_partial`: is there a stack of storage ra:temp inv_item in the container here
	 * that is below its stack size? Sets #inv_can_insert ra.temp to 1 if so, leaves it alone
	 * otherwise. Same matching as insert_or_drop's top-up step (same id and components, after
	 * normalize_probe), and the same stack size (stack_size), so the Item Mover's "is there
	 * room" agrees with what an insert would really do.
	 */
	private fun Function.canAcceptPartial() {
		raw("data modify storage ra:inventory probe set from storage ra:temp inv_item")
		function(normalizeProbe)
		raw("data modify storage ra:inventory scan set from block ~ ~ ~ Items")
		function(canAcceptNext)
		raw("data remove storage ra:inventory scan")
		raw("data remove storage ra:inventory cmp")
		raw("data remove storage ra:inventory probe")
		data(INV) { remove("slotq") }
	}

	/** Walk storage ra:inventory scan until a matching stack with room turns up. */
	private fun Function.canAcceptNext() {
		raw("execute if score #inv_can_insert ${Obj.TEMP} matches 1.. run return 0")
		raw("execute unless data storage ra:inventory scan[0] run return 0")
		raw("data modify storage ra:inventory cmp set from storage ra:inventory scan[0]")
		raw("data remove storage ra:inventory cmp.Slot")
		raw("data remove storage ra:inventory cmp.count")
		raw("execute store success score #inv_diff ${Obj.TEMP} run data modify storage ra:inventory cmp set from storage ra:inventory probe")
		raw("execute if score #inv_diff ${Obj.TEMP} matches 0 run function ${canAcceptCheck.asId()}")
		raw("data remove storage ra:inventory scan[0]")
		function(canAcceptNext)
	}

	/** scan[0] matches: room left on it? */
	private fun Function.canAcceptCheck() {
		raw("execute store result score #inv_c ${Obj.TEMP} run data get storage ra:inventory scan[0].count")
		raw("execute store result storage ra:inventory slotq.n int 1 run data get storage ra:inventory scan[0].Slot")
		function(stackSize, INV, "slotq")
		raw("execute if score #inv_c ${Obj.TEMP} < #inv_stack ${Obj.TEMP} run scoreboard players set #inv_can_insert ${Obj.TEMP} 1")
	}

	/** Walk storage ra:inventory scan (a copy of Items), topping up each stack equal to probe. */
	private fun Function.mergeNext() {
		raw("execute if score #inv_amount ${Obj.TEMP} matches ..0 run return 0")
		raw("execute unless data storage ra:inventory scan[0] run return 0")
		raw("data modify storage ra:inventory cmp set from storage ra:inventory scan[0]")
		raw("data remove storage ra:inventory cmp.Slot")
		raw("data remove storage ra:inventory cmp.count")
		// Fails (0) when cmp already equals probe: same id and same components.
		raw("execute store success score #inv_diff ${Obj.TEMP} run data modify storage ra:inventory cmp set from storage ra:inventory probe")
		raw("execute if score #inv_diff ${Obj.TEMP} matches 0 run function ${mergeOne.asId()}")
		raw("data remove storage ra:inventory scan[0]")
		function(mergeNext)
	}

	/** scan[0] matches: move min(room, #inv_amount) onto it. */
	private fun Function.mergeOne() {
		raw("execute store result score #inv_c ${Obj.TEMP} run data get storage ra:inventory scan[0].count")
		// Through a score: Slot is a byte, and a macro would substitute it as "3b".
		raw("execute store result storage ra:inventory slotq.n int 1 run data get storage ra:inventory scan[0].Slot")
		function(stackSize, INV, "slotq")
		// add = min(stack - count, amount); nothing to do on a full stack.
		raw("scoreboard players operation #inv_add ${Obj.TEMP} = #inv_stack ${Obj.TEMP}")
		raw("scoreboard players operation #inv_add ${Obj.TEMP} -= #inv_c ${Obj.TEMP}")
		raw("execute if score #inv_add ${Obj.TEMP} matches ..0 run return 0")
		raw("scoreboard players operation #inv_add ${Obj.TEMP} < #inv_amount ${Obj.TEMP}")
		raw("scoreboard players operation #inv_c ${Obj.TEMP} += #inv_add ${Obj.TEMP}")
		raw("scoreboard players operation #inv_amount ${Obj.TEMP} -= #inv_add ${Obj.TEMP}")
		raw("scoreboard players operation #inv_put ${Obj.TEMP} += #inv_add ${Obj.TEMP}")
		function(writeCount, INV, "slotq")
	}

	/**
	 * While items remain and a slot is free: claim it with one item, read that item's stack size
	 * off the slot, then set the count to min(stack size, #inv_amount).
	 */
	private fun Function.placeNext() {
		raw("execute if score #inv_amount ${Obj.TEMP} matches ..0 run return 0")
		execute {
			storeResult { score(slot, Obj.TEMP) }
			run { function(RaLib.Inventory.findFreeSlot) }
		}
		raw("execute if score #inv_slot ${Obj.TEMP} matches ..-1 run return 0")

		data(INV) { modify("put") { set(INV, "item") } }
		raw("data modify storage ra:inventory put.count set value 1")
		raw("execute store result storage ra:inventory put.Slot byte 1 run scoreboard players get #inv_slot ${Obj.TEMP}")
		data(here) { modify("Items") { append(INV, "put") } }
		data(INV) { remove("put") }

		raw("execute store result storage ra:inventory slotq.n int 1 run scoreboard players get #inv_slot ${Obj.TEMP}")
		function(stackSize, INV, "slotq")
		raw("scoreboard players operation #inv_c ${Obj.TEMP} = #inv_stack ${Obj.TEMP}")
		raw("scoreboard players operation #inv_c ${Obj.TEMP} < #inv_amount ${Obj.TEMP}")
		function(writeCount, INV, "slotq")
		raw("scoreboard players operation #inv_amount ${Obj.TEMP} -= #inv_c ${Obj.TEMP}")
		raw("scoreboard players operation #inv_put ${Obj.TEMP} += #inv_c ${Obj.TEMP}")
		function(placeNext)
	}
}
