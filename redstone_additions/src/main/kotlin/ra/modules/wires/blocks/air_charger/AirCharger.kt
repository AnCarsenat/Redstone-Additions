package ra.modules.wires.blocks.air_charger

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.predicates.providers.sub
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.dataLine
import ra.core.block.gogglesHeader
import ra.core.block.killVanillaDrop
import ra.core.block.soundListenersSel
import ra.core.computeScore
import ra.core.define
import ra.core.executeRun
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.WiresBlocks
import ra.modules.wires.WiresBlocks.stackNbt
import ra.modules.wires.common.Common
import ra.modules.wires.diving.AirTank
import ra.modules.wires.diving.DivingItems.AIR_MAX
import ra.modules.wires.diving.DivingItems.ML_PER_SECOND
import ra.modules.wires.machine.MachineClock

/**
 * Air Charger: fills the Air Tank of a player standing on it from its gas network's oxygen.
 *
 * ONE STEP A SECOND, ALL OR NOTHING. On the machine clock, a step is a minute of air (600 mL of
 * oxygen at [ML_PER_SECOND]), or what is left to fill when that is less, so an empty tank is full
 * in fifteen seconds. The network must hold the whole step; the take comes first and a short one is
 * given back, so a tank never gains air the network did not pay for.
 *
 * OXYGEN ONLY. Steam, smoke and toxic gas are gases too, but nothing you would breathe. Oxygen
 * comes from a Gas Pump beside an upward bubble column (soul sand under water), or a Creative
 * Fluid Source.
 *
 * NOBODY ON IT costs one selector a second.
 */
object AirCharger : PackPart {
	const val ID = "air_charger"
	const val BLOCK = "minecraft:waxed_weathered_copper_grate"

	/** Seconds of air per step. */
	const val STEP = 60

	private const val T = "ra.wires.tmp"
	private const val S = "storage ra:wires ac"
	private const val USER = "ra.wires.ac_user"

	val goggles = ref(Ns.WIRES, "blocks/$ID/goggles")
	val run = ref(Ns.WIRES, "blocks/$ID/run")
	val onBreak = ref(Ns.WIRES, "blocks/$ID/on_break")
	private val charge = ref(Ns.WIRES, "blocks/$ID/charge")

	override fun Ra.register() {
		properties(ID) {}

		pack.define(AirCharger.run) { run() }
		pack.define(charge) { charge() }

		pack.define(AirCharger.goggles) {
			gogglesHeader("Air Charger", nameY = 1.0)
			dataLine("charger_state", "Charger: ", "yellow", y = 0.8)
			dataLine("medium", "Gas: ", "aqua", y = 0.6)
			dataLine("amount", "Amount: ", "aqua", y = 0.4, suffix = " mL")
		}

		pack.define(AirCharger.onBreak) {
			// As the marker, at it. Ends by killing the marker.
			tag(self()) { remove("ra.wires.fluid_node") }
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
		// The space on top of the block: whoever stands on it, wearing a tank. One at a time.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as @a[dx=0,dy=0,dz=0,sort=nearest] if ${AirTank.WORN} run tag @s add $USER")
		raw("execute unless entity @a[tag=$USER,limit=1] run return run data modify entity @s data.status.charger_state set value \"idle\"")
		// Several on it: the nearest is served first, the others the next seconds.
		raw("tag @a[tag=$USER,sort=nearest,limit=1] add $USER.now")
		raw("tag @a[tag=$USER] remove $USER")
		function(charge)
		raw("tag @a[tag=$USER.now] remove $USER.now")
	}

	private fun Function.charge() {
		val user = "@a[tag=$USER.now,limit=1]"
		executeRun("as $user", AirTank.read)
		raw("execute if score #at.air $T matches $AIR_MAX.. run data modify entity @s data.status.charger_state set value \"full\"")
		executeRun("if score #at.air $T matches $AIR_MAX.. as $user", AirTank.gauge)
		raw("execute if score #at.air $T matches $AIR_MAX.. run return 0")

		raw("execute unless score @s ra.tr.net matches 1.. run return run data modify entity @s data.status.charger_state set value \"no_network\"")
		function(RaLib.Transport.Net.read)

		// The step, in seconds of air and in mL of oxygen.
		computeScore("#ac.step", T, provider { min(sub(constant(AIR_MAX), scoreOf("#at.air", T)), constant(STEP)) })
		computeScore("#ac.need", T, provider { mul(scoreOf("#ac.step", T), constant(ML_PER_SECOND)) })
		raw("execute store result score #ac.have $T run data get storage ra:transport cur.amounts.oxygen")
		raw("execute if score #ac.have $T < #ac.need $T run return run data modify entity @s data.status.charger_state set value \"no_oxygen\"")

		// Paid for first; a short take goes back and nothing is filled.
		raw("data modify $S set value {medium:\"oxygen\"}")
		raw("execute store result $S.amount int 1 run scoreboard players get #ac.need $T")
		raw("execute store result score #ac.got $T run function ${RaLib.Transport.Net.take.asId()} with $S")
		raw("execute if score #ac.got $T < #ac.need $T store result $S.amount int 1 run scoreboard players get #ac.got $T")
		raw("execute if score #ac.got $T < #ac.need $T if score #ac.got $T matches 1.. run function ${RaLib.Transport.Net.offer.asId()} with $S")
		raw("data remove $S")
		raw("execute if score #ac.got $T < #ac.need $T run return run data modify entity @s data.status.charger_state set value \"no_oxygen\"")

		raw("scoreboard players operation #at.air $T += #ac.step $T")
		executeRun("as $user", AirTank.write)
		executeRun("as $user", AirTank.gauge)
		raw("playsound minecraft:block.bubble_column.upwards_ambient block ${soundListenersSel(16)} ~ ~1 ~ 0.6 1.4")
		raw("data modify entity @s data.status.charger_state set value \"charging\"")
	}
}
