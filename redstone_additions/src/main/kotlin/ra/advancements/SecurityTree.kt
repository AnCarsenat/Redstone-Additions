package ra.advancements

import io.github.ayfri.kore.generated.Items

/** Security (`ra_security`). */
internal val securityTree = listOf(
	category("ra_security", Items.IRON_DOOR, "Security", "Keep your builds shut to anyone without the code.", "get_security_item", Items.IRON_DOOR),
	egg("ra_security", "keypad", "Keypad", Items.STONE_BUTTON, Flag.TRUE),
)
