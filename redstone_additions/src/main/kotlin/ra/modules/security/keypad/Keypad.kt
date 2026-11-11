package ra.modules.security.keypad

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TagRoute
import ra.core.Tag
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.markerSel
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
import ra.modules.security.keypad.KeypadUse.registerKeypadUse
import ra.modules.security.keypad.KeypadUse.startIfIdle
import ra.core.block.WRENCH_READOUT

/**
 * Keypad: a vanilla crafter whose slot toggles are the keypad. Players open its grid and click
 * empty slots to switch them off and on; the code is the set of switched-off slots.
 *
 * - A combination is entered once the grid has stopped changing for 1 second (see [KeypadUse]).
 *   The grid is then switched back on, whatever happened.
 * - No code yet: the first combination entered (at least one slot off) becomes the code.
 * - Code set: the right combination gives a redstone pulse of `pulse` ticks and a chime; a wrong
 *   one buzzes, and after `max_attempts` wrong tries in a row it refuses everything for `lockout`
 *   seconds.
 * - Wrench, plain RMB: the owner (the player who placed it) or an admin (`ra.admin`) clears the
 *   code; the next combination entered becomes the new one. Shift+RMB cycles the pulse length.
 * - Items put into the crafter are thrown out of its front face the tick they are seen: it never
 *   crafts.
 *
 * THE PULSE. The pack's standard output, as the sensors give it: for `pulse` ticks every iron
 * block in the 3x3x3 around the keypad is a redstone block, then iron again. The crafter itself
 * stays a crafter. A pulse cut short still ends in iron: a break reverts the blocks before the
 * marker goes, and a keypad still tagged pulsing after a reload or an unload is finished by the
 * clock ([KeypadUse.startIfIdle]).
 *
 * SECRECY. The code lives in `data.data.code`, never in `data.properties`: the Data Handler, the
 * Clipboard and the Goggles only ever read properties and status, so none of them can show or
 * copy it. Players see whether a code is set (`data.status.code`) and who owns the keypad.
 *
 * DATA. `data.data.code`: the code, as a 9-bit mask of the switched-off slots (sum of 2^slot).
 * `data.data.seen`: the grid's mask at the last look. `data.data.said`: when it last said it
 * takes no items. `data.data.due`: the game time the current entry
 * counts as entered. `data.data.owner`: the placer's UUID. `data.data.fails`: wrong tries since
 * the last right one or lockout. `data.data.locked_until` / `data.data.until`: game times, the end
 * of the lockout and of the pulse.
 * `data.properties.owner` (read-only) and `data.status.owner`: the placer's name, read from a
 * player head filled with their profile.
 */
object Keypad {
	val block = CustomBlock(Ns.SECURITY, "keypad", "Keypad", model = "minecraft:crafter", dirType = 3)
	val item = block.loreItem {
		lore(
			loreLine("Gives a redstone pulse for the right code", "gray"),
			loreLine("Open it and switch slots off: that is the code", "dark_gray"),
			loreLine("The first one entered sets it", "dark_gray"),
		)
	}

	val registerBlock = block.fn("register_block")
	/** Wrench, plain RMB: the owner or an admin clears the code. As the marker. */
	private val wrenchClear = block.fn("wrench_clear")
	private val cyclePulse = block.fn("cycle_pulse")
	/** As each marker, at it, every tick: broken, items to throw out, a grid to watch. */
	private val look = block.fn("look")
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
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.CRAFTER)) {
			pattern("BBB", "ICI", "IRI")
			key("B", Items.STONE_BUTTON)
			key("I", Items.IRON_INGOT)
			key("C", Items.CRAFTER)
			key("R", Items.REDSTONE)
		}

		// As the bat, at the block.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			orientCrafter()
			val m = block.newMarkerSel()
			setOnNew(block, "data.properties", "{pulse:$DEFAULT_PULSE,max_attempts:$DEFAULT_ATTEMPTS,lockout:$DEFAULT_LOCKOUT,owner:\"?\"}")
			setOnNew(block, "data.status", """{code:"not set",state:"ready",owner:"?"}""")
			setOnNew(block, "data.data", "{fails:0,seen:0}")
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
			raw("execute as $wrencher run tag @s add ${KeypadUse.USER}")
			raw("execute store result score #kp.own $T run function ${KeypadUse.isOwner.asId()}")
			raw("tag @a remove ${KeypadUse.USER}")
			raw("execute if score #kp.own $T matches 0 run return run tellraw $wrencher [{text:\"[Wrench] \",color:\"gold\"},{text:\"${block.name}: \",color:\"gray\"},{text:\"only its owner or an admin can clear the code\",color:\"red\"}]")
			raw("function ${KeypadUse.clearCode.asId()}")
			raw("tellraw $wrencher [{text:\"[Wrench] \",color:\"gold\"},{text:\"${block.name}: \",color:\"gray\"},{text:\"code cleared: the next combination entered sets a new one\",color:\"aqua\"}]")
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
			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"pulse ",color:"gray"},{nbt:"data.properties.pulse",entity:"@s",color:"aqua"},{text:" ticks",color:"gray"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("code", "Code: ", "aqua")
			stackedStatusLine("owner", "Owner: ", "yellow")
			stackedStatusLine("state", "State: ", "green")
			stackedPropLine("pulse", "Pulse: ", "gray", suffix = "t")
			stackedTextLine("", "Open it and switch slots off to enter a code", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true) {
			// Broken mid-pulse: the redstone blocks go back to iron before the marker goes.
			raw("execute if entity @s[tag=${KeypadUse.PULSE}] run ${KeypadUse.PULSE_OFF_FILL}")
		}
		// A keypad is broken when its block is no crafter.
		// Every tick: one block test and an `if items` test (the container's slots, no NBT read).
		// Items are thrown out every tick because a crafter crafts in a scheduled block tick, which
		// runs after the data pack's tick, so the craft never sees them. The grid is read (an NBT
		// read) only every 4 ticks (`#kp.ph` 0), only with a player within 10 blocks, and only when
		// a slot is off or an entry is under way (which is also when a settled entry is entered).
		pack.define(look) {
			raw("execute unless block ~ ~ ~ minecraft:crafter run return run function ${onBreak.asId()}")
			raw("execute if items block ~ ~ ~ container.* * run function ${KeypadUse.eject.asId()}")
			raw("execute unless score #kp.ph $T matches 0 run return 0")
			raw("execute unless entity @a[distance=..10,limit=1] run return 0")
			raw("execute if entity @s[tag=${KeypadUse.ENTRY}] run return run function ${KeypadUse.watch.asId()}")
			raw("execute if data block ~ ~ ~ disabled_slots[0] run function ${KeypadUse.watch.asId()}")
		}
		pack.define(block.tick) {
			raw("execute store result score #kp.ph $T run time query gametime")
			raw("scoreboard players set #kp.4 $T 4")
			raw("scoreboard players operation #kp.ph $T %= #kp.4 $T")
			raw("execute as ${block.markerSel()} at @s run function ${look.asId()}")
			startIfIdle()
		}

		registerKeypadUse()
	}

	/**
	 * Faces the crafter like a vanilla one placed by the same player: its front toward them
	 * (`ra.facing` from the placement: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east), its top
	 * up, or -- front up or down -- toward where they look. Right after placeBlock, so the
	 * setblock replaces an empty crafter.
	 */
	private fun io.github.ayfri.kore.functions.Function.orientCrafter() {
		val placer = "@a[tag=${Tag.PLACER},limit=1]"
		for ((f, dir) in listOf(2 to "north", 3 to "south", 4 to "west", 5 to "east"))
			raw("execute if score $placer ${ra.core.Obj.FACING} matches $f run setblock ~ ~ ~ minecraft:crafter[orientation=${dir}_up] replace")
		// Front up or down: the top points the way the player looks (yaw: 0 south, 90 west, -90 east, 180 north).
		for ((f, front) in listOf(1 to "up", 0 to "down")) for ((yaw, top) in listOf("-45..45" to "south", "46..135" to "west", "-135..-46" to "east"))
			raw("execute if score $placer ${ra.core.Obj.FACING} matches $f if score $placer ${ra.core.Obj.YAW} matches $yaw run setblock ~ ~ ~ minecraft:crafter[orientation=${front}_$top] replace")
		for ((f, front) in listOf(1 to "up", 0 to "down"))
			raw("execute if score $placer ${ra.core.Obj.FACING} matches $f unless score $placer ${ra.core.Obj.YAW} matches -135..135 run setblock ~ ~ ~ minecraft:crafter[orientation=${front}_north] replace")
	}
}
