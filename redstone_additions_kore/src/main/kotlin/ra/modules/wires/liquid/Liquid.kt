package ra.modules.wires.liquid

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.ref
import ra.modules.wires.common.Common

object Liquid : PackPart {
	val pipeAppeared = ref(Ns.WIRES, "liquid/pipe_appeared")
	val pipeVanished = ref(Ns.WIRES, "liquid/pipe_vanished")

	override fun Ra.register() {
		pack.define(Liquid.pipeAppeared) {
			// A conduit showed up at a pipe marker that did not have one. Rebuild the local
			// model and record the new state. Context: as marker, at position.

			function(Common.updateModelLocalAndNeighbors)
			tag(self()) { add("ra.wires.pipe_present") }
		}

		pack.define(Liquid.pipeVanished) {
			// The conduit at a pipe marker is gone. Tear the displays down and record it.
			// Context: as marker, at position.
			//
			// Note this runs before break detection in the same tick, so a genuinely broken
			// pipe passes through here first and then through liquid/break/pipe.

			function(Common.updatePipeDisplays)
			function(Common.updateModelLocalAndNeighbors)
			tag(self()) { remove("ra.wires.pipe_present") }
		}
	}
}
