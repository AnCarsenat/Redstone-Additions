package ra.modules.wires.fluid

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.computeInto
import ra.core.computeScore
import ra.core.provider
import ra.core.scoreOf
import ra.core.storeInt
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
import ra.core.raw
import ra.core.ref

object DrainExperience : PackPart {
	/** What one experience point is worth in the network, both ways. */
	private const val ML_PER_POINT = 100

	/** Points moved per drain action, in either direction: one action's worth of mL. */
	private const val POINTS_PER_ACTION = Drain.ML_PER_ACTION / ML_PER_POINT

	val drainExp = ref(Ns.WIRES, "fluid/drain_exp")
	val drainExpClear = ref(Ns.WIRES, "fluid/drain_exp_clear")
	val drainExpOffer = ref(Ns.WIRES, "fluid/drain_exp_offer")
	val drainExpPull = ref(Ns.WIRES, "fluid/drain_exp_pull")
	val drainExpTake = ref(Ns.WIRES, "fluid/drain_exp_take")
	val drainPlaceExp = ref(Ns.WIRES, "fluid/drain_place_exp")
	val drainPlaceOrb = ref(Ns.WIRES, "fluid/drain_place_orb")

	override fun Ra.register() {
		pack.define(DrainExperience.drainExp) {
			// Draw a sneaking player's experience into the network.
			// Context: as a vertical drain marker, at the drain position.
			//
			// One experience point is 100 mL, and that rate is used in both directions: what
			// a player pours in is exactly what comes back out of a powered drain. The
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

			// One action's worth of points (Drain.ML_PER_ACTION / 100 = 50), or as much of that as
			// the player has and the grid will hold: pts = min(pts, 50, (capacity - amount) / 100),
			// at 100 mL a point. The same figure as pouring out, so `rate` means one thing.
			computeScore(
				"#dr.pts", "ra.wires.tmp",
				provider {
					min(
						scoreOf("#dr.pts", "ra.wires.tmp"),
						constant(POINTS_PER_ACTION),
						floorDiv(sub(scoreOf("#net_capacity", "ra.tr.tmp"), scoreOf("#net_amount", "ra.tr.tmp")), constant(ML_PER_POINT)),
					)
				},
			)

			raw("execute if score #dr.pts ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value \"network_full\"")
			executeReturnRun("if score #dr.pts ra.wires.tmp matches ..0", DrainExperience.drainExpClear)

			// Charge the grid for exactly the points about to be taken, so neither side can
			// end up owing the other a fraction of a point.
			computeInto(storeInt(Storage.WIRES, "xp.amount"), provider { mul(scoreOf("#dr.pts", "ra.wires.tmp"), constant(ML_PER_POINT)) })
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

		pack.define(DrainExperience.drainPlaceExp) {
			// Give experience back to the world as orbs.
			//
			// Context: as the drain marker, at the block in front of it (ra_wires:fluid/drain_pour).
			//
			// The same 100 mL a point that drain_exp charges, so a network filled by standing
			// on one drain empties at exactly the rate it filled -- 50 points an action. What
			// went in comes back out; the network is not a lossy converter.

			function(RaLib.Transport.Net.read)

			// 50 points, in whole points only, and never more than the network holds OF
			// EXPERIENCE: pts = min(50, experience / 100). The network's total is the wrong
			// figure on a mixed run -- 300 mL of experience beside 5000 mL of water would have
			// minted ten points' worth of orbs while the take below removed only three.
			raw("execute store result score #dx.have ra.wires.tmp run data get storage ra:transport cur.amounts.experience")
			computeScore("#dr.pts", "ra.wires.tmp", provider { min(constant(POINTS_PER_ACTION), floorDiv(scoreOf("#dx.have", "ra.wires.tmp"), constant(ML_PER_POINT))) })
			raw("execute if score #dr.pts ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value \"network_empty\"")

			computeInto(storeInt(Storage.WIRES, "xp.amount"), provider { mul(scoreOf("#dr.pts", "ra.wires.tmp"), constant(ML_PER_POINT)) })
			function(DrainExperience.drainExpPull, Storage.WIRES, "xp")

			raw("execute store result storage ra:wires xp.points int 1 run scoreboard players get #dr.pts ra.wires.tmp")
			function(DrainExperience.drainPlaceOrb, Storage.WIRES, "xp")

			raw("data modify entity @s data.status.drain_state set value \"pouring\"")
			function(Fluid.particles, nbt { this["medium"] = "experience" })
		}

		pack.define(DrainExperience.drainPlaceOrb) {
			// Internal: drop one orb carrying the whole cycle's worth.
			// Context: as the drain marker, at the block in front of it.
			//
			// One orb rather than one per point: a stack of single-point orbs is the same
			// experience but a great deal more entity, and vanilla merges them anyway.

			raw("\$summon minecraft:experience_orb ~ ~ ~ {Value:\$(points)s}")
		}
	}
}
