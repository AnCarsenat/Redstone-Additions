package ra.modules.automation.splitter

import ra.api.RaLib
import ra.core.Admin.readCappedProperty
import ra.core.Obj
import ra.core.Ra
import ra.core.block.readProperty
import ra.core.define
import ra.core.raw

/**
 * One look of an [ItemSplitter], once a second from the automation clock. As the marker, at the
 * block.
 *
 * IDLE. An empty input ends the look at its second command; nothing of the splitter runs per tick
 * but the break check.
 *
 * ONE ITEM AT A TIME, up to `per_second` a look. The item is the input's first stack (in the
 * container's order), one of it:
 *  1. [pick]: from the face whose turn it is, the first face (at most a full round of five) with
 *     a share above 0, a share not used up this turn, a container (`#ra_lib:containers`) and room
 *     for one of that item (`ra_lib:inventory/can_accept_one`: an empty slot, or a matching stack
 *     below its stack size). A face passed over loses the rest of its turn.
 *  2. The item goes in with `ra_lib:inventory/insert_or_drop`, which cannot drop it: the room was
 *     just checked. Only then is one taken off the input (`consume_entry0`), in the same function.
 *  3. When the face has had its share, the turn moves on.
 * No face with room for the first stack ends the look ("outputs full"); the stack waits.
 */
internal object ItemSplitterRun {
	private const val T = Obj.TEMP
	private const val FACES = 5

	private fun fn(path: String) = ItemSplitter.block.fn(path)

	val run = fn("run")
	private val move = fn("move")
	private val pick = fn("pick")

	private fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""

	/** The next face's turn: `#sp.f` round the five, `#sp.sent` back to 0. */
	private fun io.github.ayfri.kore.functions.Function.advance(cond: String = "") {
		val pre = if (cond.isEmpty()) "" else "execute $cond run "
		raw("${pre}scoreboard players add #sp.f $T 1")
		raw("execute if score #sp.f $T matches $FACES.. run scoreboard players set #sp.f $T 0")
		raw("${pre}scoreboard players set #sp.sent $T 0")
	}

	fun Ra.registerItemSplitterRun() {
		pack.define(run) {
			raw("execute unless block ~ ~1 ~ #ra_lib:containers run return run ${state("no container above")}")
			raw("execute unless items block ~ ~1 ~ container.* * run return run ${state("waiting for items")}")
			raw("execute if function ${RaLib.Redstone.any.asId()} run return run ${state("stopped by redstone")}")

			raw("scoreboard players set #sp.sum $T 0")
			ItemSplitter.faces.forEachIndexed { i, (face, _) ->
				readProperty(face, default = 1, min = 0)
				raw("scoreboard players operation #sp.w$i $T = #prop $T")
				raw("scoreboard players operation #sp.sum $T += #prop $T")
			}
			raw("execute if score #sp.sum $T matches ..0 run return run ${state("every side's share is 0")}")
			readCappedProperty("per_second", default = ItemSplitter.DEFAULT_RATE, min = 1, adminMax = ItemSplitter.ADMIN_MAX_RATE)
			raw("scoreboard players operation #sp.rate $T = #prop $T")

			raw("scoreboard players set #sp.f $T 0")
			raw("scoreboard players set #sp.sent $T 0")
			raw("execute store result score #sp.f $T run data get entity @s data.data.face")
			raw("execute store result score #sp.sent $T run data get entity @s data.data.sent")
			raw("execute unless score #sp.f $T matches 0..${FACES - 1} run scoreboard players set #sp.f $T 0")

			raw("scoreboard players set #sp.moved $T 0")
			raw("function ${move.asId()}")
			raw("data remove storage ra:temp inv_item")

			raw("execute store result entity @s data.data.face int 1 run scoreboard players get #sp.f $T")
			raw("execute store result entity @s data.data.sent int 1 run scoreboard players get #sp.sent $T")
			raw("execute if score #sp.moved $T matches ..0 run return run ${state("outputs full")}")
			raw(state("splitting"))
			raw("execute store result score #sp.n $T run data get entity @s data.status.split")
			raw("scoreboard players operation #sp.n $T += #sp.moved $T")
			raw("execute store result entity @s data.status.split int 1 run scoreboard players get #sp.n $T")
		}

		// One item of the input's first stack to the face whose turn it is, then the next item.
		pack.define(move) {
			raw("execute if score #sp.moved $T >= #sp.rate $T run return 0")
			raw("execute unless data block ~ ~1 ~ Items[0] run return 0")
			raw("data modify storage ra:temp inv_item set from block ~ ~1 ~ Items[0]")
			raw("data remove storage ra:temp inv_item.Slot")
			raw("data modify storage ra:temp inv_item.count set value 1")
			raw("scoreboard players set #sp.tries $T 0")
			raw("scoreboard players set #sp.ok $T 0")
			raw("function ${pick.asId()}")
			raw("execute if score #sp.ok $T matches 0 run return 0")

			raw("data modify storage ra:inventory item set from storage ra:temp inv_item")
			ItemSplitter.faces.forEachIndexed { i, (_, at) ->
				raw("execute if score #sp.f $T matches $i positioned $at run function ${RaLib.Inventory.insertOrDrop.asId()}")
			}
			raw("data remove storage ra:inventory item")
			raw("execute positioned ~ ~1 ~ run function ${RaLib.Inventory.consumeEntry0.asId()}")

			raw("scoreboard players add #sp.moved $T 1")
			raw("scoreboard players add #sp.sent $T 1")
			advance("if score #sp.sent $T >= #sp.w $T")
			raw("function ${move.asId()}")
		}

		// The face whose turn it is, if it takes one storage ra:temp inv_item (#sp.ok 1, #sp.w its
		// share); otherwise the next face, a full round at most.
		pack.define(pick) {
			raw("execute if score #sp.tries $T matches $FACES.. run return 0")
			raw("scoreboard players add #sp.tries $T 1")
			ItemSplitter.faces.indices.forEach { i ->
				raw("execute if score #sp.f $T matches $i run scoreboard players operation #sp.w $T = #sp.w$i $T")
			}
			raw("scoreboard players set #inv_can_insert $T 0")
			ItemSplitter.faces.forEachIndexed { i, (_, at) ->
				raw("execute if score #sp.f $T matches $i if score #sp.w $T matches 1.. if score #sp.sent $T < #sp.w $T positioned $at if block ~ ~ ~ #ra_lib:containers run function ${RaLib.Inventory.canAcceptOne.asId()}")
			}
			raw("execute if score #inv_can_insert $T matches 1.. run return run scoreboard players set #sp.ok $T 1")
			advance()
			raw("function ${pick.asId()}")
		}
	}
}
