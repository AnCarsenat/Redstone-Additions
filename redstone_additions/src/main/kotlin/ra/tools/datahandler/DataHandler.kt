package ra.tools.datahandler

import io.github.ayfri.kore.arguments.types.literals.literal
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
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.tools.REACH_4
import ra.tools.ToolItems
import ra.tools.datahandler.DataHandlerActions.registerDataHandlerActions
import ra.tools.datahandler.props.DataHandlerProps
import ra.tools.datahandler.props.DataHandlerProps.registerDataHandlerProps
import ra.tools.raycast
import ra.tools.tool

/**
 * The Data Handler: Shift+RMB a block to list its properties, each with an editor for its type.
 * Every button is a `/trigger ra.dh.action`, so it works without operator rights.
 */
object DataHandler {
	val give = tool("data_handler/give")
	val onUse = tool("data_handler/on_use")
	val foundBlock = tool("data_handler/found_block")
	val refresh = tool("data_handler/refresh")
	val updateStorage = tool("data_handler/update_storage")
	val showMenu = tool("data_handler/show_menu")
	val showInternalData = tool("data_handler/show_internal_data")
	val listProperties = tool("data_handler/list_properties")
	val collectHidden = tool("data_handler/collect_hidden")
	val collectHiddenAt = tool("data_handler/collect_hidden_at")
	val addProperty = tool("data_handler/add_property")
	val removeProperty = tool("data_handler/remove_property")
	val setProperty = tool("data_handler/set_property")
	val setType = tool("data_handler/set_type")

	private const val TARGET = "@e[type=marker,tag=ra.dh_target,limit=1]"
	private const val PREFIX = """{text:"[Data Handler] ",color:"gold"}"""

	fun Ra.registerDataHandler() {
		pack.define(give) {
			raw("give @s ${ToolItems.dataHandler.spec.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Data Handler",color:"aqua"}]""")
		}

		// As the player, from the ra:tools/data_handler_use advancement.
		pack.define(onUse) {
			advancement { revoke(self(), AdvancementArgument("tools/data_handler_use", "ra")) }
			// Mark that player clicked this tick (for tag cleanup in ra:tick).
			tag(self()) { add("ra.dh_clicked") }
			// If player is already holding (has tag), skip execution.
			raw("execute if entity @s[tag=ra.dh_active] run return fail")
			tag(self()) { add("ra.dh_active") }
			// Only works when sneaking.
			raw("execute unless predicate ra:is_sneaking run return 0")
			// Remove the target tag from any previously selected block.
			raw("tag @e[type=marker,tag=ra.dh_target] remove ra.dh_target")
			data(Storage.TEMP) { remove("dh_found") }
			raycast(REACH_4, "dh_found", foundBlock)
			executeRun("if data storage ra:temp dh_found", refresh)
			raw("""execute unless data storage ra:temp dh_found run tellraw @s [$PREFIX,{text:"No custom block found nearby.",color:"gray"}]""")
			data(Storage.TEMP) { remove("dh_found") }
		}

		// A custom block was found. As its marker, at the marker.
		pack.define(foundBlock) {
			data(Storage.TEMP) { modify("dh_found", NbtByte(1)) }
			tag(self()) { add("ra.dh_target") }
			raw("data modify storage ra:dh target set from entity @s UUID")
			// Resolve the block's display name. Each block declares its own name in its goggles
			// function; this used to be a hand-written tag-to-name table duplicated between the two
			// Data Handlers, and blocks missing from it -- every RA Wires block here -- showed as
			// "Unknown Block" while their properties were read out correctly, which is what made it
			// look as though unknown blocks had an `enabled` property of their own.
			function(Goggles.blockName)
			raw("data modify storage ra:dh block_type set from storage ra:temp block_name")
			// Clock compatibility runs in ra_gates:blocks/clock/tick, which folds the old property
			// name into the current one every tick. Nothing is needed here -- this used to migrate
			// it the other way, and only when the target was missing, which left clocks carrying both.
			raw("data modify storage ra:dh properties set from entity @s data.properties")
			raw("data modify storage ra:dh internal_data set from entity @s data.data")
		}

		// Re-read the target block and show the menu again. As the player.
		pack.define(refresh) {
			executeRun("as $TARGET", updateStorage)
			function(showMenu)
		}
		// As the target marker.
		pack.define(updateStorage) {
			raw("data modify storage ra:dh block_type set from entity @s data.block_type")
			raw("data modify storage ra:dh properties set from entity @s data.properties")
			raw("data modify storage ra:dh internal_data set from entity @s data.data")
		}

		pack.define(showMenu) {
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Data Handler",color:"aqua",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:dh",color:"gold"}]""")
			raw("""tellraw @s [{text:"Shift+RMB with this tool to rescan a block.",color:"gray"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Properties ───",color:"yellow"}]""")
			function(collectHidden)
			function(DataHandlerProps.redact)
			raw("""tellraw @s [{text:"  Properties: ",color:"gray"},{nbt:"display_props",storage:"ra:dh",color:"white"}]""")
			function(listProperties)
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Internal Data ───",color:"aqua"},{text:" "},{text:"[View]",color:"blue",click_event:{action:"run_command",command:"/trigger ra.dh.action set 90"},hover_event:{action:"show_text",value:"View raw internal data"}}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Refresh]",color:"gray",click_event:{action:"run_command",command:"/trigger ra.dh.action set 91"},hover_event:{action:"show_text",value:"Refresh data view"}},{text:"  "},{text:"[Cancel Input]",color:"red",click_event:{action:"run_command",command:"/trigger ra.dh.action set 93"},hover_event:{action:"show_text",value:"Cancel current property edit"}}]""")
		}
		pack.define(showInternalData) {
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"═══ ",color:"dark_gray"},{text:"Internal Data",color:"aqua",bold:true},{text:" ═══",color:"dark_gray"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{nbt:"internal_data",storage:"ra:dh",color:"white"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"[Back to Menu]",color:"gray",click_event:{action:"run_command",command:"/trigger ra.dh.action set 92"},hover_event:{action:"show_text",value:"Return to main menu"}}]""")
		}

		// Every property of the selected block, with an editor for its type. As the player.
		//
		// This used to be one hand-written function per property name, which is why a block could
		// show a property the Handler had no way to change -- a wire's transfer_rate, an anchor's id.
		// Now it walks the registry, skips the names this block does not have, and picks the editor
		// from the value's actual type.
		pack.define(listProperties) {
			raw("""execute unless data storage ra:dh properties run return run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]""")
			// A working copy is consumed from the front, since a list cannot be indexed by a score
			// without a macro per read.
			raw("data modify storage ra:dh iter set from storage ra:dh registry")
			scoreboard.players.set(literal("#dh.act"), Obj.TEMP, 100)
			scoreboard.players.set(literal("#dh.shown"), Obj.TEMP, 0)
			// How many fields this player may see at all, so anything the registry does not know
			// about can be reported rather than silently dropped.
			scoreboard.players.set(literal("#dh.total"), Obj.TEMP, 0)
			raw("execute store result score #dh.total ${Obj.TEMP} run data get storage ra:dh display_props")
			function(DataHandlerProps.next)
			raw("scoreboard players operation #dh.total ${Obj.TEMP} -= #dh.shown ${Obj.TEMP}")
			raw("""execute if score #dh.total ${Obj.TEMP} matches 1.. run tellraw @s [{text:"  ",color:"dark_gray"},{score:{name:"#dh.total",objective:"${Obj.TEMP}"},color:"dark_gray"},{text:" field(s) above have no editor yet — they are listed in Properties and can be set with /data",color:"dark_gray",italic:true}]""")
			// Nothing is withheld any more; a locked field shows its value and a dead button.
		}

		// Which of this block's properties are read-only: fills storage ra:dh hidden, name -> 1b.
		//
		// One lookup keyed by the block's type, from ra:tools/readonly/init_registry. It used to fan
		// out through a #ra:hidden_fields function tag into a per-module chain of tag tests, then walk
		// the resulting list into a compound one name at a time. Storing the registry as compounds in
		// the first place removes both halves.
		pack.define(collectHidden) {
			raw("data modify storage ra:dh hidden set value {}")
			data(Storage.DH) { remove("ro_q") }
			raw("execute as $TARGET run data modify storage ra:dh ro_q.type set from entity @s data.type")
			executeRun("if data storage ra:dh ro_q.type", collectHiddenAt, "with storage ra:dh ro_q")
		}
		// {type}: copy this block type's read-only set, if it declares one.
		pack.define(collectHiddenAt) {
			raw("\$data modify storage ra:dh hidden set from storage ra:dh readonly.\"\$(type)\"")
		}

		registerManualEdits()
		registerDataHandlerActions()
		registerDataHandlerProps()
	}

	/** Chat-command editing, kept for players and scripts that call it (`/function ... {name:...}`). */
	private fun Ra.registerManualEdits() {
		// {name, value, type}: add a property to the target block. As the player.
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
		// {name}: copy storage ra:dh set_value into the property. Usage:
		//   /data modify storage ra:dh set_value set value "Hello friend !"
		//   /function ra:tools/data_handler/set_property {name:"message_block"}
		pack.define(setProperty) {
			raw("\$data modify entity $TARGET data.properties.\$(name) set from storage ra:dh set_value")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Set ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{nbt:"set_value",storage:"ra:dh",color:"green"}]""")
			function(refresh)
		}
		// {name, type}: suggests a command, since converting a type requires knowing the new value.
		pack.define(setType) {
			raw("""${'$'}tellraw @s [$PREFIX,{text:"To change type of ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" to ",color:"gray"},{text:"$(type)",color:"aqua"},{text:", use:",color:"gray"}]""")
			val cmd = "/function ${setProperty.asId()} {name:\\\"\$(name)\\\",value:"
			raw("""${'$'}tellraw @s [{text:"$cmd<new_value>}",color:"white",click_event:{action:"suggest_command",command:"$cmd}"}}]""")
		}
	}
}
