package ra.tools.multimeter

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.advancement
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import net.benwoodworth.knbt.NbtByte
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.tools.REACH_4
import ra.tools.ToolItems
import ra.tools.callWith
import ra.tools.executeReturnRun
import ra.tools.executeRun
import ra.tools.goggles.GogglesRefs
import ra.tools.raycast
import ra.tools.tool

/**
 * The Multimeter: RMB a block to read its transport network in chat.
 *
 * Read-only by design. The Data Handler edits, the Wrench cycles, the goggles show one block at a
 * time from across the room -- this one answers "where is my power actually going" while standing
 * in front of a single block, in chat, where you can read it without the billboard timing out.
 */
object Multimeter {
	val give = tool("multimeter/give")
	val onUse = tool("multimeter/on_use")
	val report = tool("multimeter/report")
	val offNetwork = tool("multimeter/off_network")
	val media = tool("multimeter/media")
	val mediaNext = tool("multimeter/media_next")
	val mediaOne = tool("multimeter/media_one")

	private const val HOLDER = "@a[tag=ra.meter_active,limit=1]"

	fun Ra.registerMultimeter() {
		pack.define(give) {
			raw("give @s ${ToolItems.multimeter.argument}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Multimeter",color:"gold"}]""")
		}

		// As the player, from the ra:tools/multimeter_use advancement.
		pack.define(onUse) {
			advancement { revoke(self(), AdvancementArgument("tools/multimeter_use", "ra")) }
			tag(self()) { add("ra.meter_clicked") }
			raw("execute if entity @s[tag=ra.meter_active] run return fail")
			tag(self()) { add("ra.meter_active") }
			data(Storage.TEMP) { remove("meter_found") }
			raycast(REACH_4, "meter_found", report)
			raw("""execute unless data storage ra:temp meter_found run tellraw @s [{text:"[Multimeter] ",color:"gold"},{text:"No custom block in front of you.",color:"gray"}]""")
			data(Storage.TEMP) { remove("meter_found") }
		}

		// What this block is doing on its network. As the target block's marker.
		pack.define(report) {
			data(Storage.TEMP) { modify("meter_found", NbtByte(1)) }
			data(Storage.TEMP) { modify("name_only", NbtByte(1)) }
			function(GogglesRefs.drawBlock)
			data(Storage.TEMP) { remove("name_only") }
			raw("""tellraw $HOLDER [{text:"── ",color:"dark_gray"},{nbt:"block_name",storage:"ra:temp",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]""")

			// Not every custom block is on a network. Say so plainly rather than printing three
			// zeroes that look like a flat battery.
			executeReturnRun("unless entity @s[tag=ra.tr.node]", offNetwork)

			function(RaLib.Transport.Net.read)
			raw("""tellraw $HOLDER [{text:"  Network ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"   this block adds ",color:"gray"},{score:{name:"@s",objective:"ra.tr.cap"},color:"yellow"}]""")
			raw("""tellraw $HOLDER [{text:"  Stored ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"green"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"green"}]""")

			// EVERY medium, not just the primary. A network can hold several at once, and this used
			// to print `cur.medium` alone -- which is media[0], the one that got there first. A run
			// holding 5000 water and 5000 lava read "Stored 10000" over "Holding water", so the tool
			// for answering "what is actually in this pipe" was the one place that could not say.
			executeRun("if data storage ra:transport cur.medium", media)
			raw("""execute unless data storage ra:transport cur.medium run tellraw $HOLDER [{text:"  Holding ",color:"gray"},{text:"nothing",color:"dark_gray"}]""")

			// Whatever the block itself publishes for the goggles is worth repeating here: a
			// consumer's draw, a generator's rate, a bridge's throughput.
			for ((path, label, color, unit) in listOf(
				listOf("properties.eu_use", "Draws ", "red", " EU per tick"),
				listOf("properties.generation_rate", "Makes ", "green", " EU per tick"),
				listOf("properties.rate", "Moves ", "yellow", " per tick"),
				listOf("status.trend", "Change ", "aqua", " per second"),
			)) raw("""execute if data entity @s data.$path run tellraw $HOLDER [{text:"  $label",color:"gray"},{nbt:"data.$path",entity:"@s",color:"$color"},{text:"$unit",color:"gray"}]""")

			raw("playsound minecraft:block.note_block.bit block @a[tag=ra.meter_active,limit=1,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.6 1.8")
		}

		// This block is not a transport node at all.
		//
		// Three cases reach here and they are worth telling apart, because "0 of 0" looks identical
		// to a flat battery: a bridge belongs to no network by design, a switch that has been turned
		// off has left its own, and everything else in the pack was simply never a node.
		pack.define(offNetwork) {
			raw("""execute if entity @s[tag=ra.wires.bridge] run return run tellraw $HOLDER [{text:"  A bridge belongs to no network — it moves between the two beside it.",color:"gray"}]""")
			raw("""execute if entity @s[tag=ra.custom_block.electric_switch] run return run tellraw $HOLDER [{text:"  Switched off, so it has left its grid.",color:"gray"}]""")
			raw("""tellraw $HOLDER [{text:"  Not a network block.",color:"gray"}]""")
		}

		registerMedia()
	}

	/**
	 * What the network just read is holding, medium by medium. As the target block's marker,
	 * immediately after ra_lib:transport/net/read.
	 *
	 * The goggles cannot do this. A billboard is one short line read from across the room, so a
	 * mixed run reads "Multimedium" there and the numbers live here -- which is what the Multimeter
	 * is for: stand in front of one block and get the whole answer in chat.
	 */
	private fun Ra.registerMedia() {
		pack.define(media) {
			data(Storage.TEMP) { remove("meterq") }
			raw("data modify storage ra:temp meterq.queue set from storage ra:transport cur.media")
			// One medium reads as a sentence. Several want a heading and a line each, so the amounts
			// line up under it instead of running together.
			scoreboard.players.set(literal("#meter.n"), Obj.TEMP, 0)
			raw("execute if data storage ra:temp meterq.queue[1] run scoreboard players set #meter.n ${Obj.TEMP} 1")
			raw("""execute if score #meter.n ${Obj.TEMP} matches 1.. run tellraw $HOLDER [{text:"  Holding",color:"gray"}]""")
			// A network with a primary and no list should not exist -- ra_lib:transport/net/read
			// backfills one -- but naming it is better than printing nothing at all.
			raw("""execute unless data storage ra:temp meterq.queue[0] run return run tellraw $HOLDER [{text:"  Holding ",color:"gray"},{nbt:"cur.medium",storage:"ra:transport",color:"aqua"}]""")
			function(mediaNext)
			data(Storage.TEMP) { remove("meterq") }
			data(Storage.TEMP) { remove("meter") }
		}

		// The head of the media list, then the rest. The list is walked rather than the `amounts`
		// compound because a compound's keys cannot be enumerated in a function, and because the
		// list order is the order the media arrived in -- the same order everything else reports.
		pack.define(mediaNext) {
			raw("execute unless data storage ra:temp meterq.queue[0] run return 0")
			raw("data modify storage ra:temp meterq.m set from storage ra:temp meterq.queue[0].m")
			raw("data remove storage ra:temp meterq.queue[0]")
			callWith(mediaOne, "temp", "meterq")
			function(mediaNext)
		}

		// {m}: one medium's display name and amount. The name falls back to the raw key, so a medium
		// that is not in the registry shows as itself rather than as a blank.
		pack.define(mediaOne) {
			raw("\$data modify storage ra:temp meter.name set value \"\$(m)\"")
			raw("\$execute if data storage ra:wires media.\$(m).name run data modify storage ra:temp meter.name set from storage ra:wires media.\$(m).name")
			raw("\$execute store result score #meter.a ${Obj.TEMP} run data get storage ra:transport cur.amounts.\$(m)")
			// EU is the one medium that is not a fluid and is deliberately not in the fluid registry
			// -- a Creative Fluid Source set to "eu" would otherwise be accepted. So: anything the
			// registry knows is measured in millilitres, and a grid's charge is measured in EU.
			raw("data modify storage ra:temp meter.unit set value \" EU\"")
			raw("\$execute if data storage ra:wires media.\$(m) run data modify storage ra:temp meter.unit set value \" mL\"")
			val amount = """{nbt:"meter.name",storage:"ra:temp",color:"aqua"},{text:"  ",color:"gray"},{score:{name:"#meter.a",objective:"ra.temp"},color:"green"},{nbt:"meter.unit",storage:"ra:temp",color:"gray"}"""
			raw("""execute if score #meter.n ${Obj.TEMP} matches 1.. run tellraw $HOLDER [{text:"    ",color:"gray"},$amount]""")
			raw("""execute if score #meter.n ${Obj.TEMP} matches ..0 run tellraw $HOLDER [{text:"  Holding ",color:"gray"},$amount]""")
		}
	}
}
