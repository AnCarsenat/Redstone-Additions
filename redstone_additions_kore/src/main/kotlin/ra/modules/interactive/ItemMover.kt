package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.modules.infinite.finishLorePlacement
import ra.modules.infinite.loreItem
import ra.modules.infinite.loreLine
import ra.modules.infinite.registerBlockMessage
import ra.modules.interactive.ItemMoverIo.checkOutputCapacity
import ra.modules.interactive.ItemMoverIo.consumeSelectedInput
import ra.modules.interactive.ItemMoverIo.registerItemMoverIo
import ra.modules.interactive.ItemMoverIo.selectInput
import ra.modules.interactive.ItemMoverIo.tryInsert
import io.github.ayfri.kore.commands.scoreboard.scoreboard

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
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("CRC", "HOH", "CRC"),
			keys = listOf(
				"C" to listOf(Items.COBBLESTONE, Items.BLACKSTONE), "R" to listOf(Items.REDSTONE),
				"O" to listOf(Items.DISPENSER), "H" to listOf(Items.HOPPER),
			),
			unlock = has(Items.DISPENSER),
		)

		pack.define(block.handlePlacement) {
			placeGuard(block)
			// Dispenser with full 6-directional facing (same block family as the item pipe).
			placeBlock(block)
			finishLorePlacement(block)
		}
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		registerItemMoverIo()
		pack.define(process) { process() }

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropSpec(item)
			breakSound()
			breakCloud()
			kill(self())
		}

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
