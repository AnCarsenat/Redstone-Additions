package ra.modules.ender.debug

import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.ender.EnderIds
import ra.modules.ender.FluidVault
import ra.modules.ender.ItemVault
import ra.modules.ender.PowerVault
import ra.core.executeRun
import ra.core.block.markerSel

/** `/function ra_ender:debug/vaults`: report every vault. */
object EnderDebug {
	private val one = ref(Ns.ENDER, "debug/one")

	fun Ra.registerEnderDebug() {
		val vaults = listOf("Item" to ItemVault.block, "Fluid" to FluidVault.block, "Power" to PowerVault.block)

		pack.define(ref(Ns.ENDER, "debug/vaults")) {
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Ender links",color:"light_purple"}]""")
			val none = vaults.joinToString(" ") { (_, b) -> "unless entity ${b.markerSel()}" }
			raw("""execute $none run tellraw @s [{text:"  no vaults loaded",color:"dark_gray"}]""")

			for ((kind, b) in vaults)
				executeRun("as ${b.markerSel()} at @s", one, "{kind:\"$kind\"}")
		}

		// Macro {kind}: one vault's report. As its marker, at the block.
		//
		// The channel's *type* is the first thing printed, because a channel that is not a string
		// matches nothing and is invisible in chat: 5 and "5" print identically.
		pack.define(one) {
			raw("data modify storage ra:ender dbg set value {}")
			raw("data modify storage ra:ender dbg.pos set from entity @s Pos")
			raw("data modify storage ra:ender dbg.props set from entity @s data.properties")

			raw("execute store success score #ender.isstr ${Obj.TEMP} run data modify storage ra:ender probe set string entity @s data.properties.channel")

			raw("""${'$'}tellraw @s [{text:"— ${'$'}(kind) Vault ",color:"gray"},{nbt:"dbg.pos",storage:"ra:ender",color:"white"}]""")
			raw("""tellraw @s [{text:"   props ",color:"gray"},{nbt:"dbg.props",storage:"ra:ender",color:"white"}]""")
			raw("""execute if score #ender.isstr ${Obj.TEMP} matches 1 run tellraw @s [{text:"   channel is a string",color:"green"}]""")
			raw("""execute if score #ender.isstr ${Obj.TEMP} matches 0 run tellraw @s [{text:"   channel is NOT a string — it matches nothing until repaired",color:"red"}]""")

			// Who this vault can currently reach: the tags are rebuilt every tick from mode.
			raw("""execute if entity @s[tag=${EnderIds.RECV_ITEM}] run tellraw @s [{text:"   can receive items",color:"aqua"}]""")
			raw("""execute if entity @s[tag=${EnderIds.SEND_ITEM}] run tellraw @s [{text:"   can be pulled from",color:"aqua"}]""")
			raw("""execute if entity @s[tag=${EnderIds.RECV_FLUID}] run tellraw @s [{text:"   can receive fluid",color:"aqua"}]""")
			raw("""execute if entity @s[tag=${EnderIds.RECV_POWER}] run tellraw @s [{text:"   can receive EU",color:"aqua"}]""")
		}
	}
}
