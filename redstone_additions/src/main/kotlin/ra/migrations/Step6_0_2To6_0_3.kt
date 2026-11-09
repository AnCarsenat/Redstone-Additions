package ra.migrations

import io.github.ayfri.kore.functions.Function
import ra.core.raw

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
	}
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
