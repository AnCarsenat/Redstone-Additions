package ra.lib.transport.net

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TrTag
import ra.lib.transport.net.Flow.registerFlow
import ra.lib.transport.net.Read.registerRead
import ra.lib.transport.tr
import ra.lib.transport.trMarkers

/** `ra_lib:transport/net/...`: node membership (join, rejoin, leave) and the root handover. */
object Net {
	val handover = lib("transport/net/handover")
	val handoverPick = lib("transport/net/handover_pick")

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

		// Internal: pass root status to any surviving member of the network. If there is no
		// survivor the contents are gone, which is the right outcome -- the last node of that
		// network was just removed.
		pack.define(handover) {
			scoreboard.players.set(tr("#handover"), Obj.TR_TMP, 0)
			execute {
				asTarget(trMarkers(TrTag.NODE))
				ifCondition { score(self(), Obj.TR_NET) equalTo score(tr("#leave_net"), Obj.TR_TMP) }
				run { function(handoverPick) }
			}
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
