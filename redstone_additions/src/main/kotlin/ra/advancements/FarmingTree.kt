package ra.advancements

import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtString
import ra.advancements.Flag.TRUE
import ra.modules.farming.net.CaptureNet

/** Farming (`ra_farming`). The Breeder moved here from Interactive Machines in v6.0.3. */
internal val farmingTree = listOf(
	category("ra_farming", Items.WHEAT, "Farming", "Tend crops and animals without lifting a finger.", "get_farming_item", Items.WHEAT_SEEDS),
	egg("ra_farming", "breeder", "Breeder", Items.DISPENSER, TRUE, "Obtain the Breeder item."),
	egg("ra_farming", "sprinkler", "Sprinkler", Items.WATER_BUCKET, TRUE),
	egg("ra_farming", "tree_farm", "Tree Farm", Items.OAK_SAPLING, TRUE),
	Unlock(
		"ra_farming/get_capture_net", "ra_farming", Items.COBWEB, "Capture Net", "Obtain a Capture Net.", "get_capture_net",
		Items.RABBIT_HIDE, Match.Partial(nbt { this["capture_net"] = nbt {} }),
	),
	// By model and rarity: an upgraded rabbit hide carries no custom data until it first catches.
	Unlock(
		"ra_farming/get_master_capture_net", "ra_farming/get_capture_net", Items.NETHER_STAR, "Master Capture Net",
		"Upgrade a Capture Net with a nether star.", "get_master_capture_net", Items.RABBIT_HIDE,
		Match.Components(mapOf("item_model" to NbtString(CaptureNet.MODEL), "rarity" to NbtString("epic"))),
	),
)
