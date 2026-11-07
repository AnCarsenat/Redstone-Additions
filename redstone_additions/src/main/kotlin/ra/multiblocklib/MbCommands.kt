package ra.multiblocklib

import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Storage

/**
 * Small command helpers shared by the ra_lib_multiblock files. Everything here works on
 * `storage ra:multiblock` and the `ra.multiblock` objective, which is all this library touches.
 */
internal val MB = Storage.MULTIBLOCK

/** A fake player (`#mb_x`, `#mb_result`...) on the `ra.multiblock` objective. */
internal fun mb(name: String) = literal(name)

/** `data modify storage ra:multiblock <to> set from storage ra:multiblock <from>`. */
internal fun Function.mbCopy(to: String, from: String) = data(MB) { modify(to) { set(MB, from) } }

/** `data remove storage ra:multiblock <path>`, once per path. */
internal fun Function.mbRemove(vararg paths: String) = paths.forEach { data(MB) { remove(it) } }

/** `execute unless data storage ra:multiblock <path> run return <value>`. */
internal fun Function.returnUnlessMb(path: String, value: Int = 0) = execute {
	unlessCondition { data(MB, path) }
	run { returnValue(value) }
}

/** `execute if score <holder> ra.multiblock matches <value> run return <ret>`. */
internal fun Function.returnIfMbScore(holder: String, value: Int, ret: Int) = execute {
	ifCondition { score(mb(holder), Obj.MULTIBLOCK) equalTo value }
	run { returnValue(ret) }
}
