package ra.modules.sensors

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.api.Goggles
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.PropertiesBuilder
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.textList
import ra.modules.sensors.SensorsModule.SELECTOR_SETTING

/**
 * The Tag Adder's and Tag Remover's `tags` list, shared.
 *
 * `tags` is a list of entity tags (format TAG, at most [MAX]), edited per entry in the Data
 * Handler. It replaces the single `tag` of older blocks: a block without `tags` still runs on its
 * `tag` (the block's own fallback), and once `tags` is written, `tag` is ignored.
 *
 * Every entry is checked by `ra_lib:safe/check` (TAG: letters, digits, `_ . + -`, never `ra.`)
 * before it is pasted into a `tag` command. An entry that fails is skipped and counted in
 * `data.status.skipped`; an empty entry ("") is an unused row and skipped silently.
 *
 * Working storage: `ra:temp sensor_tags` {in, ok, w, c, t}, removed by [done].
 */
object SensorTags {
	const val MAX = 16

	/**
	 * As the marker: `data.properties.tags` -> `storage ra:temp sensor_tags.ok`, the entries that
	 * passed. Scores: #st.list 1 when `tags` is a list, #st.len its length, #st.n entries kept,
	 * #st.bad entries refused. Also writes or clears `data.status.skipped`.
	 */
	val load = ref(Ns.SENSORS, "tags/load")
	private val loadStep = ref(Ns.SENSORS, "tags/load_step")

	/** As an entity: remove / add every tag of `ra:temp sensor_tags.ok`. Add sets #st.any 1 on a change. */
	val removeEach = ref(Ns.SENSORS, "tags/remove_each")
	val addEach = ref(Ns.SENSORS, "tags/add_each")
	private val removeStep = ref(Ns.SENSORS, "tags/remove_step")
	private val addStep = ref(Ns.SENSORS, "tags/add_step")
	private val removeOne = ref(Ns.SENSORS, "tags/remove_one")
	private val addOne = ref(Ns.SENSORS, "tags/add_one")

	/** As the marker: the Goggles line for `tags`, `{empty}` the text for an empty list. */
	val gogglesLine = ref(Ns.SENSORS, "tags/goggles_line")
	private val gogglesStep = ref(Ns.SENSORS, "tags/goggles_step")

	/** As the marker of a block from before v6.0.2 (no `tags`): its single `tag`, `{empty}` when it has none. */
	val legacyLine = ref(Ns.SENSORS, "tags/legacy_line")

	/** Shown entries before "+N more", to keep the billboard narrow. */
	private const val SHOWN = 3

	private const val T = Obj.TEMP
	private const val S = "storage ra:temp sensor_tags"

	/** The `tags` declaration, the same on both blocks. */
	fun PropertiesBuilder.tagsList() = textList(
		"tags", max = MAX, format = TextFormat.TAG, entryLabel = "Tag ",
		adminOnly = true, adminSetting = SELECTOR_SETTING,
	)

	fun Function.done() = raw("data remove $S")

	fun Ra.registerSensorTags() {
		pack.define(load) {
			raw("data remove $S")
			for (s in listOf("list", "len", "n", "bad")) raw("scoreboard players set #st.$s $T 0")
			raw("data modify $S.in set from entity @s data.properties.tags")
			raw("execute unless data $S.in run return 0")
			// A list, of any kind: `insert` fails on anything else. The probe is taken out again.
			raw("execute store success score #st.list $T run data modify $S.in insert 0 value 0b")
			raw("execute if score #st.list $T matches 0 run return 0")
			raw("data remove $S.in[0]")
			raw("execute store result score #st.len $T run data get $S.in")
			raw("data modify $S.ok set value []")
			raw("data modify storage ra:safe f set value ${TextFormat.TAG.code}")
			raw("execute if data $S.in[0] run function ${loadStep.asId()}")
			raw("data remove storage ra:safe s")
			raw("execute if score #st.bad $T matches 1.. store result entity @s data.status.skipped int 1 run scoreboard players get #st.bad $T")
			raw("execute if score #st.bad $T matches 0 run data remove entity @s data.status.skipped")
		}

		// One entry off the front of `in`: "" skipped, a safe tag kept, anything else counted.
		pack.define(loadStep) {
			raw("data remove $S.c")
			raw("data modify $S.c set from $S.in[0]")
			raw("data remove $S.in[0]")
			raw("""execute if data $S{c:""} run return run execute if data $S.in[0] run function ${loadStep.asId()}""")
			raw("data remove storage ra:safe s")
			raw("data modify storage ra:safe s set from $S.c")
			raw("execute store result score #st.ok $T run function ${RaLib.Safe.check.asId()}")
			raw("execute if score #st.ok $T matches 1 run data modify $S.ok append from $S.c")
			raw("execute if score #st.ok $T matches 1 run scoreboard players add #st.n $T 1")
			raw("execute if score #st.ok $T matches 0 run scoreboard players add #st.bad $T 1")
			raw("execute if data $S.in[0] run function ${loadStep.asId()}")
		}

		walker(removeEach, removeStep, removeOne)
		walker(addEach, addStep, addOne)
		// Macro {t}: only ever an entry of `ok`, which ra_lib:safe/check passed as one TAG word.
		pack.define(removeOne) { raw("\$tag @s remove \$(t)") }
		pack.define(addOne) {
			raw("\$execute store success score #st.s $T run tag @s add \$(t)")
			raw("execute if score #st.s $T matches 1 run scoreboard players set #st.any $T 1")
		}

		// {empty}. Each entry is copied in with `set string`: nothing is pasted, so an odd entry
		// shows as written (or not at all) and cannot break the line.
		pack.define(gogglesLine) {
			// Respect the schema's `goggles` flag, as prop_line does.
			raw("execute unless data storage ra:temp gg{loaded:1b} run function ${Goggles.Billboard.loadSchema.asId()}")
			raw("execute if data storage ra:temp gg.schema.tags.goggles unless data storage ra:temp gg.schema.tags{goggles:1b} run return 0")
			raw("""${'$'}data modify storage ra:temp gg.line set value {text:"Tags: ",color:"gray",extra:[{text:"$(empty)",color:"yellow"}]}""")
			raw("data remove $S")
			raw("data modify $S.w set from entity @s data.properties.tags")
			// Not a list (written with a command): the block does nothing with it, so say so.
			raw("execute store success score #st.l $T run data modify $S.w insert 0 value 0b")
			raw("execute if score #st.l $T matches 1 run data remove $S.w[0]")
			raw("""execute if score #st.l $T matches 0 run data modify storage ra:temp gg.line.extra[0] set value {text:"not a list",color:"red"}""")
			raw("scoreboard players set #st.g $T 0")
			raw("execute if data $S.w[0] run data remove storage ra:temp gg.line.extra[0]")
			raw("execute if data $S.w[0] run function ${gogglesStep.asId()}")
			// "+N more" past the first $SHOWN.
			raw("scoreboard players operation #st.more $T = #st.g $T")
			raw("scoreboard players remove #st.more $T $SHOWN")
			raw("execute if score #st.more $T matches 1.. store result $S.more int 1 run scoreboard players get #st.more $T")
			raw("""execute if score #st.more $T matches 1.. run data modify storage ra:temp gg.line.extra append value {text:" +",color:"gray"}""")
			raw("""execute if score #st.more $T matches 1.. run data modify storage ra:temp gg.line.extra append value {text:"?",color:"gray"}""")
			raw("execute if score #st.more $T matches 1.. run data modify storage ra:temp gg.line.extra[-1].text set string $S.more")
			raw("""execute if score #st.more $T matches 1.. run data modify storage ra:temp gg.line.extra append value {text:" more",color:"gray"}""")
			raw("data remove $S")
			function(Goggles.Billboard.append)
		}
		// {empty}. Not gated by `tag`'s schema flag (hidden): on such a block it is what fires.
		pack.define(legacyLine) {
			raw("""${'$'}data modify storage ra:temp gg.line set value {text:"Tag: ",color:"gray",extra:[{text:"$(empty)",color:"yellow"}]}""")
			raw("scoreboard players set #st.l $T 0")
			raw("""execute unless data entity @s data.properties{tag:""} store success score #st.l $T run data modify storage ra:temp gg.line.extra[0].text set string entity @s data.properties.tag""")
			raw("""execute if score #st.l $T matches 1 run data modify storage ra:temp gg.line.extra[0].color set value "aqua"""" + "")
			function(Goggles.Billboard.append)
		}
		pack.define(gogglesStep) {
			raw("scoreboard players add #st.g $T 1")
			raw("execute if score #st.g $T matches 2..$SHOWN run data modify storage ra:temp gg.line.extra append value {text:\", \",color:\"gray\"}")
			raw("execute if score #st.g $T matches ..$SHOWN run data modify storage ra:temp gg.line.extra append value {text:\"?\",color:\"aqua\"}")
			raw("execute if score #st.g $T matches ..$SHOWN run data modify storage ra:temp gg.line.extra[-1].text set string $S.w[0]")
			raw("data remove $S.w[0]")
			raw("execute if data $S.w[0] run function ${gogglesStep.asId()}")
		}
	}

	private fun Ra.walker(each: FunctionArgument, step: FunctionArgument, one: FunctionArgument) {
		pack.define(each) {
			raw("data modify $S.w set from $S.ok")
			raw("execute if data $S.w[0] run function ${step.asId()}")
		}
		pack.define(step) {
			raw("data modify $S.t set from $S.w[0]")
			raw("function ${one.asId()} with $S")
			raw("data remove $S.w[0]")
			raw("execute if data $S.w[0] run function ${step.asId()}")
		}
	}
}
