package ra.tools.datahandler.props

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import ra.lib.BlockTags
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw
import ra.tools.datahandler.DataHandlerSelection
import ra.tools.tool

/**
 * WHAT THE VEC3 EDITOR DOES TO A VALUE: its range for this player, the checks, the write, the
 * marker showing where it points, and [Pick]. Every value is three scores, `#v3.x/y/z ra.temp`;
 * nothing here is ever text. See ra.core.Vec3Props and [DataHandlerVec3].
 *
 * After [bounds] (storage ra:dh spec is the property's declaration, from probe):
 *   #v3.lo0..2 / #v3.hi0..2  the range per axis, in stored values: the players', or the admin
 *                            range when `#v3.adm` is 1 and one is declared; for a world position
 *                            (frame 2) moved by the block's own position
 *   #v3.p0..2  the block's position;  #v3.b0..2  what a stored value is measured from (0, or the
 *              block's position in the unit for frame 2);  #v3.f frame;  #v3.g grid;  #v3.u unit
 */
internal object Vec3Ops {
	val bounds = tool("data_handler/props/vec3/bounds")
	val load = tool("data_handler/props/vec3/load")
	val check = tool("data_handler/props/vec3/check")
	val write = tool("data_handler/props/vec3/write")
	val show = tool("data_handler/props/vec3/show")
	val hide = tool("data_handler/props/vec3/hide")
	val expire = tool("data_handler/props/vec3/expire")
	val pick = tool("data_handler/props/vec3/pick")
	private val pickStep = tool("data_handler/props/vec3/pick_step")
	private val pickHit = tool("data_handler/props/vec3/pick_hit")

	/** The editor's point marker: a glowing block display on the block the value points at. */
	const val POINT_TAG = "ra.dh.v3pt"
	private const val NEW_TAG = "ra.dh.v3new"
	private const val TMP_TAG = "ra.dh.v3tmp"
	/** Seconds the point stays after the last change. */
	private const val POINT_SECONDS = 10
	/** How far [Pick] reaches, in blocks. */
	const val PICK_REACH = 24

	private const val T = Obj.TEMP
	private const val PREFIX = DataHandlerProps.PREFIX
	private const val TARGET = DataHandlerProps.TARGET
	private val AXES = listOf("x", "y", "z")

	fun Ra.registerVec3Ops() {
		pack.define(bounds) {
			for (i in 0..2) {
				raw("execute store result score #v3.lo$i $T run data get storage ra:dh spec.v3.min[$i]")
				raw("execute store result score #v3.hi$i $T run data get storage ra:dh spec.v3.max[$i]")
				raw("execute if score #v3.adm $T matches 1 if data storage ra:dh spec.v3.amin store result score #v3.lo$i $T run data get storage ra:dh spec.v3.amin[$i]")
				raw("execute if score #v3.adm $T matches 1 if data storage ra:dh spec.v3.amax store result score #v3.hi$i $T run data get storage ra:dh spec.v3.amax[$i]")
			}
			raw("execute store result score #v3.f $T run data get storage ra:dh spec.v3.frame")
			raw("execute store result score #v3.g $T run data get storage ra:dh spec.v3.grid")
			raw("execute unless score #v3.g $T matches 1.. run scoreboard players set #v3.g $T 1")
			raw("execute store result score #v3.u $T run data get storage ra:dh spec.v3.unit")
			raw("execute unless score #v3.u $T matches 1.. run scoreboard players set #v3.u $T 1")
			// The block's own position: a marker summoned on its corner says it in whole numbers.
			raw("execute at $TARGET align xyz run summon minecraft:marker ~ ~ ~ {Tags:[\"$TMP_TAG\"]}")
			for (i in 0..2) raw("execute store result score #v3.p$i $T run data get entity @e[type=minecraft:marker,tag=$TMP_TAG,limit=1] Pos[$i]")
			raw("kill @e[type=minecraft:marker,tag=$TMP_TAG]")
			for (i in 0..2) {
				raw("scoreboard players set #v3.b$i $T 0")
				raw("execute if score #v3.f $T matches 2 run scoreboard players operation #v3.b$i $T = #v3.p$i $T")
				raw("execute if score #v3.f $T matches 2 run scoreboard players operation #v3.b$i $T *= #v3.u $T")
				raw("scoreboard players operation #v3.lo$i $T += #v3.b$i $T")
				raw("scoreboard players operation #v3.hi$i $T += #v3.b$i $T")
			}
		}

		// {name}: the value shown (the block's, or the schema default) into #v3.x/y/z.
		pack.define(load) {
			for (a in AXES) {
				raw("scoreboard players set #v3.$a $T 0")
				raw("\$execute store result score #v3.$a $T run data get storage ra:dh properties.\$(name).$a")
			}
		}

		// #v3.x/y/z against the range and the grid, after bounds. 1 when fine; 0 with the reason.
		pack.define(check) {
			for ((i, a) in AXES.withIndex()) {
				val why = """[$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" ${a.uppercase()} must be from ",color:"red"},{score:{name:"#v3.lo$i",objective:"$T"},color:"yellow"},{text:" to ",color:"red"},{score:{name:"#v3.hi$i",objective:"$T"},color:"yellow"},{text:". Nothing was changed.",color:"red"}]"""
				for (cond in listOf("unless score #v3.$a $T >= #v3.lo$i $T", "unless score #v3.$a $T <= #v3.hi$i $T")) {
					raw("execute $cond run tellraw @s $why")
					raw("execute $cond run return run function ${refused.asId()}")
				}
				raw("scoreboard players operation #v3.r $T = #v3.$a $T")
				raw("scoreboard players operation #v3.r $T %= #v3.g $T")
				raw("""execute unless score #v3.r $T matches 0 run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" takes multiples of ",color:"red"},{score:{name:"#v3.g",objective:"$T"},color:"yellow"},{text:" only. Nothing was changed.",color:"red"}]""")
				raw("execute unless score #v3.r $T matches 0 run return run function ${refused.asId()}")
			}
			raw("return 1")
		}
		pack.define(refused) {
			raw("playsound minecraft:block.note_block.bass block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.7")
			raw("return 0")
		}

		// {name}: #v3.x/y/z into the selected block, as three integers.
		pack.define(write) {
			raw("\$data modify entity $TARGET data.properties.\$(name) set value {x:0,y:0,z:0}")
			for (a in AXES) raw("\$execute store result entity $TARGET data.properties.\$(name).$a int 1 run scoreboard players get #v3.$a $T")
			raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.4")
		}

		// The block #v3.x/y/z points at, outlined for a few seconds; after bounds. Not for a value on
		// the block's face (frame 0): the display itself moves there. As the player.
		pack.define(show) {
			function(hide)
			raw("execute if score #v3.f $T matches 0 run return 0")
			raw("execute at $TARGET align xyz run summon minecraft:block_display ~ ~ ~ {Tags:[\"ra\",\"$POINT_TAG\",\"$NEW_TAG\"],block_state:{id:\"minecraft:lime_stained_glass\"},Glowing:1b,glow_color_override:5635925,brightness:{sky:15,block:15},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1.01f,1.01f,1.01f]}}")
			// Translation, in 1e-4 block: (value - base) * 10000 / unit, less half the outline's growth.
			raw("scoreboard players set #v3.k $T 10000")
			raw("scoreboard players operation #v3.k $T /= #v3.u $T")
			for ((i, a) in AXES.withIndex()) {
				raw("scoreboard players operation #v3.d $T = #v3.$a $T")
				raw("scoreboard players operation #v3.d $T -= #v3.b$i $T")
				raw("scoreboard players operation #v3.d $T *= #v3.k $T")
				raw("scoreboard players remove #v3.d $T 50")
				raw("execute store result entity @e[type=minecraft:block_display,tag=$NEW_TAG,limit=1] transformation.translation[$i] float 0.0001 run scoreboard players get #v3.d $T")
			}
			raw("scoreboard players operation @e[type=minecraft:block_display,tag=$NEW_TAG] ${DataHandlerSelection.MID} = @s ${DataHandlerSelection.SEL}")
			raw("scoreboard players set @e[type=minecraft:block_display,tag=$NEW_TAG] ${Obj.COOLDOWN} $POINT_SECONDS")
			raw("tag @e[type=minecraft:block_display,tag=$NEW_TAG] remove $NEW_TAG")
			schedule(expire).replace(20.ticks)
		}
		// This player's block's point, if one is shown. As the player.
		pack.define(hide) {
			raw("scoreboard players operation #v3.sel $T = @s ${DataHandlerSelection.SEL}")
			raw("execute as @e[type=minecraft:block_display,tag=$POINT_TAG] if score @s ${DataHandlerSelection.MID} = #v3.sel $T run kill @s")
		}
		// Every second while a point is shown; also from load, which clears the ones left over.
		pack.define(expire) {
			raw("scoreboard players remove @e[type=minecraft:block_display,tag=$POINT_TAG] ${Obj.COOLDOWN} 1")
			raw("kill @e[type=minecraft:block_display,tag=$POINT_TAG,scores={${Obj.COOLDOWN}=..0}]")
			raw("execute if entity @e[type=minecraft:block_display,tag=$POINT_TAG,limit=1] run schedule function ${expire.asId()} 20t replace")
		}

		// [Pick]: #v3.x/y/z from the block the player looks at, after bounds. Returns 1 when one
		// was found. Frame 0 has nothing to pick. As the player.
		pack.define(pick) {
			raw("""execute if score #v3.f $T matches 0 run return run tellraw @s [$PREFIX,{nbt:"q.name",storage:"ra:dh",color:"yellow"},{text:" is on the block's face: nudge it or type it instead.",color:"red"}]""")
			raw("scoreboard players set #v3.hit $T 0")
			raw("scoreboard players set #v3.ray $T ${PICK_REACH * 10}")
			raw("execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${pickStep.asId()}")
			raw("""execute if score #v3.hit $T matches 0 run tellraw @s [$PREFIX,{text:"Look at a block within $PICK_REACH blocks, then press [Pick].",color:"red"}]""")
			raw("execute if score #v3.hit $T matches 0 run return 0")
			for ((i, a) in AXES.withIndex()) {
				raw("scoreboard players operation #v3.$a $T = #v3.h$i $T")
				raw("execute if score #v3.f $T matches 1 run scoreboard players operation #v3.$a $T -= #v3.p$i $T")
				raw("scoreboard players operation #v3.$a $T *= #v3.u $T")
			}
			raw("return 1")
		}
		// The player's look in 0.1-block steps: the first block the crosshair stops on (not
		// #ra_lib:ray_passable, and not one the player stands in), as in ra.tools.raycast.
		pack.define(pickStep) {
			raw("execute unless block ~ ~ ~ ${BlockTags.rayPassable.asId()} align xyz unless entity @s[dx=0,dy=0,dz=0] run return run function ${pickHit.asId()}")
			raw("scoreboard players remove #v3.ray $T 1")
			raw("execute if score #v3.ray $T matches 1.. positioned ^ ^ ^0.1 run function ${pickStep.asId()}")
		}
		pack.define(pickHit) {
			raw("summon minecraft:marker ~ ~ ~ {Tags:[\"$TMP_TAG\"]}")
			for (i in 0..2) raw("execute store result score #v3.h$i $T run data get entity @e[type=minecraft:marker,tag=$TMP_TAG,limit=1] Pos[$i]")
			raw("kill @e[type=minecraft:marker,tag=$TMP_TAG]")
			raw("scoreboard players set #v3.hit $T 1")
		}
	}

	/** The refusal sound; returns 0. */
	private val refused = tool("data_handler/props/vec3/refused")

	/** As the player: q {name} -> check, write, point; returns 1 when written. */
	fun io.github.ayfri.kore.functions.Function.commit() {
		raw("execute store result score #v3.ok $T run function ${check.asId()}")
		raw("execute if score #v3.ok $T matches 0 run return 0")
		callWith(write, "storage ra:dh q")
		function(show)
	}
}
