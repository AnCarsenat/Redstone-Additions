package ra.settings

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Storage

/** Command helpers shared by the ra_settings files: `storage ra:settings` and the `ra.set.tmp` scratch objective. */
internal val S = Storage.SETTINGS

/** A fake player (`#n`, `#v`, `#setting`...) on `ra.set.tmp`. */
internal fun tmp(name: String) = literal(name)

internal fun Function.setTmp(name: String, value: Int) = scoreboard.players.set(tmp(name), Obj.SET_TMP, value)

/** `data remove storage ra:settings <path>`. */
internal fun Function.sRemove(path: String) = data(S) { remove(path) }

/** `data modify storage ra:settings <to> set from storage ra:settings <from>`. */
internal fun Function.sCopy(to: String, from: String) = data(S) { modify(to) { set(S, from) } }

/** `execute unless data storage ra:settings <path> run return 0`. */
internal fun Function.returnUnlessS(path: String) = execute {
	unlessCondition { data(S, path) }
	run { returnValue(0) }
}

/** `execute store result storage ra:settings <path> int 1 run scoreboard players get <holder> <objective>`. */
internal fun Function.storeScore(path: String, holder: io.github.ayfri.kore.arguments.types.ScoreHolderArgument, objective: String) = execute {
	storeResult { storage(S, path, io.github.ayfri.kore.arguments.enums.DataType.INT, 1.0) }
	run { scoreboard.players.get(holder, objective) }
}
