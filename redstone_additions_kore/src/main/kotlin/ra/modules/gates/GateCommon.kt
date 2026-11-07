package ra.modules.gates

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.fill
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.summon
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.markers
import ra.core.block.propLine
import ra.core.define
import ra.core.raw

/**
 * Pieces every `ra_gates` block shares: the 3x3x3 output shell, the redstone read that ignores
 * that shell, the debug registration line and the break handling of the timing blocks.
 */

/** Set while reading redstone, so the gate's own redstone_block shell does not read back as input. */
internal const val IGNORE_BLOCKS = "ra.redstone.ignore_blocks"

/** Input latch of the edge-triggered blocks: @s was powered last tick. */
internal const val WAS_POWERED = "ra.was_powered"

/**
 * Switches the output shell: iron blocks in the 3x3x3 cube around the gate become redstone
 * blocks ([on]) or back to iron. Callers run it `at @s`.
 */
internal fun Function.shell(on: Boolean) = if (on)
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.REDSTONE_BLOCK, Blocks.IRON_BLOCK)
else
	fill(vec3(-1, -1, -1).relative, vec3(1, 1, 1).relative, Blocks.IRON_BLOCK, Blocks.REDSTONE_BLOCK)

/** `ra_lib:redstone/detect_switch` with the shell ignored. Sets `ra.powered` on @s. */
internal fun Function.detectSwitchIgnoringShell() {
	tag(self()) { add(IGNORE_BLOCKS) }
	function(RaLib.Redstone.detectSwitch)
	tag(self()) { remove(IGNORE_BLOCKS) }
}

/** `ra_lib:util/property {name, default, min}` -> `#prop ra.temp`. */
internal fun Function.readProperty(name: String, default: Int, min: Int) =
	function(RaLib.Util.property, nbt { this["name"] = name; this["default"] = default; this["min"] = min })

/** `blocks/<id>/register_block`: a debug line only, so `ra_gates:load` stays verbose for ra.debug. */
internal fun DataPack.registerBlockMessage(block: CustomBlock) = define(block.fn("register_block")) {
	raw("""tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"${block.name} registered",color:"gray"}]""")
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
 * `blocks/<id>/on_break` of the timing blocks (Delayer, Extender, Shortener, Randomizer): put the
 * output out, swap the vanilla drop for the custom item, break effects, remove the marker.
 *
 * The custom item is summoned WITHOUT the `ra.drop_temp` tag, exactly as the beet version did.
 * Keep it that way: some interactive blocks overwrite the first `ra.drop_temp` item they find
 * with their container contents, so a tagged drop lying nearby could be replaced.
 */
internal fun DataPack.timingOnBreak(block: CustomBlock, breakSound: String): FunctionArgument = define(block.fn("on_break")) {
	killVanillaDrop(block.physical)
	shell(on = false)
	summon(EntityTypes.ITEM, vec3(), nbt { this["Item"] = block.itemStackNbt() })
	raw("playsound $breakSound block @a[distance=..16] ~ ~ ~ 1 1")
	raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5")
	kill(self())
}

/**
 * `blocks/<id>/tick` of the timing blocks: a marker whose block is no longer [physical] breaks,
 * then every marker runs `process`.
 *
 * The test is "not the block any more" rather than "air", as in the beet version: a timing block
 * replaced by something else (a piston pushing, a player swapping it) breaks too.
 */
internal fun DataPack.timingTick(block: CustomBlock, physical: BlockArgument, onBreak: FunctionArgument, process: FunctionArgument) = define(block.tick) {
	execute {
		asTarget(markers(block))
		at(self())
		unlessCondition { block(vec3(), physical) }
		run { function(onBreak) }
	}
	execute {
		asTarget(markers(block))
		at(self())
		run { function(process) }
	}
}
