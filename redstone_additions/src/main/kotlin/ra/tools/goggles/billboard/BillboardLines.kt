package ra.tools.goggles.billboard

import ra.core.computeInto
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import ra.core.storeDouble
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.predicates.providers.minus
import ra.api.Goggles
import ra.api.Goggles.Billboard
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.callWith
import ra.core.define
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
 * Hand-picking a y per line, which is what prop_line / text_line / data_line take, means every
 * block invents its own ladder and a block with one line too many ends up drawing inside itself. A
 * block that stacks says where its lines start and how far apart they sit, once (stack_reset).
 */
internal fun Ra.registerBillboardLines() {
	// {path,label,color,suffix,y}
	valueLine(Billboard.propLine, "properties", stacked = false)
	valueLine(Billboard.dataLine, "status", stacked = false)
	// {path,label,color,suffix}: no hand-picked height, the line lands wherever the stack is up to.
	valueLine(Billboard.stackedPropLine, "properties", stacked = true)
	valueLine(Billboard.stackedStatusLine, "status", stacked = true)
	valueLine(Billboard.stackedDataLine, "data", stacked = true)

	// {label,value,color,suffix,y}: a literal value.
	pack.define(Billboard.textLine) {
		data(Storage.TEMP) { remove("status_literal") }
		raw("""${'$'}data modify storage ra:temp status_literal set value {y:$(y),label:"$(label)",value:"$(value)",value_color:"$(color)",suffix:"$(suffix)"}""")
		callWith(Billboard.showLiteralLine, "storage ra:temp status_literal")
	}
	pack.define(Billboard.stackedTextLine) {
		data(Storage.TEMP) { remove("status_literal") }
		raw("""${'$'}data modify storage ra:temp status_literal set value {label:"$(label)",value:"$(value)",value_color:"$(color)",suffix:"$(suffix)"}""")
		function(Billboard.stackNext)
		callWith(Billboard.showLiteralLine, "storage ra:temp status_literal")
	}

	// {top,step}: start a stack of status lines at a height this block chooses. Before any stacked_*.
	// top and step are in hundredths of a block, so {top:80,step:20} puts the first line 0.8 above
	// the block and each following one 0.2 lower. Integers because macro arguments are pasted as
	// text and scoreboards do the arithmetic.
	pack.define(Billboard.stackReset) {
		raw("\$scoreboard players set #gg.stack_y ${Obj.TEMP} \$(top)")
		raw("\$scoreboard players set #gg.stack_step ${Obj.TEMP} \$(step)")
	}

	// Write the next stacked height into the line being built, then step down.
	pack.define(Billboard.stackNext) {
		// A block that forgot stack_reset still gets a sane ladder rather than a broken line.
		// The score providers' fallbacks are that ladder: an absent top reads 80, an absent step 20.
		val y = scoreOf("#gg.stack_y", Obj.TEMP, 80)
		computeInto(storeDouble(Storage.TEMP, "status_literal.y", 0.01), y)
		computeScore("#gg.stack_y", Obj.TEMP, provider { y - scoreOf("#gg.stack_step", Obj.TEMP, 20) })
	}

	// {id:"minecraft:iron_ingot",y:0.9}: draw one item, floating, as part of the readout.
	//
	// The text lines say what a value IS; this says what it LOOKS like, which is the only useful
	// way to show an item filter -- "minecraft:polished_blackstone_slab" read as text tells you far
	// less than the block does.
	//
	// Tagged ra.billboard like every other goggles display, so it is torn down and redrawn by the
	// same sweep. That matters: a filter shown from a permanent display has to be kept in step with
	// the property by its own upkeep, and an earlier version of this drew one that was never
	// cleaned up. Anchored the same way render_literal_line is, so it lines up with the text.
	pack.define(Billboard.itemLine) {
		raw("""${'$'}execute align xyz positioned ~0.5 ~1.3 ~0.5 run summon item_display ~ ~$(y) ~ {item:{id:"$(id)",count:1},item_display:"fixed",billboard:"vertical",brightness:{sky:15,block:15},Tags:["ra","ra.billboard","ra.display"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.4f,0.4f,0.4f]}}""")
	}
}

/** A line showing `data.<source>.$(path)` from the marker, at `$(y)` or at the next stacked height. */
private fun Ra.valueLine(fn: FunctionArgument, source: String, stacked: Boolean) = pack.define(fn) {
	val y = if (stacked) "" else "y:$(y),"
	data(Storage.TEMP) { remove("status_literal") }
	raw("""${'$'}data modify storage ra:temp status_literal set value {${y}label:"$(label)",value:"N/A",value_color:"red",suffix:"$(suffix)"}""")
	raw("\$execute if data entity @s data.$source.\$(path) run data modify storage ra:temp status_literal.value set from entity @s data.$source.\$(path)")
	raw("\$execute if data entity @s data.$source.\$(path) run data modify storage ra:temp status_literal.value_color set value \"\$(color)\"")
	if (stacked) function(Billboard.stackNext)
	callWith(Billboard.showLiteralLine, "storage ra:temp status_literal")
}
