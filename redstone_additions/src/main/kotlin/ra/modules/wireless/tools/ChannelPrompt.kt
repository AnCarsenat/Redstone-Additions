package ra.modules.wireless.tools

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.executeStoreRun
import ra.core.raw
import ra.core.ref

/**
 * Sneak+RMB on a channel item (the Redstone Remote, the Pager): ask the player for a channel
 * through the shared book input, then stamp it on the item. One instance per item, each with its
 * own objectives, so one item's prompt never consumes the other's answer.
 *
 * This used to print a [Set Channel] button that suggested
 * `/function ra_wireless:tools/remote/set_channel {channel:"..."}`, which only works for a player
 * who can run commands. On a survival server without cheats the remote could never be retuned
 * from its default channel at all. It goes through the shared book input instead, the same way the
 * Data Handler asks for text, so it needs no permissions.
 *
 * The typed text reaches the item through the item modifier `ra_wireless:set_channel`, which copies
 * storage `ra_wireless:remote channel` into `custom_data.ra.channel` (named for the Remote, which
 * had it first; every channel item shares it). Substituting it into an `item replace ... with
 * <item>[...]` literal instead would let a book containing a quote character append arbitrary
 * components.
 *
 * @param dir function folder, `tools/<item>`.
 * @param label chat prefix, `[<label>] `, in [color].
 * @param flag the `custom_data.ra.<flag>:1b` that marks the item.
 * @param format when set, a channel outside this [TextFormat] is refused: the Pager pastes its
 *   channel, quoted, into an item predicate.
 */
internal class ChannelPrompt(
	private val dir: String,
	private val label: String,
	private val color: String,
	private val flag: String,
	/** Marks a player waiting on a book to name the item's channel. */
	val pending: String,
	/** Hotbar slot the item occupied when its channel prompt opened. */
	val slot: String,
	/** The ra_lib:input request this player opened FOR THIS ITEM. */
	val req: String,
	private val format: TextFormat? = null,
) {
	private fun fn(path: String) = ref(Ns.WIRELESS, "$dir/$path")

	val changeChannel = fn("change_channel")
	val applyPending = fn("apply_pending")
	private val applyToSlot = fn("apply_to_slot")

	private val state = "#${flag}_state"
	private val prefix = """{text:"[$label] ",color:"$color"}"""

	fun Ra.register() {
		// As the player, holding the item.
		pack.define(changeChannel) {
			raw("data modify storage ra:temp current_channel set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.channel")

			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Set Channel",color:"$color",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Current: ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]""")

			// Remember which hotbar slot the item is in. The player is holding it right now, which
			// is the only moment that is reliably true -- once the Input Form book is in hand,
			// SelectedItem is the book.
			raw("execute store result score @s $slot run data get entity @s SelectedItemSlot")

			// Drop any half-finished request before starting a new one.
			function(RaLib.Input.cancel)

			scoreboard.players.set(self(), pending, 1)
			function(RaLib.Input.openText)
			// Which request is ours: the settings menu and the Data Handler share the library.
			raw("scoreboard players operation @s $req = @s ra.input.req")

			raw("playsound minecraft:block.note_block.bell block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
		}

		// Write a finished book input onto the player's item. As a player with [pending] set.
		//
		// This must NOT require the item to still be the selected item. Writing in the Input Form
		// means selecting the book, so a guard on SelectedItem fires the moment the player picks the
		// book up -- and cancelling the session runs clear_book_req, which deletes the book out of
		// their hand. That was exactly the "book disappears when I switch to it" behaviour.
		//
		// The item's hotbar slot is recorded when the prompt opens instead, since the player is
		// necessarily holding it at that point. The session TTL still collects a request the player
		// abandons.
		pack.define(applyPending) {
			// A cancelled session (the player dropped the form) never becomes ready, so stop waiting
			// for it. `submit` clears ra.input.active but leaves state at 2, hence the second
			// condition -- without it a finished input would be discarded here on the very tick it
			// became available.
			raw("execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s $pending 0")
			raw("execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0")

			// Not our answer. Consuming another tool's session would hand it an empty result; and ours
			// was cancelled when that one opened, so it is never coming back -- stop waiting for it.
			raw("execute unless score @s $req = @s ra.input.req run scoreboard players set @s $pending 0")
			raw("execute unless score @s $req = @s ra.input.req run return 0")

			executeStoreRun("", state, RaLib.Input.poll)
			raw("execute unless score $state ${Obj.TEMP} matches 2 run return 0")

			executeStoreRun("", state, RaLib.Input.consume)
			raw("execute unless score $state ${Obj.TEMP} matches 1 run return 0")

			scoreboard.players.set(self(), pending, 0)

			// An empty book leaves the channel alone rather than blanking it.
			raw("""execute if data storage ra:input consume{text:""} run tellraw @s [$prefix,{text:"No channel entered; left unchanged.",color:"gray"}]""")
			raw("execute if data storage ra:input consume{text:\"\"} run return 0")

			if (format != null) {
				raw("data modify storage ra:safe s set from storage ra:input consume.text")
				raw("data modify storage ra:safe f set value ${format.code}")
				executeStoreRun("", state, RaLib.Safe.check)
				raw("""execute unless score $state ${Obj.TEMP} matches 1 run tellraw @s [$prefix,{text:"A channel may use ${format.hint}; left unchanged.",color:"gray"}]""")
				raw("execute unless score $state ${Obj.TEMP} matches 1 run return 0")
			}

			raw("data modify storage ra_wireless:remote channel set from storage ra:input consume.text")

			raw("execute store result storage ra_wireless:remote slot int 1 run scoreboard players get @s $slot")
			executeStoreRun("", state, applyToSlot, "with storage ra_wireless:remote")

			raw("""execute if score $state ${Obj.TEMP} matches 0 run tellraw @s [$prefix,{text:"$label moved out of its slot; channel not applied.",color:"gray"}]""")
			raw("execute if score $state ${Obj.TEMP} matches 0 run return 0")

			raw("data modify storage ra:temp current_channel set from storage ra_wireless:remote channel")
			raw("""tellraw @s [$prefix,{text:"Channel set to ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]""")
			raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.5")
		}

		// Macro {slot}: stamp the channel onto the item in hotbar slot N. Returns 1 when applied,
		// 0 when that slot no longer holds one.
		//
		// The slot is checked rather than trusted: the player had a whole book-writing session in
		// which to move the item, and stamping the wrong slot would rewrite whatever else ended up
		// there. Matched on the custom_data alone, so an item from an older world still takes it.
		pack.define(applyToSlot) {
			raw("\$execute unless data entity @s Inventory[{Slot:\$(slot)b,components:{\"minecraft:custom_data\":{ra:{$flag:1b}}}}] run return 0")

			raw("\$item modify entity @s hotbar.\$(slot) ra_wireless:set_channel")
			returnValue(1)
		}
	}
}
