package ra.modules.ender

import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Obj
import ra.core.Ns
import ra.core.Ra
import ra.core.TextFormat
import ra.core.blockRegisterMessage
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.blockTick
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreLine
import ra.core.raw
import ra.core.textList
import ra.modules.ender.chooser.Chooser

/**
 * Anchor Terminal: a lodestone you stand on and sneak to choose where to go.
 *
 * Sneaking on top opens the destination menu ([Chooser]) in chat: the terminal's `favourites`,
 * each with a [Go], and a [Type a destination] that asks for any anchor id in the Input Form book.
 * The trip itself is the destinations table's ([ra.modules.ender.anchors.AnchorTable.send]), so
 * the anchor may be anywhere: an unloaded chunk, another dimension. A terminal costs nothing to
 * use: it is a fixed station, like the Teleport Anchor.
 *
 * WHY SNEAKING, AND NOT A RIGHT-CLICK. A block has no right-click a data pack can see without an
 * interaction entity in front of it, and that entity would catch the left-click too, so the
 * terminal could no longer be broken by hand. Standing on it is what the Teleport Anchor and the
 * Elevator already ask, and a sneak is a deliberate press, not something walking across does.
 *
 * WHAT IT COSTS WHILE NOBODY USES IT. One `@a` box test per terminal per tick (the block above
 * it), and the module tick runs none of it without a terminal in the world. Only a player in that
 * box is looked at further: a sneak pressed since the last tick (against [SNEAK], kept while the
 * player is seen on a terminal tick after tick, [SEEN]) opens the menu, holding it does not.
 */
object AnchorTerminal {
	val block = CustomBlock(Ns.ENDER, "anchor_terminal", "Anchor Terminal", model = "minecraft:lodestone")
	val item = block.enderItem(
		"epic",
		loreLine("Stand on it and sneak to choose a destination", "gray"),
		loreLine("Favourites: Data Handler, favourites", "dark_gray"),
	)

	/**
	 * As a player: the terminal they stand on, if any. Returns 1 and loads its favourites into
	 * `storage ra:ender menu.favs`, or returns 0.
	 */
	val find = block.fn("find")
	private val check = block.fn("check")
	private val rider = block.fn("rider")

	/** The game time a player was last seen on a terminal. */
	private const val SEEN = "ra.ender.t_seen"
	/** 1 when the player was sneaking the last time they were seen on a terminal. */
	private const val SNEAK = "ra.ender.t_sneak"
	/** Objectives, for the module's load. */
	val objectives = listOf(SEEN, SNEAK)

	/** On the terminal whose riders are being handled. */
	private const val HERE = "ra.ender.terminal_here"
	/** On the player [find] works for, for one command. */
	private const val ME = "ra.ender.me"

	private const val T = Obj.TEMP

	fun Ra.registerAnchorTerminal() {
		block(block)
		properties(block) {
			// The player's own list, shown as buttons; each a path key of the destinations table.
			textList("favourites", max = Chooser.MAX_FAVS, format = TextFormat.ID, entryLabel = "Favourite ")
		}
		pack.blockRegisterMessage(block)
		pack.enderGive(block, item, """{text:" — stand on it and sneak to choose where to go",color:"gray"}""")
		pack.enderRecipe(block, item, RecipeCategory.MISC) {
			pattern("OGO", "ECE", "OOO")
			key("O", Items.OBSIDIAN)
			key("G", Items.GLASS_PANE)
			key("E", Items.ENDER_PEARL)
			key("C", Items.COMPASS)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.favourites", "[]")
			finishPlacement(block)
		}

		pack.define(check) { check() }
		pack.define(rider) { rider() }
		pack.define(find) { find() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			raw("data modify storage ra:temp ender_line set value {label:\"Favourites: \",value:\"0\",color:\"aqua\",suffix:\"\"}")
			raw("execute store result storage ra:temp ender_line.value int 1 run data get entity @s data.properties.favourites")
			stackedTextLineFromStorage()
			raw("data modify storage ra:temp ender_line set value {label:\"\",value:\"Sneak on top to choose\",color:\"gray\",suffix:\"\"}")
			stackedTextLineFromStorage()
		}

		val onBreak = pack.enderOnBreak(block, item)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.LODESTONE)) { forEachMarker(block, check) }
	}

	/** One terminal. As its marker, at its block. */
	private fun Function.check() {
		// The block above it, the only place a rider stands. The idle cost of the whole block.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 unless entity @a[dx=0,dy=0,dz=0,gamemode=!spectator,limit=1] run return 0")
		raw("execute store result score #ender.now $T run time query gametime")
		raw("tag @s add $HERE")
		executeRun("positioned ~-0.5 ~0.5 ~-0.5 as @a[dx=0,dy=0,dz=0,gamemode=!spectator]", rider)
		raw("tag @s remove $HERE")
	}

	/** A player on top of a terminal. As the player; #ender.now is the game time. */
	private fun Function.rider() {
		// Handled already this tick (two terminals side by side both see a player on the seam).
		raw("execute if score @s $SEEN = #ender.now $T run return 0")
		raw("scoreboard players operation #ender.prev $T = #ender.now $T")
		raw("scoreboard players remove #ender.prev $T 1")
		raw("scoreboard players set #ender.fresh $T 0")
		raw("execute unless score @s $SEEN = #ender.prev $T run scoreboard players set #ender.fresh $T 1")
		raw("scoreboard players operation @s $SEEN = #ender.now $T")

		// A sneak pressed since the last tick, by a player who was already here then: arriving
		// while sneaking (or holding it) opens nothing.
		raw("execute store success score #ender.sn $T if predicate ra:is_sneaking")
		raw("scoreboard players set #ender.open $T 0")
		raw("execute if score #ender.fresh $T matches 0 if score #ender.sn $T matches 1 unless score @s $SNEAK matches 1 run scoreboard players set #ender.open $T 1")
		raw("scoreboard players operation @s $SNEAK = #ender.sn $T")
		raw("execute if score #ender.open $T matches 0 run return 0")

		raw("data modify storage ra:ender menu set value {}")
		raw("data modify storage ra:ender menu.favs set from entity @e[type=marker,tag=$HERE,limit=1] data.properties.favourites")
		raw("scoreboard players set @s ${Chooser.SRC} 1")
		raw("function ${Chooser.open.asId()}")
		raw("execute at @s run playsound minecraft:block.respawn_anchor.charge block ${soundListenersSel(8)} ~ ~ ~ 0.4 1.6")
	}

	/** As a player: is there a terminal under them? See [find]. */
	private fun Function.find() {
		raw("tag @s add $ME")
		raw("execute at @s as ${block.markerSel("distance=..3")} at @s positioned ~-0.5 ~0.5 ~-0.5 if entity @a[tag=$ME,dx=0,dy=0,dz=0,limit=1] run tag @s add $HERE")
		raw("tag @s remove $ME")
		raw("execute at @s run data modify storage ra:ender menu.favs set from entity ${block.markerSel("tag=$HERE", "distance=..3", "sort=nearest", "limit=1")} data.properties.favourites")
		raw("scoreboard players set #ender.ok $T 0")
		raw("execute at @s if entity ${block.markerSel("tag=$HERE", "distance=..3", "limit=1")} run scoreboard players set #ender.ok $T 1")
		raw("execute at @s run tag ${block.markerSel("tag=$HERE", "distance=..3")} remove $HERE")
		raw("return run scoreboard players get #ender.ok $T")
	}
}
