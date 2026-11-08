package ra.modules.storage

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.modules.interactive.specGive
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.block.breakCheck
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.macroCall
import ra.modules.interactive.hopperCooldown
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.markerSel
import ra.core.block.soundListenersSel
import ra.core.block.particleViewersSel
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key

/**
 * Boxer: while powered, packs the whole of its `input1` container into one Item Crate and puts
 * the crate into `output1`. A dropper; `input1` / `output1` are offsets, rear and front by default.
 */
object Boxer {
	val block = CustomBlock(Ns.STORAGE, "boxer", "Boxer", model = "minecraft:dropper", dirType = 2)
	val item = block.loreItem { lore(loreLine("Compacts input1 container into output1 boxes", "gray")) }
	val registerBlock = block.fn("register_block")
	private val process = block.fn("process")

	private const val BOX = """storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box"""

	fun Ra.registerBoxer() {
		block(block)
		properties(block) {
			text("input1")
			text("output1")
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.DROPPER), count = null) {
			pattern("PPP", "RDR", " I ")
			key("P", listOf(Items.PAPER))
			key("R", listOf(Items.REDSTONE))
			key("D", listOf(Items.DROPPER))
			key("I", listOf(Items.IRON_INGOT))
		}

		pack.define(block.handlePlacement) { handlePlacement(block, input = "^ ^ ^-1") }
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)
			ioDefaults(block, input = "^ ^ ^-1")
			executeRun("as ${block.markerSel()} at @s", RaLib.Redstone.detectSwitch)
			executeRun("as ${block.markerSel("tag=${Tag.POWERED}")} at @s", process, "with entity @s data.properties")
		}
	}

	/**
	 * Macro `{input1, output1}` (the marker's properties): copy every item of input1 into one
	 * crate and insert it into output1.
	 */
	private fun Function.process() {
		// Hopper-like cooldown.
		hopperCooldown()

		// Valid input and output containers, and something in the input.
		raw("\$execute positioned \$(input1) unless block ~ ~ ~ #ra_lib:containers run return 0")
		raw("\$execute positioned \$(output1) unless block ~ ~ ~ #ra_lib:containers run return 0")
		raw("\$execute positioned \$(input1) unless data block ~ ~ ~ Items[0] run return 0")

		// Build one crate from the entire input container, inlined.
		val atInput = "\$execute positioned \$(input1) run"
		raw("$atInput data modify storage ra:temp storage_box.target_box set value ${StorageBox.crate.stackSnbt()}")
		raw("$atInput execute if data block ~ ~ ~ Items run data modify $BOX.items set from block ~ ~ ~ Items")
		// A preview line for each of the first five stacks.
		for (i in 0..4) {
			val ifItem = "$atInput execute if data $BOX.items[$i]"
			raw("$ifItem run data modify storage ra:temp storage_box.input_macro.id set from $BOX.items[$i].id")
			raw("$ifItem store result storage ra:temp storage_box.input_macro.count int 1 run data get $BOX.items[$i].count")
			raw("$ifItem run ${macroCall(StorageBox.makePreviewFromInput, "with storage ra:temp storage_box.input_macro")}")
			raw("$ifItem run data modify $BOX.preview append from storage ra:temp storage_box.input_preview")
		}
		raw("$atInput ${macroCall(StorageBox.updateLoreStorageTarget)}")
		raw("$atInput data modify storage ra:temp storage_box.generated_item set from storage ra:temp storage_box.target_box")

		// Insert only if the output can take one crate.
		raw("data modify storage ra:temp mover_item set from storage ra:temp storage_box.generated_item")
		raw("\$execute positioned \$(output1) run ${macroCall(ModulesApi.Interactive.itemMoverCheckOutputCapacity)}")
		raw("execute if score #mover_can_forward ${Obj.TEMP} matches 0 run return 0")

		raw("\$execute positioned \$(output1) run ${macroCall(ModulesApi.Interactive.itemMoverTryInsert, "with storage ra:temp mover_item")}")
		raw("execute if score #mover_inserted ${Obj.TEMP} matches 0 run return 0")

		// Empty the input only once the crate is in.
		raw("\$execute positioned \$(input1) run data remove block ~ ~ ~ Items")

		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("playsound minecraft:block.wood.place block ${soundListenersSel()} ~ ~ ~ 0.8 1.2")
		raw("particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5 normal ${particleViewersSel()}")
	}
}
