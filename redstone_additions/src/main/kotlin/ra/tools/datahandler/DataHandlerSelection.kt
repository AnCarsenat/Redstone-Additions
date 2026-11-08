package ra.tools.datahandler

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.callWith
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * WHICH BLOCK, AND WHICH QUESTION, IS PER PLAYER.
 *
 * The Data Handler used to keep one selection for the whole server -- selecting a block untagged
 * every other -- and one pending edit, `pending_name` / `pending_kind` in storage ra:dh. With two
 * players at work, A's typed value was written onto the block B had just selected, under the
 * property B had just picked, past the read-only check that had been made against A's block.
 *
 * Now:
 *   - a selected marker carries a number, ra.dh.mid, handed out the first time anyone selects
 *     it, and keeps the ra.dh_target tag so the lookup scans only markers that were ever selected;
 *   - a player's selection is that number in ra.dh.sel;
 *   - `select` tags the executing player's block ra.dh_selected, and [TARGET] is that tag. Every
 *     entry point that acts for a player (the use advancement, the trigger dispatcher, the pending
 *     input, the chat commands) calls it first, so whatever ran before -- another player's
 *     click -- cannot leave its block selected;
 *   - a pending edit is `pend.r<id>` = {name, kind, mid}, keyed by the input request the Handler
 *     opened for it (ra.dh.req), and carries the block it was asked about.
 *
 * The Creative Data Handler uses the same selection: one "selected block" per player, whichever
 * of the two tools picked it. Its chat buttons that suggest a `/data` command name the block by
 * its number ([byMid]), so a command typed later still lands on that block and no other.
 */
object DataHandlerSelection {
	/** On a marker: its Data Handler number. `#next ra.dh.mid` is the allocator. */
	const val MID = "ra.dh.mid"

	/** On a player: the ra.dh.mid of the block they selected; 0 for none. */
	const val SEL = "ra.dh.sel"

	/** On a player: the ra_lib:input request the Data Handler opened for them. */
	const val REQ = "ra.dh.req"

	/** On every marker ever selected; kept, so the lookup has a short list to scan. */
	const val TARGET_TAG = "ra.dh_target"

	/** On the marker the executing player selected, for the length of one call. */
	const val SELECTED_TAG = "ra.dh_selected"

	/** The executing player's block. Valid after [select]. */
	const val TARGET = "@e[type=marker,tag=$SELECTED_TAG,limit=1]"

	val select = tool("data_handler/select")

	/** A selector for the marker numbered [mid] (a literal or a macro `$(mid)`). */
	fun byMid(mid: String) = "@e[type=marker,tag=$TARGET_TAG,scores={$MID=$mid},limit=1]"

	/**
	 * As a block's marker that a player just found: tag it, give it a number if it has none, and
	 * leave that number in `#dh.found ra.temp` for the player's [SEL].
	 */
	fun Function.claim() {
		raw("tag @s add $TARGET_TAG")
		raw("execute unless score @s $MID matches 1.. run scoreboard players add #next $MID 1")
		raw("execute unless score @s $MID matches 1.. run scoreboard players operation @s $MID = #next $MID")
		raw("scoreboard players operation #dh.found ${Obj.TEMP} = @s $MID")
	}

	val pendingSave = tool("data_handler/pending/save")
	private val pendingSaveAt = tool("data_handler/pending/save_at")
	val pendingLoad = tool("data_handler/pending/load")
	private val pendingLoadAt = tool("data_handler/pending/load_at")
	val pendingForget = tool("data_handler/pending/forget")
	private val pendingForgetAt = tool("data_handler/pending/forget_at")
	val pendingDrop = tool("data_handler/pending/drop")

	fun Ra.registerDataHandlerSelection() {
		// As the player: tag their block, and only theirs, ra.dh_selected.
		pack.define(select) {
			raw("tag @e[type=marker,tag=$SELECTED_TAG] remove $SELECTED_TAG")
			raw("scoreboard players set #dh.sel ${Obj.TEMP} 0")
			raw("execute if score @s $SEL matches 1.. run scoreboard players operation #dh.sel ${Obj.TEMP} = @s $SEL")
			raw("execute if score #dh.sel ${Obj.TEMP} matches 1.. as @e[type=marker,tag=$TARGET_TAG] if score @s $MID = #dh.sel ${Obj.TEMP} run tag @s add $SELECTED_TAG")
		}

		// The question just asked, from the transient pending_name / pending_kind, under this
		// player's request. As the player, right after the request opened.
		pack.define(pendingSave) {
			withReq(pendingSaveAt)
			data(Storage.DH) { remove("pending_name") }
			data(Storage.DH) { remove("pending_kind") }
			data(Storage.DH) { remove("pending_j") }
		}
		// {r}
		pack.define(pendingSaveAt) {
			raw("\$data modify storage ra:dh pend.r\$(r) set value {}")
			raw("\$data modify storage ra:dh pend.r\$(r).name set from storage ra:dh pending_name")
			raw("\$data modify storage ra:dh pend.r\$(r).kind set from storage ra:dh pending_kind")
			// A list entry's index (DataHandlerEntries); -1 for an append.
			raw("\$execute if data storage ra:dh pending_j run data modify storage ra:dh pend.r\$(r).j set from storage ra:dh pending_j")
			raw("\$execute store result storage ra:dh pend.r\$(r).mid int 1 run scoreboard players get @s $SEL")
		}

		// This player's pending edit into storage ra:dh cur (absent when there is none).
		pack.define(pendingLoad) {
			data(Storage.DH) { remove("cur") }
			withReq(pendingLoadAt)
		}
		// {r}
		pack.define(pendingLoadAt) { raw("\$data modify storage ra:dh cur set from storage ra:dh pend.r\$(r)") }

		pack.define(pendingForget) { withReq(pendingForgetAt) }
		// {r}
		pack.define(pendingForgetAt) { raw("\$data remove storage ra:dh pend.r\$(r)") }

		// Stop waiting: no pending flag, no stored edit, no working copy.
		pack.define(pendingDrop) {
			scoreboard.players.set(self(), "ra.dh.pending", 0)
			function(pendingForget)
			data(Storage.DH) { remove("cur") }
		}
	}

	/** `pq = {r: <ra.dh.req>}` (0 when unset), then `function <fn> with storage ra:dh pq`. */
	private fun Function.withReq(fn: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
		raw("data modify storage ra:dh pq set value {r:0}")
		raw("execute if score @s $REQ matches -2147483648.. store result storage ra:dh pq.r int 1 run scoreboard players get @s $REQ")
		callWith(fn, "storage ra:dh pq")
		data(Storage.DH) { remove("pq") }
	}
}
