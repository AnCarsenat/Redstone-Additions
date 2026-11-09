package ra.modules.enchanting

import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.functions.Function
import ra.api.Settings
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.computeScore
import ra.core.define
import ra.core.provider
import ra.core.raw
import ra.core.scoreOf
import ra.core.storeInt
import ra.modules.enchanting.BookPrinter.CHANCE_KEY
import ra.modules.enchanting.BookPrinter.COST_KEY
import ra.modules.enchanting.BookPrinter.DEFAULT_CHANCE
import ra.modules.enchanting.BookPrinter.DEFAULT_COST
import ra.modules.enchanting.BookPrinter.block
import ra.modules.enchanting.EnchantingModule.DONE
import ra.modules.enchanting.EnchantingModule.onTop
import ra.modules.enchanting.schematic.Schematic
import ra.modules.enchanting.xp.XpPay

/**
 * What the [BookPrinter] does with the items thrown on top, run from the module's item scan only
 * while an untagged item lies on it. One action per run.
 *
 * - An enchanted book is drawn ([draw]): used up, a Schematic of it on a hit.
 * - A plain book is printed on ([print]) with the stored Schematic's enchantments, for experience.
 * - Anything else is tagged `ra.ench.done` and ignored from then on, a thrown Schematic included:
 *   a Schematic goes in with a right-click.
 *
 * Also the lore writer ([lore]) shared by a drawn Schematic and the printer's goggles summary.
 */
internal object BookPrinterRun {
	val draw = block.fn("draw")
	val print = block.fn("print")
	val levels = block.fn("levels")
	val lore = block.fn("lore")
	private val loreOne = block.fn("lore_one")
	private val loreAdd = block.fn("lore_add")
	val cleanup = block.fn("cleanup")

	private const val IN = "ra.ench.pr_in"
	private const val BOOK = "ra.ench.pr_book"
	private const val T = Obj.TEMP
	private const val STORED = "\"minecraft:stored_enchantments\""
	/** Where a Schematic keeps the enchantments, under `Item.components`. */
	const val SCH_ENCH = "\"minecraft:custom_data\".ra.enchantments"
	private const val PRODUCT = "@e[type=item,tag=ra.ench.product,distance=..1.5,limit=1,sort=nearest]"

	fun Ra.registerBookPrinterRun() {
		pack.define(BookPrinter.process) { process() }
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
		// The lore line, and the same line as a goggles line (a "\n" before it).
		pack.define(loreAdd) {
			val line = "{translate:\"enchantment.\$(ns).\$(path)\",color:\"gray\",italic:false,extra:[\" \",{translate:\"enchantment.level.\$(lv)\",fallback:\"\$(lv)\"}]}"
			raw("\$data modify storage ra:enchant pr.lore append value $line")
			raw("\$data modify storage ra:enchant pr.gg append value {text:\"\\n\",extra:[$line]}")
		}
		pack.define(cleanup) {
			for (t in listOf(IN, BOOK)) raw("tag @e[type=item,tag=$t,distance=..2] remove $t")
			raw("return 0")
		}
	}

	/** As the marker, at the block, with an untagged item lying on top. */
	private fun Function.process() {
		val at = "execute positioned ~-0.5 ~0.5 ~-0.5"
		// Not an enchanted book or a book: ignored from now on.
		raw("$at as ${onTop("nbt=!{Item:{id:\"minecraft:enchanted_book\"}}", "nbt=!{Item:{id:\"minecraft:book\"}}")} run tag @s add $DONE")
		raw("$at as ${onTop("nbt={Item:{id:\"minecraft:enchanted_book\"}}", "limit=1")} run tag @s add $IN")
		raw("execute if entity ${sel(IN)} run return run function ${draw.asId()}")

		raw("$at as ${onTop("nbt={Item:{id:\"minecraft:book\"}}", "limit=1")} run tag @s add $BOOK")
		raw("execute unless entity ${sel(BOOK)} run return run function ${cleanup.asId()}")
		// The books wait for a Schematic: one right-clicked in later prints them.
		raw("execute unless data entity @s data.data.item run data modify entity @s data.status.state set value \"no Schematic: right-click it with one\"")
		raw("execute unless data entity @s data.data.item run return run function ${cleanup.asId()}")
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

		raw("summon item ~ ~0.7 ~ {Item:${Schematic.item.stackSnbt()},Tags:[\"ra\",\"ra.ench.product\",\"$DONE\"],PickupDelay:10s}")
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

	/** One copy of the stored Schematic (`data.data.item`) printed on one book ([BOOK]), for experience. */
	private fun Function.print() {
		raw("data modify storage ra:enchant pr set value {book:{}}")
		raw("data modify storage ra:enchant pr.book set from entity @s data.data.item.components.$SCH_ENCH")
		raw("execute store result score #pr.n $T run data get storage ra:enchant pr.book")
		raw("execute if score #pr.n $T matches ..0 run data modify entity @s data.status.state set value \"a blank Schematic\"")
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
		raw("data modify entity $PRODUCT Item.components.$STORED set from storage ra:enchant pr.book")
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
	 * `pr.lore`: a lore line per enchantment of `pr.book` (`#pr.n` of them) the game lists, in id
	 * order, and one "other enchantments" line for those it does not (a data pack's own, which still
	 * print). `pr.gg`: the same lines as goggles lines.
	 */
	private fun Function.lore() {
		raw("data modify storage ra:enchant pr.lore set value []")
		raw("data modify storage ra:enchant pr.gg set value []")
		for (id in BookUpgrader.IDS) {
			val (ns, path) = id.split(':', limit = 2)
			raw("execute if data storage ra:enchant pr.book.\"$id\" run function ${loreOne.asId()} {ns:\"$ns\",path:\"$path\"}")
		}
		raw("function ${levels.asId()}")
		val other = "{text:\"+ other enchantments\",color:\"gray\",italic:false}"
		raw("execute if score #pr.other $T matches 1.. run data modify storage ra:enchant pr.lore append value $other")
		raw("execute if score #pr.other $T matches 1.. run data modify storage ra:enchant pr.gg append value {text:\"\\n\",extra:[$other]}")
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
