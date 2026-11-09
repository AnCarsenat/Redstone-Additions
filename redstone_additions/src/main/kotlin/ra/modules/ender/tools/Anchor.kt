package ra.modules.ender.tools

import io.github.ayfri.kore.commands.function
import ra.api.ModulesApi
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.ender.TeleportAnchor
import ra.modules.ender.anchors.AnchorTable
import ra.core.TextFormat
import ra.core.callWith
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom
import ra.core.block.markerSel

/**
 * `ra_ender:tools/anchor/...`: chat commands that name an anchor and fill its target table.
 * Context of each: as a player, acting on the nearest anchor within 6 blocks.
 */
object Anchor {
	private fun fn(path: String) = ref(Ns.ENDER, "tools/anchor/$path")

	private val copyRow = fn("copy_row")
	private val copyRowWrite = fn("copy_row_write")
	private val writeTarget = fn("write_target")
	private val showRow = fn("show_row")
	private val showLine = fn("show_line")
	private val forgetHere = fn("forget_here")
	private val forgetDrop = fn("forget_drop")

	private const val T = Obj.TEMP

	/** The nearest anchor within 6 blocks. */
	private val NEAREST = TeleportAnchor.block.markerSel("distance=..6", "limit=1", "sort=nearest")

	private val NO_ANCHOR = "execute unless entity ${TeleportAnchor.block.markerSel("distance=..6")} run return run " +
		"""tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]"""

	fun Ra.registerAnchorTools() {
		// How to wire an anchor up, printed in chat. The Data Handler first: it needs no operator
		// rights and no typing beyond a name.
		pack.define(ModulesApi.Ender.anchorHelp) {
			raw("""tellraw @s [{text:"— Teleport Anchor ",color:"light_purple",bold:true},{text:"—",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Each anchor has a name (",color:"gray"},{text:"anchor_id",color:"aqua"},{text:") and a destination for each redstone strength 1–15 (",color:"gray"},{text:"targets",color:"aqua"},{text:").",color:"gray"}]""")
			raw("""tellraw @s [{text:"With the ",color:"gray"},{text:"Data Handler",color:"aqua"},{text:", Shift + right-click the anchor:",color:"gray"}]""")
			raw("""tellraw @s [{text:"  • ",color:"dark_gray"},{text:"anchor_id [Modify]",color:"yellow"},{text:" — name it, e.g. Home (one anchor per name; letters, digits and _ . + - : / #)",color:"gray"}]""")
			raw("""tellraw @s [{text:"  • ",color:"dark_gray"},{text:"targets [List]",color:"yellow"},{text:", then ",color:"gray"},{text:"Strength N [Edit]",color:"yellow"},{text:" — type the name of the destination; ",color:"gray"},{text:"[Clear]",color:"yellow"},{text:" empties it",color:"gray"}]""")
			raw("""tellraw @s [{text:"Stand within 2 blocks and power it: the strength picks the destination.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Operators, on the nearest anchor within 6 blocks:",color:"dark_gray"}]""")
			for ((cmd, does) in listOf(
				"set_id {id:\\\"A\\\"}" to "name it",
				"set_target {level:3,id:\\\"B\\\"}" to "one strength",
				"set_table {table:[\\\"A\\\",\\\"B\\\"]}" to "strengths 1, 2… at once",
				"show" to "its destinations",
				"forget {id:\\\"B\\\"}" to "drop a destination whose anchor is gone",
			)) raw("""tellraw @s [{text:"  /function ra_ender:tools/anchor/$cmd",color:"yellow"},{text:" — $does",color:"gray"}]""")
		}

		// Macro {id}: name the nearest anchor: "home", "base", "mine_2". Letters, digits and
		// _ . + - : / # (others are dropped), and not an id another anchor has. The anchor files the
		// new id in the destinations table on its next tick.
		pack.define(fn("set_id")) {
			raw(NO_ANCHOR)

			raw("data remove storage ra:ender in")
			raw("\$data modify storage ra:ender in.id set value \"\$(id)\"")
			cleanFrom("storage ra:ender in.id", TextFormat.ID)
			raw("""execute if score ${SafeClean.OK} $T matches 0 run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"An anchor id takes ${TextFormat.ID.hint}.",color:"red"}]""")
			raw("data modify storage ra:ender in.id set from storage ra:safe out")
			// Taken by another anchor? (Its own current id is no clash.)
			raw("data modify storage ra:ender lookup set value {}")
			raw("data modify storage ra:ender lookup.id set from storage ra:ender in.id")
			raw("execute store result score #ender.found $T run function ${AnchorTable.lookup.asId()}")
			raw("data modify storage ra:ender in.cur set from entity $NEAREST data.properties.anchor_id")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender in.cur set from storage ra:ender in.id")
			raw("""execute if score #ender.found $T matches 1 if score #ender.diff $T matches 1 run tellraw @s [{text:"[RA] ",color:"gold"},{text:"Another anchor already has the id ",color:"red"},{nbt:"in.id",storage:"ra:ender",color:"yellow"},{text:" (at ",color:"red"},{nbt:"lookup.dest.x",storage:"ra:ender",color:"white"},{text:" "},{nbt:"lookup.dest.y",storage:"ra:ender",color:"white"},{text:" "},{nbt:"lookup.dest.z",storage:"ra:ender",color:"white"},{text:"). Nothing was changed.",color:"red"}]""")
			raw("execute if score #ender.found $T matches 1 if score #ender.diff $T matches 1 run return run data remove storage ra:ender in")
			raw("data modify entity $NEAREST data.properties.anchor_id set from storage ra:ender in.id")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor id set to ",color:"gray"},{nbt:"in.id",storage:"ra:ender",color:"aqua"}]""")
			raw("data remove storage ra:ender in")
			raw("data remove storage ra:ender lookup")
		}

		// Macro {id}: take a destination out of the table, for an anchor that is gone without being
		// broken (/kill, a deleted chunk). Refused while the anchor is still there and loaded.
		pack.define(fn("forget")) {
			raw("data remove storage ra:ender lookup")
			raw("\$data modify storage ra:ender lookup.id set value \"\$(id)\"")
			raw("execute store result score #ender.found $T run function ${AnchorTable.lookup.asId()}")
			raw("""execute if score #ender.found $T matches 0 run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No anchor has that id.",color:"gray"}]""")
			raw("scoreboard players set #ender.here $T 0")
			raw("execute unless data storage ra:ender lookup.dest{dim:\"\"} run function ${forgetHere.asId()} with storage ra:ender lookup.dest")
			raw("""execute if score #ender.here $T matches 1 run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"That anchor is still there: break it, or give it another id.",color:"red"}]""")
			callWith(forgetDrop, "storage ra:ender lookup")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Destination ",color:"gray"},{nbt:"lookup.id",storage:"ra:ender",color:"aqua"},{text:" forgotten.",color:"gray"}]""")
			raw("data remove storage ra:ender lookup")
		}
		// {dim, x, y, z}: #ender.here 1 when an anchor marker is at that block (only a loaded one can be seen).
		pack.define(forgetHere) {
			raw("\$execute in \$(dim) positioned \$(x) \$(y) \$(z) if entity ${TeleportAnchor.block.markerSel("distance=..1")} run scoreboard players set #ender.here $T 1")
		}
		// {id}, checked by lookup.
		pack.define(forgetDrop) { raw("\$data remove storage ra:ender ${AnchorTable.TABLE}.\"\$(id)\"") }

		// Macro {table}: replace the whole target table of the nearest anchor.
		//
		// This is the one the Data Handler points at, because a list is not something its
		// per-property rows can edit: you type the list itself. Shorter lists are padded with empty
		// rows, so {table:["A","B"]} means signal 1 -> A, signal 2 -> B and nothing else wired.
		pack.define(fn("set_table")) {
			raw(NO_ANCHOR)

			// Parse what was typed before touching the anchor: a macro that fails to parse leaves the
			// table absent, and wiping first would have destroyed the old table on a typo. Removing it
			// first also stops a stale list from an earlier call being copied.
			raw("data remove storage ra:ender table")
			raw("\$data modify storage ra:ender table set value {rows:\$(table)}")
			raw("""execute unless data storage ra:ender table.rows run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"Could not read that list — try ",color:"red"},{text:"{table:[\"A\",\"B\"]}",color:"yellow"}]""")

			// Now it is safe to clear the fifteen rows and write the typed ones over the front.
			raw("data modify entity $NEAREST data.properties.targets set value ${TeleportAnchor.EMPTY_TABLE}")
			raw("data modify storage ra:ender table.i set value 0")
			function(copyRow)

			raw("data modify storage ra:ender show set value {}")
			raw("data modify storage ra:ender show.targets set from entity $NEAREST data.properties.targets")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Table set to ",color:"gray"},{nbt:"show.targets",storage:"ra:ender",color:"aqua"}]""")
		}

		// Copy typed rows into the anchor's table, one per call. storage ra:ender table = {rows:[...],i:N}.
		pack.define(copyRow) {
			raw("execute unless data storage ra:ender table.rows[0] run return 0")
			raw("execute if data storage ra:ender table{i:15} run return 0")

			function(copyRowWrite, Storage.ENDER, "table")

			raw("data remove storage ra:ender table.rows[0]")
			raw("execute store result score #ender.i $T run data get storage ra:ender table.i")
			raw("scoreboard players add #ender.i $T 1")
			raw("execute store result storage ra:ender table.i int 1 run scoreboard players get #ender.i $T")

			function(copyRow)
		}

		// Macro {i}: write the row at the front of the typed list into slot i.
		pack.define(copyRowWrite) {
			raw("\$data modify entity $NEAREST data.properties.targets[\$(i)] set from storage ra:ender table.rows[0]")
		}

		// Macro {level, id}: point one row of the nearest anchor's table at an id. level is the
		// redstone strength, 1-15. An empty id clears the row.
		pack.define(fn("set_target")) {
			raw("\$scoreboard players set #ender.level $T \$(level)")
			raw("""execute unless score #ender.level $T matches 1..15 run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"Level must be 1-15",color:"red"}]""")
			raw(NO_ANCHOR)

			// The table index is one less than the signal.
			raw("scoreboard players remove #ender.level $T 1")
			raw("execute store result storage ra:ender set.i int 1 run scoreboard players get #ender.level $T")
			raw("\$data modify storage ra:ender set.id set value \"\$(id)\"")
			// In format ID, like every id; nothing usable left clears the row.
			cleanFrom("storage ra:ender set.id", TextFormat.ID)
			raw("data modify storage ra:ender set.id set value \"\"")
			raw("execute if score ${SafeClean.OK} $T matches 1 run data modify storage ra:ender set.id set from storage ra:safe out")

			function(writeTarget, Storage.ENDER, "set")
			raw("""${'$'}tellraw @s [{text:"[RA] ",color:"gold"},{text:"Signal ",color:"gray"},{text:"${'$'}(level)",color:"yellow"},{text:" now sends to id ",color:"gray"},{nbt:"set.id",storage:"ra:ender",color:"aqua"}]""")
		}

		// Macro {i}: the dynamic-index half of set_target, writing storage ra:ender set.id.
		pack.define(writeTarget) {
			raw("\$data modify entity $NEAREST data.properties.targets[\$(i)] set from storage ra:ender set.id")
		}

		// Print the nearest anchor's id and, one line each, the strengths that lead somewhere.
		pack.define(fn("show")) {
			raw(NO_ANCHOR)

			raw("data modify storage ra:ender show set value {}")
			raw("data modify storage ra:ender show.id set from entity $NEAREST data.properties.anchor_id")
			raw("data modify storage ra:ender show.targets set from entity $NEAREST data.properties.targets")

			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor ",color:"gray"},{nbt:"show.id",storage:"ra:ender",color:"aqua"}]""")
			raw("scoreboard players set #ender.lv $T 1")
			function(showRow)
			raw("""execute if score #ender.shown $T matches 0 run tellraw @s [{text:"  No destination set.",color:"gray",italic:true}]""")
			raw("data remove storage ra:ender show")
		}
		pack.define(showRow) {
			raw("execute if score #ender.lv $T matches 1 run scoreboard players set #ender.shown $T 0")
			raw("execute unless data storage ra:ender show.targets[0] run return 0")
			function(showLine)
			raw("data remove storage ra:ender show.targets[0]")
			raw("scoreboard players add #ender.lv $T 1")
			function(showRow)
		}
		// Only for a non-empty first entry: "" is "nothing on this strength".
		pack.define(showLine) {
			raw("data modify storage ra:ender show.first set from storage ra:ender show.targets[0]")
			raw("""execute if data storage ra:ender show{first:""} run return 0""")
			raw("scoreboard players add #ender.shown $T 1")
			raw("""tellraw @s [{text:"  Strength ",color:"gray"},{score:{name:"#ender.lv",objective:"$T"},color:"yellow"},{text:" → ",color:"dark_gray"},{nbt:"show.first",storage:"ra:ender",color:"aqua"}]""")
		}
	}
}
