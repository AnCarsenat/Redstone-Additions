package ra.modules.wires.fluid

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

/**
 * A redstone-powered Liquid Drain pouring its network's contents out into the world. Nothing
 * needs to be there already: no water body, no container, no bank -- air in front of it is enough.
 *
 * WHERE
 * Only the face the drain points at (ra.facing), like a dispenser: a drain stood on the floor
 * faces up and pours on top of itself, one on a wall pours in front. No fallback to the other
 * sides -- a drain that sometimes poured left and sometimes behind would flood whatever the
 * player built there, and the face it uses is the one they can see.
 *
 * WHAT
 * The network's primary medium (ra:transport cur.medium), looked up in the media registry:
 * - `block` (water, lava, powder snow): one source block, 5000 mL, into the block in front.
 * - `orb` (experience): one experience orb, at 100 mL a point -- ra_wires:fluid/drain_place_exp.
 * - `effect` (potions): a cloud in the potion's colour, applied to the players around it --
 *   ra_wires:fluid/drain_potion.
 * - `pour` (milk, steam, smoke, oxygen, toxic gas): no world form, so it is poured away as that particle.
 *   The contents really leave the network; the particle is only what a player sees. A medium with
 *   an entry in `storage ra:wires gas_effects` ({effect, seconds, amplifier, area}) also gives
 *   that effect: `area:"column"` to whoever is in the water straight above the front (oxygen's
 *   water breathing, bubbling up like a magma block's column), anything else to every living
 *   entity within [VENT_RADIUS] of it (toxic gas's wither). That is what makes the drain a gas
 *   vent; another pack adds its own gas there.
 * Each action moves up to Drain.ML_PER_ACTION, so the drain's `rate` reads the same for all.
 *
 * NO SOUND. A drain is a machine that runs every second; it does not play a bucket each time.
 */
object DrainPour : PackPart {
	val pour = ref(Ns.WIRES, "fluid/drain_pour")
	val pourAt = ref(Ns.WIRES, "fluid/drain_pour_at")
	val pourBlock = ref(Ns.WIRES, "fluid/drain_pour_block")
	val pourPlace = ref(Ns.WIRES, "fluid/drain_pour_place")
	val pourRefund = ref(Ns.WIRES, "fluid/drain_pour_refund")
	val pourSizzle = ref(Ns.WIRES, "fluid/drain_pour_sizzle")
	val pourAway = ref(Ns.WIRES, "fluid/drain_pour_away")
	val pourAwayShow = ref(Ns.WIRES, "fluid/drain_pour_away_show")
	val pourAwayEffect = ref(Ns.WIRES, "fluid/drain_pour_away_effect")
	val pourAwayColumn = ref(Ns.WIRES, "fluid/drain_pour_away_column")

	/** How far a vented gas's effect reaches from the drain's front (`area` "radius", the default): the 4 blocks of a poured potion. */
	const val VENT_RADIUS = 4

	/** How many blocks of water an `area:"column"` effect climbs above the front at most. */
	const val VENT_COLUMN = 16

	private const val VOLUME = Drain.ML_PER_ACTION

	override fun Ra.register() {
		pack.define(DrainPour.pour) {
			// Spend network contents putting the medium back into the world.
			// Context: as a redstone-powered drain marker, at the drain position.

			function(RaLib.Transport.Net.read)
			raw("execute unless data storage ra:transport cur.medium run return run data modify entity @s data.status.drain_state set value \"network_empty\"")

			// The face it points at. ra.facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
			for ((f, pos) in listOf(0 to "~ ~-1 ~", 1 to "~ ~1 ~", 2 to "~ ~ ~-1", 3 to "~ ~ ~1", 4 to "~-1 ~ ~", 5 to "~1 ~ ~")) {
				executeRun("if score @s ra.facing matches $f positioned $pos", DrainPour.pourAt, "with storage ra:transport cur")
			}
		}

		pack.define(DrainPour.pourAt) {
			// Internal: dispatch on what the medium is. Macro: ra:transport cur ({medium,...}).
			// Context: as the drain marker, at the block in front of it.
			//
			// Potions and experience have their own amounts (a bottle is 1000 mL, a point 100 mL),
			// so they are not held back by the 5000 mL a block costs. That gate used to sit in
			// front of everything, and a network holding a single potion never poured it.

			raw("\$execute if data storage ra:wires media.\$(medium).block run return run function ${DrainPour.pourBlock.asId()} with storage ra:transport cur")
			raw("\$execute if data storage ra:wires media.\$(medium).orb run return run function ${DrainExperience.drainPlaceExp.asId()}")
			raw("\$execute if data storage ra:wires media.\$(medium).effect run return run function ${DrainPotion.drainPotion.asId()}")
			raw("\$execute if data storage ra:wires media.\$(medium).pour run return run function ${DrainPour.pourAway.asId()} with storage ra:transport cur")
			// A medium registered with none of those (one a future version adds and forgets).
			raw("data modify entity @s data.status.drain_state set value \"cannot_place_medium\"")
		}

		pack.define(DrainPour.pourBlock) {
			// Internal: put one source block of the medium in front, if it has room for one.
			// Macro: {medium}. Context: as the drain marker, at the block in front of it.

			// A whole block of THIS medium, not of the network's total: take caps at what the
			// network holds of the medium, so 3000 mL of water beside 3000 mL of lava is not a
			// water block.
			raw("\$execute store result score #pr.have ra.wires.tmp run data get storage ra:transport cur.amounts.\$(medium)")
			raw("execute if score #pr.have ra.wires.tmp matches ..${VOLUME - 1} run return run data modify entity @s data.status.drain_state set value \"not_enough\"")

			// Room: air; anything a bucket would wash away (grass, snow layers, flowers -- the
			// vanilla #replaceable tag) except liquids; or flowing liquid of the same medium,
			// which becomes a source. A source already there, or another liquid, is `blocked`:
			// placing into an existing source would charge the network for nothing, and water
			// onto lava is not this block's business.
			scoreboard.players.set(literal("#pr.ok"), "ra.wires.tmp", 0)
			raw("execute if block ~ ~ ~ #minecraft:air run scoreboard players set #pr.ok ra.wires.tmp 1")
			raw("execute if block ~ ~ ~ #minecraft:replaceable unless block ~ ~ ~ minecraft:water unless block ~ ~ ~ minecraft:lava unless block ~ ~ ~ minecraft:bubble_column run scoreboard players set #pr.ok ra.wires.tmp 1")
			for (liquid in listOf("water", "lava")) {
				raw("\$execute if data storage ra:wires media.\$(medium){block:\"minecraft:$liquid\"} if block ~ ~ ~ minecraft:$liquid unless block ~ ~ ~ minecraft:$liquid[level=0] run scoreboard players set #pr.ok ra.wires.tmp 1")
			}
			raw("execute if score #pr.ok ra.wires.tmp matches 0 run return run data modify entity @s data.status.drain_state set value \"blocked\"")

			data(Storage.WIRES) { remove("pour") }
			raw("\$data modify storage ra:wires pour set value {medium:\"\$(medium)\"}")
			raw("\$data modify storage ra:wires pour.block set from storage ra:wires media.\$(medium).block")
			function(DrainPour.pourPlace, Storage.WIRES, "pour")
			data(Storage.WIRES) { remove("pour") }
		}

		pack.define(DrainPour.pourPlace) {
			// Internal: charge the network, then place. Macro: {medium, block}.
			//
			// Paid for first, placed second, refunded if the block did not go down: the network
			// is never charged for a block that is not there, and no block appears that the
			// network did not pay for in full.

			raw("\$execute store result score #pr.got ra.wires.tmp run function ${RaLib.Transport.Net.take.asId()} {amount:$VOLUME,medium:\"\$(medium)\"}")
			raw("execute store result storage ra:wires pour.got int 1 run scoreboard players get #pr.got ra.wires.tmp")
			raw("execute if score #pr.got ra.wires.tmp matches 1..${VOLUME - 1} run function ${DrainPour.pourRefund.asId()} with storage ra:wires pour")
			raw("execute if score #pr.got ra.wires.tmp matches ..${VOLUME - 1} run return run data modify entity @s data.status.drain_state set value \"not_enough\"")

			// Water boils away where a bucket's would: the Nether, or any dimension whose
			// environment says so. Commands ignore that rule, so it is applied here, the vanilla
			// way: the water is spent (already taken above) and nothing is placed.
			raw("\$execute if data storage ra:wires media.\$(medium){block:\"minecraft:water\"} if predicate {condition:\"minecraft:environment_attribute_check\",attribute:\"minecraft:gameplay/water_evaporates\",value:true} run return run function ${DrainPour.pourSizzle.asId()}")

			// `destroy` drops what it replaces (grass, a snow layer), as a bucket does.
			raw("\$execute store success score #pr.set ra.wires.tmp run setblock ~ ~ ~ \$(block) destroy")
			raw("execute if score #pr.set ra.wires.tmp matches 0 run function ${DrainPour.pourRefund.asId()} with storage ra:wires pour")
			raw("execute if score #pr.set ra.wires.tmp matches 0 run return run data modify entity @s data.status.drain_state set value \"blocked\"")

			raw("data modify entity @s data.status.drain_state set value \"pouring\"")
			raw("\$function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
		}

		pack.define(DrainPour.pourRefund) {
			// Internal: give back what was taken for a block that was not placed.
			// Macro: {medium, got}.

			raw("\$function ${RaLib.Transport.Net.offer.asId()} {amount:\$(got),medium:\"\$(medium)\"}")
		}

		pack.define(DrainPour.pourSizzle) {
			// Internal: water poured where it evaporates -- it is gone, as from a bucket.
			// Context: at the block in front.

			raw("data modify entity @s data.status.drain_state set value \"evaporates\"")
			raw("particle minecraft:large_smoke ~ ~0.3 ~ 0.3 0.2 0.3 0.01 6 normal ${particleViewersSel()}")
			raw("playsound minecraft:block.fire.extinguish block ${soundListenersSel(8)} ~ ~ ~ 0.5 2.6")
			raw("return 1")
		}

		pack.define(DrainPour.pourAway) {
			// Internal: pour a medium with no world form away, as its `pour` particle.
			// Macro: {medium}. Context: as the drain marker, at the block in front of it.
			//
			// Up to one action's worth, or what is left of it: there is no whole unit to wait
			// for, unlike a block. Taken for real -- the particle is the only trace of it.

			raw("\$execute store result score #pr.got ra.wires.tmp run function ${RaLib.Transport.Net.take.asId()} {amount:$VOLUME,medium:\"\$(medium)\"}")
			raw("execute if score #pr.got ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value \"network_empty\"")
			// A gas is vented, anything else poured: the status says which.
			raw("data modify entity @s data.status.drain_state set value \"pouring\"")
			raw("\$execute if data storage ra:wires media.\$(medium){state:\"gas\"} run data modify entity @s data.status.drain_state set value \"venting\"")
			// The effect, over the entry's `area`: "column" is the water straight above the front
			// (oxygen, like a magma block's bubbles), anything else the VENT_RADIUS sphere.
			raw("data remove storage ra:wires vent")
			raw("\$data modify storage ra:wires vent set from storage ra:wires gas_effects.\$(medium)")
			raw("scoreboard players set #pr.col ra.wires.tmp 0")
			raw("execute if data storage ra:wires vent{area:\"column\"} align xyz run function ${DrainPour.pourAwayColumn.asId()} with storage ra:wires vent")
			raw("execute if data storage ra:wires vent unless data storage ra:wires vent{area:\"column\"} run function ${DrainPour.pourAwayEffect.asId()} with storage ra:wires vent")
			raw("data remove storage ra:wires vent")
			raw("\$function ${DrainPour.pourAwayShow.asId()} with storage ra:wires media.\$(medium)")
		}

		pack.define(DrainPour.pourAwayEffect) {
			// Internal: the vented medium's effect around the front. Macro: a gas_effects entry
			// ({effect, seconds, amplifier}). `effect give` skips anything that is not living.
			raw("\$effect give @e[distance=..$VENT_RADIUS] \$(effect) \$(seconds) \$(amplifier)")
		}

		pack.define(DrainPour.pourAwayColumn) {
			// Internal: the effect in one block of the water column above the front, then the next
			// one up, until the water ends or VENT_COLUMN blocks. Macro: a gas_effects entry.
			// Context: at the block's minimum corner (align xyz), so dx=0 is that block.
			raw("execute unless block ~ ~ ~ minecraft:water unless block ~ ~ ~ minecraft:bubble_column run return 0")
			raw("\$effect give @e[dx=0,dy=0,dz=0] \$(effect) \$(seconds) \$(amplifier)")
			raw("particle minecraft:bubble ~0.5 ~0.5 ~0.5 0.25 0.35 0.25 0.05 4 normal ${particleViewersSel(32)}")
			raw("scoreboard players add #pr.col ra.wires.tmp 1")
			raw("execute if score #pr.col ra.wires.tmp matches $VENT_COLUMN.. run return 0")
			raw("execute positioned ~ ~1 ~ run function ${DrainPour.pourAwayColumn.asId()} with storage ra:wires vent")
		}

		pack.define(DrainPour.pourAwayShow) {
			// Internal: the particle. Macro: a media registry entry ({pour,...}).

			raw("\$particle \$(pour) ~ ~ ~ 0.35 0.35 0.35 0.02 16 normal ${particleViewersSel(16)}")
		}
	}
}
