package ra.modules.enchanting

import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
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
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.core.storeInt
import ra.modules.enchanting.EnchantingModule.DONE
import ra.modules.enchanting.EnchantingModule.onTop
import ra.modules.enchanting.xp.XpPay

/**
 * Book Printer: copies enchanted books, once you have their Schematic.
 *
 * DRAWING A SCHEMATIC. Throw an enchanted book on top: it is used up and rolls once. With
 * [CHANCE_KEY] percent (server setting, default [DEFAULT_CHANCE]) a Schematic of it comes out;
 * otherwise the book is simply gone, like a failed sacrifice on the enchanting table. A gamble.
 *
 * PRINTING. Throw a Schematic and plain books on top: twice a second, while books and experience
 * last, one book becomes an enchanted book with the Schematic's enchantments. The Schematic stays.
 * The price is [COST_KEY] mL of liquid experience (default [DEFAULT_COST]) per enchantment level
 * of the copy, all or nothing, from a fluid network beside the block ([XpPay]).
 *
 * THE SCHEMATIC is paper (map look) with `custom_data.ra = {schematic:1b, enchantments:{...}}`:
 * the book's stored enchantments, kept in custom data and not in an enchantment component, so an
 * anvil, a grindstone or the Disenchanter never takes it for an enchanted book. Its lore lists them
 * (translated name and level), written by [lore] from Kore's list. It is not craftable.
 *
 * NO PLAYER TEXT IN A COMMAND. Enchantments are copied compound to compound with `data modify`;
 * the levels and the lore come from Kore's list of the game's enchantments ([BookUpgrader.IDS]),
 * so the macros only ever receive ids and ints this pack wrote.
 *
 * NO IDLE COST. Run from the module's item scan only while an untagged item lies on it; anything
 * it cannot use, and everything it makes, is tagged `ra.ench.done`.
 */
object BookPrinter {
	val block = CustomBlock(Ns.ENCHANTING, "book_printer", "Book Printer", model = "minecraft:fletching_table")
	val item = block.loreItem {
		lore(
			loreLine("Prints copies of enchanted books", "gray"),
			loreLine("Throw an enchanted book on top: a small chance of its Schematic", "dark_gray"),
			loreLine("Throw a Schematic and books on top to print, for liquid experience", "dark_gray"),
		)
	}

	/** The Schematic, before the book's enchantments are copied onto it. */
	val schematic = itemSpec("minecraft:paper") {
		model("minecraft:map")
		name("Schematic")
		rarity("rare")
		glint()
		lore(
			loreLine("Throw it on a Book Printer with books", "dark_gray"),
			loreLine("to print copies of this enchanted book", "dark_gray"),
		)
		customData(raData(flag(SCHEMATIC)))
	}

	val process = block.fn("process")
	private val draw = block.fn("draw")
	private val print = block.fn("print")
	private val levels = block.fn("levels")
	private val lore = block.fn("lore")
	private val loreOne = block.fn("lore_one")
	private val loreAdd = block.fn("lore_add")
	private val cleanup = block.fn("cleanup")

	const val CHANCE_KEY = "book_printer_chance"
	const val DEFAULT_CHANCE = 2
	const val COST_KEY = "book_printer_xp"
	const val DEFAULT_COST = 1000

	/** `custom_data.ra` flag of a Schematic. */
	const val SCHEMATIC = "schematic"

	private const val IN = "ra.ench.pr_in"
	private const val SCH = "ra.ench.pr_sch"
	private const val BOOK = "ra.ench.pr_book"
	private const val T = Obj.TEMP
	private const val STORED = "\"minecraft:stored_enchantments\""
	private const val IS_SCH = "{Item:{components:{\"minecraft:custom_data\":{ra:{$SCHEMATIC:1b}}}}}"
	/** Where a Schematic keeps the enchantments, under `Item.components`. */
	private const val SCH_ENCH = "\"minecraft:custom_data\".ra.enchantments"
	private const val PRODUCT = "@e[type=item,tag=ra.ench.product,distance=..1.5,limit=1,sort=nearest]"

	fun Ra.registerBookPrinter() {
		block(block)
		properties(block) {}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ENCHANTED_BOOK)) {
			pattern("IPI", "BLB", "OOO")
			key("I", Items.INK_SAC)
			key("P", Items.PISTON)
			key("B", Items.BOOK)
			key("L", Items.LECTERN)
			key("O", Items.OBSIDIAN)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			finishPlacement(block)
		}

		pack.define(process) { process() }
		pack.define(draw) { draw() }
		pack.define(print) { print() }
		pack.define(levels) { levels() }
		pack.define(lore) { lore() }
		// `ns`, `path`: one of Kore's ids, written by [lore]. `lv`: an int read from the book.
		pack.define(loreOne) {
			raw("\$data modify storage ra:enchant pr.cur set value {ns:\"\$(ns)\",path:\"\$(path)\"}")
			raw("\$execute store result storage ra:enchant pr.cur.lv int 1 run data get storage ra:enchant pr.book.\"\$(ns):\$(path)\"")
			raw("function ${loreAdd.asId()} with storage ra:enchant pr.cur")
		}
		pack.define(loreAdd) {
			raw("\$data modify storage ra:enchant pr.lore append value {translate:\"enchantment.\$(ns).\$(path)\",color:\"gray\",italic:false,extra:[\" \",{translate:\"enchantment.level.\$(lv)\",fallback:\"\$(lv)\"}]}")
		}
		pack.define(cleanup) {
			for (t in listOf(IN, SCH, BOOK)) raw("tag @e[type=item,tag=$t,distance=..2] remove $t")
			raw("return 0")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("price", "Last price: ", "light_purple", " mL")
			stackedTextLine("", "Throw an enchanted book, or a Schematic and books", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.FLETCHING_TABLE)) {}
	}

	/** As the marker, at the block, with an untagged item lying on top. One action per run. */
	private fun Function.process() {
		val at = "execute positioned ~-0.5 ~0.5 ~-0.5"
		// Not an enchanted book, a book or a Schematic: ignored from now on.
		raw("$at as ${onTop("nbt=!{Item:{id:\"minecraft:enchanted_book\"}}", "nbt=!{Item:{id:\"minecraft:book\"}}", "nbt=!$IS_SCH")} run tag @s add $DONE")
		raw("$at as ${onTop("nbt={Item:{id:\"minecraft:enchanted_book\"}}", "limit=1")} run tag @s add $IN")
		raw("execute if entity ${sel(IN)} run return run function ${draw.asId()}")

		raw("$at as ${onTop("nbt=$IS_SCH", "limit=1")} run tag @s add $SCH")
		raw("$at as ${onTop("nbt={Item:{id:\"minecraft:book\"}}", "limit=1")} run tag @s add $BOOK")
		raw("execute unless entity ${sel(SCH)} run data modify entity @s data.status.state set value \"waiting for an enchanted book or a Schematic\"")
		raw("execute unless entity ${sel(SCH)} run return run function ${cleanup.asId()}")
		raw("execute unless entity ${sel(BOOK)} run data modify entity @s data.status.state set value \"needs a book\"")
		raw("execute unless entity ${sel(BOOK)} run return run function ${cleanup.asId()}")
		raw("function ${print.asId()}")
	}

	/** The gamble: one enchanted book ([IN]) used up, a Schematic of it on a hit. */
	private fun Function.draw() {
		raw("data modify storage ra:enchant pr set value {book:{}}")
		raw("data modify storage ra:enchant pr.book set from entity ${sel(IN)} Item.components.$STORED")
		raw("execute store result score #pr.n $T run data get storage ra:enchant pr.book")
		raw("execute if score #pr.n $T matches ..0 run data modify entity @s data.status.state set value \"a book with no enchantments\"")
		raw("execute if score #pr.n $T matches ..0 run tag ${sel(IN)} add $DONE")
		raw("execute if score #pr.n $T matches ..0 run return run function ${cleanup.asId()}")

		// One book used up, whatever the roll.
		useOne(IN, "#pr.ins")

		raw("execute store result score #pr.chance $T run function ${Settings.get.asId()} {key:\"$CHANCE_KEY\",default:$DEFAULT_CHANCE}")
		raw("execute store result score #pr.roll $T run random value 1..100")
		raw("execute unless score #pr.roll $T <= #pr.chance $T run data modify entity @s data.status.state set value \"the book crumbled\"")
		raw("execute unless score #pr.roll $T <= #pr.chance $T run particle minecraft:smoke ~ ~0.9 ~ 0.2 0.2 0.2 0.02 12 normal ${particleViewersSel()}")
		raw("execute unless score #pr.roll $T <= #pr.chance $T run playsound minecraft:entity.item.break block ${soundListenersSel()} ~ ~ ~ 0.8 0.8")
		raw("execute unless score #pr.roll $T <= #pr.chance $T run return run function ${cleanup.asId()}")

		raw("summon item ~ ~0.7 ~ {Item:${schematic.stackSnbt()},Tags:[\"ra\",\"ra.ench.product\",\"$DONE\"],PickupDelay:10s}")
		raw("data modify entity $PRODUCT Item.components.$SCH_ENCH set from storage ra:enchant pr.book")
		// The lore: one line per enchantment, then the item's own two lines.
		raw("function ${lore.asId()}")
		raw("data modify storage ra:enchant pr.lore append from entity $PRODUCT Item.components.\"minecraft:lore\"[]")
		raw("data modify entity $PRODUCT Item.components.\"minecraft:lore\" set from storage ra:enchant pr.lore")
		raw("tag @e[type=item,tag=ra.ench.product,distance=..1.5] remove ra.ench.product")
		raw("data modify entity @s data.status.state set value \"Schematic drawn!\"")
		raw("particle minecraft:enchant ~ ~0.9 ~ 0.3 0.3 0.3 1 60 normal ${particleViewersSel()}")
		raw("particle minecraft:end_rod ~ ~0.9 ~ 0.15 0.15 0.15 0.05 10 normal ${particleViewersSel()}")
		raw("playsound minecraft:ui.cartography_table.take_result block ${soundListenersSel()} ~ ~ ~ 1 1")
		raw("playsound minecraft:entity.player.levelup block ${soundListenersSel()} ~ ~ ~ 0.8 1.4")
		raw("function ${cleanup.asId()}")
	}

	/** One copy of the Schematic ([SCH]) printed on one book ([BOOK]), for experience. */
	private fun Function.print() {
		raw("data modify storage ra:enchant pr set value {book:{}}")
		raw("data modify storage ra:enchant pr.book set from entity ${sel(SCH)} Item.components.$SCH_ENCH")
		raw("execute store result score #pr.n $T run data get storage ra:enchant pr.book")
		raw("execute if score #pr.n $T matches ..0 run data modify entity @s data.status.state set value \"a blank Schematic\"")
		raw("execute if score #pr.n $T matches ..0 run tag ${sel(SCH)} add $DONE")
		raw("execute if score #pr.n $T matches ..0 run return run function ${cleanup.asId()}")

		// price = cost per level x levels of the copy, all or nothing.
		raw("function ${levels.asId()}")
		raw("execute store result score #pr.cost $T run function ${Settings.get.asId()} {key:\"$COST_KEY\",default:$DEFAULT_COST}")
		computeScore("#pr.price", T, provider { mul(scoreOf("#pr.cost", T), scoreOf("#pr.lv", T)) }, storeInt(Storage.ENCHANT, "pay.amount"))
		raw("execute store result entity @s data.status.price int 1 run scoreboard players get #pr.price $T")
		// A price that failed to compute reads 0: refused, never free (unless the cost is 0).
		raw("execute if score #pr.cost $T matches 1.. if score #pr.price $T matches ..0 run data modify entity @s data.status.state set value \"price out of range\"")
		raw("execute if score #pr.cost $T matches 1.. if score #pr.price $T matches ..0 run return run function ${cleanup.asId()}")
		raw("scoreboard players set #pr.paid $T 1")
		raw("execute if score #pr.price $T matches 1.. store result score #pr.paid $T run function ${XpPay.pay.asId()}")
		raw("execute if score #pr.paid $T matches 0 run data modify entity @s data.status.state set value \"needs liquid experience\"")
		raw("execute if score #pr.paid $T matches 0 run return run function ${cleanup.asId()}")

		raw("summon item ~ ~0.7 ~ {Item:{id:\"minecraft:enchanted_book\",count:1},Tags:[\"ra\",\"ra.ench.product\",\"$DONE\"],PickupDelay:10s}")
		raw("data modify entity @e[type=item,tag=ra.ench.product,distance=..1.5,limit=1,sort=nearest] Item.components.$STORED set from storage ra:enchant pr.book")
		raw("tag @e[type=item,tag=ra.ench.product,distance=..1.5] remove ra.ench.product")
		useOne(BOOK, "#pr.books")

		raw("data modify entity @s data.status.state set value \"printed\"")
		raw("particle minecraft:enchant ~ ~0.9 ~ 0.3 0.3 0.3 0.8 30 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.piston.extend block ${soundListenersSel()} ~ ~ ~ 0.5 1.4")
		raw("playsound minecraft:ui.cartography_table.take_result block ${soundListenersSel()} ~ ~ ~ 0.8 1.2")
		raw("function ${cleanup.asId()}")
	}

	/**
	 * `#pr.lv`: the levels of `pr.book` added up. An enchantment the game does not list (a data
	 * pack's own) counts as one level: `#pr.n` minus the ones found in the list.
	 */
	private fun Function.levels() {
		raw("scoreboard players set #pr.lv $T 0")
		raw("scoreboard players operation #pr.other $T = #pr.n $T")
		for (id in BookUpgrader.IDS) {
			raw("execute store result score #pr.l $T run data get storage ra:enchant pr.book.\"$id\"")
			raw("scoreboard players operation #pr.lv $T += #pr.l $T")
			raw("execute if data storage ra:enchant pr.book.\"$id\" run scoreboard players remove #pr.other $T 1")
		}
		raw("execute if score #pr.other $T matches 1.. run scoreboard players operation #pr.lv $T += #pr.other $T")
	}

	/**
	 * `pr.lore`: a lore line per enchantment of `pr.book` the game lists, in id order, and one
	 * "other enchantments" line for those it does not (a data pack's own, which still print).
	 */
	private fun Function.lore() {
		raw("data modify storage ra:enchant pr.lore set value []")
		for (id in BookUpgrader.IDS) {
			val (ns, path) = id.split(':', limit = 2)
			raw("execute if data storage ra:enchant pr.book.\"$id\" run function ${loreOne.asId()} {ns:\"$ns\",path:\"$path\"}")
		}
		raw("function ${levels.asId()}")
		raw("execute if score #pr.other $T matches 1.. run data modify storage ra:enchant pr.lore append value {text:\"+ other enchantments\",color:\"gray\",italic:false}")
	}

	/** One item off the stack tagged [tag]; the entity goes when it was the last. */
	private fun Function.useOne(tag: String, score: String) {
		raw("execute store result score $score $T run data get entity ${sel(tag)} Item.count")
		raw("scoreboard players remove $score $T 1")
		raw("execute if score $score $T matches 1.. store result entity ${sel(tag)} Item.count int 1 run scoreboard players get $score $T")
		raw("execute if score $score $T matches ..0 run kill ${sel(tag)}")
	}

	private fun sel(tag: String) = "@e[type=item,tag=$tag,distance=..2,limit=1]"
}
