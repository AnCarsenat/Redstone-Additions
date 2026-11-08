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
 *   missing), the ones in `pending` (held back last time), and -- on load only, `full:1b` -- every
 *   network with a loaded node. Plus every unassigned node (net 0), which never needs naming.
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

			// After a load nobody knows what changed while the pack was off: everything loaded.
			raw("execute if data storage ra:transport {full:1b} run tag @e[type=marker,tag=${TrTag.NODE},scores={${Obj.TR_NET}=1..}] add ${TrTag.SCOPE}")
			raw("data remove storage ra:transport full")

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

		// `pend {id}`, internal: retry network <id> at a later rebuild, once per rebuild.
		pack.define(pend) {
			raw("\$execute if data storage ra:transport rb.pend.n\$(id) run return 0")
			raw("\$data modify storage ra:transport rb.pend.n\$(id) set value 1b")
			raw("\$data modify storage ra:transport pending append value \$(id)")
		}
	}
}
