package ra.settings

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import net.benwoodworth.knbt.NbtString
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/** The read API every module uses: `get`, `prop`, `user`, `enabled`, and per-player text. */
internal fun Ra.registerRead() {
	val userStrAt = ref(Ns.SETTINGS, "user_str_at")

	// `get {key, default}`: a GLOBAL setting into #setting ra.set.tmp, returned as well.
	//
	// THE DEFAULT IS NOT OPTIONAL, AND THAT IS THE POINT. This used to answer 0 for a key it could
	// not find. Zero is a real value -- "off" for a flag and "disabled" for a gate -- so a key that
	// was missing for any reason at all read as a deliberate "no", and a feature gated on it
	// switched itself off. A settings system that has not finished loading should never be able
	// to disable a module. Callers that gate a feature pass 1, so the failure direction is "carry
	// on working".
	pack.define(Settings.get) {
		raw("\$scoreboard players set #setting ${Obj.SET_TMP} \$(default)")
		raw("\$execute if data storage ra:settings global.\"\$(key)\" store result score #setting ${Obj.SET_TMP} run data get storage ra:settings global.\"\$(key)\"")
		returnSetting()
	}

	// `prop {block, prop, default}`: the configured default for one block's property. Falls back
	// to the literal default when the admin has never touched it, so a caller does not have to
	// care whether the settings page exists yet.
	pack.define(Settings.prop) {
		raw("\$scoreboard players set #setting ${Obj.SET_TMP} \$(default)")
		raw("\$execute if data storage ra:settings global.props.\"\$(block)\".\$(prop) store result score #setting ${Obj.SET_TMP} run data get storage ra:settings global.props.\"\$(block)\".\$(prop)")
		returnSetting()
	}

	// `user {obj, default}`: THIS PLAYER's value. A player who has never changed the setting has
	// no score on the objective at all, which is not the same as 0: `matches -2147483648..` is the
	// only way to ask "does this player have a score here", because every comparison against an
	// absent score is false. So the default stands until the player has actually chosen something,
	// and a setting that defaults to ON is ON for everyone who has never opened the menu.
	pack.define(Settings.user) {
		raw("\$scoreboard players set #setting ${Obj.SET_TMP} \$(default)")
		raw("\$execute if score @s \$(obj) matches -2147483648.. store result score #setting ${Obj.SET_TMP} run scoreboard players get @s \$(obj)")
		returnSetting()
	}

	// `enabled {block}`: 1 when the block type may be used, 0 when disabled.
	//
	// Stored as a list of what is OFF rather than a flag per block that is ON. A block nobody has
	// ever disabled then needs no entry at all, which means adding a block to the pack does not
	// require a settings migration to make it work.
	pack.define(Settings.enabled) {
		setTmp("#setting", 1)
		raw("\$execute if data storage ra:settings disabled[{b:\"\$(block)\"}] run scoreboard players set #setting ${Obj.SET_TMP} 0")
		returnSetting()
	}

	// `user_str {key, default}`: this player's value for a text setting, into storage
	// ra:settings out. The default stands until the player has actually typed something -- an
	// unset setting and a setting the player deliberately emptied are not the same thing.
	pack.define(Fns.userStr) {
		// A macro whose arguments are incomplete fails without running a single one of its lines,
		// so a row that omits `default` used to take its whole row down with it -- the setting
		// simply did not appear. Fill it in first.
		execute {
			unlessCondition { data(S, "cur.default") }
			run { data(S) { modify("cur.default") { set(NbtString("")) } } }
		}
		sCopy("put.key", "cur.key")
		data(S) { modify("put.u") { set(self(), "UUID") } }
		raw("\$data modify storage ra:settings out set value \"\$(default)\"")
		function(userStrAt, S, "put")
		sRemove("put")
	}

	// Overwrite the default with this player's value, if they have one.
	pack.define(userStrAt) {
		raw("\$execute if data storage ra:settings users[{u:\$(u)}].\"\$(key)\" run data modify storage ra:settings out set from storage ra:settings users[{u:\$(u)}].\"\$(key)\"")
	}
}

private fun io.github.ayfri.kore.functions.Function.returnSetting() =
	returnRun { scoreboard.players.get(tmp("#setting"), Obj.SET_TMP) }
