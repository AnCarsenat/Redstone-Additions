package ra.tools.creativedatahandler

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import net.benwoodworth.knbt.NbtByte
import ra.api.Goggles
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.callWith
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.REACH_4
import ra.tools.ToolItems
import ra.tools.creativedatahandler.props.CdhProps
import ra.tools.creativedatahandler.props.CdhProps.registerCdhProps
import ra.tools.datahandler.DataHandlerSelection
import ra.tools.datahandler.DataHandlerSelection.claim
import ra.tools.raycast
import ra.tools.tool

/**
 * The Creative Data Handler: Shift+RMB a block to see and edit everything on it, read-only fields
 * included. Its buttons run `/function` and `/data` directly, so it is an operator tool.
 *
 * PER PLAYER
 * It used to tag the block it found `ra.cdh_target` and act on `@e[tag=ra.cdh_target,limit=1]`,
 * one tag for the whole server: a second operator's Shift+RMB moved the tag, and the first one's
 * [Modify], [-] and [Refresh] then worked on the second one's block. It also showed the menu to
 * the player nearest the BLOCK, not to the one who clicked. It now shares the Data Handler's
 * selection (DataHandlerSelection): the player's own block in `ra.dh.sel`, tagged
 * `ra.dh_selected` by `select` at the start of every function a button runs, and the suggested
 * `/data` commands name the block by its number, so they still hit it when typed later.
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

	private const val PREFIX = """{text:"[Creative Data Handler] ",color:"gold"}"""
	private const val TARGET = DataHandlerSelection.TARGET
	private val BY_MID = DataHandlerSelection.byMid("\$(mid)")
	private const val NO_TARGET = """execute unless entity $TARGET run return run tellraw @s [$PREFIX,{text:"No selected block. Shift+RMB one first.",color:"red"}]"""

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
			// Forget this player's previous block, and only theirs.
			scoreboard.players.set(self(), DataHandlerSelection.SEL, 0)
			data(Storage.TEMP) { remove("cdh_found") }
			raycast(REACH_4, "cdh_found", foundBlock)
			raw("execute if data storage ra:temp cdh_found run scoreboard players operation @s ${DataHandlerSelection.SEL} = #dh.found ${Obj.TEMP}")
			executeRun("if data storage ra:temp cdh_found", refresh)
			raw("""execute unless data storage ra:temp cdh_found run tellraw @s [$PREFIX,{text:"No custom block found nearby.",color:"gray"}]""")
			data(Storage.TEMP) { remove("cdh_found") }
		}

		// A custom block was found. As its marker.
		pack.define(foundBlock) {
			data(Storage.TEMP) { modify("cdh_found", NbtByte(1)) }
			claim()
		}

		// As the player: re-read their block and show the menu. Every button ends here.
		pack.define(refresh) {
			function(DataHandlerSelection.select)
			raw(NO_TARGET)
			executeRun("as $TARGET", updateStorage)
			raw("data modify storage ra:cdh m set value {}")
			raw("execute store result storage ra:cdh m.mid int 1 run scoreboard players get @s ${DataHandlerSelection.SEL}")
			callWith(showMenu, "storage ra:cdh m")
		}
		// As the marker. The name comes from the block's own goggles function (see
		// ra:tools/block_name), not from a table here that could drift from the Data Handler's.
		pack.define(updateStorage) {
			function(Goggles.blockName)
			raw("data modify storage ra:cdh block_type set from storage ra:temp block_name")
			raw("data remove storage ra:cdh properties")
			raw("data remove storage ra:cdh internal_data")
			raw("data modify storage ra:cdh properties set from entity @s data.properties")
			raw("data modify storage ra:cdh internal_data set from entity @s data.data")
		}

		// {mid}: the player's block number, for the suggested commands. Called by refresh.
		pack.define(showMenu) {
			val edit = """click_event:{action:"suggest_command",command:"/data modify entity $BY_MID data.properties set value "}"""
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Creative Data Handler",color:"light_purple",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:cdh",color:"gold"}]""")
			raw("tellraw @s \"\"")
			raw("""${'$'}tellraw @s [{text:"─── Properties ───",color:"yellow"},{text:" "},{text:"[+Add]",color:"green",click_event:{action:"suggest_command",command:"/function ${addProperty.asId()} {name:\"\",value:\"\",type:\"\"}"},hover_event:{action:"show_text",value:"Add new property"}},{text:" "},{text:"[Edit All]",color:"aqua",$edit,hover_event:{action:"show_text",value:"Edit all properties at once"}}]""")
			raw("""${'$'}tellraw @s [{text:"  Properties: ",color:"gray"},{nbt:"properties",storage:"ra:cdh",color:"white",$edit,hover_event:{action:"show_text",value:"Click to copy and edit"}}]""")
			callWith(listProperties, "storage ra:cdh m")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Internal Data ───",color:"aqua"},{text:" "},{text:"[View]",color:"blue",click_event:{action:"run_command",command:"/function ${showInternalData.asId()}"},hover_event:{action:"show_text",value:"View raw internal data"}}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/function ${refresh.asId()}"},hover_event:{action:"show_text",value:"Refresh data view"}}]""")
		}
		pack.define(showInternalData) {
			function(DataHandlerSelection.select)
			raw(NO_TARGET)
			executeRun("as $TARGET", updateStorage)
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"═══ ",color:"dark_gray"},{text:"Internal Data",color:"aqua",bold:true},{text:" ═══",color:"dark_gray"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{nbt:"internal_data",storage:"ra:cdh",color:"white"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Back to Menu]",color:"gray",click_event:{action:"run_command",command:"/function ${refresh.asId()}"},hover_event:{action:"show_text",value:"Return to main menu"}}]""")
		}

		// {mid}: every known property the block has, each with its row. As the player.
		pack.define(listProperties) {
			for (name in LISTED) {
				val prop = CdhProps.all.getValue(name)
				// The cooldown row reads the marker itself, so its presence is tested there too.
				val source = if (prop.fromEntity) "entity $TARGET data.properties.$name" else "storage ra:cdh properties.$name"
				// mid is passed on: every row names the block by it.
				raw("\$execute if data $source run function ${prop.show.asId()} {mid:\$(mid)}")
			}
			raw("""execute unless data storage ra:cdh properties run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]""")
		}

		registerManualEdits()
		registerCdhProps(removeProperty)
	}

	/** The `/function ... {name:...}` edits the menu's buttons suggest. As the player, on their own block. */
	private fun Ra.registerManualEdits() {
		// {name, value, type}
		pack.define(addProperty) {
			function(DataHandlerSelection.select)
			raw(NO_TARGET)
			raw("\$data modify entity $TARGET data.properties.\$(name) set value \$(value)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Added property ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{text:"$(value)",color:"green"},{text:" (",color:"gray"},{text:"$(type)",color:"aqua"},{text:")",color:"gray"}]""")
			function(refresh)
		}
		// {name}
		pack.define(removeProperty) {
			function(DataHandlerSelection.select)
			raw(NO_TARGET)
			raw("\$data remove entity $TARGET data.properties.\$(name)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Removed property ",color:"gray"},{text:"$(name)",color:"red"}]""")
			function(refresh)
		}
		// {name}: copy storage ra:cdh set_value into the property. Usage:
		//   /data modify storage ra:cdh set_value set value "Hello friend !"
		//   /function ra:tools/creative_data_handler/set_property {name:"message_block"}
		pack.define(setProperty) {
			function(DataHandlerSelection.select)
			raw(NO_TARGET)
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
