package ra.modules.wires.blocks.creative_fluid

import ra.core.computeScore
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.utils.nbt
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.gogglesHeader
import ra.core.block.soundListenersSel
import ra.core.block.stackReset
import ra.core.block.stackedStatusLine
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.modules.wires.fluid.Fluid
import ra.core.block.PlacementBat
import ra.core.block.WRENCH_READOUT

object CreativeFluid : PackPart {
	val cycleMedium = ref(Ns.WIRES, "blocks/creative_fluid/cycle_medium")
	val give = ref(Ns.WIRES, "blocks/creative_fluid/give")
	val goggles = ref(Ns.WIRES, "blocks/creative_fluid/goggles")
	val known = ref(Ns.WIRES, "blocks/creative_fluid/known")
	val push = ref(Ns.WIRES, "blocks/creative_fluid/push")
	val tick = ref(Ns.WIRES, "blocks/creative_fluid/tick")

	override fun Ra.register() {
		pack.define(CreativeFluid.cycleMedium) {
			// Wrench: step the medium round. Context: as the marker, at the block.
			//
			// Only the media a network can actually be built around (oxygen and toxic gas have no
			// survival source yet, so this is how they are tested). Potions and experience
			// are media too, but they arrive by other routes and a creative source for them
			// would be a different kind of block.

			raw("execute unless data entity @s data.properties.medium run data modify entity @s data.properties.medium set value \"water\"")

			scoreboard.players.set(literal("#cr.m"), "ra.wires.tmp", 0)
			raw("""execute if data entity @s data.properties{medium:"water"} run scoreboard players set #cr.m ra.wires.tmp 1""")
			raw("""execute if data entity @s data.properties{medium:"lava"} run scoreboard players set #cr.m ra.wires.tmp 2""")
			raw("""execute if data entity @s data.properties{medium:"powder_snow"} run scoreboard players set #cr.m ra.wires.tmp 3""")
			raw("""execute if data entity @s data.properties{medium:"milk"} run scoreboard players set #cr.m ra.wires.tmp 4""")
			raw("""execute if data entity @s data.properties{medium:"steam"} run scoreboard players set #cr.m ra.wires.tmp 5""")
			raw("""execute if data entity @s data.properties{medium:"smoke"} run scoreboard players set #cr.m ra.wires.tmp 6""")
			raw("""execute if data entity @s data.properties{medium:"oxygen"} run scoreboard players set #cr.m ra.wires.tmp 7""")

			raw("execute if score #cr.m ra.wires.tmp matches 0 run data modify entity @s data.properties.medium set value \"water\"")
			raw("execute if score #cr.m ra.wires.tmp matches 1 run data modify entity @s data.properties.medium set value \"lava\"")
			raw("execute if score #cr.m ra.wires.tmp matches 2 run data modify entity @s data.properties.medium set value \"powder_snow\"")
			raw("execute if score #cr.m ra.wires.tmp matches 3 run data modify entity @s data.properties.medium set value \"milk\"")
			raw("execute if score #cr.m ra.wires.tmp matches 4 run data modify entity @s data.properties.medium set value \"steam\"")
			raw("execute if score #cr.m ra.wires.tmp matches 5 run data modify entity @s data.properties.medium set value \"smoke\"")
			raw("execute if score #cr.m ra.wires.tmp matches 6 run data modify entity @s data.properties.medium set value \"oxygen\"")
			raw("execute if score #cr.m ra.wires.tmp matches 7 run data modify entity @s data.properties.medium set value \"toxic_gas\"")

			raw("""tellraw $WRENCH_READOUT [{text:"[Wrench] ",color:"gold"},{text:"Creative source: ",color:"gray"},{nbt:"data.properties.medium",entity:"@s",color:"aqua"}]""")
			raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		}

		pack.define(CreativeFluid.give) {
			// Give the Creative Fluid Source. Creative only -- there is no recipe for it, on purpose.

			raw("""give @s bat_spawn_egg[item_model="minecraft:beacon",item_name="Creative Fluid Source",rarity=epic,lore=[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],custom_data={ra:{creative_fluid:1b}},entity_data={id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_fluid"],${PlacementBat.FLAGS}}]""")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Creative Fluid Source",color:"light_purple"}]""")
		}

		pack.define(CreativeFluid.goggles) {
			// Goggles readout, and the block's display name.
			// Context: as the block's marker, at the block position.

			gogglesHeader("Creative Fluid Source", nameY = 1.0)

			stackReset(80, 22)
			stackedStatusLine("state", "", "light_purple")
			stackedStatusLine("medium", "Medium: ", "aqua")
			stackedStatusLine("filled", "Filled: ", "yellow", suffix = " mL")
		}

		pack.define(CreativeFluid.known) {
			// Internal: is this the name of a medium the pack knows? Returns 1 or 0.
			//
			// One lookup against the registry in ra_wires:media/init. Anything added there is
			// accepted here without an edit, which is the point of the registry.

			raw("\$execute if data storage ra:wires media.\$(medium) run return 1")
			returnValue(0)
		}

		pack.define(CreativeFluid.push) {
			// Internal: offer the medium to this node's network. Context: as the marker.

			raw("\$execute store result score #cr.made ra.wires.tmp run function ${RaLib.Transport.Net.offer.asId()} {amount:\$(amount),medium:\"\$(medium)\"}")

			raw("execute if score #cr.made ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value \"Network full\"")

			raw("data modify entity @s data.status.state set value \"Generating\"")
			raw("execute store result entity @s data.status.filled int 1 run scoreboard players get #cr.made ra.wires.tmp")
			raw("\$function ${Fluid.particles.asId()} {medium:\"\$(medium)\"}")
		}

		pack.define(CreativeFluid.tick) {
			// Creative Fluid Source: fills its network with a chosen medium, from nothing.
			// Context: as its marker, at its block.
			//
			// The counterpart to the Creative EU Source, and the same reasoning: while you
			// are building the consuming half of a fluid system you do not want to also be
			// running a pump farm to feed it.
			//
			// The medium is a property rather than a fixed choice: a source stuck on water
			// could not be used to test a lava line at all. Cycle it with the wrench, or set
			// it on the `medium` row of the Data Handler.
			//
			// THE WRENCH CYCLES, THE HANDLER TYPES
			// The wrench can only ever land on a medium that exists. The Handler takes free
			// text, so it can land on one that does not -- and an unrecognised medium pushed
			// into a network would open a key nothing can drain, in a run that now holds
			// several media and would keep the junk one for ever. So it is checked here, and
			// a source set to a name the registry does not know does nothing and says so.

			raw("execute unless data entity @s data.properties.medium run data modify entity @s data.properties.medium set value \"water\"")

			data(self()) { modify("data.status.medium", self(), "data.properties.medium") }

			raw("data modify storage ra:wires cfv set value {}")
			data(Storage.WIRES) { modify("cfv.medium", self(), "data.properties.medium") }
			executeRun("store result score #cr.known ra.wires.tmp", CreativeFluid.known, "with storage ra:wires cfv")
			raw("execute if score #cr.known ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value \"Unknown medium\"")

			function(RaLib.Transport.Net.read)
			computeScore("#cr.fill", "ra.wires.tmp", RaLib.Transport.Net.free)

			raw("execute if score #cr.fill ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value \"Network full\"")

			// A network holding something else takes this alongside it now -- the run clogs on
			// the sum rather than on any one medium -- so the fill above is the room left
			// across everything in there.
			//
			// IT FILLS AT A RATE, IT DOES NOT CLAIM THE RUN
			// The room left is not the amount to offer. Offering it whole meant one source
			// took the entire network on the tick it was placed -- 100000 mL of water in one
			// go -- and every other source on that run, and every Pump and Drain feeding it,
			// then found a full network for ever. A second medium could not be got in at all,
			// which made a mixed run impossible to build anywhere a Creative Fluid Source was
			// attached, and reported itself as the run simply refusing the new medium.
			//
			// So it tops up at `rate` per tick, like every other thing that moves fluid here.
			// Sources sharing a run now interleave and the run ends up holding all of them.
			// The rate is a property: set it back up with the Data Handler for the old
			// behaviour on a single-medium test rig.
			raw("execute unless data entity @s data.properties.rate run data modify entity @s data.properties.rate set value 1000")
			raw("execute store result score #cr.rate ra.wires.tmp run data get entity @s data.properties.rate")
			raw("execute if score #cr.fill ra.wires.tmp > #cr.rate ra.wires.tmp run scoreboard players operation #cr.fill ra.wires.tmp = #cr.rate ra.wires.tmp")

			raw("execute store result storage ra:wires cf.amount int 1 run scoreboard players get #cr.fill ra.wires.tmp")
			data(Storage.WIRES) { modify("cf.medium", self(), "data.properties.medium") }
			function(CreativeFluid.push, Storage.WIRES, "cf")
		}
	}
}
