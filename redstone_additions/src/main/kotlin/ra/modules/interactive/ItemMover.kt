package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.modules.interactive.ItemMoverIo.checkOutputCapacity
import ra.modules.interactive.ItemMoverIo.consumeSelectedInput
import ra.modules.interactive.ItemMoverIo.registerItemMoverIo
import ra.modules.interactive.ItemMoverIo.selectInput
import ra.modules.interactive.ItemMoverIo.tryInsert
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.block.forEachMarker
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Item Mover: moves the first stack of the container behind it (`^ ^ ^-1`) into the container in
 * front (`^ ^ ^1`), at hopper rate. Double chests count as one container on either side.
 *
 * Its container plumbing ([ItemMoverIo]) is also what the Boxer and Unboxer move items with.
 */
object ItemMover {
	val block = CustomBlock(Ns.INTERACTIVE, "item_mover", "Item Mover", model = "minecraft:dispenser", dirType = 2)
	val item = block.loreItem { lore(loreLine("Moves items from back to front", "gray")) }
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")

	fun Ra.registerItemMover() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DISPENSER), count = null) {
			pattern("CRC", "HOH", "CRC")
			key("C", listOf(Items.COBBLESTONE, Items.BLACKSTONE))
			key("R", listOf(Items.REDSTONE))
			key("O", listOf(Items.DISPENSER))
			key("H", listOf(Items.HOPPER))
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Dispenser with full 6-directional facing (same block family as the item pipe).
			placeBlock(block)
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		registerItemMoverIo()
		pack.define(process) { process() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DISPENSER, onBreak)
			forEachMarker(block, process)
		}
	}

	/** One mover. As its marker (facing the output), at the dispenser. Hopper rate: every 4 ticks. */
	private fun Function.process() {
		hopperCooldown()

		// A container behind (input) and one in front (output).
		raw("execute positioned ^ ^ ^-1 unless block ~ ~ ~ #ra_lib:containers run return 0")
		raw("execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_lib:containers run return 0")

		// The input stack, from the primary input or its double-chest partner.
		executeRun("positioned ^ ^ ^-1", selectInput)
		raw("execute if score #mover_has_input ${Obj.TEMP} matches 0 run return 0")

		// There is work to do.
		scoreboard.players.set(self(), Obj.COOLDOWN, 0)

		// The whole selected stack moves (the insert may still be partial if the output has
		// limited space). The macro needs a components field.
		raw("execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}")

		// The output side cannot take even one item: leave the input as it is.
		executeRun("positioned ^ ^ ^1", checkOutputCapacity)
		raw("execute if score #mover_can_forward ${Obj.TEMP} matches 0 run return 0")

		executeRun("positioned ^ ^ ^1", tryInsert, "with storage ra:temp mover_item")

		// Consume only what was inserted, from the exact input half it was selected from.
		raw("execute if score #mover_inserted ${Obj.TEMP} matches 0 run return 0")
		executeRun("positioned ^ ^ ^-1", consumeSelectedInput)
	}
}
