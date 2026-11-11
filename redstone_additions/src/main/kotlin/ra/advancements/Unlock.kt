package ra.advancements

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.components.item.CustomComponent
import io.github.ayfri.kore.arguments.components.matchers.customData
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.arguments.types.resources.ModelArgument
import io.github.ayfri.kore.features.advancements.AdvancementFrameType
import io.github.ayfri.kore.features.advancements.advancement
import io.github.ayfri.kore.features.advancements.criteria
import io.github.ayfri.kore.features.advancements.display
import io.github.ayfri.kore.features.advancements.triggers.inventoryChanged
import io.github.ayfri.kore.features.advancements.triggers.item
import io.github.ayfri.kore.features.predicates.sub.components
import io.github.ayfri.kore.features.predicates.sub.predicates
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.generated.arguments.types.AdvancementArgument
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import ra.core.Ns

/**
 * How an advancement's single `inventory_changed` criterion recognises the item.
 *
 * The beet trees were written over several versions and use three shapes; each is kept as it
 * was, because they do not match the same items:
 * - [Plain]: any item of that id (the category roots).
 * - [Exact]: `components` -- an EXACT `custom_data` match.
 * - [Partial]: `predicates` -- `custom_data` must CONTAIN the given data.
 * - [Components]: `components` -- each named component exactly equal (an item recognised by its
 *   model and rarity rather than its custom data, like the Master Capture Net).
 */
internal sealed interface Match {
	data object Plain : Match
	data class Exact(val ra: NbtCompound) : Match
	data class Partial(val ra: NbtCompound) : Match
	data class Components(val values: Map<String, NbtTag>) : Match
}

/**
 * The value a `custom_data` test expects for a flag. Kept per advancement: the item carries `1b`,
 * which JSON `true` describes and JSON `1` (an int) does not, and the beet trees used both.
 */
internal enum class Flag(val tag: NbtTag) { TRUE(NbtByte(1)), ONE(NbtInt(1)) }

/** `{ <id>: <flag>, <extra>... }`, the inside of `custom_data.ra`. */
internal fun raData(id: String, flag: Flag, vararg extra: Pair<String, String>): NbtCompound = nbt {
	this[id] = flag.tag
	for ((k, v) in extra) this[k] = NbtString(v)
}

/** One `ra_advancements` advancement, all of whose criteria are "have this item". */
internal data class Unlock(
	val path: String,
	val parent: String?,
	val icon: ItemArgument,
	val title: String,
	val description: String,
	val criterion: String,
	val item: ItemArgument,
	val match: Match = Match.Plain,
	val toast: Boolean? = true,
	val announce: Boolean? = true,
	val frame: AdvancementFrameType = AdvancementFrameType.TASK,
	val background: String? = null,
)

/**
 * `ra_advancements:<tree>/get_<id>`: "Obtain the <title>.", unlocked by [item] (the block's bat
 * spawn egg unless it is a tool) with an exact `custom_data` of `{ra:{<id>:<flag>}}`.
 */
internal fun egg(
	tree: String,
	id: String,
	title: String,
	icon: ItemArgument,
	flag: Flag,
	description: String = "Obtain the $title.",
	file: String = "get_$id",
	item: ItemArgument = Items.BAT_SPAWN_EGG,
) = Unlock("$tree/$file", tree, icon, title, description, "get_$id", item, Match.Exact(raData(id, flag)))

internal fun DataPack.unlock(u: Unlock) = advancement(u.path) {
	namespace = Ns.ADVANCEMENTS.id
	u.parent?.let { parent = AdvancementArgument(it, Ns.ADVANCEMENTS.id) }
	display(u.icon, u.title, u.description) {
		frame = u.frame
		showToast = u.toast
		announceToChat = u.announce
		background = u.background?.let { ModelArgument(it.substringAfter(':'), it.substringBefore(':')) }
	}
	criteria {
		inventoryChanged(u.criterion) {
			item {
				items = listOf(u.item)
				when (val m = u.match) {
					Match.Plain -> Unit
					is Match.Exact -> components { this["custom_data"] = CustomComponent(nbt { this["ra"] = m.ra }) }
					is Match.Partial -> predicates { customData { this["ra"] = m.ra } }
					is Match.Components -> components { for ((k, v) in m.values) this[k] = CustomComponent(v) }
				}
			}
		}
	}
}
