package ra.core

import io.github.ayfri.kore.functions.Function
import ra.core.block.readProperty

/**
 * ADMINS: who they are, and what they may do past a player's limits.
 *
 * WHO. A player holding the tag [TAG], `ra.admin`. Opening the server settings
 * (`/function ra_settings:admin`, permission level 2) is what grants it, and it persists until
 * revoked (see ra.settings.Admin). Every "is this player an admin?" in the pack is a test of this
 * one tag; nothing else (operator level, game mode) counts.
 *
 * WHAT. Two things only an admin may do:
 *   - change an `adminOnly` property (see [PropSpec.adminOnly]);
 *   - set a number above its player maximum, up to its `adminMax` (ADMINS BYPASS RANGE LIMITS).
 *
 * RANGE LIMITS. A range or radius declares two ceilings:
 *
 *     number("radius", min = 1, max = 100, adminMax = BigTorch.ADMIN_MAX_RADIUS)
 *
 * `max` is what a player may set. `adminMax` is what an admin may set, and it is a TECHNICAL cap,
 * not a balance one: the size past which the block would hurt the server (a selector's sphere
 * grows with the cube of its radius). Generated into the schema as `admin_max`.
 *
 * WHERE THE LIMIT IS CHECKED. At EDIT time against `max` (or `admin_max` for an admin): the Data
 * Handler's number form and typed number, the Clipboard's paste, the settings menu's defaults. At
 * USE time only against `adminMax` ([readCappedProperty]). A block therefore keeps a range an
 * admin gave it, whoever triggers it later, with no "who set this" state on the block: a value
 * above `max` can only have been written by an admin (or the Creative Data Handler), and the menus
 * show it marked so ([overPlayerMax]).
 */
object Admin {
	/** On a player: server-settings access. The one source of truth for "is an admin". */
	const val TAG = "ra.admin"

	/** The schema key of a number's admin ceiling. */
	const val MAX_KEY = "admin_max"

	/** What a row says next to an admin-only property or an admin-range value. */
	const val MARK = " (admin)"

	/** Why an admin-only property is locked, and what its mark says on hover. */
	const val ONLY_REASON = "Only server admins (server-settings access) can change this."

	/** Over-range mark's hover: a number above the players' maximum. */
	const val OVER_REASON = "Above the players' maximum: only a server admin can set this. The block keeps it."

	/** Its colour: the gold of the menus' own headings, so it reads as a label, not an error. */
	const val MARK_COLOR = "gold"

	/**
	 * Edit time: the bound in score [max] (objective [obj]) becomes the declaration's `admin_max`
	 * when [adminCond] (an execute condition, `if score #dh.adm ra.temp matches 1`) holds.
	 * [spec] is where the declaration is, `storage ra:dh spec`. Without an `admin_max` the bound
	 * stays as it is: only a declared range is bypassed.
	 */
	fun Function.raiseMaxForAdmin(spec: String, max: String, obj: String, adminCond: String) {
		raw("execute $adminCond if data $spec.$MAX_KEY store result score $max $obj run data get $spec.$MAX_KEY")
	}

	/**
	 * Display: score [into] = 1 when the number at [value] is above the player maximum of the
	 * declaration at [spec] and that declaration has an `admin_max` (so only an admin could have
	 * set it), else 0. Scratch score `#adm.v`.
	 */
	fun Function.overPlayerMax(spec: String, value: String, into: String, obj: String) {
		// No early return: this is spliced into a longer function.
		val has = "if data $spec.$MAX_KEY if data $spec.max if data $value"
		raw("scoreboard players set $into $obj 0")
		raw("execute $has store result score #adm.v $obj run data get $value")
		raw("execute $has store result score #adm.m $obj run data get $spec.max")
		raw("execute $has if score #adm.v $obj > #adm.m $obj run scoreboard players set $into $obj 1")
	}

	/**
	 * Use time: `data.properties.<name>` of `@s` into `#prop ra.temp`, [default] when missing,
	 * never below [min] and never above [adminMax], the property's technical ceiling. NOT clamped
	 * to the player maximum: that was checked when the value was written, and clamping here would
	 * undo a range an admin set. As the block's marker.
	 *
	 * [writeBack]: a value above the cap is also corrected on the block, so the Goggles and the
	 * Data Handler (which read the stored value) agree with what the block does.
	 */
	fun Function.readCappedProperty(name: String, default: Int, min: Int, adminMax: Int, writeBack: Boolean = false) {
		readProperty(name, default, min)
		val over = "execute if score #prop ${Obj.TEMP} matches ${adminMax + 1}.. run"
		if (writeBack) raw("$over data modify entity @s data.properties.$name set value $adminMax")
		raw("$over scoreboard players set #prop ${Obj.TEMP} $adminMax")
	}
}
