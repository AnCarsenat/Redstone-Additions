package ra.modules.wires.electric

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

object EuApi : PackPart {
	val offerEu = ModulesApi.Wires.offerEu
	val peekEu = ref(Ns.WIRES, "electric/peek_eu")
	val takeEu = ModulesApi.Wires.takeEu

	override fun Ra.register() {
		pack.define(EuApi.offerEu) {
			// Internal: push EU into this node's network. Returns how much was accepted.
			// Context: as an electric node marker.
			//
			// A macro wrapper because ra_lib:transport/net/offer takes its amount as a macro
			// argument, and callers compute theirs in a scoreboard. Store the number into
			// `storage ra:wires eu` and call this `with storage ra:wires eu`.

			raw("\$return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"eu\"}")
		}

		pack.define(EuApi.peekEu) {
			// How much EU this node's network is holding, without taking any of it.
			// Returns the stored amount, 0 when the node is on no network at all.
			// Context: as an electric node marker.
			//
			// take_eu answers "could I have some" only by helping itself, which is the wrong
			// question for anything that wants to know whether it COULD run -- a machine
			// asking that every tick would drain the grid just by looking at it. net/read is
			// already the non-destructive half of net/take, so this is only a name for it in
			// EU terms.

			function(RaLib.Transport.Net.read)
			raw("return run scoreboard players get #net_amount ra.tr.tmp")
		}

		pack.define(EuApi.takeEu) {
			// Internal: draw EU out of this node's network. Returns how much was actually
			// removed, which is 0 when the grid is empty.
			// Context: as an electric node marker.

			raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"eu\"}")
		}
	}
}
