package ra.modules.wires.blocks.gas_valve

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

object GasValve : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/gas_valve/goggles")

	override fun Ra.register() {
		pack.define(GasValve.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			raw("data modify storage ra:temp block_name set value \"Gas Valve\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			// Contents belong to the network, not this node; refresh_status copies them here.

			// A bridge is not a network node, so it has no medium or amount of its own -- it
			// used to report the fluid-node fields and print N/A for all of them. What it can
			// say is whether it is actually moving anything, and why not when it is not.
			function(Goggles.Billboard.dataLine, nbt { this["path"] = "moved"; this["label"] = "Moving: "; this["color"] = "aqua"; this["suffix"] = " mL"; this["y"] = 0.55 })
			line("""execute if data entity @s data.status{bridge_state:"transferring"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Transferring",color:"green",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"level"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"unpowered"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"disabled"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Disabled",color:"gray",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Needs a pipe on two sides",color:"red",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"same_network"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Both sides one network",color:"red",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"blocked"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Far side cannot take it",color:"yellow",suffix:"",y:0.3}""")
		}
	}
}
