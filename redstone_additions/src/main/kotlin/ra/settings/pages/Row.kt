package ra.settings.pages

import ra.core.Ns

/**
 * One settings page: what used to be `tools/settings/<page>.json`.
 *
 * Add a page by adding a file here and listing it in [SettingsPages.all]. Nothing else: the
 * generator in `ra.settings.gen` turns it into the menu registry, the defaults and the operator
 * function tree, so there is no list anywhere that has to be kept in step with it.
 */
data class Page(
	val id: String,
	val title: String,
	val namespace: Ns,
	val rows: List<Row>,
	/**
	 * The id of the page this one is a SUBCATEGORY of, or null for a top-level page. A subcategory
	 * is not on the server settings index: its parent's page lists it under "Subcategories", and
	 * its Back returns to the parent. The parent must be a page with server rows of its own.
	 */
	val parent: String? = null,
)

/**
 * Scope decides how a setting is REACHED, not just who may change it.
 *
 * GLOBAL generates `/function ra_settings:admin/<page>/<label>/<action>`. That needs permission
 * level 2, and autocompletes, which is the only way an operator discovers a setting without
 * being told its name. It does NOT appear in the in-game menu at all.
 *
 * USER appears in the /trigger menu and nowhere else, and needs [ScopedRow.obj]: a short stable
 * objective name. A player's saved choice is found by that name, so renaming one resets
 * everybody. Keep it under 16 characters.
 *
 * Anything that alters game balance is global; anything that alters what one person sees or
 * hears is user. A per-player EU cost would let one player mine cheaper than someone beside them.
 */
enum class Scope(val id: String) { GLOBAL("global"), USER("user") }

sealed interface Row {
	val type: String
	val label: String
}

/** A row stored under a key, either world-wide or per player. */
sealed interface ScopedRow : Row {
	val key: String
	val scope: Scope
	/** The objective of a USER row; null for GLOBAL rows. */
	val obj: String?
}

data class BoolRow(
	override val key: String,
	override val label: String,
	override val scope: Scope,
	val default: Int,
	override val obj: String? = null,
) : ScopedRow {
	override val type get() = "bool"
}

data class IntRow(
	override val key: String,
	override val label: String,
	override val scope: Scope,
	val default: Int,
	val min: Int,
	val max: Int,
	val step: Int,
	override val obj: String? = null,
) : ScopedRow {
	override val type get() = "int"
}

/** Text. Typed, never stepped. */
data class StrRow(
	override val key: String,
	override val label: String,
	override val scope: Scope,
	val default: String = "",
	override val obj: String? = null,
) : ScopedRow {
	override val type get() = "str"
}

/**
 * A cycle through fixed choices. Stores the INDEX, not the string, so renaming a choice moves
 * the label without orphaning what anybody had selected.
 */
data class ListRow(
	override val key: String,
	override val label: String,
	override val scope: Scope,
	val default: Int,
	val values: List<String>,
	override val obj: String? = null,
) : ScopedRow {
	override val type get() = "list"
}

/** Whether a placeable may be placed. Disabling never touches blocks already standing in the world. */
data class BlockRow(val block: String, override val label: String) : Row {
	override val type get() = "block"
}

/**
 * The default for a per-block property, written onto the marker when it is placed -- see
 * ra_settings:placement/seed for why it is applied there and not inside ra_lib:util/property.
 */
data class PropRow(
	val block: String,
	val prop: String,
	override val label: String,
	val default: Int,
	val min: Int,
	val max: Int,
	val step: Int,
) : Row {
	override val type get() = "prop"
}

/**
 * Text with no value and no buttons. A page whose settings only apply to part of a module needs
 * to say so where they are, not in a wiki page nobody has open at the time.
 */
data class NoteRow(override val label: String) : Row {
	override val type get() = "note"
}

/** A rule between groups of rows. */
data class SepRow(override val label: String = "") : Row {
	override val type get() = "sep"
}

/** Several [BlockRow]s from `block id to label` pairs, in order. */
fun blocks(vararg rows: Pair<String, String>) = rows.map { (block, label) -> BlockRow(block, label) }
