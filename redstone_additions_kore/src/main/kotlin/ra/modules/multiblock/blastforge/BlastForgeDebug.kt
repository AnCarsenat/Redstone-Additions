package ra.modules.multiblock.blastforge

import io.github.ayfri.kore.DataPack
import ra.core.Ns
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * `ra_multiblock:blast_forge/debug_structure`: checks each block and reports. Macro: `bf_dir.<facing>`.
 * As the marker, at the base. Run it by hand:
 *
 *     execute as @e[type=marker,tag=ra.multiblock.blast_forge,limit=1] at @s run function ra_multiblock:blast_forge/debug_structure with storage ra:multiblock bf_dir.north
 */
object BlastForgeDebug {
	val debugStructure = ref(Ns.MULTIBLOCK, "blast_forge/debug_structure")

	private fun ok(key: String) = """tellraw @a [{text:"  $key $($key): ",color:"gray"},{text:"OK",color:"green"}]"""
	private fun fail(key: String) = """tellraw @a [{text:"  $key $($key): ",color:"gray"},{text:"FAIL",color:"red"}]"""

	fun register(pack: DataPack) = pack.define(debugStructure) {
		raw("""${'$'}tellraw @a [{text:"[MB Debug] ",color:"gold"},{text:"Checking $(facing) structure:",color:"aqua"}]""")

		// Functional blocks. The furnace's failure line also shows what is there instead.
		raw("\$execute if block $(furnace) minecraft:blast_furnace run ${ok("furnace")}")
		raw("""${'$'}execute unless block $(furnace) minecraft:blast_furnace run tellraw @a [{text:"  furnace $(furnace): ",color:"gray"},{text:"FAIL",color:"red"},{text:" (found: "},{text:"",nbt:"",block:"$(furnace)",interpret:false,color:"yellow"},{text:")"}]""")
		for (k in listOf("in1", "in2", "out1")) {
			raw("\$execute if block $($k) minecraft:barrel run ${ok(k)}")
			raw("\$execute unless block $($k) minecraft:barrel run ${fail(k)}")
		}

		// Corner bricks, then spot checks of the floor and ceiling (first, middle, last).
		for (k in listOf("b1", "b2", "b3", "b4", "f1", "f5", "f9", "c1", "c5", "c9")) {
			raw("\$execute if block $($k) ${BlastForge.BRICKS} run ${ok(k)}")
			raw("\$execute unless block $($k) ${BlastForge.BRICKS} run ${fail(k)}")
		}

		raw("""tellraw @a [{text:"[MB Debug] ",color:"gold"},{text:"Done. See above for results.",color:"gray"}]""")
	}
}
