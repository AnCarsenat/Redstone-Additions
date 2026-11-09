package ra.modules.displays

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.forEachMarker
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.lib.redstone.Side
import ra.lib.redstone.SideReaders
import ra.modules.displays.DisplayEntities.killDisplays
import ra.modules.displays.DisplayEntities.sel
import ra.modules.displays.DisplayEntities.summonDisplay
import ra.modules.displays.DisplayPoll.startIfIdle

/**
 * Text Screen: a barrel whose front shows one line of glowing amber text, nixie-tube style,
 * chosen by the redstone level feeding it.
 *
 * WHAT IT SHOWS. Level L from 1 to 15 shows the name of the item in slot 15 - L (15: the first
 * slot, 14: the second... 1: the fifteenth), through [ItemName.build]: copied as a component,
 * never pasted as text. An empty barrel shows the level itself, 0 to 15, in large digits. With
 * items but nothing in the chosen slot (or at level 0) the tube is dark.
 *
 * A barrel, as the Item Hologram: a player or a hopper fills it like any container, it breaks by
 * hand and spills what it holds, and it fires nothing on a redstone edge. Its `facing` is the
 * screen's (dir_type 2): the lid is the face the text is drawn on.
 *
 * INPUT as the Gauge Bar's: the strongest level on any side, a repeater or comparator pointing in
 * reads as 15; each side asked `any_side` first. The last level is the marker's `ra.power` score.
 *
 * REDRAW ONLY ON A CHANGE. What would be shown (`{item:<the stack, no slot or count>}`,
 * `{level:L}` or `{}`) is compared with the copy last drawn (`data.data.shown`); the displays are
 * touched only on a difference. The tick builds it only when the level changes; the module's
 * slow clock ([poll]) builds it once a second, which picks up the barrel's contents changing and
 * puts back a display killed by hand.
 */
object TextScreen : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "text_screen", "Text Screen", model = "minecraft:barrel", dirType = 2)
	override val item = block.loreItem { lore(loreLine("Shows an item's name, picked by the redstone level", "gray")) }

	private val process = block.fn("process")
	private val show = block.fn("show")
	private val draw = block.fn("draw")
	private val summon = block.fn("summon")
	override val poll = block.fn("poll")

	private const val TAG = "ra.displays.screen"
	private const val PANEL_TAG = "ra.displays.screen_panel"
	private const val TEXT_TAG = "ra.displays.screen_text"
	private val PANEL = sel("block_display", PANEL_TAG)
	private val TEXT = sel("text_display", TEXT_TAG)

	/** Nixie amber. */
	private const val AMBER = "#FF8C1A"

	private const val T = Obj.TEMP
	private const val LEVEL = "#tscr.l"
	private const val LAST = Obj.POWER

	private const val ROT = "left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]"

	/**
	 * The text's transformation for a name and for the digits. A text display grows up from its
	 * origin, so each is moved down by half a line (10 px * 0.025 * scale) to sit in the middle of
	 * the face; a name wraps at `line_width` (75 px), 0.84 block at its scale.
	 */
	private const val NAME_FORM = "{transformation:{translation:[0f,-0.056f,0.54f],scale:[0.45f,0.45f,0.45f]}}"
	private const val DIGIT_FORM = "{transformation:{translation:[0f,-0.25f,0.54f],scale:[2f,2f,2f]}}"

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	override fun Ra.registerBlock() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.GLOWSTONE_DUST), count = null) {
			pattern("GGG", "DCD", " B ")
			key("G", Items.GLASS_PANE)
			key("D", Items.GLOWSTONE_DUST)
			key("C", Items.COMPARATOR)
			key("B", Items.BARREL)
		}

		// As the bat, at it. The barrel is placed facing the player: its lid is the screen.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			raw("data modify entity ${block.newMarkerSel()} data.status.facing set from storage ra:temp facing_name")
			executeRun("as ${block.newMarkerSel()} at @s", summon)
			executeRun("as ${block.newMarkerSel()} at @s", process)
			finishPlacement(block, removePlaceTag = false)
		}

		for (s in Side.entries) {
			pack.define(any(s)) { returnRun { function(SideReaders.anySide, s.args()) } }
			pack.define(level(s)) {
				execute {
					storeResult { score(literal("#tscr.s"), T) }
					run { function(RaLib.Redstone.side, s.args()) }
				}
				raw("scoreboard players operation $LEVEL $T > #tscr.s $T")
			}
		}
		pack.define(process) { process() }
		pack.define(show) { show() }
		pack.define(draw) { draw() }
		pack.define(summon) { summon() }
		pack.define(poll) { poll() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("level", "Level: ", "red", y = 0.8)
			dataLine("facing", "Facing: ", "aqua", y = 0.6)
		}

		// The barrel spills what it holds, as any barrel does.
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.wood.break", cloud = true, cleanup = { killDisplays(TAG) })
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.BARREL)) {
			startIfIdle(block)
			forEachMarker(block, process)
		}
	}

	/** Every tick, as the marker at its block: read the level; look again only when it changed. */
	private fun Function.process() {
		raw("scoreboard players set $LEVEL $T 0")
		for (s in Side.entries) executeRun("if function ${any(s).asId()}", level(s))
		// 16 is superpower (a repeater or comparator pointing in): 15.
		raw("execute if score $LEVEL $T matches 16.. run scoreboard players set $LEVEL $T 15")
		raw("execute if score @s $LAST = $LEVEL $T run return 0")
		raw("scoreboard players operation @s $LAST = $LEVEL $T")
		raw("execute store result entity @s data.status.level int 1 run scoreboard players get @s $LAST")
		function(show)
	}

	/** What the level and the barrel ask for, drawn only if it differs from the last drawn. As the marker, at it. */
	private fun Function.show() {
		raw("data remove storage ra:temp dsp.item")
		// Level L reads slot 15 - L. A score test per level, no macro: this runs on a change or once a second.
		for (l in 1..15) {
			raw("execute if score @s $LAST matches $l run data modify storage ra:temp dsp.item set from block ~ ~ ~ Items[{Slot:${15 - l}b}]")
		}
		// What it looks like, not where it is or how many.
		raw("data remove storage ra:temp dsp.item.Slot")
		raw("data remove storage ra:temp dsp.item.count")

		raw("data modify storage ra:temp dsp.shown set value {}")
		raw("execute if data storage ra:temp dsp.item run data modify storage ra:temp dsp.shown.item set from storage ra:temp dsp.item")
		raw("execute unless data block ~ ~ ~ Items[0] store result storage ra:temp dsp.shown.level int 1 run scoreboard players get @s $LAST")
		raw("execute store success score #tscr.ch $T run data modify entity @s data.data.shown set from storage ra:temp dsp.shown")
		executeRun("if score #tscr.ch $T matches 1", draw)
	}

	/** The text from `data.data.shown`: a name, the level's digits, or nothing. As the marker, at it. */
	private fun Function.draw() {
		raw("""data modify entity $TEXT text set value """"")
		raw("execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.item set from entity @s data.data.shown.item")
		executeRun("if data entity @s data.data.shown.item", ItemName.build)
		// Wrapped, so the name takes the amber unless it has a colour of its own (an anvil name can).
		raw("""execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.text set value {text:"",color:"$AMBER",extra:[]}""")
		raw("execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.text.extra append from storage ra:temp dsp.name")
		raw("execute if data entity @s data.data.shown.item run data modify entity $TEXT text set from storage ra:temp dsp.text")
		raw("execute if data entity @s data.data.shown.item run data merge entity $TEXT $NAME_FORM")

		raw("execute if data entity @s data.data.shown.level run data merge entity $TEXT $DIGIT_FORM")
		for (l in 0..15) {
			raw("""execute if data entity @s data.data.shown{level:$l} run data modify entity $TEXT text set value {text:"$l",color:"$AMBER"}""")
		}
	}

	/**
	 * Both displays, new, and nothing remembered as drawn. As the marker, at it. Local +z is out of
	 * the front face (the marker's Rotation is the facing): a dark panel on the lid, the text just
	 * in front of it, reading from outside.
	 */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		summonDisplay("block_display", listOf(TAG, PANEL_TAG),
			"""block_state:{id:"minecraft:black_concrete"},transformation:{$ROT,translation:[-0.4375f,-0.4375f,0.5f],scale:[0.875f,0.875f,0.03125f]}""", rotated = true)
		summonDisplay("text_display", listOf(TAG, TEXT_TAG),
			"""text:"",background:0,shadow:1b,line_width:75,alignment:"center",transformation:{$ROT,translation:[0f,-0.056f,0.54f],scale:[0.45f,0.45f,0.45f]}""", rotated = true)
		raw("data remove entity @s data.data.shown")
	}

	/** Once a second, as the marker at its block: a display put back, the barrel's contents looked at. */
	private fun Function.poll() {
		executeRun("unless entity $PANEL", summon)
		executeRun("unless entity $TEXT", summon)
		function(show)
	}
}
