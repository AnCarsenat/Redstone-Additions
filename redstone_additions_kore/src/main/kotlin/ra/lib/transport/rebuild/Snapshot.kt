package ra.lib.transport.rebuild

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import net.benwoodworth.knbt.buildNbtList
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.net.Read
import ra.lib.transport.tr

/** Parking each old network's contents on its root node before every network is discarded. */
internal object Snapshot {
	val snapshot = lib("transport/rebuild/snapshot")
	val snapshotRead = lib("transport/rebuild/snapshot_read")
	val snapshotNext = lib("transport/rebuild/snapshot_next")
	val snapshotOne = lib("transport/rebuild/snapshot_one")

	fun Ra.registerSnapshot() {
		// Internal: park one network's contents on its root node.
		// Context: as the root marker of a network that is about to be discarded.
		pack.define(snapshot) {
			// The number goes on the node with the contents, so rebuild/seed can give it back to
			// whatever component this node ends up in. The counter is dragged up past it at the same
			// time: a fresh id must never collide with one that is about to be inherited, and after
			// this pass the counter is above every id in use.
			scoreboard.players.operation(self(), Obj.TR_OLD, Operation.SET, self(), Obj.TR_NET)
			execute {
				ifCondition { score(self(), Obj.TR_NET) greaterThan score(tr("#net_seq"), Obj.TR_TMP) }
				run { scoreboard.players.operation(tr("#net_seq"), Obj.TR_TMP, Operation.SET, self(), Obj.TR_NET) }
			}

			raw("execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(snapshotRead, TR, "arg")
		}

		pack.define(snapshotRead) { snapshotRead() }

		// Internal: park the head of the network's media list on the root node, then the rest.
		// Context: as the root marker, with storage ra:transport snapq holding the network id and
		// what is left of the list.
		pack.define(snapshotNext) {
			execute {
				unlessCondition { data(TR, "snapq.queue[0]") }
				run { returnValue(0) }
			}

			data(TR) { modify("snapq.m") { set(TR, "snapq.queue[0].m") } }
			data(TR) { remove("snapq.queue[0]") }

			function(snapshotOne, TR, "snapq")
			function(snapshotNext)
		}

		// `snapshot_one {id, m}`, internal: park one medium's amount on the root node.
		// Context: as that marker.
		//
		// A medium listed with nothing left in it is dropped rather than carried as a zero: it
		// would come back as an entry in the new network's media list, and the front of that list
		// is what names the whole run.
		//
		// THE ELEMENT IS BUILT WHOLE, IN STORAGE, THEN APPENDED
		// The obvious version appends {m:"water",a:0} and then fills the amount in with `execute
		// store result entity @s data.data.carry[-1].a`. That does not work. A write to entity data
		// is applied by building a compound from the path and merging it into the entity, and there
		// is nothing for a list INDEX to merge into -- so the amount silently stayed 0 on every
		// element. Every medium then came back through absorb_one, which drops anything holding
		// nothing, and a run full of water rebuilt itself as a network with no media and no name at
		// all: "Medium: Empty" over an Amount line that still had the total in it.
		//
		// Reading an index is fine and absorb_next does exactly that. It is only writing through one
		// that does not.
		pack.define(snapshotOne) {
			val a = tr("#snap.a")
			raw("\$execute store result score #snap.a ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).amounts.\$(m)")
			execute {
				ifCondition { score(a, Obj.TR_TMP) lessThanOrEqualTo 0 }
				run { returnValue(0) }
			}

			data(TR) { remove("snapi") }
			raw("\$data modify storage ra:transport snapi.m set value \"\$(m)\"")
			raw("execute store result storage ra:transport snapi.a int 1 run scoreboard players get #snap.a ${Obj.TR_TMP}")
			data(self()) { modify("data.data.carry") { append(TR, "snapi") } }

			scoreboard.players.operation(self(), Obj.TR_CARRY, Operation.ADD, a, Obj.TR_TMP)
		}
	}

	/**
	 * `snapshot_read {id}`, internal: copy the network's contents onto the root node, so they
	 * survive the rebuild that is about to discard every network.
	 *
	 * THE WHOLE BREAKDOWN TRAVELS, NOT JUST THE TOTAL
	 * This used to park `amount` and `medium` and nothing else. A rebuild therefore turned a run
	 * holding 5000 water and 5000 lava into one holding 10000 of whatever `medium` happened to
	 * name, with an EMPTY `amounts` map -- and since reset_net writes `media:[]`, the read-time
	 * migration never fired to repair it either. The next thing offered to that network became
	 * media[0] and so became the name on the goggles: pump 5000 water, place a block, pump 5000
	 * lava, and the run reports 10000 Lava.
	 *
	 * So the per-medium figures are carried too, as a list of {m,a} on the marker, and
	 * ra.tr.carry is summed from them rather than read off `amount`. Summing it is what keeps the
	 * total and the breakdown from disagreeing: whatever absorb puts back per medium is exactly
	 * what accumulate adds to the total.
	 */
	private fun io.github.ayfri.kore.functions.Function.snapshotRead() {
		// A network written before multi-medium has no breakdown to carry. Give it one first, with
		// the same code the read path uses, rather than a second dialect of the same migration here.
		raw(Read.migrateIfNeeded)

		scoreboard.players.set(self(), Obj.TR_CARRY, 0)
		data(self()) { remove("data.data.carry") }
		data(self()) { remove("data.data.carry_potion") }

		// Cleared before it is written, not merely overwritten when there is something to write. A
		// root whose network is empty would otherwise keep the name of whatever it held last, for
		// ever -- and rebuild/absorb hands that stale name to adopt_medium on the one path that
		// carries a total with no breakdown behind it, which names a network after a medium it has
		// never held.
		data(self()) { remove("data.data.medium") }

		raw("\$execute if data storage ra:transport nets.n\$(id).medium run data modify entity @s data.data.medium set from storage ra:transport nets.n\$(id).medium")

		// A Potion network's effect list belongs to the network, not to any node, so it went the
		// same way the breakdown did on every rebuild.
		raw("\$execute if data storage ra:transport nets.n\$(id).potion run data modify entity @s data.data.carry_potion set from storage ra:transport nets.n\$(id).potion")

		// NOTHING TRAVELS THAT CANNOT BE NAMED
		// This used to carry the bare `amount` on, on the grounds that losing it silently was worse
		// than losing its name. It is worse. A total with no breakdown behind it is indestructible:
		// the next rebuild carries it again, `amounts` stays empty, and the first medium offered
		// afterwards becomes the only entry in `media` and therefore the name of the whole total --
		// so 15000 mL of nothing reports itself as 15000 mL of the next thing you pour in. Three
		// networks in a test world ended up holding a five-figure amount with no medium at all and
		// no way back, because both migrations are gated on a `medium` that those networks did not
		// have.
		//
		// So the invariant is that `amount` is exactly the sum of `amounts`, and it is enforced by
		// the only thing that can enforce it: a rebuild carries the breakdown and nothing else, and
		// rebuild/accumulate sums the total back from what was carried. An amount with no breakdown
		// is not contents, it is a bookkeeping error, and it goes.
		raw("\$execute unless data storage ra:transport nets.n\$(id).media[0] run return 0")

		// Created here rather than above, so the branch that returns without walking does not
		// leave an empty list behind on the marker for ever.
		data(self()) { modify("data.data.carry") { set(buildNbtList<Nothing> { }) } }

		data(TR) { remove("snapq") }
		raw("\$data modify storage ra:transport snapq set value {id:\$(id)}")
		raw("\$data modify storage ra:transport snapq.queue set from storage ra:transport nets.n\$(id).media")
		function(snapshotNext)

		execute {
			unlessCondition { data(self(), "data.data.carry[0]") }
			run { data(self()) { remove("data.data.carry") } }
		}
	}
}
