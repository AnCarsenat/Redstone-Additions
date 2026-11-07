package ra.modules.wireless

import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.raw

/** What the emitter and receiver share: their two defaults and the wrench's enabled toggle. */

/** Channel (default "default") and enabled state (can be toggled with the wrench). As the marker. */
internal fun Function.defaultsChannelEnabled() {
	raw("execute unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value \"default\"")
	raw("execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b")
}

/** Flip `data.properties.enabled` and report it to players within 8 blocks. As the marker. */
internal fun Function.toggleEnabled(label: String, color: String) {
	// Store current state in temp.
	raw("execute store result score @s ${Obj.TEMP} run data get entity @s data.properties.enabled")

	// Toggle: 1 -> 0, 0 -> 1.
	raw("execute if score @s ${Obj.TEMP} matches 1 run data modify entity @s data.properties.enabled set value 0b")
	raw("execute if score @s ${Obj.TEMP} matches 0 run data modify entity @s data.properties.enabled set value 1b")

	// Feedback.
	val head = """{text:"[",color:"dark_gray"},{text:"$label",color:"$color"},{text:"] ",color:"dark_gray"}"""
	raw("""execute if data entity @s data.properties{enabled:1b} run tellraw @a[distance=..8] [$head,{text:"Enabled",color:"green"}]""")
	raw("""execute if data entity @s data.properties{enabled:0b} run tellraw @a[distance=..8] [$head,{text:"Disabled",color:"red"}]""")
	raw("playsound minecraft:block.lever.click block @a[distance=..8,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
}
