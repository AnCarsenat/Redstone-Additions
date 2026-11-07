package ra.modules.infinite

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Tag
import ra.core.block.CustomBlock
import ra.core.block.killVanillaDrop
import ra.core.define
import ra.core.raw
import ra.core.item.ItemSpec

/**
 * Break and placement handling for blocks whose item carries more than [CustomBlock] describes
 * (lore, rarity, glint): the Chunk Loader and the Infinite generators.
 *
 * Same shape as `blockTick` / `blockOnBreak`, with the differences these blocks had in the beet
 * pack kept: the marker counts as broken when its block is anything but [physical] (not only air),
 * the dropped item is the full [drop] stack, and the break plays a stone sound and a puff of cloud.
 */

/**
 * `ns:blocks/<dir>/on_break`, as the broken marker at its position: [cleanup], then the vanilla
 * drop is swapped for [drop], the break effects play and the marker is removed.
 */
fun DataPack.loreBlockOnBreak(block: CustomBlock, drop: ItemSpec, cleanup: Function.() -> Unit = {}) =
	define(block.fn("on_break")) {
		cleanup()
		killVanillaDrop(block.physical)
		raw("summon item ~ ~ ~ {Item:${drop.stackSnbt()}}")
		raw("playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1")
		raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5")
		kill(self())
	}

/** Every marker of [block] whose block is no longer [physical] runs [onBreak]. */
fun Function.breakCheck(block: CustomBlock, physical: BlockArgument, onBreak: FunctionArgument, skipNew: Boolean = false) {
	execute {
		asTarget(allEntities {
			type = EntityTypes.MARKER
			tag = block.tag
			if (skipNew) tag = "!${Tag.NEW}"
		})
		at(self())
		unlessCondition { block(vec3(), physical) }
		run { function(onBreak) }
	}
}

/**
 * The end of a placement handler as these blocks wrote it: `ra.new` comes off every marker of the
 * type (there is only ever the one just placed), then the handler reports success.
 */
fun Function.finishLorePlacement(block: CustomBlock) {
	tag(allEntities {
		type = EntityTypes.MARKER
		tag = block.tag
		tag = Tag.NEW
	}) { remove(Tag.NEW) }
	returnValue(1)
}

/** `tellraw @a[tag=ra.debug] ... "<name> registered"`, the per-block debug line of every module load. */
fun DataPack.registerBlockMessage(block: CustomBlock): FunctionArgument = define(block.fn("register_block")) {
	raw("""tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"${block.name} registered",color:"gray"}]""")
}

/** `[RA] <text>` in the module-load colours. */
fun Function.loadMessage(text: String, color: String) {
	raw("""tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"$text",color:"$color"}]""")
}
