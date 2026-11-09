package ra.modules.wires

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import net.benwoodworth.knbt.NbtByte
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.moduleLoaded
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.Placement
import ra.modules.wires.blocks.creative_eu.CreativeEu
import ra.modules.wires.blocks.creative_fluid.CreativeFluid
import ra.modules.wires.blocks.electric_furnace.ElectricFurnaceSettings
import ra.modules.wires.bridge.Bridge
import ra.modules.wires.common.Common
import ra.modules.wires.electric.Electric
import ra.modules.wires.fluid.Fluid
import ra.modules.wires.media.Media
import ra.modules.wires.readout.Readout
import ra.modules.wires.readout.Readout.resetFlag

object WiresLifecycle : PackPart {
	val giveCreative = ref(Ns.WIRES, "items/give_creative")
	val load = ref(Ns.WIRES, "load")
	val tick = ref(Ns.WIRES, "tick")

	override fun Ra.register() {
		pack.define(WiresLifecycle.giveCreative) {
			// Give the creative-only wires blocks.
			//
			// A shortcut. Both are also in the Wires Bundle alongside the rest of the module,
			// which is where you would look for them; neither has a recipe, so the bundle and
			// this function are the only ways to get one.

			function(CreativeEu.give)
			function(CreativeFluid.give)
		}

		pack.define(WiresLifecycle.load) {
			// RA Wires - load entrypoint for merged fluid and electric networks

			scoreboard.objectives.add("ra.wires.tmp")
			scoreboard.objectives.add("ra.wires.tmp2")

			function(Media.init)
			function(Media.potions)
			function(ElectricFurnaceSettings.initRecipes)

			data(Storage.WIRES) { modify("initialized", NbtByte(1)) }

			function(Placement.registerAll)
			// The readout clock's schedule may not have survived; the next tick starts it again.
			resetFlag()

			moduleLoaded("Wires module loaded!", "aqua")
		}

		pack.define(WiresLifecycle.tick) {
			// Tick merged fluid and electric systems in RA Wires

			executeRun("unless data storage ra:wires {initialized:1b}", WiresLifecycle.load)

			// Keep marker anchors snapped to centered block coordinates.
			raw("execute as @e[type=marker,tag=ra.wires.node] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~")

			function(Common.tickCleanup)
			// Bridges move contents between two networks and belong to neither, so they are
			// ticked here rather than inside the fluid or electric pass -- the same block
			// logic serves a Valve and a Breaker.
			executeRun("as @e[type=marker,tag=ra.wires.bridge] at @s", Bridge.tick)

			function(Fluid.tick)
			function(Electric.tick)
			function(Readout.tick)
		}
	}
}
