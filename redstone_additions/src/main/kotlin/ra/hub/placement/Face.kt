package ra.hub.placement

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

/**
 * THE BAT LANDED INSIDE A SOFT BLOCK.
 *
 * A spawn egg puts its mob INSIDE the clicked block when that block has no collision (a flower, a
 * sapling, a rail, a pressure plate, a button, a lever, a torch, redstone dust), and beside it
 * otherwise. None of those are `#minecraft:replaceable`, so the placement was refused. A player
 * clicking a flower expects what vanilla does with a stone: the block goes against the face they
 * clicked. [faceRetry] finds that face by walking the placer's line of sight until it enters the bat's
 * block, and moves the bat back one step, into the block the ray came through. Then the checks
 * run again, once.
 */
internal val faceRetry = ref(Ns.RA, "placement/face/retry")
private val step = ref(Ns.RA, "placement/face/step")

private const val TARGET = "ra.face_target"
private const val BAT = "ra.face_bat"
internal const val TRIED = "ra.face_tried"

/** Ray steps of 0.1 block: 6 blocks, past the farthest a survival player can click. */
private const val MAX_STEPS = 60

internal fun Ra.registerFace(tryPlace: FunctionArgument) {
	pack.define(faceRetry) {
		raw("tag @s add $TRIED")
		raw("tag @s add $BAT")
		// The target is the bat's block, marked at its centre so the ray can ask "am I there".
		raw("execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon minecraft:marker ~ ~ ~ {Tags:[\"${Tag.RA}\",\"$TARGET\"]}")
		raw("scoreboard players set #face ${Obj.TEMP} $MAX_STEPS")
		executeRun("as @a[tag=${Tag.PLACER},limit=1] at @s anchored eyes positioned ^ ^ ^", step)
		raw("kill @e[type=minecraft:marker,tag=$TARGET]")
		raw("tag @s remove $BAT")
		// `at @s`: the bat may have moved, and this function's own position has not.
		executeReturnRun("at @s", tryPlace)
	}

	// As the placer, at a point on its line of sight. Asks whether the NEXT point is in the bat's
	// block; if so this point's block is the one in front of the clicked face.
	pack.define(step) { faceStep() }
}

private fun Function.faceStep() {
	raw("scoreboard players remove #face ${Obj.TEMP} 1")
	raw("execute if score #face ${Obj.TEMP} matches ..0 run return 0")
	raw("execute store success score #face_hit ${Obj.TEMP} positioned ^ ^ ^0.1 align xyz positioned ~0.5 ~0.5 ~0.5 if entity @e[type=minecraft:marker,tag=$TARGET,distance=..0.01]")
	raw("execute if score #face_hit ${Obj.TEMP} matches 1 align xyz positioned ~0.5 ~0.5 ~0.5 run return run teleport @e[type=minecraft:bat,tag=$BAT,limit=1] ~ ~ ~")
	executeRun("positioned ^ ^ ^0.1", step)
}
