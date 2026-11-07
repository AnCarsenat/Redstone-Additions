package ra.tools.readonly

import ra.api.Tools
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * `ra:tools/readonly/init_registry`: which properties a block owns and the player may not edit.
 *
 * WHAT READ-ONLY MEANS HERE
 * Some properties are the block's own working numbers rather than settings: a generator's
 * generation_rate, a valve's transfer rate, a breaker's cooldown. The block writes them, reads them,
 * and depends on them being sane. Letting a player type into them produces machines that are
 * quietly broken in ways no error message explains.
 *
 * Marking one read-only (`ra.readonly(type, ...)`) does two things at once:
 *   - the Data Handler still SHOWS it, with a struck-through [Modify] and a reason on hover,
 *     because hiding a value the goggles display anyway is just confusing
 *   - the wrench never offers it in the cycle menu
 *
 * ONE DECLARATION, NOT ONE PER MODULE
 * This used to be five separate `tools/hidden_fields.mcfunction` files, each a chain of
 * `if entity @s[tag=ra.custom_block.X]`, reached through a function tag. Now it is one map keyed by
 * the block type markers carry in data.type, the same key the wrench registry uses.
 *
 * Stored as compounds rather than lists because both readers ask "is THIS name read-only?", and
 * `if data storage ... ."$(name)"` answers that in one command where a list would need a walk.
 */
object Readonly {
	val initRegistry = Tools.Readonly.initRegistry

	fun Ra.registerReadonly() {
		// Keys sorted, as the hand-written map was.
		val map = readonly.toSortedMap().entries.joinToString(",", "{", "}") { (type, props) ->
			"\"$type\":" + props.joinToString(",", "{", "}") { "$it:1b" }
		}
		pack.define(initRegistry) { raw("data modify storage ra:dh readonly set value $map") }
	}
}
