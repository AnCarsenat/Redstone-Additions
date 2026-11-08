package ra.migrations

import ra.api.RaLib
import ra.core.Tag
import ra.core.raw

/**
 * Per-block fixups from 5.1.7 ("Grids, Bridges, Millilitres") that used to run inside block ticks,
 * every tick, for every block of the type. They live here since 6.0.1 and run once per marker.
 * No world part: everything 5.1.7 changed is on the markers themselves.
 */
internal val m5_1_6 = migration("5.1.6", "5.1.7") {
	// Per-source redstone tags (ra.powered.dust, .lever, .block...) were set by the old detect and
	// read by nothing. They are no longer produced; a block that happened to be powered at the
	// moment of the upgrade would otherwise carry them for ever. Was a sweep in ra:load over the
	// loaded markers, plus seven removals in every ra_lib:redstone/clear call.
	marker(Tag.CUSTOM_BLOCK) {
		for (t in listOf("repeater", "comparator", "dust", "block", "lever", "button", "torch"))
			raw("tag @s remove ${Tag.POWERED}.$t")
	}

	// Skins moved from the block's minimum corner to its centre. A corner skin is out of reach of
	// the centre-anchored redraw, so it would sit under the new one for ever. Anchored to this
	// block's corner and narrower than half a block, so a neighbour's skin of either vintage is
	// out of reach. Was the second kill in every ra_lib:skin redraw.
	marker(Tag.CUSTOM_BLOCK) {
		raw("execute at @s align xyz run kill @e[type=block_display,tag=ra.skin,distance=..0.4]")
	}

	// The Clock's period is `cooldown`. A clock may carry the older `delay`: it becomes `cooldown`
	// unless the clock already has one, and is then removed either way -- leaving both is what
	// made the block look broken. Was in the Clock's tick.
	entity("clock") {
		raw("execute if data entity @s data.properties.delay unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set from entity @s data.properties.delay")
		raw("data remove entity @s data.properties.delay")
	}

	// The Poppy Generator's 3x3 `patch` mode is gone, and with it the `mode` property: left on a
	// marker the Data Handler would offer a setting that decides nothing. Was in its tick.
	entity("poppy_generator") {
		raw("data remove entity @s data.properties.mode")
	}

	// Valves (and Filters) are bridges: they belong to no network and pump between the two they
	// sit between. One built as a network member must leave its network -- staying would merge the
	// runs it is meant to keep apart -- and needs the bridge tag ra_wires:tick drives. Placement
	// does both for a new one. Was in ra_wires:fluid/tick, every tick.
	entity("liquid_valve", "gas_valve", "liquid_filter") {
		raw("execute if entity @s[tag=ra.tr.node] run function ${RaLib.Transport.Net.leave.asId()}")
		raw("tag @s add ra.wires.bridge")
	}
}
