package ra.migrations

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.TextFormat
import ra.core.fn
import ra.core.raw
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom

/**
 * 6.0.2 -> 6.0.3.
 *
 * The world part is a list of actions, one per change: add yours as one more call in `world { }`
 * and a private function below. An action must not `return` (that would end the whole step and
 * skip the actions after it); guard each line with `execute if/unless` instead.
 */
internal val m6_0_2 = migration("6.0.2", "6.0.3") {
	world {
		removeUpgradePlatform()
		removeAnchorList()
	}

	// Teleport Anchor: ready to file itself in the destinations table.
	entity("teleport_anchor") { anchorForTable() }
}

/**
 * The Teleport Anchor's destination list (`storage ra:ender anchor_ids`, 6.0.1 - 6.0.2) is gone,
 * and with it the Data Handler's [Choose] picker that read it: its two player scores and the
 * pickers it kept in `storage ra:dh pick`. The destinations table (`storage ra:ender anchors`)
 * replaces the list. It is not built here: at load the anchors' ids are not yet in format ID, and
 * chat is reserved for the load message, while building it may rename an anchor and say so. Each
 * anchor files itself instead ([anchorForTable], then its tick), loaded ones on the first tick and
 * the others the first tick their chunk is loaded.
 */
private fun Function.removeAnchorList() {
	raw("data remove storage ra:ender anchor_ids")
	raw("data remove storage ra:dh pick")
	raw("scoreboard objectives remove ra.dh.mkind")
	raw("scoreboard objectives remove ra.dh.pstamp")
}

/**
 * As each anchor placed before 6.0.3, once, before its first tick.
 *
 * Ids are path keys of the destinations table now, so they are in format ID (letters, digits and
 * `_ . + - : / #`). Each target is cleaned to it here (`Main base` becomes `Mainbase`), as
 * `ra_lib:safe/clean` does; the anchor cleans its own id the same way when it files itself, so the
 * two still match. Forgetting `registered_id` (the id it put on the old list) makes its tick file
 * it: the first anchor to do so keeps an id, and any other anchor with the same id (all of them
 * started as "A" before 6.0.3) becomes `A_2`, `A_3`, ..., and its owner, the admins and players
 * within 8 blocks are told the new id. Targets that named a shared id now lead to the anchor that
 * kept it.
 */
private fun Function.anchorForTable() {
	val walk = fn(Ns.MIGRATIONS, "6.0.2-to-6.0.3/anchor_targets") {
		raw("execute unless data storage ra:ender mig.rows[0] run return 0")
		cleanFrom("storage ra:ender mig.rows[0]", TextFormat.ID)
		raw("execute if score ${SafeClean.OK} ${Obj.TEMP} matches 1 run data modify storage ra:ender mig.out append from storage ra:safe out")
		raw("execute if score ${SafeClean.OK} ${Obj.TEMP} matches 0 run data modify storage ra:ender mig.out append value \"\"")
		raw("data remove storage ra:ender mig.rows[0]")
		raw("function ${Ns.MIGRATIONS.id}:6.0.2-to-6.0.3/anchor_targets")
	}
	raw("data remove entity @s data.data.registered_id")
	raw("data modify storage ra:ender mig set value {out:[]}")
	raw("data modify storage ra:ender mig.rows set from entity @s data.properties.targets")
	raw("function ${walk.asId()}")
	raw("execute if data entity @s data.properties.targets[0] run data modify entity @s data.properties.targets set from storage ra:ender mig.out")
	raw("data remove storage ra:ender mig")
}

/**
 * The Upgrade Platform is gone. An assembled one is only a marker (`ra.multiblock.upgrade_platform`)
 * on a Netherite Base: no displays, no inventory, nothing to refund. The base and the smooth stone
 * stay where they are, as blocks the player built.
 *
 * Markers loaded now are removed here, quietly: this runs at load, where chat is reserved for the
 * load message. (The platform had no on_break work, so killing the marker is the whole
 * disassembly.) One in a chunk that is not loaded is disassembled by the regular 2-second
 * validation the first time its chunk loads, with the usual "Multiblock disassembled." to players
 * nearby: no spec claims its tag any more, and it is not registry-driven, so its check fails.
 *
 * Its storage: the dropped-item scratch in `ra:temp upgrade`, and its Data Handler schema, which
 * `init_registry` only ever writes (`ra:multiblock types` and `registry` are rebuilt every load).
 * Removing them here also keeps them in the uninstall, which reads its list from the pack.
 */
private fun Function.removeUpgradePlatform() {
	raw("kill @e[type=minecraft:marker,tag=ra.multiblock.upgrade_platform]")
	raw("data remove storage ra:temp upgrade")
	raw("data remove storage ra:dh types.upgrade_platform")
	raw("data remove storage ra:dh order.upgrade_platform")
	raw("data remove storage ra:dh defaults.upgrade_platform")
}
