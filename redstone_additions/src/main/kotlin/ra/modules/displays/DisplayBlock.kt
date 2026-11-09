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
 * TAGS. `ra` (the uninstall and purge), `ra.display`, and one tag per part, so a block reaches
 * its own displays with `distance=..0.01` and a neighbour's (a block away) are never touched.
 */
internal object DisplayEntities {
	const val BRIGHT = "brightness:{sky:15,block:15}"
	private const val NEW = "ra.dsp.new"

	/** `@e[type=<type>,tag=<tag>,distance=..0.01,limit=1]`: this block's display of that part. */
	fun sel(type: String, tag: String) = "@e[type=minecraft:$type,tag=$tag,distance=..0.01,limit=1]"

	/**
	 * Summons one display at `~ ~ ~` with [nbt] (SNBT body without braces, no `Tags`), turned
	 * the way the marker faces when [rotated]. As the marker, at it.
	 */
	fun Function.summonDisplay(type: String, tags: List<String>, nbt: String, rotated: Boolean = false) {
		val all = (listOf(Tag.RA, Tag.DISPLAY) + tags + NEW).joinToString(",") { "\"$it\"" }
		raw("summon minecraft:$type ~ ~ ~ {Tags:[$all],$BRIGHT,$nbt}")
		// `summon` ignores the execution rotation: the marker's Rotation is the block's facing.
		if (rotated) raw("data modify entity @e[type=minecraft:$type,tag=$NEW,distance=..0.01,limit=1] Rotation set from entity @s Rotation")
		raw("tag @e[type=minecraft:$type,tag=$NEW,distance=..0.01] remove $NEW")
	}

	/** Every display carrying [tag] on this block. In `on_break`, before the marker goes. */
	fun Function.killDisplays(tag: String) = raw("kill @e[tag=$tag,distance=..0.6]")
}
