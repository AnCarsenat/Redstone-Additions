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
 * DRIVEN FROM THE PLAYER, NOT FROM THE BLOCK, as the Elevator. The idle cost is one `@a` score
 * filter per tick however many terminals a world holds (none without a terminal): the players who
 * sneaked during the last tick ([SNEAK_TIME], the `minecraft.sneak_time` statistic, zeroed as it is
 * read). The menu opens on the press, not while sneak is held: [SNEAK] is the game time the player
 * was last seen sneaking, so a sneak seen the tick before is a hold. Only on a press is a terminal
 * looked for, as the marker right under the player's feet ([find]).
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
	private val sneaked = block.fn("sneaked")

	/** The sneak-time statistic: ticks spent sneaking. */
	private const val SNEAK_TIME = "ra.ender.t_sneak"
	/** The game time the player was last seen sneaking. */
	private const val SNEAK = "ra.ender.t_sn"
	/** Objectives (`name criterion`), for the module's load. */
	val objectives = listOf("$SNEAK_TIME minecraft.custom:minecraft.sneak_time", "$SNEAK dummy")

	/** On the terminal under the player [find] works for, for one function. */
	private const val HERE = "ra.ender.terminal_here"

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

		pack.define(sneaked) { sneaked() }
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
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.LODESTONE)) {
			// The whole idle cost: one score filter, independent of the terminal count.
			executeRun("as @a[scores={$SNEAK_TIME=1..}] at @s", sneaked)
		}
	}

	/** A player sneaked during the last tick. As the player, at them. Opens the menu on a press only. */
	private fun Function.sneaked() {
		raw("scoreboard players set @s $SNEAK_TIME 0")
		raw("execute store result score #ender.now $T run time query gametime")
		raw("scoreboard players operation #ender.prev $T = #ender.now $T")
		raw("scoreboard players remove #ender.prev $T 1")
		raw("scoreboard players set #ender.held $T 0")
		raw("execute if score @s $SNEAK = #ender.prev $T run scoreboard players set #ender.held $T 1")
		raw("scoreboard players operation @s $SNEAK = #ender.now $T")
		raw("execute if score #ender.held $T matches 1 run return 0")
		raw("execute if entity @s[gamemode=spectator] run return 0")

		raw("data modify storage ra:ender menu set value {}")
		raw("execute store result score #ender.ok $T run function ${find.asId()}")
		raw("execute if score #ender.ok $T matches 0 run return 0")
		raw("scoreboard players set @s ${Chooser.SRC} 1")
		raw("function ${Chooser.open.asId()}")
		raw("playsound minecraft:block.respawn_anchor.charge block ${soundListenersSel(8)} ~ ~ ~ 0.4 1.6")
	}

	/**
	 * UNDER THE FEET, IN THIS COLUMN (as the Elevator). From the centre of the player's block
	 * column, 0.9 below their feet, a terminal marker within 0.5 is the block they stand on; the
	 * marker of the column next door is at least 1 away. As a player, anywhere.
	 */
	private fun Function.find() {
		raw("execute at @s align xz positioned ~0.5 ~-0.9 ~0.5 as ${block.markerSel("distance=..0.5", "limit=1")} run tag @s add $HERE")
		raw("execute unless entity ${block.markerSel("tag=$HERE", "limit=1")} run return 0")
		raw("data modify storage ra:ender menu.favs set from entity ${block.markerSel("tag=$HERE", "limit=1")} data.properties.favourites")
		raw("tag ${block.markerSel("tag=$HERE")} remove $HERE")
		raw("return 1")
	}
}
