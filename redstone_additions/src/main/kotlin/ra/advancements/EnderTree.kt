package ra.advancements

import io.github.ayfri.kore.features.advancements.AdvancementFrameType
import io.github.ayfri.kore.generated.Items
import ra.advancements.Flag.TRUE

/** Ender Links (`ra_ender`): the Overstar and the Anchor Tablet it crafts. */
internal val enderTree = listOf(
	category("ra_ender", Items.ENDER_EYE, "Ender Links", "Link places across the world.", "get_ender_item", Items.ENDER_PEARL),
	// Partial: matched on the flag alone, like the other RA items with more than a flag.
	Unlock(
		"ra_ender/get_overstar", "ra_ender", Items.NETHER_STAR, "Overstar",
		"Sacrifice nether stars on an enchanting table until one becomes an Overstar.", "get_overstar",
		Items.TEST_INSTANCE_BLOCK, Match.Partial(raData("overstar", TRUE)), frame = AdvancementFrameType.CHALLENGE,
	),
	// Partial: a tablet also carries its favourites and last trip.
	Unlock(
		"ra_ender/get_anchor_tablet", "ra_ender/get_overstar", Items.RECOVERY_COMPASS, "Anchor Tablet",
		"Craft an Anchor Tablet with an Overstar: any anchor, from anywhere.", "get_anchor_tablet",
		Items.BLAZE_ROD, Match.Partial(raData("anchor_tablet", TRUE)),
	),
)
