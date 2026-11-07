package ra.modules.interactive

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.define
import ra.core.ensureScore
import ra.core.item.ItemSpec
import ra.core.raw

/**
 * What the ra_interactive and ra_storage blocks share beyond ra.core: the give item, the
 * first-slot spill, the hopper-rate gate and the double-chest partner table.
 *
 * Their `on_break` uses core `blockOnBreak` (custom item untagged, stone break sound, cloud) when
 * its fixed step order matches the beet file. The blocks whose break steps run in another order
 * (a network to leave or a skin to clear after the sound) compose them from the core primitives
 * `dropItem`, `breakSound` and `breakCloud` in their own file, so the output order stays.
 */

/** `ns:blocks/<dir>/give`: the block's full item, lore included. */
fun DataPack.specGive(block: CustomBlock, item: ItemSpec) = define(block.give) { raw("give @s ${item.giveArg()}") }

/**
 * Spills the container's FIRST stack (Items[0]) half a block up. Only the first: this is the
 * written-out form the beet blocks used, and the Magic Crate's recursive `drop_all` is the one
 * that empties a whole container.
 */
fun Function.dropFirstSlot() {
	raw("execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:[\"${Tag.RA}\",\"${Tag.DROP_TEMP}\"]}")
	raw("execute if entity @e[type=item,tag=${Tag.DROP_TEMP},limit=1] run data modify entity @e[type=item,tag=${Tag.DROP_TEMP},limit=1] Item set from block ~ ~ ~ Items[0]")
	raw("execute as @e[type=item,tag=${Tag.DROP_TEMP}] run tag @s remove ${Tag.DROP_TEMP}")
}

/** The hopper-rate gate: a missing score starts at 0 (see [ensureScore]), then the block acts every 4th tick. */
fun Function.hopperCooldown() {
	ensureScore("@s", Obj.COOLDOWN)
	scoreboard.players.add(self(), Obj.COOLDOWN, 1)
	raw("execute if score @s ${Obj.COOLDOWN} matches ..3 run return 0")
}

/**
 * One half of a double chest: when the block here is [block] facing [facing] with [type], its
 * other half is at [offset]. A double chest is two blocks that each know only their own side, so
 * which half is which depends on facing AND type.
 */
data class ChestHalf(val block: String, val facing: String, val type: String, val offset: String) {
	/** `if block ~ ~ ~ <this half> positioned <the other half>`, an execute fragment. */
	val toPartner get() = "if block ~ ~ ~ minecraft:$block[facing=$facing,type=$type] positioned $offset"
}

/** Every chest half, chests first then trapped chests, in the order the beet files tested them. */
val CHEST_HALVES: List<ChestHalf> = listOf("chest", "trapped_chest").flatMap { b ->
	listOf(
		ChestHalf(b, "north", "left", "~1 ~ ~"),
		ChestHalf(b, "north", "right", "~-1 ~ ~"),
		ChestHalf(b, "south", "left", "~-1 ~ ~"),
		ChestHalf(b, "south", "right", "~1 ~ ~"),
		ChestHalf(b, "east", "left", "~ ~ ~1"),
		ChestHalf(b, "east", "right", "~ ~ ~-1"),
		ChestHalf(b, "west", "left", "~ ~ ~-1"),
		ChestHalf(b, "west", "right", "~ ~ ~1"),
	)
}
