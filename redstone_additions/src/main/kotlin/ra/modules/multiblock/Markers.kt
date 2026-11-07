package ra.modules.multiblock

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes

/**
 * `execute as @e[type=marker,tag=<tag>] at @s run function <fn>`: [fn] on every marker carrying
 * [tag], at it. Core `forEachMarker` takes a `CustomBlock`; multiblock markers and bases are
 * found by a type tag instead (`ra.multiblock.<id>`, `ra.multiblock_base`), so this is the same
 * call keyed by tag.
 */
fun Function.forEachMarker(tag: String, fn: FunctionArgument) = execute {
	asTarget(allEntities {
		type = EntityTypes.MARKER
		this.tag = tag
	})
	at(self())
	run { function(fn) }
}
