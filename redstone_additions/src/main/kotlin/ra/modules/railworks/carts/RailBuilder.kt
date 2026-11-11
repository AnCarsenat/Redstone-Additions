package ra.modules.railworks.carts

import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.features.tags.itemTag
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.Tags
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.define
import ra.core.item.SByte
import ra.core.item.SRaw
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref

/**
 * The Rail Builder: a hopper minecart that lays a straight track by itself.
 *
 * Put it on a straight rail, load it (rails, a filler block, redstone torches, powered rails if you
 * like) and push it. It stops, then builds in the direction it was pushed, one block every
 * [INTERVAL] ticks ([RailBuilderStep]): digs a tunnel two blocks high as a diamond pickaxe would,
 * fills the floor where there is nothing to stand on, lays a rail and moves onto it. Every
 * [TORCH_EVERY] blocks it puts a redstone torch on the tunnel wall; when it carries powered rails,
 * the rail under the torch is a powered rail, which the torch powers. When it cannot go on, it says
 * why and rolls back along its track to where it started.
 *
 * WHY A HOPPER MINECART. The game does the inventory: what it digs is dropped onto it and picked
 * up as any hopper minecart picks items up, merging stacks; what does not fit stays on the track,
 * behind it as it moves on. Breaking it drops its contents as any hopper minecart does, and the
 * [CartTracker] turns the hopper minecart item it drops back into a Rail Builder.
 *
 * STATE, all scores on the cart: [STATE] 0 idle, 1 building, 2 going back; [DIR] 1 east, 2 west,
 * 3 south, 4 north; [START] twice the start block centre's coordinate along the line (exact: a
 * centre is n + 0.5); [STEPS] blocks since the last torch; [TIMER] ticks to the next step, or ticks
 * without moving on the way back; [LAST] the last position on the way back; [BUILT] blocks laid.
 *
 * COST. One selector a tick for `hopper_minecart` with [TAG], nothing else when there is none. An
 * idle builder reads its speed every [IDLE_EVERY] ticks; a building one works every [INTERVAL]
 * ticks; one going back is pushed each tick until it arrives.
 */
internal object RailBuilder {
	const val TAG = "ra.rw.builder"

	const val STATE = "ra.rb.state"
	const val DIR = "ra.rb.dir"
	const val START = "ra.rb.start"
	const val STEPS = "ra.rb.n"
	const val TIMER = "ra.rb.t"
	const val LAST = "ra.rb.last"
	const val BUILT = "ra.rb.built"
	val objectives = listOf(STATE, DIR, START, STEPS, TIMER, LAST, BUILT)

	/** Ticks between two build steps. */
	const val INTERVAL = 6

	/** A redstone torch (and a powered rail, if it has some) every this many blocks. */
	const val TORCH_EVERY = 8

	/** An idle builder checks whether it was pushed every this many ticks. */
	const val IDLE_EVERY = 4

	/** Ticks without moving on the way back before it gives up. */
	const val STUCK_TICKS = 60

	/** Pushed faster than this (thousandths of a block per tick) starts it. */
	const val LAUNCH_SPEED = 20

	private const val T = Obj.TEMP
	const val SELF = "ra.rb.self"

	val UNBREAKABLE = "#ra_railworks:builder_unbreakable"
	val LIQUID = "#ra_railworks:builder_liquid"
	val CLEAR = "#ra_railworks:builder_clear"
	val FLOOR = "#ra_railworks:builder_floor"
	val NOT_WALL = "#ra_railworks:builder_not_wall"
	val FILLER = "#ra_railworks:builder_filler"

	/** Items it lays as the floor: stone-like blocks that stay where they are put (no sand or gravel). */
	val FILLER_ITEMS = listOf(
		"cobblestone", "cobbled_deepslate", "stone", "deepslate", "dirt", "coarse_dirt", "netherrack",
		"andesite", "diorite", "granite", "tuff", "blackstone", "basalt", "end_stone", "stone_bricks",
		"mud_bricks", "bricks", "sandstone", "red_sandstone",
	)

	private fun fn(path: String) = ref(Ns.RAILWORKS, "carts/builder/$path")

	val give = fn("give")
	val pass = fn("pass")
	private val tick = fn("tick")
	private val idle = fn("idle")
	private val back = fn("back")
	val stop = fn("stop")

	val item = itemSpec("minecraft:hopper_minecart") {
		name("Rail Builder")
		rarity("uncommon")
		customData(raData("rail_builder" to SByte(true)))
		lore(
			loreLine("Place on a straight rail, load it, push it", "gray"),
			loreLine("Digs, fills the floor, lays rails and torches", "gray"),
			loreLine("Rolls back to the start when it cannot go on", "dark_gray"),
		)
		add("entity_data", SRaw("""{id:"minecraft:hopper_minecart",Tags:["$TAG","${CartTracker.TRACKED}"],CustomName:"Rail Builder"}"""))
	}

	fun Ra.registerRailBuilder() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Rail Builder",color:"yellow"}]""")
		}
		pack.shapedRecipe(Ns.RAILWORKS, "rail_builder", item.recipeResult(Items.HOPPER_MINECART), Unlock(Items.HOPPER_MINECART), RecipeCategory.MISC) {
			pattern("RPR", "DHD")
			key("R", Items.RAIL)
			key("P", Items.DIAMOND_PICKAXE)
			key("D", Items.DISPENSER)
			key("H", Items.HOPPER_MINECART)
		}
		registerTags()

		// The module tick: a phase for the idle checks, then every builder.
		pack.define(pass) {
			raw("scoreboard players add #rb.phase $TIMER 1")
			raw("execute if score #rb.phase $TIMER matches $IDLE_EVERY.. run scoreboard players set #rb.phase $TIMER 0")
			raw("execute as @e[type=minecraft:hopper_minecart,tag=$TAG] at @s run function ${tick.asId()}")
		}

		pack.define(tick) {
			raw("execute if score @s $STATE matches 2 run return run function ${back.asId()}")
			raw("execute unless score @s $STATE matches 1 run return run function ${idle.asId()}")
			raw("scoreboard players add @s $TIMER 1")
			raw("execute if score @s $TIMER matches ..${INTERVAL - 1} run return 0")
			raw("scoreboard players set @s $TIMER 0")
			raw("execute positioned ~ ~0.2 ~ run function ${RailBuilderStep.step.asId()}")
		}

		registerIdle()
		registerBack()
		RailBuilderStep.register(this)
	}

	/** Pushed while idle on a straight rail: start building that way. */
	private fun Ra.registerIdle() {
		pack.define(idle) {
			raw("execute unless score #rb.phase $TIMER matches 0 run return 0")
			raw("execute store result score #rb.vx $T run data get entity @s Motion[0] 1000")
			raw("execute store result score #rb.vz $T run data get entity @s Motion[2] 1000")
			raw("scoreboard players operation #rb.ax $T = #rb.vx $T")
			raw("scoreboard players operation #rb.az $T = #rb.vz $T")
			raw("scoreboard players set #rb.neg $T -1")
			raw("execute if score #rb.ax $T matches ..-1 run scoreboard players operation #rb.ax $T *= #rb.neg $T")
			raw("execute if score #rb.az $T matches ..-1 run scoreboard players operation #rb.az $T *= #rb.neg $T")
			raw("execute if score #rb.ax $T matches ..${LAUNCH_SPEED - 1} if score #rb.az $T matches ..${LAUNCH_SPEED - 1} run return 0")
			raw("scoreboard players set #rb.d $T 0")
			raw("execute if score #rb.ax $T >= #rb.az $T if score #rb.vx $T matches 1.. run scoreboard players set #rb.d $T 1")
			raw("execute if score #rb.ax $T >= #rb.az $T if score #rb.vx $T matches ..0 run scoreboard players set #rb.d $T 2")
			raw("execute if score #rb.ax $T < #rb.az $T if score #rb.vz $T matches 1.. run scoreboard players set #rb.d $T 3")
			raw("execute if score #rb.ax $T < #rb.az $T if score #rb.vz $T matches ..0 run scoreboard players set #rb.d $T 4")
			// Only from a straight rail along the push.
			raw("execute if score #rb.d $T matches 1..2 positioned ~ ~0.2 ~ unless block ~ ~ ~ #minecraft:rails[shape=east_west] run return 0")
			raw("execute if score #rb.d $T matches 3..4 positioned ~ ~0.2 ~ unless block ~ ~ ~ #minecraft:rails[shape=north_south] run return 0")
			raw("scoreboard players operation @s $DIR = #rb.d $T")
			raw("scoreboard players set @s $STATE 1")
			raw("scoreboard players set @s $TIMER 0")
			raw("scoreboard players set @s $STEPS ${TORCH_EVERY - 1}")
			raw("scoreboard players set @s $BUILT 0")
			raw("execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~")
			raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			raw("execute if score @s $DIR matches 1..2 store result score @s $START run data get entity @s Pos[0] 2")
			raw("execute if score @s $DIR matches 3..4 store result score @s $START run data get entity @s Pos[2] 2")
			for ((d, name) in listOf(1 to "east", 2 to "west", 3 to "south", 4 to "north"))
				raw("""execute if score @s $DIR matches $d run tellraw @a[distance=..32] [{text:"[Rail Builder] ",color:"gold"},{text:"Building $name",color:"green"}]""")
			raw("playsound minecraft:block.piston.extend block ${soundListenersSel(24)} ~ ~ ~ 0.6 0.8")
		}

		// As the builder: start going back, telling the players near it why ([RailBuilderStep.REASONS] in #rb.why).
		pack.define(stop) {
			raw("scoreboard players set @s $STATE 2")
			raw("scoreboard players set @s $TIMER 0")
			raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			for ((code, reason) in RailBuilderStep.REASONS)
				raw("""execute if score #rb.why $T matches $code run tellraw @a[distance=..32] [{text:"[Rail Builder] ",color:"gold"},{text:"Stopped: $reason",color:"yellow"},{text:". Going back, ",color:"gray"},{score:{name:"@s",objective:"$BUILT"},color:"aqua"},{text:" blocks built.",color:"gray"}]""")
			raw("playsound minecraft:block.piston.contract block ${soundListenersSel(24)} ~ ~ ~ 0.6 0.8")
		}
	}

	/** Going back: pushed along the line, against the build direction, until it passes its start. */
	private fun Ra.registerBack() {
		val stuck = fn("stuck")
		pack.define(back) {
			for (d in Dir.ALL) raw("execute if score @s $DIR matches ${d.code} run return run function ${fn("back_${d.name}").asId()}")
			// A direction that is none of them: stand still.
			raw("scoreboard players set @s $STATE 0")
		}
		for (d in Dir.ALL) pack.define(fn("back_${d.name}")) {
			val axis = if (d.dx != 0) 0 else 2
			val sign = d.dx + d.dz
			raw("execute store result score #rb.c $T run data get entity @s Pos[$axis] 2")
			val passed = if (sign > 0) "<=" else ">="
			raw("execute if score #rb.c $T $passed @s $START run return run function ${fn("arrive_$axis").asId()}")
			raw("data modify entity @s Motion set value [${-d.dx * 0.4}d,0.0d,${-d.dz * 0.4}d]")
			raw("execute if score #rb.c $T = @s $LAST run scoreboard players add @s $TIMER 1")
			raw("execute unless score #rb.c $T = @s $LAST run scoreboard players set @s $TIMER 0")
			raw("scoreboard players operation @s $LAST = #rb.c $T")
			raw("execute if score @s $TIMER matches $STUCK_TICKS.. run function ${stuck.asId()}")
		}
		for (axis in listOf(0, 2)) pack.define(fn("arrive_$axis")) {
			raw("execute store result entity @s Pos[$axis] double 0.5 run scoreboard players get @s $START")
			raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			raw("scoreboard players set @s $STATE 0")
			raw("""tellraw @a[distance=..32] [{text:"[Rail Builder] ",color:"gold"},{text:"Back at the start.",color:"green"}]""")
			raw("playsound minecraft:block.note_block.bell block ${soundListenersSel(24)} ~ ~ ~ 0.6 1.2")
		}
		pack.define(stuck) {
			raw("data modify entity @s Motion set value [0.0d,0.0d,0.0d]")
			raw("scoreboard players set @s $STATE 0")
			raw("""tellraw @a[distance=..32] [{text:"[Rail Builder] ",color:"gold"},{text:"Something blocks the way back; it stopped here.",color:"yellow"}]""")
		}
	}

	private fun Ra.registerTags() {
		val ns = Ns.RAILWORKS.id
		pack.blockTag("builder_unbreakable", ns) {
			add(Tags.Block.WITHER_IMMUNE)
			for (b in listOf("nether_portal", "end_portal_frame", "reinforced_deepslate", "budding_amethyst")) add("minecraft:$b")
		}
		pack.blockTag("builder_liquid", ns) {
			for (b in listOf("water", "lava", "bubble_column")) add("minecraft:$b")
		}
		pack.blockTag("builder_clear", ns) {
			for (b in listOf("air", "cave_air")) add("minecraft:$b")
		}
		// Under the track: what it fills. Nothing a rail can stand on.
		pack.blockTag("builder_floor", ns) {
			add(Tags.Block.REPLACEABLE)
		}
		// Not a wall a torch can hang on.
		pack.blockTag("builder_not_wall", ns) {
			for (t in listOf(Tags.Block.REPLACEABLE, Tags.Block.RAILS, Tags.Block.LEAVES, Tags.Block.FENCES, Tags.Block.WALLS)) add(t)
			for (b in listOf("glass_pane", "iron_bars", "redstone_wall_torch", "wall_torch", "ladder")) add("minecraft:$b")
		}
		pack.itemTag("builder_filler", ns) {
			for (i in FILLER_ITEMS) add("minecraft:$i")
		}
	}

	/** A build direction. [code] is the [DIR] score; right of the travel direction is (-dz, dx). */
	data class Dir(val code: Int, val name: String, val dx: Int, val dz: Int) {
		val axis get() = if (dx != 0) "east_west" else "north_south"
		val rx get() = -dz
		val rz get() = dx
		companion object {
			val ALL = listOf(Dir(1, "east", 1, 0), Dir(2, "west", -1, 0), Dir(3, "south", 0, 1), Dir(4, "north", 0, -1))
		}

		/** A relative position [ahead] blocks forward, [up] up and [right] to the right. */
		fun at(ahead: Int, up: Int, right: Int = 0): String {
			fun c(v: Int) = if (v == 0) "~" else "~$v"
			return "${c(dx * ahead + rx * right)} ${c(up)} ${c(dz * ahead + rz * right)}"
		}
	}

}
