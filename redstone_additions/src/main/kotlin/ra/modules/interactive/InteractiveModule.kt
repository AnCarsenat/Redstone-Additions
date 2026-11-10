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
import ra.modules.interactive.Cauldrons.registerCauldrons
import ra.modules.interactive.EnchantedCoal.registerEnchantedCoal
import ra.modules.interactive.Elevator.registerElevator
import ra.modules.interactive.TrashCan.registerTrashCan
import ra.modules.interactive.Fan.registerFan
import ra.modules.interactive.ItemMover.registerItemMover
import ra.modules.interactive.ItemPipe.registerItemPipe
import ra.modules.interactive.MagicCrate.registerMagicCrate
import ra.modules.interactive.MessageBlock.registerMessageBlock
import ra.modules.interactive.Spitter.registerSpitter
import ra.core.moduleLoaded
import ra.core.raMessage

/**
 * Interactive Machines (`ra_interactive`): blocks that act on the world around them -- break,
 * place, spit, move items, send messages, refill, pull items in, keep mobs away. Each block
 * lives in its own file; this one wires them together.
 */
object InteractiveModule : GameplayModule {
	override val ns = Ns.INTERACTIVE
	override val title = "Interactive Machines"

	/** A block with the item its give, drop and bundle hand out, and its debug registration function. */
	private class Entry(val block: CustomBlock, val item: ItemSpec, val registerBlock: FunctionArgument)

	private val breaker = Entry(BlockBreaker.block, BlockBreaker.item, BlockBreaker.registerBlock)
	private val placer = Entry(BlockPlacer.block, BlockPlacer.item, BlockPlacer.registerBlock)
	private val spitter = Entry(Spitter.block, Spitter.item, Spitter.registerBlock)
	private val pipe = Entry(ItemPipe.block, ItemPipe.item, ItemPipe.registerBlock)
	private val cauldrons = Cauldrons.all.map { Entry(it.block, it.item, it.registerBlock) }
	private val message = Entry(MessageBlock.block, MessageBlock.item, MessageBlock.registerBlock)
	private val mover = Entry(ItemMover.block, ItemMover.item, ItemMover.registerBlock)
	private val crate = Entry(MagicCrate.block, MagicCrate.item, MagicCrate.registerBlock)
	private val torch = Entry(BigTorch.block, BigTorch.item, BigTorch.registerBlock)
	private val elevator = Entry(Elevator.block, Elevator.item, Elevator.registerBlock)
	private val trash = Entry(TrashCan.block, TrashCan.item, TrashCan.registerBlock)
	private val fan = Entry(Fan.block, Fan.item, Fan.registerBlock)

	/** The order the beet `load` registered blocks in. */
	private val loadOrder = listOf(breaker, placer, spitter, pipe) + cauldrons + listOf(message, mover, crate, torch, elevator, trash, fan)

	/**
	 * The order `tick` and `items/give_all` list them in. The Trash Can ticks after the Item Pipe and
	 * the Item Mover, so what they push into it is gone before any hopper under it could pull it out.
	 */
	private val tickOrder = listOf(breaker, spitter, placer, pipe) + cauldrons + listOf(message, mover, crate, torch, elevator, trash, fan)

	/** The order of the bundle: the Magic Crate sits after the pipe there. */
	private val bundleOrder = listOf(breaker, spitter, placer, pipe, crate) + cauldrons + listOf(message, mover, torch, elevator, trash, fan)

	override fun Ra.register() {
		// Registration order is the placement-handler order of the beet #ra:placement_handlers.
		registerBlockBreaker()
		registerSpitter()
		registerBlockPlacer()
		registerItemPipe()
		registerCauldrons()
		registerMessageBlock()
		registerItemMover()
		registerMagicCrate()
		registerBigTorch()
		registerEnchantedCoal()
		registerElevator()
		registerTrashCan()
		registerFan()

		pack.define(load) {
			// Countdown until each item pipe re-reads its filter frame. The frame is gone (the
			// filter is a property now, see ItemPipeTransfer); the objective is still created, as
			// the beet load did.
			raw("scoreboard objectives add ra.filter_cd dummy")
			for (o in Elevator.objectives + FanPush.objectives) raw("scoreboard objectives add $o")
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
			"interactive", "Interactive Bundle", "14 automation and utility blocks", ns,
			bundleOrder.map { it.item.stackNbt() },
		)
	}
}
