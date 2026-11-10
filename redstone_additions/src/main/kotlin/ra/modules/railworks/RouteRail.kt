package ra.modules.railworks

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.breakCheck
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
import ra.core.block.stackedStatusLine
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.text
import ra.modules.interactive.specGive

/**
 * Route Rail: a block placed under a rail junction. When a minecart comes near, the rail on top
 * is set to `on_match` if the cart carries the `route`, else to `on_miss`.
 *
 * What "carries the route" means is `source`, one of [SOURCES]:
 *  - `name`: the cart's custom name (a name tag) is `route`;
 *  - `tag`: the cart has the entity tag `route` (a Tag Adder can give it);
 *  - `item`: a chest or hopper minecart holds an item whose id is `route` (`minecraft:coal`);
 *  - `rider`: someone rides the cart (`route` is not read).
 *
 * The shapes are rail `shape` states, one of [SHAPES] (`none` leaves the rail alone). Only a
 * plain `minecraft:rail` can curve, so only that rail is switched; its `waterlogged` state is kept.
 *
 * NO TEXT IS PASTED. `route` is compared with the cart's data by copying one over the other and
 * reading whether anything changed (`store success`), never put into a selector or a command.
 * The modes are matched against fixed strings. A value typed in the Data Handler that is none of
 * them does nothing. See [RouteRailScan] for when the check runs and what it costs.
 */
object RouteRail {
	val block = CustomBlock(Ns.RAILWORKS, "route_rail", "Route Rail", model = "minecraft:waxed_cut_copper")
	val item = block.loreItem { lore(loreLine("Place under a rail junction", "gray"), loreLine("Switches it for the carts on its route", "gray")) }
	val registerBlock = block.fn("register_block")

	val SOURCES = listOf("name", "tag", "item", "rider")
	val SHAPES = listOf("north_south", "east_west", "north_east", "north_west", "south_east", "south_west", "none")

	const val DEFAULT_ROUTE = "express"
	const val DEFAULT_MATCH = "north_east"
	const val DEFAULT_MISS = "north_south"

	private val cycleSource = block.fn("cycle_source")
	private val cycleMatch = block.fn("cycle_on_match")
	private val cycleMiss = block.fn("cycle_on_miss")

	private const val T = Obj.TEMP

	fun Ra.registerRouteRail() {
		block(block)
		wrenchCycle(
			block.id,
			WrenchCycle("Reads", "source", cycleSource),
			WrenchCycle("On match", "on_match", cycleMatch),
			WrenchCycle("Otherwise", "on_miss", cycleMiss),
		)
		properties(block) {
			// Only ever compared with the cart's data, so any text is fine.
			text("route", TextFormat.FREE, default = DEFAULT_ROUTE)
			text("source", default = SOURCES.first())
			text("on_match", default = DEFAULT_MATCH)
			text("on_miss", default = DEFAULT_MISS)
		}
		pack.blockRegisterMessage(block)
		pack.specGive(block, item)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.RAIL), count = null) {
			pattern("IRI", "INI", "IDI")
			key("I", Items.COPPER_INGOT)
			key("R", Items.RAIL)
			key("N", Items.NAME_TAG)
			key("D", Items.REDSTONE)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(
				block, "data.properties",
				"""{route:"$DEFAULT_ROUTE",source:"${SOURCES.first()}",on_match:"$DEFAULT_MATCH",on_miss:"$DEFAULT_MISS"}""",
			)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(cycleSource) { cycle("source", "Reads", SOURCES) }
		pack.define(cycleMatch) { cycle("on_match", "On match", SHAPES) }
		pack.define(cycleMiss) { cycle("on_miss", "Otherwise", SHAPES) }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			stackedPropLine("route", "Route: ", "yellow")
			stackedPropLine("source", "Reads: ", "aqua")
			stackedPropLine("on_match", "On match: ", "green")
			stackedPropLine("on_miss", "Otherwise: ", "gold")
			stackedStatusLine("last", "Last cart: ", "white")
		}

		RouteRailScan.register(this)

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = "minecraft:block.copper.break", cloud = true)
		pack.define(block.tick) {
			breakCheck(block, Blocks.WAXED_CUT_COPPER, onBreak)
			function(RouteRailScan.run)
		}
	}

	/** Wrench: the next of [values] in `data.properties.<prop>`, anything else to the first. As the marker. */
	private fun Function.cycle(prop: String, label: String, values: List<String>) {
		raw("scoreboard players set #rr.cy $T 0")
		values.forEachIndexed { i, v ->
			raw("execute if data entity @s data.properties{$prop:\"$v\"} run scoreboard players set #rr.cy $T ${i + 1}")
		}
		for (k in 0..values.size) {
			raw("execute if score #rr.cy $T matches $k run data modify entity @s data.properties.$prop set value \"${values[k % values.size]}\"")
		}
		raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name} ${label.lowercase()}: ",color:"gray"},{nbt:"data.properties.$prop",entity:"@s",color:"aqua"}]""")
		raw("execute at @s run playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
	}
}
