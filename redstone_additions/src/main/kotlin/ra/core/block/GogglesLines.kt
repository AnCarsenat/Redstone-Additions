package ra.core.block

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.core.raw

/**
 * Writing a block's `goggles` function. Contract (see ra:tools/goggles):
 *
 * 1. publish the display name to `storage ra:temp block_name`;
 * 2. stop there if `storage ra:temp name_only` is set (that is how ra:tools/block_name asks
 *    for a name without drawing anything);
 * 3. draw the name billboard, then one line per readout.
 *
 * [gogglesHeader] does 1-3; the line helpers wrap the billboard macros with named arguments.
 *
 * Lines are appended in call order to ONE text_display that the goggles summon after this function,
 * 0.3 above the block, growing upward with the line count (see ra.tools.goggles.billboard). The
 * `y` / `nameY` offsets and [stackReset] are kept for source compatibility and ignored: call the
 * helpers top line first. A property line also obeys its schema's `goggles` flag (ra.core.Properties).
 */
fun Function.gogglesHeader(block: CustomBlock, nameY: Double = 1.0) = gogglesHeader(block.name, nameY)

fun Function.gogglesHeader(name: String, nameY: Double = 1.0) {
	raw("data modify storage ra:temp block_name set value \"$name\"")
	raw("execute if data storage ra:temp name_only run return 0")
	raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:$nameY}")
	raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
	raw("function ${Goggles.Billboard.handleBillboard.asId()} with storage ra:temp billboard")
}

/** A `data.properties.<path>` value. */
fun Function.propLine(path: String, label: String, color: String, y: Double, suffix: String = "") =
	function(Goggles.Billboard.propLine, nbt {
		this["path"] = path; this["label"] = label; this["color"] = color; this["suffix"] = suffix; this["y"] = y
	})

/** A `data.status.<path>` value. */
fun Function.dataLine(path: String, label: String, color: String, y: Double, suffix: String = "") =
	function(Goggles.Billboard.dataLine, nbt {
		this["path"] = path; this["label"] = label; this["color"] = color; this["suffix"] = suffix; this["y"] = y
	})

/** A literal value. */
fun Function.textLine(label: String, value: String, color: String, y: Double, suffix: String = "") =
	function(Goggles.Billboard.textLine, nbt {
		this["label"] = label; this["value"] = value; this["color"] = color; this["suffix"] = suffix; this["y"] = y
	})

/** Ignored (the billboard lays its own lines out); kept so existing callers compile. */
fun Function.stackReset(top: Int, step: Int) =
	function(Goggles.Billboard.stackReset, nbt { this["top"] = top; this["step"] = step })

/** Stacked `data.properties.<path>`. */
fun Function.stackedPropLine(path: String, label: String, color: String, suffix: String = "") =
	function(Goggles.Billboard.stackedPropLine, nbt { this["path"] = path; this["label"] = label; this["color"] = color; this["suffix"] = suffix })

/** Stacked `data.status.<path>`. */
fun Function.stackedStatusLine(path: String, label: String, color: String, suffix: String = "") =
	function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = path; this["label"] = label; this["color"] = color; this["suffix"] = suffix })

/** Stacked `data.data.<path>` -- the block's private working state, NOT its status. */
fun Function.stackedDataLine(path: String, label: String, color: String, suffix: String = "") =
	function(Goggles.Billboard.stackedDataLine, nbt { this["path"] = path; this["label"] = label; this["color"] = color; this["suffix"] = suffix })

/** Stacked literal. */
fun Function.stackedTextLine(label: String, value: String, color: String, suffix: String = "") =
	function(Goggles.Billboard.stackedTextLine, nbt { this["label"] = label; this["value"] = value; this["color"] = color; this["suffix"] = suffix })
