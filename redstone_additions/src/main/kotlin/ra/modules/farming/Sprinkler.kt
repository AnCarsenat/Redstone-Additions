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
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.ref
import ra.modules.interactive.specGive
import ra.modules.wires.machine.MachineClock
import ra.modules.farming.SprinklerPay.pay
import ra.modules.farming.SprinklerPay.refund
import ra.modules.farming.SprinklerPay.registerSprinklerPay
import ra.modules.farming.SprinklerPay.release

/**
 * Sprinkler: waters the farmland around it from a fluid network, and with liquid experience makes
 * the crops around it grow about four times as fast.
 *
 * NOT A NETWORK NODE. Like the Book Upgrader, it reaches into the network of a pipe, tank or drain
 * touching any of its six faces, so placing or breaking one never splits or joins a network. The
 * first neighbour whose network holds the whole price pays it ([SprinklerPay]).
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
 * EXPERIENCE. Once a second, three times vanilla's own growth, so a crop grows about 4x as fast.
 * Vanilla gives a block 3 random ticks per 4096 per game tick (60/4096 a second), and a random tick
 * grows a crop in a full, moist field (the water keeps it moist) 1 time in 6. Three times that is
 * `area x 30 / 4096 = area x 15 / 2048` growths a second, the remainder rolled: each picks a random
 * column of the area and moves the crop there up one age. On dry farmland vanilla is slower (1 in
 * 13), so the boost is larger there. The second's price ([XP_ML_PER_GROWTH] mL a pick) is TAKEN
 * FIRST and only the picks paid for are made; what a pick on dirt or a ripe crop did not use is
 * offered back to the same network.
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
			loreLine("Liquid experience: crops grow about 4x as fast", "gray"),
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
	/** An admin's, and the Farming settings default's ceiling: a 3267-block `fill` every 5 s, ~8 picks a second. */
	const val ADMIN_MAX_RADIUS = 16

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
		// One test per crop type, then only that crop's ages. Returns 1 when it grew.
		pack.define(growHere) {
			for ((id, _) in CROPS) raw("execute if block ~ ~ ~ minecraft:$id run return run function ${growCrop(id).asId()}")
			raw("return 0")
		}
		for ((id, last) in CROPS) pack.define(growCrop(id)) {
			for (age in 0 until last) {
				raw("execute if block ~ ~ ~ minecraft:$id[age=$age] run return run setblock ~ ~ ~ minecraft:$id[age=${age + 1}]")
			}
			raw("return 0")
		}

		registerSprinklerPay()

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.copper_grate.break", cloud = true)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.WAXED_OXIDIZED_COPPER_GRATE)) {}
	}

	private val column = block.fn("column")
	private fun growCrop(id: String) = ref(Ns.FARMING, "blocks/sprinkler/grow/$id")

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
		raw("execute store result score #spr.ok $T run function ${SprinklerPay.find("water").asId()}")
		raw("execute if score #spr.ok $T matches 0 run return run data modify entity @s data.status.water set value \"needs water\"")
		pay("water")
		// A short take (the network changed since the read) waters nothing and goes back.
		raw("scoreboard players set #spr.back $T 0")
		raw("execute if score #spr.paid $T < #spr.need $T run scoreboard players operation #spr.back $T = #spr.paid $T")
		refund("water")
		release()
		raw("execute if score #spr.paid $T < #spr.need $T run return run data modify entity @s data.status.water set value \"needs water\"")
		raw("function ${waterFill.asId()} with $S")
		raw("data modify entity @s data.status.water set value \"watering\"")
		raw("particle minecraft:splash ~ ~1 ~ 1.5 0.3 1.5 0 40 normal ${particleViewersSel()}")
		raw("playsound minecraft:weather.rain block ${soundListenersSel(16)} ~ ~ ~ 0.15 1.6")
	}

	/** As the marker, at it: the extra growth, paid in liquid experience. */
	private fun Function.grow() {
		// Picks this second: area x 15 / 2048, the remainder rolled.
		raw("scoreboard players operation #spr.k $T = #spr.area $T")
		raw("scoreboard players set #spr.c $T 15")
		raw("scoreboard players operation #spr.k $T *= #spr.c $T")
		raw("scoreboard players operation #spr.rem $T = #spr.k $T")
		raw("scoreboard players set #spr.c $T 2048")
		raw("scoreboard players operation #spr.rem $T %= #spr.c $T")
		raw("scoreboard players operation #spr.k $T /= #spr.c $T")
		raw("execute store result score #spr.roll $T run random value 0..2047")
		raw("execute if score #spr.roll $T < #spr.rem $T run scoreboard players add #spr.k $T 1")
		raw("execute if score #spr.k $T matches ..0 run return 0")

		// Every pick is paid for before it is made.
		raw("scoreboard players set #spr.c $T $XP_ML_PER_GROWTH")
		raw("scoreboard players operation #spr.need $T = #spr.k $T")
		raw("scoreboard players operation #spr.need $T *= #spr.c $T")
		raw("execute store result score #spr.ok $T run function ${SprinklerPay.find("experience").asId()}")
		raw("execute if score #spr.ok $T matches 0 run return run data modify entity @s data.status.growth set value \"off: no liquid experience\"")
		pay("experience")
		// Only as many picks as the take paid for; what paid for nothing goes back.
		raw("scoreboard players operation #spr.k $T = #spr.paid $T")
		raw("scoreboard players operation #spr.k $T /= #spr.c $T")
		raw("scoreboard players set #spr.grown $T 0")
		raw("execute if score #spr.k $T matches 1.. run function ${pick.asId()} with $S")
		raw("scoreboard players operation #spr.used $T = #spr.grown $T")
		raw("scoreboard players operation #spr.used $T *= #spr.c $T")
		raw("scoreboard players operation #spr.back $T = #spr.paid $T")
		raw("scoreboard players operation #spr.back $T -= #spr.used $T")
		refund("experience")
		release()
		raw("execute if score #spr.paid $T < #spr.c $T run return run data modify entity @s data.status.growth set value \"off: no liquid experience\"")
		raw("data modify entity @s data.status.growth set value \"about 4x\"")
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
