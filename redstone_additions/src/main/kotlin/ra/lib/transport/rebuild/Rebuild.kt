package ra.lib.transport.rebuild

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.lib
import ra.lib.transport.TrTag
import ra.lib.transport.rebuild.Absorb.registerAbsorb
import ra.lib.transport.rebuild.Clamp.registerClamp
import ra.lib.transport.rebuild.Flood.registerFlood
import ra.lib.transport.rebuild.Scope.registerScope
import ra.lib.transport.rebuild.Snapshot.registerSnapshot
import ra.lib.transport.tr

/**
 * `ra_lib:transport/rebuild/run`: bring the networks up to date with the nodes that joined or left.
 *
 * Only reached when the topology actually changed, and only the networks that change touched are
 * recounted (rebuild/scope says which). Those are taken apart and put back together: each keeps
 * one node tagged ra.tr.root, the network's contents are parked on that node for the duration of
 * the rebuild, then summed back into whichever network the node lands in. Every other network is
 * left exactly as it is, and can only GROW in a rebuild, by adopting new nodes that touch it.
 *
 * WHY NOT EVERYTHING, EVERY TIME
 * That is what this used to do, and `@e` only sees loaded entities: a network whose root was in an
 * unloaded chunk had its record wiped with nobody carrying its contents, and a network half in
 * view was recounted from that half and clamped to its capacity. Laying one pipe anywhere emptied
 * every tank out of view. A network is now only taken apart when every node of it is in view, and
 * its id and record are untouched otherwise.
 *
 * THE INVARIANT: a network's `amount` is exactly the sum of its `amounts`. A rebuild carries the
 * per-medium breakdown and nothing else, and accumulate sums the total back from what was carried
 * -- see snapshot_read. The order of the passes below is load-bearing; each comment says why.
 */
object Rebuild {
	val run = lib("transport/rebuild/run")
	val again = lib("transport/rebuild/again")

	private fun sel(tag: String, extra: String = "") = "@e[type=marker,tag=$tag$extra]"

	fun Ra.registerRebuild() {
		pack.define(run) {
			val scope = sel(TrTag.SCOPE)
			val claimed = sel(TrTag.CLAIMED)

			raw("data remove storage ra:transport rb")
			scoreboard.players.set(tr("#meet"), Obj.TR_TMP, 0)
			scoreboard.players.set(tr("#conflict"), Obj.TR_TMP, 0)
			scoreboard.players.set(tr("#next_net"), Obj.TR_TMP, 0)

			// --- Which networks may be taken apart ---
			function(Scope.scope)

			// --- Park their contents and their number on each one's root ---
			raw("scoreboard players set $scope ${Obj.TR_CARRY} 0")
			raw("scoreboard players set $scope ${Obj.TR_OLD} 0")
			raw("execute as ${sel(TrTag.SCOPE, ",tag=${TrTag.ROOT}")} run function ${Snapshot.snapshot.asId()}")

			// --- Forget their assignment ---
			// Their records went with the snapshot. No other record is touched: a network out of
			// view keeps its record, and its nodes find it again when they load.
			raw("scoreboard players set $scope ${Obj.TR_NET} 0")
			raw("tag $scope remove ${TrTag.ROOT}")

			// --- Kept networks take in the unassigned nodes that touch them ---
			// Before any fresh network is seeded, so nothing touching a kept network is left out of
			// it. #meet makes this one flood report two kept networks meeting (rebuild/meet).
			raw("execute as ${sel(TrTag.NODE, ",scores={${Obj.TR_NET}=0}")} at @s run function ${Flood.adopt.asId()}")
			scoreboard.players.set(tr("#meet"), Obj.TR_TMP, 1)
			function(Flood.expand)
			scoreboard.players.set(tr("#meet"), Obj.TR_TMP, 0)

			// --- One fresh network per remaining connected component ---
			function(Flood.assignNext)

			// --- Capacity and carried contents of every node placed in this rebuild ---
			raw("execute as $claimed run function ${Absorb.accumulateNode.asId()}")

			// --- Fold the carried per-medium breakdown back in ---
			// After the totals, because a network has to exist before anything can be added to it,
			// and before the clamp, because the clamp trims the breakdown as well as the total and
			// there is nothing to trim until this has run.
			raw("execute as ${sel(TrTag.CLAIMED, ",scores={${Obj.TR_CARRY}=1..}")} run function ${Absorb.absorb.asId()}")

			// A network can inherit more than its new capacity holds if a tank was removed. Only a
			// network built in this rebuild is clamped: it was wholly in view, so its capacity is
			// the whole truth. A kept network may be partly out of view, and cutting it to the part
			// that is loaded is exactly the bug this design exists to prevent.
			raw("execute as ${sel(TrTag.FRESH)} run function ${Clamp.clamp.asId()}")

			// Carried values are consumed; clear them so a later rebuild cannot double-count.
			raw("scoreboard players set $claimed ${Obj.TR_CARRY} 0")
			raw("scoreboard players set $claimed ${Obj.TR_OLD} 0")
			for (tag in listOf(TrTag.SCOPE, TrTag.CLAIMED, TrTag.FRESH)) raw("tag ${sel(tag)} remove $tag")
			for (scratch in listOf("arg", "snapq", "snapi", "absq", "rb")) raw("data remove storage ra:transport $scratch")

			// Two kept networks met (rebuild/meet): go round once more, with both of them in scope
			// from `pending`. Once only -- the second pass has no unassigned nodes left, so it cannot
			// find a meeting of its own, and the guard keeps it that way regardless.
			raw("execute if score #conflict ${Obj.TR_TMP} matches 1 unless score #rb_again ${Obj.TR_TMP} matches 1 run function ${again.asId()}")
		}

		pack.define(again) {
			scoreboard.players.set(tr("#rb_again"), Obj.TR_TMP, 1)
			function(run)
			scoreboard.players.set(tr("#rb_again"), Obj.TR_TMP, 0)
		}

		registerScope()
		registerFlood()
		registerSnapshot()
		registerAbsorb()
		registerClamp()
	}
}
