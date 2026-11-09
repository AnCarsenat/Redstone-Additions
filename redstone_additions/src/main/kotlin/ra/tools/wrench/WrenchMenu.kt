package ra.tools.wrench

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import net.benwoodworth.knbt.NbtByte
import ra.api.Tools
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.Tag
import ra.core.callWith
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.macroCall
import ra.core.raw
import ra.tools.RAY_TARGET
import ra.tools.tool

/**
 * Shift+RMB: the cycle menu.
 *
 * Nothing block-specific lives here. A block declares what it can cycle (`ra.wrenchCycle`, built
 * into `storage ra:wrench cyclables` by [WrenchRegistry]), properties the player may not change are
 * dropped using the block type's property schema the Data Handler reads (`storage ra:dh types`):
 * read-only ones always, admin-only and hidden ones unless the player holds `ra.admin`. What is
 * left decides the presentation:
 *
 *   0 entries   say it does not cycle
 *   1 entry     do it, no menu -- a menu with one button is a worse button
 *   2 or more   open the menu
 */
object WrenchMenu {
	val cycleBlock = tool("wrench/cycle_block")
	val cycleIndex = tool("wrench/cycle_index")
	val loadForBlock = tool("wrench/load_for_block")
	val loadEntries = tool("wrench/load_entries")
	val loadReadonly = tool("wrench/load_readonly")
	val filterReadonly = tool("wrench/filter_readonly")
	val filterNext = tool("wrench/filter_next")
	val filterTest = tool("wrench/filter_test")
	val openMenu = tool("wrench/open_menu")
	val rememberTarget = tool("wrench/remember_target")
	val menuRow = tool("wrench/menu_row")
	val menuRowNext = tool("wrench/menu_row_next")
	val menuAction = Tools.Wrench.menuAction
	val menuAt = tool("wrench/menu_at")
	val runEntry = tool("wrench/run_entry")
	val runPick = tool("wrench/run_pick")

	fun Ra.registerWrenchMenu() {
		// As the block's marker, positioned on the raycast that found it.
		//
		// The filter runs BEFORE the count. A block whose only cyclable property is one the player
		// may not touch has to read as "does not cycle", not cycle it silently on a plain
		// shift-click.
		//
		// data.type is how both registries are looked up. A data pack cannot ask an entity which of
		// its tags names its kind, so placement writes the kind down.
		pack.define(cycleBlock) {
			data(Storage.TEMP) { modify("wrench_found", NbtByte(1)) }
			raw("""execute unless data entity @s data.type run return run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block predates wrench menus — break and replace it.",color:"gray"}]""")
			function(loadForBlock)
			scoreboard.players.set(literal("#wr.n"), Obj.TEMP, 0)
			raw("execute if data storage ra:wrench list run execute store result score #wr.n ${Obj.TEMP} run data get storage ra:wrench list")
			raw("""execute if score #wr.n ${Obj.TEMP} matches 0 run return run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block doesn't support cycling.",color:"gray"}]""")
			executeReturnRun("if score #wr.n ${Obj.TEMP} matches 1", runEntry, "{i:0}")
			function(openMenu)
		}

		// {i}: rebuild this block's list and run entry i, then redraw the menu. As the marker.
		//
		// The list is rebuilt rather than kept from when the menu was drawn, because menu_row_next
		// consumes it as it goes -- and because rebuilding is what makes the click correct even if
		// the player has opened a different block's menu since. Through load_for_block both times,
		// so the indices match what was on screen.
		pack.define(cycleIndex) {
			function(loadForBlock)
			raw("\$" + macroCall(runEntry, "{i:\$(i)}"))
			function(loadForBlock)
			function(openMenu)
		}

		// One place, because the list has to be built identically when the menu is drawn and when a
		// button is clicked. The button carries a row INDEX, so if the click path rebuilt the list
		// any differently -- unfiltered, say -- index 1 would mean a different property than the one
		// the player read on screen.
		pack.define(loadForBlock) {
			data(Storage.WRENCH) { remove("q") }
			data(Storage.WRENCH) { remove("ro") }
			raw("data modify storage ra:wrench q.type set from entity @s data.type")
			callWith(loadEntries, "storage ra:wrench q")
			callWith(loadReadonly, "storage ra:wrench q")
			function(filterReadonly)
		}
		pack.define(loadEntries) {
			data(Storage.WRENCH) { remove("list") }
			raw("\$data modify storage ra:wrench list set from storage ra:wrench cyclables.\"\$(type)\"")
		}
		// {type}: the block type's schema (name -> declaration).
		pack.define(loadReadonly) {
			raw("\$data modify storage ra:wrench ro set from storage ra:dh types.\"\$(type)\"")
		}

		// Rebuilds the list rather than removing in place: deleting from a list while walking it
		// shifts every index after the one removed, which is the classic way to skip half the
		// entries you meant to check.
		pack.define(filterReadonly) {
			raw("execute unless data storage ra:wrench ro run return 0")
			raw("data modify storage ra:wrench keep set value []")
			function(filterNext)
			raw("data modify storage ra:wrench list set from storage ra:wrench keep")
			data(Storage.WRENCH) { remove("keep") }
		}
		pack.define(filterNext) {
			raw("execute unless data storage ra:wrench list[0] run return 0")
			raw("data modify storage ra:wrench fq set from storage ra:wrench list[0]")
			callWith(filterTest, "storage ra:wrench fq")
			raw("data remove storage ra:wrench list[0]")
			function(filterNext)
		}
		// {prop}: keep this entry unless the wrench's user may not change its property. As the
		// marker, so a dynamic declaration is read off the block itself.
		pack.define(filterTest) {
			raw("data remove storage ra:wrench fs")
			raw("\$data modify storage ra:wrench fs set from storage ra:wrench ro.\$(prop)")
			raw("\$execute if data storage ra:wrench fs.dynamic run data modify storage ra:wrench fs set from entity @s data.property_types.\$(prop)")
			raw("execute store result score #wr.ro ${Obj.TEMP} run data get storage ra:wrench fs.readonly")
			raw("execute if score #wr.ro ${Obj.TEMP} matches 1 run return 0")
			raw("execute if entity @a[tag=ra.wrench_user,tag=ra.admin,limit=1] run return run data modify storage ra:wrench keep append from storage ra:wrench fq")
			raw("execute store result score #wr.ao ${Obj.TEMP} run data get storage ra:wrench fs.admin_only")
			raw("execute if score #wr.ao ${Obj.TEMP} matches 1 run return 0")
			raw("scoreboard players set #wr.vis ${Obj.TEMP} 1")
			raw("execute if data storage ra:wrench fs.visible store result score #wr.vis ${Obj.TEMP} run data get storage ra:wrench fs.visible")
			raw("execute if score #wr.vis ${Obj.TEMP} matches 0 run return 0")
			raw("data modify storage ra:wrench keep append from storage ra:wrench fq")
		}

		registerMenuDrawing()
	}

	private fun Ra.registerMenuDrawing() {
		// As the marker, at the block.
		//
		// The block is remembered on the PLAYER, as three scores, because a chat button is clicked
		// some time after the menu is drawn and the click has no idea what it was aimed at. Scores
		// are the only per-player storage a data pack has, and a position survives the player
		// turning round -- which a re-raycast would not.
		//
		// ra.wrench.sel is on the marker only while the rows are being drawn: the nbt fields in a
		// tellraw are resolved when the message is built, so the tag can come straight back off.
		pack.define(openMenu) {
			// Tagged BEFORE the position is remembered: remember_target looks the marker up by this
			// tag, and running it first found nothing at all.
			tag(self()) { add("ra.wrench.sel") }
			executeRun("as @a[tag=ra.wrench_user,limit=1]", rememberTarget)
			raw("""tellraw @a[tag=ra.wrench_user,limit=1] [{text:""}]""")
			raw("""tellraw @a[tag=ra.wrench_user,limit=1] [{text:"── ",color:"dark_gray"},{nbt:"data.type",entity:"@s",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]""")
			scoreboard.players.set(literal("#wr.i"), Obj.TEMP, 0)
			function(menuRowNext)
			tag(self()) { remove("ra.wrench.sel") }
		}
		pack.define(rememberTarget) {
			for ((i, axis) in listOf("x", "y", "z").withIndex())
				raw("execute store result score @s ra.wr.$axis run data get entity @e[tag=ra.wrench.sel,limit=1] Pos[$i]")
		}

		// {label,prop,i}: one row -- name, current value, and a button that cycles it. The action
		// number carried by the button is the entry's index, so the handler only has to look the
		// same list up again rather than keep any state of its own.
		pack.define(menuRow) {
			raw("""${'$'}tellraw @a[tag=ra.wrench_user,limit=1] [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"data.properties.$(prop)",entity:"@e[tag=ra.wrench.sel,limit=1]",color:"aqua"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ CYCLE ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Step this setting to its next value"},click_event:{action:"run_command",command:"/trigger ra.wrench set $(i)"}}]""")
		}
		pack.define(menuRowNext) {
			raw("execute unless data storage ra:wrench list[0] run return 0")
			raw("data modify storage ra:wrench row set from storage ra:wrench list[0]")
			// The button carries index+1. A trigger score of 0 is indistinguishable from "nobody
			// clicked anything", and row 0 is a real row -- so every action id is shifted by one and
			// menu_action shifts it back.
			raw("scoreboard players operation #wr.btn ${Obj.TEMP} = #wr.i ${Obj.TEMP}")
			scoreboard.players.add(literal("#wr.btn"), Obj.TEMP, 1)
			raw("execute store result storage ra:wrench row.i int 1 run scoreboard players get #wr.btn ${Obj.TEMP}")
			callWith(menuRow, "storage ra:wrench row")
			raw("data remove storage ra:wrench list[0]")
			scoreboard.players.add(literal("#wr.i"), Obj.TEMP, 1)
			function(menuRowNext)
		}

		// A [Cycle] button was clicked. As the player, at the player.
		//
		// The trigger value is the row index. The block is whichever one this player last opened a
		// menu on, remembered as three scores -- see open_menu for why it is not a re-raycast.
		pack.define(menuAction) {
			raw("execute store result score #wr.act ${Obj.TEMP} run scoreboard players get @s ra.wrench")
			scoreboard.players.remove(literal("#wr.act"), Obj.TEMP, 1)
			scoreboard.players.set(self(), "ra.wrench", 0)
			raw("scoreboard players enable @s ra.wrench")
			raw("""execute unless score @s ra.wr.x matches -2147483648.. run return run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"Nothing selected — shift-click a block first.",color:"gray"}]""")
			for (axis in listOf("x", "y", "z"))
				raw("execute store result storage ra:wrench at.$axis int 1 run scoreboard players get @s ra.wr.$axis")
			raw("execute store result storage ra:wrench at.i int 1 run scoreboard players get #wr.act ${Obj.TEMP}")
			tag(self()) { add("ra.wrench_user") }
			callWith(menuAt, "storage ra:wrench at")
			tag(self()) { remove("ra.wrench_user") }
		}

		// {x,y,z,i}: find the remembered block and cycle entry i on it.
		//
		// The marker is searched for at the stored position rather than trusted to still be there:
		// it can have been broken between opening the menu and clicking, and a button that silently
		// did nothing would be worse than one that says so.
		pack.define(menuAt) {
			// x y z are the block's (floored) coordinates: centre on the block before looking, or
			// the marker of the block below is as near as this one's.
			val centre = "positioned \$(x) \$(y) \$(z) align xyz positioned ~0.5 ~0.5 ~0.5"
			raw("""${'$'}execute $centre unless entity $RAY_TARGET run return run tellraw @s [{text:"[Wrench] ",color:"gold"},{text:"That block is gone.",color:"gray"}]""")
			raw("\$execute $centre as $RAY_TARGET at @s run ${macroCall(cycleIndex, "{i:\$(i)}")}")
		}

		// {i}: run cyclable number i. As the marker, at the block.
		//
		// The function to call is a string in the registry, pasted into the command by the macro --
		// which is the whole reason the registry can be data rather than a branch per block.
		pack.define(runEntry) {
			raw("\$data modify storage ra:wrench pick set from storage ra:wrench list[\$(i)]")
			raw("execute unless data storage ra:wrench pick run return 0")
			callWith(runPick, "storage ra:wrench pick")
		}
		pack.define(runPick) { raw("\$function \$(fn)") }
	}
}
