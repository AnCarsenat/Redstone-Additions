package ra.lib.redstone

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define

/**
 * `ra_lib:redstone/local/<side>`: power arriving on one of this block's sides, 0-16.
 * Context: as the block's marker, at the block position. Returns the level, and leaves it in
 * #rs ra.temp.
 *
 * Named the way the player sees the block rather than the way the compass does: a machine that
 * cares about "the side it faces" should not have to know it ended up pointing west. The world
 * direction is resolved from the marker's own ra.facing, so a block rotated by the wrench keeps
 * meaning the same thing.
 *
 * A block facing straight up or down has no yaw stored, so left, right, up and down on those two
 * facings are a convention rather than a fact -- they keep the mapping the old look-space scores
 * used, so nothing that relied on it changes. Only front and back are unambiguous for a
 * vertically placed block.
 */
internal object Local {
	private val refs = mapOf(
		LocalSide.FRONT to RaLib.Redstone.Local.front,
		LocalSide.BACK to RaLib.Redstone.Local.back,
		LocalSide.LEFT to RaLib.Redstone.Local.left,
		LocalSide.RIGHT to RaLib.Redstone.Local.right,
		LocalSide.UP to RaLib.Redstone.Local.up,
		LocalSide.DOWN to RaLib.Redstone.Local.down,
	)

	fun Ra.registerLocal() {
		for ((local, ref) in refs) pack.define(ref) {
			// ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
			local.byFacing.forEachIndexed { facing, side ->
				execute {
					ifCondition { score(self(), Obj.FACING) equalTo facing }
					run { returnRun { function(RaLib.Redstone.side, side.args()) } }
				}
			}

			// No facing score at all -- treat the block as if it faced south, which is the default
			// ra_lib:placement/place seeds when there was no placer to read.
			execute {
				unlessCondition { score(self(), Obj.FACING) matches 0..5 }
				run { returnRun { function(RaLib.Redstone.side, local.fallback.args()) } }
			}
			returnValue(0)
		}
	}
}
