package ra.settings.gen

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.commands.DataModifyOperation
import io.github.ayfri.kore.arguments.Argument
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.buildNbtCompound
import ra.core.Ns
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.S
import ra.settings.SettingsObjectives
import ra.settings.gen.AdminRows.blockDisable
import ra.settings.gen.AdminRows.blockEnable
import ra.settings.gen.AdminRows.bool
import ra.settings.gen.AdminRows.int
import ra.settings.gen.AdminRows.list
import ra.settings.gen.AdminRows.prop
import ra.settings.gen.AdminRows.str
import ra.settings.gen.Text.gpath
import ra.settings.gen.Text.ppath
import ra.settings.pages.BlockRow
import ra.settings.pages.BoolRow
import ra.settings.pages.IntRow
import ra.settings.pages.ListRow
import ra.settings.pages.NoteRow
import ra.settings.pages.Page
import ra.settings.pages.PropRow
import ra.settings.pages.Row
import ra.settings.pages.Scope
import ra.settings.pages.ScopedRow
import ra.settings.pages.SepRow
import ra.settings.pages.StrRow

/**
 * Turns the settings pages into functions: what `tools/settings_gen.py` did at beet build time.
 *
 * Global rows generate an admin function tree and no menu entry. User rows generate a menu entry
 * and no admin function -- a player changes their own with /trigger, which needs no permissions,
 * and an operator has no business setting it for them.
 *
 * The pages are walked once, in order, and every action code is handed out as it is met
 * ([ActionTable]); [Registries] then writes the tables the runtime reads.
 */
internal class SettingsGenerator(private val pack: DataPack, private val pages: List<Page>) {
	val actions = ActionTable()

	/** `ra_settings:blockmap` entries: block -> label and the code that re-enables it. */
	val blockmap = mutableListOf<String>()

	/** Lines of `ra_settings:defaults` after the three container seeds, in page/row order. */
	val defaults = mutableListOf<Function.() -> Unit>()

	/** (page id, title) of every page with operator rows, in order. Its index is `ra.settings.apage`. */
	val adminIndex = mutableListOf<Pair<String, String>>()

	/** Pages as the /trigger menu sees them: user rows only. */
	val menuPages = mutableListOf<Page>()

	/** User rows seeded every tick by `ra_settings:sync`. */
	val userSeed = mutableListOf<ScopedRow>()

	fun generate() {
		for (page in pages) generatePage(page)
		Registries(pack, this).write()
	}

	private fun generatePage(page: Page) {
		val userRows = mutableListOf<Row>()
		val adminRows = mutableListOf<Row>()

		for (row in page.rows) {
			if (row is ScopedRow && row.scope == Scope.USER) {
				val obj = requireNotNull(row.obj) { "user row ${row.key} on page ${page.id} needs an objective" }
				defaults += { scoreboard.objectives.add(obj) }
				if (row is BoolRow || row is IntRow || row is ListRow) userSeed += row
				userRows += row
				continue
			}
			adminRows += row
			generateAdminRow(page.id, row)
		}

		if (adminRows.isNotEmpty()) {
			showPage(page, adminRows)
			adminIndex += page.id to page.title
		}
		if (userRows.isNotEmpty()) menuPages += page.copy(rows = userRows)
	}

	private fun generateAdminRow(pageId: String, row: Row) {
		when (row) {
			// Decoration, not a setting: nothing to seed, nothing to click.
			is NoteRow, is SepRow -> {}

			is BlockRow -> {
				val enable = pack.blockEnable(pageId, row)
				// So ra_settings:disabled can name a block and offer the button that turns it back
				// on, without hard-coding either.
				blockmap += "{b:\"${row.block}\",label:\"${row.label}\",code:${actions.act(enable)}}"
				pack.blockDisable(pageId, row)
			}

			is PropRow -> {
				seed(ppath(row.block, row.prop)) { set(row.default) }
				pack.prop(pageId, row)
			}

			is BoolRow -> {
				seed(gpath(row.key)) { set(NbtByte(row.default.toByte())) }
				pack.bool(pageId, row)
			}

			is IntRow -> {
				seed(gpath(row.key)) { set(row.default) }
				pack.int(pageId, row)
			}

			is StrRow -> {
				seed(gpath(row.key)) { set(NbtString(row.default)) }
				pack.str(pageId, row)
			}

			is ListRow -> {
				seed(gpath(row.key)) { set(row.default) }
				pack.list(pageId, row)
			}
		}
	}

	/** `execute unless data storage ra:settings <path> run data modify storage ra:settings <path> set value <v>`. */
	private fun seed(path: String, value: DataModifyOperation.() -> List<Argument>) {
		defaults += {
			execute {
				unlessCondition { data(S, path) }
				run { data(S) { modify(path) { value() } } }
			}
		}
	}

	/** `ra_settings:admin/<page>/show`: the page's current values, each with its buttons. */
	private fun showPage(page: Page, rows: List<Row>) {
		val index = adminIndex.size
		val (blocks, values) = rows.partition { it is BlockRow }
		// Tunables and on/off switches are different kinds of decision, so they are drawn as two
		// groups with a rule between them rather than one undifferentiated column.
		val valueLines = values.flatMap { AdminShow.lines(it, page.id, actions) }
		val needsRule = blocks.isNotEmpty() && values.any { it !is NoteRow && it !is SepRow }
		val blockLines = blocks.flatMap { AdminShow.lines(it, page.id, actions) }
		val back = Text.btn("Back", AdminTree.show, "gray", "Back to all server settings", actions.act(AdminTree.show, nav = true))

		pack.define(AdminTree.pageShow(page.id)) {
			// Records which page this is, so an action can redraw it afterwards. Per player,
			// because two operators can be on different pages at once.
			scoreboard.players.set(self(), SettingsObjectives.APAGE, index)
			raw("tellraw @s ${Text.bar(page.title)}")
			valueLines.forEach { it() }
			if (needsRule) raw("""tellraw @s [{text:"  ———— blocks ————",color:"dark_gray"}]""")
			blockLines.forEach { it() }
			raw("""tellraw @s [{text:""}]""")
			raw(Text.buttonLine(back))
		}
	}
}

/** Paths of the operator tree that are not per-row actions. */
internal object AdminTree {
	/** The server settings index. */
	val show = ra.api.Settings.Admin.show

	fun pageShow(pageId: String) = ref(Ns.SETTINGS, "admin/$pageId/show")
}
