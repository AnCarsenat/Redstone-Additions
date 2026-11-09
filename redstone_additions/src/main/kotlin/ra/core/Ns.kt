package ra.core

/**
 * Every namespace the pack writes into.
 *
 * The namespaces are the runtime contract of the pack: function paths are typed in chat
 * (`/function ra:give_all_items`), stored in marker data and read back by older worlds. They are
 * therefore kept exactly as the beet version had them; the Kotlin package layout is free, the
 * generated `ns:path` is not.
 */
enum class Ns(val id: String) {
	MINECRAFT("minecraft"),
	RA("ra"),
	LIB("ra_lib"),
	LIB_MULTIBLOCK("ra_lib_multiblock"),
	SETTINGS("ra_settings"),
	MIGRATIONS("ra_migrations"),
	ADVANCEMENTS("ra_advancements"),
	EXAMPLE("ra_example"),
	CHUNK_LOADER("ra_chunk_loader"),
	ENCHANTING("ra_enchanting"),
	ENDER("ra_ender"),
	GATES("ra_gates"),
	INFINITE("ra_infinite"),
	INTERACTIVE("ra_interactive"),
	JETPACKS("ra_jetpacks"),
	MULTIBLOCK("ra_multiblock"),
	SENSORS("ra_sensors"),
	STORAGE("ra_storage"),
	WIRELESS("ra_wireless"),
	WIRES("ra_wires"),
	AUTOMATION("ra_automation");

	override fun toString() = id
}
