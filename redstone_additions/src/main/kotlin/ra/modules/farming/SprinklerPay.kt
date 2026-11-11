package ra.modules.farming

import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

/**
 * The Sprinkler's fluid payments, out of a network beside it (the six faces), for `water` and
 * `experience`. In order, as the Sprinkler's marker at it:
 *
 *   1. `#spr.ok = find_<medium>()` with `#spr.need` set: the first neighbouring fluid node whose
 *      network holds the whole amount is tagged [SRC]; 0 when none does.
 *   2. [pay]: takes `#spr.need` from it, what was REALLY taken into `#spr.paid` (a network can change
 *      between the read and the take).
 *   3. [refund] `#spr.back` (above 0) to it, then [release].
 *
 * Every macro argument is an amount this pack computed.
 */
internal object SprinklerPay {
	private const val T = Obj.TEMP
	private const val S = "storage ra:farming spr"
	private const val SRC = "ra.farm.src"
	private const val SRC_SEL = "@e[type=marker,tag=$SRC,distance=..2,limit=1]"
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~-1 ~", "~ ~1 ~")
	val MEDIA = listOf("water", "experience")

	fun find(medium: String) = ref(Ns.FARMING, "blocks/sprinkler/find_$medium")
	private fun take(medium: String) = ref(Ns.FARMING, "blocks/sprinkler/take_$medium")
	private fun offer(medium: String) = ref(Ns.FARMING, "blocks/sprinkler/offer_$medium")

	fun Ra.registerSprinklerPay() {
		for (medium in MEDIA) {
			val scan = ref(Ns.FARMING, "blocks/sprinkler/scan_$medium")
			pack.define(find(medium)) {
				raw("scoreboard players set #spr.found $T 0")
				for (side in SIDES) executeRun("if score #spr.found $T matches 0 positioned $side as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", scan)
				raw("return run scoreboard players get #spr.found $T")
			}
			// As a neighbouring fluid node: claim it when its network holds the whole price.
			pack.define(scan) {
				raw("function ${RaLib.Transport.Net.read.asId()}")
				raw("execute store result score #spr.have $T run data get storage ra:transport cur.amounts.$medium")
				raw("execute unless score #spr.have $T >= #spr.need $T run return 0")
				raw("scoreboard players set #spr.found $T 1")
				raw("tag @s add $SRC")
			}
			// As the claimed node. Macro {amount}.
			pack.define(take(medium)) {
				raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"$medium\"}")
			}
			// As the claimed node: what was taken and not used goes back. Macro {back}.
			pack.define(offer(medium)) {
				raw("\$return run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(back),medium:\"$medium\"}")
			}
		}
	}

	fun Function.pay(medium: String) {
		raw("execute store result $S.amount int 1 run scoreboard players get #spr.need $T")
		raw("scoreboard players set #spr.paid $T 0")
		raw("execute as $SRC_SEL store result score #spr.paid $T run function ${take(medium).asId()} with $S")
	}

	fun Function.refund(medium: String) {
		raw("execute store result $S.back int 1 run scoreboard players get #spr.back $T")
		raw("execute if score #spr.back $T matches 1.. as $SRC_SEL run function ${offer(medium).asId()} with $S")
	}

	fun Function.release() = raw("tag @e[type=marker,tag=$SRC,distance=..2] remove $SRC")
}
