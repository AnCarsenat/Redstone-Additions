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
import ra.core.block.STONE_BREAK_SOUND
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
 * Gauge Bar: a bar on one face, filled to the redstone level feeding it (0-15).
 *
 * INPUT. The strongest level on any of its six sides; a repeater or comparator pointing in reads
 * as 15. Each side is first asked `any_side` (stops at the first source) and only a powered side
 * has its level resolved, so an unpowered gauge costs six `any_side` reads a tick, like a gate.
 *
 * DRAWING. One text display, like the Text Screen: fifteen full-block glyphs ([SEGMENT], 8 px
 * wide with a 1 px gap), the first L in the fill colour and the rest in the track colour, so each
 * level lights one segment of an LED bar. Text stays crisp at any scale. The text changes only
 * when the level does: the last level drawn is the marker's `ra.power` score (this block uses it
 * for nothing else), and a new one picks one of sixteen fixed texts by score test, no macro.
 *
 * CONFIGURATION ([DisplayStyle]): `facing` (the face), `direction` (which way the bar fills:
 * `right` from the left edge, `left`, `up` from the bottom, `down`; the text is rolled on its
 * face), `color` and `track_color` (a text colour name or `#RRGGBB`, checked by [TextColor];
 * anything else draws the default), `background` (a dark plate behind the bar), offsets and
 * scale. The wrench and placement redraw at once; a Data Handler edit, or a display killed by
 * hand, is picked up by [poll] within a second: it compares `data.properties` with the copy last
 * drawn (`data.data.style`).
 *
 * DRAWN BY AN EARLIER DEV BUILD. 6.0.3 dev builds drew the bar as two block displays (tags
 * `ra.displays.gauge_track`, `ra.displays.gauge_fill`, both also [TAG]). [BAR] selects a text
 * display, so [poll] finds none on such a block and restyles it; [summon] kills every display
 * carrying the block's [TAG] first, so the old parts go and the text display takes their place.
 */
object GaugeBar : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "gauge_bar", "Gauge Bar", model = "minecraft:polished_blackstone", dirType = 3)
	override val item = block.loreItem { lore(loreLine("Shows the redstone level feeding it as a bar", "gray")) }

	private val process = block.fn("process")
	private val show = block.fn("show")
	private val restyle = block.fn("restyle")
	private val summon = block.fn("summon")
	override val poll = block.fn("poll")

	private const val TAG = "ra.displays.gauge"
	private const val BAR_TAG = "ra.displays.gauge_bar"
	private val BAR = sel("text_display", BAR_TAG)

	/** One segment per level: U+2588 FULL BLOCK, an 8x8 glyph of the default font. */
	private const val SEGMENT = "█"
	private const val LEVELS = 15

	private const val FILL_DEFAULT = "red"
	private const val TRACK_DEFAULT = "dark_gray"

	private val style = DisplayStyle(
		block, defaultFacing = "south", directions = listOf("right", "left", "up", "down"),
		colors = listOf(
			ColorProp("color", "Colour", FILL_DEFAULT, listOf("red", "gold", "yellow", "green", "aqua", "blue", "light_purple", "white")),
			ColorProp("track_color", "Track colour", TRACK_DEFAULT, listOf("dark_gray", "gray", "black", "white")),
		),
		displays = listOf(BAR), background = true,
	)

	/** The bar's roll for each direction, counter-clockwise: the fill grows along the rolled x. */
	private val ROLLS = mapOf("right" to 0, "left" to 180, "up" to 90, "down" to 270)
	private val FACE = Vec(0.0, 0.0, 0.5)

	/**
	 * The text's size at 100 %. Fifteen glyphs advance 9 px each, 135 px, and a text display
	 * draws 1/40 block per pixel at scale 1: [SX] makes the bar 13/16 block long. The glyph is
	 * 8 px tall: [SY] makes it 3/16 block. A text grows up from its origin and its line is 10 px,
	 * so the origin is moved down half a line to centre the bar on the face.
	 */
	private const val SX = 0.2407
	private const val SY = 0.9375
	private val BAR_T = Vec(0.0, -5 * 0.025 * SY, 0.51)

	private const val T = Obj.TEMP
	private const val LEVEL = "#gauge.l"
	private const val LAST = Obj.POWER

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	override fun Ra.registerBlock() {
		block(block)
		properties(block) { with(style) { declare() } }
		wrenchCycle(block.id, *style.wrenchRows())
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.COMPARATOR), count = null) {
			pattern("GGG", "RCR", "BBB")
			key("G", Items.GLASS_PANE)
			key("R", Items.REDSTONE)
			key("C", Items.COMPARATOR)
			key("B", Items.POLISHED_BLACKSTONE)
		}

		// As the bat, at it. The bar goes on the face looking at the player.
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
					storeResult { score(literal("#gauge.s"), T) }
					run { function(RaLib.Redstone.side, s.args()) }
				}
				raw("scoreboard players operation $LEVEL $T > #gauge.s $T")
			}
		}
		with(style) { registerStyle(restyle) }
		val barForm = defineForms(block, "bar", BAR, ROLLS.mapValues { (_, r) -> Form(BAR_T, Vec(SX, SY, 1.0), FACE, r) })

		pack.define(process) { process() }
		pack.define(show) { show() }
		pack.define(restyle) { restyle(barForm) }
		pack.define(summon) { summon() }
		pack.define(poll) { poll() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("level", "Level: ", "red", y = 0.8)
			propLine("facing", "Facing: ", "aqua", y = 0.6)
			propLine("direction", "Direction: ", "aqua", y = 0.4)
			propLine("color", "Colour: ", "aqua", y = 0.2)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true, cleanup = { killDisplays(TAG) })
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.POLISHED_BLACKSTONE)) {
			startIfIdle(block)
			forEachMarker(block, process)
		}
	}

	/** Every tick, as the marker at its block: read the level; redraw only when it changed. */
	private fun Function.process() {
		raw("scoreboard players set $LEVEL $T 0")
		for (s in Side.entries) executeRun("if function ${any(s).asId()}", level(s))
		// 16 is superpower (a repeater or comparator pointing in): a full bar.
		raw("execute if score $LEVEL $T matches 16.. run scoreboard players set $LEVEL $T 15")
		raw("execute if score @s $LAST = $LEVEL $T run return 0")
		raw("scoreboard players operation @s $LAST = $LEVEL $T")
		raw("execute store result entity @s data.status.level int 1 run scoreboard players get @s $LAST")
		function(show)
	}

	/**
	 * The bar's text for the last level: L segments in the fill colour, 15 - L in the track
	 * colour, the colours the ones restyle checked (`data.data.color`, `data.data.track`). As the
	 * marker, at it.
	 */
	private fun Function.show() {
		raw("""data modify storage ra:temp dsp.bar set value {text:"",extra:[{text:""},{text:"${SEGMENT.repeat(LEVELS)}"}]}""")
		for (l in 1..LEVELS) {
			raw("""execute if score @s $LAST matches $l run data modify storage ra:temp dsp.bar.extra set value [{text:"${SEGMENT.repeat(l)}"},{text:"${SEGMENT.repeat(LEVELS - l)}"}]""")
		}
		raw("data modify storage ra:temp dsp.bar.extra[0].color set from entity @s data.data.color")
		raw("data modify storage ra:temp dsp.bar.extra[1].color set from entity @s data.data.track")
		raw("data modify entity $BAR text set from storage ra:temp dsp.bar")
	}

	/** Everything from the properties, the display put back if it is missing. As the marker, at it. */
	private fun Function.restyle(barForm: FunctionArgument) {
		function(style.defaults)
		executeRun("unless entity $BAR", summon)
		raw("data modify entity @s data.data.style set from entity @s data.properties")

		applyColor("color", FILL_DEFAULT)
		applyColor("track_color", TRACK_DEFAULT, into = "track")
		raw("data modify entity $BAR background set value 0")
		raw("execute if data entity @s data.properties{background:1b} run data modify entity $BAR background set value ${DisplayStyle.BACKGROUND}")
		function(style.turn)
		function(style.read)
		function(barForm)
		function(show)
	}

	/** The display, new and empty, every older part of this block gone first; restyle shapes it. As the marker, at it. */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		val zero = "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}"
		summonDisplay("text_display", listOf(TAG, BAR_TAG), """text:"",background:0,shadow:0b,line_width:200,alignment:"center",$zero""")
	}

	/** Once a second, as the marker at its block: redraw if the display is gone or the properties changed. */
	private fun Function.poll() {
		executeReturnRun("unless entity $BAR", restyle)
		// Placed before the display settings existed: restyle fills the defaults (scale, colours).
		executeReturnRun("unless data entity @s data.properties.scale", restyle)
		raw("execute store success score #gauge.ch $T run data modify entity @s data.data.style set from entity @s data.properties")
		executeRun("if score #gauge.ch $T matches 1", restyle)
	}
}
