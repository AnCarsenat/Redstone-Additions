package ra.modules.wireless.tools

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.itemmodifiers.functions.copyCustomData
import io.github.ayfri.kore.features.itemmodifiers.functions.replace
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.item.SBool
import ra.core.item.SByte
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.raData
import ra.modules.sensors.exec
import ra.modules.sensors.shapedRecipe
import ra.modules.wireless.Receiver

/**
 * Redstone Remote: RMB pulses every receiver on the remote's channel, Shift+RMB asks for a new
 * channel through the shared book input.
 *
 * A blaze rod with food + consumable components, for reliable click detection: the `using_item`
 * advancement fires while it is being "eaten", which takes a million seconds.
 */
object Remote {
	private fun fn(path: String) = ref(Ns.WIRELESS, "tools/remote/$path")

	val give = fn("give")
	val onUse = fn("on_use")
	private val changeChannel = fn("change_channel")
	private val toggleReceivers = fn("toggle_receivers")
	private val checkReceiverChannel = fn("check_receiver_channel")
	val applyPending = fn("apply_pending")
	private val applyToSlot = fn("apply_to_slot")

	/** Marks a player waiting on a book to name a remote's channel. */
	const val PENDING = "ra.remote.pending"

	/** Hotbar slot the remote occupied when its channel prompt opened. */
	const val SLOT = "ra.remote.slot"

	private val buttons = listOf(
		Items.OAK_BUTTON, Items.SPRUCE_BUTTON, Items.BIRCH_BUTTON, Items.JUNGLE_BUTTON, Items.ACACIA_BUTTON,
		Items.DARK_OAK_BUTTON, Items.CHERRY_BUTTON, Items.PALE_OAK_BUTTON, Items.MANGROVE_BUTTON, Items.BAMBOO_BUTTON,
		Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.STONE_BUTTON, Items.POLISHED_BLACKSTONE_BUTTON,
	)

	private fun loreLine(text: String) = comp("text" to SStr(text), "italic" to SBool(false), "color" to SStr("gray"))

	/**
	 * The item. The lore deliberately does not name the current channel. Lore is baked into the
	 * item and cannot read the item's own data, so it could only be kept in step by rebuilding the
	 * whole item every time the channel changed -- and rebuilding it means writing the player's
	 * typed text into a command literal. The channel is reported in chat instead: on every pulse,
	 * and when the set-channel prompt opens.
	 */
	val item = itemSpec("minecraft:blaze_rod") {
		model("minecraft:red_dye")
		name("Redstone Remote")
		add("max_stack_size", SInt(1))
		customData(raData("remote" to SByte(true), "channel" to SStr("default")))
		lore(loreLine("RMB: Pulse receivers"), loreLine("Shift+RMB: Set channel (book)"))
		// `true` in the give command, `1b` in the bundle: the same byte.
		add("enchantment_glint_override", SBool(true), SByte(true))
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	fun Ra.registerRemote() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Redstone Remote",color:"red"}]""")
		}

		// The recipe's custom data has `remote:1` (an int), not `1b`, exactly as the beet JSON did.
		val recipeItem = item.replace("custom_data", raData("remote" to SInt(1), "channel" to SStr("default")))
		pack.shapedRecipe(Ns.WIRELESS, "remote", recipeItem, unlockedBy = Items.IRON_INGOT, resultItem = Items.BLAZE_ROD) {
			pattern(" T ", "IBI", " I ")
			key("T", Items.REDSTONE_TORCH)
			key("I", Items.IRON_INGOT)
			key("B", buttons)
		}

		val useAdvancement = pack.advancement("tools/remote_use") {
			namespace = Ns.WIRELESS.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.BLAZE_ROD) {
						predicates { customData { this["ra"] = nbt { this["remote"] = true } } }
					}
				}
			}
			rewards { function = onUse }
		}

		// The player's channel text goes into the item through this modifier, reading from storage.
		pack.itemModifier("set_channel") {
			namespace = Ns.WIRELESS.id
			copyCustomData("ra_wireless:remote") { replace("channel", "ra.channel") }
		}

		// As the player, from the advancement.
		pack.define(onUse) {
			// Revoke the advancement immediately.
			raw("advancement revoke @s only ${useAdvancement.asId()}")

			// Mark that the player clicked this tick.
			tag(self()) { add("ra.remote_clicked") }

			// Already holding (has the tag): skip.
			raw("execute if entity @s[tag=ra.remote_active] run return fail")

			// Mark the player as actively using this tool.
			tag(self()) { add("ra.remote_active") }

			// Sneaking: change channel. Otherwise: pulse.
			exec("if predicate ra:is_sneaking", changeChannel)
			exec("unless predicate ra:is_sneaking", toggleReceivers)
		}

		// Pulse every receiver on the remote's channel (a string). As the player.
		pack.define(toggleReceivers) {
			// Channel from the held item, into storage.
			raw("data modify storage ra:temp remote_channel set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.channel")

			exec("as @e[type=marker,tag=${Receiver.block.tag}]", checkReceiverChannel)

			// Feedback.
			raw("""tellraw @s [{text:"[Remote] ",color:"red"},{text:"Pulsed receivers on channel ",color:"gray"},{nbt:"remote_channel",storage:"ra:temp",color:"yellow"}]""")
			raw("playsound minecraft:block.lever.click block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 1")
		}

		// As a receiver: pulse it if its channel matches the remote's.
		pack.define(checkReceiverChannel) {
			// Copy the receiver's channel to temp for comparison.
			raw("data modify storage ra:temp check_channel set from entity @s data.properties.channel")

			// Try to overwrite it with the remote's channel: success 0 (nothing changed) means a match.
			raw("execute store success score @s ${Obj.TEMP} run data modify storage ra:temp check_channel set from storage ra:temp remote_channel")

			// Channel matches and the receiver is enabled: pulse it.
			raw("execute if score @s ${Obj.TEMP} matches 0 if data entity @s data.properties{enabled:1b} run tag @s add ${Receiver.PULSING}")
			raw("execute if score @s ${Obj.TEMP} matches 0 if data entity @s data.properties{enabled:1b} run scoreboard players set @s ${Receiver.PULSE_TIMER} 4")
		}

		registerChannelInput()
	}

	/**
	 * Shift+RMB: ask the player for a channel name, then stamp it on the remote.
	 *
	 * This used to print a [Set Channel] button that suggested
	 * `/function ra_wireless:tools/remote/set_channel {channel:"..."}`, which only works for a
	 * player who can run commands. On a survival server without cheats the remote could never be
	 * retuned from its default channel at all. It now goes through the shared book input, the same
	 * way the Data Handler asks for text, so it needs no permissions.
	 */
	private fun Ra.registerChannelInput() {
		pack.define(changeChannel) {
			raw("data modify storage ra:temp current_channel set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.channel")

			raw("tellraw @s \"\"")
			raw("""tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Set Channel",color:"red",bold:true},{text:" ══════",color:"dark_gray"}]""")
			raw("""tellraw @s [{text:"Current: ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]""")

			// Remember which hotbar slot the remote is in. The player is holding it right now, which
			// is the only moment that is reliably true -- once the Input Form book is in hand,
			// SelectedItem is the book.
			raw("execute store result score @s $SLOT run data get entity @s SelectedItemSlot")

			// Drop any half-finished request before starting a new one.
			function(RaLib.Input.cancel)

			scoreboard.players.set(self(), PENDING, 1)
			function(RaLib.Input.openText)

			raw("playsound minecraft:block.note_block.bell block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.2")
		}

		// Write a finished book input onto the player's remote. As a player with ra.remote.pending set.
		//
		// This must NOT require the remote to still be the selected item. Writing in the Input Form
		// means selecting the book, so a guard on SelectedItem fires the moment the player picks the
		// book up -- and cancelling the session runs clear_book_req, which deletes the book out of
		// their hand. That was exactly the "book disappears when I switch to it" behaviour.
		//
		// The remote's hotbar slot is recorded when the prompt opens instead, since the player is
		// necessarily holding it at that point. The session TTL still collects a request the player
		// abandons.
		pack.define(applyPending) {
			// A cancelled session (the player dropped the form) never becomes ready, so stop waiting
			// for it. `submit` clears ra.input.active but leaves state at 2, hence the second
			// condition -- without it a finished input would be discarded here on the very tick it
			// became available.
			raw("execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run scoreboard players set @s $PENDING 0")
			raw("execute unless entity @s[tag=ra.input.active] unless score @s ra.input.state matches 2 run return 0")

			exec("store result score #remote_state ${Obj.TEMP}", RaLib.Input.poll)
			raw("execute unless score #remote_state ${Obj.TEMP} matches 2 run return 0")

			exec("store result score #remote_state ${Obj.TEMP}", RaLib.Input.consume)
			raw("execute unless score #remote_state ${Obj.TEMP} matches 1 run return 0")

			scoreboard.players.set(self(), PENDING, 0)

			// An empty book leaves the channel alone rather than blanking it.
			raw("""execute if data storage ra:input consume{text:""} run tellraw @s [{text:"[Remote] ",color:"red"},{text:"No channel entered; left unchanged.",color:"gray"}]""")
			raw("execute if data storage ra:input consume{text:\"\"} run return 0")

			// The player's text goes into the item via an item modifier reading from storage.
			// Substituting it into an `item replace ... with blaze_rod[...]` literal instead would let
			// a book containing a quote character append arbitrary components.
			raw("data modify storage ra_wireless:remote channel set from storage ra:input consume.text")

			raw("execute store result storage ra_wireless:remote slot int 1 run scoreboard players get @s $SLOT")
			exec("store result score #remote_state ${Obj.TEMP}", applyToSlot, "with storage ra_wireless:remote")

			raw("""execute if score #remote_state ${Obj.TEMP} matches 0 run tellraw @s [{text:"[Remote] ",color:"red"},{text:"Remote moved out of its slot; channel not applied.",color:"gray"}]""")
			raw("execute if score #remote_state ${Obj.TEMP} matches 0 run return 0")

			raw("data modify storage ra:temp current_channel set from storage ra_wireless:remote channel")
			raw("""tellraw @s [{text:"[Remote] ",color:"red"},{text:"Channel set to ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]""")
			raw("playsound minecraft:block.note_block.pling block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.5")
		}

		// Macro {slot}: stamp the channel onto the remote in hotbar slot N. Returns 1 when applied,
		// 0 when that slot no longer holds a remote.
		//
		// The slot is checked rather than trusted: the player had a whole book-writing session in
		// which to move the item, and stamping the wrong slot would rewrite whatever else ended up
		// there.
		pack.define(applyToSlot) {
			// Matched on the custom_data alone. Adding max_stack_size here would narrow the test to
			// remotes made since they became unstackable, and one from an older world would silently
			// fail to take a channel.
			raw("\$execute unless data entity @s Inventory[{Slot:\$(slot)b,components:{\"minecraft:custom_data\":{ra:{remote:1b}}}}] run return 0")

			raw("\$item modify entity @s hotbar.\$(slot) ra_wireless:set_channel")
			returnValue(1)
		}

		// Macro {slot}: stamp the channel onto a remote sitting in hotbar slot N, unchecked.
		pack.define(fn("apply_hotbar")) {
			raw("\$item modify entity @s hotbar.\$(slot) ra_wireless:set_channel")
		}
	}
}
