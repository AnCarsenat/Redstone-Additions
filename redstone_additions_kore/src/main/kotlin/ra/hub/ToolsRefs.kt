package ra.hub

import ra.core.Ns
import ra.core.ref

/**
 * Entry points of the tools (`ra:tools/...`) that `ra:load` and `ra:tick` call and that have no reference in
 * `ra/api` (only the hub calls them). `ToolsPart` owns and defines them at exactly these paths.
 */
internal object ToolsRefs {
	val wrenchInitRegistry = ref(Ns.RA, "tools/wrench/init_registry")
	val readonlyInitRegistry = ref(Ns.RA, "tools/readonly/init_registry")
	val dataHandlerInitRegistry = ref(Ns.RA, "tools/data_handler/init_registry")

	val dataHandlerTick = ref(Ns.RA, "tools/data_handler/tick")
	val gogglesTick = ref(Ns.RA, "tools/goggles/tick")
	val wrenchMenuAction = ref(Ns.RA, "tools/wrench/menu_action")
}
