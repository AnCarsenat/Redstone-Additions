package ra.modules.farming.net

import io.github.ayfri.kore.features.itemmodifiers.functions.copyCustomData
import io.github.ayfri.kore.features.itemmodifiers.functions.replace
import io.github.ayfri.kore.features.itemmodifiers.itemModifier
import io.github.ayfri.kore.features.recipes.recipesBuilder
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.craftingTransmute
import io.github.ayfri.kore.features.recipes.types.input
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.material
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.features.recipes.types.result
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.block.Unlock
import ra.core.block.recipeResult
import ra.core.block.recipeUnlock
import ra.core.block.shapedRecipe
import ra.core.define
import ra.core.item.ItemSpec
import ra.core.item.SBool
import ra.core.item.SInt
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import ra.core.raw
import ra.core.ref
import ra.modules.farming.net.CaptureNetMobs.registerCaptureNetMobs
import ra.modules.farming.net.CaptureNetUse.registerCaptureNetUse

/**
 * Capture Net: an item that catches a mob into itself, to carry it, and lets it out again on a block.
 *
 * TWO NETS. The Capture Net catches the animals in [CaptureNetMobs.SMALL]; the Master Capture Net
 * (a Capture Net upgraded with a nether star) catches any mob in [CaptureNetMobs.ANY], which is
 * every mob but the wither, the ender dragon and players.
 *
 * WHAT IDENTIFIES ONE. Both are a rabbit hide wearing the bundle model ([NET]): the item model is
 * on every net, whichever way it was made. The tier is its rarity ([MASTER], epic). The upgrade is
 * a `crafting_transmute` recipe, the vanilla recipe that keeps an item's components while changing
 * a few (it dyes shulker boxes without emptying them), so a full net keeps its mob through it. Its
 * result patch sets the name, the rarity and the item model, never `custom_data`, which holds the
 * mob.
 *
 * DATA FORMAT (saved items keep it):
 *
 * ```
 * custom_data: {ra:{capture_net:{}}}                                   empty
 * custom_data: {ra:{capture_net:{mob:{id:"minecraft:cow",             full: the mob's type,
 *                                     type:{translate:"entity.minecraft.cow"},   its name for the lore,
 *                                     data:{Health:10.0f,...}}}}}       and its entity data
 * ```
 *
 * `data` is the entity's own data as `/data get entity` reads it, without its UUID, position and
 * motion and the other fields listed in [CaptureNetUse]. Villagers keep their trades, tamed animals
 * their owner, everything its name, health and equipment. `id` comes from a fixed list of types;
 * an edited item can hold anything, so it is checked (`ra_lib:safe/check`, format ID) before it is
 * pasted into the one `summon` that lets the mob out.
 *
 * One net holds one mob, so nets do not stack.
 */
object CaptureNet {
	private fun fn(path: String) = ref(Ns.FARMING, "capture_net/$path")

	val give = fn("give")
	val giveMaster = fn("give_master")

	/** A rabbit hide, not a bundle: a real bundle has its own right-click and inventory behaviour. */
	const val BASE = "minecraft:rabbit_hide"
	const val MODEL = "minecraft:bundle"

	/** What makes an item a Capture Net, as an item-test component: its model. */
	private const val NET_COMPONENT = """minecraft:item_model="$MODEL""""

	/** With [net]: a Master Capture Net. */
	const val MASTER = """minecraft:rarity="epic""""

	/** With [net]: a net that holds a mob. */
	const val FULL = """minecraft:custom_data~{ra:{capture_net:{mob:{}}}}"""

	/** An item test for a Capture Net, plus [extra] component tests, e.g. `net(FULL)`. */
	fun net(vararg extra: String) = BASE + (listOf(NET_COMPONENT) + extra).joinToString(",", "[", "]")

	/** An item test for any Capture Net, empty or full. */
	val NET = net()

	/** `storage ra:farming net`: the scratch the net functions write through. */
	const val STORE = "ra:farming"

	/** The endless `consumable` that makes a right-click fire `using_item`, as on the Pattern. */
	private val useComponents = listOf(
		"food" to comp("nutrition" to SInt(0), "saturation" to SInt(0), "can_always_eat" to SBool(true)),
		"consumable" to comp("consume_seconds" to SInt(1000000), "animation" to SStr("none"), "has_consume_particles" to SBool(false)),
	)

	/** The lore of an empty net, either tier, as SNBT compounds. */
	fun emptyLore() = listOf(
		loreLine("Empty", "dark_gray"),
		loreLine("Right-click a mob to catch it", "gray"),
	)

	val item: ItemSpec = itemSpec(BASE) {
		model(MODEL)
		name("Capture Net")
		rarity("uncommon")
		add("max_stack_size", SInt(1))
		customData(raData("capture_net" to comp()))
		lore(*emptyLore().toTypedArray())
		for ((k, v) in useComponents) add(k, v)
	}

	/** A fresh Master Capture Net, for `give`. The upgrade recipe writes only [masterPatch]. */
	val masterItem: ItemSpec = item.replace("item_name", SStr("Master Capture Net")).replace("rarity", SStr("epic"))

	/** What the upgrade changes: name, rarity, and what makes it a net at all (so any rabbit hide upgrades). */
	private val masterPatch = itemSpec(BASE) {
		model(MODEL)
		name("Master Capture Net")
		rarity("epic")
		add("max_stack_size", SInt(1))
		for ((k, v) in useComponents) add(k, v)
	}

	/** `ra_farming:capture_net/fill`: copies `storage ra:farming net.mob` into the held net. */
	const val FILL_MODIFIER = "ra_farming:capture_net/fill"

	fun Ra.registerCaptureNet() {
		pack.define(give) { raw("give @s ${item.giveArg()}") }
		pack.define(giveMaster) { raw("give @s ${masterItem.giveArg()}") }

		// Early: a fishing-rod shape, string for the net on two sticks.
		pack.shapedRecipe(Ns.FARMING, "capture_net", item.recipeResult(Items.RABBIT_HIDE), Unlock(Items.STRING), RecipeCategory.EQUIPMENT) {
			pattern(" SS", " TS", "T  ")
			key("S", Items.STRING)
			key("T", Items.STICK)
		}

		// End: the upgrade, a net plus a nether star. Transmute keeps the net's components, so a
		// full net keeps its mob; the patch never touches custom_data.
		val upgrade = pack.recipesBuilder.craftingTransmute("master_capture_net", Ns.FARMING.id) {
			category = RecipeCategory.EQUIPMENT
			showNotification = true
			input(Items.RABBIT_HIDE)
			material(Items.NETHER_STAR)
			result(Items.RABBIT_HIDE, null) { masterPatch.applyTo(this) }
		}
		pack.recipeUnlock("master_capture_net", Ns.FARMING, upgrade, Unlock(Items.NETHER_STAR))

		pack.itemModifier("capture_net/fill") {
			namespace = Ns.FARMING.id
			copyCustomData(STORE) { replace("net.mob", "ra.capture_net.mob") }
		}

		registerCaptureNetMobs()
		registerCaptureNetUse()
	}
}
