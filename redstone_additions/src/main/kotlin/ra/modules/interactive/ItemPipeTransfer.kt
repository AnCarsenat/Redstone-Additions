package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.tag
import ra.core.executeRun
import ra.core.executeStoreRun
import ra.core.callWith
import ra.core.macroCall
import ra.core.Tag

/**
 * How an Item Pipe moves a stack: forward (`transfer`), or sideways after a filter match
 * (`check_filter` -> `filter_insert`). Both try the whole stack at once (`move_stack`) and fall back
 * to one item at a time (`*_loop`) when the destination has no free slot.
 *
 * The forward and filtered paths are the same algorithm with the source in a different place:
 * forward, the pipe is one block behind the destination (`^ ^ ^-1`); sideways, the destination is
 * a neighbour and the pipe is wherever the marker stands (`at @s`, or the `sx` offset).
 */
object ItemPipeTransfer {
	private val block = ItemPipe.block
	private val checkFilter = block.fn("check_filter")
	private val filterInsert = block.fn("filter_insert")
	private val filterInsertLoop = block.fn("filter_insert_loop")
	private val moveStack = block.fn("move_stack")
	private val transfer = block.fn("transfer")
	private val transferLoop = block.fn("transfer_loop")

	private const val MOVED = "ra.pipe_moved_any"

	/** `#ra_lib:containers` that is not a vanilla dispenser (which would fire what it is given). */
	private const val NOT_VANILLA_DISPENSER =
		"if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=${Tag.CUSTOM_BLOCK},distance=..0.75,limit=1,sort=nearest]"

	/** Defines the six functions; returns `check_filter` and `transfer`, which `process` calls. */
	fun Ra.registerItemPipeTransfer(): Pair<FunctionArgument, FunctionArgument> {
		pack.define(checkFilter) { checkFilter() }
		pack.define(filterInsert) { insert(macro = true, loop = filterInsertLoop) }
		pack.define(filterInsertLoop) { insertLoop(source = "at @s", self = filterInsertLoop) }
		pack.define(moveStack) { moveStack() }
		pack.define(transfer) { insert(macro = false, loop = transferLoop) }
		pack.define(transferLoop) { insertLoop(source = "positioned ^ ^ ^-1", self = transferLoop) }
		return checkFilter to transfer
	}

	/**
	 * Check the pipe's filter against the item about to move. At the pipe. Input: storage ra:temp
	 * pipe_item. Returns 1 when the item was sent to a side container.
	 *
	 * THE FILTER IS A PROPERTY NOW, NOT AN ITEM FRAME. It used to be an item frame stuck to the
	 * pipe. Reading it meant selecting every item_frame and glow_item_frame within 1.6 blocks of
	 * every pipe, then comparing each one's `block_pos` against the pipe's own coordinates -- an
	 * entity selector and three score comparisons per candidate, per pipe. That was expensive
	 * enough that it had to be cached and rescanned only every 20 ticks, which in turn meant a
	 * frame you had just put up did nothing for up to a second.
	 *
	 * The filter is now `filter_item`, an item id on the pipe's own properties, set with the Data
	 * Handler's [Set from hand] button. Reading it is one `data modify` against the marker that is
	 * already @s. No entity selector, no cache, no cooldown, and no stale second after a change.
	 *
	 * A pipe built before this carries its frame's item in data.data.filter; the migration copies
	 * it across and the frame becomes decoration.
	 */
	private fun Function.checkFilter() {
		raw("execute unless data entity @s data.properties.filter_item run return 0")

		// Seeded empty at placement so the Handler has a row to show. Empty means the pipe filters
		// nothing and everything carries on forward.
		raw("execute if data entity @s data.properties{filter_item:\"\"} run return 0")

		raw("data modify storage ra:temp filter_item set from entity @s data.properties.filter_item")

		// Compare item IDs. `data modify ... set from` reports success only when it actually changed
		// something, so success 0 means the two ids were already equal.
		raw("data modify storage ra:temp check_id set from storage ra:temp pipe_item.id")
		raw("execute store success score @s ${Obj.TEMP} run data modify storage ra:temp check_id set from storage ra:temp filter_item")

		// Not ours to divert: a different item, or with `invert` on, the filtered item itself.
		// (Matched from the root: same as `data.properties{invert:1b}` in game, and the form the
		// minecode emulator can check.)
		raw("execute unless data entity @s {data:{properties:{invert:1b}}} if score @s ${Obj.TEMP} matches 1 run return 0")
		raw("execute if data entity @s {data:{properties:{invert:1b}}} if score @s ${Obj.TEMP} matches 0 run return 0")

		// Diverted (a match, or a non-match when inverted). Try the four sides around the pipe's
		// axis; `sx` is the way back to the pipe from that side. ALL RELATIVE: the marker of an upright pipe is pitched +-90, so `^ ^1 ^` is a
		// horizontal side there. The old mix of absolute `~ ~+-1 ~` and relative `^+-1 ^ ^` tested the
		// pipe's own axis twice on an upright pipe and never two of its sides. The back (`^ ^ ^-1`) is
		// not a side: it is the feeder, and diverting into it sent filtered stacks back where they
		// came from.
		for ((side, back) in listOf("^ ^-1 ^" to "^ ^1 ^", "^ ^1 ^" to "^ ^-1 ^", "^-1 ^ ^" to "^1 ^ ^", "^1 ^ ^" to "^-1 ^ ^")) {
			executeStoreRun("positioned $side if block ~ ~ ~ #ra_lib:containers", "@s", filterInsert, "{sx:\"$back\"}")
			raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")
		}
		returnValue(0)
	}

	/**
	 * Insert storage ra:temp pipe_item into the container here. Returns 1 on success.
	 * [macro]: `filter_insert {sx}`, the pipe at `sx`; otherwise `transfer`, the pipe at `^ ^ ^-1`.
	 */
	private fun Function.insert(macro: Boolean, loop: FunctionArgument) {
		raw("execute unless block ~ ~ ~ #ra_lib:containers run return 0")

		// Avoid triggering vanilla dispensers by only allowing dispenser targets that are RA custom blocks.
		raw("execute $NOT_VANILLA_DISPENSER run return 0")

		// Fast path: the whole stack in one move when the destination has a free slot.
		if (macro) raw("\$execute store result score #stack ${Obj.TEMP} run ${macroCall(moveStack, "{sx:\"\$(sx)\"}")}")
		else executeStoreRun("", "#stack", moveStack, "{sx:\"^ ^ ^-1\"}")
		raw("execute if score #stack ${Obj.TEMP} matches 1 run return 1")

		// Fallback: the destination has no free slot, so top up partial stacks one at a time. If it
		// cannot accept one item, the items stay in the pipe.
		raw("data modify storage ra:temp inv_item set from storage ra:temp pipe_item")
		raw("data modify storage ra:temp inv_item.count set value 1")
		raw("execute unless data storage ra:temp inv_item.components run data modify storage ra:temp inv_item.components set value {}")
		executeRun("", RaLib.Inventory.canAcceptOne, "with storage ra:temp inv_item")
		raw("execute if score #inv_can_insert ${Obj.TEMP} matches 0 run return 0")

		tag(self()) { remove(MOVED) }
		// Move the stack one by one until blocked or the source is empty.
		function(loop)

		// Nothing moved: the insert failed.
		raw("execute unless entity @s[tag=$MOVED] run return 0")
		tag(self()) { remove(MOVED) }

		// A destination that is another item pipe processes on its own tick.
		returnValue(1)
	}

	/**
	 * Move one item from the pipe (reached with [source]) into the container here, then recurse.
	 * Stops when the pipe's slot 0 is empty or the destination refuses.
	 */
	private fun Function.insertLoop(source: String, self: FunctionArgument) {
		raw("execute $source unless data block ~ ~ ~ Items[0] run return 0")

		// Refresh the current source item.
		raw("execute $source run data modify storage ra:temp pipe_item set from block ~ ~ ~ Items[0]")
		raw("execute unless data storage ra:temp pipe_item.components run data modify storage ra:temp pipe_item.components set value {}")

		// Exactly one item.
		raw("data modify storage ra:temp pipe_item.count set value 1")
		executeStoreRun("", "@s", RaLib.Inventory.insert, "with storage ra:temp pipe_item")
		raw("execute if score @s ${Obj.TEMP} matches 0 run return 0")

		// Take it out of the pipe's slot 0.
		executeStoreRun(source, "@s", RaLib.Inventory.consumeEntry0)
		raw("execute if score @s ${Obj.TEMP} matches 0 run return 0")

		tag(self()) { add(MOVED) }
		function(self)
	}

	/**
	 * Macro `{sx}`: move the whole of the pipe's first stack (the pipe at `sx`) into the container
	 * here, in one operation. Returns 1 on success.
	 *
	 * Cost of moving a full stack, before and after:
	 *   before  64 x (loot insert + data get + data modify)
	 *   after   1 free-slot lookup + 2 /item commands
	 *
	 * `loot insert` had to rebuild the item from an id, a count and a components blob every single
	 * time; /item replace copies the stack across as it stands.
	 */
	private fun Function.moveStack() {
		raw("execute unless block ~ ~ ~ #ra_lib:containers run return 0")

		// Never feed a vanilla dispenser; it would fire the contents.
		raw("execute $NOT_VANILLA_DISPENSER run return 0")

		raw("\$execute positioned \$(sx) unless data block ~ ~ ~ Items[0] run return 0")

		// Items[0] is the first stack present, not slot 0, so read the slot it occupies.
		raw("\$execute positioned \$(sx) store result score #inv_src_slot ${Obj.TEMP} run data get block ~ ~ ~ Items[0].Slot")

		executeStoreRun("", "#inv_dst_slot", RaLib.Inventory.findFreeSlot)
		raw("execute if score #inv_dst_slot ${Obj.TEMP} matches ..-1 run return 0")

		raw("\$data modify storage ra:inventory mv.src set value \"\$(sx)\"")
		raw("execute store result storage ra:inventory mv.src_slot int 1 run scoreboard players get #inv_src_slot ${Obj.TEMP}")
		raw("execute store result storage ra:inventory mv.dst_slot int 1 run scoreboard players get #inv_dst_slot ${Obj.TEMP}")
		callWith(RaLib.Inventory.moveSlot, "storage ra:inventory mv")
		raw("data remove storage ra:inventory mv")

		returnValue(1)
	}
}
