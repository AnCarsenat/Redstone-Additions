package ra.tools.clipboard

import ra.core.DIRECTIONS
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.safe.SafeClean.isNumberAt
import ra.tools.tool

/**
 * The Clipboard's checks of a direction, facing or colour property (ra.core.DirectionProps,
 * ColorProps) before it is pasted, as [ClipboardVec3] does for a vec3: `{name}`, with storage
 * ra:temp clip.props the copied settings and storage ra:dh schema the target's schema. A value
 * the Data Handler would not have taken is dropped, and the target keeps its own:
 *
 *   direction  one of the six faces
 *   facing     both parts, in range and on the grid; rewritten as two integers
 *   color      a number; an RGB one 0..0xFFFFFF or its none; rewritten as an int
 */
internal object ClipboardTypes {
	val stripDirection = tool("clipboard/strip_direction")
	val stripFacing = tool("clipboard/strip_facing")
	val stripColor = tool("clipboard/strip_color")

	fun Ra.registerClipboardTypes() {
		val t = Obj.TEMP
		val v = "storage ra:temp clip.props.\$(name)"

		pack.define(stripDirection) {
			raw("\$execute unless data $v run return 0")
			raw("scoreboard players set #clip.ok $t 0")
			for (f in DIRECTIONS) raw("\$execute if data storage ra:temp clip.props{\$(name):\"$f\"} run scoreboard players set #clip.ok $t 1")
			raw("\$execute if score #clip.ok $t matches 0 run data remove $v")
		}

		pack.define(stripFacing) {
			raw("\$execute unless data $v run return 0")
			raw("\$execute store success score #clip.ok $t if data $v.yaw if data $v.pitch")
			raw("\$execute if score #clip.ok $t matches 0 run return run data remove $v")
			raw("data remove storage ra:temp clip_spec")
			raw("\$data modify storage ra:temp clip_spec set from storage ra:dh schema.\$(name).fc")
			raw("execute store result score #clip.g $t run data get storage ra:temp clip_spec.grid")
			raw("execute unless score #clip.g $t matches 1.. run scoreboard players set #clip.g $t 1")
			for ((i, part) in listOf("yaw", "pitch").withIndex()) {
				raw("\$execute store result score #clip.$part $t run data get $v.$part")
				raw("execute store result score #clip.min $t run data get storage ra:temp clip_spec.min[$i]")
				raw("execute store result score #clip.max $t run data get storage ra:temp clip_spec.max[$i]")
				raw("\$execute unless score #clip.$part $t >= #clip.min $t run return run data remove $v")
				raw("\$execute unless score #clip.$part $t <= #clip.max $t run return run data remove $v")
				raw("scoreboard players operation #clip.r $t = #clip.$part $t")
				raw("scoreboard players operation #clip.r $t %= #clip.g $t")
				raw("\$execute unless score #clip.r $t matches 0 run return run data remove $v")
			}
			raw("\$data modify $v set value {yaw:0,pitch:0}")
			for (part in listOf("yaw", "pitch")) raw("\$execute store result $v.$part int 1 run scoreboard players get #clip.$part $t")
			raw("data remove storage ra:temp clip_spec")
		}

		pack.define(stripColor) {
			raw("\$execute unless data $v run return 0")
			raw("data remove storage ra:temp clip_col")
			raw("\$data modify storage ra:temp clip_col set from $v")
			isNumberAt("storage ra:temp clip_col", "#clip.ok")
			raw("data remove storage ra:temp clip_col")
			raw("\$execute if score #clip.ok $t matches 0 run return run data remove $v")
			raw("\$execute store result score #clip.v $t run data get $v")
			raw("\$execute store result score #clip.a $t run data get storage ra:dh schema.\$(name).col.alpha")
			raw("\$execute store result score #clip.nv $t run data get storage ra:dh schema.\$(name).col.nv")
			raw("\$execute store result score #clip.n $t run data get storage ra:dh schema.\$(name).col.none")
			raw("scoreboard players set #clip.ok $t 1")
			raw("execute if score #clip.a $t matches 0 unless score #clip.v $t matches 0..${0xFFFFFF} run scoreboard players set #clip.ok $t 0")
			raw("execute if score #clip.a $t matches 0 if score #clip.n $t matches 1 if score #clip.v $t = #clip.nv $t run scoreboard players set #clip.ok $t 1")
			raw("\$execute if score #clip.ok $t matches 0 run return run data remove $v")
			raw("\$execute store result $v int 1 run scoreboard players get #clip.v $t")
		}
	}
}
