package ra.settings

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.Operation
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.core.ref
import ra.settings.SettingsObjectives.ADMIN_TAG
import ra.settings.SettingsObjectives.APAGE
import ra.settings.SettingsObjectives.INPUT_ACTIVE_TAG
import ra.settings.SettingsObjectives.INPUT_BACKEND
import ra.settings.SettingsObjectives.INPUT_MAX
import ra.settings.SettingsObjectives.INPUT_MIN
import ra.settings.SettingsObjectives.INPUT_MODE
import ra.settings.SettingsObjectives.INPUT_REQ
import ra.settings.SettingsObjectives.INPUT_STATE
import ra.settings.SettingsObjectives.PEND
import ra.settings.SettingsObjectives.REQ
import ra.settings.apply.ApplyFns

/**
 * ONE PAYLOAD PER PLAYER. `storage ra:settings edit` is only a working copy, filled and emptied
 * within one function call. What waits between the click and the answer is `edits.r<id>`, keyed
 * by the request this player opened for settings (ra.settings.req). It used to wait in `edit`
 * itself, one slot for the whole server: a second player starting an edit overwrote the first
 * one's, and a player's typed number could land in an operator's pending server setting.
 */
private object EditSlot {
	private fun s(path: String) = ref(Ns.SETTINGS, path)

	/** `edit` -> `edits.r<id>`. */
	val save = s("edit/save")
	val saveAt = s("edit/save_at")

	/** `edits.r<id>` -> `edit`, or no `edit` when there is none. */
	val load = s("edit/load")
	val loadAt = s("edit/load_at")

	/** Remove `edits.r<id>`. */
	val forget = s("edit/forget")
	val forgetAt = s("edit/forget_at")

	/** Stop waiting: no pending flag, no stored payload, no working copy. */
	val drop = s("edit/drop")

	/** A server setting's value arrived from a player who no longer has server-settings access. */
	val refuse = s("edit/refuse")
}

/** Typed values: asking for one (`edit_start`) and receiving it (`apply_pending`, `apply_edit`). */
internal fun Ra.registerEdit() {
	pack.define(Fns.editStart) { editStart() }
	pack.define(Fns.applyPending) { applyPending() }
	pack.define(Fns.applyEdit) { applyEdit() }

	// Each `*_at` is a `{r}` macro called with storage ra:settings eq, which withReq always fills
	// with r, so every variable they name is always present.
	pack.define(EditSlot.save) { withReq(EditSlot.saveAt) }
	pack.define(EditSlot.saveAt) { raw("\$data modify storage ra:settings edits.r\$(r) set from storage ra:settings edit") }
	pack.define(EditSlot.load) {
		sRemove("edit")
		withReq(EditSlot.loadAt)
	}
	pack.define(EditSlot.loadAt) { raw("\$data modify storage ra:settings edit set from storage ra:settings edits.r\$(r)") }
	pack.define(EditSlot.forget) { withReq(EditSlot.forgetAt) }
	pack.define(EditSlot.forgetAt) { raw("\$data remove storage ra:settings edits.r\$(r)") }

	pack.define(EditSlot.drop) {
		scoreboard.players.set(self(), PEND, 0)
		function(EditSlot.forget)
		sRemove("edit")
	}

	pack.define(EditSlot.refuse) {
		raw("""tellraw @s [{text:"[Settings] ",color:"gold"},{text:"You no longer have server-settings access; the value was not applied.",color:"red"}]""")
		sRemove("edit")
	}
}

/** `eq = {r: <ra.settings.req>}` (0 when unset), then `function <fn> with storage ra:settings eq`. */
private fun Function.withReq(fn: FunctionArgument) {
	raw("data modify storage ra:settings eq set value {r:0}")
	raw("execute if score @s $REQ matches -2147483648.. store result storage ra:settings eq.r int 1 run scoreboard players get @s $REQ")
	function(fn, S, "eq")
	sRemove("eq")
}

/**
 * Ask the player to type a value for the edit described in storage ra:settings edit:
 *   {kind:"global"|"prop"|"user", type:"str"|"int", key/block+prop/obj as the kind requires, min, max}
 *
 * MODELLED ON ra:tools/data_handler/generic_action, DELIBERATELY. The Data Handler's text input
 * works, and every difference between this and that was a difference this had to justify:
 *   - The pending flag is set BEFORE the session opens, not after. Waiting until after meant the
 *     flag depended on a backend having been chosen, which is one more thing that can be false
 *     for reasons unrelated to whether we asked.
 *   - What is being edited lives in STORAGE, as one compound per player, the way ra:dh keeps its
 *     pending edits. It used to be a scoreboard index into the menu row list, which is only
 *     meaningful while that list is unchanged.
 *   - The caller fills in storage ra:settings edit and calls this. Nothing here knows whether the
 *     target is a global key, a block property or a player's own preference. This moves it into
 *     the player's own slot once the request id is known.
 */
private fun Function.editStart() {
	// Whatever this player was asked before is over: its session is cancelled just below.
	function(EditSlot.forget)
	scoreboard.players.set(self(), PEND, 1)

	function(RaLib.Input.cancel)

	scoreboard.players.set(self(), INPUT_MODE, 2)
	scoreboard.players.set(self(), INPUT_MIN, 0)
	scoreboard.players.set(self(), INPUT_MAX, 120)

	val isInt = "edit{type:\"int\"}"
	execute { ifCondition { data(S, isInt) }; run { scoreboard.players.set(self(), INPUT_MODE, 3) } }
	execute { ifCondition { data(S, isInt) }; storeResult { score(self(), INPUT_MIN) }; run { data(S) { get("edit.min") } } }
	execute { ifCondition { data(S, isInt) }; storeResult { score(self(), INPUT_MAX) }; run { data(S) { get("edit.max") } } }

	function(RaLib.Input.sessionCreate)
	function(RaLib.Input.routerSelectBackend)
	function(RaLib.Input.routerOpen)

	// Which request is ours. ra_lib:input is shared with the Data Handler, and consume() tears a
	// session down as it reads, so whoever consumes must first check the answer was meant for them.
	scoreboard.players.operation(self(), REQ, Operation.SET, self(), INPUT_REQ)

	// Park the payload under that request, off the shared working slot.
	function(EditSlot.save)
	sRemove("edit")

	raw("""execute if entity @s[tag=${Tag.DEBUG}] run tellraw @s [{text:"[set/dbg] ",color:"dark_purple"},{text:"asked",color:"gray"},{text:"  mode ",color:"dark_gray"},{score:{name:"@s",objective:"$INPUT_MODE"},color:"dark_gray"},{text:" backend ",color:"dark_gray"},{score:{name:"@s",objective:"$INPUT_BACKEND"},color:"dark_gray"},{text:" req ",color:"dark_gray"},{score:{name:"@s",objective:"$INPUT_REQ"},color:"dark_gray"}]""")
}

/**
 * Write a typed value into whatever this player's `edits.r<id>` describes.
 *
 * The guard order is the Data Handler's, for the same reasons its comments give: a cancelled
 * session never becomes ready, so waiting on one forever is wrong; and submit clears
 * ra.input.active while leaving state at 2, so testing only the tag would throw away a finished
 * answer on the tick it arrived. Every stop that ends the wait goes through edit/drop, so the
 * stored payload never outlives it.
 */
private fun Function.applyPending() {
	// The working copy, from this player's own slot.
	function(EditSlot.load)

	val noEdit = "execute unless data storage ra:settings edit"
	debug("$noEdit if entity @s[tag=${Tag.DEBUG}]", "stop: no edit payload")
	executeReturnRun(noEdit.removePrefix("execute "), EditSlot.drop)

	val gone = "execute unless entity @s[tag=$INPUT_ACTIVE_TAG] unless score @s $INPUT_STATE matches 2"
	debug("$gone if entity @s[tag=${Tag.DEBUG}]", "stop: session gone")
	executeReturnRun(gone.removePrefix("execute "), EditSlot.drop)

	// Not our answer. The Data Handler and the Remote share this library, and consuming their
	// session would hand them an empty result and leave them waiting for one already taken. Ours
	// was cancelled when theirs opened, so it is never coming back: stop waiting for it.
	val notMine = "execute unless score @s $REQ = @s $INPUT_REQ"
	debug("$notMine if entity @s[tag=${Tag.DEBUG}]", "stop: not my request")
	executeReturnRun(notMine.removePrefix("execute "), EditSlot.drop)

	val ok = tmp("#ok")
	execute { storeResult { score(ok, Obj.SET_TMP) }; run { function(RaLib.Input.poll) } }
	debug("execute unless score #ok ${Obj.SET_TMP} matches 2 if entity @s[tag=${Tag.DEBUG}]", "waiting: not ready")
	raw("execute unless score #ok ${Obj.SET_TMP} matches 2 run data remove storage ra:settings edit")
	execute { unlessCondition { score(ok, Obj.SET_TMP) equalTo 2 }; run { returnValue(0) } }

	execute { storeResult { score(ok, Obj.SET_TMP) }; run { function(RaLib.Input.consume) } }
	debug("execute unless score #ok ${Obj.SET_TMP} matches 1 if entity @s[tag=${Tag.DEBUG}]", "stop: consume refused")
	raw("execute unless score #ok ${Obj.SET_TMP} matches 1 run data remove storage ra:settings edit")
	execute { unlessCondition { score(ok, Obj.SET_TMP) equalTo 1 }; run { returnValue(0) } }

	// The working copy is all apply_edit needs from here; the stored slot goes now.
	scoreboard.players.set(self(), PEND, 0)
	function(EditSlot.forget)
	raw("""execute if entity @s[tag=${Tag.DEBUG}] run tellraw @s [{text:"[set/dbg] ",color:"dark_purple"},{text:"consumed, applying",color:"gray"}]""")
	function(Fns.applyEdit)
}

/** A `[set/dbg]` line with the session's state, request and live request appended. */
private fun Function.debug(head: String, message: String) {
	val score = { obj: String -> """{score:{name:"@s",objective:"$obj"},color:"dark_gray"}""" }
	raw("""$head run tellraw @s [{text:"[set/dbg] ",color:"dark_purple"},{text:"$message",color:"gray"},{text:"  state ",color:"dark_gray"},${score(INPUT_STATE)},{text:" mine ",color:"dark_gray"},${score(REQ)},{text:" live ",color:"dark_gray"},${score(INPUT_REQ)}]""")
}

/**
 * Route a consumed value to whatever it was for. One entry point per kind, chosen from the payload
 * rather than from where the request came in -- that is what lets an operator's page and a
 * player's menu share this whole path instead of each carrying a copy of it.
 */
private fun Function.applyEdit() {
	// SERVER SETTINGS ARE RE-CHECKED ON ARRIVAL. The request was made by somebody allowed to make
	// it, but the value comes in later, and access can be revoked in between.
	for (kind in listOf("global", "prop"))
		executeReturnRun("if data storage ra:settings edit{kind:\"$kind\"} unless entity @s[tag=$ADMIN_TAG]", EditSlot.refuse)

	for ((kind, fn) in listOf("global" to ApplyFns.global, "prop" to ApplyFns.prop, "user" to ApplyFns.user)) execute {
		ifCondition { data(S, "edit{kind:\"$kind\"}") }
		run { function(fn, S, "edit") }
	}

	raw("""execute if entity @s[tag=${Tag.DEBUG}] run tellraw @s [{text:"[set/dbg] ",color:"dark_purple"},{text:"applied; text was ",color:"gray"},{nbt:"consume.text",storage:"ra:input",color:"aqua"},{text:"  kind ",color:"dark_gray"},{nbt:"edit.kind",storage:"ra:settings",color:"dark_gray"}]""")

	sRemove("edit")

	execute {
		ifCondition { score(self(), APAGE) matches rangeOrIntStart(0) }
		run { function(Fns.adminRefresh) }
	}
	execute {
		unlessCondition { score(self(), APAGE) matches rangeOrIntStart(0) }
		run { function(Fns.pageOpen) }
	}
}
