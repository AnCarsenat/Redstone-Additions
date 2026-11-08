package ra.migrations

import io.github.ayfri.kore.DataPack
import ra.core.define
import ra.core.raw
import ra.modules.wires.fluid.Drain

/**
 * The Liquid Drain's `cooldown` (ticks between actions) became `speed` (litres per second) and its
 * `mode` property went away (redstone decides whether it takes in or pours out).
 *
 * Two places hold the old name:
 *  - every drain marker -- `ra_wires:fluid/drain_migrate`, run here for the drains loaded now and
 *    again from the drain's own tick for any that were not (it tags a migrated drain, so each
 *    one is converted exactly once);
 *  - the server setting `global.props.liquid_drain.cooldown`, which new drains copy at
 *    placement: an admin who set it keeps the same rate as a speed (100 / cooldown).
 *
 * Safe to run twice: both halves act only while the old key is still there.
 */
internal fun DataPack.migrate6_0_0() = define(step("6.0.0", "6.0.1")) {
	raw("execute as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=!ra.wires.drain_v2] run function ${Drain.migrate.asId()}")

	raw("execute unless data storage ra:settings global.props.liquid_drain.cooldown run return 0")
	raw("execute store result score #mg.cd ra.temp run data get storage ra:settings global.props.liquid_drain.cooldown")
	raw("execute if score #mg.cd ra.temp matches ..0 run scoreboard players set #mg.cd ra.temp 1")
	raw("scoreboard players set #mg.sp ra.temp 100")
	raw("scoreboard players operation #mg.sp ra.temp /= #mg.cd ra.temp")
	raw("execute if score #mg.sp ra.temp matches ..0 run scoreboard players set #mg.sp ra.temp 1")
	raw("execute store result storage ra:settings global.props.liquid_drain.speed int 1 run scoreboard players get #mg.sp ra.temp")
	raw("data remove storage ra:settings global.props.liquid_drain.cooldown")
	raw("data remove storage ra:settings global.props.liquid_drain.mode")
}
