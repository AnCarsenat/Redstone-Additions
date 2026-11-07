package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.placeBlock
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.modules.infinite.breakCheck
import ra.core.item.loreItem
import ra.modules.infinite.registerBlockMessage

/**
 * Message Block: a note block that sends its `message_block` text to `entity_selector` on the
 * rising edge of a redstone signal.
 */
object MessageBlock {
	val block = CustomBlock(Ns.INTERACTIVE, "message_block", "Message Block", model = "minecraft:note_block")
	val item = block.loreItem {}
	val registerBlock = block.fn("register_block")
	private val send = block.fn("send_message_block")
	private val sendMacro = block.fn("send_message_block_macro")

	private const val WAS_POWERED = "ra.was_powered"

	/** `[Message] <text>`: the message is stored JSON text, so it is interpreted. */
	private const val MESSAGE = """[{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]"""

	fun Ra.registerMessageBlock() {
		block(block)
		pack.registerBlockMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(
			block.ns.id, block.id, item,
			pattern = listOf("CCC", "BNR", "CCC"),
			keys = listOf("C" to listOf(Items.COBBLESTONE), "B" to listOf(Items.BOOK), "N" to listOf(Items.NOTE_BLOCK), "R" to listOf(Items.REDSTONE)),
			unlock = has(Items.NOTE_BLOCK),
		)

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("message_block", "Text: ", "aqua", y = 0.8)
		}
		pack.define(send) { send() }
		pack.define(sendMacro) { sendMacro() }

		val onBreak = pack.define(block.fn("on_break")) {
			killVanillaDrop(block.physical)
			dropSpec(item, dropTemp = true)
			breakSound("minecraft:block.wood.break")
			breakCloud()
			kill(self())
		}

		pack.define(block.tick) {
			// Excludes new blocks, whose marker exists a moment before its note block does.
			breakCheck(block, Blocks.NOTE_BLOCK, onBreak, skipNew = true)

			executeRun("as @e[type=marker,tag=${block.tag},tag=!${Tag.NEW}] at @s", RaLib.Redstone.detectSwitch)

			// Trigger on the rising edge (just powered).
			executeRun("as @e[type=marker,tag=${block.tag},tag=!$WAS_POWERED,tag=${Tag.POWERED}] at @s", send)

			// Track power state for edge detection.
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = Tag.POWERED }) { add(WAS_POWERED) }
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = "!${Tag.POWERED}" }) { remove(WAS_POWERED) }
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placeGuard(block)
		// Note block with a marker (dir_type 0 = no direction).
		placeBlock(block)
		// Default properties first.
		raw("""data modify entity @e[type=marker,tag=${block.tag},tag=${Tag.NEW},distance=..0.5,sort=nearest,limit=1] data.properties set value {message_block:"Hello World!",entity_selector:"@a[distance=..16]"}""")
		raw("tag @e[type=marker,tag=${block.tag},tag=${Tag.NEW},distance=..0.5] remove ${Tag.NEW}")
		returnValue(1)
	}

	/** Send the message to the selected players. As the marker, at its position. */
	private fun Function.send() {
		raw("data modify storage ra:temp message_block set from entity @s data.properties.message_block")

		// Preferred targeting model: an explicit entity selector string.
		raw("execute if data entity @s data.properties.entity_selector run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
		executeRun("if data entity @s data.properties.entity_selector", sendMacro, "with storage ra:temp")
		raw("execute if data entity @s data.properties.entity_selector run return 1")

		// Legacy fallback for old blocks that still use `range`.
		function(RaLib.Util.property, nbt { this["name"] = "range"; this["default"] = 16; this["min"] = 1 })
		raw("scoreboard players operation @s ${Obj.TEMP} = #prop ${Obj.TEMP}")
		raw("execute if score @s ${Obj.TEMP} matches ..0 run scoreboard players set @s ${Obj.TEMP} 16")

		// Players within range, in brackets: a selector's distance cannot come from a score.
		for ((range, dist) in listOf("..8" to 8, "9..16" to 16, "17..32" to 32, "33.." to 64))
			raw("execute if score @s ${Obj.TEMP} matches $range run tellraw @a[distance=..$dist] $MESSAGE")
		for ((range, dist) in listOf("..16" to 16, "17..32" to 32, "33.." to 64))
			raw("execute if score @s ${Obj.TEMP} matches $range run playsound minecraft:block.note_block.chime block @a[distance=..$dist] ~ ~ ~ 0.5 1.2")
	}

	/** Macro `{entity_selector}`: the send path using the block's own selector. */
	private fun Function.sendMacro() {
		raw("\$execute at @s as \$(entity_selector) if entity @s[type=player] run tellraw @s $MESSAGE")
		raw("\$execute at @s as \$(entity_selector) if entity @s[type=player] run playsound minecraft:block.note_block.chime block @s ~ ~ ~ 0.5 1.2")
	}
}
