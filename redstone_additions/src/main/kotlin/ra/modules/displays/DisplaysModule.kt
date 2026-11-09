package ra.modules.displays

import io.github.ayfri.kore.commands.function
import ra.core.GameplayModule
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.moduleLoaded
import ra.core.raMessage
import ra.core.ref
import ra.modules.displays.DisplayPoll.registerDisplayPoll
import ra.modules.displays.DisplayPoll.resetFlag
import ra.modules.displays.ItemName.registerItemName

/**
 * Displays (`ra_displays`): blocks that show something with display entities.
 *
 * ADDING A BLOCK. Write an object implementing [DisplayBlock] in this package and add it to
 * [members]; load, tick, `items/give_all`, the bundle and the slow clock ([DisplayPoll]) follow
 * from the list. Then the per-block rows elsewhere: its enable switch on `DisplaysPage`, its
 * advancement in `displaysTree`, and the docs (`docs/displays.md`).
 */
object DisplaysModule : GameplayModule {
	override val ns = Ns.DISPLAYS
	override val title = "Displays"

	/** In registration order, which is also the placement-handler and tick order. Not `blocks`: see the gotcha. */
	private val members: List<DisplayBlock> = listOf(GaugeBar, ItemHologram, TextScreen)

	override fun Ra.register() {
		// Shared by the blocks that show an item's name.
		registerItemName()
		for (m in members) with(m) { registerBlock() }
		registerDisplayPoll(members.mapNotNull { m -> m.poll?.let { m.block to it } })

		pack.define(load) {
			for (m in members) function(m.block.fn("register_block"))
			// A schedule that did not survive is started again by the next polled block's tick.
			resetFlag()
			moduleLoaded("Displays loaded!", "aqua")
		}

		pack.define(tick!!) { for (m in members) function(m.block.tick) }

		pack.define(ref(ns, "items/give_all")) {
			for (m in members) function(m.block.give)
			raMessage("@s", "Given all display blocks!", "light_purple")
		}

		bundles += GiveBundle(
			"displays", "Displays Bundle", "Blocks that show signals and items", ns,
			members.map { it.item.stackNbt() },
		)
	}
}
