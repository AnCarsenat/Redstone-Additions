package ra.modules.wires.blocks.electric_breaker

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.textLine
import ra.core.define
import ra.core.ref

object ElectricBreaker : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/electric_breaker/goggles")

	override fun Ra.register() {
		pack.define(ElectricBreaker.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// A breaker has no stored charge to report, so what matters is whether it is
			// actually moving anything and, when it is not, which side is the reason.

			gogglesHeader("EU Breaker", nameY = 1.0)

			dataLine("moved", "Moving: ", "aqua", y = 0.8, suffix = " EU")

			execute { ifCondition { data(self(), "data.status{bridge_state:\"transferring\"}") }; run { textLine("State: ", "Transferring", "green", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"unpowered\"}") }; run { textLine("State: ", "No redstone", "gray", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"disabled\"}") }; run { textLine("State: ", "Disabled", "gray", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"needs_two_networks\"}") }; run { textLine("State: ", "Needs a wire on two sides", "red", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"same_network\"}") }; run { textLine("State: ", "Both sides one grid", "red", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"level\"}") }; run { textLine("State: ", "Balanced", "aqua", y = 0.55) } }
			execute { ifCondition { data(self(), "data.status{bridge_state:\"blocked\"}") }; run { textLine("State: ", "Far side cannot take it", "yellow", y = 0.55) } }
		}
	}
}
