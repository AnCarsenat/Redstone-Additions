package ra.modules.automation.absorber

import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.raw
import ra.modules.automation.absorber.XpAbsorber.readRadius

/**
 * One look of an [XpAbsorber], once a second from the automation clock. As the marker, at the block.
 *
 * NO COST WITHOUT ORBS. The first command is one selector for any orb within the admin maximum
 * radius; with none, the look ends there. Only then are the radius read and a network looked for.
 *
 * WHERE IT GOES. Like the Boiler and the Disenchanter, the Absorber is not a network node: it
 * offers to the network of a pipe, tank or drain touching one of its six faces, the first one with
 * room for at least one point. A network that holds something else and no experience is left
 * alone, as the Fluid Drain does.
 *
 * THE RATE. One point is [ML_PER_POINT] mL, the rate the Fluid Drain uses both ways and the
 * Disenchanter, Book Upgrader and Book Printer price in, so what an Absorber collects is worth
 * exactly what a player standing on a drain would have poured in.
 *
 * ONLY WHAT FITS. The free space is read before any orb is touched, and orbs are taken nearest
 * first ([MAX_ORBS] a look) while there is room: a whole orb when it fits, otherwise the part of
 * it that does (a smaller `Value`, or fewer orbs of a merged `Count`). The offer that follows is
 * covered by that read, so no experience is destroyed and none is made.
 */
internal object XpAbsorberRun {
	/** What one experience point is worth in a network: DrainExperience's rate. */
	const val ML_PER_POINT = 100
	/** Orbs looked at per look at most. */
	const val MAX_ORBS = 16

	private const val S = "ra_automation:absorber"
	private const val T = Obj.TEMP
	private const val TR = Obj.TR_TMP
	private const val ORB = "minecraft:experience_orb"

	/** On the neighbouring fluid node taking the experience, for one look. */
	private const val DST = "ra.auto.xa_dst"
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~")

	private fun fn(path: String) = XpAbsorber.block.fn(path)

	val run = fn("run")
	private val scan = fn("scan")
	private val findDst = fn("find_dst")
	private val absorb = fn("absorb")
	private val absorbAll = fn("absorb_all")
	private val absorbValue = fn("absorb_value")
	private val absorbCount = fn("absorb_count")
	private val offer = fn("offer")

	private fun state(text: String) = "data modify entity @s data.status.state set value \"$text\""

	fun Ra.registerXpAbsorberRun() {
		pack.define(run) {
			raw("execute unless entity @e[type=$ORB,distance=..${XpAbsorber.ADMIN_MAX_RADIUS},limit=1] run return run ${state("no experience around")}")
			readRadius()
			raw("execute store result storage $S a.r int 1 run scoreboard players get #prop $T")
			raw("function ${scan.asId()} with storage $S a")
			raw("data remove storage $S a")
		}

		// {r}: the radius, a number read from the block's property and capped.
		pack.define(scan) {
			raw("\$execute unless entity @e[type=$ORB,distance=..\$(r),limit=1] run return run ${state("no experience around")}")
			raw("scoreboard players set #xa.found $T 0")
			raw("scoreboard players set #xa.100 $T $ML_PER_POINT")
			for (at in SIDES) raw("execute positioned $at as @e[type=minecraft:marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ${findDst.asId()}")
			raw("execute if score #xa.found $T matches 0 run return run ${state("no room in a network beside it")}")

			raw("scoreboard players set #xa.got $T 0")
			raw("\$execute as @e[type=$ORB,distance=..\$(r),sort=nearest,limit=$MAX_ORBS] run function ${absorb.asId()}")

			raw("scoreboard players operation #xa.ml $T = #xa.got $T")
			raw("scoreboard players operation #xa.ml $T *= #xa.100 $T")
			raw("execute store result storage $S a.amount int 1 run scoreboard players get #xa.ml $T")
			raw("execute if score #xa.got $T matches 1.. as @e[type=minecraft:marker,tag=$DST,distance=..2,limit=1] run function ${offer.asId()} with storage $S a")
			raw("tag @e[type=minecraft:marker,tag=$DST,distance=..2] remove $DST")
			raw("execute if score #xa.got $T matches ..0 run return run ${state("no room in a network beside it")}")

			raw(state("absorbing"))
			raw("execute store result score #xa.n $T run data get entity @s data.status.absorbed")
			raw("scoreboard players operation #xa.n $T += #xa.got $T")
			raw("execute store result entity @s data.status.absorbed int 1 run scoreboard players get #xa.n $T")
			raw("particle minecraft:happy_villager ~ ~0.6 ~ 0.3 0.3 0.3 0 6 normal ${particleViewersSel()}")
			raw("playsound minecraft:entity.experience_orb.pickup block ${soundListenersSel(12)} ~ ~ ~ 0.4 1.2")
		}

		// As a neighbouring fluid node: the first with room for a point, and no other medium alone.
		pack.define(findDst) {
			raw("execute if score #xa.found $T matches 1 run return 0")
			raw("function ${RaLib.Transport.Net.read.asId()}")
			raw("execute store result score #xa.have $T run data get storage ra:transport cur.amounts.experience")
			raw("execute if score #net_amount $TR matches 1.. if score #xa.have $T matches ..0 run return 0")
			raw("scoreboard players operation #xa.fit $T = #net_capacity $TR")
			raw("scoreboard players operation #xa.fit $T -= #net_amount $TR")
			raw("scoreboard players operation #xa.fit $T /= #xa.100 $T")
			raw("execute if score #xa.fit $T matches ..0 run return 0")
			raw("scoreboard players set #xa.found $T 1")
			raw("tag @s add $DST")
		}

		// As an orb (not at it). #xa.fit: points the network still has room for.
		pack.define(absorb) {
			raw("execute if score #xa.fit $T matches ..0 run return 0")
			raw("execute store result score #xa.v $T run data get entity @s Value")
			raw("execute store result score #xa.c $T run data get entity @s Count")
			raw("execute if score #xa.c $T matches ..0 run scoreboard players set #xa.c $T 1")
			raw("execute if score #xa.v $T matches ..0 run return 0")
			raw("scoreboard players operation #xa.all $T = #xa.v $T")
			raw("scoreboard players operation #xa.all $T *= #xa.c $T")
			raw("execute if score #xa.all $T <= #xa.fit $T run return run function ${absorbAll.asId()}")
			raw("execute if score #xa.c $T matches 1 run return run function ${absorbValue.asId()}")
			raw("function ${absorbCount.asId()}")
		}

		pack.define(absorbAll) {
			raw("scoreboard players operation #xa.got $T += #xa.all $T")
			raw("scoreboard players operation #xa.fit $T -= #xa.all $T")
			raw("execute at @s run particle minecraft:happy_villager ~ ~0.2 ~ 0.1 0.1 0.1 0 2 normal ${particleViewersSel(32)}")
			raw("kill @s")
		}

		// One orb worth more than the room left: it keeps the rest.
		pack.define(absorbValue) {
			raw("scoreboard players operation #xa.got $T += #xa.fit $T")
			raw("scoreboard players operation #xa.v $T -= #xa.fit $T")
			raw("execute store result entity @s Value short 1 run scoreboard players get #xa.v $T")
			raw("scoreboard players set #xa.fit $T 0")
		}

		// Merged orbs (Count above 1): as many whole orbs as fit, the rest stay merged.
		pack.define(absorbCount) {
			raw("scoreboard players operation #xa.k $T = #xa.fit $T")
			raw("scoreboard players operation #xa.k $T /= #xa.v $T")
			raw("execute if score #xa.k $T matches ..0 run return 0")
			raw("scoreboard players operation #xa.c $T -= #xa.k $T")
			raw("execute store result entity @s Count int 1 run scoreboard players get #xa.c $T")
			raw("scoreboard players operation #xa.k $T *= #xa.v $T")
			raw("scoreboard players operation #xa.got $T += #xa.k $T")
			raw("scoreboard players operation #xa.fit $T -= #xa.k $T")
		}

		// As the chosen node. `amount` is an int this pack computed, never a player's text.
		pack.define(offer) {
			raw("\$return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"experience\"}")
		}
	}
}
