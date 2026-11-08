package ra.settings.gen

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntEnd
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.put
import ra.core.Ns
import ra.core.Obj
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.Fns
import ra.settings.S
import ra.settings.gen.Text.esc
import ra.settings.gen.Text.gpath
import ra.settings.gen.Text.ppath
import ra.settings.gen.Text.report
import ra.settings.gen.Text.slug
import ra.settings.pages.BlockRow
import ra.settings.pages.BoolRow
import ra.settings.pages.IntRow
import ra.settings.pages.ListRow
import ra.settings.pages.PropRow
import ra.settings.pages.StrRow
import ra.settings.setTmp
import ra.settings.tmp

/**
 * The operator function tree, `ra_settings:admin/<page>/<label>/<action>`: one real function per
 * setting per action.
 *
 * WHY THIS IS GENERATED AND NOT HAND-WRITTEN. Operator settings are reached with /function so
 * that they autocomplete: typing `/function ra_settings:admin/` and pressing tab walks the whole
 * tree. Autocomplete is the only discovery mechanism a command has, and it only works on real
 * function paths -- a macro argument like {key:"...",value:5} completes to nothing. That means
 * one function per setting per action, far too many to maintain by hand, so they come out of
 * the page data. The generator knows every key, bound and step as a literal, so the emitted
 * commands need no macros.
 */
internal object AdminRows {
	/** `ra_settings:admin/<page>/<slug(label)>/<action>`. */
	fun action(pageId: String, label: String, action: String) =
		ref(Ns.SETTINGS, "admin/$pageId/${slug(label)}/$action")

	fun DataPack.blockEnable(pageId: String, row: BlockRow): FunctionArgument = define(action(pageId, row.label, "enable")) {
		data(S) { remove("disabled[{b:\"${row.block}\"}]") }
		raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"${row.label}",color:"white"},{text:" enabled.",color:"green"}]""")
	}

	/** Stop it being placed. Blocks already in the world keep working -- deleting somebody's build because a box was ticked is not a setting. */
	fun DataPack.blockDisable(pageId: String, row: BlockRow) = define(action(pageId, row.label, "disable")) {
		execute {
			unlessCondition { data(S, "disabled[{b:\"${row.block}\"}]") }
			run { data(S) { modify("disabled") { append(buildNbtCompound { put("b", row.block) }) } } }
		}
		raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"${row.label}",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]""")
	}

	fun DataPack.prop(pageId: String, row: PropRow) {
		val path = ppath(row.block, row.prop)
		// Stepping applies to blocks placed from now on.
		for ((name, delta) in listOf("up" to row.step, "down" to -row.step)) {
			define(action(pageId, row.label, name)) {
				step(path, row.default, delta, row.min, row.max)
				raw(report(row.label, path, Text.NEW_BLOCKS_ONLY))
			}
		}
		define(action(pageId, row.label, "reset")) {
			data(S) { modify(path) { set(row.default) } }
			raw(report(row.label, path, Text.NEW_BLOCKS_ONLY))
		}

		// Push the configured value onto blocks that already exist, overwriting any per-block value
		// set with the wrench.
		//
		// The normal rule is that a default applies to blocks placed from now on, so that retuning
		// does not silently re-balance a build around the player. That is right as a default and
		// wrong as the only option -- an admin who has just changed a number usually wants their
		// existing machines to follow it. This is that, asked for explicitly, and it says how many
		// it changed. Addressed by the marker tag rather than data.type: the tag is what every
		// block carries and what every other sweep in the pack selects on.
		define(action(pageId, row.label, "apply_to_existing")) {
			val placed = allEntities { type = EntityTypes.MARKER; tag = Tag.customBlock(row.block) }
			setTmp("#n", 0)
			execute {
				storeResult { score(tmp("#n"), Obj.SET_TMP) }
				ifCondition { entity(placed) }
			}
			execute {
				asTarget(placed)
				run { data(self()) { modify("data.properties.${row.prop}") { set(S, path) } } }
			}
			raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"${Obj.SET_TMP}"},color:"aqua"},{text:" existing ${row.block}: ${row.prop} = ",color:"gray"},{nbt:"${esc(path)}",storage:"ra:settings",color:"aqua"}]""")
		}

		// Stepping alone is not enough for a wide range: moving a generator from 60 to 500 EU
		// would be forty-four clicks.
		define(action(pageId, row.label, "edit")) {
			edit(buildNbtCompound {
				put("kind", "prop"); put("block", row.block); put("prop", row.prop)
				put("type", "int"); put("min", row.min); put("max", row.max)
			})
		}
	}

	fun DataPack.bool(pageId: String, row: BoolRow) {
		val path = gpath(row.key)
		for ((name, value) in listOf("on" to 1, "off" to 0)) {
			define(action(pageId, row.label, name)) {
				data(S) { modify(path) { set(NbtByte(value.toByte())) } }
				raw(report(row.label, path))
			}
		}
	}

	fun DataPack.int(pageId: String, row: IntRow) {
		val path = gpath(row.key)
		for ((name, delta) in listOf("up" to row.step, "down" to -row.step)) {
			define(action(pageId, row.label, name)) {
				step(path, row.default, delta, row.min, row.max)
				raw(report(row.label, path))
			}
		}
		define(action(pageId, row.label, "reset")) {
			data(S) { modify(path) { set(row.default) } }
			raw(report(row.label, path))
		}
		// Stepping is one click; typing is for when the target is far from where the value sits.
		define(action(pageId, row.label, "edit")) {
			edit(buildNbtCompound {
				put("kind", "global"); put("key", row.key); put("type", "int"); put("min", row.min); put("max", row.max)
			})
		}
	}

	/** Text has nothing to step, so there is no up/down -- only typing it, through the same input form the Data Handler opens. */
	fun DataPack.str(pageId: String, row: StrRow) {
		val path = gpath(row.key)
		define(action(pageId, row.label, "edit")) {
			edit(buildNbtCompound { put("kind", "global"); put("key", row.key); put("type", "str") })
		}
		define(action(pageId, row.label, "reset")) {
			data(S) { modify(path) { set(NbtString(row.default)) } }
			raw(report(row.label, path))
		}
	}

	/**
	 * One function per choice, so the choices themselves autocomplete. The stored value stays the
	 * INDEX -- renaming a choice then moves the label without orphaning what anybody had selected.
	 */
	fun DataPack.list(pageId: String, row: ListRow) {
		val path = gpath(row.key)
		row.values.forEachIndexed { i, choice ->
			define(action(pageId, row.label, slug(choice))) {
				data(S) { modify(path) { set(i) } }
				raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"${row.label}",color:"white"},{text:" is now ",color:"gray"},{text:"$choice",color:"aqua"}]""")
			}
		}
	}

	/** Read the value (or its default) into #n, step it, clamp it and store it back. */
	private fun Function.step(path: String, default: Int, delta: Int, lo: Int, hi: Int) {
		val n = tmp("#n")
		setTmp("#n", default)
		execute {
			ifCondition { data(S, path) }
			storeResult { score(n, Obj.SET_TMP) }
			run { data(S) { get(path) } }
		}
		if (delta > 0) scoreboard.players.add(n, Obj.SET_TMP, delta) else scoreboard.players.remove(n, Obj.SET_TMP, -delta)
		execute {
			ifCondition { score(n, Obj.SET_TMP) matches rangeOrIntEnd(lo - 1) }
			run { setTmp("#n", lo) }
		}
		execute {
			ifCondition { score(n, Obj.SET_TMP) matches rangeOrIntStart(hi + 1) }
			run { setTmp("#n", hi) }
		}
		execute {
			storeResult { storage(S, path, io.github.ayfri.kore.arguments.enums.DataType.INT, 1.0) }
			run { scoreboard.players.get(n, Obj.SET_TMP) }
		}
	}

	/**
	 * Describe the edit in storage ra:settings edit and ask for a typed value.
	 *
	 * The value is only applied to a player holding ra.admin (see ra_settings:apply_edit), so this
	 * grants it first, as ra_settings:admin/show does. Reaching this function at all takes
	 * permission level 2 or the tag already, so an operator who typed the /function path directly
	 * gets what opening the panel would have given them, and nobody else gets anything.
	 */
	private fun Function.edit(payload: net.benwoodworth.knbt.NbtCompound) {
		function(Fns.adminGrant)
		data(S) { modify("edit") { set(payload) } }
		function(Fns.editStart)
	}
}
