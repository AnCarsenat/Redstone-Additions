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
import ra.core.macroCall
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
	val nextIdCheck = lib("transport/rebuild/next_id_check")
	val adopt = lib("transport/rebuild/adopt")
	val meet = lib("transport/rebuild/meet")
	val conflict = lib("transport/rebuild/conflict")
	val conflictIds = lib("transport/rebuild/conflict_ids")

	private val nextNet = tr("#next_net")
	private val netSeq = tr("#net_seq")

	/** The six neighbours as text, in the same order as [Transport.neighbours]. */
	private val offsets = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~")

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
			scoreboard.players.operation(netSeq, Obj.TR_TMP, Operation.MAX, nextNet, Obj.TR_TMP)

			scoreboard.players.operation(self(), Obj.TR_NET, Operation.SET, nextNet, Obj.TR_TMP)
			tag(self()) { add(TrTag.ROOT) }
			tag(self()) { add(TrTag.FRONTIER) }
			tag(self()) { add(TrTag.CLAIMED) }
			tag(self()) { add(TrTag.FRESH) }

			raw("execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(resetNet, TR, "arg")
		}

		// Internal: hand out an id no network has ever had. The counter only ever goes up, and it
		// is never reset, so a number that has been retired stays retired.
		//
		// A number that still has a record is skipped as well. Records of networks out of view are
		// kept now, and a world from before #net_seq existed may hold ids above the counter; a fresh
		// network written over one of those would delete it just as surely as the old wipe did.
		pack.define(nextId) {
			scoreboard.players.add(netSeq, Obj.TR_TMP, 1)
			scoreboard.players.operation(nextNet, Obj.TR_TMP, Operation.SET, netSeq, Obj.TR_TMP)
			raw("execute store result storage ra:transport rb.nid.id int 1 run scoreboard players get #net_seq ${Obj.TR_TMP}")
			function(nextIdCheck, TR, "rb.nid")
		}

		// `next_id_check {id}`, internal: try the next number if this one is taken.
		pack.define(nextIdCheck) {
			raw("\$execute if data storage ra:transport nets.n\$(id) run function ${nextId.asId()}")
		}

		// `reset_net {id}`, internal: zero a freshly created network's totals. amounts and media
		// start empty rather than absent, so nothing downstream has to tell "no breakdown yet" apart
		// from "migrated and holding nothing".
		pack.define(resetNet) {
			raw("\$data modify storage ra:transport nets.n\$(id) set value {amount:0,capacity:0,nodes:0,amounts:{},media:[]}")
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

		// Internal: claim the six neighbours of this node for the network this node is in.
		//
		// Networks never merge across classes: a fluid network and an item network can run through
		// the same block space without touching. The class is compared as a score rather than
		// branching on a tag per class, so adding another kind of network costs nothing here.
		//
		// The number claimed into is this node's own, not "the network being seeded": the first
		// flood of a rebuild runs out of every adopted node at once (rebuild/adopt), each carrying
		// the number of the network it joined.
		pack.define(spread) {
			val curClass = tr("#cur_class")
			scoreboard.players.operation(curClass, Obj.TR_TMP, Operation.SET, self(), Obj.TR_CLASS)
			scoreboard.players.operation(tr("#cur_net"), Obj.TR_TMP, Operation.SET, self(), Obj.TR_NET)

			for (offset in Transport.neighbours) execute {
				positioned(offset)
				asTarget(trMarkers(TrTag.NODE) { scores { score(Obj.TR_NET, 0) }; distance = rangeOrDoubleEnd(0.75); limit = 1 })
				ifCondition { score(self(), Obj.TR_CLASS) equalTo score(curClass, Obj.TR_TMP) }
				run { function(claim) }
			}

			raw("execute if score #meet ${Obj.TR_TMP} matches 1 run function ${meet.asId()}")
		}

		// Internal: add an unassigned neighbour to the network being flooded.
		pack.define(claim) {
			scoreboard.players.operation(self(), Obj.TR_NET, Operation.SET, tr("#cur_net"), Obj.TR_TMP)
			tag(self()) { add(TrTag.FRONTIER) }
			tag(self()) { add(TrTag.CLAIMED) }
		}

		// Internal: as an unassigned node, at it. Join the first network of its own class found on
		// one of its six sides, if any -- a kept network, or one a node before it just joined.
		//
		// This is how a network the rebuild is not taking apart still grows: the new node is
		// counted into the existing record (rebuild/accumulate_node), and nothing else about that
		// network is recounted. It is the reason extending a network that runs out of view is safe,
		// and the reason placing a pipe does not cost a pass over every other pipe.
		//
		// Runs for every unassigned node before any fresh network is seeded, so a node that touches a
		// kept network can never end up in a fresh one beside it.
		pack.define(adopt) {
			raw("scoreboard players operation #cur_class ${Obj.TR_TMP} = @s ${Obj.TR_CLASS}")
			raw("scoreboard players set #adopt ${Obj.TR_TMP} 0")
			for (offset in offsets) raw(
				"execute if score #adopt ${Obj.TR_TMP} matches 0 positioned $offset as @e[type=marker,tag=${TrTag.NODE},distance=..0.75,scores={${Obj.TR_NET}=1..}] " +
					"if score @s ${Obj.TR_CLASS} = #cur_class ${Obj.TR_TMP} run scoreboard players operation #adopt ${Obj.TR_TMP} = @s ${Obj.TR_NET}"
			)
			raw("execute if score #adopt ${Obj.TR_TMP} matches 0 run return 0")
			raw("scoreboard players operation @s ${Obj.TR_NET} = #adopt ${Obj.TR_TMP}")
			tag(self()) { add(TrTag.FRONTIER) }
			tag(self()) { add(TrTag.CLAIMED) }
		}

		// Internal: as a node flooded out of an adopted one, at it (only while #meet is 1). A
		// neighbour of the same class in a DIFFERENT network means two kept networks now touch --
		// a pipe was laid between them. They cannot simply be merged here: a kept network may have
		// nodes out of view that would keep the old number. Both go on `pending` and #conflict makes
		// rebuild/run go round once more straight away, with the two in scope: whichever is wholly
		// in view is taken apart and the other adopts it (both, if both are, and they merge as
		// fresh). Fresh networks never need this: they are seeded only after adoption, from nodes
		// no kept network touches.
		pack.define(meet) {
			for (offset in offsets) raw(
				"execute positioned $offset as @e[type=marker,tag=${TrTag.NODE},distance=..0.75,scores={${Obj.TR_NET}=1..}] " +
					"if score @s ${Obj.TR_CLASS} = #cur_class ${Obj.TR_TMP} unless score @s ${Obj.TR_NET} = #cur_net ${Obj.TR_TMP} run function ${conflict.asId()}"
			)
		}

		// Internal: as the neighbour found by meet.
		pack.define(conflict) {
			raw("execute store result storage ra:transport rb.c.a int 1 run scoreboard players get @s ${Obj.TR_NET}")
			raw("execute store result storage ra:transport rb.c.b int 1 run scoreboard players get #cur_net ${Obj.TR_TMP}")
			function(conflictIds, TR, "rb.c")
		}

		// `conflict_ids {a, b}`, internal.
		pack.define(conflictIds) {
			raw("\$${macroCall(Scope.pend, "{id:\$(a)}")}")
			raw("\$${macroCall(Scope.pend, "{id:\$(b)}")}")
			scoreboard.players.set(tr("#conflict"), Obj.TR_TMP, 1)
		}
	}
}
