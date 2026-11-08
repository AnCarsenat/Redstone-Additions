package ra.tools.readonly

import ra.api.Tools
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * `ra:tools/readonly/init_registry`, kept because ra:load calls it.
 *
 * Read-only properties used to be a map of their own, `storage ra:dh readonly`, filled from
 * `ra.readonly(type, ...)`. They are now a flag in each block type's property schema
 * (`number("rate", readonly = true)`, see ra/core/Properties.kt), which
 * `ra:tools/data_handler/init_registry` writes to `storage ra:dh types` with every other flag.
 * A read-only property is still shown by the Data Handler, locked, and never cycled by the wrench.
 */
object Readonly {
	val initRegistry = Tools.Readonly.initRegistry

	fun Ra.registerReadonly() {
		pack.define(initRegistry) { raw("data remove storage ra:dh readonly") }
	}
}
