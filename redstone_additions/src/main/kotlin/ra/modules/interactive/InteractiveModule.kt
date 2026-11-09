package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.item.ItemSpec
import ra.modules.interactive.BigTorch.registerBigTorch
import ra.modules.interactive.BlockBreaker.registerBlockBreaker
import ra.modules.interactive.BlockPlacer.registerBlockPlacer
import ra.modules.interactive.Breeder.registerBreeder
import ra.modules.interactive.Cauldrons.registerCauldrons
import ra.modules.interactive.EnchantedCoal.registerEnchantedCoal
import ra.modules.interactive.Elevator.registerElevator
import ra.modules.interactive.ItemMover.registerItemMover
import ra.modules.interactive.ItemPipe.registerItemPipe
import ra.modules.interactive.MagicCrate.registerMagicCrate
import ra.modules.interactive.MessageBlock.registerMessageBlock
import ra.modules.interactive.Spitter.registerSpitter
import ra.core.moduleLoaded
import ra.core.raMessage

/**
 * Interactive Machines (`ra_interactive`): blocks that act on the world around them -- break,
 * place, spit, breed, move items, send messages, refill, pull items in, keep mobs away. Each block
 * lives in its own file; this one wires them together.
 */
object InteractiveModule : GameplayModule {
	override val ns = Ns.INTERACTIVE
	override val title = "Interactive Machines"

	/** A block with the item its give, drop and bundle hand out, and its debug registration function. */
	private class Entry(val block: CustomBlock, val item: ItemSpec, val registerBlock: FunctionArgument)

	private val breaker = Entry(BlockBreaker.block, BlockBreaker.item, BlockBreaker.registerBlock)
	private val breeder = Entry(Breeder.block, Breeder.item, Breeder.registerBlock)
	private val placer = Entry(BlockPlacer.block, BlockPlacer.item, BlockPlacer.registerBlock)
	private val spitter = Entry(Spitter.block, Spitter.item, Spitter.registerBlock)
	private val pipe = Entry(ItemPipe.block, ItemPipe.item, ItemPipe.registerBlock)
	private val cauldrons = Cauldrons.all.map { Entry(it.block, it.item, it.registerBlock) }
	private val message = Entry(MessageBlock.block, MessageBlock.item, MessageBlock.registerBlock)
	private val mover = Entry(ItemMover.block, ItemMover.item, ItemMover.registerBlock)
	private val crate = Entry(MagicCrate.block, MagicCrate.item, MagicCrate.registerBlock)
	private val torch = Entry(BigTorch.block, BigTorch.item, BigTorch.registerBlock)
	private val elevator = Entry(Elevator.block, Elevator.item, Elevator.registerBlock)

	/** The order the beet `load` registered blocks in. */
	private val loadOrder = listOf(breaker, breeder, placer, spitter, pipe) + cauldrons + listOf(message, mover, crate, torch, elevator)

	/** The order `tick` and `items/give_all` list them in. */
	private val tickOrder = listOf(breaker, spitter, placer, pipe, breeder) + cauldrons + listOf(message, mover, crate, torch, elevator)

	/** The order of the bundle: the Magic Crate sits after the pipe there. */
	private val bundleOrder = listOf(breaker, spitter, placer, pipe, crate, breeder) + cauldrons + listOf(message, mover, torch, elevator)

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerBlockBreaker()
		registerSpitter()
		registerBlockPlacer()
		registerItemPipe()
		registerBreeder()
		registerCauldrons()
		registerMessageBlock()
		registerItemMover()
		registerMagicCrate()
		registerBigTorch()
		registerEnchantedCoal()
		registerElevator()

		pack.define(load) {
			// Countdown until each item pipe re-reads its filter frame. The frame is gone (the
			// filter is a property now, see ItemPipeTransfer); the objective is still created, as
			// the beet load did.
			raw("scoreboard objectives add ra.filter_cd dummy")
			for (o in Elevator.objectives) raw("scoreboard objectives add $o")
			for (e in loadOrder) function(e.registerBlock)
			moduleLoaded("Interactive blocks loaded!", "aqua")
		}
		pack.define(tick!!) {
			for (e in tickOrder) function(e.block.tick)
		}

		// /function ra_interactive:items/give_all
		pack.define(ref(ns, "items/give_all")) {
			for (e in tickOrder) function(e.block.give)
			raMessage("@s", "Given all custom blocks!", "green")
		}

		bundles += GiveBundle(
			"interactive", "Interactive Bundle", "13 automation and utility blocks", ns,
			bundleOrder.map { it.item.stackNbt() },
		)
	}
}
