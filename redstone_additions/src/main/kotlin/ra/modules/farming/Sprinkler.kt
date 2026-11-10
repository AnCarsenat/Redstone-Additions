package ra.modules.farming

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.blockRegisterMessage
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.particleViewersSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.define
import ra.core.ensureScore
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.ref
import ra.modules.interactive.specGive
import ra.modules.wires.machine.MachineClock

/**
 * Sprinkler: waters the farmland around it from a fluid network, and with liquid experience makes
 * the crops around it grow about four times as fast.
 *
 * NOT A NETWORK NODE. Like the Book Upgrader, it reaches into the network of a pipe, tank or drain
 * touching any of its six faces, so placing or breaking one never splits or joins a network. The
 * first neighbour whose network holds the whole price pays it.
 *
 * THE AREA. A square of `radius` blocks each way (1 to [MAX_RADIUS], admins up to
 * [ADMIN_MAX_RADIUS]), from one block below the sprinkler to one block above it. That covers a
 * sprinkler sitting in the farmland itself, where a water hole would be (the vanilla 9x9 farm is
 * radius 4, the default), and one standing on the farmland among the crops.
 *
 * WATER. Every [WATER_EVERY] seconds, [WATER_ML_PER_BLOCK] mL of water per block of the square's
 * area: all the farmland in the area becomes fully moist, which is what makes vanilla crops grow at
 * their best rate. One `fill`, which changes nothing (and costs next to nothing) on farmland that
 * is already wet.
 *
 * EXPERIENCE. Once a second, extra growth at three times the vanilla random tick rate (3 random
 * ticks per 4096 blocks per game tick), so with vanilla's own growth a crop grows about 4x as fast.
 * Each extra growth picks a random column of the area, like a random tick, and moves the crop there
 * up one age; the rate is `area x 15 / 1024` picks a second, the remainder rolled. Only a crop
 * that actually grew is paid for, [XP_ML_PER_GROWTH] mL each: a pick that lands on dirt or on a
 * ripe crop costs nothing. The network must hold enough for every pick before any is made.
 *
 * CHEAP WHEN IDLE. Per tick only the break check every block has. The rest runs on the network
 * machines' once-a-second clock ([MachineClock]): a locked sprinkler stops at the redstone read,
 * one with no fluid neighbour costs six selector tests. Redstone locks it, as it does a hopper.
 *
 * NO PLAYER TEXT IN A COMMAND. The only macro arguments are numbers this pack computed (the radius
 * after [readCappedProperty], random offsets, prices).
 */
object Sprinkler {
	val block = CustomBlock(Ns.FARMING, "sprinkler", "Sprinkler", model = BLOCK)
	val item = block.loreItem {
		lore(
			loreLine("Waters the farmland around it", "gray"),
			loreLine("Liquid experience: crops grow 4x as fast", "gray"),
			loreLine("Feed it from a pipe, tank or drain beside it", "dark_gray"),
		)
	}
	val registerBlock = block.fn("register_block")

	/** As the marker, at it, once a second from [MachineClock]. */
	val run = block.fn("run")

	private val water = block.fn("water")
	private val waterFill = block.fn("water_fill")
	private val grow = block.fn("grow")
	private val pick = block.fn("pick")
	private val at = block.fn("at")
	private val growHere = block.fn("grow_here")

	const val BLOCK = "minecraft:waxed_oxidized_copper_grate"

	const val DEFAULT_RADIUS = 4
	/** A player's largest radius: a 17x17 field. */
	const val MAX_RADIUS = 8
	/** An admin's (ra.core.Admin). The water `fill` covers (2r+1)^2 x 3 blocks, well under 32768. */
	const val ADMIN_MAX_RADIUS = 32

	/** Seconds between two waterings. Farmland takes minutes to dry out. */
	const val WATER_EVERY = 5
	const val WATER_ML_PER_BLOCK = 4
	/** One crop stage grown: a tenth of an experience point (100 mL a point). */
	const val XP_ML_PER_GROWTH = 10

	/** What it grows: the crops whose only block state is `age`, with their last age. */
	private val CROPS = listOf(
		"wheat" to 7, "carrots" to 7, "potatoes" to 7, "beetroots" to 3, "nether_wart" to 3,
		"melon_stem" to 7, "pumpkin_stem" to 7, "torchflower_crop" to 1, "sweet_berry_bush" to 3,
	)
	private val CROPS_TAG = "#${Ns.FARMING.id}:sprinkler_crops"

	private const val T = Obj.TEMP
	private const val S = "storage ra:farming spr"
	/** On the neighbouring fluid node that pays, for one payment. */
	private const val SRC = "ra.farm.src"
	private val SIDES = listOf("~1 ~ ~", "~-1 ~ ~", "~ ~ ~1", "~ ~ ~-1", "~ ~-1 ~", "~ ~1 ~")
	private const val MEDIA = "water experience"

	fun Ra.registerSprinkler() {
		block(block)
		properties(block) {
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.BUCKET), count = null) {
			pattern(" G ", "CBC", " C ")
			key("G", listOf(Items.COPPER_GRATE, Items.WAXED_COPPER_GRATE))
			key("C", listOf(Items.COPPER_INGOT))
			key("B", listOf(Items.BUCKET))
		}

		pack.blockTag("sprinkler_crops", Ns.FARMING.id) {
			for ((id, _) in CROPS) add("minecraft:$id")
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.radius", "$DEFAULT_RADIUS")
			raw("tag ${block.newMarkerSel()} add ${MachineClock.TAG}")
			finishPlacement(block, removePlaceTag = false, withinBlock = false)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "yellow")
			stackedStatusLine("water", "Water: ", "aqua")
			stackedStatusLine("growth", "Growth: ", "green")
			stackedPropLine("radius", "Radius: ", "aqua", " blocks")
		}

		pack.define(run) { run() }
		pack.define(water) { water() }
		// Macro {r}: the radius, a number this pack read and capped.
		pack.define(waterFill) {
			raw("\$fill ~-\$(r) ~-2 ~-\$(r) ~\$(r) ~ ~\$(r) minecraft:farmland[moisture=7] replace minecraft:farmland")
		}
		pack.define(grow) { grow() }
		// Macro {r}: one random column of the square, then the next while picks are left.
		pack.define(pick) {
			raw("\$execute store result $S.x int 1 run random value -\$(r)..\$(r)")
			raw("\$execute store result $S.z int 1 run random value -\$(r)..\$(r)")
			raw("function ${at.asId()} with $S")
			raw("scoreboard players remove #spr.k $T 1")
			raw("execute if score #spr.k $T matches 1.. run function ${pick.asId()} with $S")
		}
		// Macro {x, z}: random offsets this pack rolled.
		pack.define(at) {
			raw("\$execute positioned ~\$(x) ~ ~\$(z) run function ${column.asId()}")
		}
		pack.define(column) { column() }
		pack.define(growHere) {
			for ((id, last) in CROPS) for (age in 0 until last) {
				raw("execute if block ~ ~ ~ minecraft:$id[age=$age] run return run setblock ~ ~ ~ minecraft:$id[age=${age + 1}]")
			}
			raw("return 0")
		}

		for (medium in MEDIA.split(' ')) {
			val scan = ref(Ns.FARMING, "blocks/sprinkler/scan_$medium")
			pack.define(find(medium)) {
				raw("scoreboard players set #spr.found $T 0")
				for (side in SIDES) executeRun("if score #spr.found $T matches 0 positioned $side as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1]", scan)
				raw("return run scoreboard players get #spr.found $T")
			}
			// As a neighbouring fluid node: claim it when its network holds the whole price.
			pack.define(scan) {
				raw("function ${RaLib.Transport.Net.read.asId()}")
				raw("execute store result score #spr.have $T run data get storage ra:transport cur.amounts.$medium")
				raw("execute unless score #spr.have $T >= #spr.need $T run return 0")
				raw("scoreboard players set #spr.found $T 1")
				raw("tag @s add $SRC")
			}
			// As the claimed node. Macro {amount}: a price this pack computed.
			pack.define(take(medium)) {
				raw("\$return run function ${RaLib.Transport.Net.take.asId()} {amount:\$(amount),medium:\"$medium\"}")
			}
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.copper_grate.break", cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.WAXED_OXIDIZED_COPPER_GRATE)) {}
	}

	private val column = block.fn("column")
	private fun find(medium: String) = ref(Ns.FARMING, "blocks/sprinkler/find_$medium")
	private fun take(medium: String) = ref(Ns.FARMING, "blocks/sprinkler/take_$medium")

	/** Pay `#spr.need` of [medium] to the node [find] claimed, and release it. */
	private fun Function.payClaimed(medium: String, cond: String = "execute") {
		raw("$cond store result $S.amount int 1 run scoreboard players get #spr.need $T")
		raw("$cond as @e[type=marker,tag=$SRC,distance=..2,limit=1] run function ${take(medium).asId()} with $S")
	}

	private fun Function.release() = raw("tag @e[type=marker,tag=$SRC,distance=..2] remove $SRC")

	/** As the marker, at it. */
	private fun Function.run() {
		raw("execute if function ${RaLib.Redstone.any.asId()} run return run data modify entity @s data.status.state set value \"locked\"")
		raw("data modify entity @s data.status.state set value \"running\"")

		readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
		raw("data modify $S set value {}")
		raw("execute store result $S.r int 1 run scoreboard players get #prop $T")
		// area = (2r + 1)^2
		raw("scoreboard players operation #spr.area $T = #prop $T")
		raw("scoreboard players operation #spr.area $T += #prop $T")
		raw("scoreboard players add #spr.area $T 1")
		raw("scoreboard players operation #spr.area $T *= #spr.area $T")

		ensureScore("@s", Obj.COOLDOWN)
		raw("scoreboard players remove @s ${Obj.COOLDOWN} 1")
		raw("execute if score @s ${Obj.COOLDOWN} matches ..0 run function ${water.asId()}")
		raw("function ${grow.asId()}")
	}

	/** As the marker, at it: water the farmland, every [WATER_EVERY] seconds whether or not it could. */
	private fun Function.water() {
		raw("scoreboard players set @s ${Obj.COOLDOWN} $WATER_EVERY")
		raw("scoreboard players set #spr.need $T $WATER_ML_PER_BLOCK")
		raw("scoreboard players operation #spr.need $T *= #spr.area $T")
		raw("execute store result score #spr.ok $T run function ${find("water").asId()}")
		raw("execute if score #spr.ok $T matches 0 run return run data modify entity @s data.status.water set value \"needs water\"")
		payClaimed("water")
		release()
		raw("function ${waterFill.asId()} with $S")
		raw("data modify entity @s data.status.water set value \"watering\"")
		raw("particle minecraft:splash ~ ~1 ~ 1.5 0.3 1.5 0 40 normal ${particleViewersSel()}")
		raw("playsound minecraft:weather.rain block ${soundListenersSel(16)} ~ ~ ~ 0.15 1.6")
	}

	/** As the marker, at it: the extra growth, paid in liquid experience. */
	private fun Function.grow() {
		// Picks this second: area x 15 / 1024, the remainder rolled.
		raw("scoreboard players operation #spr.k $T = #spr.area $T")
		raw("scoreboard players set #spr.c $T 15")
		raw("scoreboard players operation #spr.k $T *= #spr.c $T")
		raw("scoreboard players operation #spr.rem $T = #spr.k $T")
		raw("scoreboard players set #spr.c $T 1024")
		raw("scoreboard players operation #spr.rem $T %= #spr.c $T")
		raw("scoreboard players operation #spr.k $T /= #spr.c $T")
		raw("execute store result score #spr.roll $T run random value 0..1023")
		raw("execute if score #spr.roll $T < #spr.rem $T run scoreboard players add #spr.k $T 1")
		raw("execute if score #spr.k $T matches ..0 run return 0")

		// The most this second can cost: every pick a growth.
		raw("scoreboard players set #spr.need $T $XP_ML_PER_GROWTH")
		raw("scoreboard players operation #spr.need $T *= #spr.k $T")
		raw("execute store result score #spr.ok $T run function ${find("experience").asId()}")
		raw("execute if score #spr.ok $T matches 0 run return run data modify entity @s data.status.growth set value \"off: no liquid experience\"")

		raw("scoreboard players set #spr.grown $T 0")
		raw("function ${pick.asId()} with $S")
		raw("data modify entity @s data.status.growth set value \"4x\"")
		// Only what grew is paid for; the network was checked for every pick, so this is there.
		val grew = "execute if score #spr.grown $T matches 1.."
		raw("$grew run scoreboard players set #spr.need $T $XP_ML_PER_GROWTH")
		raw("$grew run scoreboard players operation #spr.need $T *= #spr.grown $T")
		payClaimed("experience", grew)
		release()
	}

	/** At one random column: the crop one above, level with or one below the sprinkler, first found. */
	private fun Function.column() {
		raw("scoreboard players set #spr.g $T 0")
		for (dy in listOf(1, 0, -1)) {
			raw("execute if score #spr.g $T matches 0 positioned ~ ~$dy ~ if block ~ ~ ~ $CROPS_TAG store result score #spr.g $T run function ${growHere.asId()}")
			raw("execute if score #spr.g $T matches 1 positioned ~ ~$dy ~ run particle minecraft:happy_villager ~ ~0.4 ~ 0.25 0.2 0.25 0 4 normal ${particleViewersSel()}")
			raw("execute if score #spr.g $T matches 1 run scoreboard players set #spr.g $T 2")
		}
		raw("execute if score #spr.g $T matches 2 run scoreboard players add #spr.grown $T 1")
	}
}
