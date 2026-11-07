package ra.modules.wires

import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.arguments.components.item.itemModel
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.requirements
import io.github.ayfri.kore.features.advancements.rewards
import io.github.ayfri.kore.features.advancements.recipes
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.advancements.triggers.recipeUnlocked
import io.github.ayfri.kore.features.itemmodifiers.functions.setCount
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingShaped
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.TagRoute
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.ref
import ra.modules.wires.blocks.Placement
import ra.modules.wires.blocks.creative_fluid.CreativeFluid
import ra.modules.wires.blocks.electric_consumer.ElectricConsumer
import ra.modules.wires.blocks.electric_furnace.ElectricFurnaceSettings
import ra.modules.wires.blocks.liquid_drain.LiquidDrain

/**
 * Every Wires item, described once: its egg, recipe, unlock advancement and place in the bundle.
 *
 * Two ids per block. [WiresItem.block] `id` is the ITEM id (`custom_data`, `ra.place.<id>`, recipe
 * name); [WiresItem.marker] is the marker type the shared handler places (`ra.custom_block.<marker>`,
 * `blocks/<marker>/goggles`). They differ only for the pipe and the wire, whose items kept their
 * tier names (`liquid_pipe_copper`, `electric_wire_copper`) after the tiers merged.
 */
object WiresBlocks : PackPart {
	/** How a recipe's unlock advancement names its ingredient criterion, as each file had it. */
	enum class Unlock { INGREDIENT, MATERIAL, NAMED }

	data class WiresRecipe(
		val pattern: List<String>,
		val keys: List<Pair<String, ItemArgument>>,
		val unlockedBy: ItemArgument,
		val unlock: Unlock,
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
			recipe = WiresRecipe(listOf("CCC", "   ", "CCC"), listOf("C" to Items.COPPER_INGOT), Items.COPPER_INGOT, Unlock.MATERIAL, count = 16)),
		WiresItem(item("liquid_tank", "Liquid Tank", "minecraft:waxed_copper_block", "minecraft:waxed_copper_block"),
			recipe = WiresRecipe(listOf("C C", "CBC", "C C"), listOf("C" to Items.COPPER_INGOT, "B" to Items.BARREL), Items.BARREL, Unlock.MATERIAL)),
		WiresItem(item("liquid_pump", "Liquid Pump", "minecraft:dispenser", "minecraft:dispenser"),
			recipe = WiresRecipe(listOf("IRI", "PDP", "IBI"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "P" to Items.COPPER_INGOT, "D" to Items.DISPENSER, "B" to Items.BUCKET), Items.DISPENSER, Unlock.MATERIAL)),
		WiresItem(item("liquid_valve", "Liquid Valve", "minecraft:waxed_cut_copper", "minecraft:waxed_cut_copper"),
			recipe = WiresRecipe(listOf(" C ", "RLR", " C "), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.LEVER), Items.LEVER, Unlock.MATERIAL)),
		WiresItem(item("liquid_drain", "Liquid Drain", "minecraft:dropper", "minecraft:dropper"),
			recipe = WiresRecipe(listOf("ICI", "RDR", "IBI"), listOf("I" to Items.IRON_INGOT, "C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "D" to Items.DROPPER, "B" to Items.BUCKET), Items.DROPPER, Unlock.MATERIAL)),
		WiresItem(item("gas_tank", "Gas Tank", "minecraft:iron_block", "minecraft:iron_block"),
			recipe = WiresRecipe(listOf("I I", "IBI", "I I"), listOf("I" to Items.IRON_INGOT, "B" to Items.BARREL), Items.BARREL, Unlock.MATERIAL)),
		WiresItem(item("gas_pump", "Gas Pump", "minecraft:smoker", "minecraft:smoker"),
			recipe = WiresRecipe(listOf("IRI", "PSP", "IBI"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "P" to Items.COPPER_INGOT, "S" to Items.SMOKER, "B" to Items.GLASS_BOTTLE), Items.SMOKER, Unlock.MATERIAL)),
		WiresItem(item("gas_valve", "Gas Valve", "minecraft:smooth_basalt", "minecraft:smooth_basalt"),
			recipe = WiresRecipe(listOf(" C ", "ILI", " C "), listOf("I" to Items.IRON_INGOT, "C" to Items.COBBLESTONE, "L" to Items.LEVER), Items.SMOOTH_BASALT, Unlock.MATERIAL)),
		WiresItem(item("electric_wire_copper", "Wire", "minecraft:orange_candle", "minecraft:conduit", "electric_wire"), marker = "electric_wire",
			recipe = WiresRecipe(listOf("CCC"), listOf("C" to Items.COPPER_INGOT), Items.BLACK_DYE, Unlock.MATERIAL, count = 8)),
		// Creative only -- no recipe, on purpose. The bundle and items/give_creative are the only ways to get one.
		WiresItem(item("creative_eu", "Creative EU Source", "minecraft:beacon", "minecraft:beacon"), epic = true, lore = listOf(CREATIVE to "light_purple")),
		WiresItem(item("creative_fluid", "Creative Fluid Source", "minecraft:beacon", "minecraft:beacon"), epic = true, lore = listOf(CREATIVE to "light_purple")),
		WiresItem(item("electric_furnace", "Electric Furnace", "minecraft:blast_furnace", "minecraft:barrel"),
			lore = listOf("Smelts with EU - no fuel" to "gray", "Top row in, rows 2-3 out" to "gray"),
			recipe = WiresRecipe(listOf("III", "CFC", "IRI"), listOf("I" to Items.IRON_INGOT, "C" to Items.COPPER_INGOT, "F" to Items.BLAST_FURNACE, "R" to Items.REDSTONE_BLOCK), Items.BLAST_FURNACE, Unlock.NAMED, count = null)),
		WiresItem(item("electric_generator", "EU Generator", "minecraft:blast_furnace", "minecraft:barrel"),
			recipe = WiresRecipe(listOf("IRI", "CBC", "RFR"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "C" to Items.COPPER_INGOT, "B" to Items.BARREL, "F" to Items.FURNACE), Items.BARREL, Unlock.MATERIAL)),
		WiresItem(item("electric_consumer", "EU Consumer", "minecraft:observer", "minecraft:observer"),
			recipe = WiresRecipe(listOf("IRI", "COC", "RCR"), listOf("I" to Items.IRON_INGOT, "R" to Items.REDSTONE, "C" to Items.COPPER_INGOT, "O" to Items.OBSERVER), Items.OBSERVER, Unlock.MATERIAL)),
		WiresItem(item("electric_switch", "EU Switch", "minecraft:redstone_lamp", "minecraft:waxed_cut_copper"),
			recipe = WiresRecipe(listOf(" C ", "RLR", "CLC"), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.LEVER), Items.REDSTONE_LAMP, Unlock.MATERIAL)),
		WiresItem(item("battery", "Battery", "minecraft:waxed_copper_grate", "minecraft:waxed_copper_grate"),
			lore = listOf("Stores 10000 EU for its grid" to "gray"),
			recipe = WiresRecipe(listOf("CRC", "RBR", "CRC"), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE_BLOCK, "B" to Items.WAXED_COPPER_GRATE), Items.COPPER_INGOT, Unlock.INGREDIENT)),
		WiresItem(item("electric_breaker", "EU Breaker", "minecraft:waxed_copper_bulb", "minecraft:waxed_copper_bulb"),
			lore = listOf("Powered: moves EU from the grid behind" to "gray", "into the grid in front. Never merges them." to "gray"),
			recipe = WiresRecipe(listOf(" R ", "CLC", " R "), listOf("C" to Items.COPPER_INGOT, "R" to Items.REDSTONE, "L" to Items.WAXED_COPPER_BULB), Items.COPPER_INGOT, Unlock.INGREDIENT)),
		WiresItem(item("industrial_light", "Industrial Light", "minecraft:sea_lantern", "minecraft:sea_lantern"),
			lore = listOf("Redstone + EU: a 10 block beam of light" to "gray"),
			recipe = WiresRecipe(listOf("CGC", "GLG", "CRC"), listOf("C" to Items.COPPER_INGOT, "G" to Items.GLASS, "L" to Items.SEA_LANTERN, "R" to Items.REDSTONE), Items.GLOWSTONE_DUST, Unlock.INGREDIENT)),
		WiresItem(item("boiler", "Boiler", "minecraft:furnace", "minecraft:furnace"),
			lore = listOf("Water in, steam out, over a heat source" to "gray"),
			recipe = WiresRecipe(listOf("III", "IFI", "III"), listOf("I" to Items.IRON_INGOT, "F" to Items.FURNACE), Items.FURNACE, Unlock.INGREDIENT)),
		WiresItem(item("solar_panel", "Solar Panel", "minecraft:daylight_detector", "minecraft:daylight_detector"),
			lore = listOf("Generates EU from sunlight" to "gray"),
			recipe = WiresRecipe(listOf("GGG", "QQQ", "CRC"), listOf("G" to Items.GLASS, "Q" to Items.QUARTZ, "C" to Items.COPPER_INGOT, "R" to Items.REDSTONE), Items.DAYLIGHT_DETECTOR, Unlock.INGREDIENT)),
		WiresItem(item("liquid_filter", "Liquid Filter", "minecraft:waxed_exposed_cut_copper", "minecraft:waxed_exposed_cut_copper"),
			lore = listOf("Passes only the medium it is set to" to "gray", "Set filter_medium with the Data Handler" to "dark_gray"),
			recipe = WiresRecipe(listOf("CIC", "CHC", "CIC"), listOf("C" to Items.COPPER_INGOT, "I" to Items.IRON_INGOT, "H" to Items.HOPPER), Items.HOPPER, Unlock.MATERIAL, notify = false)),
	)

	/** Goggles readouts, in the order `ra:tools/goggles/draw_block` listed the module's markers. */
	private val gogglesOrder = listOf(
		"liquid_pipe", "liquid_tank", "liquid_pump", "liquid_valve", "liquid_filter", "liquid_drain",
		"gas_tank", "gas_pump", "gas_valve", "boiler", "electric_wire", "electric_generator",
		"electric_consumer", "electric_switch", "solar_panel", "battery", "creative_eu",
		"creative_fluid", "electric_furnace", "industrial_light", "electric_breaker",
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

		wrenchCycle("creative_fluid", WrenchCycle("Medium", "medium", CreativeFluid.cycleMedium))
		wrenchCycle("electric_consumer", WrenchCycle("Rate", "cooldown", ElectricConsumer.cycleRate))
		wrenchCycle("electric_furnace",
			WrenchCycle("Output", "output", ElectricFurnaceSettings.cycleOutput),
			WrenchCycle("Power", "mode", ElectricFurnaceSettings.cycleMode))
		wrenchCycle("liquid_drain",
			WrenchCycle("Mode", "mode", LiquidDrain.cycleMode),
			WrenchCycle("Rate", "cooldown", LiquidDrain.cycleRate))

		// The block's own working numbers: shown, never edited (see ra:tools/readonly).
		readonly("electric_breaker", "rate")
		readonly("electric_consumer", "eu_use")
		readonly("electric_generator", "generation_rate")
		readonly("gas_valve", "rate")
		readonly("industrial_light", "eu_use")
		readonly("liquid_filter", "rate")
		readonly("liquid_valve", "rate")
		readonly("solar_panel", "generation_rate")

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
		val recipe = pack.recipesBuilder.craftingShaped(b.id, b.ns.id) {
			category = RecipeCategory.REDSTONE
			if (r.notify) showNotification = true
			pattern(*r.pattern.toTypedArray())
			for ((k, item) in r.keys) key(k, item)
			result(Items.BAT_SPAWN_EGG, r.count) {
				itemModel(b.model.substringAfter(':'), "minecraft")
				// CustomComponent (raw NBT): Kore's typed item_name / custom_data / entity_data throw in JSON.
				this["item_name"] = CustomComponent(NbtString(b.name))
				if (it.lore.isNotEmpty()) this["lore"] = CustomComponent(loreNbt(it.lore))
				this["custom_data"] = CustomComponent(nbt { this["ra"] = nbt { this[b.id] = true } })
				this["entity_data"] = CustomComponent(b.entityDataNbt())
			}
		}
		// Each unlock file named its ingredient criterion its own way; the names are kept, since a
		// criterion name is how an advancement is granted or revoked by command.
		val trigger = when (r.unlock) {
			Unlock.INGREDIENT -> "has_ingredient"
			Unlock.MATERIAL -> "has_material"
			Unlock.NAMED -> "has_${r.unlockedBy.asId().substringAfter(':')}"
		}
		pack.advancement("recipes/unlock_${b.id}") {
			namespace = b.ns.id
			parent = AdvancementArgument("recipes/root", "minecraft")
			criteria {
				recipeUnlocked("has_the_recipe", recipe)
				inventoryChanged(trigger) { item { items = listOf(r.unlockedBy) } }
			}
			requirements(listOf("has_the_recipe", trigger))
			rewards { recipes(recipe) }
		}
	}
}
