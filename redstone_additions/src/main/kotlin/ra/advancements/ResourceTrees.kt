package ra.advancements

import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.advancements.AdvancementFrameType
import io.github.ayfri.kore.generated.Items
import ra.advancements.Flag.ONE
import ra.advancements.Flag.TRUE

/** One Infinite Generator line: casing -> core (a goal, won on an enchanting table) -> generator. */
private data class GeneratorLine(
	val kind: String,
	val coreIcon: ItemArgument,
	/** The core is a placeholder item (a command block / jigsaw) recognised by its custom data. */
	val coreItem: ItemArgument,
	val coreDescription: String,
	val generatorIcon: ItemArgument,
	val generatorTitle: String,
)

private val GENERATORS = listOf(
	GeneratorLine("mineral", Items.DEEPSLATE_DIAMOND_ORE, Items.JIGSAW, "Sacrifice stone on an enchanting table until a core survives.", Items.CHISELED_DEEPSLATE, "Mineral Generator"),
	GeneratorLine("nether", Items.ANCIENT_DEBRIS, Items.STRUCTURE_BLOCK, "Sacrifice netherrack on an enchanting table until a core survives.", Items.CHISELED_POLISHED_BLACKSTONE, "Nether Generator"),
	GeneratorLine("poppy", Items.FLOWERING_AZALEA, Items.CHAIN_COMMAND_BLOCK, "Sacrifice poppies on an enchanting table until a core survives.", Items.POPPY, "Poppy Generator"),
)

private const val CASING = "ra_infinite/get_generator_casing"

/** These test with `predicates` (contains), unlike the older trees' exact `components`. */
internal val infiniteTree = buildList {
	val casing = Match.Partial(raData("generator_casing", TRUE))
	add(
		Unlock(
			"ra_infinite", "ra", Items.COPPER_GRATE, "Infinite Generators", "Build generators that grow their own material.",
			"get_casing", Items.REPEATING_COMMAND_BLOCK, casing, announce = null,
		),
	)
	add(
		Unlock(
			CASING, "ra_infinite", Items.COPPER_GRATE, "Generator Casing", "Craft a Generator Casing from copper grates and a netherite scrap.",
			"get_generator_casing", Items.REPEATING_COMMAND_BLOCK, casing,
		),
	)
	for (g in GENERATORS) {
		val core = "ra_infinite/get_${g.kind}_core"
		val coreTitle = g.kind.replaceFirstChar { it.uppercase() } + " Core"
		add(
			Unlock(
				core, CASING, g.coreIcon, coreTitle, g.coreDescription, "get_${g.kind}_core", g.coreItem,
				Match.Partial(raData("${g.kind}_core", TRUE)), frame = AdvancementFrameType.GOAL,
			),
		)
		add(
			Unlock(
				"ra_infinite/get_${g.kind}_generator", core, g.generatorIcon, g.generatorTitle, "Obtain the ${g.generatorTitle}.",
				"get_${g.kind}_generator", Items.BAT_SPAWN_EGG, Match.Partial(raData("${g.kind}_generator", TRUE)),
			),
		)
	}
}

/** The upgrade kits: [id] is the kit's `upgrade` value. */
private fun upgradeKit(id: String, title: String, icon: ItemArgument) = Unlock(
	"ra_jetpacks/get_${id}_kit", "ra_jetpacks", icon, title, "Obtain the $title.", "get_${id}_kit", Items.FIREWORK_STAR,
	Match.Exact(raData("jetpack_kit", ONE, "upgrade" to id)),
)

internal val jetpacksTree = listOf(
	Unlock(
		"ra_jetpacks", "ra", Items.ELYTRA, "Jetpacks", "Fit a jetpack to a chestplate and take off.",
		"get_kit", Items.FIREWORK_STAR, Match.Partial(raData("jetpack_kit", TRUE)), announce = null,
	),
	Unlock(
		"ra_jetpacks/get_iron_jetpack_kit", "ra_jetpacks", Items.IRON_CHESTPLATE, "Iron Jetpack Kit", "Craft the Iron Jetpack Kit.",
		"get_iron_jetpack_kit", Items.FIREWORK_STAR, Match.Partial(raData("jetpack_kit", TRUE, "tier" to "iron")),
	),
	Unlock(
		"ra_jetpacks/get_infinite_jetpack_kit", "ra_jetpacks/get_iron_jetpack_kit", Items.NETHERITE_CHESTPLATE, "No Refuelling",
		"Sacrifice an Iron Jetpack Kit on an enchanting table and win the infinite one.",
		"get_infinite_jetpack_kit", Items.FIREWORK_STAR, Match.Partial(raData("jetpack_kit", TRUE, "tier" to "infinite")),
		frame = AdvancementFrameType.GOAL,
	),
	upgradeKit("lift", "Lift Kit", Items.PHANTOM_MEMBRANE),
	upgradeKit("scorch", "Scorch Kit", Items.BLAZE_POWDER),
	upgradeKit("speed", "Thruster Kit", Items.FIREWORK_ROCKET),
)

internal val wiresTree = listOf(
	category("ra_wires", Items.CONDUIT, "Fluid and EU Networks", "Build merged fluid transport and electric infrastructure with RA Wires.", "get_wires_item", Items.COPPER_INGOT),
	egg("ra_wires", "battery", "Battery", Items.WAXED_COPPER_GRATE, ONE),
	egg("ra_wires", "boiler", "Boiler", Items.FURNACE, TRUE),
	egg("ra_wires", "electric_breaker", "EU Breaker", Items.WAXED_COPPER_BULB, ONE),
	egg("ra_wires", "electric_consumer", "EU Consumer", Items.OBSERVER, TRUE),
	egg("ra_wires", "electric_furnace", "Electric Furnace", Items.BLAST_FURNACE, ONE, "Obtain the Electric Furnace item."),
	egg("ra_wires", "electric_generator", "EU Generator", Items.BLAST_FURNACE, TRUE),
	egg("ra_wires", "electric_switch", "EU Switch", Items.REDSTONE_LAMP, TRUE),
	egg("ra_wires", "electric_wire_copper", "Wire", Items.ORANGE_CANDLE, TRUE),
	egg("ra_wires", "gas_pump", "Gas Pump", Items.SMOKER, TRUE),
	egg("ra_wires", "gas_tank", "Gas Tank", Items.IRON_BLOCK, TRUE),
	egg("ra_wires", "gas_valve", "Gas Valve", Items.SMOOTH_BASALT, TRUE),
	egg("ra_wires", "industrial_light", "Industrial Light", Items.SEA_LANTERN, ONE),
	egg("ra_wires", "liquid_drain", "Liquid Drain", Items.DROPPER, TRUE),
	egg("ra_wires", "liquid_filter", "Liquid Filter", Items.WAXED_EXPOSED_CUT_COPPER, ONE, "Obtain the Liquid Filter item."),
	egg("ra_wires", "liquid_pipe_copper", "Copper Pipe", Items.COPPER_BLOCK, TRUE),
	egg("ra_wires", "liquid_pump", "Liquid Pump", Items.DISPENSER, TRUE),
	egg("ra_wires", "liquid_tank", "Liquid Tank", Items.WAXED_COPPER_BLOCK, TRUE),
	egg("ra_wires", "liquid_valve", "Liquid Valve", Items.WAXED_CUT_COPPER, TRUE),
	egg("ra_wires", "network_gauge", "Network Gauge", Items.COMPARATOR, TRUE),
	egg("ra_wires", "solar_panel", "Solar Panel", Items.DAYLIGHT_DETECTOR, TRUE),
	egg("ra_wires", "spout", "Spout", Items.WAXED_EXPOSED_COPPER_GRATE, ONE),
	egg("ra_wires", "tesla_coil", "Tesla Coil", Items.LIGHTNING_ROD, ONE),
	egg("ra_wires", "usage_meter", "Usage Meter", Items.CLOCK, TRUE),
)
