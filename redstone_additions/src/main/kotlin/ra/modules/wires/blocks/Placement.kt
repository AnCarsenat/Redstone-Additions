package ra.modules.wires.blocks

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref
import ra.core.registeredLine
import ra.modules.wires.blocks.electric_furnace.ElectricFurnace
import ra.modules.wires.blocks.electric_generator.ElectricGenerator
import ra.modules.wires.common.Common
import ra.modules.wires.machine.MachineClock
import ra.modules.wires.readout.Readout

object Placement : PackPart {
	val handlePlacement = ref(Ns.WIRES, "blocks/handle_placement")
	val placeFinish = ref(Ns.WIRES, "blocks/place_finish")
	val placeGeneric = ref(Ns.WIRES, "blocks/place_generic")
	val placeJoin = ref(Ns.WIRES, "blocks/place_join")
	val registerAll = ref(Ns.WIRES, "blocks/register_all")

	override fun Ra.register() {
		pack.define(Placement.handlePlacement) {
			// Place any RA Wires block.
			// Context: as bat, at bat position.
			//
			// This used to be sixteen blocks of nine near-identical commands, each repeating
			// the same marker selector eight times. Every new block meant another copy, and a
			// fix to the placement sequence had to be applied sixteen times. The per-block
			// part is now just the spec; blocks/place_generic does the work.
			//
			// Spec fields:
			// block     world block to place
			// marker    marker tag suffix, i.e. ra.custom_block.{marker}
			// fluid     1b to tag it as a fluid block for status and displays
			// net       transport network class to join, or absent for a block that is
			// not a network node (the Boiler sits between two networks, so it
			// must not be part of either)
			// capacity  this node's contribution to its network's capacity
			// electric  1b to tag it as an electric block for status and displays.
			// Electric runs on the same network engine as fluids now, so
			// these carry net:"electric" and a capacity like any other node.
			// bridge    1b for a block that belongs to NO network and evens out the
			// networks touching it while powered. It finds them itself, so it
			// needs no facing. It must not also declare `net` -- a bridge that
			// joined a network would merge the sides it exists to keep apart.
			// dir       placement dir_type: 0 none, 1 horizontal, 2 full 6-way,
			// 3 full 6-way on the marker with a plain block (for blocks that
			// have no `facing` state -- bridges use this)
			// props     data.properties for the new marker. Only list what the block
			// actually reads: a property nobody consults still shows up in the
			// Data Handler with a [Toggle] button that changes nothing visible.
			// Pipes and tanks are pure conductors and capacity, so they carry no
			// `enabled` -- use a valve to cut a line.

			data(Storage.WIRES) { remove("spec") }

			for ((placeId, spec) in SPECS) raw("execute if entity @s[tag=ra.place.$placeId] run data modify storage ra:wires spec set value $spec")

			raw("execute unless data storage ra:wires spec run return 0")

			// Default the facing before the call: macro arguments are bound when the function
			// is invoked, so a default written inside place_generic would arrive too late for
			// its own $(dir). A dropper or dispenser without this always points the same way
			// regardless of how the player placed it.
			raw("execute unless data storage ra:wires spec.dir run data modify storage ra:wires spec.dir set value 0")

			function(Placement.placeGeneric, Storage.WIRES, "spec")
			data(Storage.WIRES) { remove("spec") }
			returnValue(1)
		}

		pack.define(Placement.placeFinish) {
			// Internal: initialise a freshly placed RA Wires marker from storage ra:wires spec.
			// Context: as the new marker, at its position.
			//
			// Everything is read straight out of the spec rather than passed as macro
			// arguments: a macro line is substituted before its own `execute if` is
			// evaluated, so an optional field would break every block that does not have one.

			// A block with nothing configurable omits props entirely rather than carrying an
			// empty compound, so this is conditional.
			raw("execute if data storage ra:wires spec.props run data modify entity @s data.properties set from storage ra:wires spec.props")
			raw("execute if data storage ra:wires spec.nodedata run data modify entity @s data.data set from storage ra:wires spec.nodedata")

			tag(self()) { add("ra.wires.node") }

			raw("execute if data storage ra:wires spec.fluid run tag @s add ra.wires.fluid_node")
			raw("execute if data storage ra:wires spec.electric run tag @s add ra.wires.electric_node")
			// A bridge is deliberately not a node: ra_wires:bridge/tick reaches into the
			// networks on either side of it instead of joining one.
			raw("execute if data storage ra:wires spec.bridge run tag @s add ra.wires.bridge")
			raw("execute if data storage ra:wires spec.readout run tag @s add ${Readout.TAG}")
			raw("execute if data storage ra:wires spec.clocked run tag @s add ${MachineClock.TAG}")
			executeRun("if data storage ra:wires spec.net", Placement.placeJoin, "with storage ra:wires spec")

			executeRun("""if data storage ra:wires spec{marker:"electric_generator"}""", ElectricGenerator.refreshDisplay)
			executeRun("""if data storage ra:wires spec{marker:"electric_furnace"}""", ElectricFurnace.refreshDisplay)

			function(Common.updateModelLocalAndNeighbors)
		}

		pack.define(Placement.placeGeneric) {
			// Internal: place one RA Wires block and finish setting its marker up.
			// Context: as the placement bat, at the block position.

			raw("\$function ${RaLib.Placement.place.asId()} {block_id:\"\$(block)\",block_tag:\"\$(marker)\",dir_type:\$(dir)}")

			raw("\$execute as @e[type=marker,tag=ra.custom_block.\$(marker),tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ${Placement.placeFinish.asId()}")
			raw("\$tag @e[type=marker,tag=ra.custom_block.\$(marker),tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new")
		}

		pack.define(Placement.placeJoin) {
			// Internal: enrol a block in a transport network.
			// Only reached for specs that declare a network, so both fields are present.

			raw("\$function ${RaLib.Transport.Net.join.asId()} {class:\"\$(net)\",capacity:\$(capacity)}")
		}

		pack.define(Placement.registerAll) {
			// Announce every RA Wires block on load, for anyone wearing the ra.debug tag.
			//
			// This module keeps its per-block detail in one file rather than a folder each —
			// the same reason handle_placement holds every spec — but a single "blocks
			// registered" line for sixteen blocks told you nothing about which of them loaded.

			for (name in listOf(
				"Pipe",
				"Liquid Tank",
				"Liquid Pump",
				"Liquid Valve",
				"Liquid Drain",
				"Gas Tank",
				"Gas Pump",
				"Gas Valve",
				"Boiler",
				"Wire",
				"EU Generator",
				"EU Consumer",
				"EU Switch",
				"Solar Panel",
				"Battery",
				"EU Breaker",
				"Industrial Light",
				"Network Gauge",
				"Usage Meter",
				"Tesla Coil",
				"Spout",
				"Gas Vent",
			)) registeredLine(name)
		}
	}
}
