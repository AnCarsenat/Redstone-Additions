package ra.tools.goggles

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.Settings
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.tools.ToolItems
import ra.tools.callWith
import ra.tools.executeRun
import ra.tools.goggles.billboard.registerBillboardLines
import ra.tools.goggles.billboard.registerBillboardRender
import ra.tools.goggles.status.registerGogglesStatus

/**
 * The Goggles: worn or held, they draw a billboard over every custom block and multiblock in range.
 *
 * The goggles READ. They used to also change things -- sneak plus goggles cycled a block's mode and
 * toggled `enabled` -- which meant two tools that both altered blocks, with no rule about which
 * owned what. The Electric Furnace ended up with its output on the wrench and its power mode on the
 * goggles, and its wrench message addressed to a tag only the goggles ever set. Everything you can
 * change is on the wrench now.
 */
object GogglesTool {
	private val SLOTS = listOf("armor.head", "weapon.mainhand", "weapon.offhand")

	fun Ra.registerGoggles() {
		pack.define(GogglesRefs.give) {
			raw("give @s ${ToolItems.goggles.argument.removePrefix("minecraft:")}")
			raw("""tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given ",color:"green"},{text:"Goggles",color:"gold"},{text:"!",color:"green"}]""")
		}

		// Called every tick from ra:tick.
		pack.define(GogglesRefs.tick) {
			for (slot in SLOTS)
				raw("execute as @a at @s if items entity @s $slot *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active")

			// Redraw once a second, by default.
			//
			// This was every 40 ticks, and two seconds is long enough that the numbers on a billboard
			// read as broken rather than as slow -- you change a setting, look at the block, and it
			// still says the old value. The scan itself is one sweep per wearer plus one draw per
			// marker in range, so the cost is small next to what the tick loop already does.
			// The interval is a setting, so the threshold is a score rather than a literal --
			// `matches` cannot take one. Read once per tick, which is cheap beside the sweep it is
			// deciding whether to run.
			scoreboard.players.add(literal("#goggles_timer"), Obj.TEMP, 1)
			executeRun("store result score #goggles_want ${Obj.TEMP}", Settings.get, "{key:\"goggles_redraw\",default:20}")
			raw("execute if score #goggles_want ${Obj.TEMP} matches ..0 run scoreboard players set #goggles_want ${Obj.TEMP} 20")
			raw("execute if score #goggles_timer ${Obj.TEMP} < #goggles_want ${Obj.TEMP} run return 0")
			scoreboard.players.set(literal("#goggles_timer"), Obj.TEMP, 0)

			// Remove old billboards. Item displays as well as text ones since blocks can draw an item
			// into their readout -- see billboard/item_line. A display left behind by this sweep is a
			// display nothing will ever clean up.
			raw("kill @e[type=text_display,tag=ra.billboard]")
			raw("kill @e[type=item_display,tag=ra.billboard]")

			// Collect everything in range of any goggles wearer first, then draw each marker exactly
			// once. Drawing per player meant every billboard was summoned twice when two wearers stood
			// near the same block, and cost one whole-world sweep per block type per player.
			raw("tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen")
			// The range is a setting too, and a selector's distance cannot be a score, so the two
			// scans go through a macro. One instantiation per redraw, not per player.
			executeRun("store result storage ra:temp goggles.range int 1", Settings.get, "{key:\"goggles_range\",default:16}")
			raw("execute if data storage ra:temp goggles{range:0} run data modify storage ra:temp goggles.range set value 16")
			callWith(GogglesRefs.scanRange, "temp", "goggles")

			executeRun("as @e[type=marker,tag=ra.goggles_seen,tag=ra.custom_block] at @s", GogglesRefs.drawBlock)
			executeRun("as @e[type=marker,tag=ra.goggles_seen,tag=ra.multiblock] at @s", GogglesRefs.drawMultiblock)

			raw("tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen")
			// Re-applied next tick if still wearing/holding.
			raw("tag @a remove ra.goggles_active")
		}

		// {range}: tag everything a goggles wearer can see. A selector's `distance` is part of the
		// command text and cannot read a score, which is the only reason these two lines are a macro.
		pack.define(GogglesRefs.scanRange) {
			for (kind in listOf("ra.custom_block", "ra.multiblock"))
				raw("\$execute as @a[tag=ra.goggles_active] at @s run tag @e[type=marker,tag=$kind,distance=..\$(range)] add ra.goggles_seen")
		}

		// Pure routing, built from the registry: what each block shows, and what it is called, is
		// defined by that block in its own goggles function. ra:tools/block_name reuses this same
		// dispatch to resolve just the name. As the block's marker, at the block position.
		pack.define(GogglesRefs.drawBlock) {
			for (route in goggles) executeRun("if entity @s[tag=${route.tag}]", route.fn)
		}

		// One multiblock's billboard, as its marker. goggles/tick selects the markers in range once
		// and calls this per marker, so a multiblock type costs one line here and no extra sweep.
		pack.define(GogglesRefs.drawMultiblock) {
			for (route in multiblockGoggles) executeRun("if entity @s[tag=${route.tag}]", route.fn)
		}

		registerBillboardLines()
		registerBillboardRender()
		registerGogglesStatus()
	}
}
