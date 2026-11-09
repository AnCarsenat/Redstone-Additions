package ra.modules.wires

import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.arguments.components.item.itemModel
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.itemmodifiers.functions.setCount
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Tag
import ra.core.TagRoute
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.RecipeResult
import ra.core.block.Unlock
import ra.core.block.shapedRecipe
import ra.core.ref
import ra.modules.wires.blocks.Placement
import ra.modules.wires.blocks.creative_fluid.CreativeFluid
import ra.modules.wires.blocks.electric_consumer.ElectricConsumer
import ra.modules.wires.blocks.electric_furnace.ElectricFurnaceSettings
import ra.modules.wires.fluid.Drain

/**
 * Every Wires item, described once: its egg, recipe, unlock advancement and place in the bundle.
 *
 * Two ids per block. [WiresItem.block] `id` is the ITEM id (`custom_data`, `ra.place.<id>`, recipe
 * name); [WiresItem.marker] is the marker type the shared handler places (`ra.custom_block.<marker>`,
 * `blocks/<marker>/goggles`). They differ only for the pipe and the wire, whose items kept their
 * tier names (`liquid_pipe_copper`, `electric_wire_copper`) after the tiers merged.
 */
object WiresBlocks : PackPart {
	/**
	 * How a recipe's unlock advancement names its ingredient criterion, as each file had it.
	 * Kept, since a criterion name is how an advancement is granted or revoked by command.
	 */
	enum class Criterion { INGREDIENT, MATERIAL, NAMED }

	data class WiresRecipe(
		val pattern: List<String>,
		val keys: List<Pair<String, ItemArgument>>,
		val unlockedBy: ItemArgument,
		val criterion: Criterion,
		val count: Short? = 1,
		val notify: Boolean = true,
	)

	data class WiresItem(
		val block: CustomBlock,
		val marker: String = block.id,
		val lore: List<Pair<String, String>> = emptyList(),
		val epic: Boolean = false,
		val recipe: WiresRecipe? = null,
	)

	private fun item(id: String, name: String, model: String, physical: String, marker: String = id) =
		CustomBlock(Ns.WIRES, id, name, model, physical, dir = marker)

	private const val CREATIVE = "Creative: makes something from nothing"

	/** In bundle order: the order `give_wires_bundle` packed them. */
	val items = listOf(
		WiresItem(item("liquid_pipe_copper", "Copper Pipe", "minecraft:copper_block", "minecraft:conduit", "liquid_pipe"), marker = "liquid_pipe",
			recipe = WiresRecipe(listOf("CCC", "   ", "CCC"), listOf("C" to Items.COPPER_INGOT), Items.COPPER_INGOT, Criterion.MATERIAL, count = 16)),
		WiresItem(item("liquid_tank", "Liquid Tank", "minecraft:waxed_copper_block", "minecraft:waxed_copper_block"),
			recipe = WiresRecipe(listOf("C C", "CBC", "C C"), listOf("C" to Items.COPPER_INGOT, "B" to Items.BARREL), Items.BARREL, Criterion.MATERIAL)),
		WiresItem(item("liquid_pump", "Liquid Pump", "minecraft:dispenser", "minecraft:dispenser"),
			recipe = WiresRecipe(listOf("IRI", "PDP", "IBI"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "P" to Items.COPPER_INGOT, "D" to Items.DISPENSER, "B" to Items.BUCKET), Items.DISPENSER, Criterion.MATERIAL)),
		WiresItem(item("liquid_valve", "Liquid Valve", "minecraft:waxed_cut_copper", "minecraft:waxed_cut_copper"),
			recipe = WiresRecipe(listOf(" C ", "RLR", " C "), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.LEVER), Items.LEVER, Criterion.MATERIAL)),
		WiresItem(item("liquid_drain", "Fluid Drain", "minecraft:dropper", "minecraft:dropper"),
			lore = listOf("Takes fluid in; redstone lets it out" to "gray", "Gases are vented with their effect" to "gray"),
			recipe = WiresRecipe(listOf("ICI", "RDR", "IBI"), listOf("I" to Items.IRON_INGOT, "C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "D" to Items.DROPPER, "B" to Items.BUCKET), Items.DROPPER, Criterion.MATERIAL)),
		WiresItem(item("gas_tank", "Gas Tank", "minecraft:iron_block", "minecraft:iron_block"),
			recipe = WiresRecipe(listOf("I I", "IBI", "I I"), listOf("I" to Items.IRON_INGOT, "B" to Items.BARREL), Items.BARREL, Criterion.MATERIAL)),
		WiresItem(item("gas_pump", "Gas Pump", "minecraft:smoker", "minecraft:smoker"),
			recipe = WiresRecipe(listOf("IRI", "PSP", "IBI"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "P" to Items.COPPER_INGOT, "S" to Items.SMOKER, "B" to Items.GLASS_BOTTLE), Items.SMOKER, Criterion.MATERIAL)),
		WiresItem(item("gas_valve", "Gas Valve", "minecraft:smooth_basalt", "minecraft:smooth_basalt"),
			recipe = WiresRecipe(listOf(" C ", "ILI", " C "), listOf("I" to Items.IRON_INGOT, "C" to Items.COBBLESTONE, "L" to Items.LEVER), Items.SMOOTH_BASALT, Criterion.MATERIAL)),
		WiresItem(item("electric_wire_copper", "Wire", "minecraft:orange_candle", "minecraft:conduit", "electric_wire"), marker = "electric_wire",
			recipe = WiresRecipe(listOf("CCC"), listOf("C" to Items.COPPER_INGOT), Items.BLACK_DYE, Criterion.MATERIAL, count = 8)),
		// Creative only -- no recipe, on purpose. The bundle and items/give_creative are the only ways to get one.
		WiresItem(item("creative_eu", "Creative EU Source", "minecraft:beacon", "minecraft:beacon"), epic = true, lore = listOf(CREATIVE to "light_purple")),
		WiresItem(item("creative_fluid", "Creative Fluid Source", "minecraft:beacon", "minecraft:beacon"), epic = true, lore = listOf(CREATIVE to "light_purple")),
		WiresItem(item("electric_furnace", "Electric Furnace", "minecraft:blast_furnace", "minecraft:barrel"),
			lore = listOf("Smelts with EU - no fuel" to "gray", "Top row in, rows 2-3 out" to "gray"),
			recipe = WiresRecipe(listOf("III", "CFC", "IRI"), listOf("I" to Items.IRON_INGOT, "C" to Items.COPPER_INGOT, "F" to Items.BLAST_FURNACE, "R" to Items.REDSTONE_BLOCK), Items.BLAST_FURNACE, Criterion.NAMED, count = null)),
		WiresItem(item("electric_generator", "EU Generator", "minecraft:blast_furnace", "minecraft:barrel"),
			recipe = WiresRecipe(listOf("IRI", "CBC", "RFR"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "C" to Items.COPPER_INGOT, "B" to Items.BARREL, "F" to Items.FURNACE), Items.BARREL, Criterion.MATERIAL)),
		WiresItem(item("electric_consumer", "EU Consumer", "minecraft:observer", "minecraft:observer"),
			recipe = WiresRecipe(listOf("IRI", "COC", "RCR"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "C" to Items.COPPER_INGOT, "O" to Items.OBSERVER), Items.OBSERVER, Criterion.MATERIAL)),
		WiresItem(item("electric_switch", "EU Switch", "minecraft:redstone_lamp", "minecraft:waxed_cut_copper"),
			recipe = WiresRecipe(listOf(" C ", "RLR", "CLC"), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.LEVER), Items.REDSTONE_LAMP, Criterion.MATERIAL)),
		WiresItem(item("battery", "Battery", "minecraft:waxed_copper_grate", "minecraft:waxed_copper_grate"),
			lore = listOf("Stores 10000 EU for its grid" to "gray"),
			recipe = WiresRecipe(listOf("CRC", "RBR", "CRC"), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE_BLOCK, "B" to Items.WAXED_COPPER_GRATE), Items.COPPER_INGOT, Criterion.INGREDIENT)),
		WiresItem(item("electric_breaker", "EU Breaker", "minecraft:waxed_copper_bulb", "minecraft:waxed_copper_bulb"),
			lore = listOf("Powered: moves EU from the grid behind" to "gray", "into the grid in front. Never merges them." to "gray"),
			recipe = WiresRecipe(listOf(" R ", "CLC", " R "), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.WAXED_COPPER_BULB), Items.COPPER_INGOT, Criterion.INGREDIENT)),
		WiresItem(item("industrial_light", "Industrial Light", "minecraft:sea_lantern", "minecraft:sea_lantern"),
			lore = listOf("Redstone + EU: a 10 block beam of light" to "gray"),
			recipe = WiresRecipe(listOf("CGC", "GLG", "CRC"), listOf("C" to Items.COPPER_INGOT, "G" to Items.GLASS, "L" to Items.SEA_LANTERN, "R" to Items.REDSTONE), Items.GLOWSTONE_DUST, Criterion.INGREDIENT)),
		WiresItem(item("boiler", "Boiler", "minecraft:furnace", "minecraft:furnace"),
			lore = listOf("Water in, steam out, over a heat source" to "gray"),
			recipe = WiresRecipe(listOf("III", "IFI", "III"), listOf("I" to Items.IRON_INGOT, "F" to Items.FURNACE), Items.FURNACE, Criterion.INGREDIENT)),
		WiresItem(item("solar_panel", "Solar Panel", "minecraft:daylight_detector", "minecraft:daylight_detector"),
			lore = listOf("Generates EU from sunlight" to "gray"),
			recipe = WiresRecipe(listOf("GGG", "QQQ", "CRC"), listOf("G" to Items.GLASS, "Q" to Items.QUARTZ, "C" to Items.COPPER_INGOT, "R" to Items.REDSTONE), Items.DAYLIGHT_DETECTOR, Criterion.INGREDIENT)),
		WiresItem(item("liquid_filter", "Liquid Filter", "minecraft:waxed_exposed_cut_copper", "minecraft:waxed_exposed_cut_copper"),
			lore = listOf("Passes only the medium it is set to" to "gray", "Set filter_medium with the Data Handler" to "dark_gray"),
			recipe = WiresRecipe(listOf("CIC", "CHC", "CIC"), listOf("C" to Items.COPPER_INGOT, "I" to Items.IRON_INGOT, "H" to Items.HOPPER), Items.HOPPER, Criterion.MATERIAL, notify = false)),
		WiresItem(item("network_gauge", "Network Gauge", "minecraft:comparator", "minecraft:target"),
			lore = listOf("Redstone 0-15 from how full" to "gray", "the network behind it is" to "gray"),
			recipe = WiresRecipe(listOf("CGC", "RKR", "CTC"), listOf("C" to Items.COPPER_INGOT, "G" to Items.GLASS, "R" to Items.REDSTONE, "K" to Items.COMPARATOR, "T" to Items.TARGET), Items.COMPARATOR, Criterion.INGREDIENT)),
		WiresItem(item("usage_meter", "Usage Meter", "minecraft:clock", "minecraft:target"),
			lore = listOf("A redstone pulse every N units used" to "gray", "or made on the network behind it" to "gray"),
			recipe = WiresRecipe(listOf("CGC", "RKR", "CTC"), listOf("C" to Items.COPPER_INGOT, "G" to Items.GLASS, "R" to Items.REDSTONE, "K" to Items.CLOCK, "T" to Items.TARGET), Items.CLOCK, Criterion.INGREDIENT)),
		WiresItem(item("tesla_coil", "Tesla Coil", "minecraft:lightning_rod", "minecraft:waxed_lightning_rod"),
			lore = listOf("Redstone + EU: zaps hostile mobs" to "gray", "around it once a second" to "gray"),
			recipe = WiresRecipe(listOf("CLC", "RBR", "CIC"), listOf("C" to Items.COPPER_INGOT, "L" to Items.LIGHTNING_ROD, "R" to Items.REDSTONE, "B" to Items.REDSTONE_BLOCK, "I" to Items.IRON_INGOT), Items.LIGHTNING_ROD, Criterion.INGREDIENT)),
		WiresItem(item("spout", "Spout", "minecraft:waxed_exposed_copper_grate", "minecraft:waxed_exposed_copper_grate"),
			lore = listOf("Fills buckets and bottles under it" to "gray", "from its fluid network" to "gray"),
			recipe = WiresRecipe(listOf("C C", "CBC", " D "), listOf("C" to Items.COPPER_INGOT, "B" to Items.BUCKET, "D" to Items.POINTED_DRIPSTONE), Items.BUCKET, Criterion.INGREDIENT)),
		WiresItem(item("water_wheel", "Water Wheel", "minecraft:stripped_spruce_log", "minecraft:stripped_spruce_log"),
			lore = listOf("Generates EU from flowing water" to "gray", "around its paddles" to "gray"),
			recipe = WiresRecipe(listOf("PSP", "SCS", "PRP"), listOf("P" to Items.SPRUCE_SLAB, "S" to Items.STICK, "C" to Items.COPPER_BLOCK, "R" to Items.REDSTONE), Items.SPRUCE_SLAB, Criterion.INGREDIENT)),
	)

	/** Goggles readouts, in the order `ra:tools/goggles/draw_block` listed the module's markers. */
	private val gogglesOrder = listOf(
		"liquid_pipe", "liquid_tank", "liquid_pump", "liquid_valve", "liquid_filter", "liquid_drain",
		"gas_tank", "gas_pump", "gas_valve", "boiler", "electric_wire", "electric_generator",
		"electric_consumer", "electric_switch", "solar_panel", "battery", "creative_eu",
		"creative_fluid", "electric_furnace", "industrial_light", "electric_breaker", "network_gauge", "usage_meter",
		"tesla_coil", "spout", "water_wheel",
	)

	private fun loreNbt(lore: List<Pair<String, String>>) = NbtList(lore.map { (text, color) ->
		nbt { this["text"] = text; this["color"] = color; this["italic"] = false }
	})

	/** The egg as an item stack, exactly as `give_wires_bundle` spelled it (rarity and lore included). */
	fun WiresItem.stackNbt(): NbtCompound = nbt {
		this["id"] = "minecraft:bat_spawn_egg"
		this["count"] = 1
		this["components"] = nbt {
			this["minecraft:item_model"] = block.model
			this["minecraft:item_name"] = block.name
			if (epic) this["minecraft:rarity"] = "epic"
			if (lore.isNotEmpty()) this["minecraft:lore"] = loreNbt(lore)
			this["minecraft:custom_data"] = nbt { this["ra"] = nbt { this[block.id] = true } }
			this["minecraft:entity_data"] = block.entityDataNbt()
		}
	}

	override fun Ra.register() {
		// One shared placement handler for every Wires block; see blocks/handle_placement.
		placementHandlers += Placement.handlePlacement
		for (it in items) block(it.block, placement = false, goggles = false)
		for (marker in gogglesOrder) goggles += TagRoute("ra.custom_block.$marker", ref(Ns.WIRES, "blocks/$marker/goggles"))

		for (it in items) it.recipe?.let { r -> recipe(it, r) }

		bundles += GiveBundle("wires", "Wires Bundle", "Fluid and electric network blocks", Ns.WIRES, items.map { it.stackNbt() })

		// The retired pipe and wire tiers (see SPECS) place a Copper Pipe or a Wire; a refused
		// placement gives that back.
		for (old in listOf("liquid_pipe_netherite", "gas_pipe_copper", "gas_pipe_netherite"))
			retiredPlaceTags[Tag.place(old)] = Tag.place("liquid_pipe_copper")
		retiredPlaceTags[Tag.place("electric_wire_netherite")] = Tag.place("electric_wire_copper")

		wrenchCycle("creative_fluid", WrenchCycle("Medium", "medium", CreativeFluid.cycleMedium))
		wrenchCycle("electric_consumer", WrenchCycle("Draw", "eu_use", ElectricConsumer.cycleRate))
		wrenchCycle("electric_furnace",
			WrenchCycle("Output", "output", ElectricFurnaceSettings.cycleOutput),
			WrenchCycle("Power", "mode", ElectricFurnaceSettings.cycleMode))

		// Property schemas, by marker type (what data.type holds -- liquid_pipe, not
		// liquid_pipe_copper). `readonly` ones are the block's own working numbers: shown by the Data
		// Handler, never edited there or cycled by the wrench.
		for (t in listOf("liquid_pipe", "liquid_tank", "gas_tank", "liquid_pump", "gas_pump", "electric_wire", "electric_switch", "battery", "creative_eu", "boiler", "network_gauge"))
			properties(t) {}
		for (t in listOf("liquid_valve", "gas_valve", "electric_breaker"))
			properties(t) { number("rate", min = 1, max = 1000000, readonly = true) }
		properties("liquid_filter") {
			number("rate", min = 1, max = 1000000, readonly = true)
			text("filter_medium")
		}
		// Rate (mL/s), for taking in and pouring out alike: one 5000 mL action every 100000 / rate
		// ticks (5000 = one source block a second). An admin knob, kept off the Goggles; the role
		// (take in / pour out) is redstone.
		properties("liquid_drain") {
			number("rate", min = Drain.MIN_RATE, max = Drain.MAX_RATE, adminOnly = true, goggles = false)
			// Pre-6.0.2 name, removed by the 6.0.2 migration (ra_migrations:6.0.1-to-6.0.2/liquid_drain).
			number("speed", readonly = true, visible = false)
			// Pre-6.0.1 names, removed by the 6.0.1 migration (ra_migrations:6.0.0-to-6.0.1/liquid_drain).
			number("cooldown", readonly = true, visible = false)
			number("interval", readonly = true, visible = false)
			text("mode", readonly = true, visible = false)
		}
		properties("electric_generator") { number("generation_rate", min = 1, max = 10000, readonly = true) }
		// No properties: a Solar Panel's output comes from the daylight detector's power level
		// (blocks/solar_panel/tick), never from a generation_rate.
		properties("solar_panel") {}
		// eu_use is the consumer's setting, not a fixed rate: the wrench cycles it 20 / 40 / 80.
		properties("electric_consumer") { number("eu_use", min = 1, max = 10000) }
		properties("industrial_light") { number("eu_use", min = 0, max = 1000, readonly = true) }
		properties("creative_fluid") {
			text("medium")
			number("rate", min = 0, max = 1000000)
		}
		properties("electric_furnace") {
			text("mode")
			text("output")
		}

		// Removes one item from a stack in place (`item modify ... ra_wires:take_one`).
		pack.itemModifier("take_one") {
			namespace = Ns.WIRES.id
			setCount(-1, add = true)
		}

		// What a Boiler counts as fire underneath it.
		pack.blockTag("heat_sources", Ns.WIRES.id) {
			add(Blocks.LAVA); add(Blocks.FIRE); add(Blocks.SOUL_FIRE); add(Blocks.MAGMA_BLOCK)
			add(Blocks.CAMPFIRE); add(Blocks.SOUL_CAMPFIRE); add(Blocks.LAVA_CAULDRON)
		}
	}

	private fun Ra.recipe(it: WiresItem, r: WiresRecipe) {
		val b = it.block
		val result = RecipeResult(Items.BAT_SPAWN_EGG) {
			itemModel(b.model.substringAfter(':'), "minecraft")
			// CustomComponent (raw NBT): Kore's typed item_name / custom_data / entity_data throw in JSON.
			// Not blockItemComponents(): the lore goes between the name and custom_data.
			this["item_name"] = CustomComponent(NbtString(b.name))
			if (it.lore.isNotEmpty()) this["lore"] = CustomComponent(loreNbt(it.lore))
			this["custom_data"] = CustomComponent(nbt { this["ra"] = nbt { this[b.id] = true } })
			this["entity_data"] = CustomComponent(b.entityDataNbt())
		}
		// Each unlock file named its ingredient criterion its own way; the names are kept, since a
		// criterion name is how an advancement is granted or revoked by command.
		val unlock = when (r.criterion) {
			Criterion.INGREDIENT -> Unlock(r.unlockedBy, "has_ingredient")
			Criterion.MATERIAL -> Unlock(r.unlockedBy, "has_material")
			Criterion.NAMED -> Unlock(r.unlockedBy)
		}
		pack.shapedRecipe(b.ns, b.id, result, unlock, count = r.count, notify = r.notify) {
			pattern(*r.pattern.toTypedArray())
			for ((k, item) in r.keys) key(k, item)
		}
	}
}
