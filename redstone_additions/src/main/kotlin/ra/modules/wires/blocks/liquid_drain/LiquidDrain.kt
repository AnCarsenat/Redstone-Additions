package ra.modules.wires.blocks.liquid_drain

import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.ref

object LiquidDrain : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/liquid_drain/goggles")

	override fun Ra.register() {
		// No wrench cycles any more. The drain's role comes from redstone (powered: pour out,
		// unpowered: take in) and its throughput is the admin-only `speed` property, so there is
		// nothing left for a player to cycle. See ra_wires:fluid/drain_tick.

		pack.define(LiquidDrain.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Liquid Drain", nameY = 1.0)

			// Contents belong to the network, not this node; refresh_status copies them here.
			// Same three lines as the Liquid Pump. `speed` is an admin knob and stays off the
			// billboard (goggles = false in its schema).
			dataLine("medium", "Medium: ", "aqua", y = 0.8)
			dataLine("amount", "Amount: ", "yellow", y = 0.55, suffix = " mL")
			dataLine("drain_state", "Drain: ", "gray", y = 0.3)
		}
	}
}
