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
import ra.core.WrenchCycle
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
import ra.core.block.soundListenersSel
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

/**
 * Gauge Bar: a bar on its front face, filled to the redstone level feeding it (0-15).
 *
 * INPUT. The strongest level on any of its six sides; a repeater or comparator pointing in reads
 * as 15. Each side is first asked `any_side` (stops at the first source) and only a powered side
 * has its level resolved, so an unpowered gauge costs six `any_side` reads a tick, like a gate.
 *
 * DRAWING. Two block displays standing on the marker, turned the way the block faces (the
 * marker's Rotation, so all six facings work): a grey track and a coloured fill in front of it.
 * Only the fill's length changes, and only when the level does: the last level drawn is the
 * marker's `ra.power` score (this block uses it for nothing else), and a new one is one
 * `store result` into `transformation.scale`, interpolated over [GROW] ticks by the client.
 *
 * STYLE. `color` (a name, see [COLORS]; anything else draws red) and `vertical`. The wrench and
 * placement redraw at once; a Data Handler edit, or a display killed by hand, is picked up by
 * [poll] within a second: it compares `data.properties` with the copy last drawn
 * (`data.data.style`).
 */
object GaugeBar : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "gauge_bar", "Gauge Bar", model = "minecraft:polished_blackstone", dirType = 3)
	override val item = block.loreItem { lore(loreLine("Shows the redstone level feeding it as a bar", "gray")) }

	private val process = block.fn("process")
	private val show = block.fn("show")
	private val restyle = block.fn("restyle")
	private val summon = block.fn("summon")
	override val poll = block.fn("poll")
	val cycleColor = block.fn("cycle_color")
	val cycleOrientation = block.fn("cycle_orientation")

	/** The fill's colour names, in wrench order; the first is the default and the fallback. */
	val COLORS = listOf("red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink", "white")

	private const val TAG = "ra.displays.gauge"
	private const val TRACK_TAG = "ra.displays.gauge_track"
	private const val FILL_TAG = "ra.displays.gauge_fill"
	private val TRACK = sel("block_display", TRACK_TAG)
	private val FILL = sel("block_display", FILL_TAG)

	/** Ticks the fill takes to grow or shrink to a new level. */
	private const val GROW = 3

	/** Fill length per level: 13/16 of a block at 15. */
	private const val PER_LEVEL = 0.0541667

	private const val T = Obj.TEMP
	private const val LEVEL = "#gauge.l"
	private const val LAST = Obj.POWER
	private const val VERTICAL = "data.properties{vertical:1b}"

	private fun any(s: Side) = block.fn("any_${s.id}")
	private fun level(s: Side) = block.fn("level_${s.id}")

	override fun Ra.registerBlock() {
		block(block)
		properties(block) {
			text("color", default = COLORS.first())
			bool("vertical", default = false)
		}
		wrenchCycle(block.id, WrenchCycle("Colour", "color", cycleColor), WrenchCycle("Vertical", "vertical", cycleOrientation))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.COMPARATOR), count = null) {
			pattern("GGG", "RCR", "BBB")
			key("G", Items.GLASS_PANE)
			key("R", Items.REDSTONE)
			key("C", Items.COMPARATOR)
			key("B", Items.POLISHED_BLACKSTONE)
		}

		// As the bat, at it.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			raw("data modify entity ${block.newMarkerSel()} data.status.facing set from storage ra:temp facing_name")
			raw("""data modify entity ${block.newMarkerSel()} data.properties set value {color:"${COLORS.first()}",vertical:0b}""")
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
		pack.define(process) { process() }
		pack.define(show) { show() }
		pack.define(restyle) { restyle() }
		pack.define(summon) { summon() }
		pack.define(poll) { poll() }
		pack.define(cycleColor) { cycleColor() }
		pack.define(cycleOrientation) { cycleOrientation() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("level", "Level: ", "red", y = 0.8)
			propLine("color", "Colour: ", "aqua", y = 0.6)
			propLine("vertical", "Vertical: ", "aqua", y = 0.4)
			dataLine("facing", "Facing: ", "aqua", y = 0.2)
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

	/** The fill's length from the last level, along the bar's axis. As the marker, at it. */
	private fun Function.show() {
		raw("execute if data entity @s $VERTICAL store result entity $FILL transformation.scale[1] float $PER_LEVEL run scoreboard players get @s $LAST")
		raw("execute unless data entity @s $VERTICAL store result entity $FILL transformation.scale[0] float $PER_LEVEL run scoreboard players get @s $LAST")
		// The client eases from the old length to the new one over `interpolation_duration`.
		raw("data modify entity $FILL start_interpolation set value 0")
	}

	/** Colour and orientation from the properties, both displays put back if one is missing. As the marker, at it. */
	private fun Function.restyle() {
		executeRun("unless entity $TRACK", summon)
		executeRun("unless entity $FILL", summon)
		raw("data modify entity @s data.data.style set from entity @s data.properties")

		raw("""data modify entity $FILL block_state set value {id:"minecraft:${COLORS.first()}_concrete"}""")
		for (c in COLORS.drop(1)) {
			raw("""execute if data entity @s data.properties{color:"$c"} run data modify entity $FILL block_state set value {id:"minecraft:${c}_concrete"}""")
		}

		// The fill's length along the bar is left at 0 here and set by show.
		raw("execute if data entity @s $VERTICAL run data merge entity $TRACK {transformation:{translation:[-0.125f,-0.4375f,0.5f],scale:[0.25f,0.875f,0.03125f]}}")
		raw("execute if data entity @s $VERTICAL run data merge entity $FILL {transformation:{translation:[-0.09375f,-0.40625f,0.5f],scale:[0.1875f,0f,0.0625f]}}")
		raw("execute unless data entity @s $VERTICAL run data merge entity $TRACK {transformation:{translation:[-0.4375f,-0.125f,0.5f],scale:[0.875f,0.25f,0.03125f]}}")
		raw("execute unless data entity @s $VERTICAL run data merge entity $FILL {transformation:{translation:[-0.40625f,-0.09375f,0.5f],scale:[0f,0.1875f,0.0625f]}}")
		function(show)
	}

	/**
	 * Both displays, new. As the marker, at it. Local +z is out of the front face (the marker's
	 * Rotation is the facing), +x to the viewer's right, so the bar is drawn on that face.
	 */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		val rot = "left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]"
		summonDisplay("block_display", listOf(TAG, TRACK_TAG),
			"""block_state:{id:"minecraft:gray_concrete"},transformation:{$rot,translation:[-0.4375f,-0.125f,0.5f],scale:[0.875f,0.25f,0.03125f]}""", rotated = true)
		summonDisplay("block_display", listOf(TAG, FILL_TAG),
			"""block_state:{id:"minecraft:${COLORS.first()}_concrete"},interpolation_duration:$GROW,transformation:{$rot,translation:[-0.40625f,-0.09375f,0.5f],scale:[0f,0.1875f,0.0625f]}""", rotated = true)
	}

	/** Once a second, as the marker at its block: redraw if a display is gone or the properties changed. */
	private fun Function.poll() {
		executeReturnRun("unless entity $TRACK", restyle)
		executeReturnRun("unless entity $FILL", restyle)
		raw("execute store success score #gauge.ch $T run data modify entity @s data.data.style set from entity @s data.properties")
		executeRun("if score #gauge.ch $T matches 1", restyle)
	}

	/** Wrench: the next colour. Read into a score first, so a write is never tested again. As the marker, at it. */
	private fun Function.cycleColor() {
		val m = "#gauge.c $T"
		raw("scoreboard players set $m 0")
		COLORS.forEachIndexed { i, c ->
			if (i > 0) raw("""execute if data entity @s data.properties{color:"$c"} run scoreboard players set $m $i""")
		}
		COLORS.forEachIndexed { i, _ ->
			val next = COLORS[(i + 1) % COLORS.size]
			raw("""execute if score $m matches $i run data modify entity @s data.properties.color set value "$next"""")
			raw("""execute if score $m matches $i run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"$next",color:"aqua"}]""")
		}
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		function(restyle)
	}

	/** Wrench: horizontal <-> vertical. As the marker, at it. */
	private fun Function.cycleOrientation() {
		val m = "#gauge.v $T"
		raw("execute store result score $m run data get entity @s data.properties.vertical")
		raw("execute if score $m matches 1.. run data modify entity @s data.properties.vertical set value 0b")
		raw("""execute if score $m matches 1.. run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"horizontal",color:"aqua"}]""")
		raw("execute if score $m matches ..0 run data modify entity @s data.properties.vertical set value 1b")
		raw("""execute if score $m matches ..0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"vertical",color:"aqua"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		function(restyle)
	}
}
