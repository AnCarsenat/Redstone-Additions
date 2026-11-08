package ra.core.block

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.summon
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.generated.Tags
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.Obj
import ra.core.Tag
import ra.core.define
import ra.core.item.ItemSpec
import ra.core.raw

/**
 * Detecting that a custom block was broken, and what its `on_break` does about it.
 *
 * A marker outlives its block: the tick of the block type finds markers whose block is gone
 * ([breakCheck]) and runs their `on_break` ([blockOnBreak]) as the marker, at its position.
 */

/** How a block type's tick decides that a marker's block is gone. */
sealed interface BreakTest {
	/**
	 * The block is air. The cauldrons use it: a cauldron emptied by a bucket turns into a plain
	 * `cauldron`, which is still the block, only waiting for its refill.
	 */
	data object Air : BreakTest

	/**
	 * The block is anything but [physical]. A block replaced by something else (a piston pushing,
	 * a player swapping it) breaks too.
	 */
	data class NotBlock(val physical: BlockArgument) : BreakTest
}

/**
 * Every marker of [block] whose block fails [test] runs [onBreak], as the marker, at it:
 * `execute as @e[type=marker,tag=<tag>] at @s (if|unless) block ~ ~ ~ <...> run function <onBreak>`.
 * [skipNew] leaves out markers still tagged `ra.new` (placed this tick, block not set yet).
 */
fun Function.breakCheck(block: CustomBlock, test: BreakTest, onBreak: FunctionArgument, skipNew: Boolean = false) {
	execute {
		asTarget(allEntities {
			type = EntityTypes.MARKER
			tag = block.tag
			if (skipNew) tag = "!${Tag.NEW}"
		})
		at(self())
		when (test) {
			BreakTest.Air -> ifCondition { block(vec3(), Tags.Block.AIR) }
			is BreakTest.NotBlock -> unlessCondition { block(vec3(), test.physical) }
		}
		run { function(onBreak) }
	}
}

/** [breakCheck] with [BreakTest.NotBlock]: the marker breaks when its block is no longer [physical]. */
fun Function.breakCheck(block: CustomBlock, physical: BlockArgument, onBreak: FunctionArgument, skipNew: Boolean = false) =
	breakCheck(block, BreakTest.NotBlock(physical), onBreak, skipNew)

/** `execute as <every marker of block> at @s run function <fn>`. */
fun Function.forEachMarker(block: CustomBlock, fn: FunctionArgument) = execute {
	asTarget(markers(block))
	at(self())
	run { function(fn) }
}

/**
 * `ns:blocks/<dir>/tick`, the per-type tick: every marker whose block fails [test] runs
 * [onBreak], then [body] runs (typically [forEachMarker] with the block's `process`).
 *
 * The beet version spelled break handling as one `execute as @e[tag=ra.broken,...]` line per
 * cleanup step. Routing each broken marker through one `on_break` function does the same work
 * once per marker and keeps the cleanup in one readable place.
 */
fun DataPack.blockTick(
	block: CustomBlock,
	onBreak: FunctionArgument,
	test: BreakTest = BreakTest.Air,
	body: Function.() -> Unit,
) = define(block.tick) {
	breakCheck(block, test, onBreak)
	body()
}

/** The stone break sound most blocks play. */
const val STONE_BREAK_SOUND = "minecraft:block.stone.break"

/**
 * `ns:blocks/<dir>/on_break`, run as a broken block's marker at its position. In this order:
 * [cleanup] (the block's own displays, outputs, stored items), the vanilla drop removed,
 * [afterVanillaKill], [drop] (the custom item), the break [sound] if any, a puff of cloud when
 * [cloud], and the marker removed.
 *
 * The defaults are the plain block: the item dropped tagged `ra.drop_temp`, no sound, no cloud.
 * The blocks ported with their beet break effects pass e.g.
 * `drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true`.
 */
fun DataPack.blockOnBreak(
	block: CustomBlock,
	drop: Function.() -> Unit = { dropBlockItem(block) },
	sound: String? = null,
	soundListenersOnly: Boolean = false,
	cloud: Boolean = false,
	afterVanillaKill: Function.() -> Unit = {},
	cleanup: Function.() -> Unit = {},
): FunctionArgument = define(block.fn("on_break")) {
	cleanup()
	killVanillaDrop(block.physical)
	afterVanillaKill()
	drop()
	if (sound != null) breakSound(sound, soundListenersOnly)
	if (cloud) breakCloud()
	kill(self())
}

/**
 * Drops the block's item at `~ ~ ~` (typed `summon minecraft:item`), so the player gets back
 * exactly the item they placed. Tagged `ra` + `ra.drop_temp` unless ![tagged].
 *
 * Untagged is the safe choice next to containers: some interactive blocks overwrite the first
 * `ra.drop_temp` item they find with their container contents, so a tagged drop lying nearby
 * could be replaced.
 */
fun Function.dropBlockItem(block: CustomBlock, tagged: Boolean = true) {
	summon(EntityTypes.ITEM, vec3(), nbt {
		this["Item"] = block.itemStackNbt()
		if (tagged) this["Tags"] = NbtList(listOf(NbtString(Tag.RA), NbtString(Tag.DROP_TEMP)))
	})
}

/**
 * `summon item ~ ~ ~ {Item:<stack>}` for a full [ItemSpec] (lore, rarity...), as vanilla text.
 * Untagged unless [tagged] (then `,Tags:["ra","ra.drop_temp"]`); see [dropBlockItem] for why.
 */
fun Function.dropItem(item: ItemSpec, tagged: Boolean = false) {
	val tags = if (tagged) """,Tags:["${Tag.RA}","${Tag.DROP_TEMP}"]""" else ""
	raw("summon item ~ ~ ~ {Item:${item.stackSnbt()}$tags}")
}

/** Candidates for [killVanillaDrop], for the four lines it takes to choose one. */
private const val VANILLA_DROP = "ra.vanilla_drop"

/**
 * Removes the vanilla item the physical block dropped when it was broken, so only the custom item
 * is left. Run as the marker, at the block.
 *
 * ONLY THE FRESH DROP. This used to kill the nearest item of that id within 2 blocks, whatever it
 * was: a stack of 64 quartz a player had thrown down beside a Shortener, or the contents a broken
 * container had just spilled. A block's own drop is a single plain item (count 1, no components)
 * spawned at the break, and the break is noticed within a tick or two, so it is the nearest such
 * item no more than 5 ticks old. Item age is not a selector argument, hence the score; `0..` and
 * not `..5`, because an item that never despawns carries a negative age. When no item qualifies,
 * nothing is killed.
 */
fun Function.killVanillaDrop(itemId: String) {
	raw("tag @e[type=item,distance=..2,nbt={Item:{id:\"$itemId\",count:1}},nbt=!{Item:{components:{}}}] add $VANILLA_DROP")
	raw("execute as @e[type=item,tag=$VANILLA_DROP,distance=..2] store result score @s ${Obj.TEMP} run data get entity @s Age")
	raw("kill @e[type=item,tag=$VANILLA_DROP,distance=..2,scores={${Obj.TEMP}=0..5},limit=1,sort=nearest]")
	raw("scoreboard players reset @e[type=item,tag=$VANILLA_DROP,distance=..2] ${Obj.TEMP}")
	raw("tag @e[type=item,tag=$VANILLA_DROP,distance=..2] remove $VANILLA_DROP")
}

/**
 * `playsound <sound> block <who> ~ ~ ~ 1 1`: everyone within 16 blocks, or only those who asked
 * for block sounds ([listenersOnly], user setting `ra.u.snd`).
 */
fun Function.breakSound(sound: String = STONE_BREAK_SOUND, listenersOnly: Boolean = false) {
	val who = if (listenersOnly) soundListenersSel(16) else "@a[distance=..16]"
	raw("playsound $sound block $who ~ ~ ~ 1 1")
}

/** The puff of cloud a broken block leaves. */
fun Function.breakCloud() = raw("particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5")
