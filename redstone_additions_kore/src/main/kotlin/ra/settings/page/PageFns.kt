package ra.settings.page

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.Fns
import ra.settings.S
import ra.settings.SettingsObjectives.PAGE
import ra.settings.SettingsObjectives.VIEWING
import ra.settings.VIEWING_TICKS
import ra.settings.returnUnlessS
import ra.settings.row.RowFns
import ra.settings.sCopy
import ra.settings.sRemove
import ra.settings.setTmp
import ra.settings.storeScore
import ra.settings.tmp

/** `ra_settings:page/` functions: the list of menu pages and the drawing of one page. */
internal object PageFns {
	val openAt = ref(Ns.SETTINGS, "page/open_at")
	val custom = ref(Ns.SETTINGS, "page/custom")
	val list = ref(Ns.SETTINGS, "page/list")
	val listRow = ref(Ns.SETTINGS, "page/list_row")
	val rows = ref(Ns.SETTINGS, "page/rows")

	fun Ra.registerPage() {
		// Draw the page this player selected. Reads which page from ra.settings.page, so a row
		// click can find its way back here.
		pack.define(Fns.pageOpen) {
			storeScore("q.p", self(), PAGE)
			function(openAt, S, "q")
		}

		pack.define(openAt) { openAt() }

		// `page/custom {render}`: hand drawing over to a module's own function.
		pack.define(custom) { raw("\$function \$(render)") }

		// One clickable line per page, numbered by position.
		pack.define(list) {
			returnUnlessS("scan[0]")
			sCopy("cur", "scan[0]")

			// Page N is opened by the code N+4. 0 is undeliverable through /trigger; 1 is the
			// player menu, 2 the server settings and 3 the disabled-blocks list.
			scoreboard.players.operation(tmp("#code"), Obj.SET_TMP, Operation.SET, tmp("#idx"), Obj.SET_TMP)
			scoreboard.players.add(tmp("#code"), Obj.SET_TMP, 4)
			storeScore("cur.i", tmp("#code"), Obj.SET_TMP)
			function(listRow, S, "cur")

			scoreboard.players.add(tmp("#idx"), Obj.SET_TMP, 1)
			sRemove("scan[0]")
			function(list)
		}

		// `page/list_row {title, i}`: one page button.
		pack.define(listRow) {
			raw("""${'$'}tellraw @s [{text:"  "},{text:"[ ${'$'}(title) ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Open this page"},click_event:{action:"run_command",command:"/trigger ra.settings.open set ${'$'}(i)"}}]""")
		}

		pack.define(rows) { rows() }
	}

	/**
	 * `page/open_at {p}`: draw page p. A page may hand its own drawing function (`render`)
	 * instead of a row list, for anything the generic renderer cannot express. That function is
	 * called as the player and is on its own from there.
	 */
	private fun Function.openAt() {
		raw("""${'$'}execute unless data storage ra:settings pages[${'$'}(p)] run return run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"That page no longer exists — a module may have been removed.",color:"gray"}]""")
		raw("\$data modify storage ra:settings cur set from storage ra:settings pages[\$(p)]")

		// Drawing a menu is what makes its buttons usable; see ra_settings:init.
		scoreboard.players.set(self(), VIEWING, VIEWING_TICKS)

		raw("""tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{nbt:"cur.title",storage:"ra:settings",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]""")

		execute {
			ifCondition { data(S, "cur.render") }
			run { function(custom, S, "cur") }
		}
		execute {
			ifCondition { data(S, "cur.render") }
			run { returnValue(0) }
		}

		setTmp("#idx", 0)
		raw("\$data modify storage ra:settings rowscan set from storage ra:settings pages[\$(p)].rows")
		function(rows)
		sRemove("rowscan")

		raw("""tellraw @s [{text:"  "},{text:"[ Back ]",color:"gray",hover_event:{action:"show_text",value:"Back to all pages"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 1"}}]""")
	}

	/**
	 * Draw each row of the current page, numbered by position.
	 *
	 * The number a button carries is the row's index, so the dispatcher looks the same list up
	 * again rather than keeping any state of its own -- the same trick the wrench menu uses, and
	 * for the same reason: state kept between a click and its handler is state that can go stale
	 * when a module reloads.
	 */
	private fun Function.rows() {
		returnUnlessS("rowscan[0]")
		sCopy("cur", "rowscan[0]")

		// Codes are the row index PLUS ONE. /trigger cannot deliver 0 -- a trigger score of 0 is
		// exactly the "not clicked" state the dispatcher waits on -- so row 0 would be a button
		// that silently did nothing.
		code("#code", "#idx", 1, "cur.i")
		// A row with two buttons needs two codes. The second is the first plus 512, well past any
		// real row count, so the dispatcher can split them apart with one comparison.
		code("#up", "#code", 512, "cur.ip")
		// And a third for "Modify", which opens a typed input rather than stepping.
		code("#ed", "#code", 1024, "cur.ie")

		// A user row's score may not exist yet. Seeding it at draw time means the value shown is
		// the value that applies, instead of an empty bracket for everyone who never touched it.
		draw("cur{scope:\"user\"}", RowFns.seedUser)

		draw("cur{type:\"bool\"}", RowFns.bool)
		draw("cur{type:\"int\"}", RowFns.int)
		draw("cur{type:\"prop\"}", RowFns.prop)
		draw("cur{type:\"str\"}", RowFns.str)
		draw("cur{type:\"list\"}", RowFns.list)
		draw("cur{type:\"block\"}", RowFns.block)

		scoreboard.players.add(tmp("#idx"), Obj.SET_TMP, 1)
		sRemove("rowscan[0]")
		function(rows)
	}

	/** `<target> = <source> + <offset>`, stored at `storage ra:settings <path>`. */
	private fun Function.code(target: String, source: String, offset: Int, path: String) {
		scoreboard.players.operation(tmp(target), Obj.SET_TMP, Operation.SET, tmp(source), Obj.SET_TMP)
		scoreboard.players.add(tmp(target), Obj.SET_TMP, offset)
		storeScore(path, tmp(target), Obj.SET_TMP)
	}

	/** `execute if data storage ra:settings <test> run function <fn> with storage ra:settings cur`. */
	private fun Function.draw(test: String, fn: FunctionArgument) = execute {
		ifCondition { data(S, test) }
		run { function(fn, S, "cur") }
	}
}
