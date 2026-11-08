package ra.lib.transport.net

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.features.predicates.providers.add
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.sub
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
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

/** `offer`, `take`, `take_any`: moving contents in and out of a node's network. */
object Flow {
	val offerWrite = lib("transport/net/offer_write")
	val takeWrite = lib("transport/net/take_write")

	private val moved = tr("#net_moved")
	private val want = tr("#net_want")

	/** `#net_want ra.tr.tmp` as a compute provider. */
	private val wantP = scoreOf("#net_want", Obj.TR_TMP)

	fun Ra.registerFlow() {
		pack.define(RaLib.Transport.Net.offer) { offer() }
		pack.define(offerWrite) { offerWrite() }
		pack.define(RaLib.Transport.Net.take) { take() }
		pack.define(takeWrite) { takeWrite() }

		// `take_any {amount}`: take from whichever medium the network calls its primary, and leave
		// the name of what was taken in storage ra:transport took.medium. Context: as a node marker.
		// Returns how much was removed.
		//
		// For the callers that move contents around without caring what they are -- a Valve, a
		// Breaker, an Ender Fluid Vault. They still have to know afterwards WHICH medium they are
		// carrying, so that the far side is given the same thing, which is why the name is parked
		// in storage rather than thrown away.
		pack.define(RaLib.Transport.Net.takeAny) {
			data(TR) { remove("took") }
			startMove()

			function(RaLib.Transport.Net.read)
			execute {
				unlessCondition { data(TR, "cur.medium") }
				run { returnValue(0) }
			}

			data(TR) { modify("took.medium") { set(TR, "cur.medium") } }
			raw("\$data modify storage ra:transport took.amount set value \$(amount)")
			returnRun { function(RaLib.Transport.Net.take, TR, "took") }
		}
	}

	/** `#net_moved = 0`, and return 0 for a node in no network. */
	private fun Function.startMove() {
		scoreboard.players.set(moved, Obj.TR_TMP, 0)
		execute {
			ifCondition { score(self(), Obj.TR_NET) lessThanOrEqualTo 0 }
			run { returnValue(0) }
		}
	}

	/** Queue `{id, medium}` in storage ra:transport wq and run [write] with it, then report #net_want as moved. */
	private fun Function.commit(write: io.github.ayfri.kore.arguments.types.resources.FunctionArgument) {
		raw("execute store result storage ra:transport wq.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
		raw("\$data modify storage ra:transport wq.medium set value \"\$(medium)\"")
		function(write, TR, "wq")

		scoreboard.players.operation(moved, Obj.TR_TMP, Operation.SET, want, Obj.TR_TMP)
		returnRun { scoreboard.players.get(moved, Obj.TR_TMP) }
	}

	/**
	 * `offer {amount, medium}`: push contents into this node's network. Context: as a node marker.
	 * Returns how much was actually accepted, which may be less than asked for, or 0 when the
	 * network is full.
	 *
	 * A NETWORK NO LONGER REFUSES AN UNFAMILIAR MEDIUM
	 * It used to: anything already holding water turned lava away at the door, so a pipe run was
	 * single-purpose for as long as it held anything at all. Now every medium has its own entry and
	 * the only thing that can refuse an offer is space.
	 *
	 * Space is the SUM. A network clogs when water plus lava plus steam reaches its capacity, not
	 * when any one of them does -- which is the whole point of mixing: a pipe run is one pool of
	 * volume that several media share, and the player budgets it as one number.
	 */
	private fun Function.offer() {
		startMove()

		function(RaLib.Transport.Net.read)

		// want = min(amount, capacity - amount held), in one evaluation. A full (or overfull)
		// network makes the free space 0 or less, so want comes out 0 or less and the guard below
		// refuses the offer -- the same outcome the separate `free <= 0` test used to give.
		raw("\$scoreboard players set #net_want ${Obj.TR_TMP} \$(amount)")
		computeScore("#net_want", Obj.TR_TMP, provider {
			min(wantP, sub(scoreOf("#net_capacity", Obj.TR_TMP), scoreOf("#net_amount", Obj.TR_TMP)))
		})
		returnIfNothingWanted()

		commit(offerWrite)
	}

	private fun Function.returnIfNothingWanted() = execute {
		ifCondition { score(want, Obj.TR_TMP) lessThanOrEqualTo 0 }
		run { returnValue(0) }
	}

	/**
	 * `offer_write {id, medium}`, internal: commit an accepted offer, to the total and to the
	 * medium's own entry.
	 *
	 * A storage provider reads a path that is not there as 0 -- the same 0 a failed `data get`
	 * used to store -- which is exactly the right starting value for a medium arriving on this
	 * network for the first time.
	 */
	private fun Function.offerWrite() {
		computeStorage(TR, "nets.n\$(id).amount", provider { add(storageOf(TR, "nets.n\$(id).amount"), wantP) })

		val medPath = "nets.n\$(id).amounts.\$(medium)"
		computeScore("#net_med", Obj.TR_TMP, provider { add(storageOf(TR, medPath), wantP) }, storeInt(TR, medPath))

		// First arrival of this medium: give it a place in the enumeration order. The list is
		// compounds rather than bare strings so that a medium can be removed by value when it runs
		// out -- `data remove list[{m:"water"}]` only works on compound elements.
		//
		// Tested on the sum just written rather than on a separate read beforehand: the new
		// figure is no more than #net_want exactly when the old one was 0 or less, absent included.
		raw("\$execute if score #net_med ${Obj.TR_TMP} <= #net_want ${Obj.TR_TMP} run data modify storage ra:transport nets.n\$(id).media append value {m:\"\$(medium)\"}")

		raw("\$data modify storage ra:transport nets.n\$(id).medium set from storage ra:transport nets.n\$(id).media[0].m")
	}

	/**
	 * `take {amount, medium}`: pull one medium out of this node's network. Context: as a node
	 * marker. Returns how much was actually removed, capped by how much of THAT medium the network
	 * holds -- not by its total.
	 *
	 * The medium is required. A network can hold several at once, so "take 1000" is no longer a
	 * question with an answer; a caller that does not care which it gets should read net/read
	 * first and pass cur.medium, which is the primary. ra_lib:transport/net/take_any does exactly
	 * that and is there for callers with nothing better to go on.
	 */
	private fun Function.take() {
		startMove()

		function(RaLib.Transport.Net.read)

		// want = min(amount, what the network holds of this medium). A medium it does not hold
		// reads as 0, so want comes out 0 and the guard below refuses the take.
		raw("\$scoreboard players set #net_want ${Obj.TR_TMP} \$(amount)")
		computeScore("#net_want", Obj.TR_TMP, provider { min(wantP, storageOf(TR, "cur.amounts.\$(medium)")) })
		returnIfNothingWanted()

		commit(takeWrite)
	}

	/**
	 * `take_write {id, medium}`, internal: commit a withdrawal, from the total and from the
	 * medium's own entry.
	 *
	 * A medium drained to nothing leaves the network entirely -- its key goes and so does its place
	 * in the media list -- so a pipe run that once carried lava does not keep claiming to until
	 * something else displaces it. A network drained to nothing forgets everything, which is what
	 * lets a different medium be pumped in afterwards without the player breaking anything.
	 */
	private fun Function.takeWrite() {
		val amountPath = "nets.n\$(id).amount"
		computeScore("#net_new", Obj.TR_TMP, provider { sub(storageOf(TR, amountPath), wantP) }, storeInt(TR, amountPath))

		// Written unconditionally and then removed when it has run out, rather than written only
		// when something is left: the end state is the same and it saves re-reading the score.
		val medPath = "nets.n\$(id).amounts.\$(medium)"
		computeScore("#net_med", Obj.TR_TMP, provider { sub(storageOf(TR, medPath), wantP) }, storeInt(TR, medPath))
		raw("\$execute if score #net_med ${Obj.TR_TMP} matches ..0 run data remove storage ra:transport nets.n\$(id).amounts.\$(medium)")
		raw("\$execute if score #net_med ${Obj.TR_TMP} matches ..0 run data remove storage ra:transport nets.n\$(id).media[{m:\"\$(medium)\"}]")

		// The primary is whatever is left at the front of the list. Removed first so that an
		// emptied network is left with no medium at all rather than a stale one: the `set from`
		// below fails silently on an empty list and would leave the old value standing.
		raw("\$data remove storage ra:transport nets.n\$(id).medium")
		raw("\$execute if score #net_new ${Obj.TR_TMP} matches 1.. run data modify storage ra:transport nets.n\$(id).medium set from storage ra:transport nets.n\$(id).media[0].m")
	}
}
