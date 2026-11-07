package ra.modules.wires.debug

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.line

object Debug : PackPart {
	val electric = ref(Ns.WIRES, "debug/electric")
	val electricOne = ref(Ns.WIRES, "debug/electric_one")
	val generator = ref(Ns.WIRES, "debug/generator")
	val generatorLookup = ref(Ns.WIRES, "debug/generator_lookup")
	val generatorOne = ref(Ns.WIRES, "debug/generator_one")
	val light = ref(Ns.WIRES, "debug/light")
	val lightOne = ref(Ns.WIRES, "debug/light_one")

	override fun Ra.register() {
		pack.define(Debug.electric) {
			// Report every electric node: /function ra_wires:debug/electric
			//
			// Which grid each node is on and what that grid holds. Two nodes you believe are
			// wired together but that report different grid ids are the whole diagnosis.

			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Electric nodes",color:"aqua"}]""")
			raw("""execute unless entity @e[type=marker,tag=ra.wires.electric_node] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]""")
			executeRun("as @e[type=marker,tag=ra.wires.electric_node] at @s", Debug.electricOne)
		}

		pack.define(Debug.electricOne) {
			// One node's report. Context: as its marker, at the block.
			//
			// Rewritten for the network model. The questions worth asking changed with it:
			// under the push model you wanted to know a node's buffer, its transfer rate and
			// whether the adjacency probe could reach its neighbours. None of that exists any
			// more. What matters now is which grid a node is on and what that grid holds —
			// two nodes that look identical but report different network ids are not
			// connected, and that is the failure this is for.

			raw("data modify storage ra:wires dbg set value {}")
			data(Storage.WIRES) { modify("dbg.pos", self(), "Pos") }
			data(Storage.WIRES) { modify("dbg.props", self(), "data.properties") }

			function(RaLib.Transport.Net.read)

			raw("""tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"},{text:"  grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  contributes ",color:"gray"},{score:{name:"@s",objective:"ra.tr.cap"},color:"yellow"}]""")
			raw("""tellraw @s [{text:"   grid holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"yellow"}]""")
			raw("""tellraw @s [{text:"   props ",color:"gray"},{nbt:"dbg.props",storage:"ra:wires",color:"white"}]""")

			// A node with grid 0 is on no network at all: either a switch that has been
			// turned off, or something that never joined.
			raw("""execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid — switched off, or it never joined",color:"red"}]""")
			raw("""execute unless entity @s[tag=ra.tr.node] run tellraw @s [{text:"   missing ra.tr.node — ra_wires:electric/adopt should have enrolled it",color:"red"}]""")

			// The enabled flag has to be a byte. An int reads the same in chat and behaves
			// the opposite way.

			// Left over from the push model; nothing writes it now, so seeing one means an
			// old marker was never adopted.
			raw("""execute if data entity @s data.data.eu run tellraw @s [{text:"   stale per-node eu buffer — adopt did not run on this marker",color:"red"}]""")
		}

		pack.define(Debug.generator) {
			// Report every loaded EU Generator: /function ra_wires:debug/generator
			//
			// Written because "it says not active with coal in it" has several causes that
			// look identical from outside the block, and reading the source has not settled
			// which one it is.

			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"EU Generators",color:"aqua"}]""")
			raw("""execute unless entity @e[type=marker,tag=ra.custom_block.electric_generator] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]""")
			executeRun("as @e[type=marker,tag=ra.custom_block.electric_generator] at @s", Debug.generatorOne)
		}

		pack.define(Debug.generatorLookup) {
			// Internal: copy the fuel_map entry for $(id), if there is one.

			raw("\$data modify storage ra:wires dbg.hit set from storage ra:wires fuel_map.\"\$(id)\"")
		}

		pack.define(Debug.generatorOne) {
			// One generator's report. Context: as its marker, at the block.
			//
			// Every line mirrors a condition the real code tests, in the order it tests them,
			// so the first red line is the answer.
			//
			// It also runs the OLD test beside the new one. The generator used to find its
			// fuel with `execute if items block ... container.*` and now walks Items itself;
			// if those two ever disagree about the same barrel, that disagreement is a fact
			// worth having, because the Breeder and the Liquid Drain still use the old one.

			raw("data modify storage ra:wires dbg set value {}")
			data(Storage.WIRES) { modify("dbg.pos", self(), "Pos") }
			raw("""tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"}]""")

			raw("""execute unless block ~ ~ ~ minecraft:barrel run tellraw @s [{text:"   NOT a barrel — placed by an older build. Break and replace it.",color:"red"}]""")

			raw("""execute unless data storage ra:wires fuel_map run tellraw @s [{text:"   fuel_map MISSING — ra_wires:media/init did not run",color:"red"}]""")
			raw("""execute if data storage ra:wires fuel_map run tellraw @s [{text:"   fuel_map loaded",color:"dark_gray"}]""")

			raw("""execute unless data block ~ ~ ~ Items[0] run tellraw @s [{text:"   inventory empty",color:"red"}]""")
			raw("execute if data block ~ ~ ~ Items[0] run data modify storage ra:wires dbg.items set from block ~ ~ ~ Items")
			raw("""execute if data block ~ ~ ~ Items[0] run tellraw @s [{text:"   Items ",color:"gray"},{nbt:"dbg.items",storage:"ra:wires",color:"white"}]""")

			// The new path: is slot 0's id a key in fuel_map?
			data(Storage.WIRES) { remove("dbg.hit") }
			raw("execute if data block ~ ~ ~ Items[0] run data modify storage ra:wires dbg.id set from block ~ ~ ~ Items[0].id")
			executeRun("if data storage ra:wires dbg.id", Debug.generatorLookup, "with storage ra:wires dbg")
			raw("""execute if data storage ra:wires dbg.hit run tellraw @s [{text:"   slot 0 IS a fuel: ",color:"green"},{nbt:"dbg.hit.name",storage:"ra:wires",color:"yellow"},{text:"  ",color:"gray"},{nbt:"dbg.hit.ticks",storage:"ra:wires",color:"yellow"},{text:" ticks",color:"gray"}]""")
			raw("""execute unless data storage ra:wires dbg.hit run tellraw @s [{text:"   slot 0 is not a fuel",color:"yellow"}]""")

			// The old path, on the same barrel, for comparison.
			raw("""execute if items block ~ ~ ~ container.* minecraft:coal run tellraw @s [{text:"   'if items container.*' also sees coal",color:"dark_gray"}]""")
			raw("""execute unless items block ~ ~ ~ container.* minecraft:coal run tellraw @s [{text:"   'if items container.*' does NOT see coal — if there is coal above, that command is the bug, and the Breeder and Liquid Drain share it",color:"red"}]""")

			raw("""execute if data entity @s data.data.burn run tellraw @s [{text:"   BURNING, ticks left ",color:"green"},{nbt:"data.data.burn",entity:"@s",color:"yellow"}]""")
			raw("""execute unless data entity @s data.data.burn run tellraw @s [{text:"   not burning",color:"red"}]""")

			function(RaLib.Transport.Net.read)
			raw("""tellraw @s [{text:"   grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"yellow"},{text:" EU",color:"gray"}]""")
			raw("""execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid at all",color:"red"}]""")
			raw("""execute if score #net_amount ra.tr.tmp >= #net_capacity ra.tr.tmp run tellraw @s [{text:"   grid FULL — it burns but has nowhere to put EU. Add a Battery.",color:"yellow"}]""")

			raw("""execute unless entity @e[type=block_display,tag=ra.skin.electric_generator,distance=..1] run tellraw @s [{text:"   no skin entity present",color:"yellow"}]""")
		}

		pack.define(Debug.light) {
			// Report every loaded Industrial Light: /function ra_wires:debug/light

			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Industrial Lights",color:"aqua"}]""")
			raw("""execute unless entity @e[type=marker,tag=ra.custom_block.industrial_light] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]""")
			executeRun("as @e[type=marker,tag=ra.custom_block.industrial_light] at @s", Debug.lightOne)
		}

		pack.define(Debug.lightOne) {
			// One light's report. Context: as its marker, at the block.
			//
			// The beam needs four things at once and failing any of them looks the same from
			// outside: enabled, redstone, EU it can actually take, and air to shine into.

			raw("data modify storage ra:wires dbg set value {}")
			data(Storage.WIRES) { modify("dbg.pos", self(), "Pos") }
			raw("""tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"},{text:"  facing ",color:"gray"},{score:{name:"@s",objective:"ra.facing"},color:"aqua"}]""")

			line("execute if function", RaLib.Redstone.any, """run tellraw @s [{text:"   redstone: powered",color:"green"}]""")
			line("execute unless function", RaLib.Redstone.any, """run tellraw @s [{text:"   redstone: NONE — it needs a signal as well as EU",color:"red"}]""")

			function(RaLib.Transport.Net.read)
			raw("""tellraw @s [{text:"   grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"}]""")
			raw("""execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid — wire it to a battery",color:"red"}]""")
			raw("""execute if score #net_amount ra.tr.tmp matches ..0 run tellraw @s [{text:"   grid is empty, so there is nothing to draw",color:"red"}]""")

			raw("""execute unless score @s ra.facing matches 0..5 run tellraw @s [{text:"   no facing score — it does not know which way to shine",color:"red"}]""")

			// What is directly in the beam's way, one block out in each direction the facing
			// could mean. Whatever it is, if it is not air the beam stops at once.
			raw("""execute if score @s ra.facing matches 0 unless block ~ ~-1 ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")
			raw("""execute if score @s ra.facing matches 1 unless block ~ ~1 ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")
			raw("""execute if score @s ra.facing matches 2 unless block ~ ~ ~-1 #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")
			raw("""execute if score @s ra.facing matches 3 unless block ~ ~ ~1 #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")
			raw("""execute if score @s ra.facing matches 4 unless block ~-1 ~ ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")
			raw("""execute if score @s ra.facing matches 5 unless block ~1 ~ ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]""")

			raw("""execute if data entity @s data.status.beam run tellraw @s [{text:"   last state: ",color:"gray"},{nbt:"data.status.beam",entity:"@s",color:"white"}]""")
		}
	}
}
