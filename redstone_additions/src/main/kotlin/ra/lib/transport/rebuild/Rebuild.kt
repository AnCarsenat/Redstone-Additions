package ra.lib.transport.rebuild

import io.github.ayfri.kore.arguments.scores.score
import io.github.ayfri.kore.arguments.selector.scores
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.lib.lib
import ra.lib.transport.TR
import ra.lib.transport.TrTag
import ra.lib.transport.rebuild.Absorb.registerAbsorb
import ra.lib.transport.rebuild.Clamp.registerClamp
import ra.lib.transport.rebuild.Flood.registerFlood
import ra.lib.transport.rebuild.Snapshot.registerSnapshot
import ra.lib.transport.tr
import ra.lib.transport.trMarkers

/**
 * `ra_lib:transport/rebuild/run`: recompute every transport network from scratch.
 *
 * Only reached when the topology actually changed. The contents of the old networks are carried
 * over: each network keeps one seed node tagged ra.tr.root, and the network's contents are parked
 * on that node for the duration of the rebuild, then summed back into whichever new network the
 * node lands in.
 *
 * THE INVARIANT: a network's `amount` is exactly the sum of its `amounts`. A rebuild carries the
 * per-medium breakdown and nothing else, and accumulate sums the total back from what was carried
 * -- see snapshot_read. The order of the passes below is load-bearing; each comment says why.
 */
object Rebuild {
	val run = lib("transport/rebuild/run")

	fun Ra.registerRebuild() {
		pack.define(run) {
			val nodes = trMarkers(TrTag.NODE)
			val roots = trMarkers(TrTag.ROOT)

			// --- Park the old contents and the old number on each network's root ---
			scoreboard.players.set(nodes, Obj.TR_CARRY, 0)
			scoreboard.players.set(nodes, Obj.TR_OLD, 0)
			execute {
				asTarget(roots)
				run { function(Snapshot.snapshot) }
			}

			// --- Forget the old assignment ---
			scoreboard.players.set(nodes, Obj.TR_NET, 0)
			tag(roots) { remove(TrTag.ROOT) }
			scoreboard.players.set(tr("#next_net"), Obj.TR_TMP, 0)

			// Every network is rebuilt from its nodes, so this is also what deletes the ones whose
			// blocks are gone. It only LOOKS like it does not delete them if ids get recycled --
			// see rebuild/seed.
			data(TR) { modify("nets") { set(nbt { }) } }

			// --- Assign one network per connected component ---
			function(Flood.assignNext)

			// --- Total capacity and carried contents per network ---
			execute {
				asTarget(nodes)
				run { function(Absorb.accumulateNode) }
			}

			// --- Fold the carried per-medium breakdown back in ---
			// After the totals, because a network has to exist before anything can be added to it,
			// and before the clamp, because the clamp trims the breakdown as well as the total and
			// there is nothing to trim until this has run.
			execute {
				asTarget(trMarkers(TrTag.NODE) { scores { score(Obj.TR_CARRY, rangeOrIntStart(1)) } })
				run { function(Absorb.absorb) }
			}

			// A network can inherit more than its new capacity holds if a tank was removed.
			execute {
				asTarget(roots)
				run { function(Clamp.clamp) }
			}

			// Carried values are consumed; clear them so a later rebuild cannot double-count.
			scoreboard.players.set(nodes, Obj.TR_CARRY, 0)
			scoreboard.players.set(nodes, Obj.TR_OLD, 0)
			for (scratch in listOf("arg", "snapq", "snapi", "absq")) data(TR) { remove(scratch) }
		}

		registerFlood()
		registerSnapshot()
		registerAbsorb()
		registerClamp()
	}
}
