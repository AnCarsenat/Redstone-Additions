package ra.modules.wires.blocks.liquid_pump

import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.ref

object LiquidPump : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/liquid_pump/goggles")

	override fun Ra.register() {
		pack.define(LiquidPump.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Liquid Pump", nameY = 1.0)

			// Contents belong to the network, not this node; refresh_status copies them here.
			dataLine("medium", "Medium: ", "aqua", y = 0.8)
			dataLine("amount", "Amount: ", "yellow", y = 0.55, suffix = " mL")
			dataLine("pump_state", "Pump: ", "gray", y = 0.3)
		}
	}
}
