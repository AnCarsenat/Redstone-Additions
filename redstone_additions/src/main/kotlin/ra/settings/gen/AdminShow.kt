package ra.settings.gen

import io.github.ayfri.kore.functions.Function
import ra.core.raw
import ra.settings.gen.AdminRows.action
import ra.settings.gen.Text.SPACE
import ra.settings.gen.Text.btn
import ra.settings.gen.Text.gpath
import ra.settings.gen.Text.ppath
import ra.settings.gen.Text.slug
import ra.settings.gen.Text.value
import ra.settings.pages.BlockRow
import ra.settings.pages.BoolRow
import ra.settings.pages.IntRow
import ra.settings.pages.ListRow
import ra.settings.pages.NoteRow
import ra.settings.pages.PropRow
import ra.settings.pages.Row
import ra.settings.pages.ScopedRow
import ra.settings.pages.SepRow
import ra.settings.pages.StrRow

/**
 * One row of a page overview (`ra_settings:admin/<page>/show`): the value, then the buttons that
 * change it.
 *
 * The overview used to print values only, which meant reading a setting here and then typing its
 * full path somewhere else to change it. Everything on these pages is operator-only anyway --
 * they are reached through /function -- so the buttons cost nothing and remove the retyping.
 *
 * Buttons register their action codes as they are built, so the order of the [ActionTable.act]
 * calls below is the order of the codes, and must stay as it is.
 */
internal object AdminShow {
	fun lines(row: Row, pageId: String, actions: ActionTable): List<Function.() -> Unit> {
		val label = row.label
		fun fn(name: String) = action(pageId, label, name)

		when (row) {
			is NoteRow -> return listOf { raw("""tellraw @s [{text:"  $label",color:"dark_gray",italic:true}]""") }
			is SepRow -> return listOf { raw("""tellraw @s [{text:"  ————————————————",color:"dark_gray"}]""") }
			is BlockRow -> {
				val on = """{text:"  $label: ",color:"white"},{text:"enabled ",color:"green"},""" +
					btn("Disable", fn("disable"), "red", "Stop $label being placed", actions.act(fn("disable")))
				val off = """{text:"  $label: ",color:"white"},{text:"disabled ",color:"red"},""" +
					btn("Enable", fn("enable"), "green", "Allow $label again", actions.act(fn("enable")))
				val disabled = "disabled[{b:\"${row.block}\"}]"
				return listOf(
					// Whole lines: Kore moves a raw() inside execute-run into a generated scope.
					{ raw("execute unless data storage ra:settings $disabled run tellraw @s [$on]") },
					{ raw("execute if data storage ra:settings $disabled run tellraw @s [$off]") },
				)
			}
			else -> {}
		}

		val path = when (row) {
			is PropRow -> ppath(row.block, row.prop)
			is ScopedRow -> gpath(row.key)
			else -> error("unreachable")
		}
		val head = """{text:"  $label: ",color:"white"},${value(path)},{text:"  "}"""

		fun b(text: String, name: String, color: String, hover: String?) = btn(text, fn(name), color, hover, actions.act(fn(name)))

		val buttons = when (row) {
			is BoolRow -> b("On", "on", "green", null) + SPACE + b("Off", "off", "red", null)
			is IntRow, is PropRow -> {
				val step = if (row is IntRow) row.step else (row as PropRow).step
				b("-", "down", "red", "Down by $step") + SPACE +
					b("+", "up", "green", "Up by $step") + SPACE +
					b("Set", "edit", "yellow", "Type an exact value") + SPACE +
					b("Reset", "reset", "gray", null) +
					(if (row is PropRow) SPACE + b("Apply to placed", "apply_to_existing", "gold", "Push this onto blocks already in the world (overwrites wrench values)") else "")
			}
			is StrRow -> b("Set", "edit", "yellow", "Type a new value") + SPACE + b("Reset", "reset", "gray", null)
			is ListRow -> row.values.joinToString(SPACE) { b(it, slug(it), "yellow", null) }
			else -> """{text:""}"""
		}

		// An on/off value reads "on"/"off", not the stored 1b/0b: "Show the load message: 0b" does
		// not look like a switch. 0b is off and anything else on, the same test the load-message
		// gate makes (ra.core.LoadMessages).
		if (row is BoolRow) {
			val off = "global{\"${row.key}\":0b}"
			fun line(word: String, color: String) =
				"""tellraw @s [{text:"  $label: ",color:"white"},{text:"$word",color:"$color"},{text:"  "},$buttons]"""
			return listOf(
				// Whole lines: Kore moves a raw() inside execute-run into a generated scope.
				{ raw("execute unless data storage ra:settings $off run ${line("on", "green")}") },
				{ raw("execute if data storage ra:settings $off run ${line("off", "red")}") },
			)
		}

		return listOf { raw("tellraw @s [$head,$buttons]") }
	}
}
