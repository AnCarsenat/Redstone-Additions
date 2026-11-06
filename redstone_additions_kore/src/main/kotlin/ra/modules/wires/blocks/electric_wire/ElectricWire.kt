package ra.modules.wires.blocks.electric_wire

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
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

			raw("data modify storage ra:temp block_name set value \"Wire\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			// "Enabled: on" was true of every wire ever placed. What a wire can usefully tell
			// you is what the grid it belongs to is doing.
			function(Goggles.Billboard.dataLine, nbt { this["path"] = "available_eu"; this["label"] = "EU: "; this["color"] = "aqua"; this["suffix"] = " EU"; this["y"] = 0.8 })
			function(Goggles.Billboard.dataLine, nbt { this["path"] = "grid"; this["label"] = ""; this["color"] = "yellow"; this["suffix"] = ""; this["y"] = 0.55 })
			function(Goggles.Billboard.dataLine, nbt { this["path"] = "trend"; this["label"] = "Change: "; this["color"] = "gray"; this["suffix"] = " EU/s"; this["y"] = 0.3 })
		}
	}
}
