package ra.modules.wires.blocks.water_wheel

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.add
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.mod
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.particleViewersSel
import ra.core.block.readProperty
import ra.core.computeScore
import ra.core.define
import ra.core.executeRun
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.blocks.water_wheel.WaterWheelModel.build
import ra.modules.wires.blocks.water_wheel.WaterWheelModel.spin
import ra.modules.wires.common.Common
import ra.modules.wires.electric.EuApi
import ra.modules.wires.machine.MachineClock

/**
 * Water Wheel: an EU generator turned by flowing water, once a second on the machine clock.
 *
 * WHERE THE WATER IS. The wheel turns in the plane facing the player who placed it: its axle (the
 * hub log's axis) points the way they looked. The eight blocks around the hub in that plane are
 * where its paddles dip, and each one holding FLOWING water (`minecraft:water` other than a source,
 * falling water included) adds `eu_per_block` EU per tick (default 5, admin only): 5 to 40 EU/t.
 * Still water turns nothing, as a real wheel needs a current.
 *
 * ONCE A SECOND. What the second made (20 ticks' worth) is offered to the grid in one go; the
 * wheel's own capacity ([CAPACITY]) is larger than a full wheel's second at the default rate, so a
 * grid with no Battery still takes it.
 *
 * THE ANIMATION ([WaterWheelModel]) is sixteen block displays standing at the marker, turned one
 * or two slots (22.5 degrees each, two with three or more flowing blocks) every second by a
 * `left_rotation` change the client interpolates over the whole second. Nothing per tick, and
 * nothing at all while no water flows: the wheel stops where it is.
 */
object WaterWheel : PackPart {
	const val ID = "water_wheel"
	const val BLOCK = "minecraft:stripped_spruce_log"

	/** Ticks between two runs: the machine clock's period, and the animation's interpolation. */
	const val PERIOD = MachineClock.PERIOD

	const val DEFAULT_EU = 5
	const val MAX_EU = 1000
	const val CAPACITY = 1000

	private const val T = "ra.wires.tmp"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val run = ref(Ns.WIRES, "blocks/$ID/run")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	val setup = ref(Ns.WIRES, "blocks/$ID/setup")
	private val countX = ref(Ns.WIRES, "blocks/$ID/count_x")
	private val countZ = ref(Ns.WIRES, "blocks/$ID/count_z")
	private val buildX = ref(Ns.WIRES, "blocks/$ID/build_x")
	private val buildZ = ref(Ns.WIRES, "blocks/$ID/build_z")
	private fun spinRef(k: Int) = ref(Ns.WIRES, "blocks/$ID/spin_$k")

	override fun Ra.register() {
		properties(ID) {
			// Balance, so admin only; the default for new wheels is on the Power & Fluids page.
			number("eu_per_block", min = 1, max = MAX_EU, adminOnly = true, default = DEFAULT_EU)
		}

		pack.define(WaterWheel.run) { run() }

		// One flowing water block around the hub, in the wheel's plane, adds one to #ww.n.
		for ((fn, axis) in listOf(countX to WaterWheelModel.Axis.X, countZ to WaterWheelModel.Axis.Z)) pack.define(fn) {
			for (a in -1..1) for (b in -1..1) {
				if (a == 0 && b == 0) continue
				val pos = if (axis == WaterWheelModel.Axis.X) "~ ~$a ~$b" else "~$b ~$a ~"
				raw("execute if block $pos minecraft:water unless block $pos minecraft:water[level=0] run scoreboard players add #ww.n $T 1")
			}
		}

		// Placed, or found without its wheel: turn the hub log along the axle and (re)build the
		// wheel at slot 0. As the marker, at it.
		pack.define(setup) {
			raw("kill @e[type=block_display,tag=${WaterWheelModel.TAG},distance=..0.01]")
			raw("data modify entity @s data.data.spin set value 0")
			raw("execute if score @s ra.facing matches 2..3 run return run function ${buildZ.asId()}")
			function(buildX)
		}
		for ((fn, axis) in listOf(buildX to WaterWheelModel.Axis.X, buildZ to WaterWheelModel.Axis.Z)) pack.define(fn) {
			raw("execute if block ~ ~ ~ $BLOCK run setblock ~ ~ ~ $BLOCK[axis=${axis.name.lowercase()}]")
			raw("data modify entity @s data.data.axis set value \"${axis.vector}\"")
			build(axis)
		}
		for (k in 0 until WaterWheelModel.SLOTS) pack.define(spinRef(k)) { spin(k) }

		pack.define(WaterWheel.goggles) {
			gogglesHeader("Water Wheel", nameY = 1.0)
			dataLine("wheel", "Wheel: ", "yellow", y = 0.8)
			dataLine("water", "Flowing water: ", "aqua", y = 0.6, suffix = " blocks")
			dataLine("output", "Making: ", "green", y = 0.4, suffix = " EU/t")
			dataLine("available_eu", "EU: ", "aqua", y = 0.2, suffix = " EU")
		}

		pack.define(WaterWheel.onBreak) {
			// As the marker, at it. Ends by killing the marker.
			raw("kill @e[type=block_display,tag=${WaterWheelModel.TAG},distance=..0.01]")
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			tag(self()) { remove(MachineClock.TAG) }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop(BLOCK)
			raw("summon item ~ ~ ~ {Item:${WiresBlocks.items.first { it.block.id == ID }.stackNbt().toSnbt()}}")
			kill(self())
		}
	}

	/** As the marker, at it, once a second from the machine clock. */
	private fun Function.run() {
		raw("scoreboard players set #ww.n $T 0")
		raw("execute if score @s ra.facing matches 2..3 run function ${countZ.asId()}")
		raw("execute unless score @s ra.facing matches 2..3 run function ${countX.asId()}")
		raw("execute store result entity @s data.status.water int 1 run scoreboard players get #ww.n $T")
		raw("execute if score #ww.n $T matches 0 run data modify entity @s data.status.output set value 0")
		raw("execute if score #ww.n $T matches 0 run return run data modify entity @s data.status.wheel set value \"No flowing water\"")

		executeRun("unless entity @e[type=block_display,tag=${WaterWheelModel.TAG},distance=..0.01,limit=1]", setup)

		// EU per tick for the water found, and the second's worth offered in one go.
		readProperty("eu_per_block", default = DEFAULT_EU, min = 1)
		raw("execute if score #prop ra.temp matches ${MAX_EU + 1}.. run scoreboard players set #prop ra.temp $MAX_EU")
		computeScore("#ww.eu", T, provider { mul(scoreOf("#prop", "ra.temp"), scoreOf("#ww.n", T)) },
			"store result entity @s data.status.output int 1")
		computeScore("#ww.offer", T, provider { mul(scoreOf("#ww.eu", T), constant(PERIOD)) },
			"store result storage ra:wires eu.amount int 1")
		executeRun("store result score #ww.made $T", EuApi.offerEu, "with storage ra:wires eu")
		raw("execute store result entity @s data.status.accepted int 1 run scoreboard players get #ww.made $T")
		raw("execute if score #ww.made $T matches 1.. run data modify entity @s data.status.wheel set value \"Turning\"")
		raw("execute if score #ww.made $T matches ..0 run data modify entity @s data.status.wheel set value \"Grid full\"")

		raw("particle minecraft:splash ~ ~-0.9 ~ 0.5 0.1 0.5 0 6 normal ${particleViewersSel(32)}")

		// Turn: one slot a second, two with three or more flowing blocks. Water pushes the wheel
		// whether or not the grid has room, so it turns either way.
		raw("execute store result score #ww.k $T run data get entity @s data.data.spin")
		raw("scoreboard players set #ww.step $T 1")
		raw("execute if score #ww.n $T matches 3.. run scoreboard players set #ww.step $T 2")
		computeScore("#ww.k", T, provider { mod(add(scoreOf("#ww.k", T), scoreOf("#ww.step", T)), constant(WaterWheelModel.SLOTS)) },
			"store result entity @s data.data.spin int 1")
		for (k in 0 until WaterWheelModel.SLOTS)
			raw("execute if score #ww.k $T matches $k run return run function ${spinRef(k).asId()} with entity @s data.data")
	}
}
