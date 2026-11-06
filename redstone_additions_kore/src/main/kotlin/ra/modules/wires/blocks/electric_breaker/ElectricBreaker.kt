package ra.modules.wires.blocks.electric_breaker

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

object ElectricBreaker : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/electric_breaker/goggles")

	override fun Ra.register() {
		pack.define(ElectricBreaker.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// A breaker has no stored charge to report, so what matters is whether it is
			// actually moving anything and, when it is not, which side is the reason.

			raw("data modify storage ra:temp block_name set value \"EU Breaker\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			function(Goggles.Billboard.dataLine, nbt { this["path"] = "moved"; this["label"] = "Moving: "; this["color"] = "aqua"; this["suffix"] = " EU"; this["y"] = 0.8 })

			line("""execute if data entity @s data.status{bridge_state:"transferring"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Transferring",color:"green",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"unpowered"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"disabled"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Disabled",color:"gray",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Needs a wire on two sides",color:"red",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"same_network"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Both sides one grid",color:"red",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"level"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.55}""")
			line("""execute if data entity @s data.status{bridge_state:"blocked"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Far side cannot take it",color:"yellow",suffix:"",y:0.55}""")
		}
	}
}
