package ra.modules.displays

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.floorMod
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.computeScore
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.modules.displays.DisplayEntities.killDisplays
import ra.modules.displays.DisplayEntities.sel
import ra.modules.displays.DisplayEntities.summonDisplay
import ra.modules.displays.DisplayPoll.startIfIdle
import ra.modules.displays.DisplayStyle.Companion.defineForms
import ra.modules.displays.TextColor.applyColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Item Hologram: a barrel that shows the first item inside it floating out of its lid, turning
 * slowly, with its name over it.
 *
 * A barrel, because a player fills it by hand and a hopper or an Item Pipe fills it like any
 * container, it breaks by hand and spills what it holds, and it fires nothing on a redstone edge.
 * The first item is `Items[0]`: the list only holds occupied slots, in slot order.
 *
 * NOTHING PER TICK. The tick only starts the module's clock ([DisplayPoll]); once a second each
 * hologram ([poll]) compares its first item, without slot and count, with the copy it last showed
 * (`data.data.shown`), and redraws only on a difference, or restyles when its properties changed
 * (`data.data.style`). A display killed by hand is put back by the same pass.
 *
 * THE SPIN is the client's. Each run sets the item's `left_rotation` one step on, with an
 * interpolation as long as the clock's period, so the client turns it smoothly between runs: one
 * command per hologram per second. The step is 90 degrees, and the phase comes from the game time
 * modulo EIGHT steps (720 degrees) rather than four: a quaternion and its negative are the same
 * turn, and walking the double cover keeps every two consecutive targets on the same side, so
 * the interpolation never takes the short way round backwards.
 *
 * CONFIGURATION ([DisplayStyle]): `facing` is the face the item floats out of (the barrel's lid
 * follows it); the displays stay upright and only turn their yaw. `direction` is where a still
 * item (spin off) faces: `front` (out of the face), `right`, `back`, `left`. `color` is the
 * name's colour (a text colour name or `#RRGGBB`, see [TextColor]). Offsets and scale as every
 * display. `spin` and `show_name` are block properties too: a display is the same for everyone.
 */
object ItemHologram : DisplayBlock {
	override val block = CustomBlock(Ns.DISPLAYS, "item_hologram", "Item Hologram", model = "minecraft:barrel")
	override val item = block.loreItem { lore(loreLine("Shows the first item inside it, floating above", "gray")) }

	override val poll = block.fn("poll")
	private val summon = block.fn("summon")
	private val show = block.fn("show")
	private val showName = block.fn("show_name")
	private val spin = block.fn("spin")
	private val restyle = block.fn("restyle")
	val toggleSpin = block.fn("toggle_spin")
	val toggleName = block.fn("toggle_name")

	private const val TAG = "ra.displays.holo"
	private const val ITEM_TAG = "ra.displays.holo_item"
	private const val NAME_TAG = "ra.displays.holo_name"
	private val ITEM = sel("item_display", ITEM_TAG)
	private val NAME = sel("text_display", NAME_TAG)

	private val style = DisplayStyle(
		block, defaultFacing = "up", directions = listOf("front", "right", "back", "left"),
		colors = listOf(ColorProp("color", "Name colour", "white", TextColor.NAMES)),
		displays = listOf(ITEM, NAME), yawOnly = true, barrel = true,
	)

	/** A still item's turn about the vertical, counter-clockwise seen from above. */
	private val TURNS = mapOf("front" to 0, "right" to 270, "back" to 180, "left" to 90)

	/** Where the item and its name float, per facing, in the yaw-turned frame (z out of a side). */
	private class Spot(val face: Vec, val item: Vec, val name: Vec)
	private val SIDE = Spot(Vec(0.0, 0.0, 0.5), Vec(0.0, 0.0, 1.0), Vec(0.0, 0.45, 1.0))
	private val SPOTS = mapOf(
		"up" to Spot(Vec(0.0, 0.5, 0.0), Vec(0.0, 1.0, 0.0), Vec(0.0, 1.45, 0.0)),
		"down" to Spot(Vec(0.0, -0.5, 0.0), Vec(0.0, -1.2, 0.0), Vec(0.0, -0.75, 0.0)),
	) + listOf("north", "south", "west", "east").associateWith { SIDE }

	private const val T = Obj.TEMP
	private const val PHASE = "#holo.p"
	private const val CHANGED = "#holo.ch"

	override fun Ra.registerBlock() {
		block(block)
		properties(block) {
			bool("spin", default = true)
			bool("show_name", default = true)
			with(style) { declare() }
		}
		wrenchCycle(block.id, WrenchCycle("Spin", "spin", toggleSpin), WrenchCycle("Name", "show_name", toggleName), *style.wrenchRows())
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.AMETHYST_SHARD), count = null) {
			pattern(" G ", "APA", " B ")
			key("G", Items.GLASS)
			key("A", Items.AMETHYST_SHARD)
			key("P", Items.ENDER_PEARL)
			key("B", Items.BARREL)
		}

		// As the bat, at it.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			// Lid up, under the item. A fresh barrel is empty, so replacing it loses nothing.
			raw("fill ~ ~ ~ ~ ~ ~ minecraft:barrel[facing=up] replace minecraft:barrel")
			raw("data modify entity ${block.newMarkerSel()} data.properties set value {spin:1b,show_name:1b,${style.placementSnbt()}}")
			executeRun("as ${block.newMarkerSel()} at @s", poll)
			finishPlacement(block, removePlaceTag = false)
		}

		with(style) { registerStyle(restyle) }
		val itemForm = defineForms(block, "item", ITEM, SPOTS.mapValues { (_, s) -> Form(s.item, Vec(0.6, 0.6, 0.6), s.face) }, prop = "facing", keepLeft = true)
		val nameForm = defineForms(block, "name", NAME, SPOTS.mapValues { (_, s) -> Form(s.name, Vec(0.75, 0.75, 0.75), s.face) }, prop = "facing")

		pack.define(poll) { poll() }
		pack.define(summon) { summon() }
		pack.define(show) { show() }
		pack.define(restyle) { restyle(itemForm, nameForm) }
		pack.define(showName) {
			raw("data modify storage ra:temp dsp.item set from entity @s data.data.shown")
			function(ItemName.build)
			// Wrapped, so the name takes the colour unless it has one of its own (an anvil name can).
			raw("""data modify storage ra:temp dsp.text set value {text:"",extra:[]}""")
			raw("data modify storage ra:temp dsp.text.color set from entity @s data.data.color")
			raw("data modify storage ra:temp dsp.text.extra append from storage ra:temp dsp.name")
			raw("data modify entity $NAME text set from storage ra:temp dsp.text")
		}
		pack.define(spin) { spin() }
		pack.define(toggleSpin) {
			toggle("spin", "Spin")
			function(restyle)
		}
		pack.define(toggleName) {
			toggle("show_name", "Name")
			function(restyle)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("spin", "Spin: ", "aqua", y = 0.8)
			propLine("show_name", "Name: ", "aqua", y = 0.6)
			propLine("facing", "Facing: ", "aqua", y = 0.4)
			propLine("color", "Colour: ", "aqua", y = 0.2)
		}

		// The barrel spills what it holds, as any barrel does.
		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.wood.break", cloud = true, cleanup = { killDisplays(TAG) })
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.BARREL)) { startIfIdle(block) }
	}

	/** Once a second, as the marker at its block. */
	private fun Function.poll() {
		executeRun("unless entity $ITEM", restyle)
		executeRun("unless entity $NAME", restyle)

		raw("data remove storage ra:temp dsp.item")
		raw("data modify storage ra:temp dsp.item set from block ~ ~ ~ Items[0]")
		// What it looks like, not where it is or how many: moving the stack redraws nothing.
		raw("data remove storage ra:temp dsp.item.Slot")
		raw("data remove storage ra:temp dsp.item.count")
		raw("scoreboard players set $CHANGED $T 0")
		raw("execute if data storage ra:temp dsp.item store success score $CHANGED $T run data modify entity @s data.data.shown set from storage ra:temp dsp.item")
		raw("execute unless data storage ra:temp dsp.item store success score $CHANGED $T run data remove entity @s data.data.shown")
		// Placed before the display settings existed: restyle fills the defaults (scale, colours).
		executeRun("unless data entity @s data.properties.scale", restyle)
		raw("execute store success score #holo.st $T run data modify entity @s data.data.style set from entity @s data.properties")
		executeRun("if score #holo.st $T matches 1", restyle)
		executeRun("if score $CHANGED $T matches 1", show)

		executeRun("if data entity @s data.properties{spin:1b}", spin)
	}

	/** Everything from the properties; both displays put back if one is missing. As the marker, at it. */
	private fun Function.restyle(itemForm: FunctionArgument, nameForm: FunctionArgument) {
		function(style.defaults)
		executeRun("unless entity $ITEM", summon)
		executeRun("unless entity $NAME", summon)
		raw("data modify entity @s data.data.style set from entity @s data.properties")

		function(style.turn)
		// Over the lid or under the bottom the name turns to whoever looks; on a side it faces out.
		raw("""data modify entity $NAME billboard set value "fixed"""")
		raw("""execute if data entity @s data.properties{facing:"up"} run data modify entity $NAME billboard set value "vertical"""")
		raw("""execute if data entity @s data.properties{facing:"down"} run data modify entity $NAME billboard set value "vertical"""")
		applyColor("color", "white")
		function(style.read)
		function(itemForm)
		function(nameForm)

		// A spinning item keeps its phase; a still one faces its direction.
		executeRun("if data entity @s data.properties{spin:1b}", spin)
		for ((d, deg) in TURNS) {
			val q = Form(Vec(0.0, 0.0, 0.0), Vec(0.0, 0.0, 0.0), Vec(0.0, 0.0, 0.0), deg).quaternion()
			// The same numbers about y instead of z.
			raw("""execute unless data entity @s data.properties{spin:1b} if data entity @s data.properties{direction:"$d"} run data merge entity $ITEM {transformation:{left_rotation:[0f,${q[2]}f,0f,${q[3]}f]}}""")
		}
		function(show)
	}

	/** The item and its name from `data.data.shown`. As the marker, at it. */
	private fun Function.show() {
		raw("execute if data entity @s data.data.shown run data modify entity $ITEM item set from entity @s data.data.shown")
		raw("execute unless data entity @s data.data.shown run data remove entity $ITEM item")
		raw("""data modify entity $NAME text set value """"")
		executeRun("if data entity @s data.properties{show_name:1b} if data entity @s data.data.shown", showName)
	}

	/** Both displays, new, and nothing remembered as shown, so this pass draws them. As the marker, at it. */
	private fun Function.summon() {
		raw("kill @e[tag=$TAG,distance=..0.01]")
		val zero = "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0f,0f,0f]}"
		// No culling box (width/height 0): the item floats out of any face, below the block too.
		summonDisplay("item_display", listOf(TAG, ITEM_TAG), """item_display:"fixed",interpolation_duration:${DisplayPoll.PERIOD},$zero""")
		summonDisplay("text_display", listOf(TAG, NAME_TAG), """text:"",billboard:"vertical",background:0,shadow:1b,$zero""")
		raw("data remove entity @s data.data.shown")
	}

	/** One 90-degree step towards the phase of the game time. As the marker, at it. */
	private fun Function.spin() {
		raw("execute store result score $PHASE $T run time query gametime")
		computeScore(PHASE, T, provider { scoreOf(PHASE, T).floorDiv(DisplayPoll.PERIOD).floorMod(8) })
		for (p in 0 until 8) {
			val half = Math.toRadians(p * 45.0)
			val q = "[0f,${f(sin(half))}f,0f,${f(cos(half))}f]"
			raw("execute if score $PHASE $T matches $p run data merge entity $ITEM {transformation:{left_rotation:$q},start_interpolation:0}")
		}
	}

	private fun f(x: Double) = "%.4f".format(java.util.Locale.ROOT, x).let { if (it == "-0.0000") "0.0000" else it }

	/** Wrench: flip a bool property. Read first, so the write is never tested again. As the marker, at it. */
	private fun Function.toggle(prop: String, label: String) {
		val m = "#holo.t $T"
		raw("execute store result score $m run data get entity @s data.properties.$prop")
		raw("execute if score $m matches 1.. run data modify entity @s data.properties.$prop set value 0b")
		raw("execute if score $m matches ..0 run data modify entity @s data.properties.$prop set value 1b")
		raw("""execute if score $m matches 1.. run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name} $label: ",color:"gray"},{text:"off",color:"red"}]""")
		raw("""execute if score $m matches ..0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name} $label: ",color:"gray"},{text:"on",color:"green"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
	}
}
