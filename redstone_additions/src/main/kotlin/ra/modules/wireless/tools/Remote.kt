package ra.modules.wireless.tools

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.arguments.types.literals.self
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
import ra.modules.wireless.Receiver
import ra.core.block.shapedRecipe
import ra.core.block.recipeResult
import ra.core.block.Unlock
import ra.core.executeRun
import ra.core.block.markerSel
import ra.core.block.noticeOnly

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
	private val toggleReceivers = fn("toggle_receivers")
	private val checkReceiverChannel = fn("check_receiver_channel")

	/** Marks a player waiting on a book to name a remote's channel. */
	const val PENDING = "ra.remote.pending"

	/** Hotbar slot the remote occupied when its channel prompt opened. */
	const val SLOT = "ra.remote.slot"

	/** The ra_lib:input request this player opened FOR THE REMOTE, so it never consumes another tool's answer. */
	const val REQ = "ra.remote.req"

	private val prompt = ChannelPrompt("tools/remote", "Remote", "red", "remote", PENDING, SLOT, REQ)
	private val changeChannel = prompt.changeChannel
	val applyPending = prompt.applyPending

	private val buttons = listOf(
		Items.OAK_BUTTON, Items.SPRUCE_BUTTON, Items.BIRCH_BUTTON, Items.JUNGLE_BUTTON, Items.ACACIA_BUTTON,
		Items.DARK_OAK_BUTTON, Items.CHERRY_BUTTON, Items.PALE_OAK_BUTTON, Items.MANGROVE_BUTTON, Items.BAMBOO_BUTTON,
		Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.STONE_BUTTON, Items.POLISHED_BLACKSTONE_BUTTON,
	)

	/**
	 * A gray lore line. Not core `loreLine`: the beet item wrote `italic` before `color`, and the
	 * key order is part of the item's give/bundle text.
	 */
	private fun grayLoreLine(text: String) = comp("text" to SStr(text), "italic" to SBool(false), "color" to SStr("gray"))

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
		lore(grayLoreLine("RMB: Pulse receivers"), grayLoreLine("Shift+RMB: Set channel (book)"))
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
		pack.shapedRecipe(Ns.WIRELESS, "remote", recipeItem.recipeResult(Items.BLAZE_ROD), Unlock(Items.IRON_INGOT)) {
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
			executeRun("if predicate ra:is_sneaking", changeChannel)
			executeRun("unless predicate ra:is_sneaking", toggleReceivers)
		}

		// Pulse every receiver on the remote's channel (a string). As the player.
		pack.define(toggleReceivers) {
			// Channel from the held item, into storage.
			raw("data modify storage ra:temp remote_channel set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.channel")

			executeRun("as ${Receiver.block.markerSel()}", checkReceiverChannel)

			// Feedback.
			raw("""tellraw ${noticeOnly("@s")} [{text:"[Remote] ",color:"red"},{text:"Pulsed receivers on channel ",color:"gray"},{nbt:"remote_channel",storage:"ra:temp",color:"yellow"}]""")
			raw("playsound minecraft:block.lever.click block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 1")
		}

		// As a receiver: pulse it if its channel matches the remote's.
		pack.define(checkReceiverChannel) {
			// Copy the receiver's channel to temp for comparison.
			raw("data modify storage ra:temp check_channel set from entity @s data.properties.channel")

			// Try to overwrite it with the remote's channel: success 0 (nothing changed) means a match.
			raw("execute store success score @s ${Obj.TEMP} run data modify storage ra:temp check_channel set from storage ra:temp remote_channel")

			// Channel matches: pulse it.
			raw("execute if score @s ${Obj.TEMP} matches 0 run tag @s add ${Receiver.PULSING}")
			raw("execute if score @s ${Obj.TEMP} matches 0 run scoreboard players set @s ${Receiver.PULSE_TIMER} 4")
		}

		registerChannelInput()
	}

	/** Sneak+RMB: see [ChannelPrompt]. */
	private fun Ra.registerChannelInput() {
		with(prompt) { register() }

		// kept: beet contract. Macro {slot}: stamp the channel onto a remote sitting in hotbar
		// slot N, unchecked. Nothing in the pack calls it any more.
		pack.define(fn("apply_hotbar")) {
			raw("\$item modify entity @s hotbar.\$(slot) ra_wireless:set_channel")
		}
	}
}
