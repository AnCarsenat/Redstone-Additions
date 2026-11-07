package ra.modules.wires.blocks.battery

import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.ref

object Battery : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/battery/goggles")

	override fun Ra.register() {
		pack.define(Battery.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The EU shown is the whole grid's, not this battery's. Charge belongs to the
			// network, so two batteries on one run both read the same total — which is the
			// honest answer rather than an invented split.

			gogglesHeader("Battery", nameY = 1.0)

			dataLine("available_eu", "Grid: ", "aqua", y = 0.8, suffix = " EU")
			dataLine("capacity", "Of: ", "gray", y = 0.6, suffix = " EU")
		}
	}
}
