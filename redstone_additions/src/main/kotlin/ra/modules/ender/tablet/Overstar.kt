package ra.modules.ender.tablet

import io.github.ayfri.kore.commands.returnValue
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.hookEnchantRecipes
import ra.modules.enchanting.sacrificeRecipe

/**
 * Overstar: the ingredient that makes the [AnchorTablet] hard to craft.
 *
 * MADE ON AN ENCHANTING TABLE. A nether star thrown onto an enchanting table is a sacrifice
 * ([ra.modules.enchanting.EnchantingModule]): once a second the table takes one star off the stack,
 * and [CHANCE] times in a hundred it comes back as an Overstar. The rest are gone. No scan of its
 * own: the enchanting module already looks at items on tables every five ticks, and this is one
 * more entry in `#ra_enchanting:recipes`.
 *
 * Only a plain nether star counts. One with any custom data (from this pack or any other) or a
 * custom name is left alone, so nobody loses a renamed star or another pack's item built on one.
 *
 * WHY A TEST INSTANCE BLOCK. A crafting recipe matches ingredients by id alone, so the tablet's
 * recipe needs an id nothing else in the game or the pack uses, and one no survival player can
 * place: `minecraft:test_instance_block` is a game-master block item, like the generator cores'
 * jigsaw and structure block. It wears the nether star model.
 */
object Overstar {
	val give = ref(Ns.ENDER, "items/give_overstar")
	private val enchantRecipes = ref(Ns.ENDER, "enchant_recipes")

	const val BASE = "minecraft:test_instance_block"

	/** Percent, per nether star. */
	const val CHANCE = 10

	val item = itemSpec(BASE) {
		model("minecraft:nether_star")
		name("Overstar")
		rarity("epic")
		glint()
		lore(loreLine("Crafts the Anchor Tablet", "gray"))
		customData(raData(flag("overstar")))
	}

	fun Ra.registerOverstar() {
		pack.define(give) {
			raw("give @s ${item.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Overstar",color:"light_purple"}]""")
		}

		pack.define(enchantRecipes) {
			// Only a plain star: the filter below matches partially, so a star with any custom data
			// (any pack's) or a name would match it too.
			raw("""execute if data storage ra:enchant input.components."minecraft:custom_data" run return 0""")
			raw("""execute if data storage ra:enchant input.components."minecraft:custom_name" run return 0""")
			sacrificeRecipe(
				"{id:\"minecraft:nether_star\"}", item.stackSnbt(), chance = CHANCE,
				won = "The nether star became an Overstar.",
				lost = "The nether star burnt away.",
			)
			returnValue(0)
		}
		pack.hookEnchantRecipes(enchantRecipes)
	}
}
