package ra.modules.gates.debug

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.forEachMarker
import ra.core.block.markerSel
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.gates.Clock

/**
 * `/function ra_gates:debug/clock`: reports every loaded Clock.
 *
 * Written because the Clock's symptom -- "it ignores its cooldown" -- has more than one cause
 * that looks identical from across the room, and guessing between them from source alone has
 * already been wrong twice.
 *
 * kept: beet contract. Nothing in the pack calls it; players run it by hand.
 */
object ClockDebug {
	val clock = ref(Ns.GATES, "debug/clock")
	val clockOne = ref(Ns.GATES, "debug/clock_one")

	fun Ra.registerClockDebug() {
		pack.define(clock) {
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Clocks",color:"aqua"}]""")
			raw("""execute unless entity ${Clock.block.markerSel()} run tellraw @s [{text:"  none loaded",color:"dark_gray"}]""")
			forEachMarker(Clock.block, clockOne)
		}
		pack.define(clockOne) { clockOne() }
	}

	/**
	 * One clock's report. As its marker, at the block.
	 *
	 * Reads the period exactly the way process does -- inline, not through a helper -- so this
	 * cannot agree with the code while the code disagrees with reality.
	 */
	private fun Function.clockOne() {
		val t = Obj.TEMP
		raw("data modify storage ra:temp dbg set value {}")
		raw("data modify storage ra:temp dbg.pos set from entity @s Pos")
		raw("data modify storage ra:temp dbg.props set from entity @s data.properties")

		raw("""tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:temp",color:"white"}]""")
		raw("""tellraw @s [{text:"   properties ",color:"gray"},{nbt:"dbg.props",storage:"ra:temp",color:"white"},{text:"   (quotes = still a string)",color:"dark_gray"}]""")

		raw("scoreboard players set #clk.max $t 20")
		raw("execute if data entity @s data.properties.cooldown store result score #clk.max $t run data get entity @s data.properties.cooldown 1")
		raw("execute if score #clk.max $t matches ..2 run scoreboard players set #clk.max $t 2")

		raw("""tellraw @s [{text:"   period in use ",color:"gray"},{score:{name:"#clk.max",objective:"$t"},color:"yellow"},{text:"   counter ",color:"gray"},{score:{name:"@s",objective:"${Obj.COOLDOWN}"},color:"aqua"}]""")

		raw("""execute unless data entity @s data.properties.cooldown run tellraw @s [{text:"   no `cooldown` property at all — running on the default",color:"red"}]""")
		raw("""execute if score #clk.max $t matches 2 run tellraw @s [{text:"   period is at the floor of 2 — either delay is 2 or under, or the read failed.",color:"yellow"}]""")

		raw("""execute if entity @s[tag=${Clock.ON}] run tellraw @s [{text:"   pulsing this tick",color:"green"}]""")
		raw("""execute unless entity @s[tag=${Clock.ON}] run tellraw @s [{text:"   idle between pulses",color:"dark_gray"}]""")

		// The output is iron blocks becoming redstone blocks. With neither touching it there is
		// nothing to convert and a working clock looks completely dead.
		val sides = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~1 ~", "~ ~-1 ~")
		val untouched = (sides.map { "unless block $it iron_block" } + sides.map { "unless block $it redstone_block" }).joinToString(" ")
		raw("""execute $untouched run tellraw @s [{text:"   no iron or redstone block touching it — the output has nothing to switch",color:"red"}]""")

		// More than one marker in the same block space means process runs more than once per tick
		// on the same counter, and the clock runs at a multiple of its rate.
		val tag = Clock.block.tag
		raw("""execute at @s if entity @e[type=marker,tag=$tag,distance=..0.4,limit=2,sort=nearest] unless entity @e[type=marker,tag=$tag,distance=..0.4,limit=1] run tellraw @s [{text:"   duplicate clock markers here",color:"red"}]""")
	}
}
