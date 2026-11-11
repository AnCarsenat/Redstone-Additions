package ra.modules.railworks.carts

import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import io.github.ayfri.kore.features.tags.entityTypeTag
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.particleViewersSel
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.item.SBool
import ra.core.item.SByte
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.modules.railworks.RouteRailScan
import ra.modules.railworks.carts.CartRay.cartRay

/**
 * The Ram Upgrade: fitted to a minecart, the cart hurts the living things in its way while it goes
 * fast.
 *
 * FITTING. Sneak and use the upgrade on a minecart, or use it while riding a plain minecart. Sneaking is what
 * keeps the click from seating the player; a chest or hopper minecart opens its inventory instead,
 * whatever the player does, so only carts without an inventory take it. The cart gets the tag
 * [TAG] and the item is used up only once that is certain. Shift+RMB with the Wrench takes it off
 * and gives it back; breaking the cart drops it ([CartTracker]).
 *
 * RAMMING. Each tick, for upgraded carts only (from [CartTracker.cartTick]): the horizontal speed is
 * read from `Motion`; under [MIN_SPEED] nothing else runs. Above it, one 1x1x1 box ahead of the
 * cart in its direction of travel is searched for living entities (they have `HurtTime`; items,
 * frames and other carts do not), leaving out `#ra_railworks:ram_ignored` (vehicles, armor stands,
 * frames, paintings, displays...), the cart's own riders and any player riding a minecart. Each is hurt by [BANDS] (more the faster the cart), with the cart as the
 * direct source and its rider, if any, as the attacker (a player rider gets the kill). A victim is
 * hit only when its `HurtTime` is 0: once hurt, it is safe for the 10 ticks of its hurt animation,
 * so a cart that keeps pushing a mob does not hit it every tick.
 */
internal object RamUpgrade {
	const val TAG = "ra.rw.ram"
	private const val USER = "ra.rw.user"
	private const val RAMMING = "ra.rw.ramming"
	private const val RIDER = "ra.rw.ram_rider"

	/** The game time the player last used the upgrade (the advancement fires every tick it is held). */
	const val USED = "ra.rw.used"

	/** Below this horizontal speed (thousandths of a block per tick; 150 = 3 blocks a second) a cart does not ram. */
	const val MIN_SPEED = 150

	/** Speed band (thousandths of a block per tick) -> damage. 400 is the vanilla top speed. */
	val BANDS = listOf(150..249 to 3, 250..349 to 5, 350..599 to 7, 600..999 to 10, 1000..Int.MAX_VALUE to 14)

	private const val T = Obj.TEMP

	private fun fn(path: String) = ref(Ns.RAILWORKS, "carts/ram/$path")

	val give = fn("give")
	val tick = fn("tick")
	private val onUse = fn("on_use")
	private val apply = fn("apply")
	private val remove = fn("remove")
	private val hit = fn("hit")
	/** As a player holding the Wrench, sneaking: take the upgrade off the cart they look at. 1 when it did. */
	val wrenchHook = fn("wrench")

	private fun grayLine(text: String) = loreLine(text, "gray")

	val item = itemSpec("minecraft:firework_star") {
		model("minecraft:anvil")
		name("Ram Upgrade")
		add("max_stack_size", SInt(16))
		customData(raData("ram_upgrade" to SByte(true)))
		lore(
			grayLine("Sneak + RMB a minecart, or RMB while riding"),
			grayLine("The cart hurts what it runs into"),
			loreLine("Wrench Shift+RMB takes it off", "dark_gray"),
		)
		add("food", comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)))
		add("consumable", comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)))
	}

	private const val IGNORED = "#ra_railworks:ram_ignored"

	private const val HELD = "*[minecraft:custom_data~{ra:{ram_upgrade:1b}}]"
	private const val USER_SEL = "@a[tag=$USER,limit=1]"

	fun Ra.registerRamUpgrade() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Ram Upgrade",color:"yellow"}]""")
		}
		pack.shapedRecipe(Ns.RAILWORKS, "ram_upgrade", item.recipeResult(Items.FIREWORK_STAR), Unlock(Items.MINECART), RecipeCategory.MISC) {
			pattern(" I ", "IBI", " P ")
			key("I", Items.IRON_INGOT)
			key("B", Items.IRON_BLOCK)
			key("P", Items.PISTON)
		}

		val useAdvancement = pack.advancement("ram_upgrade_use") {
			namespace = Ns.RAILWORKS.id
			criteria {
				usingItem("requirement") {
					this.item = itemStackPredicate(Items.FIREWORK_STAR) {
						predicates { customData { this["ra"] = nbt { this["ram_upgrade"] = true } } }
					}
				}
			}
			rewards { function = onUse }
		}

		val ray = cartRay("ram_apply", apply)
		val rayRemove = cartRay("ram_remove", remove)

		// As the player. Only the first tick of a press fits the upgrade.
		pack.define(onUse) {
			raw("advancement revoke @s only ${useAdvancement.asId()}")
			raw("execute store result score #rw.now $T run time query gametime")
			raw("scoreboard players operation #rw.prev $T = @s $USED")
			raw("scoreboard players operation @s $USED = #rw.now $T")
			raw("scoreboard players remove #rw.now $T 1")
			raw("execute if score #rw.prev $T >= #rw.now $T run return 0")
			raw("tag @s add $USER")
			raw("scoreboard players set #rw.done $T 0")
			raw("execute on vehicle if entity @s[type=minecraft:minecart] at @s store result score #rw.done $T run function ${apply.asId()}")
			raw("execute if score #rw.done $T matches 0 store result score #rw.done $T run function ${ray.asId()}")
			raw("""execute if score #rw.done $T matches 0 run title @s actionbar [{text:"Sneak and use it on a minecart, or use it while riding one",color:"red"}]""")
			raw("tag @s remove $USER")
		}

		// As the cart, at it, with the player tagged USER. Returns 1: the click was about this cart.
		pack.define(apply) {
			raw("""execute if entity @s[tag=$TAG] run title $USER_SEL actionbar [{text:"That cart already has a Ram Upgrade",color:"red"}]""")
			raw("execute if entity @s[tag=$TAG] run return 1")
			raw("""execute if entity @s[tag=${RailBuilder.TAG}] run title $USER_SEL actionbar [{text:"A Rail Builder cannot take a Ram Upgrade",color:"red"}]""")
			raw("execute if entity @s[tag=${RailBuilder.TAG}] run return 1")
			raw("scoreboard players set #rw.ok $T 0")
			raw("execute store result score #rw.ok $T run clear $USER_SEL $HELD 1")
			raw("execute if score #rw.ok $T matches 0 run return 1")
			raw("tag @s add $TAG")
			raw("execute at @s run function ${CartTracker.attach.asId()}")
			raw("""title $USER_SEL actionbar [{text:"Ram Upgrade fitted",color:"green"},{text:" - the cart now hurts what it runs into",color:"gray"}]""")
			raw("execute at @s run particle minecraft:crit ~ ~0.5 ~ 0.4 0.2 0.4 0.2 20 normal ${particleViewersSel()}")
			raw("execute at @s run playsound minecraft:block.anvil.use block ${soundListenersSel()} ~ ~ ~ 0.6 1.4")
			raw("return 1")
		}

		pack.define(wrenchHook) {
			raw("tag @s add $USER")
			raw("execute store result score #rw.done $T run function ${rayRemove.asId()}")
			raw("tag @s remove $USER")
			raw("return run scoreboard players get #rw.done $T")
		}

		// As the cart, with the player tagged USER. 0 when it has no upgrade (the Wrench goes on to blocks).
		pack.define(remove) {
			raw("execute unless entity @s[tag=$TAG] run return 0")
			raw("tag @s remove $TAG")
			raw("execute at @s run function ${CartTracker.detach.asId()}")
			// The item comes back only once the cart no longer carries the upgrade.
			raw("execute if entity @s[tag=$TAG] run return 1")
			raw("give $USER_SEL ${item.giveArg()}")
			raw("""title $USER_SEL actionbar [{text:"Ram Upgrade removed",color:"yellow"}]""")
			raw("execute at @s run playsound minecraft:block.grindstone.use block ${soundListenersSel()} ~ ~ ~ 0.6 1.2")
			raw("return 1")
		}

		registerRamTick()
	}

	/** Each tick, as an upgraded cart, at it. */
	private fun Ra.registerRamTick() {
		// Never hurt, even if they could be: vehicles, decorations, displays and other things that
		// are not mobs. Living things are also told apart by having a `HurtTime`.
		pack.entityTypeTag("ram_ignored", Ns.RAILWORKS.id) {
			add(RouteRailScan.CARTS_TAG, Ns.RAILWORKS.id, true)
			add("boat", "minecraft", true)
			for (e in listOf(
				"oak_chest_boat", "spruce_chest_boat", "birch_chest_boat", "jungle_chest_boat", "acacia_chest_boat",
				"cherry_chest_boat", "dark_oak_chest_boat", "pale_oak_chest_boat", "mangrove_chest_boat",
				"poplar_chest_boat", "bamboo_chest_raft", "armor_stand", "item_frame", "glow_item_frame", "painting",
				"block_display", "item_display", "text_display", "interaction", "marker", "item", "experience_orb",
				"leash_knot", "end_crystal", "falling_block", "tnt",
			)) add("minecraft:$e")
		}
		pack.define(tick) {
			raw("execute store result score #ram.vx $T run data get entity @s Motion[0] 1000")
			raw("execute store result score #ram.vz $T run data get entity @s Motion[2] 1000")
			raw("scoreboard players operation #ram.ax $T = #ram.vx $T")
			raw("scoreboard players operation #ram.az $T = #ram.vz $T")
			raw("scoreboard players set #ram.neg $T -1")
			raw("execute if score #ram.ax $T matches ..-1 run scoreboard players operation #ram.ax $T *= #ram.neg $T")
			raw("execute if score #ram.az $T matches ..-1 run scoreboard players operation #ram.az $T *= #ram.neg $T")
			raw("scoreboard players operation #ram.sp $T = #ram.ax $T")
			raw("scoreboard players operation #ram.sp $T > #ram.az $T")
			raw("execute if score #ram.sp $T matches ..${MIN_SPEED - 1} run return 0")

			raw("tag @s add $RAMMING")
			raw("execute on passengers run tag @s add $RIDER")
			// Who gets the blame: 2 a player rider, 1 another rider, 0 nobody.
			raw("scoreboard players set #ram.by $T 0")
			raw("execute on passengers run scoreboard players set #ram.by $T 1")
			raw("execute on passengers if entity @s[type=minecraft:player] run scoreboard players set #ram.by $T 2")

			val victims = "@e[dx=0,dy=0,dz=0,type=!$IGNORED,tag=!$RIDER]"
			val ahead = listOf(
				"if score #ram.ax $T >= #ram.az $T if score #ram.vx $T matches 1.." to "~0.4 ~ ~-0.5",
				"if score #ram.ax $T >= #ram.az $T if score #ram.vx $T matches ..0" to "~-1.4 ~ ~-0.5",
				"if score #ram.ax $T < #ram.az $T if score #ram.vz $T matches 1.." to "~-0.5 ~ ~0.4",
				"if score #ram.ax $T < #ram.az $T if score #ram.vz $T matches ..0" to "~-0.5 ~ ~-1.4",
			)
			for ((cond, box) in ahead) raw("execute $cond positioned $box as $victims if data entity @s {HurtTime:0s} run function ${hit.asId()}")

			raw("tag @s remove $RAMMING")
			raw("execute on passengers run tag @s remove $RIDER")
		}

		// As a living entity in the box. #ram.sp: the cart's speed; #ram.by: who rides it.
		pack.define(hit) {
			raw("execute if entity @s[type=minecraft:player] on vehicle if entity @s[type=${CartTracker.CARTS}] run return 0")
			raw("execute if entity @s[type=minecraft:player,gamemode=spectator] run return 0")
			val cart = "@n[type=${CartTracker.CARTS},tag=$RAMMING]"
			raw("scoreboard players set #ram.hit $T 0")
			for ((band, dmg) in BANDS) {
				val range = if (band.last == Int.MAX_VALUE) "${band.first}.." else "${band.first}..${band.last}"
				raw("execute if score #ram.sp $T matches $range if score #ram.by $T matches 2 store success score #ram.hit $T run damage @s $dmg minecraft:player_attack by $cart from @p[tag=$RIDER]")
				raw("execute if score #ram.sp $T matches $range if score #ram.by $T matches 1 store success score #ram.hit $T run damage @s $dmg minecraft:mob_attack by $cart from @n[tag=$RIDER]")
				raw("execute if score #ram.sp $T matches $range if score #ram.by $T matches 0 store success score #ram.hit $T run damage @s $dmg minecraft:mob_attack by $cart")
			}
			raw("execute if score #ram.hit $T matches 1 at @s run playsound minecraft:entity.player.attack.knockback neutral ${soundListenersSel()} ~ ~ ~ 0.8 0.8")
			raw("execute if score #ram.hit $T matches 1 at @s run particle minecraft:crit ~ ~0.6 ~ 0.3 0.3 0.3 0.3 10 normal ${particleViewersSel()}")
		}
	}
}
