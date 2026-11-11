package ra.core.block

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.item.SByte
import ra.core.item.SComp
import ra.core.item.SList
import ra.core.item.SRaw
import ra.core.item.SStr
import ra.core.item.Snbt

/**
 * The bat a custom block's spawn egg spawns, described once.
 *
 * It only lives until the placement router (`ra:placement/detect_bats`) kills it, but that is long
 * enough to be seen: it is silent, frozen and invulnerable, and carries an infinite, hidden
 * invisibility effect (no particles, no icon) so that it never shows while a block is placed.
 *
 * Every egg the pack makes -- the `give` commands, the drops when a block is broken, recipe results,
 * refunds and bundles -- spells its bat with these values, so they all stay the same item.
 */
object PlacementBat {
	/** Everything after the bat's id and Tags, in order. */
	val flagEntries: List<Pair<String, Snbt>> = listOf(
		"Silent" to SByte(true),
		"NoAI" to SByte(true),
		"Invulnerable" to SByte(true),
		"active_effects" to SRaw("""[{id:"minecraft:invisibility",duration:-1,amplifier:0b,ambient:1b,show_particles:0b,show_icon:0b}]"""),
	)

	/** [flagEntries] as SNBT, without the braces: `Silent:1b,...`, for hand-written egg lines. */
	val FLAGS: String = SComp(flagEntries).text().removeSurrounding("{", "}")

	/** The bat's `entity_data` as SNBT: `{id:"minecraft:bat",Tags:[...],<FLAGS>}`. */
	fun snbt(tags: List<String>) = SComp(listOf("id" to SStr("minecraft:bat"), "Tags" to SList(tags.map(::SStr))) + flagEntries)

	/** The bat's `entity_data` as NBT. */
	fun nbt(tags: List<String>): NbtCompound = NbtCompound(
		mapOf("id" to NbtString("minecraft:bat"), "Tags" to NbtList(tags.map(::NbtString))) +
			flagEntries.associate { (k, v) -> k to v.nbt() },
	)
}
