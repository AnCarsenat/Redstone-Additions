package ra.lib.placement

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import net.benwoodworth.knbt.NbtString
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.lib.dummyObjectives
import ra.lib.fake
import ra.lib.lib
import ra.lib.macroCall
import ra.lib.orientation.Orientation

/** `ra_lib:placement/...`: put the vanilla block down and summon the custom block's marker. */
object Placement {
	val init = lib("placement/init")
	val setBlock = lib("placement/set_block")
	val setBlockFacing = lib("placement/set_block_facing")
	val setBlockSimple = lib("placement/set_block_simple")

	/** Facing score -> the `facing` state name, in ra.facing order (0 down .. 5 east). */
	private val facingNames = listOf("down", "up", "north", "south", "west", "east")

	fun Ra.registerPlacement() {
		// Setup placement scoreboards.
		pack.define(init) { dummyObjectives(Obj.TEMP, Obj.COOLDOWN) }

		pack.define(RaLib.Placement.place) { place() }

		// Place block with facing from storage. At position. Uses storage ra:temp {block_id, facing_name, has_facing}.
		pack.define(setBlock) {
			execute {
				ifCondition { data(Storage.TEMP, "{has_facing:1b}") }
				run { function(setBlockFacing, Storage.TEMP) }
			}
			execute {
				unlessCondition { data(Storage.TEMP, "{has_facing:1b}") }
				run { function(setBlockSimple, Storage.TEMP) }
			}
		}

		// Place block WITH facing property. Uses storage ra:temp {block_id, facing_name}.
		pack.define(setBlockFacing) { raw("\$setblock ~ ~ ~ \$(block_id)[facing=\$(facing_name)] replace") }

		// Place block WITHOUT facing property. Uses storage ra:temp {block_id}.
		pack.define(setBlockSimple) { raw("\$setblock ~ ~ ~ \$(block_id) replace") }
	}

	/**
	 * `place {block_id, block_tag, dir_type}`: place a custom block with its marker. At the
	 * position, player tagged ra.placer nearby. Output: block placed, marker with ra.custom_block +
	 * ra.custom_block.{tag} + ra.new.
	 *
	 * IMPORTANT: the caller must remove ra.new after setting properties!
	 *
	 * dir_type: 0 = no facing, 1 = horizontal only, 2 = full 6-directional, 3 = full 6-directional
	 * on the MARKER only, block placed plain. Use 3 for a block with no `facing` state of its own.
	 */
	private fun Function.place() {
		val facing = fake("#facing")
		val dirType = fake("#dir_type")

		// Reset the shared orientation scratch to the default (south) BEFORE reading the placer.
		// #facing / ra:temp Rotation persist between calls, so a placement with no nearby ra.placer
		// would otherwise silently inherit the previous block's facing.
		scoreboard.players.set(facing, Obj.TEMP, 3)
		raw("data modify storage ra:temp Rotation set value [0f, 0f]")
		data(Storage.TEMP) { modify("facing") { set(3) } }

		raw("\$execute as @p[tag=ra.placer] run ${macroCall(Orientation.getFacing, "{dir_type:\$(dir_type)}")}")

		// Set has_facing from dir_type. Only 1 and 2 write the facing onto the block itself; 3
		// resolves it for the marker and leaves the block plain.
		raw("\$scoreboard players set #dir_type ${Obj.TEMP} \$(dir_type)")
		raw("data modify storage ra:temp has_facing set value 0b")
		execute {
			ifCondition { score(dirType, Obj.TEMP) matches 1..2 }
			run { raw("data modify storage ra:temp has_facing set value 1b") }
		}

		// Facing name from the score.
		facingNames.forEachIndexed { i, name ->
			execute {
				ifCondition { score(facing, Obj.TEMP) equalTo i }
				run { data(Storage.TEMP) { modify("facing_name") { set(NbtString(name)) } } }
			}
		}

		// Place the block, with or without facing.
		raw("\$data modify storage ra:temp block_id set value \"\$(block_id)\"")
		function(setBlock)

		// Summon the marker with an initialized data structure.
		raw("\$summon marker ~ ~ ~ {Tags:[\"ra\",\"ra.custom_block\",\"ra.custom_block.\$(block_tag)\",\"ra.new\"],data:{_init:1b,type:\"\$(block_tag)\",properties:{},data:{}}}")
		raw("data modify entity @e[type=marker,tag=ra.new,distance=..0.1,sort=nearest,limit=1] Rotation set from storage ra:temp Rotation")

		// Seed the marker's facing from the scratch score rather than from the placer, so the
		// marker always ends up with a facing score even when no player was found.
		raw("execute store result score @e[type=marker,tag=ra.new,distance=..0.1,sort=nearest,limit=1] ${Obj.FACING} run scoreboard players get #facing ${Obj.TEMP}")

		raw("playsound minecraft:block.stone.place block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 1")
	}
}
