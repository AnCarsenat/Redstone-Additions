package ra.modules.farming.tree

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.features.tags.itemTag
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * One second of a [TreeFarm], from the network machines' clock. As the marker, at the block.
 *
 * 1. Redstone locks it, as it does a hopper.
 * 2. No tree on the way: the first of its four sides that is air over dirt (or grass, podzol,
 *    moss...) gets the first sapling of [TreeKinds] the container above holds, and the farm keeps
 *    that side and that kind in `data.data`.
 * 3. A sapling there: it grows on its own. With `bone_meal` on and bone meal above, one bone meal a
 *    second is used on it, as a player would: 45% of the time the sapling advances a stage, and at
 *    its last stage it grows (`place feature`, with the sapling put back when there is no room).
 * 4. The tree's log there: the tree is felled ([fell]) into the container below.
 * 5. Anything else there (the sapling broken, the spot built over): the spot is forgotten.
 *
 * FELLING. Only what grew from that spot: starting from the log on it, logs of that one wood,
 * reached from a log beside or above it (the 17 neighbours level with it or one up, so a trunk's
 * diagonal steps are followed and nothing below its base is), then that tree's leaves that are not
 * `persistent` (placed by hand) reached through its logs and leaves, each only when a felled log is
 * within its `distance` state (how far its nearest log is): a touching tree of the same kind,
 * whose leaves are nearer its own logs, keeps them. Every block must also lie within
 * [REACH] of a point [CENTRE_UP] above the spot, and at most [MAX_LOGS] logs and [MAX_LEAVES]
 * leaves are taken. Each block's drops (as if broken with an axe) go into the container below while
 * it has three free slots, and drop on the spot when it does not: nothing is destroyed.
 *
 * Every macro argument is a coordinate this pack read off the marker.
 */
internal object TreeFarmRun {
	private fun fn(path: String) = TreeFarm.block.fn(path)

	val run = fn("run")
	private val plant = fn("plant")
	private val tend = fn("tend")
	private val tendHere = fn("tend_here")
	private val fellStart = fn("fell")
	private val mine = fn("mine")

	private fun tendKind(k: TreeKind) = fn("tend/${k.name}")
	private fun grow(k: TreeKind) = fn("grow/${k.name}")
	private fun growTree(k: TreeKind) = fn("grow_tree/${k.name}")
	private fun fellLog(k: TreeKind) = fn("fell_log/${k.name}")
	private fun fellLeaf(k: TreeKind) = fn("fell_leaf/${k.name}")
	private fun tryLeaf(k: TreeKind) = fn("try_leaf/${k.name}")
	private fun leavesFrom(k: TreeKind) = fn("leaves_from/${k.name}")
	/** `ra_lib:inventory/remove` of one [id], at the container: 1 when one was taken. */
	private fun take(id: String) = "${RaLib.Inventory.remove.asId()} {id:\"$id\",count:1}"
	private val plantHere = fn("plant_here")

	const val MAX_LOGS = 96
	const val MAX_LEAVES = 384
	const val REACH = 12
	const val CENTRE_UP = 8

	private const val T = Obj.TEMP
	private const val S = "storage ra:farming tf"
	private const val ANCHOR = "ra.tf.anchor"
	/** On a marker where a felled log stood, for one fell. */
	private const val LOG_MARK = "ra.tf.log"
	/** Every felled log lies within this of the spot (REACH around a point CENTRE_UP above it). */
	private const val MARK_REACH = REACH + CENTRE_UP + 1
	private const val ANCHOR_NEAR = "@e[type=minecraft:marker,tag=$ANCHOR,distance=..$REACH,limit=1]"
	private const val SAPLINGS_TAG = "#ra_farming:tree_farm/saplings"

	/** North, east, south, west: the spot `data.data.side` names. */
	private val SIDES = listOf("~ ~ ~-1", "~1 ~ ~", "~ ~ ~1", "~-1 ~ ~")

	private val LOG_STEPS = buildList {
		for (dy in 0..1) for (dx in -1..1) for (dz in -1..1) if (dx != 0 || dy != 0 || dz != 0) add(listOf(dx, dy, dz).joinToString(" ") { if (it == 0) "~" else "~$it" })
	}
	private val LEAF_STEPS = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~")

	private fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""
	private fun leavesTag(k: TreeKind) = "#ra_farming:tree_farm/${k.name}_leaves"

	fun Ra.registerTreeFarmRun() {
		pack.itemTag("tree_farm/saplings", Ns.FARMING.id) { for (k in TreeKinds) add(k.sapling) }
		for (k in TreeKinds) pack.blockTag("tree_farm/${k.name}_leaves", Ns.FARMING.id) { for (l in k.leaves) add(l) }

		pack.define(run) {
			raw("execute if function ${RaLib.Redstone.any.asId()} run return run ${state("stopped by redstone")}")
			raw("execute if data entity @s data.data.side run return run function ${tend.asId()}")
			raw("execute unless items block ~ ~1 ~ container.* $SAPLINGS_TAG run return run ${state("needs saplings above")}")
			raw("function ${plant.asId()}")
		}

		// As the marker, at it: a sapling on the first free side.
		pack.define(plant) {
			raw("scoreboard players set #tf.side $T -1")
			for ((i, at) in SIDES.withIndex()) {
				raw("execute if score #tf.side $T matches -1 positioned $at if block ~ ~ ~ #minecraft:air if block ~ ~-1 ~ #minecraft:dirt run scoreboard players set #tf.side $T $i")
			}
			raw("execute if score #tf.side $T matches -1 run return run ${state("no free dirt beside it")}")
			raw("scoreboard players set #tf.kind $T -1")
			for (k in TreeKinds) {
				raw("scoreboard players set #tf.took $T 0")
				raw("execute if score #tf.kind $T matches -1 if items block ~ ~1 ~ container.* ${k.sapling} positioned ~ ~1 ~ store result score #tf.took $T run function ${take(k.sapling)}")
				raw("execute if score #tf.took $T matches 1 run scoreboard players set #tf.kind $T ${k.index}")
			}
			raw("execute if score #tf.kind $T matches -1 run return run ${state("needs saplings above")}")
			raw("execute store result entity @s data.data.side int 1 run scoreboard players get #tf.side $T")
			raw("execute store result entity @s data.data.kind int 1 run scoreboard players get #tf.kind $T")
			atSpot(plantHere, returns = false)
			raw(state("growing"))
		}
		// At the spot: the kind's sapling.
		pack.define(plantHere) {
			for (k in TreeKinds) raw("execute if score #tf.kind $T matches ${k.index} run setblock ~ ~ ~ ${k.sapling}")
			raw("playsound minecraft:block.grass.place block ${soundListenersSel(12)} ~ ~ ~ 0.6 1.0")
		}
		// As the marker, at it, with a tree on the way.
		pack.define(tend) {
			raw("execute store result score #tf.side $T run data get entity @s data.data.side")
			raw("execute store result score #tf.kind $T run data get entity @s data.data.kind")
			atSpot(tendHere)
		}
		pack.define(tendHere) {
			for (k in TreeKinds) raw("execute if score #tf.kind $T matches ${k.index} run return run function ${tendKind(k).asId()}")
			raw("function ${forget.asId()}")
		}
		pack.define(forget) {
			raw("data remove entity @s data.data.side")
			raw("data remove entity @s data.data.kind")
			raw(state("replanting"))
		}

		for (k in TreeKinds) {
			// At the spot.
			pack.define(tendKind(k)) {
				raw("execute if block ~ ~ ~ ${k.log} run return run function ${fellStart.asId()}")
				raw("execute if block ~ ~ ~ ${k.sapling} run return run function ${grow(k).asId()}")
				raw("function ${forget.asId()}")
			}
			// At the sapling: bone meal, when it may and has some.
			pack.define(grow(k)) {
				raw(state("growing"))
				raw("execute unless data entity @s data.properties{bone_meal:1b} run return 0")
				raw("execute at @s unless items block ~ ~1 ~ container.* minecraft:bone_meal run return 0")
				raw("scoreboard players set #tf.took $T 0")
				raw("execute at @s positioned ~ ~1 ~ store result score #tf.took $T run function ${take("minecraft:bone_meal")}")
				raw("execute unless score #tf.took $T matches 1 run return 0")
				raw(state("growing with bone meal"))
				raw("particle minecraft:happy_villager ~ ~0.4 ~ 0.3 0.3 0.3 0 6 normal ${particleViewersSel()}")
				raw("playsound minecraft:item.bone_meal.use block ${soundListenersSel(12)} ~ ~ ~ 0.6 1.0")
				raw("execute store result score #tf.roll $T run random value 1..100")
				raw("execute if score #tf.roll $T matches 46.. run return 0")
				raw("execute if block ~ ~ ~ ${k.sapling}[stage=0] run return run setblock ~ ~ ~ ${k.sapling}[stage=1]")
				raw("function ${growTree(k).asId()}")
			}
			// At the sapling at its last stage: the tree, or the sapling back when it has no room.
			pack.define(growTree(k)) {
				raw("setblock ~ ~ ~ minecraft:air")
				raw("scoreboard players set #tf.ok $T 0")
				if (k.features.size == 1) {
					raw("execute store success score #tf.ok $T run place feature ${k.features[0]} ~ ~ ~")
				} else {
					raw("execute store result score #tf.f $T run random value 0..${k.features.size - 1}")
					// A repeated id is one range of rolls (they are written next to each other).
					for (f in k.features.distinct()) {
						val at = k.features.indices.filter { k.features[it] == f }
						raw("execute if score #tf.f $T matches ${at.first()}..${at.last()} store success score #tf.ok $T run place feature $f ~ ~ ~")
					}
				}
				raw("execute if score #tf.ok $T matches 0 run return run setblock ~ ~ ~ ${k.sapling}[stage=1]")
			}

			// At a log of the tree.
			pack.define(fellLog(k)) {
				raw("scoreboard players remove #tf.logs $T 1")
				raw("function ${mine.asId()} with $S")
				raw("setblock ~ ~ ~ minecraft:air")
				// Where this tree's logs stood, for the leaves. Killed by `fell` before the function
				// chain ends, so it never lives to the next tick: no `ra` tag, no purge stamp.
				raw("summon minecraft:marker ~ ~ ~ {Tags:[\"$LOG_MARK\"]}")
				for (d in LOG_STEPS) {
					raw("execute if score #tf.logs $T matches 1.. positioned $d if block ~ ~ ~ ${k.log} if entity $ANCHOR_NEAR run function ${fellLog(k).asId()}")
				}
			}
			// At a felled log: its six neighbours, for leaves.
			pack.define(leavesFrom(k)) {
				for (d in LEAF_STEPS) raw("execute positioned $d run function ${tryLeaf(k).asId()}")
			}
			// At a block: a leaf of this tree is felled. A leaf's `distance` is how far its nearest log
			// is; it is this tree's only when one of the felled logs is that close. A touching tree's
			// leaves are nearer their own logs and stay.
			pack.define(tryLeaf(k)) {
				raw("execute unless score #tf.leaves $T matches 1.. run return 0")
				for (dist in 1..7) {
					raw("execute if block ~ ~ ~ ${leavesTag(k)}[persistent=false,distance=$dist] if entity @e[type=minecraft:marker,tag=$LOG_MARK,distance=..$dist.01,limit=1] run return run function ${fellLeaf(k).asId()}")
				}
			}
			// At a leaf of the tree.
			pack.define(fellLeaf(k)) {
				raw("scoreboard players remove #tf.leaves $T 1")
				raw("function ${mine.asId()} with $S")
				raw("setblock ~ ~ ~ minecraft:air")
				for (d in LEAF_STEPS) raw("execute positioned $d run function ${tryLeaf(k).asId()}")
			}
		}

		// As the marker, at the spot holding the tree's first log.
		pack.define(fellStart) {
			raw("execute at @s unless block ~ ~-1 ~ #ra_lib:containers run return run ${state("no container below")}")
			// The output's block coordinates (data get floors), for `loot insert`.
			raw("execute store result $S.x int 1 run data get entity @s Pos[0]")
			raw("execute store result score #tf.y $T run data get entity @s Pos[1]")
			raw("execute store result $S.y int 1 run scoreboard players remove #tf.y $T 1")
			raw("execute store result $S.z int 1 run data get entity @s Pos[2]")
			// Three free slots: the most new stacks one block's drops can need (sapling, stick, apple).
			raw("execute at @s positioned ~ ~-1 ~ run function ${RaLib.Inventory.containerSize.asId()}")
			raw("scoreboard players operation #tf.lim $T = #inv_max $T")
			raw("scoreboard players remove #tf.lim $T 2")
			raw("summon minecraft:marker ~ ~$CENTRE_UP ~ {Tags:[\"ra\",\"$ANCHOR\"]}")
			raw("scoreboard players set #tf.logs $T $MAX_LOGS")
			raw("scoreboard players set #tf.leaves $T $MAX_LEAVES")
			for (k in TreeKinds) raw("execute if score #tf.kind $T matches ${k.index} run function ${fellLog(k).asId()}")
			// The leaves, only after every log is down, from the logs' own places.
			for (k in TreeKinds) raw("execute if score #tf.kind $T matches ${k.index} as @e[type=minecraft:marker,tag=$LOG_MARK,distance=..$MARK_REACH] at @s run function ${leavesFrom(k).asId()}")
			raw("kill @e[type=minecraft:marker,tag=$LOG_MARK,distance=..$MARK_REACH]")
			raw("kill @e[type=minecraft:marker,tag=$ANCHOR,distance=..${CENTRE_UP + 1}]")
			raw("data remove $S")
			raw("data remove entity @s data.data.side")
			raw("data remove entity @s data.data.kind")
			raw("execute store result score #tf.n $T run data get entity @s data.status.felled")
			raw("execute store result entity @s data.status.felled int 1 run scoreboard players add #tf.n $T 1")
			raw(state("felled a tree"))
			raw("particle minecraft:poof ~ ~1 ~ 0.6 1 0.6 0.02 16 normal ${particleViewersSel()}")
			raw("playsound minecraft:block.wood.break block ${soundListenersSel(16)} ~ ~ ~ 1 0.8")
		}

		// {x, y, z}: the output container. At the block being felled: its drops, then it is cleared
		// by the caller. Into the container while three slots are free, on the spot otherwise.
		pack.define(mine) {
			raw("scoreboard players set #tf.used $T 0")
			raw("\$execute store result score #tf.used $T run data get block \$(x) \$(y) \$(z) Items")
			raw("\$execute if score #tf.used $T < #tf.lim $T run return run loot insert \$(x) \$(y) \$(z) mine ~ ~ ~ minecraft:iron_axe")
			raw("loot spawn ~ ~ ~ mine ~ ~ ~ minecraft:iron_axe")
		}
	}

	private val forget = fn("forget")

	/** One line per side: `fn` positioned at the spot `#tf.side` names. */
	private fun Function.atSpot(fn: FunctionArgument, returns: Boolean = true) {
		val ret = if (returns) "return run " else ""
		for ((i, at) in SIDES.withIndex()) raw("execute if score #tf.side $T matches $i positioned $at run ${ret}function ${fn.asId()}")
	}
}
