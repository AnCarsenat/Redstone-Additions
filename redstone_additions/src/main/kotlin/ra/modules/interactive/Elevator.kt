package ra.modules.interactive

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
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
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
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.macroCall
import ra.core.raw

/**
 * Elevator: stand on it and jump to go up, sneak to go down, to the next Elevator straight above
 * or below in the same column, at most `range` blocks away.
 *
 * WHAT IT COSTS WHILE NOBODY RIDES IT. One `@a` box test per Elevator per tick (the block above
 * it). Everything else -- the range, the game time, the column search -- runs only for a player
 * standing in that box. Nothing walks every player of the server: a player is only ever looked at
 * from the Elevator they stand on.
 *
 * HOW A JUMP AND A SNEAK ARE SEEN. The jump is the `minecraft.custom:minecraft.jump` statistic,
 * zeroed every tick the rider is seen, so any value above 0 is a jump since the last tick. The
 * sneak is the `ra:is_sneaking` predicate against the rider's flag from the last tick, so only
 * pressing sneak moves you, not holding it. A player who was not on an Elevator the tick before
 * (`ra.elev.t`, the game time they were last seen on one) is only synced: a jump taken elsewhere,
 * or arriving while sneaking, never fires.
 */
object Elevator {
	val block = CustomBlock(Ns.INTERACTIVE, "elevator", "Elevator", model = "minecraft:white_wool")
	val item = block.loreItem { lore(loreLine("Jump to go up, sneak to go down", "gray")) }
	val registerBlock = block.fn("register_block")
	private val check = block.fn("check")
	private val rider = block.fn("rider")
	private val fresh = block.fn("fresh")
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

	/** The game time the player was last seen on an Elevator. */
	private const val SEEN = "ra.elev.t"

	/** 1 when the player was sneaking the last time they were seen on an Elevator. */
	private const val SNEAK = "ra.elev.sn"

	/** The game time before which the player may not ride again. */
	private const val READY = "ra.elev.cd"

	/** On the Elevator whose riders are being handled, for the riders' own column test. */
	private const val HERE = "ra.elevator.here"

	/** The objectives, added by the module's load. */
	val objectives = listOf(
		"$JUMP minecraft.custom:minecraft.jump",
		"$SEEN dummy",
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
		pack.define(check) { check() }
		pack.define(rider) { rider() }
		pack.define(fresh) { fresh() }
		pack.define(goUp) { search("~-0.5 ~0.5 ~-0.5", "up") }
		pack.define(goDown) { search("~-0.5 ~-1.5 ~-0.5", "down") }
		pack.define(arrive) { arrive() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.wool.break", cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.WHITE_WOOL, onBreak)
			forEachMarker(block, check)
		}
	}

	/** One Elevator. As its marker, at its block. */
	private fun Function.check() {
		// The block above it, the only place a rider stands. The idle cost of the whole block.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 unless entity @a[dx=0,dy=0,dz=0,gamemode=!spectator,limit=1] run return 0")

		// The column search's box heights: blocks y+1..y+range above, y-range..y-1 below.
		readCappedProperty("range", default = DEFAULT_RANGE, min = 2, adminMax = ADMIN_MAX_RANGE)
		raw("execute store result storage ra:interactive elev.up int 1 run scoreboard players remove #prop $T 1")
		raw("execute store result storage ra:interactive elev.down int -1 run scoreboard players get #prop $T")

		raw("tag @s add $HERE")
		executeRun("positioned ~-0.5 ~0.5 ~-0.5 as @a[dx=0,dy=0,dz=0,gamemode=!spectator] positioned ~0.5 ~-0.5 ~0.5", rider)
		raw("tag @s remove $HERE")
	}

	/** A player in the box above an Elevator. As the player, at the Elevator's block. */
	private fun Function.rider() {
		// THE BOX IS NOT THE COLUMN. A player's hitbox reaches 0.3 past their feet, so a player
		// standing on the block next door is in this box too. Their own block column must hold
		// this Elevator: from the block their feet are in, two blocks down (feet at y+1 standing,
		// up to y+2 at the top of a jump).
		raw("execute at @s align xyz positioned ~ ~-2 ~ unless entity @e[type=marker,tag=$HERE,dx=0,dy=1,dz=0,limit=1] run return 0")

		raw("execute store result score #now $T run time query gametime")
		// Handled already this tick (two Elevators side by side both see a player on the seam).
		raw("execute if score @s $SEEN = #now $T run return 0")
		raw("scoreboard players operation #prev $T = #now $T")
		raw("scoreboard players remove #prev $T 1")
		executeReturnRun("unless score @s $SEEN = #prev $T", fresh)
		raw("scoreboard players operation @s $SEEN = #now $T")

		// What the player asks for: a sneak pressed since the last tick goes down, a jump goes up.
		raw("scoreboard players set #dir $T 0")
		raw("execute store success score #sn $T if predicate ra:is_sneaking")
		raw("execute if score #sn $T matches 1 unless score @s $SNEAK matches 1 run scoreboard players set #dir $T -1")
		raw("scoreboard players operation @s $SNEAK = #sn $T")
		raw("execute if score @s $JUMP matches 1.. run scoreboard players set #dir $T 1")
		raw("scoreboard players set @s $JUMP 0")

		raw("execute if score #dir $T matches 0 run return 0")
		raw("execute if score @s $READY > #now $T run return 0")
		raw("execute if score #dir $T matches 1 run return run ${macroCall(goUp)} with storage ra:interactive elev")
		callWith(goDown, "storage ra:interactive elev")
	}

	/** First tick on an Elevator: remember, do not act. As the player. */
	private fun Function.fresh() {
		raw("scoreboard players operation @s $SEEN = #now $T")
		raw("scoreboard players set @s $JUMP 0")
		raw("execute store success score @s $SNEAK if predicate ra:is_sneaking")
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
