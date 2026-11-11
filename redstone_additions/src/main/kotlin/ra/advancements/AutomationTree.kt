package ra.advancements

import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set

/**
 * Automation (`ra_automation`). The Fake Player is disabled (`FakePlayer.ENABLED`) and has no
 * advancement.
 */
internal val automationTree = listOf(
	category("ra_automation", Items.CRAFTER, "Automation", "Let machines run your vanilla blocks.", "get_automation_item", Items.CRAFTER),
	egg("ra_automation", "crafter_controller", "Crafter Controller", Items.TARGET, Flag.TRUE),
	egg("ra_automation", "uncrafter", "Uncrafter", Items.GRINDSTONE, Flag.TRUE),
	egg("ra_automation", "experience_absorber", "Experience Absorber", Items.VERDANT_FROGLIGHT, Flag.TRUE),
	egg("ra_automation", "item_pipe_splitter", "Item Pipe Splitter", Items.DISPENSER, Flag.TRUE),
	egg("ra_automation", "conveyor", "Conveyor", Items.MAGENTA_GLAZED_TERRACOTTA, Flag.TRUE),
	// Partial: a Pattern also carries its grid.
	Unlock(
		"ra_automation/get_pattern", "ra_automation", Items.FLOW_BANNER_PATTERN, "Pattern", "Obtain a Pattern.", "get_pattern",
		Items.PAPER, Match.Partial(nbt { this["pattern"] = nbt {} }),
	),
)
