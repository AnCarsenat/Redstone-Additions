package ra.modules.multiblock.upgradeplatform

import io.github.ayfri.kore.commands.function
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * Upgrade Platform recipes. Input: `storage ra:temp upgrade.input` (the dropped item); output:
 * `storage ra:temp upgrade.result` and its success `upgrade.chance` (percent), absent on no match.
 */
object UpgradeRecipes {
	val matchAll = ref(Ns.MULTIBLOCK, "upgrade_platform/recipes/match_all")
	val tierUpgrades = ref(Ns.MULTIBLOCK, "upgrade_platform/recipes/tier_upgrades")

	/** Material tier upgrades: input -> result, each at 10%. */
	private val tiers = listOf(
		// Ingots.
		"copper_ingot" to "iron_ingot",
		"iron_ingot" to "gold_ingot",
		"gold_ingot" to "diamond",
		"diamond" to "netherite_scrap",
		// Raw ores.
		"raw_copper" to "raw_iron",
		"raw_iron" to "raw_gold",
		// Gems.
		"coal" to "lapis_lazuli",
		"lapis_lazuli" to "redstone",
		"redstone" to "emerald",
		"emerald" to "amethyst_shard",
	)

	private const val CHANCE = 10

	fun Ra.registerRecipes() {
		pack.define(matchAll) {
			raw("data remove storage ra:temp upgrade.chance")
			function(tierUpgrades)
			raw("execute if data storage ra:temp upgrade.result run return 1")
			raw("return 0")
		}
		pack.define(tierUpgrades) {
			for ((input, result) in tiers) {
				raw("""execute if data storage ra:temp upgrade.input{id:"minecraft:$input"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:$result",count:1}""")
				raw("execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value $CHANCE")
				raw("execute if data storage ra:temp upgrade.result run return 1")
			}
		}
	}
}
