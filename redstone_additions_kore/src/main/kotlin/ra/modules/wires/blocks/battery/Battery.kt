package ra.modules.wires.blocks.battery

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

			raw("data modify storage ra:temp block_name set value \"Battery\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			function(Goggles.Billboard.dataLine, nbt { this["path"] = "available_eu"; this["label"] = "Grid: "; this["color"] = "aqua"; this["suffix"] = " EU"; this["y"] = 0.8 })
			function(Goggles.Billboard.dataLine, nbt { this["path"] = "capacity"; this["label"] = "Of: "; this["color"] = "gray"; this["suffix"] = " EU"; this["y"] = 0.6 })
		}
	}
}
