package ra.settings.row

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.Fns
import ra.settings.S
import ra.settings.sCopy
import ra.settings.setTmp
import ra.settings.storeScore
import ra.settings.tmp

/**
 * `ra_settings:row/` functions: one line of a menu page per row type, called with the row
 * (storage ra:settings cur) as macro arguments. `$(i)`, `$(ip)` and `$(ie)` are the codes of the
 * row's first, "up" and "Modify" buttons; see ra_settings:page/rows.
 */
internal object RowFns {
	val seedUser = ref(Ns.SETTINGS, "row/seed_user")
	val bool = ref(Ns.SETTINGS, "row/bool")
	val int = ref(Ns.SETTINGS, "row/int")
	val prop = ref(Ns.SETTINGS, "row/prop")
	val str = ref(Ns.SETTINGS, "row/str")
	val list = ref(Ns.SETTINGS, "row/list")
	val listShow = ref(Ns.SETTINGS, "row/list_show")
	val block = ref(Ns.SETTINGS, "row/block")

	private const val D = "$"

	private fun label() = """{text:"  $D(label): ",color:"white"}"""
	private fun bracket(value: String) = """{text:"[",color:"dark_gray"},$value,{text:"]",color:"dark_gray"}"""
	private const val YOU = """{text:" "},{text:"[you]",color:"aqua"}"""
	private fun button(text: String, color: String, bold: Boolean, hover: String, code: String) =
		"""{text:"[ $text ]",color:"$color"${if (bold) ",bold:true" else ""},hover_event:{action:"show_text",value:"$hover"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $D($code)"}}"""
	private val STEPPERS = button("-", "red", true, "Down by $D(step)", "i") + """,{text:" "},""" +
		button("+", "green", true, "Up by $D(step)", "ip")
	private val MODIFY = button("Modify", "yellow", false, "Type an exact value", "ie")

	fun Ra.registerRows() {
		// Give this player a score on a user setting if they have none.
		pack.define(seedUser) {
			raw("${D}execute unless score @s $D(obj) matches -2147483648.. run scoreboard players set @s $D(obj) $D(default)")
		}

		// An on/off row. Global values read from storage, user values from the player's own score.
		// User rows print "on"/"off" rather than the raw 1/0: a row that answers "0" to "Debug
		// messages" is not obviously a setting that is switched off.
		pack.define(bool) {
			val toggle = { hover: String -> button("Toggle", "yellow", true, hover, "i") }
			raw("""${D}execute if data storage ra:settings cur{scope:"global"} run tellraw @s [${label()},${bracket("""{nbt:"global.$D(key)",storage:"ra:settings",color:"green"}""")},{text:"  "},${toggle("Flip this for everyone")}]""")
			raw("""${D}execute if data storage ra:settings cur{scope:"user"} if score @s $D(obj) matches 1.. run tellraw @s [${label()},${bracket("""{text:"on",color:"green"}""")},$YOU,{text:"  "},${toggle("Flip this for yourself only")}]""")
			raw("""${D}execute if data storage ra:settings cur{scope:"user"} unless score @s $D(obj) matches 1.. run tellraw @s [${label()},${bracket("""{text:"off",color:"red"}""")},$YOU,{text:"  "},${toggle("Flip this for yourself only")}]""")
		}

		// A number row, changed with buttons rather than typed. Modify goes through ra_lib:input,
		// the same form the Data Handler opens for a clock's delay, so a player types a number the
		// same way everywhere in the pack. Stepping is kept because it is one click and needs no form.
		pack.define(int) {
			raw("""${D}execute if data storage ra:settings cur{scope:"global"} run tellraw @s [${label()},${bracket("""{nbt:"global.$D(key)",storage:"ra:settings",color:"aqua"}""")},{text:"  "},$STEPPERS,{text:" "},$MODIFY]""")
			raw("""${D}execute if data storage ra:settings cur{scope:"user"} run tellraw @s [${label()},${bracket("""{score:{name:"@s",objective:"$D(obj)"},color:"aqua"}""")},$YOU,{text:"  "},$STEPPERS,{text:" "},$MODIFY]""")
		}

		// The default for a per-block property. Labelled "new" on purpose: the value is copied into
		// a block's own properties when it is PLACED, so changing it does not reach back and alter
		// machines already standing in the world -- see ra_settings:placement/seed.
		pack.define(prop) {
			raw("""${D}tellraw @s [${label()},${bracket("""{nbt:"global.props.\"$D(block)\".$D(prop)",storage:"ra:settings",color:"aqua"}""")},{text:" "},{text:"(new blocks)",color:"dark_gray"},{text:"  "},$STEPPERS]""")
		}

		// A text row. Typed, not stepped, so it has one button and the value arrives on a later tick
		// through ra_lib:input. The current value is resolved into cur.show before drawing, because
		// it lives under the player's UUID and no text component can path to that.
		pack.define(str) {
			function(Fns.userStr, S, "cur")
			sCopy("cur.show", "out")
			raw("""${D}tellraw @s [${label()},${bracket("""{nbt:"cur.show",storage:"ra:settings",color:"aqua"}""")},$YOU,{text:"  "},${button("Modify", "yellow", true, "Type a new value", "ie")}]""")
		}

		// A cycle through fixed choices. The stored value is the index, so the CURRENT choice has
		// to be looked up before it can be shown -- resolved into cur.show rather than pathed to
		// directly, because an nbt text component cannot take a dynamic list index.
		pack.define(list) {
			setTmp("#v", 0)
			raw("${D}execute if data storage ra:settings cur{scope:\"global\"} store result score #v ${Obj.SET_TMP} run data get storage ra:settings global.$D(key)")
			raw("${D}execute if data storage ra:settings cur{scope:\"user\"} store result score #v ${Obj.SET_TMP} run scoreboard players get @s $D(obj)")
			storeScore("q.v", tmp("#v"), Obj.SET_TMP)
			function(listShow, S, "q")
			raw("""${D}tellraw @s [${label()},${bracket("""{nbt:"cur.show",storage:"ra:settings",color:"aqua"}""")},{text:"  "},${button("Cycle", "yellow", true, "Step to the next choice", "i")}]""")
		}

		// `row/list_show {v}`: choice v of the current row into cur.show. Falls back to "?" when the
		// stored value is out of range, which happens when a module ships a shorter list than the
		// one a value was saved against. Better a visible mark than a blank bracket.
		pack.define(listShow) {
			data(S) { modify("cur.show") { set(net.benwoodworth.knbt.NbtString("?")) } }
			raw("${D}execute if data storage ra:settings cur.values[$D(v)] run data modify storage ra:settings cur.show set from storage ra:settings cur.values[$D(v)]")
		}

		// Whether a block type may be placed at all. Shown as ON/OFF text rather than the raw list
		// membership, because the stored form is "which blocks are disabled" and printing that
		// reads backwards.
		pack.define(block) {
			setTmp("#on", 1)
			raw("${D}execute if data storage ra:settings disabled[{b:\"$D(block)\"}] run scoreboard players set #on ${Obj.SET_TMP} 0")
			raw("""${D}execute if score #on ${Obj.SET_TMP} matches 1 run tellraw @s [${label()},${bracket("""{text:"enabled",color:"green"}""")},{text:"  "},${button("Disable", "red", true, "Stop this block being placed", "i")}]""")
			raw("""${D}execute if score #on ${Obj.SET_TMP} matches 0 run tellraw @s [${label()},${bracket("""{text:"disabled",color:"red"}""")},{text:"  "},${button("Enable", "green", true, "Allow this block again", "i")}]""")
		}
	}
}
