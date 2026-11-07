package ra.modules.wires.common

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object Common : PackPart {
	val tickCleanup = ref(Ns.WIRES, "common/tick_cleanup")
	val updateModelLocalAndNeighbors = ref(Ns.WIRES, "common/update_model_local_and_neighbors")
	val updateModelSingle = ref(Ns.WIRES, "common/update_model_single")
	val updatePipeDisplays = ref(Ns.WIRES, "common/update_pipe_displays")
	val updatePipeDisplaysCopper = ref(Ns.WIRES, "common/update_pipe_displays_copper")
	val updateWireDisplays = ref(Ns.WIRES, "common/update_wire_displays")
	val updateWireDisplaysCopper = ref(Ns.WIRES, "common/update_wire_displays_copper")

	override fun Ra.register() {
		pack.define(Common.tickCleanup) {
			// Clear transient per-tick transport tags.
			//
			// There are none left. The electric system's ra.wires.eu_src latch went with the
			// push model: charge lives on the network now, so no node is ever mid-handover to
			// another and there is no per-tick state to unwind. Kept as a no-op hook so
			// ra_wires:tick keeps a place to put one if a future system needs it.
		}

		pack.define(Common.updateModelLocalAndNeighbors) {
			// Refresh current node and all immediate neighbors
			// Context: as node marker, at node position

			function(Common.updateModelSingle)

			for (side in SIDES) executeRun("positioned ${side.offset} as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s", Common.updateModelSingle)
		}

		pack.define(Common.updateModelSingle) {
			// Update connection-derived status for a single node

			function(RaLib.Transport.updateConnectionStatus)
			executeRun("if entity @s[tag=ra.custom_block.liquid_pipe]", Common.updatePipeDisplays)
			executeRun("if entity @s[tag=ra.custom_block.electric_wire]", Common.updateWireDisplays)
		}

		pack.define(Common.updatePipeDisplays) {
			// Draw conduit connection visuals for fluid pipes using block displays
			// Context: as fluid pipe marker, at pipe position

			// Remove existing local displays around this pipe.
			// 0.9 removes stale offset displays from earlier builds while preserving neighbor centers at distance 1.0.
			raw("kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..0.9]")

			raw("execute unless block ~ ~ ~ conduit run return 0")

			// One pipe, one look. The tier branch chose between two models for blocks that
			// had become the same block.
			function(Common.updatePipeDisplaysCopper)
		}

		pack.define(Common.updatePipeDisplaysCopper) {
			// Render L1 (copper tier) fluid pipe conduit visuals.
			//
			// Each node draws only its own half of a connection: from the core out to the
			// block boundary, never across into the neighbour. Both nodes of a pair used to
			// draw a full-length bar spanning centre to centre, so every connected pair
			// rendered two identical boxes in the same space and z-fought.
			//
			// Fluid pipes are the chunky ones: a 0.56 core in copper or iron. Electric wires
			// are deliberately half that thickness and a different palette, because an L1
			// pipe and an L1 wire were both a 0.56 copper_block and indistinguishable.

			// Core
			conduitCore(PIPE)

			// Half-connectors, one per connected side
			conduitLinks(PIPE)
		}

		pack.define(Common.updateWireDisplays) {
			// Draw conduit connection visuals for electric wires using block displays
			// Context: as electric wire marker, at wire position

			// Remove existing local displays around this wire.
			raw("kill @e[type=block_display,tag=ra.wires.wire_display,distance=..0.9]")

			raw("execute unless block ~ ~ ~ conduit run return 0")

			// One wire, one look.
			function(Common.updateWireDisplaysCopper)
		}

		pack.define(Common.updateWireDisplaysCopper) {
			// Render L1 electric wire conduit visuals.
			//
			// Each node draws only its own half of a connection: from the core out to the
			// block boundary, never across into the neighbour. Both nodes of a pair used to
			// draw a full-length bar spanning centre to centre, so every connected pair
			// rendered two identical boxes in the same space and z-fought.
			//
			// Electric wires are thin -- a 0.26 core against the fluid pipes' 0.56 -- and use
			// concrete rather than metal, matching their candle-coloured items. Previously an
			// L1 wire and an L1 fluid pipe rendered as exactly the same copper block.

			// Core
			conduitCore(WIRE)

			// Half-connectors, one per connected side
			conduitLinks(WIRE)
		}
	}
}
