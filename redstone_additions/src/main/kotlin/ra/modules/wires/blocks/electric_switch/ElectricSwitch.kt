package ra.modules.wires.blocks.electric_switch

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.textLine
import ra.core.define
import ra.core.ref

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

			gogglesHeader("EU Switch", nameY = 1.0)

			dataLine("available_eu", "EU: ", "aqua", y = 0.8, suffix = " EU")
			execute { ifCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "On", "green", y = 0.55) } }
			execute { unlessCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "Off", "gray", y = 0.55) } }
		}
	}
}
