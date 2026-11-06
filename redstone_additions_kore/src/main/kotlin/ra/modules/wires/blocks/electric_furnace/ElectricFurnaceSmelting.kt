package ra.modules.wires.blocks.electric_furnace

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref

object ElectricFurnaceSmelting : PackPart {
	val checkTarget = ref(Ns.WIRES, "blocks/electric_furnace/check_target")
	val deliver = ref(Ns.WIRES, "blocks/electric_furnace/deliver")
	val deliverAt = ref(Ns.WIRES, "blocks/electric_furnace/deliver_at")
	val findInput = ref(Ns.WIRES, "blocks/electric_furnace/find_input")
	val findNext = ref(Ns.WIRES, "blocks/electric_furnace/find_next")
	val findTry = ref(Ns.WIRES, "blocks/electric_furnace/find_try")
	val smeltOne = ref(Ns.WIRES, "blocks/electric_furnace/smelt_one")
	val takeInput = ref(Ns.WIRES, "blocks/electric_furnace/take_input")

	override fun Ra.register() {
		pack.define(ElectricFurnaceSmelting.checkTarget) {
			// Can the destination take one of what we are about to make?
			// Context: as the marker, at the block. Writes #ef.can.
			//
			// Asked before anything is spent or consumed, so a full chest or a missing one
			// simply stops the furnace instead of destroying an ingot.

			scoreboard.players.set(literal("#ef.can"), "ra.wires.tmp", 0)
			raw("data modify storage ra:temp inv_item set value {}")
			data(Storage.TEMP) { modify("inv_item.id", Storage.WIRES, "ef.hit.result") }
			data(Storage.TEMP) { modify("inv_item.count", 1) }

			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) unless block ~ ~ ~ #ra_lib:containers run return 0")
			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) run function ${RaLib.Inventory.canAcceptOne.asId()}")
			scoreboard.players.operation(literal("#ef.can"), "ra.wires.tmp", Operation.SET, literal("#inv_can_insert"), "ra.temp")
		}

		pack.define(ElectricFurnaceSmelting.deliver) {
			// Internal: push one result into the chosen neighbour.
			// Context: as the marker, at the block. storage ra:wires ef.hit holds the result.

			raw("\$execute positioned ~\$(dx) ~\$(dy) ~\$(dz) run function ${ElectricFurnaceSmelting.deliverAt.asId()} with storage ra:wires ef.hit")
		}

		pack.define(ElectricFurnaceSmelting.deliverAt) {
			// Internal: insert one result into the container at the current position.
			//
			// insert is safe here only because the count is one and check_target already
			// confirmed there is room -- see ra_lib:inventory/insert on why a larger count
			// cannot be trusted.

			raw("\$execute store result score #ef.done ra.wires.tmp run function ${RaLib.Inventory.insert.asId()} {id:\"\$(result)\",count:1,components:{}}")
		}

		pack.define(ElectricFurnaceSmelting.findInput) {
			// Find the first smeltable stack in the INPUT rows (slots 0-8).
			// Context: at the block. Writes #ef.slot (-1 for nothing) and storage ra:wires ef.hit.
			//
			// Walks a copy of Items rather than the container itself, and carries the real
			// slot number from each entry, because Items only holds the stacks that exist --
			// the list index is not the slot index and a barrel can be non-contiguous.

			scoreboard.players.set(literal("#ef.slot"), "ra.wires.tmp", -1)
			data(Storage.WIRES) { remove("ef") }
			raw("data modify storage ra:wires ef.scan set from block ~ ~ ~ Items")
			function(ElectricFurnaceSmelting.findNext)
		}

		pack.define(ElectricFurnaceSmelting.findNext) {
			// Internal: one entry of the input scan. Context: at the block.

			raw("execute if score #ef.slot ra.wires.tmp matches 0.. run return 0")
			raw("execute unless data storage ra:wires ef.scan[0] run return 0")

			data(Storage.WIRES) { modify("ef.q.id", Storage.WIRES, "ef.scan[0].id") }
			data(Storage.WIRES) { modify("ef.q.slot", Storage.WIRES, "ef.scan[0].Slot") }
			function(ElectricFurnaceSmelting.findTry, Storage.WIRES, "ef.q")

			data(Storage.WIRES) { remove("ef.scan[0]") }
			function(ElectricFurnaceSmelting.findNext)
		}

		pack.define(ElectricFurnaceSmelting.findTry) {
			// Internal: is the stack in this slot smeltable? Context: at the block.
			//
			// Every slot counts. Results leave the block entirely, so there is no output
			// region to keep clear and no way for the furnace to find its own product.
			//
			// The slot goes into ef.hit as well as into the score, because take_input is a
			// macro function reading ef.hit and needs $(slot) from it. Setting only the score
			// is what made the furnace duplicate: take_input's macro had no `slot` to
			// substitute, so the call failed outright and removed nothing, while #ef.took
			// still held the 1 left by the previous successful smelt -- so deliver ran anyway
			// and produced an ingot from an ore that was never consumed.

			raw("\$data modify storage ra:wires ef.hit set from storage ra:wires smelt_map.\"\$(id)\"")
			raw("execute unless data storage ra:wires ef.hit run return 0")

			raw("\$data modify storage ra:wires ef.hit.slot set value \$(slot)")
			raw("\$scoreboard players set #ef.slot ra.wires.tmp \$(slot)")
		}

		pack.define(ElectricFurnaceSmelting.smeltOne) {
			// Consume one item from the found input slot and push its result out.
			// Context: as the marker, at the block.
			//
			// #ef.took is cleared HERE, not only inside take_input. A macro function whose
			// arguments are incomplete fails without running a single one of its lines, so a
			// flag it was supposed to reset can survive from the last call -- and a stale
			// "yes I took it" is exactly how this duplicated items. Clearing it in the caller
			// means a take that does not run reads as a take that did not happen.

			scoreboard.players.set(literal("#ef.took"), "ra.wires.tmp", 0)
			function(ElectricFurnaceSmelting.takeInput, Storage.WIRES, "ef.hit")
			raw("execute if score #ef.took ra.wires.tmp matches ..0 run return 0")

			function(ElectricFurnaceSmelting.deliver, Storage.WIRES, "ef.out")

			raw("data modify entity @s data.status.state set value \"Smelting\"")
			function(ElectricFurnace.running)
		}

		pack.define(ElectricFurnaceSmelting.takeInput) {
			// Internal: take one item off the input stack in $(slot). Context: at the block.
			//
			// By Slot rather than by list index, so it does not matter how the barrel is
			// arranged. The count is read first so a stack of one is removed outright instead
			// of left as a zero-count entry, which renders as a ghost item.

			scoreboard.players.set(literal("#ef.took"), "ra.wires.tmp", 0)

			raw("\$execute store result score #ef.cnt ra.wires.tmp run data get block ~ ~ ~ Items[{Slot:\$(slot)b}].count")
			raw("execute if score #ef.cnt ra.wires.tmp matches ..0 run return 0")

			raw("\$execute if score #ef.cnt ra.wires.tmp matches 1 run data remove block ~ ~ ~ Items[{Slot:\$(slot)b}]")
			scoreboard.players.remove(literal("#ef.cnt"), "ra.wires.tmp", 1)
			raw("\$execute if score #ef.cnt ra.wires.tmp matches 1.. store result block ~ ~ ~ Items[{Slot:\$(slot)b}].count int 1 run scoreboard players get #ef.cnt ra.wires.tmp")

			scoreboard.players.set(literal("#ef.took"), "ra.wires.tmp", 1)
		}
	}
}
