package ra.modules.infinite.debug

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.markers
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.infinite.FLOWER_GROUND
import ra.modules.infinite.GROWABLE
import ra.modules.infinite.PoppyGenerator

/**
 * `/function ra_infinite:debug/poppy`: print what every Poppy Generator sees.
 *
 * Static reading of the code only gets so far -- this reports the runtime values: where the
 * marker is, which way it is turned, what the block in front is, and which of the ground rules
 * match.
 */
object PoppyDebug {
	val poppy = ref(Ns.INFINITE, "debug/poppy")
	private val poppyOne = ref(Ns.INFINITE, "debug/poppy_one")

	fun Ra.registerPoppyDebug() {
		pack.define(poppy) {
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Poppy Generator report",color:"aqua"}]""")
			execute {
				asTarget(markers(PoppyGenerator.generator.block))
				at(self())
				run { function(poppyOne) }
			}
		}
		pack.define(poppyOne) { poppyOne() }
	}

	/** One generator's report. As its marker, at the marker. */
	private fun Function.poppyOne() {
		// Where it is and how it is turned.
		raw("data modify storage ra:debug poppy set value {}")
		raw("data modify storage ra:debug poppy.pos set from entity @s Pos")
		raw("data modify storage ra:debug poppy.rot set from entity @s Rotation")
		raw("data modify storage ra:debug poppy.props set from entity @s data.properties")

		raw("execute store result score #dbg.facing ${Obj.TEMP} run scoreboard players get @s ${Obj.FACING}")
		raw("execute store result score #dbg.cd ${Obj.TEMP} run scoreboard players get @s ${Obj.COOLDOWN}")

		raw("""tellraw @p [{text:"— marker ",color:"gray"},{nbt:"poppy.pos",storage:"ra:debug",color:"white"},{text:" rot ",color:"gray"},{nbt:"poppy.rot",storage:"ra:debug",color:"white"}]""")
		raw("""tellraw @p [{text:"  props ",color:"gray"},{nbt:"poppy.props",storage:"ra:debug",color:"white"},{text:"  facing ",color:"gray"},{score:{name:"#dbg.facing",objective:"ra.temp"},color:"white"},{text:"  cooldown ",color:"gray"},{score:{name:"#dbg.cd",objective:"ra.temp"},color:"white"}]""")

		// What the block one step forward looks like.
		raw("""execute positioned ^ ^ ^1 if block ~ ~ ~ $GROWABLE run tellraw @p [{text:"  front: ",color:"gray"},{text:"growable",color:"green"}]""")
		raw("""execute positioned ^ ^ ^1 unless block ~ ~ ~ $GROWABLE run tellraw @p [{text:"  front: ",color:"gray"},{text:"blocked",color:"red"}]""")
		raw("""execute positioned ^ ^ ^1 if block ~ ~-1 ~ $FLOWER_GROUND run tellraw @p [{text:"  below front: ",color:"gray"},{text:"flower_ground",color:"green"}]""")
		raw("""execute positioned ^ ^ ^1 unless block ~ ~-1 ~ $FLOWER_GROUND run tellraw @p [{text:"  below front: ",color:"gray"},{text:"not soil",color:"red"}]""")
		raw("""execute positioned ^ ^ ^1 if block ~ ~ ~ #minecraft:dirt run tellraw @p [{text:"  front is #minecraft:dirt",color:"dark_gray"}]""")
		raw("""execute positioned ^ ^ ^1 if block ~ ~-1 ~ #minecraft:dirt run tellraw @p [{text:"  below front is #minecraft:dirt",color:"dark_gray"}]""")
		raw("""execute positioned ^ ^ ^1 if block ~ ~-1 ~ minecraft:grass_block run tellraw @p [{text:"  below front is literally grass_block",color:"dark_gray"}]""")

		// And the verdict the goggles use, over the whole 3x3.
		scoreboard.players.set(literal("#poppy.ground"), Obj.TEMP, 0)
		execute {
			positioned(vec3(0, 0, 1).local)
			run { function(PoppyGenerator.checkGround) }
		}
		raw("""tellraw @p [{text:"  ground verdict: ",color:"gray"},{score:{name:"#poppy.ground",objective:"ra.temp"},color:"yellow"},{text:"  (1 = will plant)",color:"dark_gray"}]""")
	}
}
