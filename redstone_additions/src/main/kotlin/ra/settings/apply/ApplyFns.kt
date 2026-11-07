package ra.settings.apply

import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.S

/** `ra_settings:apply/` functions: store a consumed typed value, one function per kind of edit. */
internal object ApplyFns {
	val global = ref(Ns.SETTINGS, "apply/global")
	val prop = ref(Ns.SETTINGS, "apply/prop")
	val user = ref(Ns.SETTINGS, "apply/user")

	/** `ra_settings:apply_str`, the per-player text store. */
	val str = ref(Ns.SETTINGS, "apply_str")

	fun Ra.registerApply() {
		// `apply/global {key, type}`: a world-wide setting.
		pack.define(global) {
			raw("\$execute if data storage ra:settings edit{type:\"int\"} store result storage ra:settings global.\"\$(key)\" int 1 run data get storage ra:input consume.number")
			raw("\$execute if data storage ra:settings edit{type:\"str\"} run data modify storage ra:settings global.\"\$(key)\" set from storage ra:input consume.text")
			raw("""${'$'}tellraw @s [{text:"[Settings] ",color:"gold"},{text:"${'$'}(key)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"${'$'}(key)\"",storage:"ra:settings",color:"aqua"}]""")
		}

		// `apply/prop {block, prop}`: a typed number as a block's property default.
		pack.define(prop) {
			raw("\$execute store result storage ra:settings global.props.\"\$(block)\".\"\$(prop)\" int 1 run data get storage ra:input consume.number")
			raw("""${'$'}tellraw @s [{text:"[Settings] ",color:"gold"},{text:"${'$'}(block) ${'$'}(prop)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"${'$'}(block)\".\"${'$'}(prop)\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]""")
		}

		// `apply/user {obj, type, key}`: this player's own preference. Numbers go on the objective
		// the row names. Text cannot live on a scoreboard, so it goes to storage keyed by UUID --
		// see ra_settings:apply_str.
		pack.define(user) {
			raw("\$execute if data storage ra:settings edit{type:\"int\"} store result score @s \$(obj) run data get storage ra:input consume.number")
			execute {
				ifCondition { data(S, "edit{type:\"str\"}") }
				run { function(str, S, "edit") }
			}
		}
	}
}
