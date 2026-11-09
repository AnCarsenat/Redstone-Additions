package ra.modules.ender.tablet

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.itemmodifiers.functions.copyCustomData
import io.github.ayfri.kore.features.itemmodifiers.functions.replace
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.api.Settings
import ra.core.Ns
import ra.core.TextFormat
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom
import ra.core.Obj
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.item.SBool
import ra.core.item.SByte
import ra.core.item.SInt
import ra.core.item.SList
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.modules.ender.chooser.Chooser
import ra.modules.ender.chooser.Chooser.say

/**
 * Anchor Tablet: right-click to open the destination menu ([Chooser]) from anywhere.
 *
 * The item keeps its own `favs` (up to eight anchor ids) and `last` (the last trip) in its
 * custom_data, under `ra`. They are written only from storage through the `ra_ender:tablet_data`
 * item modifier, never pasted into a command, and every id is cleaned to format ID again
 * ([Chooser.go]) before it is used, so an edited item cannot reach anything but a table key.
 *
 * A TRIP COSTS. [COST_KEY] ender pearls (default 1, none in creative) and a [COOLDOWN_KEY]-second
 * recharge (default 10), both server settings on the Ender page. The recharge is a game time on
 * the player ([READY]), compared when used: nothing counts down each tick. Vanilla's
 * `use_cooldown` component would need the use to finish, and the tablet's use never does (the
 * endless `consumable` that makes right-click visible, as on the Redstone Remote).
 */
object AnchorTablet {
	private fun fn(path: String) = ref(Ns.ENDER, "tablet/$path")

	val give = fn("give")
	private val onUse = fn("on_use")
	/** As a player: the tablet in the main hand into `storage ra:ender menu` {favs, last}. Returns 1, or 0 without one. */
	val loadHeld = fn("load_held")
	/** As a player: the same from the hotbar slot in [SLOT]. */
	val loadSlot = fn("load_slot")
	private val loadAt = fn("load_at")
	/** As a player: 1 when the trip can be paid for (cooldown over, enough pearls), else 0 with a message. */
	val canPay = fn("can_pay")
	/** As a player, after a trip: take the pearls, start the cooldown, remember `storage ra:ender trip` as the last trip. */
	val charge = fn("charge")
	private val takePearls = fn("take_pearls")
	/** As a player: `storage ra:ender menu.last` into the favourites. */
	val save = fn("save")
	private val saveCheck = fn("save_check")
	/** As a player, #ender.act 21..28: forget that favourite. */
	val forget = fn("forget")
	/** As a player: the menu's cost line. */
	val showCost = fn("show_cost")
	private val write = fn("write")
	private val writeAt = fn("write_at")

	/** The hotbar slot the tablet was in when its menu was last used. */
	const val SLOT = "ra.ender.slot"
	/** The game time the player's tablet is ready again. */
	const val READY = "ra.ender.ready"
	/** The game time the tablet was last seen in use (the advancement fires every tick it is held down). */
	const val USED = "ra.ender.used"
	val objectives = listOf(SLOT, READY, USED)

	const val COST_KEY = "ender_tablet_pearls"
	const val COOLDOWN_KEY = "ender_tablet_cooldown"
	const val DEFAULT_COST = 1
	const val DEFAULT_COOLDOWN = 10

	private const val T = Obj.TEMP
	private const val HELD = """*[minecraft:custom_data~{ra:{anchor_tablet:1b}}]"""
	private const val STORE = "ra_ender:tablet"

	private fun grayLine(text: String) = loreLine(text, "gray")

	val item = itemSpec("minecraft:blaze_rod") {
		model("minecraft:recovery_compass")
		name("Anchor Tablet")
		rarity("epic")
		add("max_stack_size", SInt(1))
		customData(raData("anchor_tablet" to SByte(true), "last" to SStr(""), "favs" to SList(emptyList())))
		lore(grayLine("RMB: choose a destination"), loreLine("Each trip costs ender pearls", "dark_gray"))
		add("enchantment_glint_override", SBool(true), SByte(true))
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	private fun cost(into: String) = "execute store result score $into $T run function ${Settings.get.asId()} {key:\"$COST_KEY\",default:$DEFAULT_COST}"
	private fun cooldown(into: String) = "execute store result score $into $T run function ${Settings.get.asId()} {key:\"$COOLDOWN_KEY\",default:$DEFAULT_COOLDOWN}"

	fun Ra.registerAnchorTablet() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Anchor Tablet",color:"light_purple"},{text:" — right-click it to choose where to go",color:"gray"}]""")
		}
		pack.shapedRecipe(Ns.ENDER, "anchor_tablet", item.recipeResult(Items.BLAZE_ROD), Unlock(Items.ENDER_PEARL, "has_pearl"), RecipeCategory.MISC) {
			pattern(" E ", "GCG", " G ")
			key("E", Items.ENDER_EYE)
			key("G", Items.GOLD_INGOT)
			key("C", Items.COMPASS)
		}

		val useAdvancement = pack.advancement("tablet/use") {
			namespace = Ns.ENDER.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.BLAZE_ROD) {
						predicates { customData { this["ra"] = nbt { this["anchor_tablet"] = true } } }
					}
				}
			}
			rewards { function = onUse }
		}

		// storage ra_ender:tablet {favs, last} onto the item's custom_data.
		pack.itemModifier("tablet_data") {
			namespace = Ns.ENDER.id
			copyCustomData(STORE) {
				replace("favs", "ra.favs")
				replace("last", "ra.last")
			}
		}

		// As the player, from the advancement, which fires every tick the button is held: only the
		// first tick of a press opens the menu.
		pack.define(onUse) {
			raw("advancement revoke @s only ${useAdvancement.asId()}")
			raw("execute store result score #ender.now $T run time query gametime")
			raw("scoreboard players operation #ender.prev $T = @s $USED")
			raw("scoreboard players operation @s $USED = #ender.now $T")
			raw("scoreboard players remove #ender.now $T 1")
			raw("execute if score #ender.prev $T >= #ender.now $T run return 0")
			raw("data modify storage ra:ender menu set value {}")
			raw("execute store result score #ender.ok $T run function ${loadHeld.asId()}")
			raw("execute if score #ender.ok $T matches 0 run return 0")
			raw("function ${Chooser.open.asId()}")
			raw("playsound minecraft:item.lodestone_compass.lock player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.6 1.3")
		}

		pack.define(loadHeld) {
			raw("execute unless items entity @s weapon.mainhand $HELD run return 0")
			raw("execute store result score @s $SLOT run data get entity @s SelectedItemSlot")
			raw("scoreboard players set @s ${Chooser.SRC} 2")
			raw("return run function ${loadSlot.asId()}")
		}
		pack.define(loadSlot) {
			raw("data modify storage ra:ender menu set value {}")
			raw("execute store result storage ra:ender menu.slot int 1 run scoreboard players get @s $SLOT")
			raw("return run function ${loadAt.asId()} with storage ra:ender menu")
		}
		// {slot}: an int from a score.
		pack.define(loadAt) {
			raw("\$execute unless items entity @s hotbar.\$(slot) $HELD run return 0")
			raw("\$data modify storage ra:ender menu.favs set from entity @s Inventory[{Slot:\$(slot)b}].components.\"minecraft:custom_data\".ra.favs")
			raw("\$data modify storage ra:ender menu.last set from entity @s Inventory[{Slot:\$(slot)b}].components.\"minecraft:custom_data\".ra.last")
			raw("return 1")
		}

		registerPayment()
		registerFavourites()
	}

	private fun Ra.registerPayment() {
		pack.define(canPay) {
			raw("execute store result score #ender.now $T run time query gametime")
			raw("scoreboard players operation #ender.wait $T = @s $READY")
			raw("scoreboard players operation #ender.wait $T -= #ender.now $T")
			raw("scoreboard players add #ender.wait $T 19")
			raw("scoreboard players set #ender.20 $T 20")
			raw("scoreboard players operation #ender.wait $T /= #ender.20 $T")
			raw("""execute if score @s $READY > #ender.now $T run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"The Anchor Tablet is recharging: ",color:"red"},{score:{name:"#ender.wait",objective:"$T"},color:"yellow"},{text:" s.",color:"red"}]""")

			raw(cost("#ender.cost"))
			raw("execute if entity @s[gamemode=creative] run scoreboard players set #ender.cost $T 0")
			raw("execute store result score #ender.have $T run clear @s minecraft:ender_pearl 0")
			raw("""execute if score #ender.have $T < #ender.cost $T run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"A trip costs ",color:"red"},{score:{name:"#ender.cost",objective:"$T"},color:"yellow"},{text:" ender pearl(s).",color:"red"}]""")
			raw("return 1")
		}

		pack.define(charge) {
			raw(cost("#ender.cost"))
			raw("execute if entity @s[gamemode=creative] run scoreboard players set #ender.cost $T 0")
			raw("execute store result storage ra:ender pay.n int 1 run scoreboard players get #ender.cost $T")
			raw("execute if score #ender.cost $T matches 1.. run function ${takePearls.asId()} with storage ra:ender pay")
			raw("data remove storage ra:ender pay")

			raw(cooldown("#ender.cd"))
			raw("scoreboard players set #ender.20 $T 20")
			raw("scoreboard players operation #ender.cd $T *= #ender.20 $T")
			raw("execute store result score @s $READY run time query gametime")
			raw("scoreboard players operation @s $READY += #ender.cd $T")

			// The trip is the last one; menu.favs is the item's own, from load_held/load_slot.
			raw("data modify storage ra:ender menu.last set from storage ra:ender trip")
			raw("function ${write.asId()}")
		}
		// {n}: an int from a setting.
		pack.define(takePearls) { raw("\$clear @s minecraft:ender_pearl \$(n)") }

		pack.define(showCost) {
			raw(cost("#ender.cost"))
			raw(cooldown("#ender.cd"))
			raw("""tellraw @s [{text:"  A trip: ",color:"dark_gray"},{score:{name:"#ender.cost",objective:"$T"},color:"gray"},{text:" ender pearl(s), then ",color:"dark_gray"},{score:{name:"#ender.cd",objective:"$T"},color:"gray"},{text:" s to recharge.",color:"dark_gray"}]""")
		}
	}

	private fun Ra.registerFavourites() {
		pack.define(save) {
			raw("""execute unless data storage ra:ender menu.last run return run ${say("No trip to save yet.", "red")}""")
			raw("""execute if data storage ra:ender menu{last:""} run return run ${say("No trip to save yet.", "red")}""")
			// In format ID before it goes near the macro below: the item's data is the player's.
			cleanFrom("storage ra:ender menu.last", TextFormat.ID)
			raw("""execute if score ${SafeClean.OK} $T matches 0 run return run ${say("No trip to save yet.", "red")}""")
			raw("data modify storage ra:ender menu.last set from storage ra:safe out")
			raw("""execute if data storage ra:ender menu.favs[${Chooser.MAX_FAVS - 1}] run return run ${say("The tablet holds ${Chooser.MAX_FAVS} favourites: forget one first.", "red")}""")
			// Already there? last is in format ID now: no quote, safe inside the macro's string.
			raw("execute store result score #ender.dup $T run function ${saveCheck.asId()} with storage ra:ender menu")
			raw("""execute if score #ender.dup $T matches 1 run return run ${say("Already a favourite.")}""")
			raw("execute unless data storage ra:ender menu.favs run data modify storage ra:ender menu.favs set value []")
			raw("data modify storage ra:ender menu.favs append from storage ra:ender menu.last")
			raw("function ${write.asId()}")
			raw("function ${Chooser.open.asId()}")
		}
		// {last}
		pack.define(saveCheck) {
			raw("\$execute if data storage ra:ender menu{favs:[\"\$(last)\"]} run return 1")
			raw("return 0")
		}

		pack.define(forget) {
			for (i in 0 until Chooser.MAX_FAVS) {
				raw("execute if score #ender.act $T matches ${21 + i} run data remove storage ra:ender menu.favs[$i]")
			}
			raw("function ${write.asId()}")
			raw("function ${Chooser.open.asId()}")
		}

		// menu {favs, last} onto the tablet in hotbar slot [SLOT], if it is still there.
		pack.define(write) {
			raw("data remove storage $STORE favs")
			raw("data remove storage $STORE last")
			raw("data modify storage $STORE favs set from storage ra:ender menu.favs")
			raw("data modify storage $STORE last set from storage ra:ender menu.last")
			raw("execute store result storage $STORE slot int 1 run scoreboard players get @s $SLOT")
			raw("function ${writeAt.asId()} with storage $STORE")
		}
		// {slot}
		pack.define(writeAt) {
			raw("\$execute unless items entity @s hotbar.\$(slot) $HELD run return 0")
			raw("\$item modify entity @s hotbar.\$(slot) ra_ender:tablet_data")
		}
	}
}
