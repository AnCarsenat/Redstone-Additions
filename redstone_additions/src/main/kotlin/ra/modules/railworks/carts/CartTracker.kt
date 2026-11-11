package ra.modules.railworks.carts

import io.github.ayfri.kore.features.tags.itemTag
import io.github.ayfri.kore.functions.Function
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * Carts that carry something of ours (a [RamUpgrade], or that are a [RailBuilder]) and what they
 * drop when they break.
 *
 * A MINECART HAS NO DEATH EVENT. When a cart breaks it is simply gone, and the game drops its own
 * item (`minecraft:minecart`, `minecraft:hopper_minecart`...). So each tracked cart has a marker,
 * its tracker, that follows it: every tick the cart moves its tracker onto itself and stamps it
 * with the game time ([SEEN]). A tracker that was not stamped this tick has lost its cart. If a
 * cart item dropped this tick or the last two lies within 2 blocks of it, the cart was broken
 * there: the tracker drops the Ram Upgrade, turns the vanilla hopper minecart item into a Rail
 * Builder, and removes itself. If there is no such item, the cart was removed some other way
 * (`/kill`, a creative player, a chunk that unloaded under it, a portal): the tracker removes
 * itself and drops nothing, and a cart that comes back makes itself a new tracker.
 *
 * NO DUPLICATE, NO LOSS. An upgrade is dropped only next to the item the game dropped for the same
 * cart, and only once (the item is tagged [CLAIMED]). A creative player breaking a cart gets
 * nothing, as for the cart itself. Taking an upgrade off with the Wrench removes it from the cart
 * first, then replaces the tracker ([detach]), so the old tracker never sees "its cart gone".
 *
 * LINK. Cart and tracker share a number in [ID], handed out from `#rw.next`. A tracker is looked
 * for within 4 blocks of its cart: a cart moves 0.4 blocks a tick, a Rail Builder one block a step.
 *
 * COST. Per tracked cart per tick: one 4-block selector and a teleport. Per tick: one selector over
 * the trackers. Nothing for carts without upgrades.
 */
internal object CartTracker {
	const val ID = "ra.rw.id"
	const val SEEN = "ra.rw.seen"

	/** On a cart: it has a tracker. */
	const val TRACKED = "ra.rw.tracked"
	const val TRACKER = "ra.rw.tracker"
	private const val NEW = "ra.rw.new"
	private const val CLAIMED = "ra.rw.claimed"
	private const val LOST = "ra.rw.lost"

	/** Tracker tags: what the cart carried. */
	const val T_RAM = "ra.rw.t.ram"
	const val T_BUILDER = "ra.rw.t.builder"

	const val CARTS = "#ra_railworks:minecarts"
	const val CART_ITEMS = "#ra_railworks:minecart_items"

	/** Within this distance of its cart, a tracker is found again. */
	private const val FOLLOW = 4

	private const val T = Obj.TEMP

	private fun fn(path: String) = ref(Ns.RAILWORKS, "carts/$path")

	/** As a tracked cart, at it, from the module tick. */
	val cartTick = fn("tracker/cart_tick")

	/** As the module tick: trackers whose cart is gone. */
	val lostPass = fn("tracker/lost_pass")

	/** As a cart, at it: track it, give it an id, and give it a tracker matching what it carries now. */
	val attach = fn("tracker/attach")

	/** As a cart, at it, after an upgrade came off: replace its tracker, or stop tracking it. */
	val detach = fn("tracker/detach")

	private val follow = fn("tracker/follow")
	private val summon = fn("tracker/summon")
	private val killOwn = fn("tracker/kill_own")
	private val lost = fn("tracker/lost")
	private val claim = fn("tracker/claim")
	private val convert = fn("tracker/convert_builder")

	val objectives = listOf(ID, SEEN)

	fun register(ra: Ra, ramStack: String, builderStack: String) = with(ra) {
		pack.itemTag("minecart_items", Ns.RAILWORKS.id) {
			for (i in listOf("minecart", "chest_minecart", "furnace_minecart", "hopper_minecart", "tnt_minecart", "command_block_minecart"))
				add("minecraft:$i")
		}

		pack.define(cartTick) {
			raw("execute unless score @s $ID = @s $ID run function ${attach.asId()}")
			raw("scoreboard players operation #rw.id $T = @s $ID")
			raw("scoreboard players set #rw.found $T 0")
			raw("execute as @e[type=marker,tag=$TRACKER,distance=..$FOLLOW] if score @s $ID = #rw.id $T store result score #rw.found $T run function ${follow.asId()}")
			raw("execute if score #rw.found $T matches 0 run function ${summon.asId()}")
			// The upgrades' own ticks, after the tracker has followed the cart.
			raw("execute if entity @s[tag=${RamUpgrade.TAG}] run function ${RamUpgrade.tick.asId()}")
		}

		// As the tracker, positioned at its cart.
		pack.define(follow) {
			raw("tp @s ~ ~ ~")
			raw("scoreboard players operation @s $SEEN = #rw.now $T")
			raw("return 1")
		}

		pack.define(attach) {
			raw("tag @s add $TRACKED")
			raw("execute unless score @s $ID = @s $ID run scoreboard players add #rw.next $ID 1")
			raw("execute unless score @s $ID = @s $ID run scoreboard players operation @s $ID = #rw.next $ID")
			raw("function ${killOwn.asId()}")
			raw("function ${summon.asId()}")
		}

		pack.define(detach) {
			raw("function ${killOwn.asId()}")
			raw("execute unless entity @s[tag=${RamUpgrade.TAG}] unless entity @s[tag=${RailBuilder.TAG}] run return run tag @s remove $TRACKED")
			raw("function ${summon.asId()}")
		}

		pack.define(killOwn) {
			raw("scoreboard players operation #rw.id $T = @s $ID")
			raw("execute as @e[type=marker,tag=$TRACKER,distance=..$FOLLOW] if score @s $ID = #rw.id $T run kill @s")
		}

		// As the cart, at it.
		pack.define(summon) {
			val sel = "@e[type=marker,tag=$NEW,distance=..0.1,limit=1]"
			raw("""summon minecraft:marker ~ ~ ~ {Tags:["ra","$TRACKER","$NEW"]}""")
			raw("scoreboard players operation $sel $ID = @s $ID")
			raw("execute store result score $sel $SEEN run time query gametime")
			raw("execute if entity @s[tag=${RamUpgrade.TAG}] run tag $sel add $T_RAM")
			raw("execute if entity @s[tag=${RailBuilder.TAG}] run tag $sel add $T_BUILDER")
			raw(Settings.Purge.stampLine(sel))
			raw("tag $sel remove $NEW")
		}

		pack.define(lostPass) {
			raw("execute as @e[type=marker,tag=$TRACKER] unless score @s $SEEN = #rw.now $T at @s run function ${lost.asId()}")
		}

		// As a tracker whose cart was not seen this tick, at it.
		pack.define(lost) {
			raw("tag @s add $LOST")
			raw("scoreboard players set #rw.drop $T 0")
			raw("execute unless entity @s[tag=$T_RAM] unless entity @s[tag=$T_BUILDER] run scoreboard players set #rw.drop $T 1")
			raw("execute as @e[type=item,distance=..2,tag=!$CLAIMED] if score #rw.drop $T matches 0 if items entity @s contents $CART_ITEMS at @s run function ${claim.asId()}")
			raw("kill @s")
		}

		// As a cart item near a lost tracker, at it.
		pack.define(claim) {
			raw("execute store result score #rw.age $T run data get entity @s Age")
			raw("execute unless score #rw.age $T matches 0..2 run return 0")
			raw("scoreboard players set #rw.drop $T 1")
			raw("tag @s add $CLAIMED")
			fun lostWith(tag: String) = "@e[type=marker,tag=$LOST,tag=$tag,distance=..3,limit=1]"
			raw("execute if entity ${lostWith(T_RAM)} run summon minecraft:item ~ ~ ~ {Item:$ramStack,PickupDelay:10s}")
			raw("execute if entity ${lostWith(T_BUILDER)} if items entity @s contents minecraft:hopper_minecart run function ${convert.asId()}")
		}

		// As the vanilla hopper minecart item a Rail Builder dropped: one of them becomes the Rail Builder.
		pack.define(convert) {
			raw("execute store result score #rw.cnt $T run data get entity @s Item.count")
			raw("execute if score #rw.cnt $T matches ..1 run return run data modify entity @s Item set value $builderStack")
			raw("execute store result entity @s Item.count int 1 run scoreboard players remove #rw.cnt $T 1")
			raw("summon minecraft:item ~ ~ ~ {Item:$builderStack,PickupDelay:10s}")
		}
	}

	/** In the module tick, before the cart passes. */
	fun Function.stampNow() = raw("execute store result score #rw.now $T run time query gametime")
}
