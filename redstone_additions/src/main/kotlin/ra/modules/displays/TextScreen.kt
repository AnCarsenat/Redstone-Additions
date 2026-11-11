package ra.modules.displays

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
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
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeReturnRun
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
import ra.modules.displays.DisplayStyle.Companion.defineForms
import ra.modules.displays.TextColor.applyColor

/**
 * Text Screen: a barrel whose screen shows one line of glowing text, nixie-tube style, chosen by
 * the redstone level feeding it.
 *
 * WHAT IT SHOWS. Level L from 1 to 15 shows the name of the item in slot 15 - L (15: the first
 * slot, 14: the second... 1: the fifteenth), through [ItemName.build]: copied as a component,
 * never pasted as text. An empty barrel shows the level itself, 0 to 15, in large digits. With
 * items but nothing in the chosen slot (or at level 0) the tube is dark.
 *
 * A barrel, as the Item Hologram: a player or a hopper fills it like any container, it breaks by
 * hand and spills what it holds, and it fires nothing on a redstone edge. The screen is on the
 * face `facing` names, and the barrel's lid follows it.
 *
 * INPUT as the Gauge Bar's: the strongest level on any side, a repeater or comparator pointing in
 * reads as 15; each side asked `any_side` first. The last level is the marker's `ra.power` score.
 *
 * REDRAW ONLY ON A CHANGE. What would be shown (`{item:<the stack, no slot or count>}`,
 * `{level:L}` or `{}`) is compared with the copy last drawn (`data.data.shown`); the displays are
 * touched only on a difference. The tick builds it only when the level changes; the module's
 * slow clock ([poll]) builds it once a second, which picks up the barrel's contents changing,
 * restyles on a property change and puts back a display killed by hand.
 *
 * CONFIGURATION ([DisplayStyle]): `facing`, `direction` (where the top of the text points: `up`,
 * `left`, `down`, `right`, the text rolled on the screen), `color` (a text colour name or
 * `#RRGGBB`, nixie amber by default, see [TextColor]), `background` (a colour behind the text,
 * none by default: the dark screen shows), `rotation` (the text's tilt; the screen stays), offsets
 * and scale.
 */
object TextScreen : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "text_screen", "Text Screen", model = "minecraft:barrel", dirType = 2)
	override val item = block.loreItem { lore(loreLine("Shows an item's name, picked by the redstone level", "gray")) }

	private val process = block.fn("process")
	private val show = block.fn("show")
	private val draw = block.fn("draw")
	private val summon = block.fn("summon")
	private val restyle = block.fn("restyle")
	override val poll = block.fn("poll")

	private const val TAG = "ra.displays.screen"
	private const val PANEL_TAG = "ra.displays.screen_panel"
	private const val TEXT_TAG = "ra.displays.screen_text"
	private val PANEL = sel("block_display", PANEL_TAG)
	private val TEXT = sel("text_display", TEXT_TAG)

	/** Nixie amber. */
	private const val AMBER = "#FF8C1A"

	private val style = DisplayStyle(
		block, defaultFacing = "south", directions = listOf("up", "left", "down", "right"),
		colors = listOf(ColorProp("color", "Colour", AMBER, listOf(AMBER, "red", "gold", "yellow", "green", "aqua", "blue", "light_purple", "white"))),
		displays = listOf(PANEL, TEXT), barrel = true, backgrounds = listOf(TEXT),
	)

	/** The text's roll for each direction, counter-clockwise. */
	private val ROLLS = mapOf("up" to 0, "left" to 90, "down" to 180, "right" to 270)
	private val FACE = Vec(0.0, 0.0, 0.5)
	/** Half a 10 px line, in the text's own units: where a tilt turns it about. */
	private const val HALF_LINE = 5 * 0.025

	private const val T = Obj.TEMP
	private const val LEVEL = "#tscr.l"
	private const val LAST = Obj.POWER

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	override fun Ra.registerBlock() {
		block(block)
		properties(block) { with(style) { declare() } }
		wrenchCycle(block.id, *style.wrenchRows())
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
			raw("data modify entity ${block.newMarkerSel()} data.properties set value {${style.placementSnbt()}}")
			raw("data modify entity ${block.newMarkerSel()} data.properties.facing set from storage ra:temp facing_name")
			executeRun("as ${block.newMarkerSel()} at @s", restyle)
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
		with(style) { registerStyle(restyle) }
		val panelForm = defineForms(block, "panel", PANEL, mapOf("" to Form(Vec(-0.4375, -0.4375, 0.5), Vec(0.875, 0.875, 0.03125), FACE)))
		// A text display grows up from its origin: each form is moved down by half a line (10 px *
		// 0.025 * scale) to sit in the middle of the face; a name wraps at `line_width` (75 px).
		val nameForm = defineForms(block, "name", TEXT, ROLLS.mapValues { (_, r) -> Form(Vec(0.0, -0.056, 0.54), Vec(0.45, 0.45, 0.45), FACE, r, pivot = HALF_LINE) }, tilt = true)
		val digitForm = defineForms(block, "digits", TEXT, ROLLS.mapValues { (_, r) -> Form(Vec(0.0, -0.25, 0.54), Vec(2.0, 2.0, 2.0), FACE, r, pivot = HALF_LINE) }, tilt = true)

		pack.define(process) { process() }
		pack.define(show) { show() }
		pack.define(draw) { draw(nameForm, digitForm) }
		pack.define(summon) { summon() }
		pack.define(restyle) { restyle(panelForm) }
		pack.define(poll) { poll() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("level", "Level: ", "red", y = 0.8)
			propLine("facing", "Facing: ", "aqua", y = 0.6)
			propLine("direction", "Direction: ", "aqua", y = 0.4)
			propLine("color", "Colour: ", "aqua", y = 0.2)
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
	private fun Function.draw(nameForm: FunctionArgument, digitForm: FunctionArgument) {
		raw("""data modify entity $TEXT text set value """"")
		raw("execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.item set from entity @s data.data.shown.item")
		executeRun("if data entity @s data.data.shown.item", ItemName.build)
		// Wrapped, so the name takes the colour unless it has one of its own (an anvil name can).
		raw("""execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.text set value {text:"",extra:[]}""")
		raw("execute if data entity @s data.data.shown.item run data modify storage ra:temp dsp.text.extra append from storage ra:temp dsp.name")
		for (l in 0..15) {
			raw("""execute if data entity @s data.data.shown{level:$l} run data modify storage ra:temp dsp.text set value {text:"$l"}""")
		}
		raw("execute unless data entity @s data.data.shown.item unless data entity @s data.data.shown.level run return 0")
		raw("data modify storage ra:temp dsp.text.color set from entity @s data.data.color")
		raw("data modify entity $TEXT text set from storage ra:temp dsp.text")
		function(style.read)
		executeRun("if data entity @s data.data.shown.item", nameForm)
		executeRun("if data entity @s data.data.shown.level", digitForm)
	}

	/** Everything from the properties, both displays put back if one is missing, then drawn again. As the marker, at it. */
	private fun Function.restyle(panelForm: FunctionArgument) {
		function(style.defaults)
		executeRun("unless entity $PANEL", summon)
		executeRun("unless entity $TEXT", summon)
		raw("data modify entity @s data.data.style set from entity @s data.properties")

		function(style.turn)
		applyColor("color", AMBER)
		function(style.read)
		function(panelForm)
		raw("data remove entity @s data.data.shown")
		function(show)
	}

	/** Both displays, new and empty, and nothing remembered as drawn; restyle shapes them. As the marker, at it. */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		val zero = "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}"
		summonDisplay("block_display", listOf(TAG, PANEL_TAG), """block_state:{id:"minecraft:black_concrete"},$zero""")
		summonDisplay("text_display", listOf(TAG, TEXT_TAG), """text:"",background:0,shadow:1b,line_width:75,alignment:"center",$zero""")
		raw("data remove entity @s data.data.shown")
	}

	/** Once a second, as the marker at its block: a display put back, a property change drawn, the barrel looked at. */
	private fun Function.poll() {
		executeReturnRun("unless entity $PANEL", restyle)
		executeReturnRun("unless entity $TEXT", restyle)
		// Placed before the display settings existed: restyle fills the defaults (scale, colours).
		executeReturnRun("unless data entity @s data.properties.scale", restyle)
		raw("execute store success score #tscr.st $T run data modify entity @s data.data.style set from entity @s data.properties")
		executeReturnRun("if score #tscr.st $T matches 1", restyle)
		function(show)
	}
}
