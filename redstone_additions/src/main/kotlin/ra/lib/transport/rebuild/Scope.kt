package ra.lib.transport.rebuild

import io.github.ayfri.kore.commands.function
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.macroCall
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.TrTag

/**
 * Choosing which networks a rebuild may take apart, and holding back the ones it must not.
 *
 * THE BUG THIS EXISTS FOR
 * A rebuild used to wipe `nets` and recount every network from the node markers it could see.
 * `@e` only sees LOADED entities, so a network whose root sat in an unloaded chunk had its record
 * deleted with nobody to carry its contents across, and a network only partly loaded was recounted
 * from the loaded half and then clamped to that half's capacity. Placing one pipe anywhere emptied
 * every tank out of view.
 *
 * WHAT A REBUILD TOUCHES NOW
 * - **In scope**: the networks named in `dirtyq` (a node left them, or a reader found their record
 *   missing), the ones in `pending` (held back last time), and -- after a load, `full:1b` -- every
 *   network with a loaded node, listed on `fullq` and taken [FULL_BATCH_NODES] nodes per rebuild.
 *   Plus every unassigned node (net 0), which never needs naming.
 * - **Held**: a network in scope with a loaded node next to a chunk that is not loaded, or itself
 *   in one, may go on into the dark. It is left exactly as it is: its record, its ids, its root.
 *   `execute if loaded` is true only once a chunk's entities are loaded too, so a node `@e` cannot
 *   see yet always holds its network back. Only the four horizontal sides are tested -- a node's
 *   upper and lower neighbours are in its own chunk column -- plus the node's own position.
 *   A network with no record is never held: there is nothing to protect, and rebuilding it is what
 *   repairs it.
 * - **Everything else** -- every network not in scope -- is not looked at. It keeps its record and
 *   its nodes keep their number. A new node next to it joins it (rebuild/adopt) without the
 *   network being recounted, which is why placing a pipe no longer costs a pass over every pipe in
 *   the world, and why extending a network that runs out of view is safe.
 *
 * A held network that was in scope for a reason (a leave may have split it in two) goes back on
 * `pending`, and transport/tick retries it every few seconds until it is
 * whole in view. Until then its parts share one pool, which loses nothing.
 */
internal object Scope {
	val scope = lib("transport/rebuild/scope")
	val scopeNext = lib("transport/rebuild/scope_next")
	val scopeId = lib("transport/rebuild/scope_id")
	val edge = lib("transport/rebuild/edge")
	val hold = lib("transport/rebuild/hold")
	val holdId = lib("transport/rebuild/hold_id")
	val pend = lib("transport/rebuild/pend")
	val fullCollect = lib("transport/rebuild/full_collect")
	val fullList = lib("transport/rebuild/full_list")
	val fullListId = lib("transport/rebuild/full_list_id")
	val fullBatch = lib("transport/rebuild/full_batch")
	val fullPop = lib("transport/rebuild/full_pop")
	val fullOne = lib("transport/rebuild/full_one")

	/**
	 * Loaded nodes put in scope per rebuild while working through `fullq` after a load. A scoped
	 * rebuild costs ~35-40 commands a node, so this keeps a batch near 20000 of the 65536 a tick
	 * may run, with room for whatever else that rebuild and that tick are doing.
	 */
	const val FULL_BATCH_NODES = 512

	private val horizontal = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1")

	fun Ra.registerScope() {
		// Internal: tag ra.tr.scope on every node of every network this rebuild may take apart.
		pack.define(scope) {
			// What was asked for: left networks, read repairs, and last rebuild's held-back ones.
			// Both lists are emptied before anything is held: holding writes `pending` afresh.
			for (list in listOf("dirtyq", "pending")) {
				raw("data modify storage ra:transport rb.q set from storage ra:transport $list")
				function(scopeNext)
			}
			for (list in listOf("dirtyq", "dirtyset", "pending")) raw("data remove storage ra:transport $list")

			// After a load nobody knows what changed while the pack was off: everything loaded is
			// rebuilt, but a batch at a time (rebuild/full_batch). Not on the second pass of a
			// rebuild that goes round again, which would double the batch in one tick.
			raw("execute if data storage ra:transport {full:1b} run function ${fullCollect.asId()}")
			raw("execute unless score #rb_again ${Obj.TR_TMP} matches 1 if data storage ra:transport fullq[0] run function ${fullBatch.asId()}")

			// Hold back whatever may reach past the loaded area.
			raw("execute as @e[type=marker,tag=${TrTag.SCOPE}] at @s run function ${edge.asId()}")
		}

		// Internal: walk rb.q.
		pack.define(scopeNext) {
			raw("execute unless data storage ra:transport rb.q[0] run return 0")
			raw("data modify storage ra:transport rb.cur.id set from storage ra:transport rb.q[0]")
			raw("data remove storage ra:transport rb.q[0]")
			function(scopeId, TR, "rb.cur")
			function(scopeNext)
		}

		// `scope_id {id}`, internal: put one named network in scope, once.
		//
		// `rb.seen` doubles as "named on purpose", which is what decides whether a held network
		// goes back on `pending` (rebuild/hold_id).
		//
		// A `rootless` record lost its root while the rest of it was out of view (net/handover).
		// Its first loaded node becomes the root here, so the contents are parked on somebody when
		// the network is taken apart. That is safe because rootless means no root exists anywhere.
		pack.define(scopeId) {
			raw("\$execute if data storage ra:transport rb.seen.n\$(id) run return 0")
			raw("\$data modify storage ra:transport rb.seen.n\$(id) set value 1b")
			raw("\$tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)}] add ${TrTag.SCOPE}")
			raw("\$execute if data storage ra:transport nets.n\$(id).rootless run tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)},limit=1] add ${TrTag.ROOT}")
			raw("\$execute if entity @e[type=marker,tag=${TrTag.ROOT},scores={${Obj.TR_NET}=\$(id)},limit=1] run data remove storage ra:transport nets.n\$(id).rootless")
		}

		// Internal: as a node in scope, at it. Hold its network back if it touches the dark.
		pack.define(edge) {
			// Its network was already held by an earlier node.
			raw("execute unless entity @s[tag=${TrTag.SCOPE}] run return 0")
			raw("execute unless loaded ~ ~ ~ run return run function ${hold.asId()}")
			for (pos in horizontal) raw("execute unless loaded $pos run return run function ${hold.asId()}")
		}

		// Internal: as the node that found the edge.
		pack.define(hold) {
			raw("execute store result storage ra:transport rb.h.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(holdId, TR, "rb.h")
		}

		// `hold_id {id}`, internal: take network <id> back out of scope.
		//
		// Its id stays reserved: #net_seq is dragged past it, so a fresh number can never land on a
		// network that still has nodes out of view (rebuild/next_id also skips any id with a record).
		pack.define(holdId) {
			raw("\$execute unless data storage ra:transport nets.n\$(id) run return 0")
			raw("\$tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)}] remove ${TrTag.SCOPE}")
			raw("scoreboard players operation #net_seq ${Obj.TR_TMP} > @s ${Obj.TR_NET}")
			raw("\$execute if data storage ra:transport rb.seen.n\$(id) run ${macroCall(pend, "{id:\$(id)}")}")
		}

		// Internal, on the first rebuild after a load: list every loaded network once on `fullq`.
		//
		// The load used to put every loaded node in scope for that one rebuild. At 35-40 commands a
		// node, a world with ~1700 loaded nodes ran past the 65536-command limit, and the cut left
		// nodes on network 0 with rebuild tags still on them and half the networks rebuilt. The list
		// costs one command a node plus a few a network; full_batch then works through it.
		pack.define(fullCollect) {
			raw("data remove storage ra:transport full")
			raw("data modify storage ra:transport fullq set value []")
			raw("tag @e[type=marker,tag=${TrTag.LISTED}] remove ${TrTag.LISTED}")
			raw("execute as @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=1..}] run function ${fullList.asId()}")
			raw("tag @e[type=marker,tag=${TrTag.LISTED}] remove ${TrTag.LISTED}")
		}

		// Internal: as a node, list its network unless one of its nodes already did.
		pack.define(fullList) {
			raw("execute if entity @s[tag=${TrTag.LISTED}] run return 0")
			raw("execute store result storage ra:transport rb.fl.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(fullListId, TR, "rb.fl")
		}

		// `full_list_id {id}`, internal.
		pack.define(fullListId) {
			raw("\$tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)}] add ${TrTag.LISTED}")
			raw("\$data modify storage ra:transport fullq append value \$(id)")
		}

		// Internal: put the next networks on `fullq` in scope, up to [FULL_BATCH_NODES] loaded
		// nodes. Always at least one network, however big: a network is rebuilt whole or not at all.
		// What is left waits for the next rebuild, which `dirty` asks for; until then those networks
		// keep working from their records, exactly as they were before the load.
		//
		// Like the old `full`, this does not mark the networks as named (rb.seen), so one that
		// reaches out of view is held without going on `pending`: nothing asked for it to change.
		pack.define(fullBatch) {
			raw("scoreboard players set #full_budget ${Obj.TR_TMP} $FULL_BATCH_NODES")
			function(fullPop)
			raw("execute if data storage ra:transport fullq[0] run data modify storage ra:transport dirty set value 1b")
			raw("execute unless data storage ra:transport fullq[0] run data remove storage ra:transport fullq")
		}

		// Internal: walk `fullq` until the budget is spent.
		pack.define(fullPop) {
			raw("execute unless data storage ra:transport fullq[0] run return 0")
			raw("execute if score #full_budget ${Obj.TR_TMP} matches ..0 run return 0")
			raw("data modify storage ra:transport rb.fb.id set from storage ra:transport fullq[0]")
			raw("data remove storage ra:transport fullq[0]")
			function(fullOne, TR, "rb.fb")
			function(fullPop)
		}

		// `full_one {id}`, internal: network <id> in scope, its loaded nodes off the budget. A
		// network that merged away or unloaded since the list was made has no nodes and costs nothing.
		pack.define(fullOne) {
			raw("\$execute store result score #full_n ${Obj.TR_TMP} if entity @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)}]")
			raw("scoreboard players operation #full_budget ${Obj.TR_TMP} -= #full_n ${Obj.TR_TMP}")
			raw("\$tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=\$(id)}] add ${TrTag.SCOPE}")
		}

		// `pend {id}`, internal: retry network <id> at a later rebuild, once per rebuild.
		pack.define(pend) {
			raw("\$execute if data storage ra:transport rb.pend.n\$(id) run return 0")
			raw("\$data modify storage ra:transport rb.pend.n\$(id) set value 1b")
			raw("\$data modify storage ra:transport pending append value \$(id)")
		}
	}
}
