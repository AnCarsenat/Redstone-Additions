package ra.modules.railworks

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.tags.entityTypeTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.markerSel
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw

/**
 * When a [RouteRail] looks at carts, and how it decides.
 *
 * COST. Every other tick, one test: is there a minecart anywhere loaded ([CARTS])? With none,
 * that is all. With some, each Route Rail runs one box-bounded selector for a cart within
 * [RADIUS] blocks of its rail; only a rail with a cart that close reads its properties, checks the
 * nearest cart and sets the rail. A cart at the game's top speed (8 blocks a second, 0.4 per
 * tick) crosses the [RADIUS] in more than 4 ticks, so it is always seen before it reaches the
 * junction.
 *
 * THE MATCH. As the cart, with the rail's properties in `storage ra:temp rr.p`: `route` is copied
 * into `rr.c`, then the cart's value is copied over it; `store success` is 0 when nothing changed,
 * that is, when the two are equal. A missing value never reaches the copy (a failed copy would read
 * as "equal"). Lists (`Tags`, `Items`) are walked one entry at a time, the same way.
 */
internal object RouteRailScan {
	/** Entity type tag `#ra_railworks:minecarts`. */
	const val CARTS_TAG = "minecarts"
	private val CARTS = "#${Ns.RAILWORKS.id}:$CARTS_TAG"

	/** Distance from the rail's centre at which a cart is looked at. */
	const val RADIUS = 2.5

	private const val T = Obj.TEMP
	private const val S = "storage ra:temp rr"
	private const val PHASE = "#rr.phase"

	val run = RouteRail.block.fn("scan")
	private val one = RouteRail.block.fn("scan_one")
	private val match = RouteRail.block.fn("match")
	private val matchName = RouteRail.block.fn("match_name")
	private val walkTags = RouteRail.block.fn("walk_tags")
	private val walkItems = RouteRail.block.fn("walk_items")
	private val setShape = RouteRail.block.fn("set_shape")

	private fun cartNear(extra: String = "") = "@e[type=$CARTS,distance=..$RADIUS$extra]"

	/** [into] = 0 when `rr.c` (a copy of `route`) equals the value at [source], else 1. */
	private fun Function.differs(source: String, into: String = "#rr.d") {
		raw("scoreboard players set $into $T 1")
		raw("data modify $S.c set from $S.p.route")
		raw("execute if data $source store success score $into $T run data modify $S.c set from $source")
	}

	fun register(ra: Ra) = with(ra) {
		pack.entityTypeTag(CARTS_TAG, Ns.RAILWORKS.id) {
			for (e in listOf(
				EntityTypes.MINECART, EntityTypes.CHEST_MINECART, EntityTypes.FURNACE_MINECART, EntityTypes.HOPPER_MINECART,
				EntityTypes.TNT_MINECART, EntityTypes.SPAWNER_MINECART, EntityTypes.COMMAND_BLOCK_MINECART,
			)) add(e)
		}

		// In the block's tick, after the break check.
		pack.define(run) {
			raw("scoreboard players add $PHASE $T 1")
			raw("execute if score $PHASE $T matches 2.. run scoreboard players set $PHASE $T 0")
			raw("execute unless score $PHASE $T matches 0 run return 0")
			raw("execute unless entity @e[type=$CARTS,limit=1] run return 0")
			executeRun("as ${RouteRail.block.markerSel()} at @s positioned ~ ~1 ~ if entity ${cartNear(",limit=1")}", one)
		}

		// As the marker, positioned at the centre of the rail block on top.
		pack.define(one) {
			raw("execute unless block ~ ~ ~ minecraft:rail run return 0")
			raw("data remove $S")
			raw("data modify $S.p set from entity @s data.properties")
			raw("scoreboard players set #rr.m $T 0")
			executeRun("as @n[type=$CARTS,distance=..$RADIUS]", match)
			raw("data modify $S.want set from $S.p.on_miss")
			raw("execute if score #rr.m $T matches 1 run data modify $S.want set from $S.p.on_match")
			function(setShape)
			// Written only on a change: a marker's data is not rewritten every other tick a cart waits.
			raw("""execute if score #rr.m $T matches 1 unless data entity @s data.status{last:"match"} run data modify entity @s data.status.last set value "match"""")
			raw("""execute if score #rr.m $T matches 0 unless data entity @s data.status{last:"no match"} run data modify entity @s data.status.last set value "no match"""")
			raw("data remove $S")
		}

		// As the cart. #rr.m = 1 when it carries the route.
		pack.define(match) {
			raw("""execute if data $S.p{source:"rider"} on passengers run return run scoreboard players set #rr.m $T 1""")
			raw("""execute if data $S.p{source:"rider"} run return 0""")
			raw("execute unless data $S.p.route run return 0")
			raw("""execute if data $S.p{route:""} run return 0""")
			raw("""execute if data $S.p{source:"name"} run return run function ${matchName.asId()}""")
			raw("""execute if data $S.p{source:"tag"} run data modify $S.l set from entity @s Tags""")
			raw("""execute if data $S.p{source:"tag"} run return run function ${walkTags.asId()}""")
			raw("""execute if data $S.p{source:"item"} run data modify $S.l set from entity @s Items""")
			raw("""execute if data $S.p{source:"item"} run return run function ${walkItems.asId()}""")
		}

		// A name tag's name is a plain string; a name set by command may be a text component.
		pack.define(matchName) {
			differs("entity @s CustomName")
			raw("execute if score #rr.d $T matches 0 run return run scoreboard players set #rr.m $T 1")
			differs("entity @s CustomName.text")
			raw("execute if score #rr.d $T matches 0 run scoreboard players set #rr.m $T 1")
		}

		for ((fn, path) in listOf(walkTags to "$S.l[0]", walkItems to "$S.l[0].id")) pack.define(fn) {
			raw("execute unless data $S.l[0] run return 0")
			differs(path)
			raw("execute if score #rr.d $T matches 0 run return run scoreboard players set #rr.m $T 1")
			raw("data remove $S.l[0]")
			function(fn)
		}

		// Positioned at the rail. `rr.want`: one of RouteRail.SHAPES; `none` or anything else leaves it.
		pack.define(setShape) {
			raw("scoreboard players set #rr.sw $T 0")
			for (shape in RouteRail.SHAPES.filter { it != "none" }) for (wet in listOf(false, true)) {
				raw(
					"""execute if data $S{want:"$shape"} if block ~ ~ ~ minecraft:rail[waterlogged=$wet] unless block ~ ~ ~ minecraft:rail[shape=$shape] """ +
						"store success score #rr.sw $T run setblock ~ ~ ~ minecraft:rail[shape=$shape,waterlogged=$wet]",
				)
			}
			raw("execute if score #rr.sw $T matches 1 run playsound minecraft:block.lever.click block ${soundListenersSel(16)} ~ ~ ~ 0.4 1.6")
		}
	}
}
