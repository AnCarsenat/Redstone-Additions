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
import ra.core.block.killVanillaDrop
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

/**
 * Keypad Controller: a vanilla crafter whose slot toggles are the keypad. Players open its grid
 * and click empty slots to switch them off and on; the code is the set of switched-off slots.
 *
 * - A combination is entered once the grid has stopped changing for 4 seconds (see [KeypadUse]).
 *   The grid is then switched back on, whatever happened.
 * - No code yet: the first combination entered (at least one slot off) becomes the code.
 * - Code set: the right combination gives a redstone pulse of `pulse` ticks and a chime; a wrong
 *   one buzzes, and after `max_attempts` wrong tries in a row it refuses everything for `lockout`
 *   seconds.
 * - Wrench, plain RMB: the owner (the player who placed it) or an admin (`ra.admin`) clears the
 *   code; the next combination entered becomes the new one. Shift+RMB cycles the pulse length.
 * - Items put into the crafter are thrown back out the next tick: it never crafts.
 *
 * THE PULSE. A crafter is no power source, so for the pulse the crafter is swapped for a powered
 * target block (`target[power=15]`, a source that powers dust and components around it, as the
 * Keypad always was), drawn as a lit crafter by a block display. When the pulse ends the crafter
 * comes back, facing the same way, empty and with every slot on.
 *
 * SECRECY. The code lives in `data.data.code`, never in `data.properties`: the Data Handler, the
 * Clipboard and the Goggles only ever read properties and status, so none of them can show or
 * copy it. Players see whether a code is set (`data.status.code`) and who owns the keypad.
 *
 * DATA. `data.data.code`: the code, `disabled_slots` as the crafter stores it (an int array).
 * `data.data.seen`: the grid at the last look. `data.data.due`: the game time the current entry
 * counts as entered. `data.data.owner`: the placer's UUID. `data.data.fails`: wrong tries since
 * the last right one or lockout. `data.data.locked_until` / `data.data.until`: game times, the end
 * of the lockout and of the pulse. `data.data.orient`: the crafter's orientation during a pulse.
 * `data.properties.owner` (read-only) and `data.status.owner`: the placer's name, read from a
 * player head filled with their profile.
 */
object Keypad {
	val block = CustomBlock(Ns.SECURITY, "keypad", "Keypad Controller", model = "minecraft:crafter")
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
	/** As a marker broken mid-pulse: the target's drop and the crafter display go. */
	private val pulseBreak = block.fn("pulse_break")
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
			val m = block.newMarkerSel()
			setOnNew(block, "data.properties", "{pulse:$DEFAULT_PULSE,max_attempts:$DEFAULT_ATTEMPTS,lockout:$DEFAULT_LOCKOUT,owner:\"?\"}")
			setOnNew(block, "data.status", """{code:"not set",state:"ready",owner:"?"}""")
			setOnNew(block, "data.data", "{fails:0,seen:[I;]}")
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
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"pulse ",color:"gray"},{nbt:"data.properties.pulse",entity:"@s",color:"aqua"},{text:" ticks",color:"gray"}]""")
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
			// Broken mid-pulse: the block was the target standing in for the crafter.
			raw("execute if entity @s[tag=${KeypadUse.PULSE}] run function ${pulseBreak.asId()}")
		}
		pack.define(pulseBreak) {
			killVanillaDrop("minecraft:target")
			raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=minecraft:block_display,tag=${KeypadUse.SKIN},distance=..0.4]")
		}
		// A keypad is broken when its block is no crafter, or, while it pulses, no target either.
		// Every slot on and nothing inside is idle: a block test and two NBT tests, nothing else.
		// Items are thrown out every tick: a crafter crafts in a scheduled block tick, which runs
		// after the data pack's tick, so the craft never sees them.
		pack.define(look) {
			raw("execute unless block ~ ~ ~ minecraft:crafter unless block ~ ~ ~ minecraft:target run return run function ${onBreak.asId()}")
			raw("execute unless block ~ ~ ~ minecraft:crafter unless entity @s[tag=${KeypadUse.PULSE}] run return run function ${onBreak.asId()}")
			raw("execute unless block ~ ~ ~ minecraft:crafter run return 0")
			raw("execute if data block ~ ~ ~ Items[0] run function ${KeypadUse.eject.asId()}")
			raw("execute if entity @s[tag=${KeypadUse.ENTRY}] run return run function ${KeypadUse.watch.asId()}")
			raw("execute if data block ~ ~ ~ disabled_slots[0] run function ${KeypadUse.watch.asId()}")
		}
		pack.define(block.tick) {
			raw("execute as ${block.markerSel()} at @s run function ${look.asId()}")
			startIfIdle()
		}

		registerKeypadUse()
	}
}
