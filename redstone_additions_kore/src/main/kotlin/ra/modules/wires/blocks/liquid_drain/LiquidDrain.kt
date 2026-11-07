package ra.modules.wires.blocks.liquid_drain

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.utils.nbt
import ra.api.Goggles
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.ref

object LiquidDrain : PackPart {
	val cycleMode = ref(Ns.WIRES, "blocks/liquid_drain/cycle_mode")
	val cycleRate = ref(Ns.WIRES, "blocks/liquid_drain/cycle_rate")
	val goggles = ref(Ns.WIRES, "blocks/liquid_drain/goggles")

	override fun Ra.register() {
		pack.define(LiquidDrain.cycleMode) {
			// Flip the drain between taking from the world and putting back into it.
			// Context: as the marker, at the block.
			//
			// Lifted out of the goggles tinker when that was removed. Same two-state flip,
			// read before write.

			raw("execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value \"drain\"")

			scoreboard.players.set(literal("#wr.was"), "ra.temp", 0)
			raw("""execute if data entity @s data.properties{mode:"drain"} run scoreboard players set #wr.was ra.temp 1""")

			raw("execute if score #wr.was ra.temp matches 1 run data modify entity @s data.properties.mode set value \"place\"")
			raw("execute if score #wr.was ra.temp matches 0 run data modify entity @s data.properties.mode set value \"drain\"")

			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain mode: ",color:"gray"},{nbt:"data.properties.mode",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4")
		}

		pack.define(LiquidDrain.cycleRate) {
			// Cycle the drain's throughput: slow -> normal -> fast.
			// Context: as the drain marker, at the block.
			//
			// The knob is the cooldown between actions rather than the amount moved, because
			// a drain deals in whole source blocks: it either places a bucket's worth or it
			// does not. Five litres every 40, 20 or 10 ticks is 2.5, 5 or 10 L a second.

			raw("execute unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 20")

			scoreboard.players.set(literal("#dr.rate"), "ra.wires.tmp", 0)
			raw("execute if data entity @s data.properties{cooldown:20} run scoreboard players set #dr.rate ra.wires.tmp 1")
			raw("execute if data entity @s data.properties{cooldown:10} run scoreboard players set #dr.rate ra.wires.tmp 2")

			raw("execute if score #dr.rate ra.wires.tmp matches 0 run data modify entity @s data.properties.cooldown set value 20")
			raw("execute if score #dr.rate ra.wires.tmp matches 1 run data modify entity @s data.properties.cooldown set value 10")
			raw("execute if score #dr.rate ra.wires.tmp matches 2 run data modify entity @s data.properties.cooldown set value 40")

			raw("""execute if score #dr.rate ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"normal — 5 L/s",color:"yellow"}]""")
			raw("""execute if score #dr.rate ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"fast — 10 L/s",color:"green"}]""")
			raw("""execute if score #dr.rate ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"slow — 2.5 L/s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4")
		}

		pack.define(LiquidDrain.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Liquid Drain", nameY = 1.0)

			// Contents belong to the network, not this node; refresh_status copies them here.
			dataLine("medium", "Medium: ", "aqua", y = 0.8)
			dataLine("amount", "Amount: ", "yellow", y = 0.55, suffix = " mL")
			dataLine("drain_state", "Drain: ", "gray", y = 0.3)
			propLine("mode", "Mode: ", "aqua", y = 0.05)
			propLine("cooldown", "Cooldown: ", "gray", y = -0.2, suffix = "t")
		}
	}
}
