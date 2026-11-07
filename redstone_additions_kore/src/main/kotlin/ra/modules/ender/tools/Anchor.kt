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

/**
 * `ra_ender:tools/anchor/...`: chat commands that name an anchor and fill its target table.
 * Context of each: as a player, acting on the nearest anchor within 6 blocks.
 */
object Anchor {
	private fun fn(path: String) = ref(Ns.ENDER, "tools/anchor/$path")

	private val copyRow = fn("copy_row")
	private val copyRowWrite = fn("copy_row_write")
	private val writeTarget = fn("write_target")

	private const val T = Obj.TEMP

	/** The nearest anchor within 6 blocks. */
	private val NEAREST = "@e[type=marker,tag=${TeleportAnchor.block.tag},distance=..6,limit=1,sort=nearest]"

	private val NO_ANCHOR = "execute unless entity @e[type=marker,tag=${TeleportAnchor.block.tag},distance=..6] run return run " +
		"""tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]"""

	fun Ra.registerAnchorTools() {
		// How to wire an anchor up, printed in chat.
		pack.define(ModulesApi.Ender.anchorHelp) {
			raw("""tellraw @s [{text:"— Teleport Anchor ",color:"light_purple",bold:true},{text:"—",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Every anchor has a string ",color:"gray"},{text:"id",color:"aqua"},{text:" and a table of 15 target ids, one per redstone strength.",color:"gray"}]""")
			raw("""tellraw @s [{text:"/function ra_ender:tools/anchor/set_id {id:\"A\"}",color:"yellow"},{text:" — name the nearest anchor",color:"gray"}]""")
			raw("""tellraw @s [{text:"/function ra_ender:tools/anchor/set_table {table:[\"A\",\"B\",\"C\"]}",color:"yellow"},{text:" — the whole table at once",color:"gray"}]""")
			raw("""tellraw @s [{text:"/function ra_ender:tools/anchor/set_target {level:3,id:\"B\"}",color:"yellow"},{text:" — one row",color:"gray"}]""")
			raw("""tellraw @s [{text:"/function ra_ender:tools/anchor/show",color:"yellow"},{text:" — print the nearest anchor's table",color:"gray"}]""")
			raw("""tellraw @s [{text:"Stand within 2 blocks and power it: strength picks the row.",color:"gray"}]""")
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

		// Print the nearest anchor's id and target table.
		pack.define(fn("show")) {
			raw(NO_ANCHOR)

			raw("data modify storage ra:ender show set value {}")
			raw("data modify storage ra:ender show.id set from entity $NEAREST data.properties.anchor_id")
			raw("data modify storage ra:ender show.targets set from entity $NEAREST data.properties.targets")

			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor id ",color:"gray"},{nbt:"show.id",storage:"ra:ender",color:"aqua"}]""")
			raw("""tellraw @s [{text:"  targets 1-15: ",color:"gray"},{nbt:"show.targets",storage:"ra:ender",color:"white"}]""")
		}
	}
}
