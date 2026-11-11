package ra.modules.railworks.carts

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import ra.core.Admin
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.lib.BlockTags

/**
 * The minecart a player is looking at: walk the look from the eyes in 0.1-block steps, and at each
 * point take a cart whose box comes within half a block of it. A block the crosshair would stop on
 * ends the walk, as for the tools' block ray (`ra.tools.raycast`). Players reach 4.5 blocks,
 * server admins 8.
 */
internal object CartRay {
	private const val T = Obj.TEMP
	private const val STEPS = 45
	private const val ADMIN_STEPS = 80

	/**
	 * `ra_railworks:carts/ray/<name>`: as the player, run [fn] as the first cart on the look and
	 * return what it returns; 0 when there is none.
	 */
	fun Ra.cartRay(name: String, fn: FunctionArgument): FunctionArgument {
		val start = ref(Ns.RAILWORKS, "carts/ray/$name")
		val step = ref(Ns.RAILWORKS, "carts/ray/${name}_step")
		pack.define(step) {
			raw("execute positioned ~-0.5 ~-0.5 ~-0.5 if entity @e[type=${CartTracker.CARTS},dx=0,dy=0,dz=0] positioned ~0.5 ~0.5 ~0.5 as @n[type=${CartTracker.CARTS},distance=..2] run return run function ${fn.asId()}")
			raw("execute unless block ~ ~ ~ ${BlockTags.rayPassable.asId()} align xyz unless entity @s[dx=0,dy=0,dz=0] run return 0")
			raw("scoreboard players remove #rw.ray $T 1")
			raw("execute if score #rw.ray $T matches 1.. positioned ^ ^ ^0.1 run return run function ${step.asId()}")
			raw("return 0")
		}
		pack.define(start) {
			raw("scoreboard players set #rw.ray $T $STEPS")
			raw("execute if entity @s[tag=${Admin.TAG}] run scoreboard players set #rw.ray $T $ADMIN_STEPS")
			raw("return run execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${step.asId()}")
		}
		return start
	}
}
