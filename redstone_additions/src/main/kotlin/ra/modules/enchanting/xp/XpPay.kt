package ra.modules.enchanting.xp

import io.github.ayfri.kore.commands.function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

/**
 * `ra_enchanting:xp/pay`: paying a price in liquid experience out of a fluid network next to a block.
 *
 * The Disenchanter and the Book Upgrader are not network nodes. Like the Boiler, they reach into
 * the network of a pipe, tank or drain touching them (the four sides and below: the top is where
 * items are thrown), so placing or breaking one never splits or joins a network.
 *
 * ALL OR NOTHING. `net/take` takes what it can, capped by what the network holds. A price paid in
 * part would leave the player poorer and the book unchanged, so the first neighbour whose network
 * holds the WHOLE price of experience is chosen before anything is taken; with none, nothing moves.
 */
object XpPay {
	/**
	 * Input `storage ra:enchant pay.amount` (mL, above 0). As a block's marker, at the block.
	 * Returns 1 when the whole amount was taken, 0 (nothing taken) otherwise.
	 */
	val pay = ref(Ns.ENCHANTING, "xp/pay")
	private val scan = ref(Ns.ENCHANTING, "xp/scan")
	private val take = ref(Ns.ENCHANTING, "xp/take")

	/** On the neighbouring node whose network pays, for one call of [pay]. */
	private const val SRC = "ra.ench.xp_src"
	private const val T = Obj.TEMP

	/** The neighbours a block draws from: the four sides and below. */
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~-1 ~")

	fun Ra.registerXpPay() {
		pack.define(pay) {
			raw("scoreboard players set #xp.found $T 0")
			raw("execute store result score #xp.price $T run data get storage ra:enchant pay.amount")
			raw("execute unless score #xp.price $T matches 1.. run return 0")
			for (at in SIDES) executeRun("positioned $at as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", scan)
			raw("execute if score #xp.found $T matches 0 run return 0")

			raw("scoreboard players set #xp.took $T 0")
			raw("execute as @e[type=marker,tag=$SRC,distance=..2,limit=1] store result score #xp.took $T run function ${take.asId()} with storage ra:enchant pay")
			raw("tag @e[type=marker,tag=$SRC,distance=..2] remove $SRC")
			raw("execute if score #xp.took $T = #xp.price $T run return 1")
			raw("return 0")
		}

		// As a neighbouring fluid node: claim it when its network holds the whole price.
		pack.define(scan) {
			raw("execute if score #xp.found $T matches 1 run return 0")
			function(RaLib.Transport.Net.read)
			// Experience, not the network's total: a mixed run counts only what is experience.
			raw("execute store result score #xp.have $T run data get storage ra:transport cur.amounts.experience")
			raw("execute unless score #xp.have $T >= #xp.price $T run return 0")
			raw("scoreboard players set #xp.found $T 1")
			raw("tag @s add $SRC")
		}

		// As the chosen node. `amount` is an int this pack computed, never a player's text.
		pack.define(take) {
			raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"experience\"}")
		}
	}
}
