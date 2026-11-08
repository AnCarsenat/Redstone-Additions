package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeReturnRun
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.Cip
import ra.modules.wires.common.storeCompute
import ra.modules.wires.common.storeComputeStorage

object DrainExperience : PackPart {
	/** What one experience point is worth in the network, both ways. */
	private const val ML_PER_POINT = 100

	val drainExp = ref(Ns.WIRES, "fluid/drain_exp")
	val drainExpClear = ref(Ns.WIRES, "fluid/drain_exp_clear")
	val drainExpOffer = ref(Ns.WIRES, "fluid/drain_exp_offer")
	val drainExpPull = ref(Ns.WIRES, "fluid/drain_exp_pull")
	val drainExpTake = ref(Ns.WIRES, "fluid/drain_exp_take")
	val drainPlace = ref(Ns.WIRES, "fluid/drain_place")
	val drainPlaceExp = ref(Ns.WIRES, "fluid/drain_place_exp")
	val drainPlaceOrb = ref(Ns.WIRES, "fluid/drain_place_orb")
	val drainPlaceRun = ref(Ns.WIRES, "fluid/drain_place_run")
	val drainPlaceTry = ref(Ns.WIRES, "fluid/drain_place_try")

	override fun Ra.register() {
		pack.define(DrainExperience.drainExp) {
			// Draw a sneaking player's experience into the network.
			// Context: as a vertical drain marker, at the drain position.
			//
			// One experience point is 100 mL, and that rate is used in both directions: what
			// a player pours in is exactly what comes back out of a drain set to place. The
			// conversion is deliberately a round number so a player can read a billboard in
			// litres and know what they will get back.
			//
			// Whole points only. Experience is not divisible, so the network is charged for
			// what can actually be taken off the player rather than for a fraction that would
			// round away to nothing on the way out.

			raw("tag @a[distance=..2.5,predicate=ra:is_sneaking,sort=nearest,limit=1] add ra.wires.xp_giver")
			raw("execute unless entity @a[tag=ra.wires.xp_giver,limit=1] run return 0")

			raw("execute store result score #dr.pts ra.wires.tmp run xp query @a[tag=ra.wires.xp_giver,limit=1] points")
			executeReturnRun("if score #dr.pts ra.wires.tmp matches ..0", DrainExperience.drainExpClear)

			// A network holding something else will not take experience.
			function(RaLib.Transport.Net.read)
			// Is there EXPERIENCE here, whatever else shares the run.
			raw("execute store result score #dx.have ra.wires.tmp run data get storage ra:transport cur.amounts.experience")
			raw("execute if score #net_amount ra.tr.tmp matches 1.. if score #dx.have ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value \"wrong_medium\"")
			executeReturnRun("if score #net_amount ra.tr.tmp matches 1.. if score #dx.have ra.wires.tmp matches ..0", DrainExperience.drainExpClear)

			// Ten points a cycle, or as much of that as the player has and the grid will hold:
			// pts = min(pts, 10, (capacity - amount) / 100), at 100 mL a point.
			storeCompute(
				"#dr.pts", "ra.wires.tmp",
				Cip.min(
					Cip.score("#dr.pts", "ra.wires.tmp"),
					10,
					Cip.floorDiv(Cip.sub(Cip.score("#net_capacity", "ra.tr.tmp"), Cip.score("#net_amount", "ra.tr.tmp")), ML_PER_POINT),
				),
			)

			raw("execute if score #dr.pts ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value \"network_full\"")
			executeReturnRun("if score #dr.pts ra.wires.tmp matches ..0", DrainExperience.drainExpClear)

			// Charge the grid for exactly the points about to be taken, so neither side can
			// end up owing the other a fraction of a point.
			storeComputeStorage("ra:wires", "xp.amount", Cip.mul(Cip.score("#dr.pts", "ra.wires.tmp"), ML_PER_POINT))
			function(DrainExperience.drainExpOffer, Storage.WIRES, "xp")

			raw("execute store result storage ra:wires xp.points int 1 run scoreboard players get #dr.pts ra.wires.tmp")
			function(DrainExperience.drainExpTake, Storage.WIRES, "xp")

			raw("data modify entity @s data.status.drain_state set value \"absorbing_xp\"")
			function(Fluid.particles, nbt { this["medium"] = "experience" })
			raw("playsound minecraft:entity.experience_orb.pickup block ${soundListenersSel(8)} ~ ~ ~ 0.6 1.4")
			function(DrainExperience.drainExpClear)
		}

		pack.define(DrainExperience.drainExpClear) {
			// Internal: drop the working tag, on every exit path out of drain_exp.
			// Returns 0 so it can be used as `return run function ...`.

			raw("tag @a[tag=ra.wires.xp_giver] remove ra.wires.xp_giver")
			returnValue(0)
		}

		pack.define(DrainExperience.drainExpOffer) {
			// Internal: put the millilitres on the network. Context: as the drain marker.

			raw("\$return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"experience\"}")
		}

		pack.define(DrainExperience.drainExpPull) {
			// Internal: take the millilitres back off the network. Context: as the drain.

			raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"experience\"}")
		}

		pack.define(DrainExperience.drainExpTake) {
			// Internal: take the points off the player who is paying them.
			// Context: as the drain marker.

			raw("\$xp add @a[tag=ra.wires.xp_giver,limit=1] -\$(points) points")
		}

		pack.define(DrainExperience.drainPlace) {
			// Spend network contents placing the medium into the world.
			// Context: as a drain marker in "place" mode, at the drain position.

			function(RaLib.Transport.Net.read)

			raw("execute unless data storage ra:transport cur.medium run data modify entity @s data.status.drain_state set value \"network_empty\"")
			raw("execute unless data storage ra:transport cur.medium run return 0")

			raw("execute if score #net_amount ra.tr.tmp matches ..4999 run data modify entity @s data.status.drain_state set value \"not_enough\"")
			raw("execute if score #net_amount ra.tr.tmp matches ..4999 run return 0")

			function(DrainExperience.drainPlaceRun, Storage.TRANSPORT, "cur")
		}

		pack.define(DrainExperience.drainPlaceExp) {
			// Give experience back to the world as orbs.
			// Context: as a drain marker in "place" mode, at the drain position.
			//
			// The same 100 mL a point that drain_exp charges, so a network filled by standing
			// on one drain empties at exactly the rate it filled — ten points a cycle. What
			// went in comes back out; the network is not a lossy converter.

			function(RaLib.Transport.Net.read)

			// Ten points, in whole points only, and never more than the network holds:
			// pts = min(10, amount / 100).
			storeCompute("#dr.pts", "ra.wires.tmp", Cip.min(10, Cip.floorDiv(Cip.score("#net_amount", "ra.tr.tmp"), ML_PER_POINT)))
			raw("execute if score #dr.pts ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value \"not_enough\"")

			storeComputeStorage("ra:wires", "xp.amount", Cip.mul(Cip.score("#dr.pts", "ra.wires.tmp"), ML_PER_POINT))
			function(DrainExperience.drainExpPull, Storage.WIRES, "xp")

			raw("execute store result storage ra:wires xp.points int 1 run scoreboard players get #dr.pts ra.wires.tmp")
			function(DrainExperience.drainPlaceOrb, Storage.WIRES, "xp")

			raw("data modify entity @s data.status.drain_state set value \"placing\"")
			function(Fluid.particles, nbt { this["medium"] = "experience" })
		}

		pack.define(DrainExperience.drainPlaceOrb) {
			// Internal: drop one orb carrying the whole cycle's worth.
			// Context: as the drain marker, at the drain position.
			//
			// One orb rather than one per point: a stack of single-point orbs is the same
			// experience but a great deal more entity, and vanilla merges them anyway.

			raw("\$summon minecraft:experience_orb ~ ~1 ~ {Value:\$(points)s}")
		}

		pack.define(DrainExperience.drainPlaceRun) {
			// Internal: find a free adjacent block and put one source block of the medium in
			// it. Media with no world block (steam, smoke) cannot be placed and say so.

			// A medium flagged `effect` is applied to whoever is standing there rather than
			// placed as a block -- see ra_wires:fluid/drain_potion.
			raw("\$execute if data storage ra:wires media.\$(medium).effect run return run function ${DrainPotion.drainPotion.asId()}")

			// Experience has no world block; it comes back as orbs instead.
			raw("\$execute if data storage ra:wires media.\$(medium).orb run return run function ${DrainExperience.drainPlaceExp.asId()}")

			raw("\$execute unless data storage ra:wires media.\$(medium).block run data modify entity @s data.status.drain_state set value \"not_placeable\"")
			raw("\$execute unless data storage ra:wires media.\$(medium).block run return 0")

			data(Storage.WIRES) { remove("place") }
			raw("\$data modify storage ra:wires place.block set from storage ra:wires media.\$(medium).block")

			scoreboard.players.set(literal("#placed"), "ra.wires.tmp", 0)
			executeRun("positioned ~1 ~ ~ if block ~ ~ ~ #ra_lib:passable", DrainExperience.drainPlaceTry, "with storage ra:wires place")
			executeRun("positioned ~-1 ~ ~ if block ~ ~ ~ #ra_lib:passable", DrainExperience.drainPlaceTry, "with storage ra:wires place")
			executeRun("positioned ~ ~ ~1 if block ~ ~ ~ #ra_lib:passable", DrainExperience.drainPlaceTry, "with storage ra:wires place")
			executeRun("positioned ~ ~ ~-1 if block ~ ~ ~ #ra_lib:passable", DrainExperience.drainPlaceTry, "with storage ra:wires place")
			executeRun("positioned ~ ~-1 ~ if block ~ ~ ~ #ra_lib:passable", DrainExperience.drainPlaceTry, "with storage ra:wires place")

			raw("execute if score #placed ra.wires.tmp matches 0 run data modify entity @s data.status.drain_state set value \"no_room\"")
			raw("execute if score #placed ra.wires.tmp matches 0 run return 0")

			raw("\$execute store result score #spent ra.wires.tmp run function ${RaLib.Transport.Net.take.asId()} {amount:5000,medium:\"\$(medium)\"}")
			raw("\$execute run function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
			raw("data modify entity @s data.status.drain_state set value \"placing\"")
		}

		pack.define(DrainExperience.drainPlaceTry) {
			// Internal: place into the first free side only.

			raw("execute if score #placed ra.wires.tmp matches 1 run return 0")

			raw("\$setblock ~ ~ ~ \$(block)")
			scoreboard.players.set(literal("#placed"), "ra.wires.tmp", 1)
		}
	}
}
