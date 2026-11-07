package ra.lib

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.ref

/**
 * Small helpers shared by the ra_lib files. Nothing here reaches outside `ra_lib`; the public
 * entry points are the refs in [ra.api.RaLib].
 */

/** `ra_lib:<path>`. */
internal fun lib(path: String): FunctionArgument = ref(Ns.LIB, path)

/** A fake player (`#facing`, `#rs`, `#inv_len`...). */
internal fun fake(name: String) = literal(name)

/** `scoreboard objectives add <name> dummy`, once per name. */
internal fun Function.dummyObjectives(vararg names: String) = names.forEach { scoreboard.objectives.add(it) }
