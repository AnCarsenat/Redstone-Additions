package ra.modules.farming.net

import io.github.ayfri.kore.commands.returnValue
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.hookEnchantRecipes
import ra.modules.enchanting.sacrificeRecipe

/**
 * The Master Capture Net comes out of an enchanting table: an empty Capture Net thrown onto one is
 * a sacrifice ([ra.modules.enchanting.EnchantingModule]), [CHANCE] percent a net. A miss uses the
 * net up.
 *
 * Only an EMPTY, ordinary net counts: a full one is left lying there, so no caught mob is ever lost
 * to a roll, and a Master net (epic) is not rolled again. The net is recognised by its custom data,
 * not its model.
 */
object CaptureNetSacrifice {
	private val enchantRecipes = ref(Ns.FARMING, "enchant_recipes")

	/** Percent, per empty Capture Net. */
	const val CHANCE = 35

	fun Ra.registerCaptureNetSacrifice() {
		pack.define(enchantRecipes) {
			// The filter below matches partially: a full net or a Master net would match it too.
			raw("""execute if data storage ra:enchant input.components."minecraft:custom_data".ra.capture_net.mob run return 0""")
			raw("""execute if data storage ra:enchant input.components{"minecraft:rarity":"epic"} run return 0""")
			sacrificeRecipe(
				"""{id:"${CaptureNet.BASE}",components:{"minecraft:custom_data":{ra:{capture_net:{}}}}}""",
				CaptureNet.masterItem.stackSnbt(), chance = CHANCE,
				sacrificeName = "Capture Net (empty)",
				won = "The Capture Net became a Master Capture Net.",
				lost = "The Capture Net burnt away.",
			)
			returnValue(0)
		}
		pack.hookEnchantRecipes(enchantRecipes)
	}
}
