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
import ra.core.Obj
import ra.core.define
import ra.core.raw

/**
 * Pieces every `ra_gates` block shares: the redstone read that ignores the output shell
 * (`ra.core.block.outputShell`), the goggles readout and the break handling of the timing blocks.
 */

/** Set while reading redstone, so the gate's own redstone_block shell does not read back as input. */
internal const val IGNORE_BLOCKS = "ra.redstone.ignore_blocks"

/** Input latch of the edge-triggered blocks: @s was powered last tick. */
internal const val WAS_POWERED = "ra.was_powered"

/** A gate whose output pulse is lit; its `ra.cooldown` score is the ticks left. See [pulseCountdown]. */
internal const val PULSING = "ra.gate.pulsing"

/** The lit pulse ran out this tick; [pulseFinish] puts it out unless [pulseStart] re-lit it. */
private const val PULSE_ENDING = "ra.gate.pulse_ending"

/*
 * OUTPUT PULSES (Counter, Edge Detector). A pulse of N ticks is lit by [pulseStart] and put out N
 * ticks later. process runs [pulseCountdown] first and [pulseFinish] last, with any [pulseStart] in
 * between, so a pulse re-triggered on the very tick it runs out is extended rather than switched
 * off and on again inside one tick (both fills in the same tick read as no pulse at all; see the
 * Clock). An idle gate pays two tag tests: the shell is only written when a pulse starts or ends.
 */

/** Top of process: count the lit pulse down. As the marker. */
internal fun Function.pulseCountdown() {
	raw("scoreboard players remove @s[tag=$PULSING] ${Obj.COOLDOWN} 1")
	raw("execute if entity @s[tag=$PULSING] if score @s ${Obj.COOLDOWN} matches ..0 run tag @s add $PULSE_ENDING")
}

/** Light the output for `#prop ra.temp` ticks (at least 1), or restart a lit pulse. As the marker, at it. */
internal fun Function.pulseStart() {
	raw("scoreboard players operation @s ${Obj.COOLDOWN} = #prop ${Obj.TEMP}")
	raw("tag @s remove $PULSE_ENDING")
	raw("execute if entity @s[tag=!$PULSING] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
	raw("tag @s add $PULSING")
}

/** End of process: put out a pulse that ran out. As the marker, at it. */
internal fun Function.pulseFinish() {
	raw("execute if entity @s[tag=$PULSE_ENDING] run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
	raw("tag @s[tag=$PULSE_ENDING] remove $PULSING")
	raw("tag @s remove $PULSE_ENDING")
}

/** Drop a lit pulse without touching the shell (the caller puts it out). As the marker. */
internal fun Function.pulseCancel() {
	raw("tag @s remove $PULSING")
	raw("tag @s remove $PULSE_ENDING")
}

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
