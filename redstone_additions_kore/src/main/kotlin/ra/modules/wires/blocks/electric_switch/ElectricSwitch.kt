package ra.modules.wires.blocks.electric_switch

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
import ra.modules.wires.common.line

object ElectricSwitch : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/electric_switch/goggles")

	override fun Ra.register() {
		pack.define(ElectricSwitch.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			raw("data modify storage ra:temp block_name set value \"EU Switch\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			function(Goggles.Billboard.dataLine, nbt { this["path"] = "available_eu"; this["label"] = "EU: "; this["color"] = "aqua"; this["suffix"] = " EU"; this["y"] = 0.8 })
			line("execute if data entity @s data.status{active:1b} run function", Goggles.Billboard.textLine, """{label:"Active: ",value:"On",color:"green",suffix:"",y:0.55}""")
			line("execute unless data entity @s data.status{active:1b} run function", Goggles.Billboard.textLine, """{label:"Active: ",value:"Off",color:"gray",suffix:"",y:0.55}""")
		}
	}
}
