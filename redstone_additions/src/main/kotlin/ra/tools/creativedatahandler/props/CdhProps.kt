package ra.tools.creativedatahandler.props

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import ra.core.Ra
import ra.core.define
import ra.core.raw
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
	/** Shown type and its colour (`int`/blue, `str`/green...). */
	val type: String,
	val typeColor: String,
	val valueColor: String,
	/** A string value: the suggested command opens a quote. */
	val quoted: Boolean = false,
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
}

internal object CdhProps {
	private const val TARGET = "@e[type=marker,tag=ra.cdh_target,limit=1]"

	val all = listOf(
		CdhProp("chance", "int", "blue", "gold", unit = "%"),
		CdhProp("channel", "string", "green", "gold", quoted = true),
		CdhProp("cooldown", "int", "blue", "gold", fromEntity = true),
		CdhProp("delay", "int", "blue", "gold"),
		CdhProp("distance", "int", "blue", "gold"),
		CdhProp("enabled", "bool", "light_purple", "light_purple"),
		CdhProp("entity_selector", "str", "green", "green", quoted = true),
		CdhProp("extend", "int", "blue", "gold"),
		CdhProp("gate", "str", "green", "green", quoted = true, clickValue = true, hover = "Click to edit (AND, OR, NOT, XOR, NAND, NOR, XNOR)"),
		CdhProp("gate_type", "str", "green", "green", quoted = true, clickValue = true, hover = "Click to edit value"),
		CdhProp("inverted", "bool", "light_purple", "light_purple", hover = "Click to edit (true/false)"),
		CdhProp("message_block", "str", "green", "green", quoted = true),
		CdhProp("mode", "str", "green", "green", quoted = true),
		CdhProp("power", "int", "blue", "gold"),
		CdhProp("pulse", "int", "blue", "gold", hover = "Click to edit value"),
		CdhProp("range", "int", "blue", "gold", hover = "Click to edit value"),
		CdhProp("tag", "str", "green", "green", quoted = true, hover = "Click to edit value"),
		CdhProp("target", "str", "green", "green", quoted = true, hover = "Click to edit value"),
	).associateBy { it.name }

	fun Ra.registerCdhProps(removeProperty: FunctionArgument) {
		for (p in all.values) pack.define(p.show) { raw("tellraw @s ${row(p, removeProperty)}") }
	}

	private fun row(p: CdhProp, removeProperty: FunctionArgument): String {
		val suggest = """click_event:{action:"suggest_command",command:"/data modify entity $TARGET data.properties.${p.name} set value ${if (p.quoted) "\\\"" else ""}"}"""
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
