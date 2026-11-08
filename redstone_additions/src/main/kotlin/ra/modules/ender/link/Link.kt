package ra.modules.ender.link

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.ender.EnderIds
import ra.modules.ender.ItemVault
import ra.core.executeRun
import ra.core.block.soundListenersSel
import ra.core.block.particleViewersSel
import ra.core.executeStoreRun

/**
 * `ra_ender:link/...`: the transfer half of every vault. The channel is a string, so finding the
 * partner needs a macro: no selector predicate can compare one entity's property with another's.
 */
object Link {
	private fun fn(path: String) = ref(Ns.ENDER, "link/$path")

	val done = fn("done")
	val moveStack = fn("move_stack")
	val offerFluid = fn("offer_fluid")
	val probeFluid = fn("probe_fluid")
	val probeFluidRead = fn("probe_fluid_read")
	val probePower = fn("probe_power")
	val probePowerRead = fn("probe_power_read")
	val receiveFluid = fn("receive_fluid")
	val receiveItems = fn("receive_items")
	val receivePower = fn("receive_power")
	val requestItems = fn("request_items")
	val returnFluid = fn("return_fluid")
	val sendFluid = fn("send_fluid")
	val sendItems = fn("send_items")
	val sendPower = fn("send_power")

	private const val T = Obj.TEMP

	/** The nearest other vault wearing [recv] whose channel is `$(channel)`, as an execute chain. */
	private fun partner(recv: String, exclude: String = EnderIds.SELF) =
		"as @e[type=marker,tag=$recv,tag=!$exclude,limit=1,sort=nearest] if data entity @s data.properties{channel:\"\$(channel)\"}"

	fun Ra.registerLink() {
		// Drop the self tag at the end of a vault's cycle, and on every early return out of one.
		// Returns 0 so it can be used as `return run function ...`. As the vault marker.
		pack.define(done) {
			tag(self()) { remove(EnderIds.SELF) }
			returnValue(0)
		}

		registerItems()
		registerFluid()
		registerPower()
	}

	private fun Ra.registerItems() {
		// Macro {x,y,z,src_slot,dst_slot}, at the receiving barrel. Wraps the library primitive,
		// whose src is one string, with the coordinates the sender stored as three numbers.
		pack.define(moveStack) {
			raw("\$function ${RaLib.Inventory.moveSlot.asId()} {src:\"\$(x) \$(y) \$(z)\",src_slot:\$(src_slot),dst_slot:\$(dst_slot)}")
		}

		// Macro {channel, recv}: hand the pending stack to the nearest vault on this channel wearing
		// `recv`. As the sending vault marker, at its barrel.
		//
		// The excluded tag is ra.ender.sending, held by whoever is pushing, not ra.ender.self, which
		// marks the vault whose cycle is running. On a pull those are two different vaults -- the one
		// whose cycle it is *is* the receiver -- and excluding self meant a pull could never deliver
		// anything.
		pack.define(sendItems) {
			raw("\$execute ${partner("\$(recv)", EnderIds.SENDING)} at @s run function ${receiveItems.asId()}")
		}

		// As the receiving vault marker, at its barrel. The pending move is in storage ra:ender move.
		//
		// The destination slot has to be empty before the stack is copied in: /item replace
		// overwrites whatever is there, and an overwrite would destroy items.
		// ra_lib:inventory/move_slot then clears the source, so the stack exists in exactly one
		// place at every point.
		pack.define(receiveItems) {
			raw("execute unless block ~ ~ ~ barrel run return 0")

			function(RaLib.Inventory.findFreeSlot)
			raw("execute if score #inv_slot $T matches ..-1 run return 0")

			raw("execute store result storage ra:ender move.dst_slot int 1 run scoreboard players get #inv_slot $T")
			function(moveStack, Storage.ENDER, "move")

			// A delivery is not an insert: mark this end now, or the next cycle reads the arriving
			// stack as something a player put in and pushes it back.
			function(ItemVault.mark)

			raw("scoreboard players set #ender.moved $T 1")
			raw("playsound minecraft:entity.enderman.teleport block ${soundListenersSel(8)} ~ ~ ~ 0.25 1.6")
			raw("particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 6 normal ${particleViewersSel()}")
		}

		// Macro {channel, recv}: find a vault on this channel willing to give, and run its push.
		// As the vault that wants a refill.
		pack.define(requestItems) {
			raw("\$execute ${partner(EnderIds.SEND_ITEM)} at @s run function ${ItemVault.pushToPuller.asId()}")
		}
	}

	private fun Ra.registerFluid() {
		// Macro {amount, medium}, as the receiving node. Splits the macro so the amount can come
		// from a score rather than being known when send_fluid was written.
		pack.define(offerFluid) {
			raw("\$function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"\$(medium)\"}")
		}

		// Macro {channel}: read the partner network's contents into #ender.theirs, or leave it at
		// -1. As the asking vault marker.
		pack.define(probeFluid) {
			raw("\$execute ${partner(EnderIds.RECV_FLUID)} run function ${probeFluidRead.asId()}")
		}

		// The partner reports its own network's amount. As the partner vault marker.
		pack.define(probeFluidRead) {
			function(RaLib.Transport.Net.read)
			raw("scoreboard players operation #ender.theirs $T = #net_amount ${Obj.TR_TMP}")
		}

		// Macro {channel, medium, rate}, as the sending vault marker.
		pack.define(sendFluid) {
			// Take our slice out of the local network first.
			raw("\$function ${RaLib.Transport.Net.take.asId()} {amount:\$(rate),medium:\"\$(medium)\"}")
			raw("scoreboard players operation #ender.carry $T = #net_moved ${Obj.TR_TMP}")
			raw("execute if score #ender.carry $T matches ..0 run return 0")

			// Offer it to the partner. A partner that takes less than everything leaves the
			// remainder in #ender.carry.
			raw("\$execute ${partner(EnderIds.RECV_FLUID)} at @s run function ${receiveFluid.asId()} with storage ra:ender fluid")

			// Whatever came back goes straight into the network it came from, so nothing is lost
			// when the far side is full or holds another medium.
			executeRun("if score #ender.carry $T matches 1..", returnFluid, "with storage ra:ender fluid")
		}

		// Macro {medium}, as the receiving vault marker, at its block.
		pack.define(receiveFluid) {
			raw("execute store result storage ra:ender offer.amount int 1 run scoreboard players get #ender.carry $T")
			raw("data modify storage ra:ender offer.medium set from storage ra:ender fluid.medium")

			function(offerFluid, Storage.ENDER, "offer")
			raw("scoreboard players operation #ender.carry $T -= #net_moved ${Obj.TR_TMP}")

			raw("execute if score #net_moved ${Obj.TR_TMP} matches 1.. run playsound minecraft:entity.enderman.teleport block ${soundListenersSel(8)} ~ ~ ~ 0.2 1.9")
			raw("execute if score #net_moved ${Obj.TR_TMP} matches 1.. run particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 4 normal ${particleViewersSel()}")
		}

		// Macro {medium}, as the sending vault marker. Puts the unaccepted remainder back.
		pack.define(returnFluid) {
			raw("execute store result storage ra:ender offer.amount int 1 run scoreboard players get #ender.carry $T")
			raw("data modify storage ra:ender offer.medium set from storage ra:ender fluid.medium")
			function(offerFluid, Storage.ENDER, "offer")
		}
	}

	private fun Ra.registerPower() {
		// Macro {channel}: read how much the partner's GRID is holding into #ender.theirs, or leave
		// it at -1 when there is no partner on this channel. As the asking vault marker.
		//
		// Grid totals, not vault buffers: in two-way mode the vault is levelling the two bases
		// against each other, not two boxes that happen to sit between them.
		pack.define(probePower) {
			raw("\$execute ${partner(EnderIds.RECV_POWER)} run function ${probePowerRead.asId()}")
		}

		// Read this vault's grid total into #ender.theirs. As the partner vault marker.
		//
		// Split out because net/read leaves its answer in a scratch score rather than returning
		// it, so the caller's `store result` had nothing to capture.
		pack.define(probePowerRead) {
			function(RaLib.Transport.Net.read)
			raw("scoreboard players operation #ender.theirs $T = #net_amount ${Obj.TR_TMP}")
		}

		// Macro {channel}, as the sending vault marker. Reads #ender.carry, writes #ender.sent.
		pack.define(sendPower) {
			raw("scoreboard players set #ender.sent $T 0")
			raw("\$execute ${partner(EnderIds.RECV_POWER)} at @s run function ${receivePower.asId()}")
		}

		// As the receiving vault marker, at its block. Offers #ender.carry to the receiving vault's
		// GRID and reports what was taken in #ender.sent, so the sender knows exactly what to keep
		// and what to refund.
		//
		// The receiver has no buffer of its own any more. What arrives goes onto its grid -- into
		// that base's batteries, or straight into whatever is drawing there on the same tick -- and
		// a grid with no room simply accepts less.
		pack.define(receivePower) {
			raw("scoreboard players set #ender.sent $T 0")

			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.carry $T")
			executeStoreRun("", "#ender.sent", ModulesApi.Wires.offerEu, "with storage ra:wires eu")

			raw("execute if score #ender.sent $T matches ..0 run return 0")

			raw("playsound minecraft:block.beacon.activate block ${soundListenersSel(8)} ~ ~ ~ 0.12 1.9")
			raw("particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 3 normal ${particleViewersSel()}")
		}
	}
}
