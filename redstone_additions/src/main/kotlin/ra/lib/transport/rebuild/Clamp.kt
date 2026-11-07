package ra.lib.transport.rebuild

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.tr

/**
 * Spilling what a network inherited beyond its new capacity, when a tank is broken out of it,
 * rather than leaving the network reporting more than it can store.
 */
internal object Clamp {
	val clamp = lib("transport/rebuild/clamp")
	val clampRun = lib("transport/rebuild/clamp_run")
	val clampMedia = lib("transport/rebuild/clamp_media")
	val clampOne = lib("transport/rebuild/clamp_one")

	private val amt = tr("#net_amt_c")
	private val cap = tr("#net_cap_c")
	private val over = tr("#net_over_c")
	private val med = tr("#net_med_c")

	fun Ra.registerClamp() {
		// Internal: as a root marker.
		pack.define(clamp) {
			raw("execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ${Obj.TR_NET}")
			function(clampRun, TR, "arg")
		}

		// `clamp_run {id}`, internal: the dynamic-name half of clamp.
		//
		// A network whose capacity shrank -- pipes broken out of it, a tank removed -- can be
		// holding more than it can hold. The excess is dropped.
		//
		// The per-medium breakdown has to be trimmed with it. Clamping only the total would leave
		// `amounts` summing to more than `amount`, and every later take would be capped by a
		// per-medium figure the total could not cover, driving the total negative and handing out
		// contents the network does not have.
		pack.define(clampRun) {
			raw("\$execute store result score #net_amt_c ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).amount")
			raw("\$execute store result score #net_cap_c ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).capacity")

			returnUnlessOver()

			// What is lost comes off the NEWEST arrival first, which is the same rule the primary
			// medium follows from the other end: what got there first is what stays.
			data(TR) { modify("clampq") { set(nbt { }) } }
			raw("\$data modify storage ra:transport clampq.id set value \$(id)")
			function(clampMedia, TR, "clampq")

			raw("\$execute store result storage ra:transport nets.n\$(id).amount int 1 run scoreboard players get #net_amt_c ${Obj.TR_TMP}")
			raw("\$execute if score #net_amt_c ${Obj.TR_TMP} matches ..0 run data remove storage ra:transport nets.n\$(id).medium")
			raw("\$execute if score #net_amt_c ${Obj.TR_TMP} matches 1.. run data modify storage ra:transport nets.n\$(id).medium set from storage ra:transport nets.n\$(id).media[0].m")
		}

		// `clamp_media {id}`, internal: drop the newest medium, or part of it, until the total fits.
		// #net_amt_c / #net_cap_c ra.tr.tmp carry the running total and the capacity.
		//
		// Walked from the tail with media[-1] rather than by rotating the list, because rotating
		// reorders it -- and the order is what says which medium arrived first, which is what
		// `medium` and every bridge downstream depend on.
		pack.define(clampMedia) {
			returnUnlessOver()
			raw("\$execute unless data storage ra:transport nets.n\$(id).media[-1] run return 0")

			data(TR) { remove("clampm") }
			raw("\$data modify storage ra:transport clampm.id set value \$(id)")
			raw("\$data modify storage ra:transport clampm.medium set from storage ra:transport nets.n\$(id).media[-1].m")
			function(clampOne, TR, "clampm")

			function(clampMedia, TR, "clampq")
		}

		// `clamp_one {id, medium}`, internal: take the overflow out of one medium.
		//
		// Either the whole medium goes, because it is smaller than the excess, or it is reduced by
		// exactly the excess and the walk stops on the next test. Both cases leave #net_amt_c
		// holding the new total.
		pack.define(clampOne) {
			scoreboard.players.operation(over, Obj.TR_TMP, Operation.SET, amt, Obj.TR_TMP)
			scoreboard.players.operation(over, Obj.TR_TMP, Operation.REMOVE, cap, Obj.TR_TMP)

			raw("\$execute store result score #net_med_c ${Obj.TR_TMP} run data get storage ra:transport nets.n\$(id).amounts.\$(medium)")

			// Smaller than the overflow: the whole medium goes, and the walk moves on to the one
			// before it with the remainder still to find.
			raw("\$execute if score #net_med_c ${Obj.TR_TMP} <= #net_over_c ${Obj.TR_TMP} run data remove storage ra:transport nets.n\$(id).amounts.\$(medium)")
			raw("\$execute if score #net_med_c ${Obj.TR_TMP} <= #net_over_c ${Obj.TR_TMP} run data remove storage ra:transport nets.n\$(id).media[-1]")
			execute {
				ifCondition { score(med, Obj.TR_TMP) lessThanOrEqualTo score(over, Obj.TR_TMP) }
				run { scoreboard.players.operation(amt, Obj.TR_TMP, Operation.REMOVE, med, Obj.TR_TMP) }
			}
			execute {
				ifCondition { score(med, Obj.TR_TMP) lessThanOrEqualTo score(over, Obj.TR_TMP) }
				run { returnValue(0) }
			}

			// Bigger: it absorbs the whole overflow and the network is back within capacity.
			scoreboard.players.operation(med, Obj.TR_TMP, Operation.REMOVE, over, Obj.TR_TMP)
			raw("\$execute store result storage ra:transport nets.n\$(id).amounts.\$(medium) int 1 run scoreboard players get #net_med_c ${Obj.TR_TMP}")
			scoreboard.players.operation(amt, Obj.TR_TMP, Operation.SET, cap, Obj.TR_TMP)
		}
	}

	/** `execute unless score #net_amt_c ra.tr.tmp > #net_cap_c ra.tr.tmp run return 0`. */
	private fun Function.returnUnlessOver() = execute {
		unlessCondition { score(amt, Obj.TR_TMP) greaterThan score(cap, Obj.TR_TMP) }
		run { returnValue(0) }
	}
}
