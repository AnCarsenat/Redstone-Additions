package ra.modules.enchanting

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.Goggles
import ra.core.Ns
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.TagRoute
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
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedStatusLine
import ra.core.block.stackedTextLine
import ra.core.define
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.modules.enchanting.BookPrinterRun.SCH_ENCH
import ra.modules.enchanting.BookPrinterRun.registerBookPrinterRun
import ra.modules.enchanting.schematic.Schematic
import ra.modules.enchanting.schematic.Schematic.registerSchematic

/**
 * Book Printer: copies enchanted books, once you have their Schematic.
 *
 * DRAWING A SCHEMATIC. Throw an enchanted book on top: it is used up and rolls once. With
 * [CHANCE_KEY] percent (server setting, default [DEFAULT_CHANCE]) a [Schematic] of it comes out;
 * otherwise the book is simply gone, like a failed sacrifice on the enchanting table. A gamble.
 *
 * SETTING THE TARGET. Right-click the printer with a Schematic ([insert]): one is taken from the
 * hand and kept in the block (`data.data.item`); the one it held comes back to the player. A plain
 * wrench click gives it back ([eject]); breaking the printer drops it.
 *
 * PRINTING. Throw plain books on top: twice a second, while books and experience last, one book
 * becomes an enchanted book with the stored Schematic's enchantments. The price is [COST_KEY] mL of
 * liquid experience (default [DEFAULT_COST]) per enchantment level of the copy, all or nothing,
 * from a fluid network beside the block ([ra.modules.enchanting.xp.XpPay]). See [BookPrinterRun].
 *
 * DATA. `data.properties.schematic`: whether a Schematic is set (read-only, for the Data Handler).
 * `data.data.item`: the Schematic itself. `data.data.summary`: its enchantments as goggles lines.
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
			loreLine("Right-click it with a Schematic, then throw books on top", "dark_gray"),
		)
	}

	val process = block.fn("process")
	/** As the marker at the block, the player tagged [Schematic.USER] holding a Schematic. */
	val insert = block.fn("insert")
	/** Wrench (plain RMB): the Schematic back out. As the marker, at the block. */
	private val eject = block.fn("eject")
	/** As the marker: drop `data.data.item` at `~ ~0.7 ~` and forget it. The item keeps [OUT]: the caller removes it. */
	private val dropSchematic = block.fn("drop_schematic")

	const val CHANCE_KEY = "book_printer_chance"
	const val DEFAULT_CHANCE = 2
	const val COST_KEY = "book_printer_xp"
	const val DEFAULT_COST = 2500

	/** Item entities this block just spawned, before their stack is filled in. */
	private const val OUT = "ra.ench.pr_out"
	private const val T = Obj.TEMP
	private const val USER = "@a[tag=${Schematic.USER},limit=1]"
	private const val untagOut = "tag @e[type=minecraft:item,tag=$OUT] remove $OUT"

	fun Ra.registerBookPrinter() {
		block(block)
		properties(block) {
			bool("schematic", readonly = true, goggles = false, default = false)
		}
		wrenchToggles += TagRoute(block.tag, eject)
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
			setOnNew(block, "data.properties", "{schematic:0b}")
			setOnNew(block, "data.status", """{state:"no Schematic: right-click it with one"}""")
			finishPlacement(block)
		}

		registerSchematic()
		registerBookPrinterRun()
		pack.define(insert) { insert() }
		pack.define(eject) {
			raw("""execute unless data entity @s data.data.item run return run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"no Schematic set",color:"yellow"}]""")
			raw("function ${dropSchematic.asId()}")
			raw(untagOut)
			raw("data modify entity @s data.status.state set value \"no Schematic: right-click it with one\"")
			raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{text:"Schematic taken out",color:"aqua"}]""")
			raw("playsound minecraft:item.book.page_turn block ${soundListenersSel(10)} ~ ~ ~ 0.7 1.0")
		}
		pack.define(dropSchematic) {
			raw("""summon minecraft:item ~ ~0.7 ~ {Item:{id:"minecraft:paper",count:1},Tags:["$OUT","${EnchantingModule.DONE}"],PickupDelay:10s}""")
			raw("data modify entity @n[type=minecraft:item,tag=$OUT,distance=..1.5] Item set from entity @s data.data.item")
			raw("data remove entity @s data.data.item")
			raw("data remove entity @s data.data.summary")
			raw("data modify entity @s data.properties.schematic set value 0b")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("price", "Last price: ", "light_purple", " mL")
			raw("""execute unless data entity @s data.data.item run function ${Goggles.Billboard.stackedTextLine.asId()} {label:"Schematic: ",value:"none",color:"red",suffix:""}""")
			raw("""execute if data entity @s data.data.item run function ${Goggles.Billboard.stackedTextLine.asId()} {label:"Prints:",value:"",color:"gray",suffix:""}""")
			// The stored Schematic's enchantments, one line each: each entry starts with its own "\n".
			raw("data modify storage ra:temp gg.text append from entity @s data.data.summary[]")
			stackedTextLine("", "Right-click with a Schematic, throw books on top", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND) {
			raw("execute if data entity @s data.data.item run function ${dropSchematic.asId()}")
			raw(untagOut)
		}
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.FLETCHING_TABLE)) {}
	}

	/** As the marker at the block, the player tagged [Schematic.USER]: swap their Schematic for ours. */
	private fun Function.insert() {
		raw("scoreboard players set ${Schematic.HIT} $T 1")
		raw("data modify storage ra:enchant pr set value {book:{}}")
		raw("data modify storage ra:enchant pr.new set from entity $USER SelectedItem")
		raw("data modify storage ra:enchant pr.book set from storage ra:enchant pr.new.components.$SCH_ENCH")
		raw("execute store result score #pr.n $T run data get storage ra:enchant pr.book")
		raw("""execute if score #pr.n $T matches ..0 run return run tellraw $USER [$RA_CHAT_PREFIX,{text:"This Schematic is blank: it has nothing to print.",color:"red"}]""")

		// The Schematic it held goes back to the player, at their feet with no pickup delay.
		raw("execute if data entity @s data.data.item at $USER run function ${dropSchematic.asId()}")
		raw("data modify entity @e[type=minecraft:item,tag=$OUT,limit=1] PickupDelay set value 0s")
		raw(untagOut)
		// One off the stack: Schematics of the same book stack.
		raw("""item modify entity $USER weapon.mainhand {type:"minecraft:set_count",count:-1,add:true}""")

		raw("data modify entity @s data.data.item set from storage ra:enchant pr.new")
		raw("data modify entity @s data.data.item.count set value 1")
		raw("function ${BookPrinterRun.lore.asId()}")
		raw("data modify entity @s data.data.summary set from storage ra:enchant pr.gg")
		raw("data modify entity @s data.properties.schematic set value 1b")
		raw("data modify entity @s data.status.state set value \"ready: throw books on top\"")
		raw("data remove storage ra:enchant pr")
		raw("""tellraw $USER [$RA_CHAT_PREFIX,{text:"Schematic set: the Book Printer now prints this book.",color:"green"}]""")
		raw("playsound minecraft:ui.cartography_table.take_result block ${soundListenersSel(10)} ~ ~ ~ 0.7 1.0")
	}
}
