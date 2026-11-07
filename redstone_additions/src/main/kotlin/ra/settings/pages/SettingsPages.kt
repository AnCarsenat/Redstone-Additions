package ra.settings.pages

/**
 * Every settings page, in the order the beet build read `tools/settings/<page>.json` (sorted by
 * file name). The order is observable: it numbers the admin pages and every clickable action
 * code, and those codes are what the chat buttons a player has on screen carry.
 */
object SettingsPages {
	val all: List<Page> = listOf(
		ChunkLoaderPage,
		EnchantingPage,
		EnderPage,
		GatesPage,
		GeneralPage,
		InfinitePage,
		InteractivePage,
		JetpacksPage,
		MultiblockPage,
		SensorsPage,
		StoragePage,
		WirelessPage,
		WiresPage,
		YouPage,
	)
}
