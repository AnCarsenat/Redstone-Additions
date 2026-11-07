package ra.modules.sensors

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import ra.api.RaLib
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.markers

/** The Tag Adder / Tag Remover rising-edge trigger: read power, act on the edge, remember the state. */

/** Input latch: the block was powered last tick. */
const val WAS_POWERED = "ra.was_powered"

/** Redstone detection on every marker of [block] (sets `ra.powered`). */
fun Function.detectAll(block: CustomBlock) = execute {
	asTarget(markers(block))
	at(self())
	run { function(RaLib.Redstone.detectSwitch) }
}

/** Markers of [block] powered this tick and not last tick, as selector text. */
fun risingEdge(block: CustomBlock) = block.sel("tag=${Tag.POWERED}", "tag=!$WAS_POWERED")

/** Track power state for edge detection. */
fun Function.trackPower(block: CustomBlock) {
	tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = Tag.POWERED }) { add(WAS_POWERED) }
	tag(allEntities { type = EntityTypes.MARKER; tag = block.tag; tag = "!${Tag.POWERED}" }) { remove(WAS_POWERED) }
}
