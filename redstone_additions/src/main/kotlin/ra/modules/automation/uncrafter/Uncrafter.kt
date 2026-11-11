package ra.modules.automation.uncrafter

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
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
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.automation.AutomationClock
import ra.modules.automation.uncrafter.UncrafterRun.registerUncrafterRun

/**
 * Uncrafter: takes items back apart into the ingredients of their vanilla crafting recipe, for a
 * lot of EU. Reads the container above it, writes into the container below it, pays from a wire
 * or battery beside it. What it can take apart is [UncraftTable]; how one look goes is
 * [UncrafterRun].
 *
 * THE BLOCK. A grindstone standing on the floor, turned the way it was placed: the vanilla block
 * that undoes things (it takes enchantments off), and not a container, so a hopper above it keeps
 * its items for the Uncrafter to take. Right-clicking it still opens the vanilla grindstone.
 *
 * DATA. `data.properties.eu_per_uncraft`: EU per item taken apart (admin only; the default for new
 * Uncrafters is on the Automation settings page). `data.status.state`, `data.status.uncrafted`:
 * what the Goggles show.
 */
object Uncrafter {
	val block = CustomBlock(Ns.AUTOMATION, "uncrafter", "Uncrafter", model = "minecraft:grindstone", dirType = 1)
	val item = block.loreItem {
		lore(
			loreLine("Takes items back apart into their ingredients", "gray"),
			loreLine("Input: the container above. Output: the one below", "dark_gray"),
			loreLine("Uses EU from a wire or battery beside it", "dark_gray"),
		)
	}

	const val DEFAULT_EU = 2000
	const val MAX_EU = 100_000

	val registerBlock = block.fn("register_block")
	val run get() = UncrafterRun.run

	fun Ra.registerUncrafter() {
		block(block)
		properties(block) {
			number("eu_per_uncraft", min = 1, max = MAX_EU, adminOnly = true, default = DEFAULT_EU)
		}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		// Machine-shaped: the crafter it reverses in the middle, the grindstone that grinds it back
		// down on top, redstone below, an iron frame.
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.CRAFTER)) {
			pattern("IGI", "ICI", "IRI")
			key("I", Items.IRON_INGOT)
			key("G", Items.GRINDSTONE)
			key("C", Items.CRAFTER)
			key("R", Items.REDSTONE_BLOCK)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			// Placed with a horizontal facing on its wall face; stood on the floor, the same way round.
			for (f in listOf("north", "south", "east", "west")) {
				raw("execute if block ~ ~ ~ minecraft:grindstone[face=wall,facing=$f] run setblock ~ ~ ~ minecraft:grindstone[face=floor,facing=$f]")
			}
			setOnNew(block, "data.properties", "{eu_per_uncraft:$DEFAULT_EU}")
			setOnNew(block, "data.status", """{state:"waiting for items",uncrafted:0}""")
			raw("tag ${block.newMarkerSel()} add ${AutomationClock.TAG}")
			finishPlacement(block)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("uncrafted", "Uncrafted: ", "yellow")
			stackedPropLine("eu_per_uncraft", "Per item: ", "aqua", " EU")
			stackedTextLine("", "Input above, output below", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.GRINDSTONE)) {}

		registerUncrafterRun()
	}
}
