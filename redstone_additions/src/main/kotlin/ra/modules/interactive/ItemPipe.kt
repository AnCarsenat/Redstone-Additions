package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.block.stackReset
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.modules.interactive.ItemPipeTransfer.registerItemPipeTransfer
import io.github.ayfri.kore.commands.tag
import ra.core.block.breakCheck
import ra.core.block.finishPlacement
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.executeStoreRun
import ra.core.block.forEachMarker
import ra.core.block.breakSound
import ra.core.block.breakCloud
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.markerSel
import ra.core.block.newMarkerSel
import ra.core.block.setOnNew
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Item Pipe: a dispenser that pushes its first stack into the container in front, or into a side
 * container when the stack matches the pipe's `filter_item`. The moving itself is in
 * [ItemPipeTransfer].
 */
object ItemPipe {
	val block = CustomBlock(Ns.INTERACTIVE, "item_pipe", "Item Pipe", model = "minecraft:dispenser", dirType = 2)
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")

	/** Set while a pipe has run this tick, so a chain never visits the same pipe twice. */
	private const val VISITED = "ra.pipe_chain_visited"

	fun Ra.registerItemPipe() {
		block(block)
		properties(block) {
			item("filter_item")
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.HOPPER), count = 8) {
			pattern("CCC", "IHI", "CCC")
			key("I", listOf(Items.COPPER_INGOT))
			key("H", listOf(Items.HOPPER))
			key("C", listOf(Items.COBBLESTONE))
		}

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) { goggles() }
		val (checkFilter, transfer) = registerItemPipeTransfer()
		pack.define(process) { process(checkFilter, transfer) }

		// Hand-composed rather than blockOnBreak: the pipe leaves its network AFTER the break
		// effects, and blockOnBreak runs its cleanup first.
		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropFirstSlot()
			dropItem(item)
			breakSound()
			breakCloud()
			function(RaLib.Transport.Net.leave)
			kill(self())
		}

		pack.define(block.tick) {
			// Keep marker anchors snapped to centred block coordinates.
			raw("execute as ${block.markerSel()} at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~")

			breakCheck(block, Blocks.DISPENSER, onBreak)

			// Clear the per-tick chain traversal tags.
			raw("tag ${block.markerSel("tag=$VISITED")} remove $VISITED")

			forEachMarker(block, process)
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		placeBlock(block)

		val fresh = block.newMarkerSel()

		// Snap the marker to centred world-grid coordinates.
		raw("execute as $fresh at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~")

		// Item pipes join the shared transport engine as an item-class network, so a connected run
		// is one network rather than a chain of independent hops.
		executeRun("as $fresh", RaLib.Transport.Net.join, "{class:\"item\",capacity:0}")

		// Seeded EMPTY rather than left absent. The Data Handler draws a row only for a property the
		// block actually has -- props/render returns before drawing when the name is missing -- so
		// an absent filter_item meant a pipe with no way to set one. Empty string is the
		// "no filter" value, which check_filter tests for.
		setOnNew(block, "data.properties.filter_item", "\"\"")

		finishPlacement(block, removePlaceTag = false, withinBlock = false)
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.0)
		stackReset(80, 22)
		val none = "{label:\"Filter: \",value:\"None\",color:\"gray\",suffix:\"\",y:0.55}"
		executeRun("unless data entity @s data.properties.filter_item", Goggles.Billboard.textLine, none)
		executeRun("if data entity @s data.properties{filter_item:\"\"}", Goggles.Billboard.textLine, none)
		executeRun(
			"if data entity @s data.properties.filter_item unless data entity @s data.properties{filter_item:\"\"}",
			Goggles.Billboard.stackedPropLine, "{path:\"filter_item\",label:\"Filter: \",color:\"aqua\",suffix:\"\"}",
		)

		// And the item itself, above the text. The id names it; this shows it, which is the
		// difference between reading "minecraft:polished_blackstone_slab" and recognising a sorting
		// wall at a glance.
		raw("data remove storage ra:temp pipefilter")
		raw("execute unless data entity @s data.properties{filter_item:\"\"} run data modify storage ra:temp pipefilter.id set from entity @s data.properties.filter_item")
		raw("data modify storage ra:temp pipefilter.y set value 0.55")
		executeRun("if data storage ra:temp pipefilter.id", Goggles.Billboard.itemLine, "with storage ra:temp pipefilter")
	}

	/**
	 * Move the pipe's first stack on. As the marker (rotated to face the output), at the
	 * dispenser. A stack matching the filter goes to a side container; anything else, or a
	 * filtered stack no side would take, goes forward; with no valid output it stays in the pipe.
	 */
	private fun Function.process(checkFilter: FunctionArgument, transfer: FunctionArgument) {
		// Never revisit the same pipe within one chain tick.
		raw("execute if entity @s[tag=$VISITED] run return 0")
		tag(self()) { add(VISITED) }

		raw("execute unless data block ~ ~ ~ Items[0] run return 0")
		raw("data modify storage ra:temp pipe_item set from block ~ ~ ~ Items[0]")

		executeStoreRun("", "@s", checkFilter)
		raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")

		// Not filtered to a side: forward (^ ^ ^1).
		executeStoreRun("positioned ^ ^ ^1 if block ~ ~ ~ #ra_lib:containers", "@s", transfer)
		raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")

		// No valid output: keep the item in the pipe.
		returnValue(0)
	}
}
