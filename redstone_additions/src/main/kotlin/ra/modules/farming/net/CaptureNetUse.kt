package ra.modules.farming.net

import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.triggers.playerInteractedWithEntity
import io.github.ayfri.kore.features.advancements.triggers.usingItem
import io.github.ayfri.kore.features.predicates.sub.components
import io.github.ayfri.kore.features.predicates.sub.itemStackPredicate
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import net.benwoodworth.knbt.NbtString
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.lib.BlockTags
import ra.modules.farming.net.CaptureNet.FULL
import ra.modules.farming.net.CaptureNet.MASTER
import ra.modules.farming.net.CaptureNet.NET
import ra.modules.farming.net.CaptureNet.net
import ra.modules.farming.net.CaptureNet.STORE

/**
 * What right-clicking a [CaptureNet] does. Two advancements fire it, so nothing runs while nobody
 * uses one:
 * - `capture_net/use` (`using_item`): the net is an endless `consumable`, like the Pattern. A
 *   right-click on a mob that does nothing with the item (a cow, a zombie), on a block or on air.
 * - `capture_net/interact` (`player_interacted_with_entity`): a right-click a mob answered itself
 *   (a villager opening its trades, a tamed wolf sitting down), where no item use follows. Only an
 *   empty net acts on it.
 * Holding the button fires `using_item` every tick: only the first tick of a press counts ([USED]).
 * Only the main hand is read.
 *
 * CATCHING (empty net). A walk from the eyes, 0.1 block a step, finds the first mob of
 * [CaptureNetMobs.ANY] whose hitbox holds the point, stopping at the first block the crosshair
 * would stop on. It is refused (nothing changes) when it is not a [CaptureNetMobs.SMALL] mob and
 * the net is not a Master net, is tagged `ra`, is invulnerable, carries a rider, or is
 * tamed by another player. Otherwise, in this order:
 *   1. its data is copied into `storage ra:farming net.mob`, its type added ([CaptureNetMobs.typeOf]);
 *   2. that is copied into the held net, and the net is read back: it must now hold a mob;
 *   3. only then is the mob removed: silenced, its owner and loot table cleared, teleported far
 *      below the world and killed there, so whatever it drops falls out of the world.
 * A mob is never removed before the net holds it, and never kept once it does.
 *
 * LETTING IT OUT (full net). The same walk, for blocks only: the mob is summoned on the last free
 * point before the first solid block, then its stored data is merged into it, then the net is
 * emptied. The summon is checked: no mob, no emptying. The type id is pasted into that `summon`
 * only after `ra_lib:safe/check` (format ID) passed it. A mob that Peaceful would remove is not
 * let out on Peaceful.
 *
 * PLAYER TEXT. A mob's custom name is copied into the lore as stored data (an `nbt` text
 * component), never pasted into a command.
 */
internal object CaptureNetUse {
	private fun fn(path: String) = ref(Ns.FARMING, "capture_net/$path")

	private val onUse = fn("on_use")
	private val onInteract = fn("on_interact")
	private val press = fn("press")
	private val catch = fn("catch")
	private val rayMob = fn("ray_mob")
	private val take = fn("take")
	private val takeAsMob = fn("take_as_mob")
	private val fill = fn("fill")
	private val removeMob = fn("remove_mob")
	private val release = fn("release")
	private val rayBlock = fn("ray_block")
	private val releaseAt = fn("release_at")
	private val summonMob = fn("summon")
	private val mergeMob = fn("merge")
	private val vanish = fn("vanish")
	private val ownerCheck = fn("owner_check")

	/** The game time the player's net was last seen in use. */
	const val USED = "ra.farm.net_used"

	private const val T = Obj.TEMP
	private const val N = "storage $STORE net"
	private const val USER = "ra.cn.user"
	private const val HIT = "ra.cn.hit"
	private const val TARGET = "ra.cn.target"
	private const val NEW = "ra.cn.new"
	private const val TARGET_SEL = "@e[tag=$TARGET,distance=..8,limit=1]"
	private const val REACH_STEPS = 45

	/** Entity fields a released mob must not get back: identity, place and motion, and passing state. */
	private val STRIP = listOf(
		"UUID", "Pos", "Motion", "FallDistance", "fall_distance", "Fire", "OnGround", "PortalCooldown",
		"Air", "Brain", "leash", "Leash", "sleeping_pos", "SleepingX", "SleepingY", "SleepingZ",
		"HurtTime", "HurtByTimestamp", "DeathTime", "AngryAt", "angry_at", "AngerTime", "anger_end_time",
		// Never another entity, never a leash, never a flag a player could not set: whatever rides
		// it or holds it stays out of the net, and so do tags and invulnerability.
		"Passengers", "RootVehicle", "leash_holder", "Tags", "Invulnerable",
	)

	private val holdsLore = """{text:"",color:"gray",italic:false,extra:[{text:"Holds: "},{nbt:"net.mob.type",storage:"$STORE",interpret:true,color:"white"}]}"""
	private val nameLore = """{nbt:"net.mob.data.CustomName",storage:"$STORE",interpret:true,color:"aqua",italic:true}"""
	private val releaseLore = """{text:"Right-click a block to let it out",color:"dark_gray",italic:false}"""
	private val emptyLore = CaptureNet.emptyLore().joinToString(",", "[", "]") { it.text() }

	private fun setLore(lines: String) = """{type:"minecraft:set_lore",entity:"this",mode:"replace_all",lore:$lines}"""

	fun Ra.registerCaptureNetUse() {
		val netItem = itemStackPredicate(Items.RABBIT_HIDE) {
			components { this["item_model"] = CustomComponent(NbtString(CaptureNet.MODEL)) }
		}
		val useAdv = pack.advancement("capture_net/use") {
			namespace = Ns.FARMING.id
			criteria { usingItem("requirement") { item = netItem } }
			rewards { function = onUse }
		}
		val interactAdv = pack.advancement("capture_net/interact") {
			namespace = Ns.FARMING.id
			criteria { playerInteractedWithEntity("requirement") { item = netItem } }
			rewards { function = onInteract }
		}

		// As the player, from the advancements.
		pack.define(onUse) {
			firstTickOfPress(useAdv)
			raw("execute if items entity @s weapon.mainhand ${net(FULL)} run return run function ${release.asId()}")
			raw("function ${catch.asId()}")
		}
		pack.define(onInteract) {
			firstTickOfPress(interactAdv)
			raw("execute if items entity @s weapon.mainhand ${net(FULL)} run return 0")
			raw("function ${catch.asId()}")
		}
		// Returns 1 on the first tick of a press, 0 on the ticks the button stays down.
		pack.define(press) {
			raw("execute store result score #cn.now $T run time query gametime")
			raw("scoreboard players operation #cn.prev $T = @s $USED")
			raw("scoreboard players operation @s $USED = #cn.now $T")
			raw("scoreboard players remove #cn.now $T 1")
			raw("execute if score #cn.prev $T >= #cn.now $T run return 0")
			raw("return 1")
		}

		// As the player holding an empty net.
		pack.define(catch) {
			raw("tag @s add $USER")
			raw("scoreboard players set #cn.ray $T $REACH_STEPS")
			raw("execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${rayMob.asId()}")
			raw("execute as @e[tag=$HIT,distance=..8,limit=1,sort=nearest] run tag @s add $TARGET")
			raw("tag @e[tag=$HIT,distance=..8] remove $HIT")
			raw("execute unless entity $TARGET_SEL run ${say("Right-click a mob to catch it.", "gray")}")
			raw("execute if entity $TARGET_SEL run function ${take.asId()}")
			raw("tag @e[tag=$TARGET,distance=..8] remove $TARGET")
			raw("tag @s remove $USER")
		}

		// One point of the look, as the player: a catchable mob's hitbox holding the point ends it,
		// so does a block the crosshair stops on. The two boxes meet in a 0.02 cube at the point.
		pack.define(rayMob) {
			raw("execute positioned ~-0.99 ~-0.99 ~-0.99 as @e[type=${CaptureNetMobs.ANY_TAG},tag=!ra,dx=0,dy=0,dz=0] positioned ~0.98 ~0.98 ~0.98 if entity @s[dx=0,dy=0,dz=0] run tag @s add $HIT")
			raw("execute if entity @e[tag=$HIT,distance=..3,limit=1] run return 0")
			raw("execute unless block ~ ~ ~ ${BlockTags.rayPassable.asId()} align xyz unless entity @s[dx=0,dy=0,dz=0] run return 0")
			raw("scoreboard players remove #cn.ray $T 1")
			raw("execute if score #cn.ray $T matches 1.. positioned ^ ^ ^0.1 run function ${rayMob.asId()}")
		}

		// As the player, with the mob tagged TARGET: every refusal, then the catch.
		pack.define(take) {
			raw("execute unless items entity @s weapon.mainhand ${net(MASTER)} if entity @e[tag=$TARGET,distance=..8,limit=1,type=!${CaptureNetMobs.SMALL_TAG}] run return run ${say("This net only catches animals. A Master Capture Net catches any mob.", "yellow")}")
			raw("execute if entity @e[tag=$TARGET,distance=..8,limit=1,nbt={Invulnerable:1b}] run return run ${say("It cannot be caught.", "yellow")}")
			raw("scoreboard players set #cn.rider $T 0")
			raw("execute as $TARGET_SEL on passengers run scoreboard players set #cn.rider $T 1")
			raw("execute if score #cn.rider $T matches 1 run return run ${say("Something is riding it.", "yellow")}")
			// Tamed by someone else: its Owner differs from this player's UUID in any of the four ints.
			raw("scoreboard players set #cn.other $T 0")
			raw("execute if data entity $TARGET_SEL Owner run function ${ownerCheck.asId()}")
			raw("execute if score #cn.other $T matches 1 run return run ${say("It belongs to another player.", "red")}")
			raw("execute as $TARGET_SEL run function ${takeAsMob.asId()}")
		}

		// As the player, the target tamed: #cn.other 1 unless Owner[i] = UUID[i] for i 0..3.
		pack.define(ownerCheck) {
			for (i in 0..3) {
				raw("execute store result score #cn.o$i $T run data get entity $TARGET_SEL Owner[$i]")
				raw("execute store result score #cn.u$i $T run data get entity @s UUID[$i]")
				raw("execute unless score #cn.o$i $T = #cn.u$i $T run scoreboard players set #cn.other $T 1")
			}
		}

		// As the mob, the player tagged USER.
		pack.define(takeAsMob) {
			raw("tag @s remove $TARGET")
			raw("data remove $N.mob")
			raw("data modify $N.mob.data set from entity @s")
			for (k in STRIP) raw("data remove $N.mob.data.$k")
			raw("function ${CaptureNetMobs.typeOf.asId()}")
			raw("scoreboard players set #cn.ok $T 0")
			raw("execute as @a[tag=$USER,limit=1] run function ${fill.asId()}")
			raw("data remove $N.mob")
			// Only once the net holds it.
			raw("execute if score #cn.ok $T matches 1 run function ${removeMob.asId()}")
		}

		// As the player: the mob into the held net, then read back.
		pack.define(fill) {
			raw("execute unless data $N.mob.id run return run ${say("It cannot be caught.", "yellow")}")
			raw("item modify entity @s weapon.mainhand ${CaptureNet.FILL_MODIFIER}")
			raw("execute if data $N.mob.data.CustomName run item modify entity @s weapon.mainhand ${setLore("[$holdsLore,$nameLore,$releaseLore]")}")
			raw("execute unless data $N.mob.data.CustomName run item modify entity @s weapon.mainhand ${setLore("[$holdsLore,$releaseLore]")}")
			raw("""item modify entity @s weapon.mainhand {type:"minecraft:set_components",components:{"minecraft:enchantment_glint_override":true}}""")
			// Read back by path: the held net is a net and holds this mob's type and data.
			raw("execute store success score #cn.ok $T if items entity @s weapon.mainhand $NET if data entity @s SelectedItem.components.\"minecraft:custom_data\".ra.capture_net.mob.data if data entity @s SelectedItem.components.\"minecraft:custom_data\".ra.capture_net.mob.id")
			raw("execute if score #cn.ok $T matches 0 run return run ${say("The net could not hold it.", "red")}")
			raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"Caught: ",color:"green"},{nbt:"net.mob.type",storage:"$STORE",interpret:true,color:"white"}]""")
			raw("playsound minecraft:entity.item.pickup player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 0.8")
		}

		// As the caught mob: gone without a trace. Far below the world, whatever it drops falls out.
		pack.define(removeMob) {
			// Tied to a fence or a mob: the lead comes off where it is, one lead, as when a leash
			// breaks. Its own leash then goes with it below the world, where nothing it drops is
			// left. A mob it holds on a lead drops that lead by itself once its holder is gone.
			raw("scoreboard players set #cn.leashed $T 0")
			raw("execute on leasher run scoreboard players set #cn.leashed $T 1")
			raw("""execute at @s if score #cn.leashed $T matches 1 run summon minecraft:item ~ ~0.5 ~ {Item:{id:"minecraft:lead",count:1},PickupDelay:10s}""")
			raw("""data merge entity @s {Silent:1b,DeathLootTable:"minecraft:empty",Size:0}""")
			// No "<name> died" sent to an owner.
			raw("execute if data entity @s Owner run data modify entity @s Owner set value [I;0,0,0,0]")
			raw("execute at @s run tp @s ~ -400 ~")
			raw("kill @s")
		}

		// As the player holding a full net.
		pack.define(release) {
			raw("data remove $N.mob")
			raw("data modify $N.mob set from entity @s SelectedItem.components.\"minecraft:custom_data\".ra.capture_net.mob")
			raw("data remove storage ra:safe s")
			raw("data modify storage ra:safe s set from $N.mob.id")
			raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
			raw("scoreboard players set #cn.ok $T 0")
			raw("execute store result score #cn.ok $T run function ${RaLib.Safe.check.asId()}")
			raw("execute unless score #cn.ok $T matches 1 run return run ${say("This net holds something it cannot let out.", "red")}")
			// Only a type on this net's list, checked before anything is summoned.
			raw("scoreboard players set #cn.ok $T 0")
			raw("execute store result score #cn.ok $T run function ${CaptureNetMobs.allowed.asId()} with $N.mob")
			raw("execute unless score #cn.ok $T matches 1 run return run ${say("This net holds something it cannot let out.", "red")}")
			raw("scoreboard players set #cn.hit $T 0")
			raw("scoreboard players set #cn.ray $T $REACH_STEPS")
			raw("execute at @s anchored eyes positioned ^ ^ ^ anchored feet run function ${rayBlock.asId()}")
			raw("data remove $N.mob")
			raw("execute if score #cn.hit $T matches 0 run ${say("Right-click a block to let it out.", "gray")}")
		}

		// One point of the look, as the player: the next point is a block the crosshair stops on.
		pack.define(rayBlock) {
			raw("execute unless block ^ ^ ^0.1 ${BlockTags.rayPassable.asId()} if block ~ ~ ~ ${BlockTags.rayPassable.asId()} run return run function ${releaseAt.asId()}")
			raw("scoreboard players remove #cn.ray $T 1")
			raw("execute if score #cn.ray $T matches 1.. positioned ^ ^ ^0.1 run function ${rayBlock.asId()}")
		}

		// As the player, at the last free point: on the floor of that block.
		pack.define(releaseAt) {
			raw("scoreboard players set #cn.hit $T 1")
			raw("execute align xyz positioned ~0.5 ~ ~0.5 run function ${summonMob.asId()} with $N.mob")
		}

		// {id}: a type id that passed ra_lib:safe/check in format ID. As the player.
		pack.define(summonMob) {
			raw("scoreboard players set #cn.ok $T 0")
			raw("\$execute store success score #cn.ok $T run summon \$(id) ~ ~ ~ {Tags:[\"$NEW\"]}")
			raw("execute if score #cn.ok $T matches 0 run return run ${say("It cannot be let out here.", "red")}")
			raw("execute store result score #cn.diff $T run difficulty")
			raw("execute if score #cn.diff $T matches 0 if entity @e[tag=$NEW,type=${CaptureNetMobs.HOSTILE_TAG},distance=..1] run return run function ${vanish.asId()}")
			raw("execute as @e[tag=$NEW,distance=..1,limit=1] run function ${mergeMob.asId()}")
			raw("""item modify entity @s weapon.mainhand {type:"minecraft:set_components",components:{"minecraft:custom_data":{ra:{capture_net:{}}},"minecraft:lore":$emptyLore,"!minecraft:enchantment_glint_override":{}}}""")
			raw("particle minecraft:poof ~ ~0.5 ~ 0.3 0.3 0.3 0.02 8 normal")
			raw("playsound minecraft:entity.item.pickup player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.7 1.4")
		}

		// As the released mob.
		pack.define(mergeMob) {
			raw("data modify entity @s {} merge from $N.mob.data")
			raw("tag @s remove $NEW")
		}

		// As the player, the fresh mob tagged NEW: Peaceful would remove it, so it is taken back at
		// once and the net keeps the stored one.
		pack.define(vanish) {
			raw("execute as @e[tag=$NEW,distance=..1] at @s run tp @s ~ -400 ~")
			raw("kill @e[tag=$NEW]")
			raw(say("Peaceful would remove it: it stays in the net.", "yellow"))
		}
	}

	/** First lines of both advancement rewards: revoke, then stop unless this is a fresh press with a net in the main hand. */
	private fun Function.firstTickOfPress(adv: AdvancementArgument) {
		raw("advancement revoke @s only ${adv.asId()}")
		raw("execute unless function ${press.asId()} run return 0")
		raw("execute unless items entity @s weapon.mainhand $NET run return 0")
	}

	private fun say(text: String, color: String) =
		"""tellraw @s [$RA_CHAT_PREFIX,{text:"$text",color:"$color"}]"""
}
