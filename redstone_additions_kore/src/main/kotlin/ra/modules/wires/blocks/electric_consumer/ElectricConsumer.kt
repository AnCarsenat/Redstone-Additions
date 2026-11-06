package ra.modules.wires.blocks.electric_consumer

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.textLine
import ra.core.define
import ra.core.raw
import ra.core.ref

object ElectricConsumer : PackPart {
	val cycleRate = ref(Ns.WIRES, "blocks/electric_consumer/cycle_rate")
	val goggles = ref(Ns.WIRES, "blocks/electric_consumer/goggles")

	override fun Ra.register() {
		pack.define(ElectricConsumer.cycleRate) {
			// Cycle the consumer's draw: normal -> heavy -> light.
			// Context: as the consumer marker, at the block.
			//
			// A consumer takes all or nothing, so this is what it costs to run it for a tick.
			// A grid that cannot cover the heavy setting simply leaves the block idle, which
			// is the point of having the setting at all: it is how you decide what a base
			// spends its generation on when there is not enough to go round.

			raw("execute unless data entity @s data.properties.eu_use run data modify entity @s data.properties.eu_use set value 40")

			scoreboard.players.set(literal("#co.rate"), "ra.wires.tmp", 0)
			raw("execute if data entity @s data.properties{eu_use:40} run scoreboard players set #co.rate ra.wires.tmp 1")
			raw("execute if data entity @s data.properties{eu_use:80} run scoreboard players set #co.rate ra.wires.tmp 2")

			raw("execute if score #co.rate ra.wires.tmp matches 0 run data modify entity @s data.properties.eu_use set value 40")
			raw("execute if score #co.rate ra.wires.tmp matches 1 run data modify entity @s data.properties.eu_use set value 80")
			raw("execute if score #co.rate ra.wires.tmp matches 2 run data modify entity @s data.properties.eu_use set value 20")

			raw("""execute if score #co.rate ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"normal — 40 EU/t",color:"yellow"}]""")
			raw("""execute if score #co.rate ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"heavy — 80 EU/t",color:"red"}]""")
			raw("""execute if score #co.rate ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"light — 20 EU/t",color:"green"}]""")
			raw("playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4")
		}

		pack.define(ElectricConsumer.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("EU Consumer", nameY = 1.0)

			dataLine("available_eu", "EU: ", "aqua", y = 0.8, suffix = " EU")
			execute { ifCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "On", "green", y = 0.55) } }
			execute { unlessCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "Off", "gray", y = 0.55) } }
		}
	}
}
