package ra.modules.jetpacks.kit

import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.features.itemmodifiers.functions.setComponents
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import ra.core.Ns
import ra.core.Ra
import ra.modules.infinite.SByte
import ra.modules.infinite.SList
import ra.modules.infinite.SStr
import ra.modules.infinite.flag
import ra.modules.infinite.loreLine
import ra.modules.infinite.raData
import ra.modules.jetpacks.Kits

/**
 * `ra_jetpacks:fit_<tier>_<n>`: the chestplate's whole jetpack state, one file per reachable state.
 *
 * WHY SIXTEEN GENERATED ITEM MODIFIERS
 * An item modifier is a static JSON file: it cannot read what is already on the item, and
 * `set_components` replaces a component whole rather than merging into it. Writing "add the
 * Scorch flag" is therefore not expressible -- the only thing that can be expressed is "the item
 * now has exactly this custom_data and exactly this lore".
 *
 * So every reachable state gets its own file: two tiers times eight upgrade combinations. They
 * are generated rather than hand-written, the state is a three-bit number ([Kits.Upgrade.bit]),
 * and the file is chosen by pasting that number into the modifier's name (kit/apply_fit). Adding
 * a fourth kit doubles the count, which is the honest cost of the item being the record instead
 * of the player.
 */
object FitModifiers {
	/** A jetpack tier: its name in `jetpack_tier` and the first two lore lines. */
	data class Tier(val id: String, val title: String, val titleColor: String, val fuel: String)

	val tiers = listOf(
		Tier("iron", "Iron Jetpack", "gold", "Sneak to fly - 1 coal per 2 minutes"),
		Tier("infinite", "Infinite Iron Jetpack", "light_purple", "Sneak to fly - burns nothing"),
	)

	private val states = 0 until (1 shl Kits.upgrades.size)

	fun Ra.registerFitModifiers() {
		for (tier in tiers) for (n in states) {
			val fitted = Kits.upgrades.filter { n and it.bit != 0 }

			val data = raData(
				flag("jetpack"),
				"jetpack_tier" to SStr(tier.id),
				*fitted.map { flag(it.key) }.toTypedArray(),
			)
			val lore = buildList {
				add(loreLine(tier.title, tier.titleColor))
				add(loreLine(tier.fuel, "gray"))
				for (u in fitted) add(loreLine(u.fittedLore, u.fittedColor))
				if (fitted.isNotEmpty()) add(loreLine("/trigger ra.jp.kits manages the kits", "dark_gray"))
				add(loreLine("/trigger ra.jp.mode switches classic / hover", "dark_gray"))
				add(loreLine("/trigger ra.jp.sound mutes the engine", "dark_gray"))
			}

			pack.itemModifier("fit_${tier.id}_$n") {
				namespace = Ns.JETPACKS.id
				setComponents {
					this["custom_data"] = CustomComponent(data.nbt())
					this["enchantment_glint_override"] = CustomComponent(SByte(true).nbt())
					this["lore"] = CustomComponent(SList(lore).nbt())
				}
			}
		}
	}
}
