package ra.lib.util

import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.lib.fake

/** `ra_lib:util/...`. */
object Util {
	fun Ra.registerUtil() {
		// `property {name, default, min}`: read a numeric property off this marker into
		// #prop ra.temp. Context: as the marker.
		//
		// Four commands, one macro instantiation, no storage and no sub-calls. That matters because
		// this sits in per-tick paths -- every consumer, every bridge, every drain, every tick.
		//
		// WHAT IT GUARDS
		// `execute store result ... run data get <missing path>` writes **zero** when the read
		// fails, and zero is never a sane period or rate: it means "every tick" to a counter and
		// "free" to a cost. The `if data` guard keeps the default standing instead of letting a
		// failed read overwrite it, and the floor catches a value that is present but nonsensical.
		//
		// WHAT IT NO LONGER DOES
		// It used to also re-parse strings through a macro and write the repaired value back,
		// because a number stored as the text "5" reads through `data get` as its LENGTH -- 1 --
		// and nothing errors. That belongs at the source, and now lives there: the Data Handler's
		// registry declares which names are numeric, so the editor writes an int and the string
		// never gets created. Carrying the repair in the hot path as well cost a macro, three
		// storage writes and two extra function calls on every read, to fix something that can no
		// longer happen.
		//
		// A value that is ALREADY a string in an old world still reads as its length. It is
		// corrected the first time the property is edited, because the Handler now offers a number
		// editor for it.
		pack.define(RaLib.Util.property) {
			val prop = fake("#prop")
			val min = fake("#prop.min")
			raw("\$scoreboard players set #prop ${Obj.TEMP} \$(default)")
			raw("\$scoreboard players set #prop.min ${Obj.TEMP} \$(min)")

			raw("\$execute if data entity @s data.properties.\$(name) store result score #prop ${Obj.TEMP} run data get entity @s data.properties.\$(name) 1")

			execute {
				ifCondition { score(prop, Obj.TEMP) lessThan score(min, Obj.TEMP) }
				run { scoreboard.players.operation(prop, Obj.TEMP, Operation.SET, min, Obj.TEMP) }
			}
		}
	}
}
