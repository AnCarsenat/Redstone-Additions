package ra.modules.automation.conveyor

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeReturnRun
import ra.core.macroCall
import ra.core.raw
import ra.modules.automation.conveyor.Conveyor.readSpeed
import ra.modules.interactive.Fan
import ra.modules.interactive.FanPush
import ra.modules.interactive.FanPush.playerGuards

/**
 * What an active [Conveyor] does, every tick: a push for each thing standing on it. The same
 * technique as the Fan's (see `ra/modules/interactive/FanPush.kt`), along the ground only.
 *
 * WHAT STANDS ON IT. The box is the half block above the slab and the half above that
 * (`align xyz positioned ~ ~0.5 ~`, `dx=0,dy=0,dz=0`), which every hitbox resting on the slab
 * meets. The pack's own entities (tagged `ra`), armour stands, markers, displays, interactions
 * and what hangs on walls are left alone ([FanPush.MOVABLE], the Fan's [Fan.UNMOVED] tag).
 *
 * MOBS AND ITEMS get their `Motion` along the push axis raised to the Conveyor's speed (lowered,
 * for a negative axis), never cut, and the other axes left alone: something already moving faster
 * that way keeps its speed, and a mob carried along still falls and still walks sideways. Two
 * Conveyors both reaching an item on their border give it the same speed, not twice.
 *
 * PLAYERS cannot have `Motion` written, so they are moved by a relative `tp` of one small step
 * every tick (0.05 × speed blocks): 20 small steps a second, which looks smooth, and a relative
 * teleport keeps the player's own velocity, so walking and jumping carry on through it. A player
 * is pushed only by the Conveyor under their feet (the block their position is in, 0.1 below the
 * feet), so one standing across two Conveyors is not pushed twice, and not at all in the air.
 * Each step first checks the cells the player's front edge would enter, at the feet (a passable
 * block or a bottom slab, such as the next Conveyor) and at both head heights: a Conveyor never
 * pushes anyone into a wall. Spectators, riders, elytra gliders and creative players who are
 * flying are left alone, and a player a Fan already stepped this tick is not stepped again
 * ([FanPush.PUSHED]).
 *
 * CHAINS. A Conveyor that pushed something tags the one in front of it [Conveyor.ACTIVE], so the
 * next one carries on at once instead of at its next poll.
 */
internal object ConveyorPush {
	private val T = Obj.TEMP

	/** The selector filter for what a Conveyor may carry: the Fan's, nothing tagged `ra`. */
	val MOVABLE get() = FanPush.MOVABLE

	/** On the Conveyor running, for the player's "is this the one under me" test. */
	private const val SELF = "ra.auto.cv_self"

	private const val STORE = "ra_automation:conveyor push"

	val run = Conveyor.block.fn("run")
	private val sweep = Conveyor.block.fn("sweep")
	private val pushMotion = Conveyor.block.fn("push_motion")
	private val pushPlayer = Conveyor.block.fn("push_player")
	private val motionX = Conveyor.block.fn("motion_x")
	private val motionZ = Conveyor.block.fn("motion_z")

	/**
	 * One marker facing (`ra.facing`), and the way it pushes: the opposite one, away from the
	 * player who placed it. [axis] 0 is x, 2 is z.
	 */
	private class Dir(val facing: Int, val name: String, val axis: Int, val sign: Int) {
		val fn = Conveyor.block.fn("dir/$name")
		private val a = "xyz"[axis]

		fun Function.body() {
			val n = (0..2).map { if (it == axis) sign else 0 }
			raw("data modify storage $STORE set value {dx:0,dz:0,cx:0,cz:0,nx:${n[0]},nz:${n[2]}}")
			if (sign < 0) for (s in listOf("#cv.v", "#cv.reach")) raw("scoreboard players operation $s $T *= #cv.neg $T")
			raw("execute store result storage $STORE.d$a double 0.001 run scoreboard players get #cv.v $T")
			raw("execute store result storage $STORE.c$a double 0.001 run scoreboard players get #cv.reach $T")
			raw("scoreboard players set #cv.axis $T $axis")
		}
	}

	private val dirs = listOf(Dir(2, "south", 2, 1), Dir(3, "north", 2, -1), Dir(4, "east", 0, 1), Dir(5, "west", 0, -1))

	fun register(ra: Ra) = with(ra) {
		pack.define(run) { run() }
		for (d in dirs) pack.define(d.fn) { with(d) { body() } }
		pack.define(sweep) { sweep() }
		pack.define(pushMotion) {
			executeReturnRun("if score #cv.axis $T matches 0", motionX)
			raw("function ${motionZ.asId()}")
		}
		pack.define(motionX) { motionOn(0) }
		pack.define(motionZ) { motionOn(2) }
		pack.define(pushPlayer) { pushPlayer() }
	}

	/** An active Conveyor. As its marker, at its block. */
	private fun Function.run() {
		raw("execute align xyz positioned ~ ~0.5 ~ unless entity @e[$MOVABLE,dx=0,dy=0,dz=0,limit=1] run return 0")
		readSpeed()
		// Thousandths of a block: v per tick, for Motion and as the players' step; the step plus a
		// player's half-width for the wall check.
		raw("scoreboard players set #cv.k $T 50")
		raw("scoreboard players set #cv.neg $T -1")
		raw("scoreboard players operation #cv.v $T = #prop $T")
		raw("scoreboard players operation #cv.v $T *= #cv.k $T")
		raw("scoreboard players operation #cv.reach $T = #cv.v $T")
		raw("scoreboard players add #cv.reach $T 300")
		for (d in dirs) raw("execute if score @s ${Obj.FACING} matches ${d.facing} run function ${d.fn.asId()}")
		raw("execute unless data storage $STORE.dx run return 0")
		raw("tag @s add $SELF")
		raw("${macroCall(sweep)} with storage $STORE")
		raw("tag @s remove $SELF")
		raw("data remove storage $STORE")
	}

	/** Macro: everything on it, then the next Conveyor. As the marker, at the Conveyor. */
	private fun Function.sweep() {
		val box = "align xyz positioned ~ ~0.5 ~"
		raw("execute $box as @e[type=!minecraft:player,$MOVABLE,dx=0,dy=0,dz=0] run function ${pushMotion.asId()}")
		raw("execute $box as @a[gamemode=!spectator,dx=0,dy=0,dz=0] at @s positioned ~ ~-0.1 ~ align xyz " +
			"if entity @e[type=minecraft:marker,tag=$SELF,dx=0,dy=0,dz=0,limit=1] at @s run ${macroCall(pushPlayer)} with storage $STORE")
		raw("\$execute positioned ~\$(nx) ~ ~\$(nz) as @e[type=minecraft:marker,tag=${Conveyor.block.tag},distance=..0.5,limit=1] run tag @s add ${Conveyor.ACTIVE}")
	}

	/** Raise (or lower) Motion[i] to the Conveyor's speed. As the entity. */
	private fun Function.motionOn(i: Int) {
		raw("execute store result score #m $T run data get entity @s Motion[$i] 1000")
		raw("execute if score #cv.v $T matches 1.. run scoreboard players operation #m $T > #cv.v $T")
		raw("execute if score #cv.v $T matches ..-1 run scoreboard players operation #m $T < #cv.v $T")
		raw("execute store result entity @s Motion[$i] double 0.001 run scoreboard players get #m $T")
	}

	/** Macro: one step for a player. As the player, at them. */
	private fun Function.pushPlayer() {
		playerGuards()
		val passable = "#ra_lib:ray_passable"
		raw("\$execute positioned ~\$(cx) ~ ~\$(cz) unless block ~ ~ ~ $passable unless block ~ ~ ~ #minecraft:slabs[type=bottom] run return 0")
		raw("\$execute positioned ~\$(cx) ~1 ~\$(cz) unless block ~ ~ ~ $passable run return 0")
		raw("\$execute positioned ~\$(cx) ~1.75 ~\$(cz) unless block ~ ~ ~ $passable run return 0")
		raw("scoreboard players operation @s ${FanPush.PUSHED} = ${FanPush.NOW} $T")
		raw("\$tp @s ~\$(dx) ~ ~\$(dz)")
	}
}
