package ra.modules.wires.blocks.creative_eu

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line
import ra.modules.wires.electric.EuApi

object CreativeEu : PackPart {
	val give = ref(Ns.WIRES, "blocks/creative_eu/give")
	val goggles = ref(Ns.WIRES, "blocks/creative_eu/goggles")
	val tick = ref(Ns.WIRES, "blocks/creative_eu/tick")

	override fun Ra.register() {
		pack.define(CreativeEu.give) {
			// Give the Creative EU Source. Creative only -- there is no recipe for it, on purpose.

			raw("""give @s bat_spawn_egg[item_model="minecraft:beacon",item_name="Creative EU Source",rarity=epic,lore=[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],custom_data={ra:{creative_eu:1b}},entity_data={id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_eu"],Silent:1b,NoAI:1b,Invulnerable:1b}]""")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Creative EU Source",color:"light_purple"}]""")
		}

		pack.define(CreativeEu.goggles) {
			// Goggles readout, and the block's display name.
			// Context: as the block's marker, at the block position.

			raw("data modify storage ra:temp block_name set value \"Creative EU Source\"")
			raw("execute if data storage ra:temp name_only run return 0")

			raw("data modify storage ra:temp billboard set value {show_name:1b,name_y:1.0}")
			raw("data modify storage ra:temp billboard.name set from storage ra:temp block_name")
			function(Goggles.Billboard.handleBillboard, Storage.TEMP, "billboard")

			function(Goggles.Billboard.stackReset, nbt { this["top"] = 80; this["step"] = 22 })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "state"; this["label"] = ""; this["color"] = "light_purple"; this["suffix"] = "" })
			function(Goggles.Billboard.stackedStatusLine, nbt { this["path"] = "filled"; this["label"] = "Filled: "; this["color"] = "yellow"; this["suffix"] = " EU" })
		}

		pack.define(CreativeEu.tick) {
			// Creative EU Source: fills its grid from nothing, every tick.
			// Context: as its marker, at its block.
			//
			// A testing and building block. It has no fuel, no recipe and no cost -- the
			// point is to take the power supply out of the question entirely while you are
			// working on the half of the build that consumes it.
			//
			// It fills to capacity rather than offering a fixed rate, so it does not matter
			// how much the grid draws: whatever was spent last tick is back this tick, and a
			// machine on a creative grid never sees a brownout. net/offer clamps to the free
			// space itself, so asking for the whole capacity is safe and needs no arithmetic
			// here.

			function(RaLib.Transport.Net.read)
			raw("scoreboard players operation #cr.fill ra.wires.tmp = #net_capacity ra.tr.tmp")
			raw("scoreboard players operation #cr.fill ra.wires.tmp -= #net_amount ra.tr.tmp")

			raw("execute if score #cr.fill ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value \"Grid full\"")

			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #cr.fill ra.wires.tmp")
			line("execute store result score #cr.made ra.wires.tmp run function", EuApi.offerEu, "with storage ra:wires eu")

			raw("data modify entity @s data.status.state set value \"Generating\"")
			raw("execute store result entity @s data.status.filled int 1 run scoreboard players get #cr.made ra.wires.tmp")

			raw("particle minecraft:end_rod ~ ~0.8 ~ 0.25 0.15 0.25 0.01 3 normal @a[scores={ra.u.par=1..}]")
		}
	}
}
