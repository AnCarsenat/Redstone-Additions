package ra.migrations

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Tag
import ra.core.raw

/** `@e[type=marker,tag=ra.custom_block.<type>]`. */
internal fun blockMarkers(type: String) = allEntities { this.type = EntityTypes.MARKER; tag = Tag.customBlock(type) }

/**
 * Give every entity this pack owns the plain `ra` tag.
 *
 * 5.1.8 tags everything it summons with `ra` from birth, so that `/kill @e[tag=ra]` sweeps up the
 * pack and nothing else. A world built on 5.1.7 or earlier is full of markers, skins and
 * billboards that predate the tag and would be left behind by exactly the command meant to clean
 * them up. Idempotent: adding a tag an entity already has does nothing.
 */
internal val m5_1_7 = migration("5.1.7", "5.1.8") { world {
	val owned = listOf(
		Tag.CUSTOM_BLOCK, Tag.MULTIBLOCK, Tag.DISPLAY, "ra.skin", Tag.BILLBOARD, Tag.SPAWNED,
		"ra.node", "ra.wires.node", "ra.wires.fluid_node", "ra.wires.electric_node",
	)
	for (t in owned) tag(allEntities { tag = t }) { add(Tag.RA) }
}}

/**
 * Every block type that existed in 5.1.8. A historical list, not the current registry: it is the
 * set of markers this migration may find, so it must not grow or shrink with the pack.
 */
private val TYPES_5_1_8 = listOf(
	"battery", "block_breaker", "block_placer", "boiler", "boxer", "breeder", "chunk_loader", "clock",
	"creative_eu", "creative_fluid", "delayer", "electric_breaker", "electric_consumer",
	"electric_furnace", "electric_generator", "electric_switch", "electric_wire", "emitter",
	"ender_fluid_vault", "ender_item_vault", "ender_power_vault", "entity_detector", "extender",
	"gas_pump", "gas_tank", "gas_valve", "industrial_light", "infinite_lava_cauldron",
	"infinite_snow_cauldron", "infinite_water_cauldron", "item_mover", "item_pipe", "liquid_drain",
	"liquid_pipe", "liquid_pump", "liquid_tank", "liquid_valve", "magic_crate", "message_block",
	"mineral_generator", "multiblock_base", "nether_generator", "poppy_generator", "randomizer",
	"receiver", "shortener", "solar_panel", "spitter", "tag_adder", "tag_remover", "teleport_anchor",
	"unboxer", "uni_gate",
)

/**
 * Write each existing marker's block type into `data.type`.
 *
 * 5.1.9 keys the wrench's cyclable-property registry by block type, and a data pack cannot ask an
 * entity which of its tags names its kind -- so placement now writes it down. Markers placed
 * before that have the tag but not the field, and without this the wrench would tell you every
 * block you already built predates menus.
 *
 * One line per block type is the whole trick: the tag is the type, it just cannot be read back as
 * a string. Idempotent -- it overwrites with the same value.
 */
internal val m5_1_8 = migration("5.1.8", "5.1.9") { world {
	for (type in TYPES_5_1_8) execute {
		asTarget(blockMarkers(type))
		run { data(self()) { modify("data.type") { set(type) } } }
	}
}}

/** RA Wires blocks (and the other powered blocks) that carried the dead `enabled` property. */
private val HAD_ENABLED = listOf(
	"battery", "boiler", "creative_eu", "creative_fluid", "electric_breaker", "electric_consumer",
	"electric_furnace", "electric_generator", "electric_switch", "electric_wire", "gas_pump",
	"gas_tank", "gas_valve", "industrial_light", "liquid_drain", "liquid_pipe", "liquid_pump",
	"liquid_tank", "liquid_valve", "solar_panel", "magic_crate", "ender_item_vault",
	"ender_fluid_vault", "ender_power_vault", "teleport_anchor", "mineral_generator",
	"nether_generator", "poppy_generator",
)

/**
 * Drop the dead `enabled` property from RA Wires blocks.
 *
 * 5.1.10 removed it from the module. It was a second off switch sitting next to redstone on blocks
 * that mostly had nothing else to configure, and it turned the wrench menu into a list of one
 * useless row. Existing markers still carry the field, where it would show up in the Data Handler
 * as a property that no longer does anything -- worse than not being there.
 *
 * The EU Switch is the one that actually used it and now runs on redstone, so its copy has to go
 * too or a switch saved as disabled would look configurable and be inert.
 *
 * Idempotent: removing an absent path does nothing.
 */
internal val m5_1_9 = migration("5.1.9", "5.1.10") { world {
	for (type in HAD_ENABLED) execute {
		asTarget(blockMarkers(type))
		run { data(self()) { remove("data.properties.enabled") } }
	}
}}

/**
 * Clear every existing skin so it is redrawn with the wider anti-z-fighting margin.
 *
 * 5.1.12 widened the skin overlay from 1.004 to 1.02 -- two thousandths of a block of clearance to
 * a hundredth -- because the old margin was geometrically clear of the block underneath but too
 * close for the depth buffer to separate at range, so skins flickered.
 *
 * Displays already in the world keep whatever transformation they were summoned with; nothing
 * rewrites it. Killing them is the update: every skinned block checks each tick whether its skin
 * is missing and redraws it if so, which is verified to cover all four of them -- Breeder, EU
 * Generator, Electric Furnace and Unboxer -- so the gap lasts one tick.
 *
 * Runs once per world (see [MigrationsPart]); safe to run again regardless -- it costs one
 * redraw per skinned block.
 */
internal val m5_1_11 = migration("5.1.11", "5.1.12") {
	world { kill(allEntities { type = EntityTypes.BLOCK_DISPLAY; tag = "ra.skin" }) }
	// The same for a block that was in an unloaded chunk at load: its own skin only, the one
	// centred on it, killed the first time the block is loaded again.
	marker(Tag.CUSTOM_BLOCK) {
		raw("execute at @s align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin,distance=..0.4]")
	}
}
