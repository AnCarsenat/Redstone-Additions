package ra.tools.creativedatahandler

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import net.benwoodworth.knbt.NbtByte
import ra.api.Goggles
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.REACH_4
import ra.tools.ToolItems
import ra.tools.creativedatahandler.props.CdhProps
import ra.tools.creativedatahandler.props.CdhProps.registerCdhProps
import ra.tools.raycast
import ra.tools.tool

/**
 * The Creative Data Handler: Shift+RMB a block to see and edit everything on it, read-only fields
 * included. Its buttons run `/function` and `/data` directly, so it is an operator tool.
 */
object CreativeDataHandler {
	val give = tool("creative_data_handler/give")
	val onUse = tool("creative_data_handler/on_use")
	val foundBlock = tool("creative_data_handler/found_block")
	val refresh = tool("creative_data_handler/refresh")
	val updateStorage = tool("creative_data_handler/update_storage")
	val showMenu = tool("creative_data_handler/show_menu")
	val showInternalData = tool("creative_data_handler/show_internal_data")
	val listProperties = tool("creative_data_handler/list_properties")
	val addProperty = tool("creative_data_handler/add_property")
	val removeProperty = tool("creative_data_handler/remove_property")
	val setProperty = tool("creative_data_handler/set_property")
	val setType = tool("creative_data_handler/set_type")

	private const val TARGET = "@e[type=marker,tag=ra.cdh_target,limit=1]"
	private const val PREFIX = """{text:"[Creative Data Handler] ",color:"gold"}"""

	/** list_properties order; `gate` has a row function but was never listed. */
	private val LISTED = listOf(
		"range", "message_block", "tag", "entity_selector", "inverted", "delay", "cooldown", "power", "mode",
		"target", "enabled", "gate_type", "channel", "distance", "extend", "pulse", "chance",
	)

	fun Ra.registerCreativeDataHandler() {
		pack.define(give) {
			raw("give @s ${ToolItems.creativeDataHandler.spec.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Creative Data Handler",color:"aqua"}]""")
		}

		// As the player, from the ra:tools/creative_data_handler_use advancement.
		pack.define(onUse) {
			advancement { revoke(self(), AdvancementArgument("tools/creative_data_handler_use", "ra")) }
			tag(self()) { add("ra.cdh_clicked") }
			raw("execute if entity @s[tag=ra.cdh_active] run return fail")
			tag(self()) { add("ra.cdh_active") }
			raw("execute unless predicate ra:is_sneaking run return 0")
			raw("tag @e[type=marker,tag=ra.cdh_target] remove ra.cdh_target")
			data(Storage.TEMP) { remove("cdh_found") }
			data(Storage.TEMP) { remove("cdh_menu_shown") }
			raycast(REACH_4, "cdh_found", foundBlock)
			raw("""execute unless data storage ra:temp cdh_found run tellraw @s [$PREFIX,{text:"No custom block found nearby.",color:"gray"}]""")
			data(Storage.TEMP) { remove("cdh_found") }
		}

		// A custom block was found. As its marker.
		pack.define(foundBlock) {
			data(Storage.TEMP) { modify("cdh_found", NbtByte(1)) }
			tag(self()) { add("ra.cdh_target") }
			raw("data modify storage ra:cdh target set from entity @s UUID")
			// The name comes from the block's own goggles function (see ra:tools/block_name), not
			// from a table here that could drift from the Data Handler's.
			function(Goggles.blockName)
			raw("data modify storage ra:cdh block_type set from storage ra:temp block_name")
			raw("data modify storage ra:cdh properties set from entity @s data.properties")
			raw("data modify storage ra:cdh internal_data set from entity @s data.data")
			// Show the info to the nearest player who used the tool (only once).
			executeRun("unless data storage ra:temp cdh_menu_shown as @p[distance=..10]", showMenu)
			data(Storage.TEMP) { modify("cdh_menu_shown", NbtByte(1)) }
		}

		pack.define(refresh) {
			executeRun("as $TARGET", updateStorage)
			function(showMenu)
		}
		pack.define(updateStorage) {
			raw("data modify storage ra:cdh block_type set from entity @s data.block_type")
			raw("data modify storage ra:cdh properties set from entity @s data.properties")
			raw("data modify storage ra:cdh internal_data set from entity @s data.data")
		}

		pack.define(showMenu) {
			val edit = """click_event:{action:"suggest_command",command:"/data modify entity $TARGET data.properties set value "}"""
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Creative Data Handler",color:"light_purple",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:cdh",color:"gold"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Properties ───",color:"yellow"},{text:" "},{text:"[+Add]",color:"green",click_event:{action:"suggest_command",command:"/function ${addProperty.asId()} {name:\"\",value:\"\",type:\"\"}"},hover_event:{action:"show_text",value:"Add new property"}},{text:" "},{text:"[Edit All]",color:"aqua",$edit,hover_event:{action:"show_text",value:"Edit all properties at once"}}]""")
			raw("""tellraw @s [{text:"  Properties: ",color:"gray"},{nbt:"properties",storage:"ra:cdh",color:"white",$edit,hover_event:{action:"show_text",value:"Click to copy and edit"}}]""")
			function(listProperties)
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Internal Data ───",color:"aqua"},{text:" "},{text:"[View]",color:"blue",click_event:{action:"run_command",command:"/function ${showInternalData.asId()}"},hover_event:{action:"show_text",value:"View raw internal data"}}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/function ${refresh.asId()}"},hover_event:{action:"show_text",value:"Refresh data view"}}]""")
		}
		pack.define(showInternalData) {
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"═══ ",color:"dark_gray"},{text:"Internal Data",color:"aqua",bold:true},{text:" ═══",color:"dark_gray"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{nbt:"internal_data",storage:"ra:cdh",color:"white"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Back to Menu]",color:"gray",click_event:{action:"run_command",command:"/function ${showMenu.asId()}"},hover_event:{action:"show_text",value:"Return to main menu"}}]""")
		}

		// Every known property the block has, each with its row. As the player.
		pack.define(listProperties) {
			for (name in LISTED) {
				val prop = CdhProps.all.getValue(name)
				// The cooldown row reads the marker itself, so its presence is tested there too.
				val source = if (prop.fromEntity) "entity $TARGET data.properties.$name" else "storage ra:cdh properties.$name"
				executeRun("if data $source", prop.show)
			}
			raw("""execute unless data storage ra:cdh properties run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]""")
		}

		registerManualEdits()
		registerCdhProps(removeProperty)
	}

	/** The `/function ... {name:...}` edits the menu's buttons suggest. As the player. */
	private fun Ra.registerManualEdits() {
		// {name, value, type}
		pack.define(addProperty) {
			raw("\$data modify entity $TARGET data.properties.\$(name) set value \$(value)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Added property ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{text:"$(value)",color:"green"},{text:" (",color:"gray"},{text:"$(type)",color:"aqua"},{text:")",color:"gray"}]""")
			function(refresh)
		}
		// {name}
		pack.define(removeProperty) {
			raw("\$data remove entity $TARGET data.properties.\$(name)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Removed property ",color:"gray"},{text:"$(name)",color:"red"}]""")
			function(refresh)
		}
		// {name}: copy storage ra:cdh set_value into the property. Usage:
		//   /data modify storage ra:cdh set_value set value "Hello friend !"
		//   /function ra:tools/creative_data_handler/set_property {name:"message_block"}
		pack.define(setProperty) {
			raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:cdh set_value")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Set ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{nbt:"set_value",storage:"ra:cdh",color:"green"}]""")
			function(refresh)
		}
		// {name, type}: suggests a command, since converting a type requires knowing the new value.
		// Still prefixed "[Data Handler]", as it always was.
		pack.define(setType) {
			raw("""${'$'}tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"To change type of ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" to ",color:"gray"},{text:"$(type)",color:"aqua"},{text:", use:",color:"gray"}]""")
			val cmd = "/function ${setProperty.asId()} {name:\\\"\$(name)\\\",value:"
			raw("""${'$'}tellraw @s [{text:"$cmd<new_value>}",color:"white",click_event:{action:"suggest_command",command:"$cmd}"}}]""")
		}
	}
}
