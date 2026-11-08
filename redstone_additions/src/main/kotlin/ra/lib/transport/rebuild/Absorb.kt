package ra.lib.transport.rebuild

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.features.predicates.providers.add
import ra.core.Obj
import ra.core.Ra
import ra.core.computeScore
import ra.core.computeStorage
import ra.core.provider
import ra.core.scoreOf
import ra.core.storageOf
import ra.core.storeInt
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.tr

/** Folding each node's capacity and carried contents into the network it landed in. */
internal object Absorb {
	val accumulateNode = lib("transport/rebuild/accumulate_node")
	val accumulate = lib("transport/rebuild/accumulate")
	val absorb = lib("transport/rebuild/absorb")
	val absorbNext = lib("transport/rebuild/absorb_next")
	val absorbOne = lib("transport/rebuild/absorb_one")
	val absorbPotion = lib("transport/rebuild/absorb_potion")
	val adoptMedium = lib("transport/rebuild/adopt_medium")
	val adoptMediumWrite = lib("transport/rebuild/adopt_medium_write")

	fun Ra.registerAbsorb() {
		// Internal: fold one node's capacity and carried contents into its new network.
		pack.define(accumulateNode) {
			raw("execute store result score #node_cap ${Obj.TR_TMP} run scoreboard players get @s ${Obj.TR_CAP}")
			raw("execute store result score #node_carry ${Obj.TR_TMP} run scoreboard players get @s ${Obj.TR_CARRY}")
			storeNetId("arg")
			function(accumulate, TR, "arg")
		}

		// `accumulate {id}`, internal: the dynamic-name half of accumulate_node.
		pack.define(accumulate) {
			for ((key, node) in listOf("capacity" to "#node_cap", "amount" to "#node_carry")) {
				val path = "nets.n\$(id).$key"
				computeStorage(TR, path, provider { add(storageOf(TR, path), scoreOf(node, Obj.TR_TMP)) })
			}
		}

		// Internal: fold one node's carried contents into the network it landed in, medium by
		// medium. Context: as a node marker carrying something.
		//
		// Replaces adopt_medium, which could only name the network after the fact and had to
		// pretend everything it inherited was that one medium. Two runs joined by a new pipe now
		// arrive as two sets of per-medium figures that add up, and a run that carried water and
		// lava still carries water and lava afterwards.
		//
		// The old function is still called for a node carrying a total with no breakdown behind
		// it, which is the only case left that cannot be split up.
		pack.define(absorb) {
			execute {
				unlessCondition { data(self(), "data.data.carry[0]") }
				run { returnRun { function(adoptMedium) } }
			}

			data(TR) { remove("absq") }
			storeNetId("absq")
			data(TR) { modify("absq.queue") { set(self(), "data.data.carry") } }

			// The first carrier to arrive with a potion sets the network's, matching the rule the
			// media list follows: what got there first is what stays.
			execute {
				ifCondition { data(self(), "data.data.carry_potion") }
				run { data(TR) { modify("absq.potion") { set(self(), "data.data.carry_potion") } } }
			}
			execute {
				ifCondition { data(TR, "absq.potion") }
				run { function(absorbPotion, TR, "absq") }
			}

			function(absorbNext)

			// Insurance. If every carried entry came through holding nothing, the network is left
			// nameless -- and a run reporting "Empty" over a non-zero Amount is the most confusing
			// thing this code can produce. adopt_medium only writes when there is no medium yet, so
			// this costs one test in the normal case.
			function(adoptMedium)

			data(self()) { remove("data.data.carry") }
			data(self()) { remove("data.data.carry_potion") }
		}

		// Internal: fold the head of the carried list into the network, then the rest.
		// Context: as the carrying node, with storage ra:transport absq holding the network id and
		// what is left of the list.
		pack.define(absorbNext) {
			execute {
				unlessCondition { data(TR, "absq.queue[0]") }
				run { returnValue(0) }
			}

			data(TR) { modify("absq.m") { set(TR, "absq.queue[0].m") } }
			raw("execute store result score #abs.a ${Obj.TR_TMP} run data get storage ra:transport absq.queue[0].a")
			data(TR) { remove("absq.queue[0]") }

			function(absorbOne, TR, "absq")
			function(absorbNext)
		}

		// `absorb_one {id, m}`, internal: add one carried medium to the network's breakdown.
		// #abs.a ra.tr.tmp holds how much of it there is.
		//
		// The same shape as net/offer_write, and for the same reason: a storage provider reads a
		// key that is not there as 0, and 0 is exactly the right starting value for a medium
		// arriving on this network for the first time. A new medium is appended to the list, so the
		// order still records what got here first -- and it is new exactly when the sum just
		// written is no more than what was added.
		pack.define(absorbOne) {
			execute {
				ifCondition { score(tr("#abs.a"), Obj.TR_TMP) lessThanOrEqualTo 0 }
				run { returnValue(0) }
			}

			val path = "nets.n\$(id).amounts.\$(m)"
			computeScore("#abs.cur", Obj.TR_TMP, provider { add(storageOf(TR, path), scoreOf("#abs.a", Obj.TR_TMP)) }, storeInt(TR, path))
			raw("\$execute if score #abs.cur ${Obj.TR_TMP} <= #abs.a ${Obj.TR_TMP} run data modify storage ra:transport nets.n\$(id).media append value {m:\"\$(m)\"}")

			raw("\$data modify storage ra:transport nets.n\$(id).medium set from storage ra:transport nets.n\$(id).media[0].m")
		}

		// `absorb_potion {id}`, internal: give the network the carried potion contents, if it has none yet.
		pack.define(absorbPotion) {
			raw("\$execute unless data storage ra:transport nets.n\$(id).potion run data modify storage ra:transport nets.n\$(id).potion set from storage ra:transport absq.potion")
		}

		// Internal: give a network the medium of the contents it inherited. When two networks
		// holding different media are joined, the first carrier's medium wins and the rest is
		// treated as the same medium rather than vanishing -- merging two incompatible tanks is a
		// player mistake, not a reason to void metal.
		pack.define(adoptMedium) {
			execute {
				unlessCondition { data(self(), "data.data.medium") }
				run { returnValue(0) }
			}

			storeNetId("arg")
			data(TR) { modify("arg.medium") { set(self(), "data.data.medium") } }
			function(adoptMediumWrite, TR, "arg")
		}

		// `adopt_medium_write {id, medium}`, internal: set the network's medium if it does not have one yet.
		pack.define(adoptMediumWrite) {
			raw("\$execute unless data storage ra:transport nets.n\$(id).medium run data modify storage ra:transport nets.n\$(id).medium set value \"\$(medium)\"")
		}
	}

	/** `execute store result storage ra:transport <queue>.id int 1 run scoreboard players get @s ra.tr.net`. */
	private fun io.github.ayfri.kore.functions.Function.storeNetId(queue: String) =
		raw("execute store result storage ra:transport $queue.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
}
