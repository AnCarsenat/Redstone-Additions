package ra.modules.wires.blocks.liquid_filter

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

object LiquidFilter : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/liquid_filter/goggles")
	val gogglesMedium = ref(Ns.WIRES, "blocks/liquid_filter/goggles_medium")
	val gogglesMediumRun = ref(Ns.WIRES, "blocks/liquid_filter/goggles_medium_run")

	override fun Ra.register() {
		pack.define(LiquidFilter.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.

			raw("data modify storage ra:temp block_name set value \"Liquid Filter\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			// What it is set to let through, said in the medium's own name and colour rather
			// than as the registry key -- ra_wires:media/label is what every other fluid
			// block uses for this and it is why `water` reads as Water.
			function(LiquidFilter.gogglesMedium)

			function(Goggles.Billboard.dataLine, nbt { this["path"] = "moved"; this["label"] = "Moving: "; this["color"] = "aqua"; this["suffix"] = " mL"; this["y"] = 0.55 })
			line("""execute if data entity @s data.status{bridge_state:"transferring"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Passing",color:"green",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"level"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Balanced",color:"aqua",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"unpowered"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"No redstone",color:"gray",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"needs_two_networks"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Needs a pipe on two sides",color:"red",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"same_network"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Both sides one network",color:"red",suffix:"",y:0.3}""")
			line("""execute if data entity @s data.status{bridge_state:"blocked"} run function""", Goggles.Billboard.textLine, """{label:"State: ",value:"Nothing of that medium to pass",color:"yellow",suffix:"",y:0.3}""")
		}

		pack.define(LiquidFilter.gogglesMedium) {
			// Internal: draw the "Passing:" line, in the medium's own display name.
			// Context: as the filter marker.

			line("execute unless data entity @s data.properties.filter_medium run return run function", Goggles.Billboard.textLine, """{label:"Passing: ",value:"Anything",color:"gray",suffix:"",y:0.8}""")

			raw("data modify storage ra:wires lq set value {}")
			raw("data modify storage ra:wires lq.medium set from entity @s data.properties.filter_medium")
			function(LiquidFilter.gogglesMediumRun, Storage.WIRES, "lq")
		}

		pack.define(LiquidFilter.gogglesMediumRun) {
			// Internal: the dynamic-name half of goggles_medium.
			//
			// A medium the registry does not know still gets a line, showing the raw key.
			// That is deliberate: a filter set to a misspelt medium would otherwise look
			// identical to one set correctly and pass nothing, with nothing on the block to
			// say why.

			raw("\$execute unless data storage ra:wires media.\$(medium) run return run function ${Goggles.Billboard.textLine.asId()} {label:\"Passing: \",value:\"\$(medium) (unknown)\",color:\"red\",suffix:\"\",y:0.8}")

			data(Storage.WIRES) { remove("lqline") }
			raw("\$data modify storage ra:wires lqline.value set from storage ra:wires media.\$(medium).name")
			raw("\$data modify storage ra:wires lqline.color set from storage ra:wires media.\$(medium).color")
			raw("data modify storage ra:wires lqline.label set value \"Passing: \"")
			raw("data modify storage ra:wires lqline.suffix set value \"\"")
			raw("data modify storage ra:wires lqline.y set value 0.8")
			function(Goggles.Billboard.textLine, Storage.WIRES, "lqline")
		}
	}
}
