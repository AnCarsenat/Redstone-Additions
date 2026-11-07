package ra.modules.wires.blocks.boiler

import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.ref

object Boiler : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/boiler/goggles")

	override fun Ra.register() {
		pack.define(Boiler.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Boiler", nameY = 1.0)

			dataLine("boiler_state", "Boiler: ", "gold", y = 0.8)
		}
	}
}
