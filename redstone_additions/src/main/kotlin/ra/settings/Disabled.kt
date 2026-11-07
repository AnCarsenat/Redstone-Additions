package ra.settings

import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.SettingsObjectives.ADMIN_TAG

/**
 * `ra_settings:disabled`: every block currently switched off, and a button to switch each back on.
 *
 * The disabled list is stored as what is OFF, which keeps the common case free -- but it also
 * means a disabled block leaves no trace anywhere a player looks. The block simply refuses to
 * place. This is the page that answers "why can I not build this", and it is why ra:load says
 * something when the list is not empty.
 */
internal fun Ra.registerDisabled() {
	val step = ref(Ns.SETTINGS, "disabled_step")
	val row = ref(Ns.SETTINGS, "disabled_row")
	val rowShow = ref(Ns.SETTINGS, "disabled_row_show")

	pack.define(Fns.disabled) {
		raw("""tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{text:"Disabled Blocks",color:"red",bold:true},{text:" ───",color:"dark_gray"}]""")

		raw("""execute unless data storage ra:settings disabled[0] run tellraw @s [{text:"  Nothing is disabled — every block can be placed.",color:"green"}]""")
		returnUnlessS("disabled[0]")

		sCopy("dscan", "disabled")
		function(step)
		sRemove("dscan")

		raw("""tellraw @s [{text:"  Disabled blocks cannot be PLACED. Ones already built keep working,",color:"dark_gray"}]""")
		raw("""tellraw @s [{text:"  and the item can still be crafted and held.",color:"dark_gray"}]""")
	}

	// Walk the disabled list.
	pack.define(step) {
		returnUnlessS("dscan[0]")
		function(row, S, "dscan[0]")
		sRemove("dscan[0]")
		function(step)
	}

	// `disabled_row {b}`: one disabled block, with the button that re-enables it.
	//
	// The stored form is just a block name, so the readable label and the action code are looked
	// up in the generated blockmap. A name with no entry is still shown -- raw, and without a
	// button -- because a block removed from the pack while disabled would otherwise vanish from
	// this page and stay disabled forever with nothing to point at.
	pack.define(row) {
		sRemove("drow")
		raw("\$data modify storage ra:settings drow set from storage ra:settings blockmap[{b:\"\$(b)\"}]")
		execute {
			ifCondition { data(S, "drow") }
			run { function(rowShow, S, "drow") }
		}
		raw("""${'$'}execute unless data storage ra:settings drow run tellraw @s [{text:"  ${'$'}(b)",color:"red"},{text:"  (no longer in this pack)",color:"dark_gray"}]""")
		sRemove("drow")
	}

	// `disabled_row_show {label, code}`. The button is shown only to somebody who can press it.
	// The dispatcher would refuse anyone else and the objective is not even enabled for them, so
	// offering it would be offering a button whose whole behaviour is to say no.
	pack.define(rowShow) {
		raw("""${'$'}execute unless entity @s[tag=$ADMIN_TAG] run tellraw @s [{text:"  "},{text:"${'$'}(label)",color:"red"}]""")
		raw("""${'$'}execute if entity @s[tag=$ADMIN_TAG] run tellraw @s [{text:"  "},{text:"${'$'}(label)",color:"red"},{text:"  "},{text:"[ Enable ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Allow ${'$'}(label) to be placed again"},click_event:{action:"run_command",command:"/trigger ra.settings.admin set ${'$'}(code)"}}]""")
	}
}
