package ra.advancements

import io.github.ayfri.kore.generated.Items
import ra.advancements.Flag.TRUE

/** Farming (`ra_farming`). The Breeder moved here from Interactive Machines in v6.0.3. */
internal val farmingTree = listOf(
	category("ra_farming", Items.WHEAT, "Farming", "Tend crops and animals without lifting a finger.", "get_farming_item", Items.WHEAT_SEEDS),
	egg("ra_farming", "breeder", "Breeder", Items.DISPENSER, TRUE, "Obtain the Breeder item."),
	egg("ra_farming", "sprinkler", "Sprinkler", Items.WATER_BUCKET, TRUE),
)
