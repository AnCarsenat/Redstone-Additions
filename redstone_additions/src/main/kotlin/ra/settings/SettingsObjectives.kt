package ra.settings

/**
 * Objectives and tags only ra_settings uses. The shared ones (`ra.set.tmp`, the `ra.u.*` user
 * settings) are in [ra.core.Obj]. All of them are written into worlds, so the spelling is frozen.
 */
internal object SettingsObjectives {
	/**
	 * The objective a player actually types. Named to be remembered rather than to be short: it is
	 * printed once at load as a suggestion and then has to survive in somebody's memory until they
	 * want it. `/trigger ra.settings.open` with no argument adds 1, which is exactly the "open the
	 * root menu" value, so there is no `set 1` for anyone to get wrong.
	 */
	const val OPEN = "ra.settings.open"

	/** Row buttons. Only enabled while a menu is on screen. */
	const val ACT = "ra.settings.act"

	/** Ticks left before a drawn menu's buttons stop working. */
	const val VIEWING = "ra.settings.viewing"

	/** Server-settings buttons. Only ever enabled for a player holding [ADMIN_TAG]. */
	const val ADMIN = "ra.settings.admin"

	/** Which page a player is looking at, so a row click knows what it belongs to. */
	const val PAGE = "ra.settings.page"

	/** Which SERVER settings page an operator is on, so an action can redraw it. */
	const val APAGE = "ra.settings.apage"

	/** The ra_lib:input request this player opened FOR SETTINGS. */
	const val REQ = "ra.settings.req"

	/** 1 while a typed value is awaited. */
	const val PEND = "ra.settings.pend"

	/** Last tick's debug setting, so ra_settings:hooks can act on the change rather than the value. */
	const val DBG_WAS = "ra.u.dbg.was"

	/** The server-settings permission. A role, not a session: it persists until revoked. */
	const val ADMIN_TAG = ra.core.Admin.TAG

	/** What `ra_settings:uninstall` removes besides the user-setting objectives, in its order. */
	val REMOVED_ON_UNINSTALL = listOf(OPEN, ACT, ADMIN, PAGE, PEND, ra.core.Obj.SET_TMP, DBG_WAS, REQ)

	// ra_lib:input's objectives, read here to follow a typed-value session.
	const val INPUT_STATE = "ra.input.state"
	const val INPUT_REQ = "ra.input.req"
	const val INPUT_MODE = "ra.input.mode"
	const val INPUT_MIN = "ra.input.min"
	const val INPUT_MAX = "ra.input.max"
	const val INPUT_BACKEND = "ra.input.backend"
	const val INPUT_ACTIVE_TAG = "ra.input.active"
}
