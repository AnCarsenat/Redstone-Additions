package ra.modules.farming.tree

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
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
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.farming.tree.TreeFarmRun.registerTreeFarmRun
import ra.modules.wires.machine.MachineClock

/**
 * Tree Farm: plants a sapling beside it, waits for the tree (or grows it with bone meal), fells
 * the whole tree and puts the wood, saplings, sticks and apples into the container below it.
 *
 * INPUT ABOVE, OUTPUT BELOW, like the Uncrafter. Saplings and bone meal come from the container on
 * top of it; the drops go into the container under it. The tree grows on one of its four sides:
 * the first free block standing on dirt or grass, which it then keeps until the tree is down.
 * What it grows and fells is [TreeKinds]; how a second goes is [TreeFarmRun].
 *
 * DATA. `data.properties.bone_meal`: whether it uses bone meal from the container above.
 * `data.data.side`, `data.data.kind`: the spot and the sapling it planted there, while a tree is
 * on the way. `data.status.state`, `data.status.felled`: what the Goggles show.
 *
 * CHEAP WHEN IDLE. Per tick only the break check every block has. The rest runs once a second on
 * the network machines' clock ([MachineClock]); with nothing to plant a run is a redstone read
 * and one item test.
 */
object TreeFarm {
	const val BLOCK = "minecraft:bamboo_mosaic"

	val block = CustomBlock(Ns.FARMING, "tree_farm", "Tree Farm", model = BLOCK)
	val item = block.loreItem {
		lore(
			loreLine("Plants saplings beside it and fells the grown tree", "gray"),
			loreLine("Saplings and bone meal: the container above", "dark_gray"),
			loreLine("Wood, saplings and sticks: the container below", "dark_gray"),
		)
	}
	val registerBlock = block.fn("register_block")

	/** As the marker, at it, once a second from [MachineClock]. */
	val run get() = TreeFarmRun.run

	fun Ra.registerTreeFarm() {
		block(block)
		properties(block) {
			bool("bone_meal", default = true, goggles = false)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// Machine-shaped: a log frame, the axe on top, the hopper that collects in the middle,
		// redstone below.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.IRON_AXE)) {
			pattern("LAL", "LHL", "LRL")
			key("L", listOf(
				Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG,
				Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.PALE_OAK_LOG, Items.POPLAR_LOG,
			))
			key("A", Items.IRON_AXE)
			key("H", Items.HOPPER)
			key("R", Items.REDSTONE)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", "{bone_meal:1b}")
			setOnNew(block, "data.status", """{state:"needs saplings above",felled:0}""")
			raw("tag ${block.newMarkerSel()} add ${MachineClock.TAG}")
			finishPlacement(block)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("felled", "Trees felled: ", "yellow")
			stackedTextLine("", "Saplings above, drops below", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.bamboo_wood.break", cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.BAMBOO_MOSAIC)) {}

		registerTreeFarmRun()
	}
}
