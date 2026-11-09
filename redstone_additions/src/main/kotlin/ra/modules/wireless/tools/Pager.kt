package ra.modules.wireless.tools

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
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
import ra.core.TextFormat
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.executeRun
import ra.core.item.SBool
import ra.core.item.SByte
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.lib.safe.SafeText.guardProperty
import ra.modules.wireless.Channel

/**
 * Pager: alerts the player carrying it when its channel turns on -- anywhere in the inventory or
 * the offhand. RMB says which channel it listens on, Sneak+RMB sets it ([ChannelPrompt]).
 *
 * NOTHING POLLS. A channel turning on is already an event: [Channel.changed] calls [page] on the
 * tick the first emitter on a channel starts sending (nothing else on it was). [page] then asks
 * every player once, with an item predicate on the channel. The channel is pasted into that
 * predicate, inside quotes, so the emitter's channel must pass `ra_lib:safe/guard` as a NAME
 * first (letters, digits, spaces and `_ . + - : / #`: nothing that can leave a quoted string),
 * and the Pager refuses any other channel when it is set.
 *
 * The click detection is the Remote's: a blaze rod "eaten" for a million seconds.
 */
object Pager {
	private fun fn(path: String) = ref(Ns.WIRELESS, "tools/pager/$path")

	val give = fn("give")
	private val onUse = fn("on_use")
	private val status = fn("status")
	private val find = fn("find")
	private val alert = fn("alert")

	/** On every player to alert, between [find] and [alert]. */
	private const val PAGED = "ra.pager.paged"

	internal val prompt = ChannelPrompt(
		"tools/pager", "Pager", "light_purple", "pager",
		pending = "ra.pager.pending", slot = "ra.pager.slot", req = "ra.pager.req",
		format = TextFormat.NAME,
	)

	private fun grayLoreLine(text: String) = comp("text" to SStr(text), "italic" to SBool(false), "color" to SStr("gray"))

	val item = itemSpec("minecraft:blaze_rod") {
		model("minecraft:echo_shard")
		name("Pager")
		add("max_stack_size", SInt(1))
		customData(raData("pager" to SByte(true), "channel" to SStr("default")))
		lore(
			grayLoreLine("Alerts you when its channel turns on"),
			grayLoreLine("RMB: Show channel"),
			grayLoreLine("Shift+RMB: Set channel (book)"),
		)
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	fun Ra.registerPager() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Pager",color:"light_purple"}]""")
		}

		pack.shapedRecipe(Ns.WIRELESS, "pager", item.recipeResult(Items.BLAZE_ROD), Unlock(Items.NOTE_BLOCK)) {
			pattern(" A ", "INI", " R ")
			key("A", Items.AMETHYST_SHARD)
			key("I", Items.IRON_INGOT)
			key("N", Items.NOTE_BLOCK)
			key("R", Items.REDSTONE)
		}

		val useAdvancement = pack.advancement("tools/pager_use") {
			namespace = Ns.WIRELESS.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.BLAZE_ROD) {
						predicates { customData { this["ra"] = nbt { this["pager"] = true } } }
					}
				}
			}
			rewards { function = onUse }
		}

		// As the player, from the advancement, every tick the button is held.
		pack.define(onUse) {
			raw("advancement revoke @s only ${useAdvancement.asId()}")
			raw("tag @s add ra.pager_clicked")
			raw("execute if entity @s[tag=ra.pager_active] run return fail")
			raw("tag @s add ra.pager_active")
			executeRun("if predicate ra:is_sneaking", prompt.changeChannel)
			executeRun("unless predicate ra:is_sneaking", status)
		}

		pack.define(status) {
			raw("data modify storage ra:temp current_channel set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.channel")
			raw("""tellraw @s [{text:"[Pager] ",color:"light_purple"},{text:"Listening on channel ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]""")
			raw("playsound minecraft:block.lever.click block @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 1.4")
		}

		with(prompt) { register() }

		// As the emitter that turned its channel on (see Channel.changed). Its channel is pasted
		// into [find], so it must be a NAME first; anything else pages nobody.
		pack.define(Channel.page) {
			guardProperty("channel", TextFormat.NAME)
			raw("data modify storage ra:temp wl_page.channel set from entity @s data.properties.channel")
			raw("function ${find.asId()} with storage ra:temp wl_page")
			raw("execute as @a[tag=$PAGED] at @s run function ${alert.asId()}")
		}

		// Macro {channel}, guarded: who carries a Pager on it, in the 36 inventory slots or the
		// offhand. A partial custom_data match, so other keys on the item do not matter.
		pack.define(find) {
			val pager = """*[minecraft:custom_data~{ra:{pager:1b,channel:"$(channel)"}}]"""
			raw("\$execute as @a if items entity @s container.* $pager run tag @s add $PAGED")
			raw("\$execute as @a[tag=!$PAGED] if items entity @s weapon.offhand $pager run tag @s add $PAGED")
		}

		// As a paged player, at them. The title is the point of carrying one; the sound follows
		// the player's sound setting.
		pack.define(alert) {
			raw("tag @s remove $PAGED")
			raw("""title @s subtitle [{text:"Channel ",color:"gray"},{nbt:"wl_page.channel",storage:"ra:temp",color:"yellow"},{text:" is on",color:"gray"}]""")
			raw("""title @s title {text:"Pager",color:"light_purple"}""")
			raw("""tellraw @s [{text:"[Pager] ",color:"light_purple"},{text:"Channel ",color:"gray"},{nbt:"wl_page.channel",storage:"ra:temp",color:"yellow"},{text:" turned on.",color:"gray"}]""")
			raw("playsound minecraft:block.note_block.chime player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 1.5")
			raw("playsound minecraft:block.note_block.chime player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 2")
		}
	}
}
