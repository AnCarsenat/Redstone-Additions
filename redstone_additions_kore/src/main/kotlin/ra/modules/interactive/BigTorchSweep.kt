package ra.modules.interactive

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard

/**
 * The Big Torch's mob denial: `blocks/big_torch/sweep` and `sweep_at`.
 *
 * HOW "SPAWNED IN" IS TOLD FROM "WALKED IN". A data pack cannot stop a spawn from happening; it
 * can only remove what appeared. Removing every hostile mob inside the radius would be a
 * different block entirely -- it would clear mobs that walked in from outside, which is a mob
 * grinder, not a torch.
 *
 * So every mob in a band REACHING PAST the radius is remembered with `ra.big_torch.seen`, and only
 * unremembered mobs inside the radius are removed. Anything approaching on foot crosses the outer
 * band first and is remembered there, so it survives crossing into the radius. Anything that
 * spawns inside appears with no tag at all and is denied on the next sweep.
 *
 * The order matters: deny first, remember second. Doing it the other way round would tag a mob in
 * the same sweep it spawned in and never deny anything.
 *
 * The band is 16 blocks wide against a 10-tick sweep. The fastest thing in the tag covers about 5
 * blocks in that time, so nothing can cross the band unseen.
 */
object BigTorchSweep {
	private val sweep = BigTorch.block.fn("sweep")
	private val sweepAt = BigTorch.block.fn("sweep_at")

	private const val SEEN = "ra.big_torch.seen"
	private val blocked = "#${Ns.INTERACTIVE.id}:${BigTorch.SPAWN_BLOCKED}"

	/** Defines both functions; returns `sweep`, which the torch's tick calls. */
	fun Ra.registerBigTorchSweep(): FunctionArgument {
		pack.define(sweep) { sweep() }
		pack.define(sweepAt) { sweepAt() }
		return sweep
	}

	/** One torch's sweep. As the torch marker, at the torch position. */
	private fun Function.sweep() {
		function(RaLib.Util.property, nbt { this["name"] = "radius"; this["default"] = 16; this["min"] = 1 })

		// THE 100-BLOCK CEILING IS WRITTEN BACK, NOT ONLY APPLIED. The selector cost is the reason
		// for it: `distance` describes a sphere, so doubling the radius is eight times the volume
		// to search, and this runs per torch every ten ticks.
		//
		// Clamping the working value alone was not enough. `data.properties.radius` kept whatever
		// was typed, and the goggles read that path directly -- so a torch set to 500 advertised
		// `Radius: 500 blocks` while sweeping 100, and the Data Handler offered the same 500 back
		// for editing. The stored value is corrected here so every reader of it agrees with what
		// the block actually does. The write-back goes first: it tests the value as typed, and the
		// clamp below is what would otherwise have already hidden it.
		raw("execute if score #prop ${Obj.TEMP} matches 101.. run data modify entity @s data.properties.radius set value 100")
		raw("execute if score #prop ${Obj.TEMP} matches 101.. run scoreboard players set #prop ${Obj.TEMP} 100")

		raw("execute store result storage ra:temp big_torch.radius int 1 run scoreboard players get #prop ${Obj.TEMP}")
		raw("scoreboard players operation #prop.outer ${Obj.TEMP} = #prop ${Obj.TEMP}")
		scoreboard.players.add(literal("#prop.outer"), Obj.TEMP, 16)
		raw("execute store result storage ra:temp big_torch.outer int 1 run scoreboard players get #prop.outer ${Obj.TEMP}")

		callWith(sweepAt, "storage ra:temp big_torch")
	}

	/**
	 * Macro `{radius, outer}`: the two selectors, once the radius is known. As the torch marker, at
	 * the torch position.
	 *
	 * Split out because a selector's distance cannot be written without a macro, and a macro line
	 * is re-parsed every time the function runs -- so the reading and clamping in `sweep` is kept
	 * out of it.
	 */
	private fun Function.sweepAt() {
		val unseen = "@e[type=$blocked,tag=!$SEEN,distance=..\$(radius)]"

		// Count first, because the drop below is what makes them stop matching.
		raw("\$execute store result score #bt.killed ${Obj.TEMP} run execute if entity $unseen")

		raw("\$execute as $unseen at @s run particle minecraft:smoke ~ ~0.5 ~ 0.2 0.3 0.2 0.01 6 normal @a[distance=..24,scores={${Obj.USER_PARTICLES}=1..}]")

		// DROPPED INTO THE VOID RATHER THAN KILLED. A kill fires the death, which means drops and
		// experience at the torch: a Big Torch in a dark room was a passive mob farm that also lit
		// the room. Dropping them out of the world takes the mob without paying anybody for it.
		//
		// THE Y IS NOT DERIVED FROM THE WORLD BOTTOM. Void damage begins some distance below a
		// dimension's `min_y`, and that depth has moved between versions. Rather than track a
		// number that moves, this goes far enough down that no dimension can reach it. It costs
		// nothing to overshoot: an entity below the world falls and takes void damage wherever the
		// threshold happens to sit, and entity positions are valid to +/-30 million.
		//
		// X and Z are relative, so the destination chunk is the one the mob was already standing
		// in and is loaded.
		raw("\$execute as $unseen at @s run tp @s ~ -1000000 ~")

		// Remember: everything in the wider band, so a mob walking in is not denied.
		raw("\$tag @e[type=$blocked,distance=..\$(outer)] add $SEEN")

		// A running total since the torch was placed, which is what makes the goggles line worth
		// reading -- a per-sweep count is almost always 0.
		raw("execute unless data entity @s data.data.removed run data modify entity @s data.data.removed set value 0")
		raw("execute store result score #bt.total ${Obj.TEMP} run data get entity @s data.data.removed 1")
		raw("scoreboard players operation #bt.total ${Obj.TEMP} += #bt.killed ${Obj.TEMP}")
		raw("execute store result entity @s data.data.removed int 1 run scoreboard players get #bt.total ${Obj.TEMP}")
	}
}
