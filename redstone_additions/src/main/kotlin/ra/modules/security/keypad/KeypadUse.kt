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

/**
 * How a [Keypad] reads its grid, what an entered combination does, and the clock that ends its
 * entries, pulses and lockouts.
 *
 * THE GRID. The Keypad is a vanilla crafter. Its `disabled_slots` (an int array of the switched-off
 * slots, 0..8, ascending) is the combination being typed. The block's tick looks, once per tick,
 * only at keypads whose crafter has a switched-off slot or holds an item, or that are mid-entry
 * ([ENTRY]): an idle keypad (every slot on, nothing inside) costs one block test.
 *
 * ENTERING. A data pack cannot see the grid screen close, so a combination is entered once the grid
 * has not changed for [SETTLE_TICKS] ticks. [watch] copies `disabled_slots` into `data.data.seen`
 * (`execute store success ... set from`): success 1 is a change, which (re)starts the wait
 * (`data.data.due`) and tags the keypad [ENTRY]. The clock then runs [entryDue] once the wait is
 * over: the grid is read, switched back on, and compared with the code. Nothing the player does is
 * ever pasted into a command.
 *
 * NO CRAFTING. Every item put into the crafter (by a player or a hopper) is thrown back out the next
 * tick, before the crafter's own scheduled craft could run, so a redstone signal on it only plays
 * the crafter's "nothing to craft" sound.
 *
 * THE CLOCK. A keypad that is pulsing carries [PULSE], one that is locked out [LOCKED], one waiting
 * for its grid to settle [ENTRY], and any of them [TIMED]. While one is [PULSE], [run] runs every
 * tick (pulse lengths are exact); while only [LOCKED] or [ENTRY] ones are left, once a second;
 * with none, nothing is scheduled. A keypad whose chunk unloaded mid-pulse is found by the block's
 * tick ([startIfIdle], one score test while the clock runs), which starts the clock again.
 */
object KeypadUse {
	/** `storage ra_security:keypad`: scratch. */
	private const val STORE = "ra_security:keypad"
	private const val T = Obj.TEMP

	/** How long the grid must stay unchanged before it counts as entered: 4 seconds. */
	const val SETTLE_TICKS = 80

	/** On a keypad powering itself (its crafter swapped for a powered target block, see [Keypad]). */
	const val PULSE = "ra.sec.kp_pulse"
	/** On a keypad refusing every try until `data.data.locked_until`. */
	private const val LOCKED = "ra.sec.kp_locked"
	/** On a keypad whose grid changed less than [SETTLE_TICKS] ago. */
	const val ENTRY = "ra.sec.kp_entry"
	/** On a keypad with [PULSE], [LOCKED] or [ENTRY]: what the clock looks for. */
	private const val TIMED = "ra.sec.kp_timed"
	/** 1 while [run] is scheduled. */
	private const val FLAG = "#sec.kp_clock"
	/** The block display that draws the crafter over the target block while it pulses. */
	const val SKIN = "ra.sec.kp_skin"
	/** Item entities [eject] just spawned, before their stack is filled in. */
	private const val OUT = "ra.sec.kp_out"
	/** On the player [isOwner] asks about. */
	const val USER = "ra.sec.kp_user"

	/** The crafter's twelve orientations; `data.data.orient` is an index into this list. */
	val ORIENTATIONS = listOf(
		"north_up", "south_up", "east_up", "west_up",
		"up_north", "up_south", "up_east", "up_west",
		"down_north", "down_south", "down_east", "down_west",
	)

	private fun fn(path: String) = Keypad.block.fn(path)

	/** As the marker, the player tagged [USER]: 1 when that player owns it or is an admin. */
	val isOwner = fn("is_owner")
	/** As the marker: forget the code. */
	val clearCode = fn("clear_code")
	/** As the marker, at the block, whose crafter has a switched-off slot or is mid-entry. */
	val watch = fn("watch")
	/** As the marker, at the block, whose crafter holds items: throw them out. */
	val eject = fn("eject")
	private val ejectNext = fn("eject_next")
	/** The clock; cleared by the uninstall. */
	val run = fn("clock/run")
	private val startFast = fn("clock/start_fast")
	private val startSlow = fn("clock/start_slow")
	private val pulseDue = fn("clock/pulse_due")
	private val lockDue = fn("clock/lock_due")
	private val entryDue = fn("clock/entry_due")
	private val setCode = fn("set_code")
	private val accept = fn("accept")
	private val reject = fn("reject")
	private val locked = fn("locked")
	private val pulseOn = fn("pulse_on")
	private val pulseOff = fn("pulse_off")

	/** In the block's tick: one score test while the clock runs. */
	fun Function.startIfIdle() = executeRun("unless score $FLAG $T matches 1 if entity @e[type=minecraft:marker,tag=$TIMED,limit=1]", startFast)

	/** The players who hear about it: whoever is at the grid. */
	private const val NEAR = "@a[distance=..8]"
	private fun say(text: String, color: String) = """tellraw $NEAR [$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]"""
	private fun sound(id: String, pitch: String) = "playsound $id block ${soundListenersSel(10)} ~ ~ ~ 0.8 $pitch"
	private const val FAIL_SOUND = "minecraft:block.note_block.bass"

	/** Every slot back on, and remembered as seen that way. As the marker, at the crafter. */
	private fun Function.resetGrid() {
		raw("execute if block ~ ~ ~ minecraft:crafter run data modify block ~ ~ ~ disabled_slots set value [I;]")
		raw("data modify entity @s data.data.seen set value [I;]")
	}

	fun Ra.registerKeypadUse() {
		pack.define(isOwner) {
			raw("execute if entity @a[tag=$USER,tag=${Admin.TAG},limit=1] run return 1")
			raw("execute unless data entity @s data.data.owner run return 0")
			raw("data modify storage $STORE u set from entity @s data.data.owner")
			raw("execute store success score #kp.d $T run data modify storage $STORE u set from entity @a[tag=$USER,limit=1] UUID")
			raw("data remove storage $STORE u")
			raw("execute if score #kp.d $T matches 0 run return 1")
			raw("return 0")
		}

		pack.define(clearCode) {
			raw("data remove entity @s data.data.code")
			raw("data modify entity @s data.status.code set value \"not set\"")
			raw("data modify entity @s data.data.fails set value 0")
		}

		// A change restarts the wait; no change costs one failed `data modify`.
		pack.define(watch) {
			raw("execute store success score #kp.ch $T run data modify entity @s data.data.seen set from block ~ ~ ~ disabled_slots")
			raw("execute if score #kp.ch $T matches 0 run return 0")
			raw("execute store result score #kp.due $T run time query gametime")
			raw("scoreboard players add #kp.due $T $SETTLE_TICKS")
			raw("execute store result entity @s data.data.due int 1 run scoreboard players get #kp.due $T")
			raw("tag @s add $ENTRY")
			raw("tag @s add $TIMED")
			raw("execute unless score $FLAG $T matches 1 run function ${startSlow.asId()}")
		}

		pack.define(eject) {
			raw("data modify storage $STORE ej set from block ~ ~ ~ Items")
			raw("data modify block ~ ~ ~ Items set value []")
			raw("function ${ejectNext.asId()}")
			raw("data remove storage $STORE ej")
			raw(say("The Keypad Controller takes no items: switch its slots off and on to enter a code.", "gray"))
		}
		pack.define(ejectNext) {
			raw("execute unless data storage $STORE ej[0] run return 0")
			raw("data remove storage $STORE ej[0].Slot")
			raw("""summon minecraft:item ~ ~0.7 ~ {Item:{id:"minecraft:stone",count:1},Tags:["$OUT"],PickupDelay:10s}""")
			raw("data modify entity @n[type=minecraft:item,tag=$OUT,distance=..1.5] Item set from storage $STORE ej[0]")
			raw("tag @e[type=minecraft:item,tag=$OUT] remove $OUT")
			raw("data remove storage $STORE ej[0]")
			raw("function ${ejectNext.asId()}")
		}

		// As a keypad tagged ENTRY, at it, `#kp.now` set: the grid has settled, or not yet.
		pack.define(entryDue) {
			raw("execute store result score #kp.u $T run data get entity @s data.data.due")
			raw("execute if score #kp.u $T > #kp.now $T run return 0")
			raw("tag @s remove $ENTRY")
			raw("data remove entity @s data.data.due")
			raw("execute unless entity @s[tag=$PULSE] unless entity @s[tag=$LOCKED] run tag @s remove $TIMED")
			raw("data remove storage $STORE in")
			raw("execute if block ~ ~ ~ minecraft:crafter run data modify storage $STORE in set from block ~ ~ ~ disabled_slots")
			resetGrid()
			// Every slot back on before the wait ran out: nothing was entered.
			raw("execute unless data storage $STORE in[0] run return run data remove storage $STORE in")
			raw("execute unless data entity @s data.data.code run return run function ${setCode.asId()}")
			raw("scoreboard players set #kp.lock $T 0")
			raw("execute if data entity @s data.data.locked_until store result score #kp.lock $T run data get entity @s data.data.locked_until")
			raw("execute if score #kp.now $T < #kp.lock $T run return run function ${locked.asId()}")
			// Equal when overwriting a copy of the code with the entry changes nothing.
			raw("data modify storage $STORE cmp set from entity @s data.data.code")
			raw("execute store success score #kp.diff $T run data modify storage $STORE cmp set from storage $STORE in")
			raw("data remove storage $STORE in")
			raw("data remove storage $STORE cmp")
			raw("execute if score #kp.diff $T matches 0 run return run function ${accept.asId()}")
			raw("function ${reject.asId()}")
		}

		pack.define(setCode) {
			raw("data modify entity @s data.data.code set from storage $STORE in")
			raw("data remove storage $STORE in")
			raw("data modify entity @s data.status.code set value \"set\"")
			raw("data modify entity @s data.data.fails set value 0")
			raw(say("Keypad Controller code set. Switch the same slots off to open it.", "green"))
			raw(sound("minecraft:ui.cartography_table.take_result", "1.2"))
		}

		pack.define(accept) {
			raw("data modify entity @s data.data.fails set value 0")
			raw("scoreboard players set #kp.p $T ${Keypad.DEFAULT_PULSE}")
			raw("execute if data entity @s data.properties.pulse store result score #kp.p $T run data get entity @s data.properties.pulse")
			raw("execute if score #kp.p $T matches ..0 run scoreboard players set #kp.p $T 1")
			raw("scoreboard players operation #kp.p $T += #kp.now $T")
			raw("execute store result entity @s data.data.until int 1 run scoreboard players get #kp.p $T")
			raw("execute unless entity @s[tag=$PULSE] run function ${pulseOn.asId()}")
			raw("tag @s add $PULSE")
			raw("tag @s add $TIMED")
			raw("data modify entity @s data.status.state set value \"open\"")
			raw("function ${startFast.asId()}")
			raw(say("Code accepted.", "green"))
			raw(sound("minecraft:block.note_block.chime", "1.4"))
		}

		// The crafter becomes a powered target block, as the Keypad used to be, drawn as a crafter.
		pack.define(pulseOn) {
			raw("execute unless block ~ ~ ~ minecraft:crafter run return 0")
			raw("execute if data block ~ ~ ~ Items[0] run function ${eject.asId()}")
			raw("scoreboard players set #kp.o $T 0")
			ORIENTATIONS.forEachIndexed { i, o ->
				if (i > 0) raw("execute if block ~ ~ ~ minecraft:crafter[orientation=$o] run scoreboard players set #kp.o $T $i")
			}
			raw("execute store result entity @s data.data.orient int 1 run scoreboard players get #kp.o $T")
			raw("setblock ~ ~ ~ minecraft:target[power=15] replace")
			ORIENTATIONS.forEachIndexed { i, o ->
				raw(
					"execute if score #kp.o $T matches $i align xyz positioned ~0.5 ~0.5 ~0.5 run summon minecraft:block_display ~ ~ ~ " +
						"{Tags:[\"ra\",\"ra.display\",\"$SKIN\"],brightness:{sky:15,block:15}," +
						"block_state:{Name:\"minecraft:crafter\",Properties:{orientation:\"$o\",crafting:\"true\"}}," +
						"transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.51f,-0.51f,-0.51f],scale:[1.02f,1.02f,1.02f]}}"
				)
			}
		}

		// The crafter comes back, facing as it did, every slot on and nothing inside.
		pack.define(pulseOff) {
			raw("execute store result score #kp.o $T run data get entity @s data.data.orient")
			raw("data remove entity @s data.data.orient")
			ORIENTATIONS.forEachIndexed { i, o ->
				raw("execute if score #kp.o $T matches $i if block ~ ~ ~ minecraft:target run setblock ~ ~ ~ minecraft:crafter[orientation=$o] replace")
			}
			raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=minecraft:block_display,tag=$SKIN,distance=..0.4]")
			raw("data modify entity @s data.data.seen set value [I;]")
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
			raw("""tellraw $NEAR [$RA_CHAT_PREFIX,{text:"Wrong code. The Keypad Controller is locked for ",color:"red"},{score:{name:"#kp.l",objective:"$T"},color:"yellow"},{text:" seconds.",color:"red"}]""")
		}

		// `#kp.now` before `#kp.lock`: how long is left, in seconds rounded up.
		pack.define(locked) {
			raw("data remove storage $STORE in")
			raw("scoreboard players operation #kp.lock $T -= #kp.now $T")
			raw("scoreboard players add #kp.lock $T 19")
			raw("scoreboard players set #kp.20 $T 20")
			raw("scoreboard players operation #kp.lock $T /= #kp.20 $T")
			raw("""tellraw $NEAR [$RA_CHAT_PREFIX,{text:"This Keypad Controller is locked: try again in ",color:"red"},{score:{name:"#kp.lock",objective:"$T"},color:"yellow"},{text:" s.",color:"red"}]""")
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
			raw("execute as @e[type=minecraft:marker,tag=$ENTRY] at @s run function ${entryDue.asId()}")
			raw("execute if entity @e[type=minecraft:marker,tag=$PULSE,limit=1] run return run function ${startFast.asId()}")
			raw("execute if entity @e[type=minecraft:marker,tag=$TIMED,limit=1] run function ${startSlow.asId()}")
		}
		// As a pulsing keypad, at it.
		pack.define(pulseDue) {
			raw("execute store result score #kp.u $T run data get entity @s data.data.until")
			raw("execute if score #kp.u $T > #kp.now $T run return 0")
			raw("function ${pulseOff.asId()}")
			raw("tag @s remove $PULSE")
			raw("data remove entity @s data.data.until")
			raw("execute unless entity @s[tag=$LOCKED] run data modify entity @s data.status.state set value \"ready\"")
			raw("execute unless entity @s[tag=$LOCKED] unless entity @s[tag=$ENTRY] run tag @s remove $TIMED")
		}
		// As a locked keypad.
		pack.define(lockDue) {
			raw("execute store result score #kp.u $T run data get entity @s data.data.locked_until")
			raw("execute if score #kp.u $T > #kp.now $T run return 0")
			raw("tag @s remove $LOCKED")
			raw("data remove entity @s data.data.locked_until")
			raw("execute if entity @s[tag=$PULSE] run data modify entity @s data.status.state set value \"open\"")
			raw("execute unless entity @s[tag=$PULSE] run data modify entity @s data.status.state set value \"ready\"")
			raw("execute unless entity @s[tag=$PULSE] unless entity @s[tag=$ENTRY] run tag @s remove $TIMED")
		}
	}

	/** In the module's load: a schedule that did not survive is started again by [startIfIdle]. */
	fun Function.resetFlag() = raw("scoreboard players set $FLAG $T 0")
}
