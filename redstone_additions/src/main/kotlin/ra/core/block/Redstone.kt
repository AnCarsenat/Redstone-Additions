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
 * `function ra_lib:util/property {name, default, min}`: reads numeric `data.properties.<name>` of
 * `@s` into `#prop ra.temp`, [default] when missing, never below [min].
 */
fun Function.readProperty(name: String, default: Int, min: Int) =
	function(RaLib.Util.property, nbt { this["name"] = name; this["default"] = default; this["min"] = min })
