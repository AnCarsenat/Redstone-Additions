package ra.lib.transport.net

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.sub
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.computeScore
import ra.core.computeStorage
import ra.core.define
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.core.storageOf
import ra.core.storeInt
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.TrTag
import ra.lib.transport.net.Flow.registerFlow
import ra.lib.transport.net.Read.registerRead
import ra.lib.transport.tr
import ra.lib.transport.trMarkers

/** `ra_lib:transport/net/...`: node membership (join, rejoin, leave) and the root handover. */
object Net {
	val handover = lib("transport/net/handover")
	val handoverPick = lib("transport/net/handover_pick")
	val leaveRecord = lib("transport/net/leave_record")
	val leaveRecordRun = lib("transport/net/leave_record_run")
	val markRootless = lib("transport/net/mark_rootless")

	fun Ra.registerNet() {
		// `join {class, capacity}`: enrol this marker as a transport node and request a network
		// rebuild. Context: as the node marker. Call once, at placement.
		//
		// `class` is one of "fluid", "item", "electric". Nodes only ever join a network of their
		// own class.
		pack.define(RaLib.Transport.Net.join) {
			tag(self()) { add(TrTag.NODE) }
			raw("\$scoreboard players set @s ${Obj.TR_CAP} \$(capacity)")
			scoreboard.players.set(self(), Obj.TR_NET, 0)
			scoreboard.players.set(self(), Obj.TR_CARRY, 0)

			scoreboard.players.set(self(), Obj.TR_CLASS, 0)
			readClass()

			function(RaLib.Transport.markDirty)
		}

		// `rejoin {class}`: put a node back into a network it previously left, keeping the capacity
		// it was registered with. Context: as the node marker.
		//
		// Unlike net/join this does not touch ra.tr.cap, so a caller toggling a node in and out
		// does not have to remember and restate the node's capacity -- getting that wrong silently
		// changes how much the whole network can hold.
		pack.define(RaLib.Transport.Net.rejoin) {
			tag(self()) { add(TrTag.NODE) }
			scoreboard.players.set(self(), Obj.TR_NET, 0)
			scoreboard.players.set(self(), Obj.TR_CARRY, 0)

			readClass()

			function(RaLib.Transport.markDirty)
		}

		// `leave`: remove this marker from its network and request a rebuild.
		// Context: as the node marker. Call on break, before the marker is killed.
		//
		// The network's contents are parked on its root node between rebuilds, so if the node being
		// removed IS the root, the root has to be handed to a survivor first or the whole network's
		// contents would disappear with it.
		pack.define(RaLib.Transport.Net.leave) {
			// Before anything below forgets which network this was.
			execute {
				ifCondition { score(self(), Obj.TR_NET) greaterThanOrEqualTo 1 }
				run { function(leaveRecord) }
			}

			scoreboard.players.set(tr("#handover"), Obj.TR_TMP, 0)
			raw("execute if entity @s[tag=${TrTag.ROOT}] run scoreboard players operation #leave_net ${Obj.TR_TMP} = @s ${Obj.TR_NET}")
			raw("execute if entity @s[tag=${TrTag.ROOT}] run scoreboard players set #handover ${Obj.TR_TMP} 1")

			tag(self()) { remove(TrTag.NODE) }
			tag(self()) { remove(TrTag.ROOT) }
			scoreboard.players.set(self(), Obj.TR_NET, 0)
			scoreboard.players.set(self(), Obj.TR_CLASS, 0)

			execute {
				ifCondition { score(tr("#handover"), Obj.TR_TMP) equalTo 1 }
				run { function(handover) }
			}

			function(RaLib.Transport.markDirty)
		}

		// Internal: tell the network it lost this node. Context: as the leaving node, still holding
		// its ra.tr.net.
		//
		// THE REBUILD ONLY TOUCHES WHAT IT IS TOLD ABOUT
		// It used to discard and recount every network on every rebuild, which deleted the contents
		// of any network whose root sat in an unloaded chunk (see rebuild/run). Now a rebuild works
		// on the networks named in `dirtyq` and leaves every other one exactly as it is. So a leave
		// names its network there, and also takes this node's capacity and count off the record
		// straight away: if the network turns out to reach into unloaded chunks the rebuild will
		// not recount it, and the record has to be right without that.
		pack.define(leaveRecord) {
			raw("execute store result storage ra:transport lq.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			scoreboard.players.operation(tr("#leave_cap"), Obj.TR_TMP, Operation.SET, self(), Obj.TR_CAP)
			function(leaveRecordRun, TR, "lq")
		}

		// `leave_record_run {id}`, internal: the dynamic-name half of leave_record.
		//
		// `nodes` is only kept on records written by a rebuild of this version; an older record has
		// none and is left uncounted rather than counted wrong. A record whose count reaches 0 has
		// lost its last node and is deleted: nothing else would, now that a rebuild no longer wipes
		// `nets`.
		pack.define(leaveRecordRun) {
			raw("\$execute unless data storage ra:transport dirtyset.n\$(id) run data modify storage ra:transport dirtyq append value \$(id)")
			raw("\$data modify storage ra:transport dirtyset.n\$(id) set value 1b")
			raw("\$execute unless data storage ra:transport nets.n\$(id) run return 0")

			val capPath = "nets.n\$(id).capacity"
			computeStorage(TR, capPath, provider { sub(storageOf(TR, capPath), scoreOf("#leave_cap", Obj.TR_TMP)) })

			val nodesPath = "nets.n\$(id).nodes"
			raw("\$execute unless data storage ra:transport $nodesPath run return 0")
			computeScore("#leave_nodes", Obj.TR_TMP, provider { sub(storageOf(TR, nodesPath), constant(1)) }, storeInt(TR, nodesPath))
			raw("\$execute if score #leave_nodes ${Obj.TR_TMP} matches ..0 run data remove storage ra:transport nets.n\$(id)")
		}

		// Internal: pass root status to any surviving member of the network.
		//
		// Only LOADED members can be found. With none, either the last node just went --
		// leave_record has already deleted the record when its count reached 0 -- or the survivors
		// are all in unloaded chunks. In that case the record is flagged `rootless`, and the
		// rebuild that next sees one of its nodes makes that node the root (rebuild/scope_id),
		// so the contents are still parked on someone when the network is next taken apart.
		pack.define(handover) {
			scoreboard.players.set(tr("#handover"), Obj.TR_TMP, 0)
			execute {
				asTarget(trMarkers(TrTag.NODE))
				ifCondition { score(self(), Obj.TR_NET) equalTo score(tr("#leave_net"), Obj.TR_TMP) }
				run { function(handoverPick) }
			}
			execute {
				ifCondition { score(tr("#handover"), Obj.TR_TMP) equalTo 0 }
				run { function(markRootless, TR, "lq") }
			}
		}

		// `mark_rootless {id}`, internal.
		pack.define(markRootless) {
			raw("\$execute if data storage ra:transport nets.n\$(id) run data modify storage ra:transport nets.n\$(id).rootless set value 1b")
		}

		// Internal: first candidate wins.
		pack.define(handoverPick) {
			execute {
				ifCondition { score(tr("#handover"), Obj.TR_TMP) equalTo 1 }
				run { returnValue(0) }
			}
			scoreboard.players.set(tr("#handover"), Obj.TR_TMP, 1)
			tag(self()) { add(TrTag.ROOT) }
		}

		registerRead()
		registerFlow()
	}

	/** `ra.tr.class` from the `class` macro argument, left alone for an unknown class. */
	private fun Function.readClass() =
		raw("\$execute if data storage ra:transport classes.\$(class) run execute store result score @s ${Obj.TR_CLASS} run data get storage ra:transport classes.\$(class)")
}
