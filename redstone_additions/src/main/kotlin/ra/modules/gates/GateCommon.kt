package ra.modules.gates

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropBlockItem
import ra.core.block.forEachMarker
import ra.core.block.gogglesHeader
import ra.core.block.outputShell
import ra.core.block.propLine
import ra.core.define

/**
 * Pieces every `ra_gates` block shares: the redstone read that ignores the output shell
 * (`ra.core.block.outputShell`), the goggles readout and the break handling of the timing blocks.
 */

/** Set while reading redstone, so the gate's own redstone_block shell does not read back as input. */
internal const val IGNORE_BLOCKS = "ra.redstone.ignore_blocks"

/** Input latch of the edge-triggered blocks: @s was powered last tick. */
internal const val WAS_POWERED = "ra.was_powered"

/** `ra_lib:redstone/detect_switch` with the shell ignored. Sets `ra.powered` on @s. */
internal fun Function.detectSwitchIgnoringShell() {
	tag(self()) { add(IGNORE_BLOCKS) }
	function(RaLib.Redstone.detectSwitch)
	tag(self()) { remove(IGNORE_BLOCKS) }
}

/**
 * The standard goggles readout: the name, then one property line.
 *
 * The name is published to storage ra:temp block_name before anything is drawn, and the
 * function stops there when the caller only wants the name. That makes it the single source of
 * the name: ra:tools/block_name reuses the same dispatch to answer the Data Handlers, which used
 * to carry two separate hand-written name tables that had already drifted apart.
 */
internal fun DataPack.gateGoggles(block: CustomBlock, nameY: Double, prop: String, label: String, suffix: String, y: Double) =
	define(block.goggles) {
		gogglesHeader(block, nameY)
		propLine(prop, label, "aqua", y, suffix)
	}

/**
 * `blocks/<id>/on_break` of the timing blocks (Delayer, Extender, Shortener, Randomizer): swap the
 * vanilla drop for the custom item, put the output out, break effects, remove the marker.
 *
 * The custom item is summoned WITHOUT the `ra.drop_temp` tag, exactly as the beet version did.
 * Keep it that way: some interactive blocks overwrite the first `ra.drop_temp` item they find
 * with their container contents, so a tagged drop lying nearby could be replaced.
 */
internal fun DataPack.timingOnBreak(block: CustomBlock, breakSound: String): FunctionArgument = blockOnBreak(
	block,
	drop = { dropBlockItem(block, tagged = false) },
	sound = breakSound,
	cloud = true,
	afterVanillaKill = { outputShell(on = false) },
)

/**
 * `blocks/<id>/tick` of the timing blocks: a marker whose block is no longer [physical] breaks,
 * then every marker runs `process`.
 *
 * The test is "not the block any more" rather than "air", as in the beet version: a timing block
 * replaced by something else (a piston pushing, a player swapping it) breaks too.
 */
internal fun DataPack.timingTick(block: CustomBlock, physical: BlockArgument, onBreak: FunctionArgument, process: FunctionArgument) =
	blockTick(block, onBreak, BreakTest.NotBlock(physical)) { forEachMarker(block, process) }
