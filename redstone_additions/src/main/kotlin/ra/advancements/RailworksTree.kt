package ra.advancements

import io.github.ayfri.kore.generated.Items

/** Railworks (`ra_railworks`). */
internal val railworksTree = listOf(
	category("ra_railworks", Items.RAIL, "Railworks", "Send each cart where it belongs.", "get_railworks_item", Items.RAIL),
	egg("ra_railworks", "route_rail", "Route Rail", Items.DETECTOR_RAIL, Flag.TRUE),
	// Partial: neither item is a bat spawn egg, and the Rail Builder also carries entity data.
	Unlock(
		"ra_railworks/get_ram_upgrade", "ra_railworks", Items.ANVIL, "Ram Upgrade", "Obtain the Ram Upgrade.",
		"get_ram_upgrade", Items.FIREWORK_STAR, Match.Partial(raData("ram_upgrade", Flag.TRUE)),
	),
	Unlock(
		"ra_railworks/get_rail_builder", "ra_railworks", Items.HOPPER_MINECART, "Rail Builder", "Obtain the Rail Builder.",
		"get_rail_builder", Items.HOPPER_MINECART, Match.Partial(raData("rail_builder", Flag.TRUE)),
	),
)
