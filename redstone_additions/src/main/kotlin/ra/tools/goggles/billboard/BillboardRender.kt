package ra.tools.goggles.billboard

import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.api.Goggles.Billboard
import ra.core.Obj
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw

/**
 * LAYOUT: ONE TEXT_DISPLAY PER BILLBOARD, GROWING UPWARD FROM A FIXED BASE
 *
 * A billboard used to be one text_display per line, each at a height its block picked by hand
 * from an anchor 1.3 above the block: the name of a one-line block floated as high as a five-line
 * one's, and a long ladder could reach back into the block. Now the lines are joined with "\n"
 * into one text component ([Billboard.append]) and drawn by one display whose position is the
 * bottom edge of the text: a text_display lays its lines out upward from its origin. So the base
 * stays put and more lines simply grow the billboard upward.
 *
 * Line-height maths. The font's line is 9 px of glyph plus 1 px of spacing = 10 px, and a
 * text_display draws 1 px as 0.025 block before its transformation, so one line is
 *   10 * 0.025 * SCALE = 0.25 * SCALE blocks   ->   0.1 block at SCALE 0.4,
 * and N lines stand N * 0.1 block tall (plus 1 px = 0.01 block of background under the base).
 * A block's billboard starts at [BLOCK_BASE], 0.3 above the block's top face, so a name and two
 * lines (0.3 block) end 0.6 above the block. An item line's item is drawn centred
 * [ITEM_ABOVE_HUNDREDTHS] above the text's top, see flush_here.
 *
 * One entity per billboard instead of 1 + N, and one summon + two data writes per redraw instead
 * of a macro summon per line.
 */
private const val SCALE = "0.4f"

/** Hundredths of a block per line at [SCALE]: 0.25 * 0.4 = 0.10. */
private const val LINE_HUNDREDTHS = 10

/** Item centre above the top of the text, in hundredths: 0.05 gap + 0.2, half of a 0.4-scaled item. */
private const val ITEM_ABOVE_HUNDREDTHS = 25

/** Where a single block's billboard starts: block corner + (0.5, 1.3, 0.5), 0.3 above its top face. */
internal const val BLOCK_BASE = "align xyz positioned ~0.5 ~1.3 ~0.5"

private const val TEXT_DISPLAY_NBT =
	"""{Tags:["ra","ra.billboard","ra.display"],billboard:"center",alignment:"center",background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$SCALE,$SCALE,$SCALE]}}"""

/** The I/O label text_display; these stay one per label, at their I/O block. */
private fun ioDisplay(text: String) =
	"""{Tags:["ra","ra.billboard","ra.display"],billboard:"center",text:$text,background:553648127,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$(scale)f,$(scale)f,$(scale)f]}}"""

internal fun Ra.registerBillboardRender() {
	val temp = Obj.TEMP

	// Initialize the configurable I/O label offsets once. (billboard_name and status_line, the
	// per-line offsets of the old layout, are no longer read: the billboard lays itself out.)
	pack.define(Billboard.initOffsets) {
		raw("execute if data storage ra:display offsets.io_label if data storage ra:display offsets.multiblock_io run return 0")
		raw("data modify storage ra:display offsets merge value {io_label:{x:0.0,y:1.9,z:0.0,scale:0.4},multiblock_io:{x:0.0,y:1.9,z:0.0,scale:0.4}}")
	}

	// MACRO {name}, with storage ra:temp billboard: the block's name, as the billboard's first line.
	// As the block marker. `show_name` (implied by a name) is still honoured.
	pack.define(Billboard.handleBillboard) {
		raw("execute unless data storage ra:temp billboard.show_name if data storage ra:temp billboard.name run data modify storage ra:temp billboard.show_name set value 1b")
		raw("execute unless data storage ra:temp billboard.show_name run return 0")
		raw("\$function ${Billboard.renderName.asId()} {name:\"\$(name)\"}")
	}

	// MACRO {name}: the name line, gold.
	pack.define(Billboard.renderName) {
		raw("""${'$'}data modify storage ra:temp gg.line set value {text:"$(name)",color:"gold"}""")
		function(Billboard.append)
	}

	// MACRO {label, value, suffix, value_color}: one literal line.
	pack.define(Billboard.renderLiteralLine) {
		raw("""${'$'}data modify storage ra:temp gg.line set value {text:"$(label)",color:"gray",extra:[{text:"$(value)",color:"$(value_color)"},{text:"$(suffix)",color:"gray"}]}""")
		function(Billboard.append)
	}

	// MACRO {x, y, z, ox, oy, oz, label, color, scale}: one I/O label.
	pack.define(Billboard.renderIoLine) {
		raw(
			"\$execute positioned ~\$(x) ~\$(y) ~\$(z) align xyz positioned ~0.5 ~0.5 ~0.5 run summon text_display ~\$(ox) ~\$(oy) ~\$(oz) " +
				ioDisplay("""{text:"$(label)",color:"$(color)",italic:false}""")
		)
	}

	// A line from storage ra:temp status_literal {label, value, suffix, value_color}, each optional;
	// x / y / z / scale are accepted and ignored. As the source entity.
	pack.define(Billboard.showLiteralLine) {
		for ((field, default) in listOf("label" to "", "value" to "N/A", "suffix" to "", "value_color" to "white"))
			raw("execute unless data storage ra:temp status_literal.$field run data modify storage ra:temp status_literal.$field set value \"$default\"")
		callWith(Billboard.renderLiteralLine, "storage ra:temp status_literal")
	}

	// As the marker, at the billboard's base: summon what was built since begin, if anything.
	pack.define(Billboard.flushHere) {
		raw("execute if data storage ra:temp name_only run return 0")
		raw("execute unless score #gg.lines $temp matches 1.. run return 0")
		executeRun("summon text_display", Billboard.spawnText)
		raw("execute unless data storage ra:temp gg.item run return 0")
		// Item centre = base + lines * LINE_HUNDREDTHS + ITEM_ABOVE_HUNDREDTHS, in hundredths.
		raw("scoreboard players set #gg.iy $temp $LINE_HUNDREDTHS")
		raw("scoreboard players operation #gg.iy $temp *= #gg.lines $temp")
		raw("scoreboard players add #gg.iy $temp $ITEM_ABOVE_HUNDREDTHS")
		raw("execute store result storage ra:temp gg.item.y double 0.01 run scoreboard players get #gg.iy $temp")
		callWith(Billboard.spawnItem, "storage ra:temp gg.item")
	}

	// As the new text_display, where flush_here summoned it.
	pack.define(Billboard.spawnText) {
		raw("data merge entity @s $TEXT_DISPLAY_NBT")
		raw("data modify entity @s text set from storage ra:temp gg.text")
	}

	// {id, y}: the item of an item line, y above here. Tagged ra.billboard like the text, so the same
	// sweep tears it down: a filter shown from a permanent display would need its own upkeep, and
	// an earlier version drew one that was never cleaned up.
	pack.define(Billboard.spawnItem) {
		raw("""${'$'}summon item_display ~ ~$(y) ~ {item:{id:"$(id)",count:1},item_display:"fixed",billboard:"vertical",brightness:{sky:15,block:15},Tags:["ra","ra.billboard","ra.display"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.4f,0.4f,0.4f]}}""")
	}

	// Render one colored I/O label from storage ra:temp io.
	pack.define(Billboard.showIoLine) {
		executeRun("unless data storage ra:display offsets.io_label", Billboard.initOffsets)
		for (axis in listOf("x", "y", "z"))
			raw("execute unless data storage ra:temp io.$axis run data modify storage ra:temp io.$axis set value 0")
		raw("execute unless data storage ra:temp io.label run data modify storage ra:temp io.label set value \"I/O\"")
		raw("execute unless data storage ra:temp io.color run data modify storage ra:temp io.color set value \"white\"")
		for ((field, offset) in listOf("ox" to "x", "oy" to "y", "oz" to "z", "scale" to "scale"))
			raw("execute unless data storage ra:temp io.$field run data modify storage ra:temp io.$field set from storage ra:display offsets.io_label.$offset")
		callWith(Billboard.renderIoLine, "storage ra:temp io")
	}

	// MACRO {label, x, y, z}: one colored indicator at an I/O block, as the multiblock marker.
	// Green = Input, Red = Output, Yellow = Control.
	for ((fn, color) in listOf(Billboard.showInput to "green", Billboard.showOutput to "red", Billboard.showControl to "yellow"))
		pack.define(fn) {
			raw("""${'$'}data modify storage ra:temp io set value {x:$(x),y:$(y),z:$(z),label:"$(label)",color:"$color"}""")
			function(Billboard.showIoLine)
		}

	// MACRO {name}: a generic multiblock billboard -- name (+ blast forge status) above a 2-tall
	// structure, and its I/O indicators. As the multiblock marker, at its base; draw_multiblock
	// has already called begin.
	pack.define(Billboard.handleMultiblock) {
		raw("""${'$'}data modify storage ra:temp gg.line set value {text:"$(name)",color:"green",bold:true}""")
		function(Billboard.append)
		// Heat status for the blast forge.
		executeRun("if entity @s[tag=ra.multiblock.blast_forge]", Goggles.Status.readStatusMultiblock)
		executeRun("positioned ~ ~2.3 ~", Billboard.flushHere)
		// Copy each indicator's offsets + label to temp storage, then call its show function.
		val io = listOf(
			Triple("inputs.input_1", "Input (Material)", Billboard.showInput),
			Triple("inputs.input_2", "Input (Fuel)", Billboard.showInput),
			Triple("outputs.output_1", "Output", Billboard.showOutput),
			Triple("controls.redstone_in", "Redstone Control", Billboard.showControl),
		)
		for ((path, label, show) in io) {
			raw("execute if data entity @s data.$path run data modify storage ra:temp io set from entity @s data.$path")
			raw("execute if data entity @s data.$path run data modify storage ra:temp io.label set value \"$label\"")
			executeRun("if data entity @s data.$path", show, "with storage ra:temp io")
		}
	}
}
