package ra.tools.creativedatahandler.props

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.datahandler.DataHandlerRegistry
import ra.tools.datahandler.DataHandlerSelection
import ra.tools.datahandler.PropType
import ra.tools.tool

/**
 * `ra:tools/creative_data_handler/props/show_<name>`: one chat row per known property, with a
 * [Modify] button that suggests the `/data modify` command and a [-] button that removes it.
 *
 * The Creative Data Handler predates the registry-driven Data Handler and still has one row
 * function per property; they differ only in the data below, so they are generated from it.
 */
internal class CdhProp(
	val name: String,
	val valueColor: String,
	/** Hover on [Modify] (or on the value, for [clickValue] rows). */
	val hover: String? = null,
	/** Shown right after the value, inside the brackets (`%`). */
	val unit: String? = null,
	/** Read straight off the marker rather than from the `ra:cdh` snapshot. */
	val fromEntity: Boolean = false,
	/** The value itself is the edit button; no [Modify]. */
	val clickValue: Boolean = false,
) {
	val show: FunctionArgument = tool("creative_data_handler/props/show_$name")

	/** The type shown, from the property's declaration -- one list for both Data Handlers. */
	val decl = requireNotNull(DataHandlerRegistry.declOf(name)) { "Creative Data Handler row $name has no declared type" }

	/** Shown type and its colour. */
	val type get() = when (decl.type) {
		PropType.NUMBER -> "int"
		PropType.TEXT -> "string"
		else -> decl.type.id
	}
	val typeColor get() = when (decl.type) {
		PropType.NUMBER -> "blue"
		PropType.BOOL -> "light_purple"
		PropType.LIST -> "aqua"
		else -> "green"
	}

	/** A text value: the suggested command opens a quote. */
	val quoted get() = decl.type == PropType.TEXT || decl.type == PropType.ITEM
}

internal object CdhProps {
	/** The player's selected block, for the rows that read the marker itself. */
	private const val TARGET = DataHandlerSelection.TARGET

	/** The same block by number, `{mid}`: a suggested command still finds it when typed later. */
	private val BY_MID = DataHandlerSelection.byMid("\$(mid)")

	val all = listOf(
		CdhProp("chance", "gold", unit = "%"),
		CdhProp("channel", "gold"),
		CdhProp("cooldown", "gold", fromEntity = true),
		CdhProp("delay", "gold"),
		CdhProp("distance", "gold"),
		CdhProp("enabled", "light_purple"),
		CdhProp("entity_selector", "green"),
		CdhProp("extend", "gold"),
		CdhProp("gate", "green", clickValue = true, hover = "Click to edit (AND, OR, NOT, XOR, NAND, NOR, XNOR)"),
		CdhProp("gate_type", "green", clickValue = true, hover = "Click to edit value"),
		CdhProp("inverted", "light_purple", hover = "Click to edit (true/false)"),
		CdhProp("message_block", "green"),
		CdhProp("mode", "green"),
		CdhProp("power", "gold"),
		CdhProp("pulse", "gold", hover = "Click to edit value"),
		CdhProp("range", "gold", hover = "Click to edit value"),
		CdhProp("tag", "green", hover = "Click to edit value"),
		CdhProp("target", "green", hover = "Click to edit value"),
	).associateBy { it.name }

	/** `{mid}`: one row. */
	fun Ra.registerCdhProps(removeProperty: FunctionArgument) {
		for (p in all.values) pack.define(p.show) { raw("\$tellraw @s ${row(p, removeProperty)}") }
	}

	private fun row(p: CdhProp, removeProperty: FunctionArgument): String {
		val suggest = """click_event:{action:"suggest_command",command:"/data modify entity $BY_MID data.properties.${p.name} set value ${if (p.quoted) "\\\"" else ""}"}"""
		val hover = p.hover?.let { """,hover_event:{action:"show_text",value:"$it"}""" } ?: ""
		val source = if (p.fromEntity) """nbt:"data.properties.${p.name}",entity:"$TARGET"""" else """nbt:"properties.${p.name}",storage:"ra:cdh""""
		val value = if (p.clickValue) """{$source,color:"${p.valueColor}",$suggest$hover}""" else """{$source,color:"${p.valueColor}"}"""
		val parts = buildList {
			add("""{text:"  ${p.name}: ",color:"white"}""")
			add("""{text:"[",color:"dark_gray"}""")
			add(value)
			p.unit?.let { add("""{text:"$it",color:"${p.valueColor}"}""") }
			add("""{text:"]",color:"dark_gray"}""")
			add("""{text:" "}""")
			add("""{text:"${p.type}",color:"${p.typeColor}"}""")
			add("""{text:" "}""")
			if (!p.clickValue) {
				add("""{text:"[Modify]",color:"yellow",$suggest$hover}""")
				add("""{text:" "}""")
			}
			add("""{text:"[-]",color:"red",click_event:{action:"run_command",command:"/function ${removeProperty.asId()} {name:\"${p.name}\"}"},hover_event:{action:"show_text",value:"Remove property"}}""")
		}
		return parts.joinToString(",", "[", "]")
	}
}
