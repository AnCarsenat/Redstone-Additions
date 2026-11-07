package ra.lib.orientation

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.dummyObjectives
import ra.lib.fake
import ra.lib.lib

/** `ra_lib:orientation/...`: turn a player's look into a block facing. */
object Orientation {
	val init = lib("orientation/init")
	val getFacing = lib("orientation/get_facing")
	val setFacing = lib("orientation/set_facing")

	/** One outcome of [getFacing]: the facing score and the marker Rotation that goes with it. */
	private class Facing(val f: Int, val yaw: Float, val pitch: Float) {
		fun args() = nbt {
			this["f"] = f
			this["yaw"] = yaw
			this["pitch"] = pitch
		}
	}

	fun Ra.registerOrientation() {
		// Setup orientation scoreboards.
		pack.define(init) { dummyObjectives(Obj.FACING, Obj.YAW, Obj.PITCH) }

		pack.define(getFacing) { getFacing() }

		// `set_facing {f, yaw, pitch}`: internal helper. Sets the facing score and the rotation storage.
		pack.define(setFacing) {
			raw("\$scoreboard players set @s ${Obj.FACING} \$(f)")
			raw("\$data modify storage ra:temp Rotation set value [\$(yaw), \$(pitch)]")
			raw("\$execute store result storage ra:temp facing int 1 run scoreboard players set #facing ${Obj.TEMP} \$(f)")
		}
	}

	/**
	 * `get_facing {dir_type}`: facing from the player's rotation. As the player.
	 * Output: @s ra.facing (0-5), storage ra:temp Rotation [yaw,pitch].
	 *
	 * dir_type 3 is 'orient the marker, but place a plain block'. Some blocks have no `facing`
	 * state at all -- waxed_cut_copper has no states whatsoever -- and asking for one produces
	 * `setblock waxed_cut_copper[facing=south]`, which is not a real block state. The command
	 * fails, nothing is placed, and the only symptom is an item that does nothing when you use it.
	 */
	private fun Function.getFacing() {
		val dirType = fake("#dir_type")

		raw("execute store result score @s ${Obj.YAW} run data get entity @s Rotation[0]")
		raw("execute store result score @s ${Obj.PITCH} run data get entity @s Rotation[1]")

		raw("\$scoreboard players set #dir_type ${Obj.TEMP} \$(dir_type)")

		// Default: south (3).
		scoreboard.players.set(self(), Obj.FACING, 3)
		raw("data modify storage ra:temp Rotation set value [0f, 0f]")
		raw("execute store result storage ra:temp facing int 1 run scoreboard players set #facing ${Obj.TEMP} 3")

		execute {
			ifCondition { score(dirType, Obj.TEMP) equalTo 0 }
			run { returnFacing() }
		}

		// Vertical (dir_type 2 and 3).
		for ((pitch, facing) in listOf(rangeOrIntStart(45) to Facing(1, 0f, -90f), rangeOrIntEnd(-45) to Facing(0, 0f, 90f))) {
			execute {
				ifCondition { score(dirType, Obj.TEMP) matches 2..3 }
				ifCondition { score(self(), Obj.PITCH) matches pitch }
				run { function(setFacing, facing.args()) }
			}
			execute {
				ifCondition { score(dirType, Obj.TEMP) matches 2..3 }
				ifCondition { score(self(), Obj.PITCH) matches pitch }
				run { returnFacing() }
			}
		}

		// Horizontal: the block faces the player.
		//   looking south (yaw ~0)    -> block faces north
		//   looking west  (yaw ~90)   -> block faces east
		//   looking east  (yaw ~-90)  -> block faces west
		for ((yaw, facing) in listOf(
			-45..45 to Facing(2, 180f, 0f),
			46..135 to Facing(5, -90f, 0f),
			-135..-46 to Facing(4, 90f, 0f),
		)) {
			execute {
				ifCondition { score(self(), Obj.YAW) matches yaw }
				run { function(setFacing, facing.args()) }
			}
			execute {
				ifCondition { score(self(), Obj.YAW) matches yaw }
				run { returnFacing() }
			}
		}

		// Looking north (yaw ~180/-180) -> block faces south.
		function(setFacing, Facing(3, 0f, 0f).args())
		returnFacing()
	}

	private fun Function.returnFacing() = returnRun { scoreboard.players.get(self(), Obj.FACING) }
}
