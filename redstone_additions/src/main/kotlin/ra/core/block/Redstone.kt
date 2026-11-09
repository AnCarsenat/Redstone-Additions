package ra.core.block

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.commands.fill
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib

/** Block outputs and property reads shared by the redstone-driven modules (gates, wireless...). */

/**
 * Switches the 3x3x3 output shell: iron blocks in the cube around the block become redstone
 * blocks ([on]) or back to iron. Callers run it `at` the block.
 */
fun Function.outputShell(on: Boolean) = if (on)
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.REDSTONE_BLOCK, Blocks.IRON_BLOCK)
else
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.IRON_BLOCK, Blocks.REDSTONE_BLOCK)

/**
 * The analog output: every target block within [radius] of the position gets `power=<level>`.
 * A target's `power` state (0-15) is the weak power it gives every neighbour, so dust beside it
 * carries the level and a comparator behind it reads it. A target placed by a command resets to
 * 0 only when it replaces a different block (TargetBlock#onPlace); target over target keeps the
 * state, and a fill to the state already there changes and updates nothing. An arrow pulses it
 * as in vanilla until the owner writes the level again.
 *
 * [level] is the text of the level: `$(level)` in a macro function (the line then needs its
 * leading `$`), or a literal number. Radius 0 is the block itself (the Light Sensor IS a target);
 * radius 1 is the 3x3x3 cube of the analog gates, the analog twin of [outputShell].
 */
fun targetPowerFill(radius: Int, level: String): String {
	val box = if (radius == 0) "~ ~ ~ ~ ~ ~" else "~-$radius ~-$radius ~-$radius ~$radius ~$radius ~$radius"
	return "fill $box minecraft:target[power=$level] replace minecraft:target"
}

/**
 * `function ra_lib:util/property {name, default, min}`: reads numeric `data.properties.<name>` of
 * `@s` into `#prop ra.temp`, [default] when missing, never below [min].
 */
fun Function.readProperty(name: String, default: Int, min: Int) =
	function(RaLib.Util.property, nbt { this["name"] = name; this["default"] = default; this["min"] = min })
