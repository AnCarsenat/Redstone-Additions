package ra.modules.wires.electric

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.block.killVanillaDrop
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.blocks.industrial_light.IndustrialLight
import ra.modules.wires.common.Common
import ra.core.block.PlacementBat

object ElectricBreak : PackPart {
	val battery = ref(Ns.WIRES, "electric/break/battery")
	val breaker = ref(Ns.WIRES, "electric/break/breaker")
	val consumer = ref(Ns.WIRES, "electric/break/consumer")
	val creativeEu = ref(Ns.WIRES, "electric/break/creative_eu")
	val creativeFluid = ref(Ns.WIRES, "electric/break/creative_fluid")
	val electricFurnace = ref(Ns.WIRES, "electric/break/electric_furnace")
	val generator = ref(Ns.WIRES, "electric/break/generator")
	val industrialLight = ref(Ns.WIRES, "electric/break/industrial_light")
	val solarPanel = ref(Ns.WIRES, "electric/break/solar_panel")
	val switch = ref(Ns.WIRES, "electric/break/switch")
	val wire = ref(Ns.WIRES, "electric/break/wire")

	override fun Ra.register() {
		pack.define(ElectricBreak.battery) {
			// Clean up a broken Battery and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// Breaking a battery takes 10000 EU of capacity out of the grid with it. The
			// rebuild's clamp step spills whatever the shrunken grid can no longer hold,
			// which is the honest outcome — the charge was in the battery.

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:waxed_copper_grate")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_grate","minecraft:item_name":'Battery',"minecraft:lore":[{text:"Stores 10000 EU for its grid",color:"gray",italic:false}],"minecraft:custom_data":{ra:{battery:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.battery"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.breaker) {
			// Clean up a broken EU Breaker and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// A breaker is a bridge, not a node, so there is no network membership to release
			// — but the two grids it was joining are about to become permanently separate,
			// and their connectors need redrawing.

			tag(self()) { remove("ra.wires.bridge") }
			tag(self()) { remove("ra.wires.node") }
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:waxed_copper_bulb")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_bulb","minecraft:item_name":'EU Breaker',"minecraft:lore":[{text:"Powered: moves EU from the grid behind",color:"gray",italic:false},{text:"into the grid in front. Never merges them.",color:"gray",italic:false}],"minecraft:custom_data":{ra:{electric_breaker:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_breaker"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.consumer) {
			// Clean up a broken consumer and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			// Stop advertising this node BEFORE the neighbours redraw. update_model_local_and_neighbors
			// asks each neighbour to look around, and their connection test finds node markers —
			// so while this marker still carried its node tags, every neighbour redrew a
			// connector pointing at a pipe that had already been destroyed. That is the
			// connection that was left dangling next to a broken block.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:observer")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:observer","minecraft:item_name":'EU Consumer',"minecraft:custom_data":{ra:{electric_consumer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_consumer"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.creativeEu) {
			// Clean up a broken Creative EU Source. Context: as the marker, at its position.

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			// Both creative sources are beacons now, so the radius is tightened to the block
			// itself -- at distance 2 this would happily eat the OTHER one's dropped beacon
			// when the two are placed next to each other.
			killVanillaDrop("minecraft:beacon")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:beacon","minecraft:item_name":'Creative EU Source',"minecraft:rarity":"epic","minecraft:lore":[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],"minecraft:custom_data":{ra:{creative_eu:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_eu"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.creativeFluid) {
			// Clean up a broken Creative Fluid Source. Context: as the marker, at its position.

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			// Both creative sources are beacons now, so the radius is tightened to the block
			// itself -- at distance 2 this would happily eat the OTHER one's dropped beacon
			// when the two are placed next to each other.
			killVanillaDrop("minecraft:beacon")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:beacon","minecraft:item_name":'Creative Fluid Source',"minecraft:rarity":"epic","minecraft:lore":[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],"minecraft:custom_data":{ra:{creative_fluid:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_fluid"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.electricFurnace) {
			// Clean up a broken Electric Furnace and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// The contents are not dropped by hand: the real block is a barrel and vanilla
			// already spills a barrel's inventory when it breaks, inputs and outputs alike.

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			function(RaLib.Skin.clear, nbt { this["id"] = "electric_furnace" })
			killVanillaDrop("minecraft:barrel")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blast_furnace","minecraft:item_name":'Electric Furnace',"minecraft:lore":[{text:"Smelts with EU - no fuel",color:"gray",italic:false},{text:"Top row in, rows 2-3 out",color:"gray",italic:false}],"minecraft:custom_data":{ra:{electric_furnace:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_furnace"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.generator) {
			// Clean up a broken generator and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			// Stop advertising this node BEFORE the neighbours redraw. update_model_local_and_neighbors
			// asks each neighbour to look around, and their connection test finds node markers —
			// so while this marker still carried its node tags, every neighbour redrew a
			// connector pointing at a pipe that had already been destroyed. That is the
			// connection that was left dangling next to a broken block.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			// The skin comes down before the marker dies -- nothing else knows it is there.
			function(RaLib.Skin.clear, nbt { this["id"] = "electric_generator" })
			killVanillaDrop("minecraft:barrel")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blast_furnace","minecraft:item_name":'EU Generator',"minecraft:custom_data":{ra:{electric_generator:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_generator"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.industrialLight) {
			// Clean up a broken Industrial Light and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// The beam is put out BEFORE the marker dies. The marker is the only thing that
			// knows where its own light blocks are, and once it is gone nothing in the world
			// can find them — an invisible block with no source is not something a player can
			// reasonably clear up.

			function(IndustrialLight.cast, nbt { this["mode"] = 0 })

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:sea_lantern")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:sea_lantern","minecraft:item_name":'Industrial Light',"minecraft:lore":[{text:"Redstone + EU: a 10 block beam of light",color:"gray",italic:false}],"minecraft:custom_data":{ra:{industrial_light:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.industrial_light"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.solarPanel) {
			// Clean up a broken Solar Panel and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			// Release the grid capacity this panel contributed. Every other electric
			// block already did this; the panel never did, because it was not a network
			// node until now.
			function(RaLib.Transport.Net.leave)
			killVanillaDrop("minecraft:daylight_detector")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:daylight_detector","minecraft:item_name":'Solar Panel',"minecraft:lore":[{text:"Generates EU from sunlight",color:"gray",italic:false}],"minecraft:custom_data":{ra:{solar_panel:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.solar_panel"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.switch) {
			// Clean up a broken switch and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			// Stop advertising this node BEFORE the neighbours redraw. update_model_local_and_neighbors
			// asks each neighbour to look around, and their connection test finds node markers —
			// so while this marker still carried its node tags, every neighbour redrew a
			// connector pointing at a pipe that had already been destroyed. That is the
			// connection that was left dangling next to a broken block.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			// The switch is placed as waxed_cut_copper now, so that is what breaking it drops.
			// The item model stays a redstone lamp so it is still tellable apart from a Valve
			// in an inventory -- the pack already does this for wires, whose item is a candle.
			killVanillaDrop("minecraft:waxed_cut_copper")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:redstone_lamp","minecraft:item_name":'EU Switch',"minecraft:custom_data":{ra:{electric_switch:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_switch"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}

		pack.define(ElectricBreak.wire) {
			// Clean up a broken wire and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			raw("kill @e[type=block_display,tag=ra.wires.wire_display,distance=..0.9]")
			// Stop advertising this node BEFORE the neighbours redraw. update_model_local_and_neighbors
			// asks each neighbour to look around, and their connection test finds node markers —
			// so while this marker still carried its node tags, every neighbour redrew a
			// connector pointing at a pipe that had already been destroyed. That is the
			// connection that was left dangling next to a broken block.
			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			killVanillaDrop("minecraft:conduit")
			killVanillaDrop("minecraft:mud_brick_wall")
			killVanillaDrop("minecraft:polished_blackstone_wall")

			// One wire, so one drop. The tier branch that used to be here decided between
			// two items that had become identical.
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:orange_candle","minecraft:item_name":'Wire',"minecraft:custom_data":{ra:{electric_wire_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_wire_copper"],${PlacementBat.FLAGS}}}}}""")
			kill(self())
		}
	}
}
