package ra.modules.enchanting

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.mul
import io.github.ayfri.kore.features.predicates.providers.pow
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
import ra.core.TextFormat
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
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.block.stackedPropLine
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
import ra.core.text
import ra.modules.enchanting.EnchantingModule.DONE
import ra.modules.enchanting.EnchantingModule.onTop
import ra.modules.enchanting.xp.XpPay

/**
 * Book Upgrader: raises one enchantment of a book by one level, for liquid experience.
 *
 * Throw an enchanted book on top. The upgrader picks ONE enchantment of it: the one named by its
 * `enchantment` property when that is set (`sharpness` or `minecraft:sharpness`), else the first
 * one, in id order, that is below the cap. It raises it from level L to L+1 for
 * `base x 2^L` mL of liquid experience, all or nothing, from a fluid network touching the block
 * ([XpPay]). Each step costs double the last, so a book goes up a level or two cheaply and the
 * last levels before the cap are a real project.
 *
 * Settings (global): [BASE_KEY] the base price (default [DEFAULT_BASE] mL), [CAP_KEY] the highest
 * level it makes (default [DEFAULT_CAP], at most 20 so the price stays inside 32 bits).
 *
 * NO PLAYER TEXT IN A COMMAND. The enchantment ids come from Kore's list of the game's
 * enchantments, written into the functions at build time; the property is only ever COMPARED with
 * them (an NBT match), never pasted. The one macro ([apply]) receives an id this pack wrote itself.
 * An enchantment a data pack adds is not in the list, and is never raised.
 *
 * A raised book is tagged `ra.ench.done` and stays on the block: pick it up and throw it back for
 * the next level. Each level is a deliberate step, never a chain that drains a network.
 */
object BookUpgrader {
	val block = CustomBlock(Ns.ENCHANTING, "book_upgrader", "Book Upgrader", model = "minecraft:lapis_block")
	val item = block.loreItem {
		lore(
			loreLine("Raises one enchantment of a book by a level", "gray"),
			loreLine("Throw an enchanted book on top", "dark_gray"),
			loreLine("Each level costs twice the last, in liquid experience", "dark_gray"),
		)
	}

	val process = block.fn("process")
	private val pick = block.fn("pick")
	private val only = block.fn("only")
	private val apply = block.fn("apply")
	private val cleanup = block.fn("cleanup")

	const val BASE_KEY = "book_upgrader_base"
	const val DEFAULT_BASE = 1000
	const val CAP_KEY = "book_upgrader_max"
	const val DEFAULT_CAP = 10
	/** base (at most 1000) x 2^19 still fits in an int. */
	const val MAX_CAP = 20

	private const val IN = "ra.ench.up_in"
	private const val T = Obj.TEMP
	private const val STORED = "\"minecraft:stored_enchantments\""

	/** Every enchantment of the game, by id: the order [pick] tries them in. */
	internal val IDS = Enchantments.entries.map { it.asId() }.sorted().onEach {
		check(it.matches(Regex("[a-z0-9_.-]+:[a-z0-9_./-]+"))) { "enchantment id $it cannot be pasted into a path" }
	}

	fun Ra.registerBookUpgrader() {
		block(block)
		properties(block) {
			// Compared with the game's ids, never pasted: see the class comment.
			text("enchantment", TextFormat.ID, default = "")
		}
		pack.blockRegisterMessage(block)
		pack.define(block.give) { raw("give @s ${item.giveArg()}") }
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ENCHANTING_TABLE)) {
			pattern("LSL", "BEB", "LBL")
			key("L", Items.LAPIS_BLOCK)
			key("S", Items.NETHER_STAR)
			key("B", Items.BOOK)
			key("E", Items.ENCHANTING_TABLE)
		}

		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.enchantment", "\"\"")
			finishPlacement(block)
		}

		pack.define(process) { process() }
		pack.define(pick) { pick() }
		pack.define(only) { only() }
		// As the marker. `key` is one of IDS, `lv` an int: both written by this pack.
		pack.define(apply) {
			raw("\$data modify entity @e[type=item,tag=$IN,distance=..2,limit=1] Item.components.$STORED.\"\$(key)\" set value \$(lv)")
		}
		pack.define(cleanup) {
			raw("tag @e[type=item,tag=$IN,distance=..2] remove $IN")
			raw("return 0")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.2)
			stackedPropLine("enchantment", "Raises: ", "aqua")
			stackedStatusLine("state", "State: ", "aqua")
			stackedStatusLine("price", "Last price: ", "light_purple", " mL")
			stackedTextLine("", "Throw an enchanted book on top", "gray")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.LAPIS_BLOCK)) {}
	}

	/** As the marker, at the block, with an untagged item lying on top. */
	private fun Function.process() {
		val cleanupRun = "return run function ${cleanup.asId()}"

		// Only single enchanted books are for us; anything else is ignored from now on.
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("nbt=!{Item:{id:\"minecraft:enchanted_book\",count:1}}")} run tag @s add $DONE")
		raw("execute positioned ~-0.5 ~0.5 ~-0.5 as ${onTop("limit=1")} run tag @s add $IN")
		raw("execute unless entity $IN_SEL run return 0")

		raw("data modify storage ra:enchant up set value {book:{}}")
		raw("data modify storage ra:enchant up.book set from entity $IN_SEL Item.components.$STORED")
		// The property, when it names something: the book is narrowed to that one enchantment.
		raw("execute unless data entity @s data.properties{enchantment:\"\"} run data modify storage ra:enchant up.want set from entity @s data.properties.enchantment")
		raw("execute if data storage ra:enchant up.want run function ${only.asId()}")

		raw("execute store result score #up.cap $T run function ${Settings.get.asId()} {key:\"$CAP_KEY\",default:$DEFAULT_CAP}")
		raw("execute if score #up.cap $T matches $MAX_CAP.. run scoreboard players set #up.cap $T $MAX_CAP")
		raw("function ${pick.asId()}")
		raw("execute unless data storage ra:enchant up.key run data modify entity @s data.status.state set value \"nothing to raise\"")
		raw("execute unless data storage ra:enchant up.key run tag $IN_SEL add $DONE")
		raw("execute unless data storage ra:enchant up.key run $cleanupRun")

		// price = base x 2^level, in one evaluation. The level is below the cap (at most 19), so
		// it cannot overflow; if it ever did, /compute stores 0 and the guard refuses the book.
		raw("execute store result score #up.base $T run function ${Settings.get.asId()} {key:\"$BASE_KEY\",default:$DEFAULT_BASE}")
		computeScore("#up.price", T, provider { mul(scoreOf("#up.base", T), pow(constant(2), scoreOf("#up.lv", T))) }, storeInt(Storage.ENCHANT, "pay.amount"))
		raw("execute store result entity @s data.status.price int 1 run scoreboard players get #up.price $T")
		raw("execute if score #up.price $T matches ..0 run data modify entity @s data.status.state set value \"price out of range\"")
		raw("execute if score #up.price $T matches ..0 run $cleanupRun")
		raw("execute store result score #up.paid $T run function ${XpPay.pay.asId()}")
		raw("execute if score #up.paid $T matches 0 run data modify entity @s data.status.state set value \"needs liquid experience\"")
		raw("execute if score #up.paid $T matches 0 run $cleanupRun")

		raw("scoreboard players add #up.lv $T 1")
		raw("execute store result storage ra:enchant up.lv int 1 run scoreboard players get #up.lv $T")
		raw("function ${apply.asId()} with storage ra:enchant up")
		raw("tag $IN_SEL add $DONE")

		raw("data modify entity @s data.status.state set value \"upgraded\"")
		raw("particle minecraft:enchant ~ ~0.9 ~ 0.3 0.3 0.3 1 60 normal ${particleViewersSel()}")
		raw("particle minecraft:end_rod ~ ~0.9 ~ 0.15 0.15 0.15 0.05 10 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.enchantment_table.use block ${soundListenersSel()} ~ ~ ~ 1 1.2")
		raw("playsound minecraft:entity.player.levelup block ${soundListenersSel()} ~ ~ ~ 0.6 1.4")
		raw("function ${cleanup.asId()}")
	}

	/**
	 * The first enchantment of `up.book` (in id order) whose level is 1 or more and below the cap:
	 * its id into `up.key`, its level left in `#up.lv`. Lines after the hit are skipped, which is
	 * what leaves `#up.lv` holding the chosen level. A level `data get` cannot read stores 0.
	 */
	private fun Function.pick() {
		for (id in IDS) {
			raw("execute unless data storage ra:enchant up.key store result score #up.lv $T run data get storage ra:enchant up.book.\"$id\"")
			raw("execute unless data storage ra:enchant up.key if score #up.lv $T matches 1.. if score #up.lv $T < #up.cap $T run data modify storage ra:enchant up.key set value \"$id\"")
		}
	}

	/** Narrow `up.book` to the enchantment `up.want` names, with or without `minecraft:`; nothing if none. */
	private fun Function.only() {
		raw("data modify storage ra:enchant up.only set value {}")
		for (id in IDS) {
			val short = id.removePrefix("minecraft:")
			raw("execute if data storage ra:enchant up{want:\"$id\"} run data modify storage ra:enchant up.only.\"$id\" set from storage ra:enchant up.book.\"$id\"")
			if (short != id) raw("execute if data storage ra:enchant up{want:\"$short\"} run data modify storage ra:enchant up.only.\"$id\" set from storage ra:enchant up.book.\"$id\"")
		}
		raw("data modify storage ra:enchant up.book set from storage ra:enchant up.only")
	}

	private const val IN_SEL = "@e[type=item,tag=$IN,distance=..2,limit=1]"
}
