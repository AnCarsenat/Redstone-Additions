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
			raw("""tellraw @s [{text:"  • ",color:"dark_gray"},{text:"anchor_id [Modify]",color:"yellow"},{text:" — name it, e.g. Home",color:"gray"}]""")
			raw("""tellraw @s [{text:"  • ",color:"dark_gray"},{text:"Strength N [Choose]",color:"yellow"},{text:" — pick the destination from the anchors already named; ",color:"gray"},{text:"[Edit]",color:"yellow"},{text:" types one, ",color:"gray"},{text:"[Clear]",color:"yellow"},{text:" empties it",color:"gray"}]""")
			raw("""tellraw @s [{text:"Stand within 2 blocks and power it: the strength picks the destination.",color:"gray"}]""")
			raw("""tellraw @s [{text:"Operators, on the nearest anchor within 6 blocks:",color:"dark_gray"}]""")
			for ((cmd, does) in listOf(
				"set_id {id:\\\"A\\\"}" to "name it",
				"set_target {level:3,id:\\\"B\\\"}" to "one strength",
				"set_table {table:[\\\"A\\\",\\\"B\\\"]}" to "strengths 1, 2… at once",
				"show" to "its destinations",
				"list" to "every known anchor name",
			)) raw("""tellraw @s [{text:"  /function ra_ender:tools/anchor/$cmd",color:"yellow"},{text:" — $does",color:"gray"}]""")
		}

		// Every anchor id filed so far (storage ra:ender anchor_ids), the Data Handler's pick list.
		pack.define(fn("list")) {
			raw("""execute unless data storage ra:ender ${TeleportAnchor.REGISTRY}[0] run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No anchor has been named yet.",color:"gray"}]""")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Known anchors: ",color:"gray"},{nbt:"${TeleportAnchor.REGISTRY}[]",storage:"ra:ender",color:"aqua",separator:{text:", ",color:"gray"}}]""")
		}

		// Macro {id}: name the nearest anchor. Ids are strings: "A", "base", "mine_2". Anything you can type.
		pack.define(fn("set_id")) {
			raw(NO_ANCHOR)

			raw("\$data modify entity $NEAREST data.properties.anchor_id set value \"\$(id)\"")
			raw("""${'$'}tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor id set to ",color:"gray"},{text:"${'$'}(id)",color:"aqua"}]""")
		}

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

			function(writeTarget, Storage.ENDER, "set")
			raw("""${'$'}tellraw @s [{text:"[RA] ",color:"gold"},{text:"Signal ",color:"gray"},{text:"${'$'}(level)",color:"yellow"},{text:" now sends to id ",color:"gray"},{text:"${'$'}(id)",color:"aqua"}]""")
		}

		// Macro {i, id}: the dynamic-index half of set_target.
		pack.define(writeTarget) {
			raw("\$data modify entity $NEAREST data.properties.targets[\$(i)] set value \"\$(id)\"")
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
