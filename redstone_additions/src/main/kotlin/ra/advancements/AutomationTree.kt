package ra.advancements

import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set

/** Automation (`ra_automation`). */
internal val automationTree = listOf(
	category("ra_automation", Items.CRAFTER, "Automation", "Let machines run your vanilla blocks.", "get_automation_item", Items.CRAFTER),
	egg("ra_automation", "crafter_controller", "Crafter Controller", Items.TARGET, Flag.TRUE),
	egg("ra_automation", "uncrafter", "Uncrafter", Items.CHISELED_TUFF_BRICKS, Flag.TRUE),
	egg("ra_automation", "experience_absorber", "Experience Absorber", Items.VERDANT_FROGLIGHT, Flag.TRUE),
	// Partial: a Pattern also carries its grid.
	Unlock(
		"ra_automation/get_pattern", "ra_automation", Items.FLOW_BANNER_PATTERN, "Pattern", "Obtain a Pattern.", "get_pattern",
		Items.PAPER, Match.Partial(nbt { this["pattern"] = nbt {} }),
	),
)
