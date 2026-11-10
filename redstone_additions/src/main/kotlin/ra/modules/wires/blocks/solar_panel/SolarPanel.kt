package ra.modules.wires.blocks.solar_panel

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.features.predicates.conditions.MatchBlock
import io.github.ayfri.kore.features.predicates.providers.intNumberDispatcher
import io.github.ayfri.kore.features.predicates.sub.blockPredicate
import io.github.ayfri.kore.generated.Blocks
import ra.api.Goggles
import ra.core.COMPUTE_BLOCK_HERE
import ra.core.computeScore
import ra.core.defineProvider
import ra.core.namedProvider
import ra.core.provider
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.electric.EuApi

object SolarPanel : PackPart {
	val goggles = ref(Ns.WIRES, "blocks/solar_panel/goggles")
	val tick = ref(Ns.WIRES, "blocks/solar_panel/tick")

	/**
	 * THE SOLAR CURVE: EU per tick for each daylight detector `power`. Below 5 a panel makes
	 * nothing. 50 at full sun is the figure the rest of the pack is priced against (the Electric
	 * Furnace's mode table, the changelog's "twenty panels"), so change it here or nowhere.
	 */
	private val CURVE = listOf(5 to 10, 6 to 12, 7 to 15, 8 to 18, 9 to 22, 10 to 26, 11 to 30, 12 to 35, 13 to 40, 14 to 45, 15 to 50)

	/**
	 * Named provider `ra_wires:blocks/solar_panel/eu`: [CURVE] as a `number_dispatcher` whose
	 * cases are `match_block` tests on the detector's `power`, 0 when none matches. Evaluated
	 * with `compute block ~ ~ ~`, which hands the block's state to the conditions.
	 */
	val EU = namedProvider(Ns.WIRES, "blocks/solar_panel/eu")

	override fun Ra.register() {
		pack.defineProvider(EU, provider {
			intNumberDispatcher(0) {
				for ((power, eu) in CURVE)
					case(MatchBlock(blockPredicate(Blocks.DAYLIGHT_DETECTOR) { state = mapOf("power" to "$power") }), eu)
			}
		})

		pack.define(SolarPanel.goggles) {
			// Goggles readout for this block, and the block's display name.
			// Context: as the block's marker, at the block position.
			//
			// The name is published to storage ra:temp block_name before anything is
			// drawn, and the function stops there when the caller only wants the name.
			// That makes this the single source of the name: ra:tools/block_name reuses
			// the same dispatch to answer the Data Handlers, which used to carry two
			// separate hand-written name tables that had already drifted apart.

			gogglesHeader("Solar Panel", nameY = 0.7)

			// `fuel` says "Sunlight", "No sunlight" or "Grid full", so labelling it "Light:"
			// read as nonsense the moment the grid backed up. The grid line answers that
			// question properly and the sun line only ever talks about the sun.
			dataLine("available_eu", "EU: ", "aqua", y = 0.7, suffix = " EU")
			dataLine("grid", "", "yellow", y = 0.45)
			dataLine("fuel", "Sun: ", "gold", y = 0.2)
			dataLine("output", "Making: ", "green", y = -0.05, suffix = " EU/t")
		}

		pack.define(SolarPanel.tick) {
			// Generate EU from sunlight and put it on the grid.
			// Context: as the solar panel marker, at the panel position.
			//
			// The panel IS a vanilla daylight detector, so the game has already worked out
			// how much sky light reaches it and published the answer as the block's `power`
			// state. Reading that is free and handles night, rain, roofs, snow cover and the
			// Nether without a single predicate of our own.
			//
			// THE TABLE BELOW IS EU PER TICK, AND IT IS OFFERED EVERY TICK
			// It used to be gated behind a 20-tick duty cycle, which quietly divided the
			// panel's output by twenty: the scale peaks at 50, so a panel made 2.5 EU/t and
			// twenty of them made 50 EU/t at absolute noon. Every number the pack states
			// about this block assumes otherwise -- the Electric Furnace's whole mode table
			// is priced against "a Solar Panel peaks at 50 EU/t, one EU Generator is 60"
			// (see electric_furnace/read_mode), and the changelog puts twenty panels at
			// ~200 EU/t averaged over a daylight cycle. Both are only true at one offer per
			// tick.
			//
			// The visible symptom was an Electric Furnace on `superpowered` -- 300 EU every
			// 5 ticks, 60 EU/t -- outrunning twenty panels' 50 EU/t for ever, so the grid sat
			// at zero and the Batteries next to it never charged however much sun there was.
			//
			// Batching it back up instead (20x the amount, once every 20 ticks) would average
			// the same and behave worse: a 1000 EU burst needs somewhere to land on the tick
			// it arrives, and a panel contributes only 50 of capacity, so most of it would be
			// refused outright on any grid without a Battery.

			// EU from the detector's power, through the named curve (see [CURVE]). One /compute in
			// the block's context reads the state the eleven `execute if block` lines used to test.
			// The 0 first is belt and braces: the curve has a default, so it cannot fail.
			scoreboard.players.set(literal("#solar_gen"), "ra.wires.tmp", 0)
			computeScore("#solar_gen", "ra.wires.tmp", EU, source = COMPUTE_BLOCK_HERE)

			raw("execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.fuel set value \"No sunlight\"")
			raw("execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.active set value 0b")
			raw("execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.output set value 0")
			raw("execute if score #solar_gen ra.wires.tmp matches 0 run data modify entity @s data.status.accepted set value 0")
			raw("execute if score #solar_gen ra.wires.tmp matches 0 run return 0")

			raw("data modify entity @s data.status.fuel set value \"Sunlight\"")

			// Offer the batch to the grid rather than into a buffer of our own. The network
			// refuses whatever will not fit, so there is no free-space arithmetic here and no
			// capacity of ours to overflow.
			raw("execute store result storage ra:wires eu.amount int 1 run scoreboard players get #solar_gen ra.wires.tmp")
			executeRun("store result score #solar_made ra.wires.tmp", EuApi.offerEu, "with storage ra:wires eu")

			raw("execute if score #solar_made ra.wires.tmp matches 1.. run data modify entity @s data.status.active set value 1b")
			raw("execute if score #solar_made ra.wires.tmp matches ..0 run data modify entity @s data.status.active set value 0b")

			// What the panel is MAKING, which is not what the grid took -- a full grid
			// refuses a panel in full sun and the two numbers are worth telling apart. The EU
			// Generator publishes the same pair for the same reason.
			raw("execute store result entity @s data.status.output int 1 run scoreboard players get #solar_gen ra.wires.tmp")
			raw("execute store result entity @s data.status.accepted int 1 run scoreboard players get #solar_made ra.wires.tmp")
		}
	}
}
