package ra.tools.goggles.billboard

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import ra.api.Goggles.Billboard
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw

/**
 * The line writers a block's `goggles` function calls, as the block marker at its position.
 *
 * THE NAMING IN HERE IS A TRAP
 *   prop_line   / stacked_prop_line     -> data.properties  (player configures)
 *   data_line                           -> data.status      (block computes)
 *   stacked_data_line                   -> data.data        (block's private state)
 *   stacked_status_line                 -> data.status
 *
 * `data_line` and `stacked_data_line` do NOT read the same place, which is easy to miss and silent
 * when you get it wrong: the value is simply absent and the line renders "N/A". The Electric Furnace
 * shipped with every readout blank for exactly that reason -- it wrote data.status and the stacked
 * reader looked in data.data. stacked_status_line is the stacked counterpart of data_line that was
 * missing.
 *
 * A missing value renders "N/A" in red rather than being skipped, so a block that has not been
 * configured yet says so instead of silently showing nothing.
 *
 * ONE BILLBOARD, BUILT AS A LIST
 * Every writer appends one text component to `storage ra:temp gg.text`; the dispatcher
 * (goggles/draw_block) started the list with [Billboard.begin] and summons it as ONE text_display
 * afterwards ([Billboard.flushHere]). Lines therefore land in call order, top to bottom, and the
 * hand-picked `y` the plain writers take, the stacked writers' ladder and stack_reset are all
 * accepted and ignored: the display lays its own lines out. That is what used to go wrong -- every
 * block invented its own ladder, a block with one line too many drew inside itself, and one with
 * a single line still floated as high as one with five.
 *
 * PROPERTY LINES ARE GATED BY THE SCHEMA
 * A prop_line draws only when its property's resolved `goggles` flag (see ra.core.Properties) says
 * so: by default whatever the Data Handler shows players, `goggles = true` to show a value hidden
 * there, `goggles = false` to keep a visible one off the billboard. Status and data lines are the
 * block's own readout and are not gated.
 */
internal fun Ra.registerBillboardLines() {
	val temp = Obj.TEMP

	// As the marker. Clears the billboard being built; draw_block / draw_multiblock call it before
	// routing, so nothing from the previous block can leak into this one.
	pack.define(Billboard.begin) {
		raw("data modify storage ra:temp gg set value {text:[{text:\"\"}]}")
		raw("scoreboard players set #gg.lines $temp 0")
	}

	// storage ra:temp gg.line -> the next line. A "\n" component goes between lines, not before the
	// first, so the display has no empty line to pad it. The list's first element is an empty
	// component: as the parent of the others it gives them no style to inherit.
	pack.define(Billboard.append) {
		raw("execute if score #gg.lines $temp matches 1.. run data modify storage ra:temp gg.text append value {text:\"\\n\"}")
		raw("data modify storage ra:temp gg.text append from storage ra:temp gg.line")
		raw("scoreboard players add #gg.lines $temp 1")
	}

	// As the marker: its type's schema compound, once per billboard (the first prop_line asks), so
	// each line after that tests its flag with one storage match. `type:"-"` keeps the macro
	// argument present on a marker with no data.type; `types."-"` does not exist, so nothing is gated.
	pack.define(Billboard.loadSchema) {
		raw("data modify storage ra:temp gg.loaded set value 1b")
		raw("data modify storage ra:temp gg.q set value {type:\"-\"}")
		raw("data modify storage ra:temp gg.q.type set from entity @s data.type")
		callWith(Billboard.loadSchemaAt, "storage ra:temp gg.q")
	}
	// {type}
	pack.define(Billboard.loadSchemaAt) {
		raw("\$data modify storage ra:temp gg.schema set from storage ra:dh types.\"\$(type)\"")
	}

	// {path,label,color,suffix,y}
	valueLine(Billboard.propLine, "properties")
	valueLine(Billboard.dataLine, "status")
	// {path,label,color,suffix}
	valueLine(Billboard.stackedPropLine, "properties")
	valueLine(Billboard.stackedStatusLine, "status")
	valueLine(Billboard.stackedDataLine, "data")

	// {label,value,color,suffix[,y]}: a literal value.
	for (fn in listOf(Billboard.textLine, Billboard.stackedTextLine))
		pack.define(fn) {
			raw("""${'$'}data modify storage ra:temp gg.line set value {text:"$(label)",color:"gray",extra:[{text:"$(value)",color:"$(color)"},{text:"$(suffix)",color:"gray"}]}""")
			function(Billboard.append)
		}

	// {top,step}: the old ladder. Kept so existing callers (and add-ons) still load; the billboard
	// lays its lines out itself now, so nothing reads these.
	pack.define(Billboard.stackReset) {
		raw("\$scoreboard players set #gg.stack_y $temp \$(top)")
		raw("\$scoreboard players set #gg.stack_step $temp \$(step)")
	}

	// {id:"minecraft:iron_ingot"[,y]}: draw one item, floating above the text, as part of the readout.
	//
	// The text lines say what a value IS; this says what it LOOKS like, which is the only useful
	// way to show an item filter -- "minecraft:polished_blackstone_slab" read as text tells you far
	// less than the block does.
	//
	// Only remembered here: flush_here summons it once the line count, and so the height of the
	// text under it, is known. `y` is ignored like every other line's.
	pack.define(Billboard.itemLine) {
		raw("\$data modify storage ra:temp gg.item set value {id:\"\$(id)\"}")
	}
}

/**
 * A line showing `data.<source>.$(path)` from the marker.
 *
 * The value is copied with `set string`, which writes a number's digits and a string as is, and
 * fails for a missing value or a list/compound -- the line then keeps its red "N/A". A string with
 * quotes in it no longer breaks the line, as it did when the value went through a macro argument.
 */
private fun Ra.valueLine(fn: FunctionArgument, source: String) = pack.define(fn) {
	val temp = Obj.TEMP
	if (source == "properties") {
		executeRun("unless data storage ra:temp gg{loaded:1b}", Billboard.loadSchema)
		// Every test is "present and not 1b" rather than a match on 0b: the same thing for the
		// bytes the registry writes, and it does not depend on how a reader stores a false.
		val s = "storage ra:temp gg.schema.\$(path)"
		val i = "entity @s data.property_types.\$(path)"
		// The schema's resolved flag: decides every declared, non-dynamic property in one line.
		raw("\$execute if data $s.goggles unless data $s{goggles:1b} run return 0")
		// A type written without `goggles` (an add-on's own, or an older pack): follow `visible`.
		raw("\$execute unless data $s.goggles if data $s.visible unless data $s{visible:1b} unless data $s{dynamic:1b} run return 0")
		// Dynamic or undeclared: the instance's own declaration, if it has one. Only read when the
		// schema had no verdict, so a declared property never pays for these entity reads.
		raw("\$execute unless data $s.goggles if data $i.goggles unless data $i{goggles:1b} run return 0")
		raw("\$execute unless data $s.goggles unless data $i.goggles if data $i.visible unless data $i{visible:1b} run return 0")
	}
	raw("""${'$'}data modify storage ra:temp gg.line set value {text:"$(label)",color:"gray",extra:[{text:"N/A",color:"red"},{text:"$(suffix)",color:"gray"}]}""")
	raw("\$execute store success score #gg.ok $temp run data modify storage ra:temp gg.line.extra[0].text set string entity @s data.$source.\$(path)")
	raw("\$execute if score #gg.ok $temp matches 1 run data modify storage ra:temp gg.line.extra[0].color set value \"\$(color)\"")
	function(Billboard.append)
}
