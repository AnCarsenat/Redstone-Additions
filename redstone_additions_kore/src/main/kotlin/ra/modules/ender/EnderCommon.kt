package ra.modules.ender

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import ra.core.Obj
import ra.core.block.CustomBlock
import ra.core.define
import ra.core.raw
import ra.core.item.ItemSpec
import ra.core.item.SComp
import ra.core.item.loreItem
import ra.modules.sensors.plainOnBreak
import ra.modules.sensors.shapedRecipe

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
	shapedRecipe(block.ns, block.id, item, unlockedBy = Items.ENDER_PEARL, category = category, trigger = "has_pearl", shape = shape)

/** `on_break` of an ender block: [cleanup], drop swap, stone break sound, marker removed. No cloud. */
internal fun DataPack.enderOnBreak(block: CustomBlock, item: ItemSpec, cleanup: Function.() -> Unit = {}): FunctionArgument =
	plainOnBreak(block, item, particle = false, cleanup = cleanup)

/** `blocks/<dir>/register_block`: the debug line of `ra_ender:load`. */
internal fun DataPack.enderRegisterBlock(block: CustomBlock): FunctionArgument = define(block.fn("register_block")) {
	raw("""tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"${block.name} registered",color:"gray"}]""")
}

/** `blocks/<dir>/give`: the item, then a one-line hint in chat. */
internal fun DataPack.enderGive(block: CustomBlock, item: ItemSpec, hint: String) = define(block.give) {
	raw("give @s ${item.giveArg()}")
	raw("tellraw @s [{text:\"[RA] \",color:\"gold\"},{text:\"${block.name}\",color:\"light_purple\"},$hint]")
}

/** One wrench line: `[Wrench] <name>: <mode>`, for players within 10 blocks. */
internal fun wrenchSay(name: String, mode: String, color: String) =
	"""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"$name: ",color:"gray"},{text:"$mode",color:"$color"}]"""

internal const val WRENCH_CLICK = "playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4"

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

/** `execute as <every marker of block> unless data entity @s data.properties.<key> run ... set value <snbt>`. */
internal fun Function.enderDefault(block: CustomBlock, key: String, snbt: String) =
	raw("execute as @e[type=marker,tag=${block.tag}] unless data entity @s data.properties.$key run data modify entity @s data.properties.$key set value $snbt")

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
	raw("execute as @e[type=marker,tag=${block.tag}] unless data entity @s data.properties{mode:\"$notMode\"} run tag @s add $tag")
