package ra.modules.interactive

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.particleViewersSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.macroCall
import ra.core.raw

/**
 * Elevator: stand on it and jump to go up, sneak to go down, to the next Elevator straight above
 * or below in the same column, at most `range` blocks away.
 *
 * DRIVEN FROM THE PLAYER, NOT FROM THE BLOCK. The idle cost is two `@a` score filters per tick,
 * however many Elevators a world holds: the players who jumped since the last tick
 * (`minecraft.jump` statistic) and the players who sneaked during it (`minecraft.sneak_time`, which
 * counts every tick spent sneaking). Both scores are zeroed as they are read. Only for those players
 * is an Elevator looked for, as a marker right under their feet.
 *
 * A SNEAK RIDES ON THE PRESS, NOT WHILE HELD. A held sneak shows up every tick; the game time of
 * the last tick a player was seen sneaking (`ra.elev.sn`) tells a press (not seen the tick before)
 * from a hold. Walking onto an Elevator while sneaking is a hold, so it does not ride either.
 */
object Elevator {
	val block = CustomBlock(Ns.INTERACTIVE, "elevator", "Elevator", model = "minecraft:white_wool")
	val item = block.loreItem { lore(loreLine("Jump to go up, sneak to go down", "gray")) }
	val registerBlock = block.fn("register_block")
	private val jumped = block.fn("jumped")
	private val sneaked = block.fn("sneaked")
	private val ride = block.fn("ride")
	private val fromMarker = block.fn("from_marker")
	private val goUp = block.fn("go_up")
	private val goDown = block.fn("go_down")
	private val arrive = block.fn("arrive")

	/** A player's farthest Elevator. */
	const val MAX_RANGE = 64

	/** An admin's (ra.core.Admin): a whole world's height, which is what a column search can span. */
	const val ADMIN_MAX_RANGE = 384

	const val DEFAULT_RANGE = 16

	/** Ticks after a ride before the next one, so a held jump does not skip floors every tick. */
	private const val COOLDOWN = 6

	/** The jump statistic. */
	private const val JUMP = "ra.elev.jump"

	/** The sneak-time statistic: ticks spent sneaking. */
	private const val SNEAK_TIME = "ra.elev.sneak"

	/** The game time the player was last seen sneaking. */
	private const val SNEAK = "ra.elev.sn"

	/** The game time before which the player may not ride again. */
	private const val READY = "ra.elev.cd"

	/** On the player being carried, while the Elevator under them reads its range. */
	private const val RIDER = "ra.elev.rider"

	/** The objectives, added by the module's load. */
	val objectives = listOf(
		"$JUMP minecraft.custom:minecraft.jump",
		"$SNEAK_TIME minecraft.custom:minecraft.sneak_time",
		"$SNEAK dummy",
		"$READY dummy",
	)

	private val T = Obj.TEMP

	fun Ra.registerElevator() {
		block(block)
		properties(block) {
			// Players up to 64, admins to 384; the column search clamps only to 384 (ra.core.Admin).
			number("range", min = 2, max = MAX_RANGE, adminMax = ADMIN_MAX_RANGE)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ENDER_PEARL), count = 2) {
			pattern("WWW", "WEW", "WWW")
			key("W", listOf(Items.WHITE_WOOL))
			key("E", listOf(Items.ENDER_PEARL))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.range", "$DEFAULT_RANGE")
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			stackedPropLine("range", "Range: ", "aqua", " blocks")
		}
		pack.define(jumped) { jumped() }
		pack.define(sneaked) { sneaked() }
		pack.define(ride) { ride() }
		pack.define(fromMarker) { fromMarker() }
		pack.define(goUp) { search("~-0.5 ~0.5 ~-0.5", "up") }
		pack.define(goDown) { search("~-0.5 ~-1.5 ~-0.5", "down") }
		pack.define(arrive) { arrive() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.wool.break", cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.WHITE_WOOL, onBreak)
			// The whole idle cost of riding: two score filters, independent of the Elevator count.
			executeRun("as @a[scores={$JUMP=1..}] at @s", jumped)
			executeRun("as @a[scores={$SNEAK_TIME=1..}] at @s", sneaked)
		}
	}

	/** A player jumped since the last tick. As the player, at them. */
	private fun Function.jumped() {
		raw("scoreboard players set @s $JUMP 0")
		raw("scoreboard players set #dir $T 1")
		function(ride)
	}

	/** A player sneaked during the last tick. As the player, at them. Rides on a press only. */
	private fun Function.sneaked() {
		raw("scoreboard players set @s $SNEAK_TIME 0")
		raw("execute store result score #now $T run time query gametime")
		raw("scoreboard players operation #prev $T = #now $T")
		raw("scoreboard players remove #prev $T 1")
		raw("scoreboard players set #held $T 0")
		raw("execute if score @s $SNEAK = #prev $T run scoreboard players set #held $T 1")
		raw("scoreboard players operation @s $SNEAK = #now $T")
		raw("execute if score #held $T matches 1 run return 0")
		raw("scoreboard players set #dir $T -1")
		function(ride)
	}

	/**
	 * `#dir` 1 up, -1 down: ride from the Elevator under the player, if there is one. As the player,
	 * at them.
	 *
	 * UNDER THE FEET, IN THIS COLUMN. From the centre of the player's block column, 0.9 below their
	 * feet, an Elevator marker within 0.5 is the block they stand on: feet from its top (y+1) up to
	 * 0.9 above it, which covers the tick or two of a jump before it is seen. The marker of the
	 * column next door is at least 1 away, and an Elevator one block lower at least 0.6.
	 */
	private fun Function.ride() {
		raw("execute if entity @s[gamemode=spectator] run return 0")
		raw("execute store result score #now $T run time query gametime")
		raw("execute if score @s $READY > #now $T run return 0")
		raw("tag @s add $RIDER")
		executeRun("align xz positioned ~0.5 ~-0.9 ~0.5 as @e[type=marker,tag=${block.tag},distance=..0.5,limit=1] at @s", fromMarker)
		raw("tag @s remove $RIDER")
	}

	/** The Elevator under a rider: hand its range to the search. As its marker, at its block. */
	private fun Function.fromMarker() {
		// The column search's box heights: blocks y+1..y+range above, y-range..y-1 below.
		readCappedProperty("range", default = DEFAULT_RANGE, min = 2, adminMax = ADMIN_MAX_RANGE)
		raw("execute store result storage ra:interactive elev.up int 1 run scoreboard players remove #prop $T 1")
		raw("execute store result storage ra:interactive elev.down int -1 run scoreboard players get #prop $T")
		raw("execute if score #dir $T matches 1 as @a[tag=$RIDER,distance=..3,limit=1] run return run ${macroCall(goUp)} with storage ra:interactive elev")
		raw("execute as @a[tag=$RIDER,distance=..3,limit=1] run ${macroCall(goDown)} with storage ra:interactive elev")
	}


	/**
	 * Macro `{up}` or `{down}`: the nearest Elevator marker in this column, above or below. As the
	 * player, at the Elevator's block. [corner] is the bottom corner of the box's first block (the
	 * block above, or the block below), so `sort=nearest` from there is the next floor.
	 *
	 * Only the NEXT Elevator: one with no room above it stops the ride rather than being skipped,
	 * so a floor you cannot reach is never jumped over without you knowing why.
	 */
	private fun Function.search(corner: String, dy: String) {
		raw("\$execute positioned $corner positioned as @e[type=marker,tag=${block.tag},dx=0,dy=\$($dy),dz=0,sort=nearest,limit=1] run return run ${macroCall(arrive)}")
		// No Elevator in range.
		raw(deny)
	}

	/** Land on top of the target Elevator, if there is room. As the player, at the target's marker. */
	private fun Function.arrive() {
		// Room for a player: two air blocks on top of it.
		raw("execute unless block ~ ~1 ~ #minecraft:air run return run $deny")
		raw("execute unless block ~ ~2 ~ #minecraft:air run return run $deny")

		raw("execute at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.3 0.6 0.3 0.02 20 normal ${particleViewersSel()}")
		// A position only: the player keeps the way they were looking.
		raw("tp @s ~ ~0.5 ~")
		raw("particle minecraft:portal ~ ~1 ~ 0.3 0.6 0.3 0.4 30 normal ${particleViewersSel()}")
		raw("playsound minecraft:entity.enderman.teleport block ${soundListenersSel()} ~ ~1 ~ 0.5 1.6")

		raw("scoreboard players operation @s $READY = #now $T")
		raw("scoreboard players add @s $READY $COOLDOWN")
		raw("return 1")
	}

	/** The low note a ride that cannot happen plays, to the rider only. As the player. */
	private val deny get() = "execute at @s run playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.4 0.6"
}
