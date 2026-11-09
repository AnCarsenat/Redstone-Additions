package ra.migrations

import ra.core.raw
import ra.modules.wires.fluid.Drain

/**
 * 6.0.1 -> 6.0.2.
 *
 * The Liquid Drain's `speed` (litres per second, 1..100, one 5 L block every 100 / speed ticks)
 * became `rate`, in mL per second: the same throughput, written in the unit the rest of the fluid
 * network uses, so rate = speed * 1000 (5 -> 5000, one block a second). A drain that already has a rate keeps it.
 */
internal val m6_0_1 = migration("6.0.1", "6.0.2") {
	world {
		// The server setting new drains copy at placement. Acts only while the old key is there, and
		// overwrites `rate`: ra_settings:init runs first and has just seeded the default 5000.
		raw("execute unless data storage ra:settings global.props.liquid_drain.speed run return 0")
		raw("execute store result storage ra:settings global.props.liquid_drain.rate int 1000 run data get storage ra:settings global.props.liquid_drain.speed")
		raw("data remove storage ra:settings global.props.liquid_drain.speed")
	}

	entity("liquid_drain") {
		raw("execute if data entity @s data.properties.speed unless data entity @s data.properties.rate store result entity @s data.properties.rate int 1000 run data get entity @s data.properties.speed")
		raw("data remove entity @s data.properties.speed")
		raw("execute unless data entity @s data.properties.rate run data modify entity @s data.properties.rate set value ${Drain.DEFAULT_RATE}")
	}
}
