package ra.modules.interactive

import ra.core.block.soundListenersSel
import ra.core.TextFormat
import ra.core.text
import ra.lib.safe.SafeText.guardProperty
import ra.modules.sensors.SensorsModule.SELECTOR_SETTING
import io.github.ayfri.kore.arguments.types.literals.allEntities
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
import ra.core.block.placeBlock
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.core.block.breakCheck
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.block.placementGuard
import ra.core.block.dropItem
import ra.core.block.blockOnBreak
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.markerSel
import ra.core.block.setOnNew
import ra.core.block.readProperty
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.commands.returnValue

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

	/** `[Message] <text>`: interpreted, so an operator's compound shows as rich text; a string shows as typed. */
	private const val MESSAGE = """[{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]"""

	fun Ra.registerMessageBlock() {
		block(block)
		properties(block) {
			// Shown, never pasted into a command. Drawn with interpret:true, which decodes the stored
			// tag itself (since 1.21.5): a string is plain text, whatever it spells, and only a
			// compound is rich text that could carry a click_event. Players can only ever write a
			// string (the Data Handler types text, the Clipboard checks it with ra_lib:safe/check),
			// so the text is everyone's; rich text stays with operators and commands.
			text("message_block", TextFormat.FREE)
			// Pasted into a command: admins only unless the server allows it (Sensors settings).
			text("entity_selector", TextFormat.SELECTOR, adminOnly = true, adminSetting = SELECTOR_SETTING)
			number("range", min = 1, max = 128)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.NOTE_BLOCK), count = null) {
			pattern("CCC", "BNR", "CCC")
			key("C", listOf(Items.COBBLESTONE))
			key("B", listOf(Items.BOOK))
			key("N", listOf(Items.NOTE_BLOCK))
			key("R", listOf(Items.REDSTONE))
		}

		pack.define(block.handlePlacement) { handlePlacement() }
		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("message_block", "Text: ", "aqua", y = 0.8)
		}
		pack.define(send) { send() }
		pack.define(sendMacro) { sendMacro() }

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item, tagged = true) }, sound = "minecraft:block.wood.break", cloud = true)

		pack.define(block.tick) {
			// Excludes new blocks, whose marker exists a moment before its note block does.
			breakCheck(block, Blocks.NOTE_BLOCK, onBreak, skipNew = true)

			executeRun("as ${block.markerSel("tag=!${Tag.NEW}")} at @s", RaLib.Redstone.detectSwitch)

			// Trigger on the rising edge (just powered).
			executeRun("as ${block.markerSel("tag=!$WAS_POWERED", "tag=${Tag.POWERED}")} at @s", send)

			// Track power state for edge detection.
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = Tag.POWERED }) { add(WAS_POWERED) }
			tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = "!${Tag.POWERED}" }) { remove(WAS_POWERED) }
		}
	}

	/** As the spawned bat, at its position. */
	private fun Function.handlePlacement() {
		placementGuard(block, checkExisting = false)
		// Note block with a marker (dir_type 0 = no direction).
		placeBlock(block)
		// Default properties first.
		setOnNew(block, "data.properties", """{message_block:"Hello World!",entity_selector:"@a[distance=..16]"}""")
		// Text, not finishPlacement: the typed clearNewTag writes the selector arguments in another
		// order, and this handler keeps the beet spelling.
		raw("tag ${block.markerSel("tag=${Tag.NEW}", "distance=..0.5")} remove ${Tag.NEW}")
		returnValue(1)
	}

	/** Send the message to the selected players. As the marker, at its position. */
	private fun Function.send() {
		raw("data modify storage ra:temp message_block set from entity @s data.properties.message_block")

		// Preferred targeting model: an explicit entity selector string.
		raw("execute if data entity @s data.properties.entity_selector run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
		// Pasted into the command: an unsafe selector sends nothing (ra_lib:safe/guard).
		raw("execute if data entity @s data.properties.entity_selector store result score #sf.g ${Obj.TEMP} run function ${ra.api.RaLib.Safe.guard.asId()} {p:\"entity_selector\",f:${TextFormat.SELECTOR.code}}")
		raw("execute if data entity @s data.properties.entity_selector unless score #sf.g ${Obj.TEMP} matches 1 run return 0")
		executeRun("if data entity @s data.properties.entity_selector", sendMacro, "with storage ra:temp")
		raw("execute if data entity @s data.properties.entity_selector run return 1")

		// Legacy fallback for old blocks that still use `range`.
		readProperty("range", default = 16, min = 1)
		raw("scoreboard players operation @s ${Obj.TEMP} = #prop ${Obj.TEMP}")
		raw("execute if score @s ${Obj.TEMP} matches ..0 run scoreboard players set @s ${Obj.TEMP} 16")

		// Players within range, in brackets: a selector's distance cannot come from a score.
		for ((range, dist) in listOf("..8" to 8, "9..16" to 16, "17..32" to 32, "33.." to 64))
			raw("execute if score @s ${Obj.TEMP} matches $range run tellraw @a[distance=..$dist] $MESSAGE")
		for ((range, dist) in listOf("..16" to 16, "17..32" to 32, "33.." to 64))
			raw("execute if score @s ${Obj.TEMP} matches $range run playsound minecraft:block.note_block.chime block ${soundListenersSel(dist)} ~ ~ ~ 0.5 1.2")
	}

	/** Macro `{entity_selector}`: the send path using the block's own selector. */
	private fun Function.sendMacro() {
		raw("\$execute at @s as \$(entity_selector) if entity @s[type=player] run tellraw @s $MESSAGE")
		raw("\$execute at @s as \$(entity_selector) if entity @s[type=player] run playsound minecraft:block.note_block.chime block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
	}
}
