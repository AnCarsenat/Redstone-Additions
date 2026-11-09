package ra.modules.security.keypad

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TagRoute
import ra.core.Tag
import ra.core.WrenchCycle
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.automation.pattern.Pattern
import ra.modules.security.keypad.KeypadUse.registerKeypadUse
import ra.modules.security.keypad.KeypadUse.startIfIdle

/**
 * Keypad: a target block that powers itself for `pulse` ticks when the right Pattern is shown to
 * it. The code is a [Pattern]: two codes are equal when their `slots` lists are.
 *
 * - Right-click it with a Pattern: enter that Pattern. Right: a pulse and a chime. Wrong: a buzz,
 *   and after `max_attempts` wrong tries in a row it refuses everything for `lockout` seconds.
 * - Sneak + right-click it with a Pattern: set the code (a blank Pattern clears it). Allowed while
 *   it has no code, and always for its owner (the player who placed it) and admins (`ra.admin`).
 * - Wrench, plain RMB: the owner or an admin clears the code. Shift+RMB cycles the pulse length.
 * The Pattern always stays in the player's hand.
 *
 * SECRECY. The code lives in `data.data.code`, never in `data.properties`: the Data Handler, the
 * Clipboard and the Goggles only ever read properties and status, so none of them can show or
 * copy it. Players see whether a code is set (`data.status.code`) and who owns the keypad.
 *
 * DATA. `data.data.owner`: the placer's UUID. `data.data.fails`: wrong tries since the last right
 * one or lockout. `data.data.locked_until` / `data.data.until`: game times, the end of the lockout
 * and of the pulse. `data.properties.owner` (read-only) and `data.status.owner`: the placer's
 * name, read from a player head filled with their profile.
 */
object Keypad {
	val block = CustomBlock(Ns.SECURITY, "keypad", "Keypad Controller", model = "minecraft:target")
	val item = block.loreItem {
		lore(
			loreLine("Powers itself when shown the right Pattern", "gray"),
			loreLine("Sneak + right-click with a Pattern to set the code", "dark_gray"),
			loreLine("Right-click with a Pattern to enter it", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	/** Wrench, plain RMB: the owner or an admin clears the code. As the marker. */
	private val wrenchClear = block.fn("wrench_clear")
	private val cyclePulse = block.fn("cycle_pulse")
	/** The item display that holds a player head for one command, while the placer's profile is read from it. */
	private const val nameTag = "ra.sec.name"

	private const val T = Obj.TEMP
	const val DEFAULT_PULSE = 20
	const val DEFAULT_ATTEMPTS = 3
	const val DEFAULT_LOCKOUT = 30

	fun Ra.registerKeypad() {
		block(block)
		properties(block) {
			number("pulse", min = 1, max = 200, default = DEFAULT_PULSE)
			number("max_attempts", min = 1, max = 20, adminOnly = true, goggles = false, default = DEFAULT_ATTEMPTS)
			number("lockout", min = 1, max = 3600, adminOnly = true, goggles = false, default = DEFAULT_LOCKOUT)
			text("owner", readonly = true, goggles = false)
		}
		wrenchCycle(block.id, WrenchCycle("Pulse", "pulse", cyclePulse))
		wrenchToggles += TagRoute(block.tag, wrenchClear)
		Pattern.targets += TagRoute(block.tag, KeypadUse.use)
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.TARGET)) {
			pattern("BBB", "ITI", "IRI")
			key("B", Items.STONE_BUTTON)
			key("I", Items.IRON_INGOT)
			key("T", Items.TARGET)
			key("R", Items.REDSTONE)
		}

		// As the bat, at the block.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			val m = block.newMarkerSel()
			setOnNew(block, "data.properties", "{pulse:$DEFAULT_PULSE,max_attempts:$DEFAULT_ATTEMPTS,lockout:$DEFAULT_LOCKOUT,owner:\"?\"}")
			setOnNew(block, "data.status", """{code:"not set",state:"ready",owner:"?"}""")
			setOnNew(block, "data.data", "{fails:0}")
			raw("data modify entity $m data.data.owner set from entity @a[tag=${Tag.PLACER},limit=1] UUID")
			// The placer's name: a player head given their profile, read back, thrown away.
			raw("""summon minecraft:item_display ~ ~ ~ {Tags:["ra","$nameTag"],item:{id:"minecraft:player_head",count:1}}""")
			raw("""execute as @a[tag=${Tag.PLACER},limit=1] run item modify entity @n[type=minecraft:item_display,tag=$nameTag] contents {type:"minecraft:fill_player_head",entity:"this"}""")
			raw("data modify entity $m data.properties.owner set from entity @n[type=minecraft:item_display,tag=$nameTag] item.components.\"minecraft:profile\".name")
			raw("data modify entity $m data.status.owner set from entity $m data.properties.owner")
			raw("kill @e[type=minecraft:item_display,tag=$nameTag]")
			finishPlacement(block)
		}

		pack.define(wrenchClear) {
			val wrencher = "@a[tag=ra.wrench_active,distance=..8,sort=nearest,limit=1]"
			raw("execute unless data entity @s data.data.code run return run tellraw $wrencher [{text:\"[Wrench] \",color:\"gold\"},{text:\"${block.name}: \",color:\"gray\"},{text:\"no code set\",color:\"yellow\"}]")
			raw("execute as $wrencher run tag @s add ${Pattern.USER}")
			raw("execute store result score #kp.own $T run function ${KeypadUse.isOwner.asId()}")
			raw("tag @a remove ${Pattern.USER}")
			raw("execute if score #kp.own $T matches 0 run return run tellraw $wrencher [{text:\"[Wrench] \",color:\"gold\"},{text:\"${block.name}: \",color:\"gray\"},{text:\"only its owner or an admin can clear the code\",color:\"red\"}]")
			raw("function ${KeypadUse.clearCode.asId()}")
			raw("tellraw $wrencher [{text:\"[Wrench] \",color:\"gold\"},{text:\"${block.name}: \",color:\"gray\"},{text:\"code cleared\",color:\"aqua\"}]")
			raw("playsound minecraft:item.book.page_turn block ${soundListenersSel(10)} ~ ~ ~ 0.7 1.0")
		}

		// 2 -> 10 -> 20 -> 40 -> 2 ticks. Read first, then write.
		pack.define(cyclePulse) {
			raw("execute store result score #kp.p $T run data get entity @s data.properties.pulse")
			raw("scoreboard players set #kp.n $T 2")
			raw("execute if score #kp.p $T matches 2..9 run scoreboard players set #kp.n $T 10")
			raw("execute if score #kp.p $T matches 10..19 run scoreboard players set #kp.n $T 20")
			raw("execute if score #kp.p $T matches 20..39 run scoreboard players set #kp.n $T 40")
			raw("execute store result entity @s data.properties.pulse int 1 run scoreboard players get #kp.n $T")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"pulse ",color:"gray"},{nbt:"data.properties.pulse",entity:"@s",color:"aqua"},{text:" ticks",color:"gray"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("code", "Code: ", "aqua")
			stackedStatusLine("owner", "Owner: ", "yellow")
			stackedStatusLine("state", "State: ", "green")
			stackedPropLine("pulse", "Pulse: ", "gray", suffix = "t")
			stackedTextLine("", "Right-click with a Pattern to enter it", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.TARGET)) { startIfIdle() }

		registerKeypadUse()
	}
}
