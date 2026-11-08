package ra.migrations

import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Tag
import ra.core.fn
import ra.core.raw
import ra.modules.ender.TeleportAnchor
import ra.modules.wires.fluid.Drain

/**
 * 6.0.0 -> 6.0.1. Also the home of the one-time fixes that, until 6.0.1, ran from block ticks on
 * every tick (the Liquid Drain's, the Boxer/Unboxer's, the Teleport Anchor's): a marker below 6.0.1
 * gets each of them once, here, and the blocks' ticks no longer carry them.
 */
internal val m6_0_0 = migration("6.0.0", "6.0.1") {
	world {
		// The server setting new drains copy at placement: an admin who set a cooldown keeps the
		// same rate as a speed (100 / cooldown, at least 1). Acts only while the old key is there.
		raw("execute unless data storage ra:settings global.props.liquid_drain.cooldown run return run data remove storage ra:settings global.props.liquid_drain.mode")
		raw("execute store result score #mg.cd ra.temp run data get storage ra:settings global.props.liquid_drain.cooldown")
		raw("execute if score #mg.cd ra.temp matches ..0 run scoreboard players set #mg.cd ra.temp 1")
		raw("scoreboard players set #mg.sp ra.temp ${Drain.TICKS_AT_SPEED_1}")
		raw("scoreboard players operation #mg.sp ra.temp /= #mg.cd ra.temp")
		raw("execute if score #mg.sp ra.temp matches ..0 run scoreboard players set #mg.sp ra.temp 1")
		raw("execute store result storage ra:settings global.props.liquid_drain.speed int 1 run scoreboard players get #mg.sp ra.temp")
		raw("data remove storage ra:settings global.props.liquid_drain.cooldown")
		raw("data remove storage ra:settings global.props.liquid_drain.mode")

		// Up to 6.0.0 the Blast Forge's per-facing coordinates were written to storage as bf_dir;
		// its spec and tick carry them now and nothing reads the old copy. Was in ra_multiblock:load.
		raw("data remove storage ra:multiblock bf_dir")
	}

	// The Liquid Drain's `cooldown` (ticks between actions; `interval` before that) became `speed`
	// (litres per second, 100 / cooldown clamped to 1..100: 20 ticks -> 5, 10 -> 10, 40 -> 2), and
	// `mode` went away -- redstone decides whether it takes in or pours out. A speed already there
	// wins. ra.wires.drain_v2 is the tag the drain's tick used to gate this on.
	entity("liquid_drain") {
		val speedFromCooldown = fn(Ns.MIGRATIONS, "6.0.0-to-6.0.1/liquid_drain_speed") {
			raw("execute store result score #mg.cd ra.temp run data get entity @s data.properties.cooldown")
			raw("execute if score #mg.cd ra.temp matches ..0 run scoreboard players set #mg.cd ra.temp 1")
			raw("scoreboard players set #mg.sp ra.temp ${Drain.TICKS_AT_SPEED_1}")
			raw("scoreboard players operation #mg.sp ra.temp /= #mg.cd ra.temp")
			raw("execute if score #mg.sp ra.temp matches ..0 run scoreboard players set #mg.sp ra.temp 1")
			raw("execute store result entity @s data.properties.speed int 1 run scoreboard players get #mg.sp ra.temp")
		}
		raw("execute if data entity @s data.properties.interval unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set from entity @s data.properties.interval")
		raw("execute if data entity @s data.properties.cooldown unless data entity @s data.properties.speed run function ${speedFromCooldown.asId()}")
		raw("data remove entity @s data.properties.interval")
		raw("data remove entity @s data.properties.cooldown")
		raw("data remove entity @s data.properties.mode")
		raw("execute unless data entity @s data.properties.speed run data modify entity @s data.properties.speed set value ${Drain.DEFAULT_SPEED}")
		raw("tag @s remove ra.wires.drain_v2")
	}

	// Boxer and Unboxer: the IO properties their macro processing needs (placement sets them; a
	// block from before they existed has neither), and the move of the old up/down defaults to
	// rear/front. ra.storage.io_default_migrated marked a block already moved -- one whose player
	// has since chosen up/down on purpose keeps it. All of it was in both blocks' ticks.
	for ((type, input) in listOf("boxer" to "^ ^ ^-1", "unboxer" to "~ ~ ~")) entity(type) {
		raw("execute unless entity @s[tag=ra.storage.io_default_migrated] if data entity @s data.properties{input1:\"^ ^-1 ^\",output1:\"^ ^1 ^\"} run data merge entity @s {data:{properties:{input1:\"^ ^ ^-1\",output1:\"^ ^ ^1\"}}}")
		raw("execute unless data entity @s data.properties.input1 run data modify entity @s data.properties.input1 set value \"$input\"")
		raw("execute unless data entity @s data.properties.output1 run data modify entity @s data.properties.output1 set value \"^ ^ ^1\"")
		raw("tag @s remove ra.storage.io_default_migrated")
	}

	// Teleport Anchor: an id for an anchor from before it had one, and a target table of exactly
	// fifteen entries (6.0.0's Data Handler pasted whatever parsed). ra.ender.anchor_checked is the
	// tag the anchor's tick used to gate the repair on.
	entity("teleport_anchor") {
		raw("execute unless data entity @s data.properties.anchor_id run data modify entity @s data.properties.anchor_id set value \"A\"")
		function(TeleportAnchor.repairTargets)
		raw("tag @s remove ra.ender.anchor_checked")
	}

	// Block Breaker and Block Placer: `cooldown` became a real (admin-only) setting in 6.0.1, read
	// with a default of 20; placement seeds it. One placed earlier gets the same 20, so the Data
	// Handler shows the value it runs at. A value an older version left there is kept and used.
	for (type in listOf("block_breaker", "block_placer")) entity(type) {
		raw("execute unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 20")
	}

	// Item Pipe: `invert` is new in 6.0.1 and placement seeds it 0b. The Data Handler only draws a
	// row for a property the block carries, so an older pipe gets it too (off: unchanged behaviour).
	entity("item_pipe") {
		raw("execute unless data entity @s data.properties.invert run data modify entity @s data.properties.invert set value 0b")
	}

	// Custom blocks were re-centred every tick up to 6.0.0 though nothing moves them; placement
	// puts the marker at the centre. One an older version left off-centre is straightened once.
	// Was a sweep over the loaded markers in ra:load.
	marker(Tag.CUSTOM_BLOCK) {
		raw("execute at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~")
	}
}
