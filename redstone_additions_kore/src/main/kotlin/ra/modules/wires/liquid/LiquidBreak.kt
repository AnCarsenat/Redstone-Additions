package ra.modules.wires.liquid

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.utils.nbt
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.wires.common.Common

object LiquidBreak : PackPart {
	val boiler = ref(Ns.WIRES, "liquid/break/boiler")
	val drain = ref(Ns.WIRES, "liquid/break/drain")
	val filter = ref(Ns.WIRES, "liquid/break/filter")
	val gasPump = ref(Ns.WIRES, "liquid/break/gas_pump")
	val gasTank = ref(Ns.WIRES, "liquid/break/gas_tank")
	val gasValve = ref(Ns.WIRES, "liquid/break/gas_valve")
	val pipe = ref(Ns.WIRES, "liquid/break/pipe")
	val pump = ref(Ns.WIRES, "liquid/break/pump")
	val tank = ref(Ns.WIRES, "liquid/break/tank")
	val valve = ref(Ns.WIRES, "liquid/break/valve")

	override fun Ra.register() {
		pack.define(LiquidBreak.boiler) {
			// Clean up a broken Boiler and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// The boiler is not a network node, so there is no network membership to release.

			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:furnace"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:furnace","minecraft:item_name":'Boiler',"minecraft:lore":[{text:"Water in, steam out, over a heat source",color:"gray",italic:false}],"minecraft:custom_data":{ra:{boiler:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.boiler"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.drain) {
			// Clean up a broken drain and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":'Liquid Drain',"minecraft:custom_data":{ra:{liquid_drain:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_drain"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.filter) {
			// Clean up a broken Liquid Filter and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.

			tag(self()) { remove("ra.wires.fluid_node") }
			tag(self()) { remove("ra.wires.electric_node") }
			tag(self()) { remove("ra.wires.node") }
			function(RaLib.Transport.Net.leave)
			function(Common.updateModelLocalAndNeighbors)
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:waxed_exposed_cut_copper"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_exposed_cut_copper","minecraft:item_name":'Liquid Filter',"minecraft:lore":[{text:"Passes only the medium it is set to",color:"gray",italic:false},{text:"Set filter_medium with the Data Handler",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{liquid_filter:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_filter"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.gasPump) {
			// Clean up a broken gas pump and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:smoker"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smoker","minecraft:item_name":'Gas Pump',"minecraft:custom_data":{ra:{gas_pump:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.gas_pump"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.gasTank) {
			// Clean up a broken gas tank and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:iron_block"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:iron_block","minecraft:item_name":'Gas Tank',"minecraft:custom_data":{ra:{gas_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.gas_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.gasValve) {
			// Clean up a broken gas valve and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:smooth_basalt"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:smooth_basalt","minecraft:item_name":'Gas Valve',"minecraft:custom_data":{ra:{gas_valve:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.gas_valve"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.pipe) {
			// Clean up a broken pipe and return its item.
			// Context: as the marker, at its position. Ends by killing the marker.
			//
			// only ever writes tier "copper" or "iron" (blocks/handle_placement), and the
			// "liquid_pipe_netherite" item is crafted from iron ingots and named "L2 Iron
			// Pipe" — the id is a leftover name, not a third tier. Left in place because a
			// real netherite tier would need its own recipe, item and transfer values.

			raw("kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..0.9]")
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]""")
			kill(self())
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:copper_block","minecraft:item_name":'Copper Pipe',"minecraft:custom_data":{ra:{liquid_pipe_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_pipe_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
		}

		pack.define(LiquidBreak.pump) {
			// Clean up a broken pump and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":'Liquid Pump',"minecraft:custom_data":{ra:{liquid_pump:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_pump"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.tank) {
			// Clean up a broken tank and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:waxed_copper_block"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_copper_block","minecraft:item_name":'Liquid Tank',"minecraft:custom_data":{ra:{liquid_tank:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_tank"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}

		pack.define(LiquidBreak.valve) {
			// Clean up a broken valve and return its item.
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
			raw("""kill @e[type=item,nbt={Item:{id:"minecraft:waxed_cut_copper"}},distance=..2,limit=1]""")
			raw("""summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_cut_copper","minecraft:item_name":'Liquid Valve',"minecraft:custom_data":{ra:{liquid_valve:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.liquid_valve"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}""")
			kill(self())
		}
	}
}
