package ra.modules.wires.bridge

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line

object Bridge : PackPart {
	val balanced = ref(Ns.WIRES, "bridge/balanced")
	val consider = ref(Ns.WIRES, "bridge/consider")
	val give = ref(Ns.WIRES, "bridge/give")
	val readAmount = ref(Ns.WIRES, "bridge/read_amount")
	val readDst = ref(Ns.WIRES, "bridge/read_dst")
	val readSrc = ref(Ns.WIRES, "bridge/read_src")
	val scan = ref(Ns.WIRES, "bridge/scan")
	val take = ref(Ns.WIRES, "bridge/take")
	val tick = ref(Ns.WIRES, "bridge/tick")
	val transfer = ref(Ns.WIRES, "bridge/transfer")

	override fun Ra.register() {
		pack.define(Bridge.balanced) {
			// Internal: the two sides already hold the same amount. Context: as the bridge.

			raw("data modify entity @s data.status.bridge_state set value \"level\"")
			data(self()) { modify("data.status.moved", 0) }
		}

		pack.define(Bridge.consider) {
			// Internal: weigh one neighbouring network against the best seen so far.
			// Context: as that neighbour's node marker.
			//
			// Compared by HOW FULL, not by how much. A tank farm holding 2000 mL of its
			// 300000 is nearly empty; a three-pipe stub holding 3000 of its 5000 is more than
			// half full. Comparing raw amounts said the stub had more and called it level, so
			// a valve between them sat there while the tanks stayed empty — which is exactly
			// what a bank of tanks is for.
			//
			// Ten-thousandths rather than percent, because integer division throws away the
			// remainder and a percent is too coarse to tell 0.4% from 0.9% — which is the
			// difference between a tank farm filling and a tank farm looking full.

			function(RaLib.Transport.Net.read)
			scoreboard.players.add(literal("#br.found"), "ra.wires.tmp", 1)

			// fill = amount * 10000 / capacity, floored like the scoreboard `/=` it replaced. The
			// capacity guard stays: a zero divisor is an error for /compute, and a network with
			// no capacity reads as empty.
			scoreboard.players.set(literal("#br.fill"), "ra.wires.tmp", 0)
			computeScore(
				"#br.fill", "ra.wires.tmp",
				provider { floorDiv(mul(scoreOf("#net_amount", "ra.tr.tmp"), constant(10000)), scoreOf("#net_capacity", "ra.tr.tmp")) },
				chain = "if score #net_capacity ra.tr.tmp matches 1..",
			)

			raw("\$execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run data modify storage ra:wires bridge.src set value {dx:\$(dx),dy:\$(dy),dz:\$(dz)}")
			raw("execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi_net ra.wires.tmp = @s ra.tr.net")
			raw("execute if score #br.fill ra.wires.tmp > #br.hi ra.wires.tmp run scoreboard players operation #br.hi ra.wires.tmp = #br.fill ra.wires.tmp")

			raw("\$execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run data modify storage ra:wires bridge.dst set value {dx:\$(dx),dy:\$(dy),dz:\$(dz)}")
			raw("execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo_net ra.wires.tmp = @s ra.tr.net")
			raw("execute if score #br.fill ra.wires.tmp < #br.lo ra.wires.tmp run scoreboard players operation #br.lo ra.wires.tmp = #br.fill ra.wires.tmp")
		}

		pack.define(Bridge.give) {
			// Internal: credit the network in front. Context: as the node in front.
			// Carries the source's medium across, so the far side ends up holding what was
			// actually sent rather than adopting a name of its own.

			raw("\$function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"\$(medium)\"}")
		}

		pack.define(Bridge.readAmount) {
			// Internal: how much of one medium the network just read holds. Returns it.
			// Context: anywhere, immediately after ra_lib:transport/net/read.

			raw("\$execute store result score #br.amt ra.wires.tmp run data get storage ra:transport cur.amounts.\$(medium)")
			raw("return run scoreboard players get #br.amt ra.wires.tmp")
		}

		pack.define(Bridge.readDst) {
			// Internal: inspect the network in front of the bridge.
			// Context: as the node in front. Writes #br.dst / #br.dst_free / #br.dst_net and
			// #br.dst_amount, the latter being how much of the medium being MOVED is already
			// there.
			//
			// It no longer refuses a network holding something else. Networks hold several
			// media at once now, so "already holds something else" is not a refusal any more
			// -- the only thing that can turn a transfer away is having no room, and room is
			// the sum of everything in there against the capacity.

			function(RaLib.Transport.Net.read)

			computeScore("#br.dst_free", "ra.wires.tmp", provider { sub(scoreOf("#net_capacity", "ra.tr.tmp"), scoreOf("#net_amount", "ra.tr.tmp")) })
			raw("execute if score #br.dst_free ra.wires.tmp matches ..0 run return 0")

			// Compared per medium, so the half-the-gap convergence in bridge/transfer levels
			// the thing being moved rather than the totals. Two runs each holding 5000, one
			// all water and one all lava, are not level from a water valve's point of view.
			raw("data modify storage ra:wires amtq set value {}")
			data(Storage.WIRES) { modify("amtq.medium", Storage.WIRES, "bridge.medium") }
			executeRun("store result score #br.dst_amount ra.wires.tmp", Bridge.readAmount, "with storage ra:wires amtq")

			scoreboard.players.set(literal("#br.dst"), "ra.wires.tmp", 1)
			scoreboard.players.operation(literal("#br.dst_net"), "ra.wires.tmp", Operation.SET, self(), "ra.tr.net")
		}

		pack.define(Bridge.readSrc) {
			// Internal: inspect the network behind the bridge.
			// Context: as the node behind it. Writes #br.src / #br.src_amount / #br.src_net
			// and parks the medium being moved in storage ra:wires bridge.medium.

			function(RaLib.Transport.Net.read)

			raw("execute if score #net_amount ra.tr.tmp matches ..0 run return 0")

			// WHICH MEDIUM MOVES
			// A plain Valve or Breaker moves the network's primary -- whatever is at the
			// front of its media list. A Liquid Filter moves only the medium it is set to,
			// and is the reason this is a choice at all: it is the block that lets a mixed
			// pipe run be separated back out into single-medium runs.
			data(Storage.WIRES) { modify("bridge.medium", Storage.TRANSPORT, "cur.medium") }
			raw("execute if data storage ra:wires filter.medium run data modify storage ra:wires bridge.medium set from storage ra:wires filter.medium")

			// How much of THAT medium is here, not how much the network holds in total. A
			// filter set to lava on a run holding 9000 water and 1000 lava has 1000 to move.
			raw("data modify storage ra:wires amtq set value {}")
			data(Storage.WIRES) { modify("amtq.medium", Storage.WIRES, "bridge.medium") }
			executeRun("store result score #br.src_amount ra.wires.tmp", Bridge.readAmount, "with storage ra:wires amtq")

			raw("execute if score #br.src_amount ra.wires.tmp matches ..0 run return 0")

			scoreboard.players.set(literal("#br.src"), "ra.wires.tmp", 1)
			scoreboard.players.operation(literal("#br.src_net"), "ra.wires.tmp", Operation.SET, self(), "ra.tr.net")
		}

		pack.define(Bridge.scan) {
			// Internal: find the fullest and emptiest networks touching this block, and move
			// contents from one to the other. Context: as the bridge marker, at its block.
			//
			// Six neighbours, no facing. Whichever adjacent network is the FULLEST gives to
			// whichever is the emptiest — see bridge/consider for why fullness rather than
			// amount. A valve dropped into a pipe run works whichever way round it was placed,
			// and a bridge at a junction still does the sensible thing.

			// Fill, in ten-thousandths. -1 and 10001 sit outside the real range so the first
			// neighbour seen always beats both.
			scoreboard.players.set(literal("#br.hi"), "ra.wires.tmp", -1)
			scoreboard.players.set(literal("#br.lo"), "ra.wires.tmp", 10001)
			scoreboard.players.set(literal("#br.found"), "ra.wires.tmp", 0)
			data(Storage.WIRES) { remove("bridge") }

			executeRun("positioned ~1 ~ ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:1,dy:0,dz:0}")
			executeRun("positioned ~-1 ~ ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:-1,dy:0,dz:0}")
			executeRun("positioned ~ ~ ~1 as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:0,dy:0,dz:1}")
			executeRun("positioned ~ ~ ~-1 as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:0,dy:0,dz:-1}")
			executeRun("positioned ~ ~1 ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:0,dy:1,dz:0}")
			executeRun("positioned ~ ~-1 ~ as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1]", Bridge.consider, "{dx:0,dy:-1,dz:0}")

			raw("execute if score #br.found ra.wires.tmp matches ..1 run return run data modify entity @s data.status.bridge_state set value \"needs_two_networks\"")

			// Both extremes landing on the same network means everything touching this block
			// is already one network -- pipes on both sides that meet somewhere behind it.
			// There is nothing to move, and reporting throughput would be a lie.
			raw("execute if score #br.hi_net ra.wires.tmp = #br.lo_net ra.wires.tmp run return run data modify entity @s data.status.bridge_state set value \"same_network\"")

			executeReturnRun("if score #br.hi ra.wires.tmp = #br.lo ra.wires.tmp", Bridge.balanced)

			raw("data modify storage ra:wires move set value {}")
			data(Storage.WIRES) { modify("move.sx", Storage.WIRES, "bridge.src.dx") }
			data(Storage.WIRES) { modify("move.sy", Storage.WIRES, "bridge.src.dy") }
			data(Storage.WIRES) { modify("move.sz", Storage.WIRES, "bridge.src.dz") }
			data(Storage.WIRES) { modify("move.dx", Storage.WIRES, "bridge.dst.dx") }
			data(Storage.WIRES) { modify("move.dy", Storage.WIRES, "bridge.dst.dy") }
			data(Storage.WIRES) { modify("move.dz", Storage.WIRES, "bridge.dst.dz") }
			function(Bridge.transfer, Storage.WIRES, "move")
			data(Storage.WIRES) { remove("move") }
		}

		pack.define(Bridge.take) {
			// Internal: debit the network behind. Context: as the node behind.
			// The amount was already clamped against both sides, so this cannot come up short.

			raw("\$function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"\$(medium)\"}")
		}

		pack.define(Bridge.tick) {
			// Even out the two networks this block sits between.
			// Context: as a bridge marker, at its block.
			//
			// WHAT A BRIDGE IS
			// A bridge belongs to neither network. That is the whole point: a node belongs to
			// exactly one network, so anything that joined would merge the two sides and there
			// would be nothing left to bridge. The Boiler has always worked this way.
			//
			// Because it is not a member, it also breaks connectivity by simply existing: a
			// pipe run with a valve in the middle is two networks, always, and the valve is
			// the only thing that moves anything between them.
			//
			// IT NO LONGER CARES WHICH WAY IT FACES
			// It used to bridge the block in front and the block behind, which meant a valve
			// placed on an east-west pipe run while the player happened to be facing south
			// pointed at two empty spaces and did nothing at all, with no way to tell from
			// looking at it. Now it looks at all six neighbours and finds the networks itself.

			// Redstone is the on switch. ra_lib:redstone/any is the cheap reader: it stops at
			// the first live side and never resolves a level, which is all that is wanted.
			line("execute unless function", RaLib.Redstone.any, "run return run data modify entity @s data.status.bridge_state set value \"unpowered\"")

			function(Bridge.scan)
		}

		pack.define(Bridge.transfer) {
			// Internal: move contents from the source side to the destination side.
			// Context: as the bridge marker, at its block. bridge/scan has already picked the
			// fullest adjacent network as the source and the emptiest as the destination.
			//
			// Both ends are inspected before anything moves, for the same reason the Boiler
			// does it: taking first and discovering afterwards that the far side is full or
			// holds something else would delete the difference. Nothing here half-succeeds.

			scoreboard.players.set(literal("#br.src"), "ra.wires.tmp", 0)
			scoreboard.players.set(literal("#br.dst"), "ra.wires.tmp", 0)
			data(Storage.WIRES) { remove("bridge") }

			// The rate stays in #prop (ra.temp): nothing between here and the clamp below calls
			// ra_lib:util/property again.
			function(RaLib.Util.property, nbt { this["name"] = "rate"; this["default"] = 1000; this["min"] = 1 })

			// A Liquid Filter carries filter_medium and moves only that. Resolved here rather
			// than in read_src because read_src runs `as` the neighbouring node, and the
			// property is on the bridge -- by then @s is somebody else.
			data(Storage.WIRES) { remove("filter") }
			raw("execute if data entity @s data.properties.filter_medium run data modify storage ra:wires filter.medium set from entity @s data.properties.filter_medium")

			raw("\$execute positioned ~\$(sx) ~\$(sy) ~\$(sz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ${Bridge.readSrc.asId()}")
			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ${Bridge.readDst.asId()}")

			raw("execute if score #br.dst ra.wires.tmp matches 0 run return run data modify entity @s data.status.bridge_state set value \"blocked\"")

			// move = min(rate, src_amount, dst_free, (src_amount - dst_amount) / 2)
			//
			// Never move more than HALF the difference between the two sides.
			//
			// Moving the whole difference does not level them, it swaps them: 3000 and 2000
			// becomes 2000 and 3000, and the next tick moves it all back. That is what a valve
			// reporting a steady "Moving: 1000 mL" between two sides that never changed was
			// actually doing — ping-ponging the same litre for ever.
			//
			// Half converges instead, and once the two are within one unit the halved gap
			// rounds to zero and nothing moves at all. The halving floors, as scoreboard `/=` did:
			// a negative gap stays negative and the `..0` check below blocks it.
			//
			// Stored into bridge.amount in the same command. A blocked transfer leaves it there
			// unused: nothing reads it on that path, and the next transfer clears `bridge` first.
			computeScore(
				"#br.move", "ra.wires.tmp",
				provider {
					min(
						scoreOf("#prop", "ra.temp"),
						scoreOf("#br.src_amount", "ra.wires.tmp"),
						scoreOf("#br.dst_free", "ra.wires.tmp"),
						floorDiv(sub(scoreOf("#br.src_amount", "ra.wires.tmp"), scoreOf("#br.dst_amount", "ra.wires.tmp")), constant(2)),
					)
				},
				chain = "store result storage ra:wires bridge.amount int 1",
			)

			raw("execute if score #br.move ra.wires.tmp matches ..0 run return run data modify entity @s data.status.bridge_state set value \"blocked\"")

			raw("\$execute positioned ~\$(sx) ~\$(sy) ~\$(sz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ${Bridge.take.asId()} with storage ra:wires bridge")
			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) as @e[type=marker,tag=ra.tr.node,distance=..0.75,limit=1] run function ${Bridge.give.asId()} with storage ra:wires bridge")

			raw("data modify entity @s data.status.bridge_state set value \"transferring\"")
			raw("execute store result entity @s data.status.moved int 1 run scoreboard players get #br.move ra.wires.tmp")
			data(self()) { modify("data.status.moving", Storage.WIRES, "bridge.medium") }
			data(Storage.WIRES) { remove("filter") }
		}
	}
}
