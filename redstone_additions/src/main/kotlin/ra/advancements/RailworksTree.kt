package ra.advancements

import io.github.ayfri.kore.generated.Items

/** Railworks (`ra_railworks`). */
internal val railworksTree = listOf(
	category("ra_railworks", Items.RAIL, "Railworks", "Send each cart where it belongs.", "get_railworks_item", Items.RAIL),
	egg("ra_railworks", "route_rail", "Route Rail", Items.DETECTOR_RAIL, Flag.TRUE),
)
