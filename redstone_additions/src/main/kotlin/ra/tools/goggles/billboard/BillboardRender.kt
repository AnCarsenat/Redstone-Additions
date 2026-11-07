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

/** The text_display a billboard line is drawn on; only the `text` and `scale` differ. */
private fun display(text: String, background: Long, scale: String) =
	"""{Tags:["ra","ra.billboard","ra.display"],billboard:"center",text:$text,background:$background,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$scale,$scale,$scale]}}"""

private const val MACRO_SCALE = "$(scale)f"

/**
 * THE ANCHOR IS ABOVE THE BLOCK, NOT INSIDE IT
 * This used to anchor at the block centre (~0.5 ~0.5 ~0.5), which put the top face of the block at
 * y+0.5. Every ladder in the pack counts downwards from its first line, so the third line onwards
 * landed under that top face and the text rendered inside the block that owned it -- readable only
 * by standing in the wall. Anchoring at ~1.3 lifts the whole ladder clear: the lowest offset any
 * caller uses is -0.2, which now sits at y+1.1, just above the block.
 *
 * render_name uses the same anchor, so the name still sits above the lines rather than the two
 * swapping places. Both must move together or the name drops in among the lines.
 */
private const val ANCHOR = "align xyz positioned ~0.5 ~1.3 ~0.5"

internal fun Ra.registerBillboardRender() {
	// Initialize configurable text-display offsets once.
	pack.define(Billboard.initOffsets) {
		raw("execute if data storage ra:display offsets run return 0")
		raw("data modify storage ra:display offsets set value {billboard_name:{x:0.0,y:1.0,z:0.0,scale:0.5},status_line:{x:0.0,y:0.8,z:0.0,scale:0.35},io_label:{x:0.0,y:1.9,z:0.0,scale:0.4},multiblock_name:{x:0.0,y:2.1,z:0.0,scale:0.6},multiblock_io:{x:0.0,y:1.9,z:0.0,scale:0.4}}")
	}

	// MACRO {name}, with storage ra:temp billboard: the block's name billboard.
	// As the block marker, at the block. Optional in billboard: show_name, name_x/y/z, name_scale
	// (defaults from offsets.billboard_name).
	pack.define(Billboard.handleBillboard) {
		executeRun("unless data storage ra:display offsets.billboard_name", Billboard.initOffsets)
		// A caller that supplied a name wants it rendered.
		raw("execute unless data storage ra:temp billboard.show_name if data storage ra:temp billboard.name run data modify storage ra:temp billboard.show_name set value 1b")
		raw("execute unless data storage ra:temp billboard.show_name run return 0")
		val shown = "execute if data storage ra:temp billboard.show_name"
		raw("$shown run data modify storage ra:temp billboard_render set from storage ra:display offsets.billboard_name")
		for ((key, field) in listOf("name_x" to "x", "name_y" to "y", "name_z" to "z", "name_scale" to "scale"))
			raw("$shown if data storage ra:temp billboard.$key run data modify storage ra:temp billboard_render.$field set from storage ra:temp billboard.$key")
		raw("\$$shown run data modify storage ra:temp billboard_render.name set value \"\$(name)\"")
		executeRun("if data storage ra:temp billboard.show_name", Billboard.renderName, "with storage ra:temp billboard_render")
		// No status section here any more. Each block emits its own lines from its
		// blocks/<name>/goggles function, using prop_line / data_line / text_line, which is what
		// removed the central read_status dispatch table.
	}

	// MACRO {name, x, y, z, scale}: a centered block name billboard.
	pack.define(Billboard.renderName) {
		raw("\$execute $ANCHOR run summon text_display ~\$(x) ~\$(y) ~\$(z) " + display("""{text:"$(name)",color:"gold",bold:false}""", 1073741824, MACRO_SCALE))
	}

	// MACRO {x, y, z, scale, label, value, suffix, value_color}: one literal status line.
	pack.define(Billboard.renderLiteralLine) {
		raw(
			"\$execute $ANCHOR run summon text_display ~\$(x) ~\$(y) ~\$(z) " +
				display("""[{text:"$(label)",color:"gray"},{text:"$(value)",color:"$(value_color)"},{text:"$(suffix)",color:"gray"}]""", 1073741824, MACRO_SCALE)
		)
	}

	// MACRO {x, y, z, ox, oy, oz, label, color, scale}: one I/O label.
	pack.define(Billboard.renderIoLine) {
		raw(
			"\$execute positioned ~\$(x) ~\$(y) ~\$(z) align xyz positioned ~0.5 ~0.5 ~0.5 run summon text_display ~\$(ox) ~\$(oy) ~\$(oz) " +
				display("""{text:"$(label)",color:"$(color)",italic:false}""", 553648127, MACRO_SCALE)
		)
	}

	// Show a value line from storage ra:temp status_literal. As the source entity, at its position.
	//   y = vertical offset (relative to the anchor), x/z = optional horizontal offsets,
	//   scale = optional text scale, label / suffix = gray text, value = literal text,
	//   value_color = color for the value.
	pack.define(Billboard.showLiteralLine) {
		executeRun("unless data storage ra:display offsets.status_line", Billboard.initOffsets)
		for (axis in listOf("x", "y", "z", "scale"))
			raw("execute unless data storage ra:temp status_literal.$axis run data modify storage ra:temp status_literal.$axis set from storage ra:display offsets.status_line.$axis")
		for ((field, default) in listOf("label" to "", "value" to "N/A", "suffix" to "", "value_color" to "white"))
			raw("execute unless data storage ra:temp status_literal.$field run data modify storage ra:temp status_literal.$field set value \"$default\"")
		// Floor, so a ladder can never reach back down into its own block. The anchor is 1.3 above
		// the block corner and the block is 1 tall, so anything below -0.3 is inside it. A block with
		// more lines than fit now piles the extras on the bottom rung, which is ugly but readable --
		// being inside the block was neither.
		raw("execute store result score #gg.y ${Obj.TEMP} run data get storage ra:temp status_literal.y 100")
		raw("execute if score #gg.y ${Obj.TEMP} matches ..-25 run data modify storage ra:temp status_literal.y set value -0.25d")
		callWith(Billboard.renderLiteralLine, "storage ra:temp status_literal")
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

	// MACRO {name}: multiblock name + I/O indicators. As the multiblock marker, at its base.
	pack.define(Billboard.handleMultiblock) {
		raw("\$summon text_display ~ ~2.2 ~ " + display("""{text:"$(name)",color:"green",bold:true}""", 1073741824, "0.6f"))
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
		// Heat status for the blast forge.
		executeRun("if entity @s[tag=ra.multiblock.blast_forge]", Goggles.Status.readStatusMultiblock)
	}
}
