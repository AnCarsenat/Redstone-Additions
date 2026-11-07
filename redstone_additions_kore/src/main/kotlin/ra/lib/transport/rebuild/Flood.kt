package ra.lib.transport.rebuild

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.scores.score
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.Transport
import ra.lib.transport.TrTag
import ra.lib.transport.tr
import ra.lib.transport.trMarkers

/** Assigning networks: one seed per connected component, flooded breadth-first. */
internal object Flood {
	val assignNext = lib("transport/rebuild/assign_next")
	val seed = lib("transport/rebuild/seed")
	val nextId = lib("transport/rebuild/next_id")
	val resetNet = lib("transport/rebuild/reset_net")
	val expand = lib("transport/rebuild/expand")
	val spread = lib("transport/rebuild/spread")
	val claim = lib("transport/rebuild/claim")

	private val nextNet = tr("#next_net")
	private val netSeq = tr("#net_seq")

	fun Ra.registerFlood() {
		// Internal: seed a new network on the next still-unassigned node, flood it, and repeat
		// until every node belongs to one.
		//
		// OLD ROOTS GO FIRST
		// Seeding at a node that used to be a root is what lets rebuild/seed give the component
		// that node's old number back. Doing all of those before any new component is seeded also
		// means every inherited number is claimed before the counter issues a fresh one, so the two
		// can never land on the same id.
		pack.define(assignNext) {
			val unassigned = trMarkers(TrTag.NODE) { scores { score(Obj.TR_NET, 0) }; limit = 1 }
			execute {
				unlessCondition { entity(unassigned) }
				run { returnValue(0) }
			}

			execute {
				asTarget(trMarkers(TrTag.NODE) { scores { score(Obj.TR_NET, 0); score(Obj.TR_OLD, rangeOrIntStart(1)) }; limit = 1 })
				run { function(seed) }
			}
			execute {
				unlessCondition { entity(trMarkers(TrTag.FRONTIER) { limit = 1 }) }
				asTarget(unassigned)
				run { function(seed) }
			}

			function(expand)

			function(assignNext)
		}

		// Internal: start a new network at this node. The seed also becomes the network's root,
		// which is where the contents get parked on the next rebuild.
		//
		// A NETWORK KEEPS ITS NUMBER
		// Ids used to be handed out 1, 2, 3... on every rebuild, so they said nothing about
		// identity: break one run and every run numbered above it slid down one. The Multimeter
		// would report "Network 22", the run would be torn out, and `nets.n22` would still be
		// sitting in storage afterwards holding somebody else's water -- which reads exactly like a
		// network that refused to die.
		//
		// So a component seeded at a node that was the root of an old network inherits that
		// network's number, and anything genuinely new takes the next number the counter has never
		// issued. rebuild/assign_next is what makes this work: it seeds at old roots first, so every
		// id that is going to be inherited is claimed before any fresh one is handed out.
		pack.define(seed) {
			scoreboard.players.set(nextNet, Obj.TR_TMP, 0)
			execute {
				ifCondition { score(self(), Obj.TR_OLD) greaterThanOrEqualTo 1 }
				run { scoreboard.players.operation(nextNet, Obj.TR_TMP, Operation.SET, self(), Obj.TR_OLD) }
			}
			execute {
				ifCondition { score(nextNet, Obj.TR_TMP) lessThanOrEqualTo 0 }
				run { function(nextId) }
			}
			execute {
				ifCondition { score(nextNet, Obj.TR_TMP) greaterThan score(netSeq, Obj.TR_TMP) }
				run { scoreboard.players.operation(netSeq, Obj.TR_TMP, Operation.SET, nextNet, Obj.TR_TMP) }
			}

			scoreboard.players.operation(self(), Obj.TR_NET, Operation.SET, nextNet, Obj.TR_TMP)
			tag(self()) { add(TrTag.ROOT) }
			tag(self()) { add(TrTag.FRONTIER) }

			raw("execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(resetNet, TR, "arg")
		}

		// Internal: hand out an id no network has ever had. The counter only ever goes up, and it
		// is never reset, so a number that has been retired stays retired.
		pack.define(nextId) {
			scoreboard.players.add(netSeq, Obj.TR_TMP, 1)
			scoreboard.players.operation(nextNet, Obj.TR_TMP, Operation.SET, netSeq, Obj.TR_TMP)
		}

		// `reset_net {id}`, internal: zero a freshly created network's totals. amounts and media
		// start empty rather than absent, so nothing downstream has to tell "no breakdown yet" apart
		// from "migrated and holding nothing".
		pack.define(resetNet) {
			raw("\$data modify storage ra:transport nets.n\$(id) set value {amount:0,capacity:0,amounts:{},media:[]}")
		}

		// Internal: breadth-first flood of the current network. One pass per wave, so the cost is
		// proportional to the network's diameter rather than to a fixed guess at how far it might reach.
		pack.define(expand) {
			execute {
				unlessCondition { entity(trMarkers(TrTag.FRONTIER) { limit = 1 }) }
				run { returnValue(0) }
			}

			tag(trMarkers(TrTag.FRONTIER)) { add(TrTag.WAVE) }
			tag(trMarkers(TrTag.FRONTIER)) { remove(TrTag.FRONTIER) }

			execute {
				asTarget(trMarkers(TrTag.WAVE))
				at(self())
				run { function(spread) }
			}
			tag(trMarkers(TrTag.WAVE)) { remove(TrTag.WAVE) }

			function(expand)
		}

		// Internal: claim the six neighbours of this node for the current network.
		//
		// Networks never merge across classes: a fluid network and an item network can run through
		// the same block space without touching. The class is compared as a score rather than
		// branching on a tag per class, so adding another kind of network costs nothing here.
		pack.define(spread) {
			val curClass = tr("#cur_class")
			scoreboard.players.operation(curClass, Obj.TR_TMP, Operation.SET, self(), Obj.TR_CLASS)

			for (offset in Transport.neighbours) execute {
				positioned(offset)
				asTarget(trMarkers(TrTag.NODE) { scores { score(Obj.TR_NET, 0) }; distance = rangeOrDoubleEnd(0.75); limit = 1 })
				ifCondition { score(self(), Obj.TR_CLASS) equalTo score(curClass, Obj.TR_TMP) }
				run { function(claim) }
			}
		}

		// Internal: add an unassigned neighbour to the network being flooded.
		pack.define(claim) {
			scoreboard.players.operation(self(), Obj.TR_NET, Operation.SET, nextNet, Obj.TR_TMP)
			tag(self()) { add(TrTag.FRONTIER) }
		}
	}
}
