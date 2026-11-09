package ra.modules.ender

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import ra.core.Obj
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.raw
import ra.core.item.ItemSpec
import ra.core.item.SComp
import ra.core.item.loreItem
import ra.core.block.soundListenersSel
import ra.core.block.markerSel
import ra.api.Goggles
import ra.core.Storage
import ra.core.TextFormat
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom
import io.github.ayfri.kore.commands.function

/**
 * What every `ra_ender` block shares: identifiers, the item shape, the recipe and the break.
 *
 * A vault is half of a link. Two vaults sharing a channel string move contents between them, one
 * sending and one receiving, so nothing is ever copied: every transfer removes from the source
 * before, or in the same command as, it adds to the destination.
 */
internal object EnderIds {
	/** Per-block work timer. */
	const val CD = "ra.ender.cd"

	/** Teleport cooldown, on the anchor. */
	const val TP_CD = "ra.ender.tp_cd"

	/** Arrival grace, on the player who just teleported. */
	const val GRACE = "ra.ender.grace"

	/**
	 * Held by a vault for the length of its own cycle: a two-way vault wears the send and the
	 * receive tag, so without this every partner search could find itself.
	 */
	const val SELF = "ra.ender.self"

	/** Held by whichever vault is pushing items (not necessarily the one whose cycle it is). */
	const val SENDING = "ra.ender.sending"
	const val PULL_TARGET = "ra.ender.pull_target"

	// Who can receive / give this tick. Recomputed every tick from `mode`, never stored.
	const val RECV_ITEM = "ra.ender.recv_item"
	const val SEND_ITEM = "ra.ender.send_item"
	const val RECV_FLUID = "ra.ender.recv_fluid"
	const val RECV_POWER = "ra.ender.recv_power"

	/** The destination anchor of the teleport being processed. */
	const val TP_DEST = "ra.ender.tp_dest"

	// Network membership the wires module looks for.
	const val FLUID_NODE = "ra.wires.fluid_node"
	const val ELECTRIC_NODE = "ra.wires.electric_node"
}

/** An ender block's egg: rare (or epic) with a glint and its lore, between the name and custom_data. */
internal fun CustomBlock.enderItem(rarity: String, vararg lore: SComp): ItemSpec = loreItem {
	rarity(rarity)
	glint()
	lore(*lore)
}

/**
 * The block's recipe and its unlock advancement, criterion `has_pearl`: every ender block is
 * discovered by holding an ender pearl.
 */
internal fun DataPack.enderRecipe(block: CustomBlock, item: ItemSpec, category: RecipeCategory, shape: CraftingShaped.() -> Unit) =
	shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ENDER_PEARL, "has_pearl"), category, shape = shape)

/** `on_break` of an ender block: [cleanup], drop swap, stone break sound, marker removed. No cloud. */
internal fun DataPack.enderOnBreak(block: CustomBlock, item: ItemSpec, cleanup: Function.() -> Unit = {}): FunctionArgument =
	blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cleanup = cleanup)

/** `blocks/<dir>/give`: the item, then a one-line hint in chat. */
internal fun DataPack.enderGive(block: CustomBlock, item: ItemSpec, hint: String) = define(block.give) {
	raw("give @s ${item.giveArg()}")
	raw("tellraw @s [{text:\"[RA] \",color:\"gold\"},{text:\"${block.name}\",color:\"light_purple\"},$hint]")
}

/** One wrench line: `[Wrench] <name>: <mode>`, for players within 10 blocks. */
internal fun wrenchSay(name: String, mode: String, color: String) =
	"""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"$name: ",color:"gray"},{text:"$mode",color:"$color"}]"""

internal val WRENCH_CLICK = "playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4"

/**
 * `cycle_mode` of the fluid and power vaults: link -> send -> receive -> link.
 * Context: as the vault marker, at the block.
 */
internal fun DataPack.cycleLinkSendReceive(block: CustomBlock) = define(block.fn("cycle_mode")) {
	raw("scoreboard players set #ender.mode ${Obj.TEMP} 0")
	raw("execute if data entity @s data.properties{mode:\"send\"} run scoreboard players set #ender.mode ${Obj.TEMP} 1")
	raw("execute if data entity @s data.properties{mode:\"receive\"} run scoreboard players set #ender.mode ${Obj.TEMP} 2")

	raw("execute if score #ender.mode ${Obj.TEMP} matches 0 run data modify entity @s data.properties.mode set value \"send\"")
	raw("execute if score #ender.mode ${Obj.TEMP} matches 1 run data modify entity @s data.properties.mode set value \"receive\"")
	raw("execute if score #ender.mode ${Obj.TEMP} matches 2 run data modify entity @s data.properties.mode set value \"link\"")

	raw("execute if score #ender.mode ${Obj.TEMP} matches 0 run ${wrenchSay(block.name, "send", "green")}")
	raw("execute if score #ender.mode ${Obj.TEMP} matches 1 run ${wrenchSay(block.name, "receive", "aqua")}")
	raw("execute if score #ender.mode ${Obj.TEMP} matches 2 run ${wrenchSay(block.name, "link (two-way)", "light_purple")}")
	raw(WRENCH_CLICK)
}

/**
 * Who can receive this tick: every marker of [block] not in [notMode] gets [tag]. Recomputed
 * every tick rather than stored, so changing the mode with the wrench takes effect immediately.
 *
 * THESE WERE LOST IN v5.1.8, AND THE VAULTS HAVE NOT LINKED SINCE
 * They used to carry an `unless data.properties{enabled:0b}` clause. When the `enabled` property
 * was removed from the module, the whole line went with it instead of just that clause -- so the
 * tags were still cleared every tick and still required by link/send_items, but nothing ever
 * added them again. A sending vault searched for a receiver wearing a tag no vault could be
 * wearing, found nothing, and silently did nothing.
 */
internal fun Function.tagUnlessMode(block: CustomBlock, notMode: String, tag: String) =
	raw("execute as ${block.markerSel()} unless data entity @s data.properties{mode:\"$notMode\"} run tag @s add $tag")

/**
 * `function ra:tools/goggles/billboard/stacked_text_line with storage ra:temp ender_line`. The
 * value is pasted inside `{text:"$(value)"}` and can be a player's text (a Teleport Anchor
 * target), so it goes through the core sanitiser first: a `"` in it can never close the string.
 */
internal fun Function.stackedTextLineFromStorage() {
	cleanFrom("storage ra:temp ender_line.value", TextFormat.FREE)
	raw("execute if score ${SafeClean.OK} ${Obj.TEMP} matches 1 run data modify storage ra:temp ender_line.value set from storage ra:safe out")
	raw("execute if score ${SafeClean.OK} ${Obj.TEMP} matches 0 run data modify storage ra:temp ender_line.value set value \"?\"")
	function(Goggles.Billboard.stackedTextLine, Storage.TEMP, "ender_line")
}
