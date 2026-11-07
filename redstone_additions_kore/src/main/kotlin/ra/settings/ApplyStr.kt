package ra.settings

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.apply.ApplyFns

/**
 * `apply_str {key}`: store typed text for this player.
 *
 * Text cannot live on a scoreboard, so a per-player string needs its own store, keyed by something
 * that identifies the player. UUID rather than name: a name can change and would orphan the
 * setting, and the UUID is the only identifier a data pack can actually read off a player.
 *
 * The UUID is an int array, which no `if data` predicate can be written against by hand -- but a
 * macro substitutes it verbatim as [I;a,b,c,d], which is exactly the literal a path predicate
 * needs. That is what makes this two lines instead of a lookup table.
 */
internal fun Ra.registerApplyStr() {
	val at = ref(Ns.SETTINGS, "apply_str_at")

	pack.define(ApplyFns.str) {
		sCopy("put.key", "edit.key")
		data(S) { modify("put.u") { set(self(), "UUID") } }
		function(at, S, "put")
		sRemove("put")
	}

	// `apply_str_at {u, key}`: append this player's row if they have none, then write the value.
	pack.define(at) {
		raw("\$execute unless data storage ra:settings users[{u:\$(u)}] run data modify storage ra:settings users append value {u:\$(u)}")
		raw("\$data modify storage ra:settings users[{u:\$(u)}].\"\$(key)\" set from storage ra:input consume.text")
	}
}
