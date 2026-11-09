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

/**
 * Gauge Bar: a bar on one face, filled to the redstone level feeding it (0-15).
 *
 * INPUT. The strongest level on any of its six sides; a repeater or comparator pointing in reads
 * as 15. Each side is first asked `any_side` (stops at the first source) and only a powered side
 * has its level resolved, so an unpowered gauge costs six `any_side` reads a tick, like a gate.
 *
 * DRAWING. Two block displays standing on the marker: a track and a coloured fill in front of
 * it. Only the fill's length changes, and only when the level does: the last level drawn is the
 * marker's `ra.power` score (this block uses it for nothing else), and a new one is one
 * `store result` into `transformation.scale[0]`, interpolated over [GROW] ticks by the client.
 *
 * CONFIGURATION ([DisplayStyle]): `facing` (the face), `direction` (which way the bar fills:
 * `right` from the left edge, `left`, `up` from the bottom, `down`; the bar is rolled on its
 * face, so the fill always grows along its own x), `color` and `track_color` (a dye, see [DYES];
 * anything else draws the default), offsets and scale. The wrench and placement redraw at once;
 * a Data Handler edit, or a display killed by hand, is picked up by [poll] within a second: it
 * compares `data.properties` with the copy last drawn (`data.data.style`).
 */
object GaugeBar : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "gauge_bar", "Gauge Bar", model = "minecraft:polished_blackstone", dirType = 3)
	override val item = block.loreItem { lore(loreLine("Shows the redstone level feeding it as a bar", "gray")) }

	private val process = block.fn("process")
	private val show = block.fn("show")
	private val restyle = block.fn("restyle")
	private val summon = block.fn("summon")
	override val poll = block.fn("poll")

	/** The colours a bar may take: the sixteen dyes, as concrete. */
	val DYES = listOf(
		"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
		"lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink",
	)

	private const val TAG = "ra.displays.gauge"
	private const val TRACK_TAG = "ra.displays.gauge_track"
	private const val FILL_TAG = "ra.displays.gauge_fill"
	private val TRACK = sel("block_display", TRACK_TAG)
	private val FILL = sel("block_display", FILL_TAG)

	private val style = DisplayStyle(
		block, defaultFacing = "south", directions = listOf("right", "left", "up", "down"),
		colors = listOf(ColorProp("color", "Colour", "red", DYES), ColorProp("track_color", "Track colour", "gray", DYES)),
		displays = listOf(TRACK, FILL),
	)

	/** The bar's roll for each direction, counter-clockwise: the fill grows along the rolled x. */
	private val ROLLS = mapOf("right" to 0, "left" to 180, "up" to 90, "down" to 270)
	private val FACE = Vec(0.0, 0.0, 0.5)

	/** Ticks the fill takes to grow or shrink to a new level. */
	private const val GROW = 3

	/** Fill length per level at 100 %: 13/16 of a block at 15. */
	private const val PER_LEVEL = 0.0541667

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
		val track = defineForms(block, "track", TRACK, ROLLS.mapValues { (_, r) -> Form(Vec(-0.4375, -0.125, 0.5), Vec(0.875, 0.25, 0.03125), FACE, r) })
		// The fill's length (its x) is show's.
		val fill = defineForms(block, "fill", FILL, ROLLS.mapValues { (_, r) -> Form(Vec(-0.40625, -0.09375, 0.5), Vec(0.0, 0.1875, 0.0625), FACE, r) })

		pack.define(process) { process() }
		pack.define(show) { show() }
		pack.define(restyle) { restyle(track, fill) }
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

	/** The fill's length from the last level, times the scale. As the marker, at it. */
	private fun Function.show() {
		function(style.read)
		raw("scoreboard players operation #gauge.v $T = @s $LAST")
		raw("scoreboard players operation #gauge.v $T *= #dsp.k $T")
		raw("execute store result entity $FILL transformation.scale[0] float ${"%.9f".format(java.util.Locale.ROOT, PER_LEVEL / 100)} run scoreboard players get #gauge.v $T")
		// The client eases from the old length to the new one over `interpolation_duration`.
		raw("data modify entity $FILL start_interpolation set value 0")
	}

	/** Everything from the properties, both displays put back if one is missing. As the marker, at it. */
	private fun Function.restyle(track: FunctionArgument, fill: FunctionArgument) {
		function(style.defaults)
		executeRun("unless entity $TRACK", summon)
		executeRun("unless entity $FILL", summon)
		raw("data modify entity @s data.data.style set from entity @s data.properties")

		concrete(FILL, "color", "red")
		concrete(TRACK, "track_color", "gray")
		function(style.turn)
		function(style.read)
		function(track)
		function(fill)
		function(show)
	}

	/** [target]'s block from the dye named by [prop]; [default] for anything else. */
	private fun Function.concrete(target: String, prop: String, default: String) {
		raw("""data modify entity $target block_state set value {id:"minecraft:${default}_concrete"}""")
		for (c in DYES) if (c != default) {
			raw("""execute if data entity @s data.properties{$prop:"$c"} run data modify entity $target block_state set value {id:"minecraft:${c}_concrete"}""")
		}
	}

	/** Both displays, new and empty; restyle shapes them. As the marker, at it. */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		val zero = "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}"
		summonDisplay("block_display", listOf(TAG, TRACK_TAG), """block_state:{id:"minecraft:gray_concrete"},$zero""")
		summonDisplay("block_display", listOf(TAG, FILL_TAG), """block_state:{id:"minecraft:red_concrete"},interpolation_duration:$GROW,$zero""")
	}

	/** Once a second, as the marker at its block: redraw if a display is gone or the properties changed. */
	private fun Function.poll() {
		executeReturnRun("unless entity $TRACK", restyle)
		executeReturnRun("unless entity $FILL", restyle)
		raw("execute store success score #gauge.ch $T run data modify entity @s data.data.style set from entity @s data.properties")
		executeRun("if score #gauge.ch $T matches 1", restyle)
	}
}
