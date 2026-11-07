package ra.settings.gen

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.allPlayers
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.buildNbtList
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.settings.Fns
import ra.settings.S
import ra.settings.SettingsObjectives
import ra.settings.pages.BoolRow
import ra.settings.pages.IntRow
import ra.settings.pages.ListRow
import ra.settings.pages.Page
import ra.settings.pages.ScopedRow
import ra.settings.pages.StrRow

/**
 * The tables and seeders the runtime reads, written from what [SettingsGenerator] collected:
 * `pages`, `defaults`, `sync`, `uninstall`, the admin index, `blockmap`, `admin_pages`,
 * `admin_actions` and the `#ra_settings:register` tag that rebuilds them on every load.
 */
internal class Registries(private val pack: DataPack, private val gen: SettingsGenerator) {
	fun write() {
		// The registry the /trigger menu renders from. User rows only: operator settings are not
		// in the menu at all, so there is nothing there for a player to click and be refused.
		pack.define(Fns.pages) {
			data(S) { modify("pages") { set(buildNbtList<Nothing> { }) } }
			for (page in gen.menuPages) raw("data modify storage ra:settings pages append value ${Text.snbt(menuEntry(page))}")
		}

		// Seeds every declared setting that has no value yet. Runs on every load, so it must never
		// overwrite: `unless data` is what makes it a seed and not a reset.
		pack.define(Fns.defaults) {
			for (container in listOf("global" to buildNbtCompound { }, "disabled" to buildNbtList<Nothing> { }, "users" to buildNbtList<Nothing> { })) {
				execute {
					unlessCondition { data(S, container.first) }
					run { data(S) { modify(container.first) { set(container.second) } } }
				}
			}
			gen.defaults.forEach { it() }
		}

		// Every tick, give any player who has no score on a user setting its default.
		//
		// This is what makes the gated selectors work at all. A player who has never opened the
		// menu has no score, and an ABSENT score fails every comparison -- so
		// @a[scores={ra.u.snd=1..}] would exclude them and they would hear nothing. Seeding at
		// load would not do: players join later than load.
		pack.define(Fns.sync) {
			for (row in gen.userSeed) {
				val obj = row.obj!!
				execute {
					asTarget(allPlayers())
					unlessCondition { score(self(), obj) matches rangeOrIntStart(Int.MIN_VALUE) }
					run { scoreboard.players.set(self(), obj, default(row)) }
				}
			}
		}

		writeAdminIndex()

		pack.define(Fns.blockmap) {
			raw("data modify storage ra:settings blockmap set value [${gen.blockmap.joinToString(",")}]")
		}

		// Uninstall, generated so it cannot drift from what was created. Every objective this
		// system adds is in the page data; hand-maintaining a second copy is how the old
		// uninstall ended up missing half of them. Called from ra:uninstall/confirm.
		pack.define(Settings.uninstall) {
			for (obj in gen.userSeed.mapNotNull { it.obj }.toSortedSet()) scoreboard.objectives.remove(obj)
			for (obj in SettingsObjectives.REMOVED_ON_UNINSTALL) scoreboard.objectives.remove(obj)
			data(S) { remove("{}") }
			tag(allPlayers()) { remove(SettingsObjectives.ADMIN_TAG) }
		}

		// Index -> page show function, so an action can redraw the page it belongs to.
		pack.define(Fns.adminPages) {
			val paths = gen.adminIndex.joinToString(",") { (id, _) -> "\"${AdminTree.pageShow(id).asId()}\"" }
			raw("data modify storage ra:settings apages set value [$paths]")
		}

		// The dispatch table. A macro runs the path, so this is one command rather than a
		// comparison per action.
		pack.define(Fns.adminActions) {
			raw("data modify storage ra:settings actions set value ${gen.actions.snbt()}")
		}

		for (fn in listOf(Fns.pages, Fns.defaults, Fns.adminActions, Fns.adminPages, Fns.blockmap)) {
			pack.hook(Ns.SETTINGS, "register", fn)
		}
	}

	/**
	 * `ra_settings:admin/show`, the server settings index. Context: as an operator.
	 *
	 * Opening this is what grants ra.admin, so every button below can be a /trigger instead of a
	 * /function and stop raising a confirmation prompt on each click. Reaching this function at
	 * all needs permission level 2, so the tag cannot be obtained any other way -- see
	 * ra_settings:admin_grant.
	 */
	private fun writeAdminIndex() {
		val pageButtons = gen.adminIndex.map { (id, title) ->
			val show = AdminTree.pageShow(id)
			Text.btn(title, show, "yellow", "Open $title", gen.actions.act(show, nav = true))
		}
		val disabled = Text.btn("Disabled blocks", Fns.disabled, "red", "List every block that cannot be placed", gen.actions.act(Fns.disabled, nav = true))
		val uninstall = Text.btn("Uninstall", Fns.raUninstall, "dark_red", "Remove Redstone Additions from this world", gen.actions.act(Fns.raUninstall, nav = true))

		pack.define(AdminTree.show) {
			function(Fns.adminGrant)
			raw("tellraw @s ${Text.bar("Redstone Additions — Server Settings")}")
			pageButtons.forEach { raw(Text.buttonLine(it)) }
			raw("""tellraw @s [{text:""}]""")
			raw(Text.buttonLine(disabled))
			raw(Text.buttonLine(uninstall))
			raw("""tellraw @s [{text:"Players change their own preferences with ",color:"gray"},{text:"/trigger ra.settings.open",color:"yellow"},{text:" — no operator needed.",color:"gray"}]""")
		}
	}

	private fun default(row: ScopedRow) = when (row) {
		is BoolRow -> row.default
		is IntRow -> row.default
		is ListRow -> row.default
		is StrRow -> error("text settings cannot be seeded on a scoreboard")
	}

	/** A page as the menu registry stores it, with each row's fields in the order the page JSON had them. */
	private fun menuEntry(page: Page): Map<String, Any?> = linkedMapOf(
		"id" to page.id,
		"title" to page.title,
		"rows" to page.rows.map { row ->
			val r = row as ScopedRow
			linkedMapOf<String, Any?>("type" to r.type, "key" to r.key, "label" to r.label, "scope" to r.scope.id).apply {
				if (r.obj != null) put("obj", r.obj)
				when (r) {
					is BoolRow -> put("default", r.default)
					is IntRow -> { put("default", r.default); put("min", r.min); put("max", r.max); put("step", r.step) }
					is ListRow -> { put("default", r.default); put("values", r.values) }
					is StrRow -> put("default", r.default)
				}
			}
		},
	)
}
