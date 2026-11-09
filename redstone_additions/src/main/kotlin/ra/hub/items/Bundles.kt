package ra.hub.items

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.toSnbt
import ra.core.GiveBundle
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raMessage
import ra.core.raw
import ra.core.ref

/**
 * `/function ra:give_all_items`: every module's give bundle, built from [Ra.bundles].
 *
 * Each module registers one [GiveBundle]; this writes `ra:items/bundles/give_<id>_bundle` for it
 * and `ra:items/bundles/give_all` calling them all.
 */
internal val giveAllItems = ref(Ns.RA, "give_all_items")
private val giveAll = ref(Ns.RA, "items/bundles/give_all")

/**
 * The order beet handed bundles out in (tools first, sensors after gates). Registration order
 * differs -- the tools bundle registers last because ToolsPart runs last -- and the order is what
 * a player sees in their hotbar, so bundles are sorted by this list. A bundle not in it is handed
 * out after these, in registration order.
 */
private val HAND_OUT_ORDER = listOf(
	"tools", "interactive", "storage", "gates", "sensors", "wireless", "wires",
	"chunk_loader", "multiblock", "infinite", "jetpacks", "ender", "displays",
)

/**
 * What [GiveBundle] cannot express yet, kept so the generated functions match beet: the tools
 * bundle's second lore line and `ra_tools` label (it has no namespace of its own), and the line
 * the Ender bundle prints after giving.
 */
private val EXTRA_LORE = mapOf(
	"tools" to """{text:"Use this to avoid inventory clutter",color:"dark_gray",italic:false}""",
)
private val NAMESPACE_LABEL = mapOf("tools" to "ra_tools")
private val AFTER_GIVE = mapOf<String, Function.() -> Unit>(
	"ender" to {
		raMessage("@s", "Ender Bundle", "light_purple")
	},
)

internal fun Ra.registerBundles() {
	val ordered = bundles.sortedBy { HAND_OUT_ORDER.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
	val gives = ordered.map { bundle -> pack.define(ref(Ns.RA, "items/bundles/give_${bundle.id}_bundle")) { giveBundle(bundle) } }

	pack.define(giveAll) {
		gives.forEach { function(it) }
		raMessage("@s", "Given categorized namespace bundles!", "green")
	}

	// Give categorized namespace bundles directly to the executing player.
	pack.define(giveAllItems) { function(giveAll) }
}

/** One `give @s minecraft:bundle[...]` holding the bundle's items (a long SNBT literal, hence raw). */
private fun Function.giveBundle(bundle: GiveBundle) {
	val lore = listOfNotNull(
		"""{text:"${bundle.lore}",color:"gray",italic:false}""",
		EXTRA_LORE[bundle.id],
	).joinToString(",")
	val label = NAMESPACE_LABEL[bundle.id] ?: bundle.ns.id
	val contents = bundle.items.joinToString(",") { it.toSnbt() }
	raw("""give @s minecraft:bundle[item_name="${bundle.title}",lore=[$lore],custom_data={ra:{bundle_namespace:"$label"}},bundle_contents=[$contents]]""")
	AFTER_GIVE[bundle.id]?.invoke(this)
}
