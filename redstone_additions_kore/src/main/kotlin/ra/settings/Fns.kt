package ra.settings

import ra.core.Ns
import ra.core.ref

/**
 * The ra_settings functions called from more than one file of this part. Entry points other
 * parts call are in [ra.api.Settings]; one-file helpers are declared next to their definition.
 */
internal object Fns {
	private fun s(path: String) = ref(Ns.SETTINGS, path)

	// Generated from the pages (ra.settings.gen).
	val pages = s("pages")
	val defaults = s("defaults")
	val sync = s("sync")
	val blockmap = s("blockmap")
	val adminPages = s("admin_pages")
	val adminActions = s("admin_actions")

	val hooks = s("hooks")
	val api = s("api")

	val menu = s("menu")
	val openClick = s("open_click")
	val serverOpen = s("server_open")
	val disabled = s("disabled")

	val pageOpen = s("page/open")

	val act = s("act")

	val editStart = s("edit_start")
	val applyPending = s("apply_pending")
	val applyEdit = s("apply_edit")
	val userStr = s("user_str")

	val adminDispatch = s("admin_dispatch")
	val adminGrant = s("admin_grant")
	val adminCall = s("admin_call")
	val adminRefresh = s("admin_refresh")

	/** Cross-namespace: owned by the Hub (`ra:uninstall`), offered from the server-settings index. */
	val raUninstall = ref(Ns.RA, "uninstall")
}
