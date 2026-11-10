package ra.modules.displays

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import ra.core.Ra
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.item.ItemSpec
import ra.core.raw

/**
 * One block of the Displays module. [DisplaysModule] wires every member the same way (load,
 * tick, give_all, bundle, slow clock), so a new display block is one object implementing this
 * and one entry in [DisplaysModule.members].
 *
 * Every display block is configurable the same way (facing, direction, colours, offset, scale):
 * declare a [DisplayStyle] (DisplayConfig.kt) and call its `declare`, `wrenchRows` and
 * `registerStyle` from the block, and `defineForms` for each display part.
 */
interface DisplayBlock {
	val block: CustomBlock
	val item: ItemSpec

	/** Run once a second by [DisplayPoll] as each loaded marker, at it; null for a block that needs no clock. */
	val poll: FunctionArgument?

	/** Recipe, placement, goggles, tick, break: everything particular to the block. */
	fun Ra.registerBlock()
}

/**
 * The display entities of a block, and the rules every display block follows.
 *
 * AT THE MARKER, OFFSET BY THE TRANSFORMATION. Every display is summoned on the marker's own
 * position and moved where it is drawn with `transformation.translation`. The admin purge
 * finds a block's displays as "within 0.6 of its marker" (and orphans as "no marker within
 * 0.6"), so a display summoned a block above its marker would be swept as an orphan.
 *
 * LIT FROM THE INSIDE. A display takes its light from the block it stands in, and these stand
 * inside a solid block: without `brightness` they render black. Every display here is fully lit.
 *
 * `block_state:{id:"..."}`. Since 26.3 a block display's state is `{id, properties}` (or a bare
 * id string); the old `{Name:"..."}` fails to decode, the game only logs "Serialization errors"
 * and the display shows air. Replace the whole `block_state`, never one key of it.
 *
 * TAGS. `ra` (the uninstall and purge), `ra.display`, and one tag per part, so a block reaches
 * its own displays with `distance=..0.01` and a neighbour's (a block away) are never touched.
 */
internal object DisplayEntities {
	const val BRIGHT = "brightness:{sky:15,block:15}"

	/** `@e[type=<type>,tag=<tag>,distance=..0.01,limit=1]`: this block's display of that part. */
	fun sel(type: String, tag: String) = "@e[type=minecraft:$type,tag=$tag,distance=..0.01,limit=1]"

	/**
	 * Summons one display at `~ ~ ~` with [nbt] (SNBT body without braces, no `Tags`). Its
	 * Rotation (the facing) and transformation are the block's restyle ([DisplayStyle]). As the
	 * marker, at it.
	 */
	fun Function.summonDisplay(type: String, tags: List<String>, nbt: String) {
		val all = (listOf(Tag.RA, Tag.DISPLAY) + tags).joinToString(",") { "\"$it\"" }
		raw("summon minecraft:$type ~ ~ ~ {Tags:[$all],$BRIGHT,$nbt}")
	}

	/** Every display carrying [tag] on this block. In `on_break`, before the marker goes. */
	fun Function.killDisplays(tag: String) = raw("kill @e[tag=$tag,distance=..0.6]")
}
