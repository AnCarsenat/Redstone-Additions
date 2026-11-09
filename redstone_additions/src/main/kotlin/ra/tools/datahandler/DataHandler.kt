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
import ra.tools.ToolItems
import ra.tools.datahandler.DataHandlerActions.registerDataHandlerActions
import ra.tools.datahandler.DataHandlerSelection.claim
import ra.core.callWith
import ra.tools.datahandler.DataHandlerSelection.registerDataHandlerSelection
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
	val loadSchema = tool("data_handler/load_schema")
	private val fillDefaults = tool("data_handler/fill_defaults")
	private val loadSchemaAt = tool("data_handler/load_schema_at")
	val addProperty = tool("data_handler/add_property")
	val removeProperty = tool("data_handler/remove_property")
	val setProperty = tool("data_handler/set_property")
	val setType = tool("data_handler/set_type")

	private const val TARGET = DataHandlerSelection.TARGET
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
			// Forget this player's previous block. Only theirs: other players keep their own selection.
			scoreboard.players.set(self(), DataHandlerSelection.SEL, 0)
			// Opened fresh from the tool: every list compact again.
			raw("scoreboard players reset @s ${DataHandlerRegistry.EXPAND}")
			data(Storage.TEMP) { remove("dh_found") }
			raycast("dh_found", foundBlock)
			raw("execute if data storage ra:temp dh_found run scoreboard players operation @s ${DataHandlerSelection.SEL} = #dh.found ${Obj.TEMP}")
			function(DataHandlerSelection.select)
			executeRun("if data storage ra:temp dh_found", refresh)
			raw("""execute unless data storage ra:temp dh_found run tellraw @s [$PREFIX,{text:"No custom block found nearby.",color:"gray"}]""")
			data(Storage.TEMP) { remove("dh_found") }
		}

		// A custom block was found. As its marker, at the marker.
		pack.define(foundBlock) {
			data(Storage.TEMP) { modify("dh_found", NbtByte(1)) }
			// A number of its own the first time anybody selects it, so a player can point at it.
			claim()
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
		// As the target marker. The name is resolved again rather than read from data.block_type,
		// which markers do not carry: that read failed, so the menu kept whichever block name the
		// last player to select anything had left in the shared storage.
		pack.define(updateStorage) {
			function(Goggles.blockName)
			raw("data modify storage ra:dh block_type set from storage ra:temp block_name")
			// Cleared first: a block with no properties must not show the previous block's.
			raw("data remove storage ra:dh properties")
			raw("data remove storage ra:dh internal_data")
			raw("data modify storage ra:dh properties set from entity @s data.properties")
			raw("data modify storage ra:dh internal_data set from entity @s data.data")
			function(loadSchema)
			// stored: what the block really carries. properties: that, over the schema's defaults,
			// so a declared property the block does not have yet (placed before it existed) still
			// gets its row, showing the default; its first edit writes it (DataHandlerProps.materialize).
			raw("data remove storage ra:dh stored")
			raw("data modify storage ra:dh stored set from storage ra:dh properties")
			executeRun("if data storage ra:dh bt.type", fillDefaults, "with storage ra:dh bt")
		}
		// {type}
		pack.define(fillDefaults) {
			raw("data remove storage ra:dh df")
			raw("\$data modify storage ra:dh df set from storage ra:dh defaults.\"\$(type)\"")
			raw("execute unless data storage ra:dh df run return 0")
			// Something other than a compound (written with a command): left as it is.
			raw("execute if data storage ra:dh stored unless data storage ra:dh stored{} run return run data remove storage ra:dh df")
			raw("data modify storage ra:dh df merge from storage ra:dh stored")
			raw("data modify storage ra:dh properties set from storage ra:dh df")
			raw("data remove storage ra:dh df")
		}

		// As a block's marker: its type's property schema into storage ra:dh -- schema (name ->
		// declaration), block_order (menu order) -- and the instance's own declarations for dynamic
		// properties (inst_types, from data.property_types). Shared with the Creative Data Handler.
		pack.define(loadSchema) {
			for (k in listOf("schema", "block_order", "inst_types", "bt")) raw("data remove storage ra:dh $k")
			raw("data modify storage ra:dh inst_types set from entity @s data.property_types")
			raw("data modify storage ra:dh bt.type set from entity @s data.type")
			executeRun("if data storage ra:dh bt.type", loadSchemaAt, "with storage ra:dh bt")
		}
		// {type}
		pack.define(loadSchemaAt) {
			raw("\$data modify storage ra:dh schema set from storage ra:dh types.\"\$(type)\"")
			raw("\$data modify storage ra:dh block_order set from storage ra:dh order.\"\$(type)\"")
		}

		// As the player. Every drawing gets a new stamp, which its row buttons carry: see
		// DataHandlerRegistry for why a click from an older menu is refused.
		pack.define(showMenu) {
			val mid = DataHandlerSelection.MID
			raw("scoreboard players add #dh.stamp $mid 1")
			raw("execute unless score #dh.stamp $mid matches 1..${DataHandlerRegistry.STAMP_MAX} run scoreboard players set #dh.stamp $mid 1")
			raw("scoreboard players operation @s ${DataHandlerRegistry.MENU} = #dh.stamp $mid")
			raw("scoreboard players set @s ${DataHandlerRegistry.KIND} 0")
			function(ra.tools.datahandler.props.DataHandlerEntries.dropPick)
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Data Handler",color:"aqua",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Block: ",color:"gray"},{nbt:"block_type",storage:"ra:dh",color:"gold"}]""")
			raw("""tellraw @s [{text:"Shift+RMB with this tool to rescan a block.",color:"gray"}]""")
			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"─── Properties ───",color:"yellow"}]""")
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
		// First the block type's declared properties, in declaration order, each with the editor
		// its declared type gets. Then any property the block carries without declaring it: a name
		// some other block declares gets a row with a guessed type and a warning; anything left
		// after that is listed, with no editor.
		pack.define(listProperties) {
			raw("""execute unless data storage ra:dh properties unless data storage ra:dh block_order[0] run return run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]""")
			// rest: what no row has claimed yet. Each row removes its own name.
			raw("data modify storage ra:dh rest set from storage ra:dh properties")
			// Row actions: this menu's stamp * MUL + 100 + index (see DataHandlerActions).
			raw("scoreboard players set #dh.k1000 ${Obj.TEMP} ${DataHandlerRegistry.MUL}")
			raw("scoreboard players operation #dh.base ${Obj.TEMP} = @s ${DataHandlerRegistry.MENU}")
			raw("scoreboard players operation #dh.base ${Obj.TEMP} *= #dh.k1000 ${Obj.TEMP}")
			// A working copy is consumed from the front, since a list cannot be indexed by a score
			// without a macro per read.
			raw("data modify storage ra:dh iter set from storage ra:dh block_order")
			raw("scoreboard players operation #dh.act ${Obj.TEMP} = #dh.base ${Obj.TEMP}")
			scoreboard.players.add(literal("#dh.act"), Obj.TEMP, 100)
			function(DataHandlerProps.next)
			raw("data modify storage ra:dh iter set from storage ra:dh known")
			raw("scoreboard players operation #dh.act ${Obj.TEMP} = #dh.base ${Obj.TEMP}")
			scoreboard.players.add(literal("#dh.act"), Obj.TEMP, 700)
			function(DataHandlerProps.nextGuess)
			// Every declared name -- hidden ones included -- was taken out of rest by its own row, so
			// what is left was declared by nobody.
			raw("execute store result score #dh.left ${Obj.TEMP} run data get storage ra:dh rest")
			raw("""execute if score #dh.left ${Obj.TEMP} matches 1.. run tellraw @s [{text:"  "},{text:"⚠ ",color:"yellow",hover_event:{action:"show_text",value:"No block declares these, so there is no editor for them. Set them with the Creative Data Handler."}},{text:"Undeclared: ",color:"gray"},{nbt:"rest",storage:"ra:dh",color:"white"}]""")
			raw("data remove storage ra:dh rest")
			raw("data remove storage ra:dh iter")
		}

		registerDataHandlerSelection()
		registerManualEdits()
		registerDataHandlerActions()
		registerDataHandlerProps()
	}

	/**
	 * Chat-command editing, kept for players and scripts that call it (`/function ... {name:...}`).
	 * Each acts on the calling player's own selected block.
	 */
	private fun Ra.registerManualEdits() {
		// {name, value, type}: add a property to the target block. As the player.
		pack.define(addProperty) {
			function(DataHandlerSelection.select)
			raw("\$data modify entity $TARGET data.properties.\$(name) set value \$(value)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Added property ",color:"gray"},{text:"$(name)",color:"yellow"},{text:" = ",color:"gray"},{text:"$(value)",color:"green"},{text:" (",color:"gray"},{text:"$(type)",color:"aqua"},{text:")",color:"gray"}]""")
			function(refresh)
		}
		// {name}
		pack.define(removeProperty) {
			function(DataHandlerSelection.select)
			raw("\$data remove entity $TARGET data.properties.\$(name)")
			raw("""${'$'}tellraw @s [$PREFIX,{text:"Removed property ",color:"gray"},{text:"$(name)",color:"red"}]""")
			function(refresh)
		}
		// {name}: copy storage ra:dh set_value into the property. Usage:
		//   /data modify storage ra:dh set_value set value "Hello friend !"
		//   /function ra:tools/data_handler/set_property {name:"message_block"}
		pack.define(setProperty) {
			function(DataHandlerSelection.select)
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
