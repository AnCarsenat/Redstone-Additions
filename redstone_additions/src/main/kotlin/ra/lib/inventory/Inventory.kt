package ra.lib.inventory

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import net.benwoodworth.knbt.buildNbtList
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.dummyObjectives
import ra.lib.fake
import ra.lib.lib
import ra.lib.inventory.InsertOrDrop.registerInsertOrDrop
import ra.lib.inventory.Remove.registerRemove

/** `ra_lib:inventory/...`: container helpers, all at the container's position. */
object Inventory {
	val init = lib("inventory/init")

	internal val here = vec3().relative
	internal val canInsert = fake("#inv_can_insert")
	internal val len = fake("#inv_len")
	internal val max = fake("#inv_max")

	/** Slot count per container type; anything else (chests, barrels, shulker boxes) has 27. */
	private val sizes = listOf(
		Blocks.HOPPER to 5, Blocks.BREWING_STAND to 5, Blocks.DROPPER to 9, Blocks.DISPENSER to 9,
		Blocks.CRAFTER to 9, Blocks.FURNACE to 3, Blocks.BLAST_FURNACE to 3, Blocks.SMOKER to 3,
	)

	fun Ra.registerInventory() {
		// Setup inventory scoreboards.
		pack.define(init) { dummyObjectives(Obj.INV_COUNT, Obj.TEMP) }

		// Clear all items from the container at the current position.
		pack.define(RaLib.Inventory.clear) { data(here) { modify("Items") { set(buildNbtList<Nothing> { }) } } }

		// `container_size`: slot count of the container at the current position, into
		// #inv_max ra.temp. Defaults to 27, which covers chests, barrels and shulker boxes.
		pack.define(RaLib.Inventory.containerSize) {
			scoreboard.players.set(max, Obj.TEMP, 27)
			for ((block, size) in sizes) execute {
				ifCondition { block(here, block) }
				run { scoreboard.players.set(max, Obj.TEMP, size) }
			}
		}

		pack.define(RaLib.Inventory.canAcceptOne) { canAcceptOne() }


		// `consume_entry0`: consume one item from Items[0] at the current container position.
		// Returns 1 on success, 0 if the slot is empty.
		pack.define(RaLib.Inventory.consumeEntry0) {
			returnUnlessEntry0()
			countEntry0()

			raw("execute if score @s ${Obj.INV_COUNT} matches 1 run data remove block ~ ~ ~ Items[0]")
			execute {
				ifCondition { score(self(), Obj.INV_COUNT) equalTo 1 }
				run { returnValue(1) }
			}

			scoreboard.players.remove(self(), Obj.INV_COUNT, 1)
			raw("execute store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ${Obj.INV_COUNT}")
			returnValue(1)
		}

		// `consume_entry0_count {count}`: consume N items from Items[0] at the current container
		// position. Returns 1 on success, 0 if the slot is missing or holds too few.
		pack.define(RaLib.Inventory.consumeEntry0Count) {
			returnUnlessEntry0()
			countEntry0()
			raw("\$scoreboard players set @s ${Obj.TEMP} \$(count)")

			execute {
				ifCondition { score(self(), Obj.INV_COUNT) lessThan score(self(), Obj.TEMP) }
				run { returnValue(0) }
			}
			scoreboard.players.operation(self(), Obj.INV_COUNT, Operation.REMOVE, self(), Obj.TEMP)

			raw("execute if score @s ${Obj.INV_COUNT} matches 0 run data remove block ~ ~ ~ Items[0]")
			raw("execute if score @s ${Obj.INV_COUNT} matches 1.. store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ${Obj.INV_COUNT}")

			returnValue(1)
		}

		// `move_slot {src, src_slot, dst_slot}`: move one whole slot from a source container into
		// the container at the current position. The destination slot must already be empty.
		//
		// This is the primitive the rest of the library should be built on. `/item replace ...
		// from block ...` copies a stack across verbatim -- every component, the exact count -- with
		// no loot table to parse and no NBT arithmetic. The older path went through `loot insert`
		// with a hand-built loot table that reconstructed the item from an id, a count and a
		// components blob, one item at a time.
		pack.define(RaLib.Inventory.moveSlot) {
			raw("\$item replace block ~ ~ ~ container.\$(dst_slot) from block \$(src) container.\$(src_slot)")
			raw("\$item replace block \$(src) container.\$(src_slot) with air")
		}

		registerInsertOrDrop()
		registerRemove()
	}

	private fun Function.returnUnlessEntry0() = execute {
		unlessCondition { data(here, "Items[0]") }
		run { returnValue(0) }
	}

	private fun Function.countEntry0() = raw("execute store result score @s ${Obj.INV_COUNT} run data get block ~ ~ ~ Items[0].count")

	/**
	 * `can_accept_one`: can the container at the current position accept one storage ra:temp
	 * inv_item? Output: #inv_can_insert ra.temp (1 = yes, 0 = no), also returned.
	 */
	private fun Function.canAcceptOne() {
		scoreboard.players.set(canInsert, Obj.TEMP, 0)
		scoreboard.players.set(len, Obj.TEMP, 0)
		scoreboard.players.set(max, Obj.TEMP, 27)

		// A shulker box never fits in a shulker box: no room, whatever the slots say.
		raw("execute if block ~ ~ ~ #minecraft:shulker_boxes if function ${InsertOrDrop.isShulkerProbe.asId()} run return 0")

		// Capacity by container type.
		for ((block, size) in listOf(Blocks.HOPPER to 5, Blocks.DROPPER to 9, Blocks.DISPENSER to 9)) execute {
			ifCondition { block(here, block) }
			run { scoreboard.players.set(max, Obj.TEMP, size) }
		}

		// A free entry slot means we can insert.
		storeItemsLength()
		execute {
			ifCondition { score(len, Obj.TEMP) lessThan score(max, Obj.TEMP) }
			run { scoreboard.players.set(canInsert, Obj.TEMP, 1) }
		}

		// With no free entry, allow stacking onto a compatible partial stack. This used to be an
		// Items[{id:...,count:..63}] filter, which never matched anything: an NBT filter compares
		// values exactly, `..63` is not a range there. A full container with room on its partial
		// stacks always reported full.
		execute {
			ifCondition { score(canInsert, Obj.TEMP) equalTo 0 }
			ifCondition { data(here, "Items[0]") }
			run { function(InsertOrDrop.canAcceptPartial) }
		}

		returnRun { scoreboard.players.get(canInsert, Obj.TEMP) }
	}
}

/** `execute if data block ~ ~ ~ Items store result score #inv_len ra.temp run data get block ~ ~ ~ Items`. */
internal fun Function.storeItemsLength() =
	raw("execute if data block ~ ~ ~ Items store result score #inv_len ${Obj.TEMP} run data get block ~ ~ ~ Items")
