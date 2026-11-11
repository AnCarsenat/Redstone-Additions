package ra.lib.transport.net

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.defineProvider
import ra.core.provider
import ra.core.scoreOf
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.tr
import ra.core.macroCall

/** `net/read` and the read-time migration of pre-multi-medium networks. */
object Read {
	val readRun = lib("transport/net/read_run")
	val migratePick = lib("transport/net/migrate_pick")
	val migrateRun = lib("transport/net/migrate_run")
	val requestRebuild = lib("transport/net/request_rebuild")

	/**
	 * `execute if data ...nets.n$(id).medium unless data ...media[0] run function migrate_pick {id}`,
	 * as a macro line. Shared with rebuild/snapshot_read: the read path and the rebuild path must
	 * make the SAME test, and the two disagreeing is what once let a broken state persist.
	 */
	val migrateIfNeeded = "\$execute if data storage ra:transport nets.n\$(id).medium unless data storage ra:transport nets.n\$(id).media[0] run " +
		macroCall(migratePick, "{id:\$(id)}")

	fun Ra.registerRead() {
		// The room on the network `read` loaded: capacity - amount. A named provider because
		// every machine that puts something on a network asks it (pumps, drains, boilers, bridges,
		// creative sources). See RaLib.Transport.Net.free.
		pack.defineProvider(RaLib.Transport.Net.free, provider { sub(scoreOf("#net_capacity", Obj.TR_TMP), scoreOf("#net_amount", Obj.TR_TMP)) })

		// `read`: load this node's network state into scratch. Context: as a node marker.
		// Output: #net_amount / #net_capacity ra.tr.tmp -- the TOTAL across every medium -- and
		// storage ra:transport cur:
		//   cur.medium   the primary medium (absent when the network is empty)
		//   cur.amounts  {water:5000,lava:2000}
		//   cur.media    [{m:"water"},{m:"lava"}]
		pack.define(RaLib.Transport.Net.read) {
			scoreboard.players.set(tr("#net_amount"), Obj.TR_TMP, 0)
			scoreboard.players.set(tr("#net_capacity"), Obj.TR_TMP, 0)
			data(TR) { remove("cur") }

			execute {
				ifCondition { score(self(), Obj.TR_NET) lessThanOrEqualTo 0 }
				run { returnValue(0) }
			}

			raw("execute store result storage ra:transport rq.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(readRun, TR, "rq")
		}

		// `read_run {id}`: the dynamic-name half of net/read.
		//
		// The totals live in storage rather than on a `net<id>` fake player. Two reasons. A
		// scoreboard is a flat namespace of one number per network, which is exactly the shape that
		// had to change once a network can hold several media at once -- `nets.n<id>` is a compound
		// and gained its `amounts` map without moving anything. And in millilitres the numbers are a
		// thousand times what they were, so the headroom matters.
		//
		// Arithmetic reads the values where they live: a `data modify ... set compute` adds a score
		// to a storage path in one command (ra/core/Compute.kt), so nothing has to be copied into a
		// scoreboard and back just to change it.
		//
		// WHAT A NETWORK HOLDS
		//   amount    the total across every medium. This is what capacity is checked against, so a
		//             network clogs on the sum and not on any one medium.
		//   amounts   {water:5000,lava:2000} -- the per-medium breakdown.
		//   media     [{m:"water"},{m:"lava"}] -- the same keys as a list, because a compound's keys
		//             cannot be enumerated in a function and a list's elements can. media[0] is the
		//             primary: what a bridge moves and what a drain places when nobody named a medium.
		//   medium    a copy of media[0].m, kept because every display and every bridge already reads it.
		pack.define(readRun) {
			raw("\$execute store result score #net_capacity ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).capacity")

			// A node pointing at a network with no record -- left behind by the rebuild that used to
			// wipe every network, including the ones out of loaded range -- or at a record whose root
			// went with no loaded survivor, asks for its network to be rebuilt. Asking is all this
			// does; the rebuild is what writes.
			raw("\$execute unless data storage ra:transport nets.n\$(id) run ${macroCall(requestRebuild, "{id:\$(id)}")}")
			raw("\$execute if data storage ra:transport nets.n\$(id).rootless run ${macroCall(requestRebuild, "{id:\$(id)}")}")

			// A network written before multi-medium has `medium` and `amount` and no breakdown.
			// Backfill it the first time it is read rather than sweeping every network on load: the
			// ids cannot be enumerated, this costs one test per read once migrated, and a network
			// nobody touches does not need migrating.
			//
			// The test is `media[0]`, not `media`. rebuild/reset_net writes `media:[]`, and an empty
			// list IS present as far as `if data` is concerned -- so a network left holding an
			// amount with an empty breakdown never migrated on read, and the next medium offered to
			// it became the only entry in the list and so became the name on the whole total. That
			// is the same test rebuild/snapshot_read makes, and the two disagreeing is what let the
			// state persist.
			raw(migrateIfNeeded)

			// NOTHING HERE WRITES BACK
			// A network holding an amount with no breakdown at all cannot be migrated -- the
			// migration above is gated on `medium` because there is no other name to give what it
			// holds. It was briefly repaired here, by zeroing the total. That was wrong twice over:
			// read runs many times a tick, from every block's tick and every goggles refresh, so any
			// moment when `media` was transiently empty while a total stood destroyed the network's
			// contents outright and left the goggles reading "Empty"; and it was not needed, because
			// rebuild/snapshot_read already refuses to carry a nameless total across a rebuild and
			// transport/init marks every world dirty on load. The phantom therefore dies once,
			// during a rebuild, with nothing else running. This function only reads.
			raw("\$execute store result score #net_amount ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).amount")

			for (key in listOf("medium", "amounts", "media")) {
				raw("\$execute if data storage ra:transport nets.n\$(id).$key run data modify storage ra:transport cur.$key set from storage ra:transport nets.n\$(id).$key")
			}
		}

		// `request_rebuild {id}`, internal: queue network <id> for the next rebuild, once.
		pack.define(requestRebuild) {
			raw("\$execute if data storage ra:transport dirtyset.n\$(id) run return 0")
			raw("\$data modify storage ra:transport dirtyset.n\$(id) set value 1b")
			raw("\$data modify storage ra:transport dirtyq append value \$(id)")
			function(RaLib.Transport.markDirty)
		}

		// `migrate_pick {id}`, internal: read the old single-medium shape off a network, so
		// migrate_run can be handed both the id and the medium as macro arguments.
		//
		// Two steps because a macro line is substituted once: the medium is itself read out of a
		// path that needs the id substituted into it, so it cannot also be a macro argument of the
		// same line.
		pack.define(migratePick) {
			raw("\$data modify storage ra:transport mig set value {id:\$(id)}")
			raw("\$data modify storage ra:transport mig.medium set from storage ra:transport nets.n\$(id).medium")
			function(migrateRun, TR, "mig")
		}

		// `migrate_run {id, medium}`, internal: give a pre-multi-medium network the breakdown it
		// never had. Everything it holds is the one medium it was recorded as holding, so the whole
		// amount moves into that key and the media list gets its single entry.
		pack.define(migrateRun) {
			raw("\$data modify storage ra:transport nets.n\$(id).media set value [{m:\"\$(medium)\"}]")
			raw("\$data modify storage ra:transport nets.n\$(id).amounts set value {}")
			raw("\$execute store result score #mig.amt ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).amount")
			raw("\$execute if score #mig.amt ${Obj.TR_TMP} matches 1.. store result storage ra:transport nets.n\$(id).amounts.\$(medium) int 1 run scoreboard players get #mig.amt ${Obj.TR_TMP}")
		}
	}
}
