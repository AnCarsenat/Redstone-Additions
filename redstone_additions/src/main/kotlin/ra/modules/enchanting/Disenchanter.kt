package ra.modules.enchanting

import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Enchantments
import io.github.ayfri.kore.generated.Items
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.blockRegisterMessage
import ra.core.block.BreakTest
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.particleViewersSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.computeScore
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.core.storeInt
import ra.modules.enchanting.EnchantingModule.DONE
import ra.modules.enchanting.EnchantingModule.onTop
import ra.modules.enchanting.xp.XpPay

/**
 * Disenchanter: moves an item's enchantments onto a book.
 *
 * Throw an enchanted item and a book on top, as you would sacrifice on an enchanting table. The
 * block makes an enchanted book holding the item's enchantments and hands the item back without
 * them. Curses stay on the item, as at a grindstone: a curse is not something to pass on. The
 * item's repair cost is reset too, again as a grindstone does.
 *
 * THE PRICE. [COST_KEY] mL of liquid experience per enchantment moved (server setting, default
 * [DEFAULT_COST] = 10 points), taken from a fluid network touching the block (see [XpPay]). At 0
 * it is free and needs no network.
 *
 * NO IDLE COST. The module's item scan runs it every ten ticks, and only for a Disenchanter with
 * an untagged item lying on it. Items that can never be used here (anything but a book or a
 * single enchanted item, or an item carrying only curses) are tagged `ra.ench.done` and ignored
 * until picked up.
 */
object Disenchanter {
	val block = CustomBlock(Ns.ENCHANTING, "disenchanter", "Disenchanter", model = "minecraft:crying_obsidian")
	val item = block.loreItem {
		lore(
			loreLine("Moves an item's enchantments onto a book", "gray"),
			loreLine("Throw an enchanted item and a book on top", "dark_gray"),
			loreLine("Costs liquid experience from a pipe beside it", "dark_gray"),
		)
	}

	val process = block.fn("process")

	/** The working tags off. Returns 0, for `return run`. */
	private val cleanup = block.fn("cleanup")

	/** Global setting: mL of experience per enchantment moved. */
	const val COST_KEY = "disenchanter_xp"
	const val DEFAULT_COST = 1000

	private const val IN = "ra.ench.dis_in"
	private const val BOOK = "ra.ench.dis_book"
	private const val T = Obj.TEMP

	/** Curses never leave the item. The vanilla ones: a data pack's own curse would move. */
	private val CURSES = listOf(Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE).map { it.asId() }

	private const val ENCH = "\"minecraft:enchantments\""

	fun Ra.registerDisenchanter() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.GRINDSTONE)) {
			pattern("BGB", "ODO", "OOO")
			key("B", Items.BOOK)
			key("G", Items.GRINDSTONE)
			key("D", Items.DIAMOND)
			key("O", Items.OBSIDIAN)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			finishPlacement(block)
		}

		pack.define(process) { process() }
		pack.define(cleanup) {
			raw("tag @e[type=item,tag=$IN,distance=..2] remove $IN")
			raw("tag @e[type=item,tag=$BOOK,distance=..2] remove $BOOK")
			raw("return 0")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("price", "Last price: ", "light_purple", " mL")
			stackedTextLine("", "Throw an enchanted item and a book on top", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.CRYING_OBSIDIAN)) {}
	}

	/** As the marker, at the block, with an untagged item lying on top. */
	private fun Function.process() {
		// Whatever is neither a book nor an enchanted item is not for us: ignored from now on.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("nbt=!{Item:{id:\"minecraft:book\"}}", "nbt=!{Item:{components:{$ENCH:{}}}}")} run tag @s add $DONE")
		// An enchanted stack of several: one book cannot take the enchantments of them all.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("nbt={Item:{components:{$ENCH:{}}}}", "nbt=!{Item:{count:1}}")} run tag @s add $DONE")
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("nbt={Item:{count:1,components:{$ENCH:{}}}}", "limit=1")} run tag @s add $IN")
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("nbt={Item:{id:\"minecraft:book\"}}", "nbt=!{Item:{components:{$ENCH:{}}}}", "limit=1")} run tag @s add $BOOK")

		raw("execute unless entity ${sel(IN)} run data modify entity @s data.status.state set value \"waiting for an enchanted item\"")
		raw("execute unless entity ${sel(IN)} run return run function ${cleanup.asId()}")
		raw("execute unless entity ${sel(BOOK)} run data modify entity @s data.status.state set value \"needs a book\"")
		raw("execute unless entity ${sel(BOOK)} run return run function ${cleanup.asId()}")

		// What moves: every enchantment but the curses.
		raw("data modify storage ra:enchant dis set value {move:{},keep:{}}")
		raw("data modify storage ra:enchant dis.move set from entity ${sel(IN)} Item.components.$ENCH")
		for (c in CURSES) {
			raw("data modify storage ra:enchant dis.keep.\"$c\" set from storage ra:enchant dis.move.\"$c\"")
			raw("data remove storage ra:enchant dis.move.\"$c\"")
		}
		raw("execute store result score #dis.n $T run data get storage ra:enchant dis.move")
		raw("execute if score #dis.n $T matches ..0 run data modify entity @s data.status.state set value \"only curses: nothing to move\"")
		raw("execute if score #dis.n $T matches ..0 run tag ${sel(IN)} add $DONE")
		raw("execute if score #dis.n $T matches ..0 run return run function ${cleanup.asId()}")

		// price = cost per enchantment x enchantments moved.
		raw("execute store result score #dis.cost $T run function ${Settings.get.asId()} {key:\"$COST_KEY\",default:$DEFAULT_COST}")
		computeScore("#dis.price", T, provider { mul(scoreOf("#dis.cost", T), scoreOf("#dis.n", T)) }, storeInt(Storage.ENCHANT, "pay.amount"))
		raw("execute store result entity @s data.status.price int 1 run scoreboard players get #dis.price $T")
		raw("scoreboard players set #dis.paid $T 1")
		raw("execute if score #dis.price $T matches 1.. store result score #dis.paid $T run function ${XpPay.pay.asId()}")
		raw("execute if score #dis.paid $T matches 0 run data modify entity @s data.status.state set value \"needs liquid experience\"")
		raw("execute if score #dis.paid $T matches 0 run return run function ${cleanup.asId()}")

		// The book, with what moved.
		raw("summon item ~ ~0.7 ~ {Item:{id:\"minecraft:enchanted_book\",count:1},Tags:[\"ra\",\"ra.ench.product\",\"$DONE\"],PickupDelay:10s}")
		raw("data modify entity @e[type=item,tag=ra.ench.product,distance=..1.5,limit=1,sort=nearest] Item.components.\"minecraft:stored_enchantments\" set from storage ra:enchant dis.move")
		raw("tag @e[type=item,tag=ra.ench.product,distance=..1.5] remove ra.ench.product")

		// The item keeps its curses and nothing else; with none, the component goes altogether.
		raw("data modify entity ${sel(IN)} Item.components.$ENCH set from storage ra:enchant dis.keep")
		raw("execute unless data storage ra:enchant dis.keep.\"${CURSES[0]}\" unless data storage ra:enchant dis.keep.\"${CURSES[1]}\" run data remove entity ${sel(IN)} Item.components.$ENCH")
		raw("data remove entity ${sel(IN)} Item.components.\"minecraft:repair_cost\"")
		raw("tag ${sel(IN)} add $DONE")

		// One book used up.
		raw("execute store result score #dis.books $T run data get entity ${sel(BOOK)} Item.count")
		raw("scoreboard players remove #dis.books $T 1")
		raw("execute if score #dis.books $T matches 1.. store result entity ${sel(BOOK)} Item.count int 1 run scoreboard players get #dis.books $T")
		raw("execute if score #dis.books $T matches ..0 run kill ${sel(BOOK)}")

		raw("data modify entity @s data.status.state set value \"disenchanted\"")
		raw("particle minecraft:enchant ~ ~0.9 ~ 0.3 0.3 0.3 0.8 40 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.grindstone.use block ${soundListenersSel()} ~ ~ ~ 0.8 1.2")
		raw("playsound minecraft:block.enchantment_table.use block ${soundListenersSel()} ~ ~ ~ 0.8 0.9")
		raw("function ${cleanup.asId()}")
	}

	private fun sel(tag: String) = "@e[type=item,tag=$tag,distance=..2,limit=1]"
}
