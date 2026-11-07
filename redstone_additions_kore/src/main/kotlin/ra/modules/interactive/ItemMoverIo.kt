package ra.modules.interactive

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw

/**
 * The Item Mover's container plumbing: pick an input stack, check and fill the output, consume
 * what moved. Every step treats a double chest as one container -- when the half in front of the
 * block is empty (input) or full (output), its partner half is tried, through [CHEST_HALVES].
 *
 * `select_input`, `try_insert` and `check_output_capacity` are also the Boxer's and Unboxer's
 * entry points into this ([ModulesApi.Interactive]).
 *
 * Scratch scores (`ra.temp` fake players):
 *   #mover_has_input      1 when an input stack was selected
 *   #mover_input_partner  1 when it came from the partner half
 *   #mover_can_forward    1 when the output takes at least one
 *   #mover_inserted       how many items went in
 */
object ItemMoverIo {
	private val block = ItemMover.block

	val selectInput = ModulesApi.Interactive.itemMoverSelectInput
	val tryInsert = ModulesApi.Interactive.itemMoverTryInsert
	val checkOutputCapacity = ModulesApi.Interactive.itemMoverCheckOutputCapacity
	val consumeSelectedInput = block.fn("consume_selected_input")
	private val consumeInput = block.fn("consume_input")
	private val tryInsertLoop = block.fn("try_insert_loop")
	private val tryInsertOne = block.fn("try_insert_one")

	/** Never a vanilla dispenser: only RA custom blocks (markers, or the old armor stands) are valid. */
	private const val VANILLA_DISPENSER =
		"if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] unless entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest]"

	fun Ra.registerItemMoverIo() {
		pack.define(selectInput) { selectInput() }
		pack.define(checkOutputCapacity) { checkOutputCapacity() }
		pack.define(tryInsert) { tryInsert() }
		pack.define(tryInsertLoop) { tryInsertLoop() }
		pack.define(tryInsertOne) { tryInsertOne() }
		pack.define(consumeInput) { consumeInput() }
		pack.define(consumeSelectedInput) { consumeSelectedInput() }
	}

	/**
	 * Select one input stack from the container here, or from its partner half. At the primary
	 * input. Out: #mover_has_input, #mover_input_partner, storage ra:temp mover_item. Returns
	 * #mover_has_input.
	 */
	private fun Function.selectInput() {
		raw("scoreboard players set #mover_has_input ${Obj.TEMP} 0")
		raw("scoreboard players set #mover_input_partner ${Obj.TEMP} 0")

		// The primary container first.
		raw("execute if data block ~ ~ ~ Items[0] run data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]")
		raw("execute if data block ~ ~ ~ Items[0] run scoreboard players set #mover_has_input ${Obj.TEMP} 1")

		// Empty: probe the partner half.
		for (h in CHEST_HALVES) {
			val at = "${h.toPartner} if data block ~ ~ ~ Items[0] run"
			raw("execute if score #mover_has_input ${Obj.TEMP} matches 0 $at data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0]")
			raw("execute if score #mover_has_input ${Obj.TEMP} matches 0 $at scoreboard players set #mover_has_input ${Obj.TEMP} 1")
			raw("execute if score #mover_has_input ${Obj.TEMP} matches 1 $at scoreboard players set #mover_input_partner ${Obj.TEMP} 1")
		}

		raw("return run scoreboard players get #mover_has_input ${Obj.TEMP}")
	}

	/**
	 * Can the output take at least one of storage ra:temp mover_item? At the primary output.
	 * Out: #mover_can_forward, also returned.
	 */
	private fun Function.checkOutputCapacity() {
		raw("scoreboard players set #mover_can_forward ${Obj.TEMP} 0")

		raw("execute $VANILLA_DISPENSER run return 0")

		raw("data modify storage ra:temp inv_item set from storage ra:temp mover_item")
		raw("data modify storage ra:temp inv_item.count set value 1")
		raw("execute unless data storage ra:temp inv_item.components run data modify storage ra:temp inv_item.components set value {}")

		// The primary output.
		executeRun("", RaLib.Inventory.canAcceptOne, "with storage ra:temp inv_item")
		raw("execute if score #inv_can_insert ${Obj.TEMP} matches 1.. run scoreboard players set #mover_can_forward ${Obj.TEMP} 1")

		// Primary full: the partner half.
		for (h in CHEST_HALVES) {
			val partner = "if score #mover_can_forward ${Obj.TEMP} matches 0 ${h.toPartner}"
			executeRun(partner, RaLib.Inventory.canAcceptOne, "with storage ra:temp inv_item")
			raw("execute $partner if score #inv_can_insert ${Obj.TEMP} matches 1.. run scoreboard players set #mover_can_forward ${Obj.TEMP} 1")
		}

		raw("return run scoreboard players get #mover_can_forward ${Obj.TEMP}")
	}

	/**
	 * Insert storage ra:temp mover_item one item at a time into the output, then its partner half.
	 * At the primary output. Out: #mover_inserted, also returned.
	 */
	private fun Function.tryInsert() {
		raw("scoreboard players set #mover_inserted ${Obj.TEMP} 0")
		raw("execute store result score @s ${Obj.INV_COUNT} run data get storage ra:temp mover_item.count")
		raw("execute if score @s ${Obj.INV_COUNT} matches ..0 run return 0")

		// The macro needs a components field.
		raw("execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}")

		function(tryInsertLoop)

		raw("return run scoreboard players get #mover_inserted ${Obj.TEMP}")
	}

	/** One item into the output or its partner half, then recurse until full or exhausted. */
	private fun Function.tryInsertLoop() {
		raw("execute if score @s ${Obj.INV_COUNT} matches ..0 run return 0")

		// The primary output.
		executeRun("", tryInsertOne)

		// Failed: the partner half.
		for (h in CHEST_HALVES) executeRun("if score @s ${Obj.TEMP} matches 0 ${h.toPartner}", tryInsertOne)

		// Stop when the output can no longer take one.
		raw("execute if score @s ${Obj.TEMP} matches 0 run return 0")

		raw("scoreboard players add #mover_inserted ${Obj.TEMP} 1")
		raw("scoreboard players remove @s ${Obj.INV_COUNT} 1")
		function(tryInsertLoop)
	}

	/** Exactly one item into the container here. Out: `@s ra.temp` = 0 or 1. */
	private fun Function.tryInsertOne() {
		raw("scoreboard players set @s ${Obj.TEMP} 0")

		raw("execute $VANILLA_DISPENSER run return 0")

		raw("data modify storage ra:temp mover_item_single set from storage ra:temp mover_item")
		raw("data modify storage ra:temp mover_item_single.count set value 1")
		raw("execute unless data storage ra:temp mover_item_single.components run data modify storage ra:temp mover_item_single.components set value {}")

		raw("data modify storage ra:temp inv_item set from storage ra:temp mover_item_single")
		executeRun("", RaLib.Inventory.canAcceptOne, "with storage ra:temp inv_item")
		executeStoreRun("if score #inv_can_insert ${Obj.TEMP} matches 1..", "@s", RaLib.Inventory.insert, "with storage ra:temp mover_item_single")
	}

	/** Remove what was inserted (#mover_inserted) from the first entry of the container here. */
	private fun Function.consumeInput() {
		raw("execute if score #mover_inserted ${Obj.TEMP} matches ..0 run return 0")
		raw("execute store result storage ra:temp inv_consume.count int 1 run scoreboard players get #mover_inserted ${Obj.TEMP}")
		returnRun { function(RaLib.Inventory.consumeEntry0Count, Storage.TEMP, "inv_consume") }
	}

	/** Consume from the primary input or its partner half, per #mover_input_partner. At the primary input. */
	private fun Function.consumeSelectedInput() {
		executeRun("if score #mover_input_partner ${Obj.TEMP} matches 0", consumeInput)
		for (h in CHEST_HALVES) executeRun("if score #mover_input_partner ${Obj.TEMP} matches 1 ${h.toPartner}", consumeInput)
		returnValue(1)
	}
}
