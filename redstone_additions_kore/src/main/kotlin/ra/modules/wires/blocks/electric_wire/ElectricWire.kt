package ra.modules.wires.blocks.electric_wire

import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.ref

object ElectricWire : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/electric_wire/goggles")

	override fun Ra.register() {
		pack.define(ElectricWire.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Wire", nameY = 1.0)

			// "Enabled: on" was true of every wire ever placed. What a wire can usefully tell
			// you is what the grid it belongs to is doing.
			dataLine("available_eu", "EU: ", "aqua", y = 0.8, suffix = " EU")
			dataLine("grid", "", "yellow", y = 0.55)
			dataLine("trend", "Change: ", "gray", y = 0.3, suffix = " EU/s")
		}
	}
}
