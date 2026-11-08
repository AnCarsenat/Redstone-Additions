package ra.lib.transport

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtByte
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.dummyObjectives
import ra.lib.fake
import ra.lib.lib
import ra.lib.transport.net.Net.registerNet
import ra.lib.transport.rebuild.Rebuild
import ra.lib.transport.rebuild.Rebuild.registerRebuild

/**
 * `ra_lib:transport/...`: the shared network engine for fluids, gases, items and electricity.
 *
 * The engine groups adjacent transport nodes into networks and keeps the network contents in one
 * place, instead of every node holding a buffer and shoving it at its neighbours each tick. That
 * removes the whole class of order-dependent propagation bugs, and makes a pipe run cost nothing
 * per tick -- only the nodes that actually source or sink anything do work.
 *
 * Hot numbers live in scoreboards; only the medium name, which changes rarely, lives in storage as
 * a readable string.
 */
object Transport {
	val init = lib("transport/init")
	val pendingTick = lib("transport/pending_tick")

	/** Ticks between retries of networks held back for reaching out of view. */
	private const val PENDING_RETRY = 100

	/** The wires node marker update_connection_status counts. */
	private const val WIRES_NODE = "ra.wires.node"

	fun Ra.registerTransport() {
		pack.define(init) { init() }

		// `tick`: rebuild networks when the topology changed. Called once per tick, before any
		// module moves contents around.
		//
		// Debounced: a rebuild runs at most once every 5 ticks, so a player laying down a line of
		// pipes does not trigger one flood fill per block placed.
		pack.define(RaLib.Transport.tick) {
			val cooldown = fake("#rebuild_cd")
			scoreboard.players.remove(cooldown, Obj.TR_TMP, 1)

			// A network held back because it reached out of view (rebuild/scope) is retried now
			// and then, so a split or a join made while it was half loaded settles once it is whole.
			// Slow on purpose: until then nothing is lost, its parts just share one pool.
			raw("execute if data storage ra:transport pending[0] run function ${pendingTick.asId()}")

			execute {
				unlessCondition { data(TR, "{dirty:1b}") }
				run { returnValue(0) }
			}
			execute {
				ifCondition { score(cooldown, Obj.TR_TMP) greaterThanOrEqualTo 1 }
				run { returnValue(0) }
			}

			scoreboard.players.set(cooldown, Obj.TR_TMP, 5)
			data(TR) { modify("dirty") { set(NbtByte(0)) } }

			function(Rebuild.run)
		}

		pack.define(pendingTick) {
			val cd = fake("#pending_cd")
			scoreboard.players.remove(cd, Obj.TR_TMP, 1)
			execute {
				ifCondition { score(cd, Obj.TR_TMP) greaterThanOrEqualTo 1 }
				run { returnValue(0) }
			}
			scoreboard.players.set(cd, Obj.TR_TMP, PENDING_RETRY)
			data(TR) { modify("dirty") { set(NbtByte(1)) } }
		}

		// `mark_dirty`: request a network rebuild. Call whenever a transport node is placed, broken
		// or has its connectivity changed. Rebuilds are debounced by transport/tick, so placing a
		// long pipe run in one tick still only costs one rebuild.
		pack.define(RaLib.Transport.markDirty) { data(TR) { modify("dirty") { set(NbtByte(1)) } } }

		// `update_connection_status`: count adjacent transport nodes and store a compact status
		// field. Context: as node marker, at node position.
		pack.define(RaLib.Transport.updateConnectionStatus) {
			val conn = fake("#conn")
			scoreboard.players.set(conn, Obj.TEMP, 0)

			for (offset in neighbours) execute {
				positioned(offset)
				ifCondition { entity(allEntities { type = EntityTypes.MARKER; tag = WIRES_NODE; distance = rangeOrDoubleEnd(0.75); limit = 1 }) }
				run { scoreboard.players.add(conn, Obj.TEMP, 1) }
			}

			raw("execute store result entity @s data.status.connections int 1 run scoreboard players get #conn ${Obj.TEMP}")
			// It also used to copy `data.properties.enabled` into `data.status.enabled`. No wires
			// block has an `enabled` property any more (see WrenchRegistry), and nothing read the copy.
		}

		registerNet()
		registerRebuild()
	}

	private fun io.github.ayfri.kore.functions.Function.init() {
		// Per node.
		dummyObjectives(Obj.TR_NET, Obj.TR_CAP, Obj.TR_CARRY, Obj.TR_CLASS)

		// The id of the network a node was the ROOT of, set only for the duration of a rebuild. It
		// is what lets a component that still contains an old root be given that network's number
		// back instead of a freshly minted one.
		dummyObjectives(Obj.TR_OLD)

		// Per network totals live in storage under nets.n<id>, not on a fake player. A scoreboard
		// gives one number per network; a compound gives a place to put the per-medium map that
		// multi-medium needs, without moving anything that is already there. The objectives
		// ra.tr.amount and ra.tr.capacity are gone -- in an upgraded world, `scoreboard objectives
		// remove` them to tidy up the leftovers.

		// Scratch.
		dummyObjectives(Obj.TR_TMP)

		scoreboard.players.set(fake("#next_net"), Obj.TR_TMP, 0)
		scoreboard.players.set(fake("#rebuild_cd"), Obj.TR_TMP, 0)

		// The id allocator, and the one number here that must NOT be reset on load: it is what
		// stops a retired network id from being handed to a different network later. `add 0`
		// creates it at 0 the first time and leaves it alone every time after.
		scoreboard.players.add(fake("#net_seq"), Obj.TR_TMP, 0)

		// Class ids. Networks only join nodes that share one.
		data(TR) {
			modify("classes") {
				set(nbt {
					this["fluid"] = 1
					this["item"] = 2
					this["electric"] = 3
				})
			}
		}

		// Seeded only when absent. This used to be overwritten on every load, which threw away live
		// state: a network's medium lives in `nets.n<id>.medium` and is only copied onto its root
		// node at the next rebuild, while the amount lives in a scoreboard that survives a reload
		// untouched.
		//
		// So a network filled since the last rebuild came back from /reload holding its contents
		// with no medium attached. net/offer refused anything when the amount was non-zero and the
		// medium did not match, and nothing matches an absent medium -- the network went
		// permanently deaf to its own pumps until a drain emptied it. Now that electric is on the
		// same engine, the same reload would have left a grid full of EU refusing everything its
		// generators produced.
		//
		// The scoreboards this pairs with are world state and persist by themselves; storage is
		// world state too, and the only reason it was being cleared was that nothing had noticed it
		// did not need to be.
		execute {
			unlessCondition { data(TR, "nets") }
			run { data(TR) { modify("nets") { set(nbt { }) } } }
		}

		// Always ask for a rebuild on load: markers may have been added or removed while the pack
		// was off, and a rebuild is cheap next to being wrong. `full` puts every loaded network in
		// scope for that one rebuild instead of only the ones named in `dirtyq`; a network reaching
		// out of the loaded area is still held back (rebuild/scope), and one wholly out of it is
		// not seen at all, so its record survives the load untouched.
		data(TR) { modify("dirty") { set(NbtByte(1)) } }
		data(TR) { modify("full") { set(NbtByte(1)) } }
	}

	/** The six face neighbours, in the order the beet functions tested them (+x -x +z -z +y -y). */
	internal val neighbours = listOf(
		vec3(1, 0, 0), vec3(-1, 0, 0), vec3(0, 0, 1), vec3(0, 0, -1), vec3(0, 1, 0), vec3(0, -1, 0),
	).map { it.relative }
}
