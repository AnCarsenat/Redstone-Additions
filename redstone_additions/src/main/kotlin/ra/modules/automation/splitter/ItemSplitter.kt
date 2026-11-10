package ra.modules.automation.splitter

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Ra
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.automation.AutomationClock
import ra.modules.automation.splitter.ItemSplitterRun.registerItemSplitterRun

/**
 * Item Pipe Splitter: takes items out of the container above it and shares them out between the
 * containers on its four sides and under it, one item at a time, round the sides in turn.
 *
 * Not a container itself (chiseled tuff), so nothing is ever pushed into it: a hopper or an Item
 * Pipe pointing into it from above holds its items, and the splitter takes them out. That also
 * means it can never send an item back where it came from: the input is above, the outputs are
 * the other five faces.
 *
 * SHARES. Each output face has a weight, `north`, `east`, `south`, `west`, `down` (0 to
 * [MAX_SHARE], default 1): the splitter sends that many items in a row to one face before it moves
 * on to the next. 1 everywhere is a plain round robin, 0 switches a face off, `north` 2 and `south`
 * 1 sends two thirds north. A face with no container, or one that is full, is skipped and its
 * turn passes on, so nothing waits on a full output while another has room.
 *
 * SPEED. `per_second` items a second (1 to [MAX_RATE], admins up to [ADMIN_MAX_RATE]), moved in one
 * go on the automation clock.
 *
 * DATA. `data.data.face` (0 north, 1 east, 2 south, 3 west, 4 down) and `data.data.sent` (items sent
 * to it this turn): where the round robin is. `data.status.state`, `data.status.split`: Goggles.
 */
object ItemSplitter {
	val block = CustomBlock(Ns.AUTOMATION, "item_pipe_splitter", "Item Pipe Splitter", model = "minecraft:chiseled_tuff")
	val item = block.loreItem {
		lore(
			loreLine("Shares out the items of the container above it", "gray"),
			loreLine("between the containers on its sides and below", "gray"),
			loreLine("Data Handler: the share of each side", "dark_gray"),
		)
	}

	/** The output faces, in turn order: property name and offset. */
	val faces = listOf("north" to "~ ~ ~-1", "east" to "~1 ~ ~", "south" to "~ ~ ~1", "west" to "~-1 ~ ~", "down" to "~ ~-1 ~")

	const val MAX_SHARE = 16
	const val DEFAULT_RATE = 8
	const val MAX_RATE = 16
	const val ADMIN_MAX_RATE = 64

	val registerBlock = block.fn("register_block")
	val run get() = ItemSplitterRun.run
	private val cycleRate = block.fn("cycle_rate")

	fun Ra.registerItemSplitter() {
		block(block)
		properties(block) {
			for ((face, _) in faces) number(face, min = 0, max = MAX_SHARE, default = 1)
			number("per_second", min = 1, max = MAX_RATE, adminMax = ADMIN_MAX_RATE, default = DEFAULT_RATE)
		}
		wrenchCycle(block.id, WrenchCycle("Speed", "per_second", cycleRate))
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// A hopper on top, where the items come in, over a comparator that keeps count, in the
		// iron of a hopper's own funnel.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.HOPPER)) {
			pattern(" H ", "ICI", "I I")
			key("H", Items.HOPPER)
			key("I", Items.IRON_INGOT)
			key("C", Items.COMPARATOR)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			val props = (faces.map { "${it.first}:1" } + "per_second:$DEFAULT_RATE").joinToString(",", "{", "}")
			setOnNew(block, "data.properties", props)
			setOnNew(block, "data.data", "{face:0,sent:0}")
			setOnNew(block, "data.status", """{state:"waiting for items",split:0}""")
			raw("tag ${block.newMarkerSel()} add ${AutomationClock.TAG}")
			finishPlacement(block)
		}

		// Shift+RMB with the Wrench: 2, 4, 8, 16 items a second and round; anything else goes back
		// to 2. As the marker, at it.
		pack.define(cycleRate) {
			readCappedProperty("per_second", default = DEFAULT_RATE, min = 1, adminMax = ADMIN_MAX_RATE)
			raw("scoreboard players set #sp.r ra.temp 2")
			for (r in listOf(2, 4, 8)) raw("execute if score #prop ra.temp matches $r run scoreboard players set #sp.r ra.temp ${r * 2}")
			raw("execute store result entity @s data.properties.per_second int 1 run scoreboard players get #sp.r ra.temp")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.properties.per_second",entity:"@s",color:"aqua"},{text:" items a second",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedPropLine("per_second", "Speed: ", "aqua", " items/s")
			stackedStatusLine("split", "Moved: ", "yellow", " items")
			stackedTextLine("", "Input above; sides and below share", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.CHISELED_TUFF)) {}

		registerItemSplitterRun()
	}
}
