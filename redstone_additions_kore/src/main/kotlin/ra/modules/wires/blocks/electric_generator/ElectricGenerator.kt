package ra.modules.wires.blocks.electric_generator

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.textLine
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line

object ElectricGenerator : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/electric_generator/goggles")
	val idle = ref(Ns.WIRES, "blocks/electric_generator/idle")
	val refreshDisplay = ref(Ns.WIRES, "blocks/electric_generator/refresh_display")
	val running = ref(Ns.WIRES, "blocks/electric_generator/running")

	override fun Ra.register() {
		pack.define(ElectricGenerator.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("EU Generator", nameY = 1.0)

			dataLine("available_eu", "EU: ", "aqua", y = 0.8, suffix = " EU")
			execute { ifCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "On", "green", y = 0.55) } }
			execute { unlessCondition { data(self(), "data.status{active:1b}") }; run { textLine("Active: ", "Off", "gray", y = 0.55) } }
			dataLine("fuel", "Fuel: ", "gold", y = 0.3)
		}

		pack.define(ElectricGenerator.idle) {
			// Put out a generator that has stopped burning.
			// Context: as the generator marker, at its block.
			//
			// Runs on every tick it is not burning, not just the tick it stops. A light left
			// behind by a chunk unload or a marker killed mid-burn would otherwise stay lit
			// for ever, and an invisible block with no source is not something a player can
			// reasonably find.

			// gen_lit comes off BEFORE the repaint, because refresh_display now reads that tag
			// to decide which way to draw the skin. Repainting first would ask it "are you
			// lit?" while the tag still says yes, and the generator would go dark only on the
			// NEXT stop -- one stop behind for ever.
			raw("execute if entity @s[tag=ra.wires.gen_was_lit] run tag @s remove ra.wires.gen_lit")
			line("execute if entity @s[tag=ra.wires.gen_was_lit] run function", ElectricGenerator.refreshDisplay)
			tag(self()) { remove("ra.wires.gen_was_lit") }
			tag(self()) { remove("ra.wires.gen_lit") }

			raw("execute if block ~ ~1 ~ minecraft:light[level=10] run setblock ~ ~1 ~ air")
		}

		pack.define(ElectricGenerator.refreshDisplay) {
			// Rebuild the EU Generator's furnace skin, lit or unlit.
			// Context: as the generator marker, at its block position.
			//
			// The block underneath is a barrel, because the generator only wants somewhere to
			// put fuel. A real furnace brings two input slots and its own smelting, neither of
			// which mean anything here, and players fill the top slot with ore and wait. See
			// ra_lib:skin/apply for the full reasoning.
			//
			// The skin carries the `lit` state, so a running generator glows the way a burning
			// furnace does. It goes through apply_lit rather than apply: a block state cannot
			// ride along in the block's NAME, and putting it there is what made this skin
			// disappear entirely rather than fail loudly.
			//
			// Changing lit on the display that is already there beats rebuilding it -- see
			// ra_lib:skin/set_lit for why a kill-and-respawn blinks and z-fights. apply_lit
			// is the fallback for when there is no display to edit yet.
			//
			// LIT COMES FROM THE gen_lit TAG, NOT FROM data.data.burn
			// It used to read data.data.burn, which is the SOLID FUEL countdown. A generator
			// running on steam never sets that field -- generator_tick raises #gen.burning
			// from the steam path directly -- so a steam generator produced EU while drawn
			// permanently unlit. `lit` is a question about whether the block is generating,
			// and data.data.burn only answers "is it burning an item", which is a different
			// question that happens to coincide for one of the two fuels.
			//
			// running/idle already maintain gen_lit for BOTH fuel paths, so that tag is the
			// honest answer. idle must therefore clear gen_lit BEFORE calling this.

			raw("data modify storage ra:wires gen.skin.id set value \"electric_generator\"")
			raw("data modify storage ra:wires gen.skin.lit set value \"false\"")
			raw("execute if entity @s[tag=ra.wires.gen_lit] run data modify storage ra:wires gen.skin.lit set value \"true\"")

			line("execute store result score #gen.skinned ra.wires.tmp run function", RaLib.Skin.setLit, "with storage ra:wires gen.skin")
			raw("execute if score #gen.skinned ra.wires.tmp matches 1.. run return 0")

			line("execute if entity @s[tag=ra.wires.gen_lit] run function", RaLib.Skin.applyLit, """{real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"true"}""")
			line("execute unless entity @s[tag=ra.wires.gen_lit] run function", RaLib.Skin.applyLit, """{real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"false"}""")
		}

		pack.define(ElectricGenerator.running) {
			// Show that this generator is actually burning.
			// Context: as the generator marker, at its block.
			//
			// Three signals, because a barrel wearing a skin has nothing of its own to show:
			// the skin flips to a lit furnace, the block throws smoke and flame the way a
			// furnace does, and it lights its own space so a room full of running generators
			// is visibly lit rather than pitch dark.

			// Repaint only on the tick the state actually changes, so this is not a skin
			// respawn every tick for every generator in the world.
			raw("execute unless entity @s[tag=ra.wires.gen_lit] run tag @s add ra.wires.gen_lit")
			line("execute unless entity @s[tag=ra.wires.gen_was_lit] run function", ElectricGenerator.refreshDisplay)
			tag(self()) { add("ra.wires.gen_was_lit") }

			raw("particle minecraft:smoke ~ ~1.1 ~ 0.15 0.05 0.15 0.01 2 normal @a[scores={ra.u.par=1..}]")
			raw("particle minecraft:flame ~ ~0.6 ~ 0.25 0.1 0.25 0.005 1 normal @a[scores={ra.u.par=1..}]")

			// One block of light above it. Only ever placed into air and only ever removed
			// again if it is still the light this put there -- the same discipline the
			// Industrial Light uses, so nothing a player built can be replaced.
			raw("execute if block ~ ~1 ~ #minecraft:air run setblock ~ ~1 ~ minecraft:light[level=10]")
		}
	}
}
