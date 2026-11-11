package ra.modules.interactive

import io.github.ayfri.kore.functions.Function
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.macroCall
import ra.core.raw

/**
 * What a powered [Fan] does, every tick: the box in front of it, then a push for each thing in it.
 *
 * MOBS AND ITEMS get their `Motion` along the Fan's axis raised to the Fan's speed (lowered, for a
 * Fan facing a negative axis), never cut: something already moving faster that way keeps its speed,
 * and the other two axes are left alone, so a pushed mob still falls and still walks sideways.
 *
 * PLAYERS cannot have `Motion` written, and 26.3 has no command that gives a player velocity. The
 * routes that do move a player for real all need something the pack cannot fire on demand: the
 * `apply_impulse` and `explode` enchantment effects run only from an enchanted item the player
 * wears or holds (or a projectile fired from one, on a hit), which would mean putting an item into
 * the player's equipment; a wind charge is a single burst that also flips levers and doors;
 * `/damage` knockback hurts and has a fixed size; geysers only push up. So:
 *  - along the ground or down, a relative `tp` by one small step every tick (0.05 × strength
 *    blocks, the speed the mobs get): 20 steps a second instead of 10 twice the size, which is
 *    what makes it look smooth. A relative teleport keeps the player's own velocity, so walking,
 *    jumping and falling carry on through it. Each step first checks the block the
 *    player's front edge would enter, at the feet and the head, against `#ra_lib:ray_passable`
 *    (air, water, plants, rails, redstone...): a Fan never pushes anyone into a wall.
 *  - up, Levitation of the Fan's strength, refreshed every tick, and taken away within three
 *    ticks of leaving the column ([liftEnd]) -- a step up would be undone by gravity in between.
 * Spectators, riders, elytra gliders and creative players who are flying are left alone, and a
 * player a Conveyor already stepped this tick is not stepped again ([PUSHED]). The pack's own
 * entities (tagged `ra`) are never moved ([MOVABLE]).
 */
internal object FanPush {
	val T = Obj.TEMP

	/** On a player lifted by an upward Fan. */
	const val LIFTED = "ra.fan.lifted"

	/** The game time the lift ends unless a Fan refreshes it. */
	private const val LIFT_UNTIL = "ra.fan.lift"

	/**
	 * On a player: the game time a Fan or a Conveyor last stepped them. Whichever mover runs
	 * first in a tick moves the player; the other skips them, so two movers never fight.
	 */
	const val PUSHED = "ra.pushed"

	/** The game time this tick, read by the Fan's and the Conveyor's ticks while one is moving. */
	const val NOW = "#push.now"

	/**
	 * The selector filter for what a Fan or a Conveyor may move: nothing of the pack's own
	 * (every entity it summons carries `ra`, the old armour-stand block markers too), nothing of
	 * [Fan.UNMOVED].
	 */
	val MOVABLE = "type=!#${Ns.INTERACTIVE.id}:${Fan.UNMOVED},tag=!ra"

	/** The objectives, added by the module's load. */
	val objectives = listOf("$LIFT_UNTIL dummy", "$PUSHED dummy")

	/**
	 * The tests every mover makes before stepping a player, as the player: not riding, not
	 * gliding with an elytra, not flying in creative, not already stepped this tick.
	 */
	fun Function.playerGuards() {
		raw("execute if entity @s[gamemode=creative,nbt={abilities:{flying:1b}}] run return 0")
		raw("execute on vehicle run return 0")
		raw("execute if predicate {type:\"minecraft:entity_properties\",entity:\"this\",predicate:{flags:{is_fall_flying:true}}} run return 0")
		raw("execute if score @s $PUSHED = $NOW $T run return 0")
	}

	private const val STORE = "ra:interactive fan"

	val run = Fan.block.fn("run")
	private val sweep = Fan.block.fn("sweep")
	private val pushMotion = Fan.block.fn("push_motion")
	private val pushPlayer = Fan.block.fn("push_player")
	private val lift = Fan.block.fn("lift")
	val liftEnd = Fan.block.fn("lift_end")
	private val liftDrop = Fan.block.fn("lift_drop")
	private val motion = listOf("x", "y", "z").map { Fan.block.fn("motion_$it") }

	/**
	 * One facing, in `ra.facing` order: the [axis] it pushes along (0 x, 1 y, 2 z) and which way
	 * ([sign]). The box starts at the block in front for a positive facing, `range` blocks out for
	 * a negative one, so it always runs from its low corner.
	 */
	private class Dir(val name: String, val axis: Int, val sign: Int) {
		val fn = Fan.block.fn("dir/$name")
		private val a = "xyz"[axis]
		private fun v(i: Int, n: Double) = if (i == axis) n else 0.0

		fun Function.body() {
			val o = (0..2).map { if (it == axis && sign > 0) "1" else "0" }
			val q = (0..2).map { v(it, 0.6 * sign) }
			val p = (0..2).map { v(it, sign.toDouble()) }
			raw("data modify storage $STORE set value {ox:${o[0]},oy:${o[1]},oz:${o[2]},ex:0,ey:0,ez:0," +
				"qx:${q[0]},qy:${q[1]},qz:${q[2]},px:${p[0]},py:${p[1]},pz:${p[2]},ps:0,dx:0,dy:0,dz:0,cx:0,cy:0.5,cz:0,amp:0}")
			raw("execute store result storage $STORE.e$a int 1 run scoreboard players get #fan.r1 $T")
			if (sign < 0) raw("execute store result storage $STORE.o$a int 1 run scoreboard players get #fan.rn $T")
			raw("execute store result storage $STORE.ps double 0.001 run scoreboard players get #fan.puff $T")
			raw("execute store result storage $STORE.amp int 1 run scoreboard players get #fan.amp $T")
			if (sign < 0) for (s in listOf("#fan.v", "#fan.step", "#fan.reach")) raw("scoreboard players operation $s $T *= #fan.neg $T")
			raw("execute store result storage $STORE.d$a double 0.001 run scoreboard players get #fan.step $T")
			// Down: the block under the feet. Sideways: the block the front edge enters.
			if (axis == 1) raw("execute store result storage $STORE.cy double 0.001 run scoreboard players get #fan.step $T")
			else raw("execute store result storage $STORE.c$a double 0.001 run scoreboard players get #fan.reach $T")
			raw("scoreboard players set #fan.axis $T $axis")
			raw("scoreboard players set #fan.up $T ${if (axis == 1 && sign > 0) 1 else 0}")
		}
	}

	private val dirs = listOf(Dir("down", 1, -1), Dir("up", 1, 1), Dir("north", 2, -1), Dir("south", 2, 1), Dir("west", 0, -1), Dir("east", 0, 1))

	fun register(ra: Ra) = with(ra) {
		pack.define(run) { run() }
		for (d in dirs) pack.define(d.fn) { with(d) { body() } }
		pack.define(sweep) { sweep() }
		pack.define(pushMotion) {
			for (i in 0..1) executeReturnRun("if score #fan.axis $T matches $i", motion[i])
			raw("function ${motion[2].asId()}")
		}
		motion.forEachIndexed { i, fn -> pack.define(fn) { motionOn(i) } }
		pack.define(pushPlayer) { pushPlayer() }
		pack.define(lift) { lift() }
		pack.define(liftEnd) {
			raw("execute store result score #now $T run time query gametime")
			executeRun("as @a[tag=$LIFTED] if score @s $LIFT_UNTIL < #now $T", liftDrop)
		}
		pack.define(liftDrop) {
			raw("effect clear @s minecraft:levitation")
			raw("tag @s remove $LIFTED")
		}
	}

	/** A powered Fan. As its marker, at its block. */
	private fun Function.run() {
		readCappedProperty("range", default = Fan.DEFAULT_RANGE, min = 1, adminMax = Fan.ADMIN_MAX_RANGE)
		raw("scoreboard players operation #fan.r1 $T = #prop $T")
		raw("scoreboard players remove #fan.r1 $T 1")
		raw("scoreboard players set #fan.neg $T -1")
		raw("scoreboard players operation #fan.rn $T = #prop $T")
		raw("scoreboard players operation #fan.rn $T *= #fan.neg $T")
		readCappedProperty("strength", default = Fan.DEFAULT_STRENGTH, min = 1, adminMax = Fan.ADMIN_MAX_STRENGTH)
		// Thousandths of a block: v per tick, for Motion and as the players' step; the step plus a
		// player's half-width for the wall check; the puff's particle speed, twice v.
		raw("scoreboard players set #fan.k $T 50")
		raw("scoreboard players operation #fan.v $T = #prop $T")
		raw("scoreboard players operation #fan.v $T *= #fan.k $T")
		raw("scoreboard players operation #fan.step $T = #fan.v $T")
		raw("scoreboard players operation #fan.puff $T = #fan.v $T")
		raw("scoreboard players operation #fan.puff $T += #fan.v $T")
		raw("scoreboard players operation #fan.reach $T = #fan.step $T")
		raw("scoreboard players add #fan.reach $T 350")
		raw("scoreboard players operation #fan.amp $T = #prop $T")
		raw("scoreboard players remove #fan.amp $T 1")
		dirs.forEachIndexed { f, d -> executeRun("if score @s ${Obj.FACING} matches $f", d.fn) }
		raw("${macroCall(sweep)} with storage $STORE")
	}

	/** Macro: the box in front, then the wind. As the marker, at the Fan. */
	private fun Function.sweep() {
		val box = "align xyz positioned ~\$(ox) ~\$(oy) ~\$(oz)"
		val extent = "dx=\$(ex),dy=\$(ey),dz=\$(ez)"
		raw("\$execute $box as @e[type=!minecraft:player,$MOVABLE,$extent] run ${macroCall(pushMotion)}")
		raw("\$execute $box as @a[gamemode=!spectator,$extent] at @s run ${macroCall(pushPlayer)} with storage $STORE")
		// The puff every other tick, as before the push went every tick.
		raw("\$execute if score ${Fan.PHASE} $T matches 0 run particle minecraft:cloud ~\$(qx) ~\$(qy) ~\$(qz) \$(px) \$(py) \$(pz) \$(ps) 0 normal ${particleViewersSel(32)}")
	}

	/** Raise (or lower) Motion[i] to the Fan's speed. As the entity. */
	private fun Function.motionOn(i: Int) {
		raw("execute store result score #m $T run data get entity @s Motion[$i] 1000")
		raw("execute if score #fan.v $T matches 1.. run scoreboard players operation #m $T > #fan.v $T")
		raw("execute if score #fan.v $T matches ..-1 run scoreboard players operation #m $T < #fan.v $T")
		raw("execute store result entity @s Motion[$i] double 0.001 run scoreboard players get #m $T")
	}

	/** Macro: one step for a player, or a lift. As the player, at them. */
	private fun Function.pushPlayer() {
		playerGuards()
		executeReturnRun("if score #fan.up $T matches 1", lift, "with storage $STORE")
		val passable = "#ra_lib:ray_passable"
		raw("\$execute positioned ~\$(cx) ~\$(cy) ~\$(cz) unless block ~ ~ ~ $passable run return 0")
		raw("\$execute positioned ~\$(cx) ~\$(cy) ~\$(cz) unless block ~ ~1 ~ $passable run return 0")
		raw("scoreboard players operation @s $PUSHED = $NOW $T")
		raw("\$tp @s ~\$(dx) ~\$(dy) ~\$(dz)")
	}

	/** Macro `{amp}`: Levitation, and the time it ends unless refreshed. As the player. */
	private fun Function.lift() {
		raw("\$effect give @s minecraft:levitation 1 \$(amp) true")
		raw("tag @s add $LIFTED")
		raw("execute store result score @s $LIFT_UNTIL run time query gametime")
		raw("scoreboard players add @s $LIFT_UNTIL 3")
	}
}
