package ra.modules.storage

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.kill
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
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.modules.infinite.finishLorePlacement
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.modules.infinite.registerBlockMessage
import ra.modules.interactive.breakCloud
import ra.modules.interactive.breakSound
import ra.modules.interactive.dropSpec
import ra.modules.interactive.executeRun
import ra.modules.interactive.has
import ra.modules.interactive.placeGuard
import ra.modules.interactive.shapedRecipe
import ra.modules.interactive.specGive
import io.github.ayfri.kore.commands.scoreboard.scoreboard

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
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("PPP", "RDR", " I "),
			keys = listOf("P" to listOf(Items.PAPER), "R" to listOf(Items.REDSTONE), "D" to listOf(Items.DROPPER), "I" to listOf(Items.IRON_INGOT)),
			unlock = has(Items.DROPPER),
		)

		pack.define(block.handlePlacement) { handlePlacement(block, input = "^ ^ ^-1") }
		pack.define(block.goggles) { gogglesHeader(block, nameY = 1.0) }
		pack.define(process) { process() }

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropSpec(item)
			breakSound()
			breakCloud()
			kill(self())
		}

		pack.define(block.tick) {
			breakCheck(block, Blocks.DROPPER, onBreak)
			ioDefaults(block, input = "^ ^ ^-1")
			executeRun("as @e[type=marker,tag=${block.tag}] at @s", RaLib.Redstone.detectSwitch)
			executeRun("as @e[type=marker,tag=${block.tag},tag=${Tag.POWERED}] at @s", process, "with entity @s data.properties")
		}
	}

	/**
	 * Macro `{input1, output1}` (the marker's properties): copy every item of input1 into one
	 * crate and insert it into output1.
	 */
	private fun Function.process() {
		// Hopper-like cooldown.
		raw("execute unless score @s ${Obj.COOLDOWN} matches -2147483648.. run scoreboard players set @s ${Obj.COOLDOWN} 0")
		scoreboard.players.add(self(), Obj.COOLDOWN, 1)
		raw("execute if score @s ${Obj.COOLDOWN} matches ..3 run return 0")

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
			raw("$ifItem run function ${StorageBox.makePreviewFromInput.asId()} with storage ra:temp storage_box.input_macro")
			raw("$ifItem run data modify $BOX.preview append from storage ra:temp storage_box.input_preview")
		}
		raw("$atInput function ${StorageBox.updateLoreStorageTarget.asId()}")
		raw("$atInput data modify storage ra:temp storage_box.generated_item set from storage ra:temp storage_box.target_box")

		// Insert only if the output can take one crate.
		raw("data modify storage ra:temp mover_item set from storage ra:temp storage_box.generated_item")
		raw("\$execute positioned \$(output1) run function ${ModulesApi.Interactive.itemMoverCheckOutputCapacity.asId()}")
		raw("execute if score #mover_can_forward ${Obj.TEMP} matches 0 run return 0")

		raw("\$execute positioned \$(output1) run function ${ModulesApi.Interactive.itemMoverTryInsert.asId()} with storage ra:temp mover_item")
		raw("execute if score #mover_inserted ${Obj.TEMP} matches 0 run return 0")

		// Empty the input only once the crate is in.
		raw("\$execute positioned \$(input1) run data remove block ~ ~ ~ Items")

		scoreboard.players.set(self(), Obj.COOLDOWN, 0)
		raw("playsound minecraft:block.wood.place block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1.2")
		raw("particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
	}
}

/** As the spawned bat: place [block], seed `input1` = [input] and `output1` = front. Shared by the Boxer and Unboxer. */
internal fun Function.handlePlacement(block: CustomBlock, input: String, afterSeed: Function.() -> Unit = {}) {
	placeGuard(block)
	placeBlock(block)
	val fresh = "@e[type=marker,tag=${block.tag},tag=${Tag.NEW},distance=..0.5,sort=nearest,limit=1]"
	raw("data modify entity $fresh data.properties.input1 set value \"$input\"")
	raw("data modify entity $fresh data.properties.output1 set value \"^ ^ ^1\"")
	afterSeed()
	finishLorePlacement(block)
}

/**
 * Tick-time IO upkeep, shared by the Boxer and Unboxer: default `input1` / `output1` where missing,
 * and the one-time move of the old up/down defaults to rear/front.
 */
internal fun Function.ioDefaults(block: CustomBlock, input: String) {
	// IO defaults the macro processing needs.
	raw("execute as @e[type=marker,tag=${block.tag}] unless data entity @s data.properties.input1 run data modify entity @s data.properties.input1 set value \"$input\"")
	raw("execute as @e[type=marker,tag=${block.tag}] unless data entity @s data.properties.output1 run data modify entity @s data.properties.output1 set value \"^ ^ ^1\"")

	// Migrate legacy up/down defaults to rear/front defaults.
	val migrated = "ra.storage.io_default_migrated"
	raw("execute as @e[type=marker,tag=${block.tag},tag=!$migrated] if data entity @s data.properties{input1:\"^ ^-1 ^\",output1:\"^ ^1 ^\"} run data merge entity @s {data:{properties:{input1:\"^ ^ ^-1\",output1:\"^ ^ ^1\"}}}")
	raw("tag @e[type=marker,tag=${block.tag},tag=!$migrated] add $migrated")
}
