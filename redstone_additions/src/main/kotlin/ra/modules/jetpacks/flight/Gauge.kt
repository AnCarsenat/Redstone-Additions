package ra.modules.jetpacks.flight

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.predicates.providers.coerceIn
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.plus
import io.github.ayfri.kore.features.predicates.providers.unaryMinus
import io.github.ayfri.kore.functions.Function
import ra.core.COMPUTE_SELF
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.computeScore
import ra.core.define
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.core.thisScoreOf
import ra.modules.jetpacks.Jp

/**
 * The fuel gauge: `Fuel ▮▮▮▮▮▮▯▯▯▯ 63% · 12 coal` on the actionbar while a coal-burning jetpack
 * is in the air.
 *
 * WHAT IT MEASURES
 * An iron jetpack burns one piece of coal per [Engine]'s 2400 ticks of powered flight, taken from
 * the inventory when that time is up (ra.jp.fuel counts towards it). The bar is what is left of
 * the piece burning now; the count is the coal and charcoal carried, each another two minutes.
 * Green while there is coal to come, then yellow, and red once the last piece is under a quarter.
 *
 * WHEN IT IS DRAWN
 * The actionbar fades after about two seconds, so the gauge is redrawn once a second while it is
 * relevant: worn, switched on, in the air, not dry, not the infinite tier, and the player's "Jetpack
 * fuel gauge" setting on. Nothing is sent otherwise. Every other jetpack actionbar message sets
 * [Jp.GAUGE] to 40 ([Jp.HOLD_GAUGE]) and the gauge waits it out, so a mode switch or "kit fitted"
 * is not overwritten by the next refresh. No other part of the pack uses the actionbar.
 */
object Gauge {
	val gauge = ref(Ns.JETPACKS, "flight/gauge")
	/** Macro `{on, off, c}`: the filled and empty halves of the bar and its colour. */
	val show = ref(Ns.JETPACKS, "flight/gauge_show")

	/** Ticks between redraws. */
	private const val REFRESH = 20
	private const val SEGMENTS = 10
	private const val TICKS_PER_COAL = 2400

	private val t = Obj.TEMP

	fun Ra.registerGauge() {
		pack.define(gauge) { gauge() }
		pack.define(show) { show() }
	}

	/** As the player wearing a switched-on jetpack, every tick. Reads #jp.airborne ra.temp. */
	private fun Function.gauge() {
		raw("execute if score @s ${Jp.GAUGE} matches 1.. run scoreboard players remove @s ${Jp.GAUGE} 1")
		raw("execute if score @s ${Jp.GAUGE} matches 1.. run return 0")
		raw("execute unless score #jp.airborne $t matches 1 run return 0")
		raw("execute unless score @s ${Obj.USER_JETPACK_GAUGE} matches 1.. run return 0")
		raw("execute if entity @s[tag=${Jp.DRY}] run return 0")
		raw("""execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack_tier:"infinite"}}] run return 0""")
		raw("scoreboard players set @s ${Jp.GAUGE} $REFRESH")

		// Percent of the burning piece left, and the bar segments that rounds up to (any fuel at
		// all shows at least one).
		computeScore(
			"#jp.pct", t,
			provider { (-thisScoreOf(Jp.FUEL).floorDiv(TICKS_PER_COAL / 100) + 100).coerceIn(0, 100) },
			source = COMPUTE_SELF,
		)
		computeScore("#jp.seg", t, provider { (scoreOf("#jp.pct", t) + (100 / SEGMENTS - 1)).floorDiv(100 / SEGMENTS) })

		// `clear ... 0` counts without taking anything.
		raw("execute store result score #jp.coal $t run clear @s minecraft:coal 0")
		raw("execute store result score #jp.char $t run clear @s minecraft:charcoal 0")
		raw("scoreboard players operation #jp.coal $t += #jp.char $t")

		val gauge = "${Storage.JETPACKS.asString()} gauge"
		for (n in 0..SEGMENTS) {
			raw("""execute if score #jp.seg $t matches $n run data modify storage $gauge set value {on:"${"▮".repeat(n)}",off:"${"▯".repeat(SEGMENTS - n)}",c:"green"}""")
		}
		raw("""execute if score #jp.coal $t matches ..0 run data modify storage $gauge.c set value "yellow"""")
		raw("""execute if score #jp.coal $t matches ..0 if score #jp.pct $t matches ..25 run data modify storage $gauge.c set value "red"""")
		function(show, Storage.JETPACKS, "gauge")
	}

	private fun Function.show() {
		val pct = """{score:{name:"#jp.pct",objective:"$t"},color:"$(c)"},{text:"%",color:"$(c)"}"""
		val bar = """{text:"Fuel ",color:"gray"},{text:"$(on)",color:"$(c)"},{text:"$(off)",color:"dark_gray"},{text:" "},$pct,{text:" · ",color:"dark_gray"}"""
		raw("""${'$'}execute if score #jp.coal $t matches 1.. run title @s actionbar [$bar,{score:{name:"#jp.coal",objective:"$t"},color:"white"},{text:" coal",color:"gray"}]""")
		raw("""${'$'}execute unless score #jp.coal $t matches 1.. run title @s actionbar [$bar,{text:"no coal",color:"red"}]""")
	}
}
