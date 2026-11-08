package ra.modules.wireless

import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.raw
import ra.core.block.soundListenersSel

/**
 * What the emitter and receiver share: the channel default. The emitter alone also has an
 * `enabled` property, its default and the wrench's toggle for it. The receiver had one too until
 * v6.0.1; its channel is its only control now (see Receiver).
 */

/** Channel (default "default"). As the marker. */
internal fun Function.defaultChannel() {
	raw("execute unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value \"default\"")
}

/** Channel and the emitter's enabled state (toggled with the wrench). As the emitter's marker. */
internal fun Function.defaultsChannelEnabled() {
	defaultChannel()
	raw("execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b")
}

/** Flip `data.properties.enabled` and report it to players within 8 blocks. As the emitter's marker. */
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
	raw("playsound minecraft:block.lever.click block ${soundListenersSel(8)} ~ ~ ~ 0.5 1.2")
}
