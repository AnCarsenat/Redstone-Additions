package ra.modules.jetpacks

import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.features.recipes.types.CraftingShaped
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.enchanting.sacrificeRecipe
import io.github.ayfri.kore.commands.returnValue
import ra.core.item.ItemSpec
import ra.core.item.SBool
import ra.core.item.SByte
import ra.core.item.SFloat
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.SComp
import ra.core.item.SList
import ra.core.item.comp
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData

/**
 * The kits: the two that turn a chestplate into a jetpack (iron, infinite) and the three
 * upgrades that go on top of one ([Upgrade]).
 *
 * Every kit is a firework star with a food/consumable pair: that pair is what makes a right-click
 * register at all, exactly as it does for the Wrench (see ra:tools/wrench/give). The click is
 * picked up by the `kit_use` advancement.
 */
object Kits {
	/** One upgrade kit and everything keyed on it: its chestplate flag, menu row and lore line. */
	data class Upgrade(
		val kit: String,
		/** Bit in the fitted state, see kit/read_fitted. */
		val bit: Int,
		val name: String,
		val model: String,
		val description: String,
		/** Action-bar hint once fitted. */
		val hint: String,
		/** Line the fitted jetpack's lore gets, and its colour. */
		val fittedLore: String,
		val fittedColor: String,
		/** /trigger ra.jp.kits values of its menu buttons. */
		val toggle: Int,
		val drop: Int,
	) {
		/** custom_data flag on a chestplate that has it fitted. */
		val key get() = "jp_$kit"

		/** Player tag: switched off by its wearer. */
		val mute get() = "ra.jp.mute_$kit"

		/** Player tag: fitted and switched on, re-derived every tick (see flight/tick_player). */
		val active get() = "ra.jp.kit_$kit"

		val item = kitItem(model, "$name Kit", "rare", raData(flag("jetpack_kit"), "upgrade" to SStr(kit)),
			loreLine("RMB while wearing a jetpack to fit it", "gray"), loreLine(description, "gray"))

		val give = ref(Ns.JETPACKS, "items/give_${kit}_kit")
	}

	val speed = Upgrade("speed", 1, "Thruster", "minecraft:firework_rocket", "Moves you faster in a straight line",
		"faster in a straight line", "Thruster Kit - +45% speed while flying", "gold", 11, 21)
	val lift = Upgrade("lift", 2, "Lift", "minecraft:phantom_membrane", "Climbs and sinks about twice as fast",
		"climbs and sinks faster", "Lift Kit - climbs and sinks twice as fast", "aqua", 12, 22)
	val scorch = Upgrade("scorch", 4, "Scorch", "minecraft:blaze_powder", "Sets fire to anything under your exhaust",
		"burns what flies under you", "Scorch Kit - burns what flies under you", "red", 13, 23)

	/** In bit order. */
	val upgrades = listOf(speed, lift, scorch)

	private val modeHint = loreLine("/trigger ra.jp.mode switches classic / hover", "dark_gray")
	private val soundHint = loreLine("/trigger ra.jp.sound mutes the engine", "dark_gray")

	val ironKit = kitItem("minecraft:elytra", "Iron Jetpack Kit", "rare", raData(flag("jetpack_kit"), "tier" to SStr("iron")),
		loreLine("RMB while wearing a chestplate to fit it", "gray"), loreLine("Burns 1 coal every 2 minutes of flight", "gray"), modeHint, soundHint)

	/**
	 * In survival this is not crafted: it is won by sacrificing an Iron Jetpack Kit on an
	 * enchanting table -- see [enchantRecipes].
	 */
	val infiniteKit = kitItem("minecraft:elytra", "Infinite Iron Jetpack Kit", "epic", raData(flag("jetpack_kit"), "tier" to SStr("infinite")),
		loreLine("RMB while wearing a chestplate to fit it", "gray"), loreLine("Burns nothing at all", "gray"), modeHint, soundHint)

	val giveIron = ref(Ns.JETPACKS, "items/give_iron_kit")
	val giveInfinite = ref(Ns.JETPACKS, "items/give_infinite_kit")
	val enchantRecipes = ref(Ns.JETPACKS, "enchant_recipes")

	/** The iron and infinite kits as the bundle and the enchanting table hand them out: without the sound hint. */
	fun withoutSoundHint(kit: ItemSpec) = kit.replace("lore", SList(
		(kit.components.first { it.name == "lore" }.give as SList).items.filterNot { it == soundHint }
	))

	fun Ra.registerKits() {
		pack.define(giveIron) { giveKit(ironKit, "Iron Jetpack Kit", "aqua") }
		pack.define(giveInfinite) { giveKit(infiniteKit, "Infinite Iron Jetpack Kit", "light_purple") }
		for (u in upgrades) pack.define(u.give) { giveKit(u.item, "${u.name} Kit", "aqua") }

		kitRecipe("iron_jetpack_kit", ironKit, Items.BLAZE_ROD, count = 1) {
			pattern("IBI", "IRI", "F F")
			key("I", Items.IRON_INGOT)
			key("B", Items.BLAZE_ROD)
			key("R", Items.REDSTONE_BLOCK)
			key("F", Items.FIRE_CHARGE)
		}
		// The upgrade kits' recipes produce the kit without its lore, as the beet pack did.
		kitRecipe("speed_kit", speed.item.without("lore"), Items.WIND_CHARGE) {
			pattern("FWF", "WRW", "FWF")
			key("F", Items.FIREWORK_ROCKET)
			key("W", Items.WIND_CHARGE)
			key("R", Items.REDSTONE_BLOCK)
		}
		kitRecipe("lift_kit", lift.item.without("lore"), Items.PHANTOM_MEMBRANE) {
			pattern("PMP", "MRM", "PMP")
			key("P", Items.PHANTOM_MEMBRANE)
			key("M", Items.SHULKER_SHELL)
			key("R", Items.REDSTONE_BLOCK)
		}
		kitRecipe("scorch_kit", scorch.item.without("lore"), Items.BLAZE_POWDER) {
			pattern("BLB", "LRL", "BLB")
			key("B", Items.BLAZE_POWDER)
			key("L", Items.LAVA_BUCKET)
			key("R", Items.REDSTONE_BLOCK)
		}

		// Sacrifice recipes, listed in #ra_enchanting:recipes: Iron Jetpack Kit -> Infinite Iron
		// Jetpack Kit, 1%. The beet list said 10, but the tag order overrode it to 1 and that is
		// the rate players have had; 1% is the intended rate.
		pack.define(enchantRecipes) {
			sacrificeRecipe(
				"{components:{\"minecraft:custom_data\":{ra:{jetpack_kit:1b,tier:\"iron\"}}}}",
				withoutSoundHint(infiniteKit).stackSnbt(),
				chance = 1,
				sacrificeName = "Iron Jetpack Kit",
			)
			returnValue(0)
		}
	}

	private fun Function.giveKit(kit: ItemSpec, name: String, color: String) {
		raw("give @s ${kit.giveArg()}")
		raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"$name",color:"$color"}]""")
	}

	private fun Ra.kitRecipe(
		name: String,
		kit: ItemSpec,
		unlockedBy: ItemArgument,
		count: Short? = null,
		shape: CraftingShaped.() -> Unit,
	) {
		pack.shapedRecipe(Ns.JETPACKS, name, kit.recipeResult(Items.FIREWORK_STAR), Unlock(unlockedBy), RecipeCategory.EQUIPMENT, count, shape = shape)
	}
}

/** A kit: firework star, glint, and the food/consumable pair that makes right-click register. */
private fun kitItem(model: String, name: String, rarity: String, data: SComp, vararg lore: SComp) =
	itemSpec("minecraft:firework_star") {
		model(model)
		name(name)
		rarity(rarity)
		glint()
		lore(*lore)
		customData(data)
		// Spelled differently in a give argument and in an item stack, as the beet pack had them.
		add(
			"food",
			comp("nutrition" to SInt(0), "saturation" to SFloat(0f, "0"), "can_always_eat" to SBool(true)),
			comp("nutrition" to SInt(0), "saturation" to SFloat(0f, "0.0f"), "can_always_eat" to SByte(true)),
		)
		add(
			"consumable",
			comp("consume_seconds" to SFloat(1_000_000f, "1000000"), "animation" to SStr("none"), "has_consume_particles" to SBool(false)),
			comp("consume_seconds" to SFloat(1_000_000f, "1000000.0f"), "animation" to SStr("none"), "has_consume_particles" to SByte(false)),
		)
	}
