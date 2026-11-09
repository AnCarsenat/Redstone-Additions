package ra.modules.security.keypad

import io.github.ayfri.kore.functions.Function
import ra.core.Admin
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.modules.automation.pattern.Pattern

/**
 * What showing a Pattern to a [Keypad] does, and the clock that ends its pulses and lockouts.
 *
 * Everything starts from a Pattern use ([Pattern.targets]): nothing runs while nobody uses one.
 *
 * THE COMPARISON copies the stored code into storage and overwrites it with the entered `slots`
 * (`execute store success ... data modify ... set from`): success 0 means nothing changed, so the
 * two lists are equal, whole, in order. Not a `custom_data~` match, which is partial on lists.
 * Nothing the player holds is ever pasted into a command.
 *
 * THE CLOCK. A keypad that is pulsing carries [PULSE], one that is locked out [LOCKED], and either
 * also [TIMED]. While one is [PULSE], [run] runs every tick (pulse lengths are exact); while only
 * [LOCKED] ones are left, once a second; with neither, nothing is scheduled. A keypad whose chunk
 * unloaded mid-pulse is found by the block's tick ([startIfIdle], one score test while the clock
 * runs), which starts the clock again.
 */
object KeypadUse {
	/** `storage ra_security:keypad`: scratch. */
	private const val STORE = "ra_security:keypad"
	private const val T = Obj.TEMP
	private const val USER = "@a[tag=${Pattern.USER},limit=1]"

	/** On a keypad powering itself. */
	private const val PULSE = "ra.sec.kp_pulse"
	/** On a keypad refusing every try until `data.data.locked_until`. */
	private const val LOCKED = "ra.sec.kp_locked"
	/** On a keypad with [PULSE] or [LOCKED]: what the clock looks for. */
	private const val TIMED = "ra.sec.kp_timed"
	/** 1 while [run] is scheduled. */
	private const val FLAG = "#sec.kp_clock"

	private fun fn(path: String) = Keypad.block.fn(path)

	/** As the marker, the player tagged [Pattern.USER]. */
	val use = fn("pattern_use")
	/** As the marker, the player tagged [Pattern.USER]: 1 when that player owns it or is an admin. */
	val isOwner = fn("is_owner")
	/** As the marker: forget the code. */
	val clearCode = fn("clear_code")
	/** The clock; cleared by the uninstall. */
	val run = fn("clock/run")
	private val startFast = fn("clock/start_fast")
	private val startSlow = fn("clock/start_slow")
	private val pulseDue = fn("clock/pulse_due")
	private val lockDue = fn("clock/lock_due")
	private val configure = fn("configure")
	private val enter = fn("enter")
	private val accept = fn("accept")
	private val reject = fn("reject")
	private val locked = fn("locked")
	private val count = fn("count")

	/** In the block's tick: one score test while the clock runs. */
	fun Function.startIfIdle() = executeRun("unless score $FLAG $T matches 1 if entity @e[type=minecraft:marker,tag=$TIMED,limit=1]", startFast)

	private fun say(text: String, color: String) = """tellraw $USER [$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]"""
	private fun sound(id: String, pitch: String) = "playsound $id block ${soundListenersSel(10)} ~ ~ ~ 0.8 $pitch"
	private const val FAIL_SOUND = "minecraft:block.note_block.bass"
	private const val SLOTS = """SelectedItem.components."minecraft:custom_data".ra.pattern.slots"""

	fun Ra.registerKeypadUse() {
		pack.define(use) {
			raw("scoreboard players set #pat.hit $T 1")
			raw("execute if entity @a[tag=${Pattern.USER},limit=1,predicate=ra:is_sneaking] run return run function ${configure.asId()}")
			raw("function ${enter.asId()}")
		}

		pack.define(isOwner) {
			raw("execute if entity @a[tag=${Pattern.USER},tag=${Admin.TAG},limit=1] run return 1")
			raw("execute unless data entity @s data.data.owner run return 0")
			raw("data modify storage $STORE u set from entity @s data.data.owner")
			raw("execute store success score #kp.d $T run data modify storage $STORE u set from entity $USER UUID")
			raw("data remove storage $STORE u")
			raw("execute if score #kp.d $T matches 0 run return 1")
			raw("return 0")
		}

		// Sneaking: set (or, with a blank Pattern, clear) the code.
		pack.define(configure) {
			raw("scoreboard players set #kp.own $T 1")
			raw("execute if data entity @s data.data.code store result score #kp.own $T run function ${isOwner.asId()}")
			raw("execute if score #kp.own $T matches 0 run ${sound(FAIL_SOUND, "0.7")}")
			raw("execute if score #kp.own $T matches 0 run return run ${say("Only this Keypad's owner can change its code.", "red")}")
			raw("data remove storage $STORE new")
			raw("data modify storage $STORE new set from entity $USER $SLOTS")
			raw("execute store result score #kp.n $T run function ${count.asId()}")
			raw("data remove storage $STORE new")
			raw("execute if score #kp.n $T matches ..0 if data entity @s data.data.code run function ${clearCode.asId()}")
			raw("execute if score #kp.n $T matches ..0 run ${sound("minecraft:item.book.page_turn", "1.0")}")
			raw("execute if score #kp.n $T matches ..0 run return run ${say("Keypad code cleared. Sneak + right-click it with a recorded Pattern to set one.", "yellow")}")
			raw("data modify entity @s data.data.code set from entity $USER $SLOTS")
			raw("data modify entity @s data.status.code set value \"set\"")
			raw("data modify entity @s data.data.fails set value 0")
			raw(say("Keypad Controller code set. Right-click it with the same Pattern to open it.", "green"))
			raw(sound("minecraft:ui.cartography_table.take_result", "1.2"))
		}

		pack.define(clearCode) {
			raw("data remove entity @s data.data.code")
			raw("data modify entity @s data.status.code set value \"not set\"")
			raw("data modify entity @s data.data.fails set value 0")
		}

		// `storage new` -> how many of its nine entries are not "" (0 for a list that is not nine long).
		pack.define(count) {
			raw("execute unless data storage $STORE new[8] run return 0")
			raw("execute if data storage $STORE new[9] run return 0")
			raw("scoreboard players set #kp.n $T 0")
			for (n in 0 until 9) {
				raw("data modify storage $STORE cur set from storage $STORE new[$n]")
				raw("execute unless data storage $STORE {cur:\"\"} run scoreboard players add #kp.n $T 1")
			}
			raw("data remove storage $STORE cur")
			raw("return run scoreboard players get #kp.n $T")
		}

		// Not sneaking: try the Pattern held against the code.
		pack.define(enter) {
			raw("execute unless data entity @s data.data.code run return run ${say("This Keypad has no code yet: sneak + right-click it with a Pattern to set one.", "gray")}")
			raw("execute store result score #kp.now $T run time query gametime")
			raw("scoreboard players set #kp.lock $T 0")
			raw("execute if data entity @s data.data.locked_until store result score #kp.lock $T run data get entity @s data.data.locked_until")
			raw("execute if score #kp.now $T < #kp.lock $T run return run function ${locked.asId()}")
			raw("data remove storage $STORE in")
			raw("data modify storage $STORE in set from entity $USER $SLOTS")
			raw("scoreboard players set #kp.diff $T 1")
			raw("execute if data storage $STORE in run data modify storage $STORE cmp set from entity @s data.data.code")
			raw("execute if data storage $STORE in store success score #kp.diff $T run data modify storage $STORE cmp set from storage $STORE in")
			raw("data remove storage $STORE in")
			raw("data remove storage $STORE cmp")
			raw("execute if score #kp.diff $T matches 0 run return run function ${accept.asId()}")
			raw("function ${reject.asId()}")
		}

		pack.define(accept) {
			raw("data modify entity @s data.data.fails set value 0")
			raw("scoreboard players set #kp.p $T ${Keypad.DEFAULT_PULSE}")
			raw("execute if data entity @s data.properties.pulse store result score #kp.p $T run data get entity @s data.properties.pulse")
			raw("execute if score #kp.p $T matches ..0 run scoreboard players set #kp.p $T 1")
			raw("scoreboard players operation #kp.p $T += #kp.now $T")
			raw("execute store result entity @s data.data.until int 1 run scoreboard players get #kp.p $T")
			raw("execute if block ~ ~ ~ minecraft:target run setblock ~ ~ ~ minecraft:target[power=15]")
			raw("tag @s add $PULSE")
			raw("tag @s add $TIMED")
			raw("data modify entity @s data.status.state set value \"open\"")
			raw("function ${startFast.asId()}")
			raw(say("Code accepted.", "green"))
			raw(sound("minecraft:block.note_block.chime", "1.4"))
		}

		pack.define(reject) {
			raw("execute store result score #kp.f $T run data get entity @s data.data.fails")
			raw("scoreboard players add #kp.f $T 1")
			raw("scoreboard players set #kp.max $T ${Keypad.DEFAULT_ATTEMPTS}")
			raw("execute if data entity @s data.properties.max_attempts store result score #kp.max $T run data get entity @s data.properties.max_attempts")
			raw("execute store result entity @s data.data.fails int 1 run scoreboard players get #kp.f $T")
			raw(sound(FAIL_SOUND, "0.5"))
			raw("execute if score #kp.f $T < #kp.max $T run return run ${say("Wrong code.", "red")}")
			// Too many: locked for `lockout` seconds.
			raw("data modify entity @s data.data.fails set value 0")
			raw("scoreboard players set #kp.l $T ${Keypad.DEFAULT_LOCKOUT}")
			raw("execute if data entity @s data.properties.lockout store result score #kp.l $T run data get entity @s data.properties.lockout")
			raw("execute if score #kp.l $T matches ..0 run scoreboard players set #kp.l $T 1")
			raw("scoreboard players set #kp.20 $T 20")
			raw("scoreboard players operation #kp.u $T = #kp.l $T")
			raw("scoreboard players operation #kp.u $T *= #kp.20 $T")
			raw("scoreboard players operation #kp.u $T += #kp.now $T")
			raw("execute store result entity @s data.data.locked_until int 1 run scoreboard players get #kp.u $T")
			raw("tag @s add $LOCKED")
			raw("tag @s add $TIMED")
			raw("data modify entity @s data.status.state set value \"locked\"")
			raw("execute unless score $FLAG $T matches 1 run function ${startSlow.asId()}")
			raw("""tellraw $USER [$RA_CHAT_PREFIX,{text:"Wrong code. The Keypad is locked for ",color:"red"},{score:{name:"#kp.l",objective:"$T"},color:"yellow"},{text:" seconds.",color:"red"}]""")
		}

		// `#kp.now` before `#kp.lock`: how long is left, in seconds rounded up.
		pack.define(locked) {
			raw("scoreboard players operation #kp.lock $T -= #kp.now $T")
			raw("scoreboard players add #kp.lock $T 19")
			raw("scoreboard players set #kp.20 $T 20")
			raw("scoreboard players operation #kp.lock $T /= #kp.20 $T")
			raw("""tellraw $USER [$RA_CHAT_PREFIX,{text:"This Keypad is locked: try again in ",color:"red"},{score:{name:"#kp.lock",objective:"$T"},color:"yellow"},{text:" s.",color:"red"}]""")
			raw(sound(FAIL_SOUND, "0.5"))
		}

		registerClock()
	}

	private fun Ra.registerClock() {
		pack.define(startFast) {
			raw("scoreboard players set $FLAG $T 1")
			raw("schedule function ${run.asId()} 1t replace")
		}
		pack.define(startSlow) {
			raw("scoreboard players set $FLAG $T 1")
			raw("schedule function ${run.asId()} 20t replace")
		}
		pack.define(run) {
			raw("scoreboard players set $FLAG $T 0")
			raw("execute store result score #kp.now $T run time query gametime")
			raw("execute as @e[type=minecraft:marker,tag=$PULSE] at @s run function ${pulseDue.asId()}")
			raw("execute as @e[type=minecraft:marker,tag=$LOCKED] run function ${lockDue.asId()}")
			raw("execute if entity @e[type=minecraft:marker,tag=$PULSE,limit=1] run return run function ${startFast.asId()}")
			raw("execute if entity @e[type=minecraft:marker,tag=$LOCKED,limit=1] run function ${startSlow.asId()}")
		}
		// As a pulsing keypad, at it.
		pack.define(pulseDue) {
			raw("execute store result score #kp.u $T run data get entity @s data.data.until")
			raw("execute if score #kp.u $T > #kp.now $T run return 0")
			raw("execute if block ~ ~ ~ minecraft:target run setblock ~ ~ ~ minecraft:target[power=0]")
			raw("tag @s remove $PULSE")
			raw("data remove entity @s data.data.until")
			raw("execute unless entity @s[tag=$LOCKED] run data modify entity @s data.status.state set value \"ready\"")
			raw("execute unless entity @s[tag=$LOCKED] run tag @s remove $TIMED")
		}
		// As a locked keypad.
		pack.define(lockDue) {
			raw("execute store result score #kp.u $T run data get entity @s data.data.locked_until")
			raw("execute if score #kp.u $T > #kp.now $T run return 0")
			raw("tag @s remove $LOCKED")
			raw("data remove entity @s data.data.locked_until")
			raw("execute if entity @s[tag=$PULSE] run data modify entity @s data.status.state set value \"open\"")
			raw("execute unless entity @s[tag=$PULSE] run data modify entity @s data.status.state set value \"ready\"")
			raw("execute unless entity @s[tag=$PULSE] run tag @s remove $TIMED")
		}
	}

	/** In the module's load: a schedule that did not survive is started again by [startIfIdle]. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG $T 0")
}
