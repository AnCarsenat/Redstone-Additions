package ra.modules.wires.diving

import io.github.ayfri.kore.arguments.numbers.ticks
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.schedule
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.floorMod
import io.github.ayfri.kore.features.predicates.providers.plus
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.computeScore
import ra.core.define
import ra.core.defineProvider
import ra.core.namedProvider
import ra.core.executeRun
import ra.core.provider
import ra.core.raw
import ra.core.ref
import ra.core.scoreOf
import ra.modules.wires.diving.DivingItems.AIR_MAX
import ra.modules.wires.diving.DivingItems.MODS_CHARGED
import ra.modules.wires.diving.DivingItems.MODS_EMPTY
import ra.modules.wires.diving.DivingItems.TANK_LORE_HINT
import ra.modules.wires.diving.DivingItems.TANK_LORE_TOP

/**
 * The Air Tank at work: once a second, for each player wearing one with their head in water, one
 * second of air is spent and the tank rewritten; the actionbar gauge shows what is left.
 *
 * ONCE A SECOND, NEVER PER TICK. [second] is a self-rescheduling 20-tick schedule started at the
 * module load: a single `@a` item test a second while nobody dives, and nothing per tick. The
 * breath itself is the tank's `oxygen_bonus` modifier ([DivingItems]), which the game applies
 * every tick on its own; this only counts down and, at zero, takes the modifier off the item.
 *
 * NOT SPENT WHEN YOU BREATHE ANYWAY. With Water Breathing (a potion, the Fluid Drain's oxygen) or
 * Conduit Power, the tank keeps its air.
 *
 * `air` in the tank's custom data is in SECONDS, 0..[AIR_MAX] (fifteen minutes is 18000 ticks).
 */
object AirTank : PackPart {
	private const val T = "ra.wires.tmp"
	private const val S = "storage ra:wires at"
	private const val SEGMENTS = 10

	/** The player's head (eyes) is in water. Run `anchored eyes positioned ^ ^ ^`. */
	private const val IN_WATER = """{type:"minecraft:location_check",predicate:{fluid:{fluids:"#minecraft:water"}}}"""
	private const val BREATHES_ANYWAY = """{type:"minecraft:any_of",terms:[{type:"minecraft:entity_properties",entity:"this",predicate:{"minecraft:effects":{"minecraft:water_breathing":{}}}},{type:"minecraft:entity_properties",entity:"this",predicate:{"minecraft:effects":{"minecraft:conduit_power":{}}}}]}"""

	/** `items` test for a worn tank. */
	const val WORN = "items entity @s armor.chest *[minecraft:custom_data~{ra:{air_tank:1b}}]"

	val second = ref(Ns.WIRES, "diving/second")

	/** As the wearer: `#at.air ra.wires.tmp` from the worn tank. */
	val read = ref(Ns.WIRES, "diving/read")
	private val tank = ref(Ns.WIRES, "diving/tank")

	/** As the wearer: writes `#at.air ra.wires.tmp` into the worn tank (custom data, lore, breath). */
	val write = ref(Ns.WIRES, "diving/write")
	private val writeRun = ref(Ns.WIRES, "diving/write_run")

	/** As the wearer: draws the gauge from `#at.air ra.wires.tmp`, if the player has it on. */
	val gauge = ref(Ns.WIRES, "diving/gauge")
	private val gaugeShow = ref(Ns.WIRES, "diving/gauge_show")

	/** In the module load: start the clock (replacing any run a reload left scheduled). */
	fun Function.startClock() = schedule(second).replace(20.ticks)

	private fun Function.readAir() =
		raw("""execute store result score #at.air $T run data get entity @s equipment.chest.components."minecraft:custom_data".ra.air""")

	/**
	 * Named providers `ra_wires:diving/air_minutes` and `air_seconds`: `#at.air` (ra.wires.tmp,
	 * seconds of air) split into whole minutes and the seconds left over. The tank's lore and the
	 * actionbar gauge both show `7m 48s`, from the same two files.
	 */
	private val AIR_MINUTES = namedProvider(Ns.WIRES, "diving/air_minutes")
	private val AIR_SECONDS = namedProvider(Ns.WIRES, "diving/air_seconds")

	override fun Ra.register() {
		pack.defineProvider(AIR_MINUTES, provider { scoreOf("#at.air", T).floorDiv(60) })
		pack.defineProvider(AIR_SECONDS, provider { scoreOf("#at.air", T).floorMod(60) })
		pack.define(read) { readAir() }

		pack.define(second) {
			schedule(second).replace(20.ticks)
			executeRun("as @a if $WORN at @s", tank)
		}

		pack.define(tank) {
			raw("execute anchored eyes positioned ^ ^ ^ unless predicate $IN_WATER run return 0")
			readAir()
			// Rewritten only when a second was spent; the last one takes the breath off with it.
			executeRun("if predicate $BREATHES_ANYWAY", gauge)
			raw("execute if predicate $BREATHES_ANYWAY run return 0")
			executeRun("if score #at.air $T matches ..0", gauge)
			raw("execute if score #at.air $T matches ..0 run return 0")
			raw("scoreboard players remove #at.air $T 1")
			function(write)
			function(gauge)
		}

		pack.define(write) {
			raw("execute if score #at.air $T matches ..-1 run scoreboard players set #at.air $T 0")
			raw("execute if score #at.air $T matches ${AIR_MAX + 1}.. run scoreboard players set #at.air $T $AIR_MAX")
			raw("data remove $S")
			raw("execute store result $S.air int 1 run scoreboard players get #at.air $T")
			computeScore("#at.m", T, AIR_MINUTES)
			computeScore("#at.s", T, AIR_SECONDS)
			raw("execute store result $S.m int 1 run scoreboard players get #at.m $T")
			raw("execute store result $S.s int 1 run scoreboard players get #at.s $T")
			function(writeRun, Storage.WIRES, "at")
			raw("data remove $S")
		}

		// Macro {air, m, s}. The whole component is set, never merged: the breath goes on and off with it.
		pack.define(writeRun) {
			val airLine = """{text:"Air: ",color:"gray",italic:false,extra:[{text:"${'$'}(m)m ${'$'}(s)s",color:"aqua"},{text:" / ${AIR_MAX / 60}m",color:"dark_gray"}]}"""
			val emptyLine = """{text:"Air: ",color:"gray",italic:false,extra:[{text:"empty",color:"red"}]}"""
			val data = """{type:"minecraft:set_custom_data",tag:{ra:{air:${'$'}(air)}}}"""
			raw("""${'$'}execute if score #at.air $T matches 1.. run item modify entity @s armor.chest [$data,{type:"minecraft:set_components",components:{"minecraft:lore":[$TANK_LORE_TOP,$airLine,$TANK_LORE_HINT],"minecraft:attribute_modifiers":$MODS_CHARGED}}]""")
			raw("""${'$'}execute if score #at.air $T matches ..0 run item modify entity @s armor.chest [$data,{type:"minecraft:set_components",components:{"minecraft:lore":[$TANK_LORE_TOP,$emptyLine,$TANK_LORE_HINT],"minecraft:attribute_modifiers":$MODS_EMPTY}}]""")
		}

		pack.define(gauge) { gauge() }
		pack.define(gaugeShow) { gaugeShow() }
	}

	/**
	 * `Air ▮▮▮▮▮▯▯▯▯▯ 52% · 7m 48s`, aqua, then yellow under a quarter and red under a tenth;
	 * `Air tank empty` once it is. Drawn once a second, which is what keeps it on the actionbar.
	 */
	private fun Function.gauge() {
		raw("execute unless score @s ${Obj.USER_AIR_GAUGE} matches 1.. run return 0")
		raw("""execute if score #at.air $T matches ..0 run return run title @s actionbar [{text:"Air tank empty",color:"red"}]""")
		computeScore("#at.pct", T, provider { (scoreOf("#at.air", T) + (AIR_MAX / 100 - 1)).floorDiv(AIR_MAX / 100) })
		computeScore("#at.seg", T, provider { (scoreOf("#at.pct", T) + (100 / SEGMENTS - 1)).floorDiv(100 / SEGMENTS) })
		computeScore("#at.m", T, AIR_MINUTES)
		computeScore("#at.s", T, AIR_SECONDS)
		val g = "$S.gauge"
		for (n in 0..SEGMENTS) {
			raw("""execute if score #at.seg $T matches $n run data modify $g set value {on:"${"▮".repeat(n)}",off:"${"▯".repeat(SEGMENTS - n)}",c:"aqua"}""")
		}
		raw("""execute if score #at.pct $T matches ..25 run data modify $g.c set value "yellow"""")
		raw("""execute if score #at.pct $T matches ..10 run data modify $g.c set value "red"""")
		function(gaugeShow, Storage.WIRES, "at.gauge")
		raw("data remove $g")
	}

	private fun Function.gaugeShow() {
		val score = { name: String, color: String -> """{score:{name:"$name",objective:"$T"},color:"$color"}""" }
		raw(
			"""${'$'}title @s actionbar [{text:"Air ",color:"gray"},{text:"${'$'}(on)",color:"${'$'}(c)"},{text:"${'$'}(off)",color:"dark_gray"},{text:" "},""" +
				"""${score("#at.pct", "\$(c)")},{text:"%",color:"${'$'}(c)"},{text:" · ",color:"dark_gray"},""" +
				"""${score("#at.m", "white")},{text:"m ",color:"gray"},${score("#at.s", "white")},{text:"s",color:"gray"}]""",
		)
	}
}
