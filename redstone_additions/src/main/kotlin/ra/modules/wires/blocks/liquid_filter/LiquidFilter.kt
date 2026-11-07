package ra.modules.wires.blocks.liquid_filter

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.textLine
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.core.ref

object LiquidFilter : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/liquid_filter/goggles")
	val gogglesMedium = ref(Ns.WIRES, "blocks/liquid_filter/goggles_medium")
	val gogglesMediumRun = ref(Ns.WIRES, "blocks/liquid_filter/goggles_medium_run")

	override fun Ra.register() {
		pack.define(LiquidFilter.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.

			gogglesHeader("Liquid Filter", nameY = 1.0)

			// What it is set to let through, said in the medium's own name and colour rather
			// than as the registry key -- ra_wires:media/label is what every other fluid
			// block uses for this and it is why `water` reads as Water.
			function(LiquidFilter.gogglesMedium)

			dataLine("moved", "Moving: ", "aqua", y = 0.55, suffix = " mL")
			execute { ifCondition { data(self(), "data.status{bridge_state:\"transferring\"}") }; run { textLine("State: ", "Passing", "green", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"level\"}") }; run { textLine("State: ", "Balanced", "aqua", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"unpowered\"}") }; run { textLine("State: ", "No redstone", "gray", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"needs_two_networks\"}") }; run { textLine("State: ", "Needs a pipe on two sides", "red", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"same_network\"}") }; run { textLine("State: ", "Both sides one network", "red", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"blocked\"}") }; run { textLine("State: ", "Nothing of that medium to pass", "yellow", y = 0.3) } }
		}

		pack.define(LiquidFilter.gogglesMedium) {
			// Internal: draw the "Passing:" line, in the medium's own display name.
			// Context: as the filter marker.

			executeReturnRun("unless data entity @s data.properties.filter_medium", Goggles.Billboard.textLine, """{label:"Passing: ",value:"Anything",color:"gray",suffix:"",y:0.8}""")

			raw("data modify storage ra:wires lq set value {}")
			data(Storage.WIRES) { modify("lq.medium", self(), "data.properties.filter_medium") }
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
