package ra.tools.clipboard

import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.tool

/**
 * The Clipboard's check of a vec3 property (ra.core.Vec3Props) before it is pasted: `{name}`,
 * with storage ra:temp clip.props the copied settings, storage ra:dh schema the target's schema
 * and `#clip.adm` 1 for an admin holder (Clipboard.strip).
 *
 * Kept only when it has all three parts, each inside the holder's range (the admin range for an
 * admin) and on the grid; then rewritten as three integers. A world position (frame 2) is not
 * pasted: its range is measured from the block, and the target block is somewhere else.
 */
internal object ClipboardVec3 {
	val strip = tool("clipboard/strip_vec3")

	fun Ra.registerClipboardVec3() = pack.define(strip) {
		val t = Obj.TEMP
		val v = "storage ra:temp clip.props.\$(name)"
		raw("data remove storage ra:temp clip_spec")
		raw("\$data modify storage ra:temp clip_spec set from storage ra:dh schema.\$(name).v3")
		raw("\$execute if data storage ra:temp clip_spec{frame:2} run return run data remove $v")
		raw("\$execute unless data $v run return 0")
		raw("\$execute store success score #clip.ok $t if data $v.x if data $v.y if data $v.z")
		raw("\$execute if score #clip.ok $t matches 0 run return run data remove $v")
		raw("scoreboard players set #clip.g $t 1")
		raw("execute store result score #clip.g $t run data get storage ra:temp clip_spec.grid")
		raw("execute unless score #clip.g $t matches 1.. run scoreboard players set #clip.g $t 1")
		for ((i, a) in listOf("x", "y", "z").withIndex()) {
			for (s in listOf("v", "min", "max")) raw("scoreboard players set #clip.$s $t 0")
			raw("\$execute store result score #clip.v $t run data get $v.$a")
			raw("execute store result score #clip.min $t run data get storage ra:temp clip_spec.min[$i]")
			raw("execute store result score #clip.max $t run data get storage ra:temp clip_spec.max[$i]")
			raw("execute if score #clip.adm $t matches 1 if data storage ra:temp clip_spec.amin store result score #clip.min $t run data get storage ra:temp clip_spec.amin[$i]")
			raw("execute if score #clip.adm $t matches 1 if data storage ra:temp clip_spec.amax store result score #clip.max $t run data get storage ra:temp clip_spec.amax[$i]")
			raw("\$execute unless score #clip.v $t >= #clip.min $t run return run data remove $v")
			raw("\$execute unless score #clip.v $t <= #clip.max $t run return run data remove $v")
			raw("scoreboard players operation #clip.r $t = #clip.v $t")
			raw("scoreboard players operation #clip.r $t %= #clip.g $t")
			raw("\$execute unless score #clip.r $t matches 0 run return run data remove $v")
			raw("scoreboard players operation #clip.$a $t = #clip.v $t")
		}
		raw("\$data modify $v set value {x:0,y:0,z:0}")
		for (a in listOf("x", "y", "z")) raw("\$execute store result $v.$a int 1 run scoreboard players get #clip.$a $t")
		raw("data remove storage ra:temp clip_spec")
	}
}
