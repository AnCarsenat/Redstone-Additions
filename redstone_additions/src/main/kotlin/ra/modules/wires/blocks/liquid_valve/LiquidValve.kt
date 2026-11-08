package ra.modules.wires.blocks.liquid_valve

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

object LiquidValve : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/liquid_valve/goggles")

	override fun Ra.register() {
		pack.define(LiquidValve.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Liquid Valve", nameY = 1.0)

			// Contents belong to the network, not this node; refresh_status copies them here.

			// A bridge is not a network node, so it has no medium or amount of its own -- it
			// used to report the fluid-node fields and print N/A for all of them. What it can
			// say is whether it is actually moving anything, and why not when it is not.
			dataLine("moved", "Moving: ", "aqua", y = 0.55, suffix = " mL")
			execute { ifCondition { data(self(), "data.status{bridge_state:\"transferring\"}") }; run { textLine("State: ", "Transferring", "green", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"level\"}") }; run { textLine("State: ", "Balanced", "aqua", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"unpowered\"}") }; run { textLine("State: ", "No redstone", "gray", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"needs_two_networks\"}") }; run { textLine("State: ", "Needs a pipe on two sides", "red", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"same_network\"}") }; run { textLine("State: ", "Both sides one network", "red", y = 0.3) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"blocked\"}") }; run { textLine("State: ", "Far side cannot take it", "yellow", y = 0.3) } }
		}
	}
}
