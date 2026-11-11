package ra.core.block

import io.github.ayfri.kore.arguments.components.ComponentsScope
import io.github.ayfri.kore.arguments.components.item.itemModel
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.arguments.types.resources.ItemArgument
import io.github.ayfri.kore.generated.Items
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import io.github.ayfri.kore.arguments.components.item.CustomComponent
import ra.core.Ns
import ra.core.Tag
import ra.core.ref

/**
 * One placeable custom block, described once.
 *
 * In the beet version a block's identity was retyped in up to nine places: the give command,
 * the recipe result, the on-break drop, the bundle, the placement tag, the marker tag, the
 * goggles dispatch, the `give_all` list and the unlock advancement. Every one had to agree byte
 * for byte, and "ID mismatch between recipe result and placement handler tag" was the first
 * entry in the developer guide's list of common failures. Here all of them are derived from
 * this value, so they cannot disagree.
 *
 * A custom block is a bat spawn egg whose bat is tagged `ra.spawned` + `ra.place.<id>`; the
 * placement router (`ra:placement/detect_bats`) finds the bat and runs every registered
 * `handle_placement`, which places [physical] and summons a marker tagged
 * `ra.custom_block.<id>`.
 *
 * @property ns Namespace the block's functions live in.
 * @property id The block id: `custom_data={ra:{<id>:1b}}`, `ra.place.<id>`, `ra.custom_block.<id>`.
 * @property name Item name shown to players and on goggles.
 * @property model `item_model` of the egg (what the item looks like in hand).
 * @property physical The vanilla block placed in the world. Defaults to [model].
 * @property dir Folder under `blocks/` holding the block's functions. Defaults to [id]; differs
 *   for a few blocks whose id carries a module prefix (`ender_item_vault` lives in `blocks/item_vault`).
 * @property dirType 0 = no facing, 1 = horizontal facing, 2 = full facing (see ra_lib:orientation).
 */
data class CustomBlock(
	val ns: Ns,
	val id: String,
	val name: String,
	val model: String,
	val physical: String = model,
	val dir: String = id,
	val dirType: Int = 0,
) {
	/** `ra.custom_block.<id>`, on the block's marker. */
	val tag get() = Tag.customBlock(id)

	/** `ra.place.<id>`, on the bat the egg spawns. */
	val placeTag get() = Tag.place(id)

	/** `ns:blocks/<dir>/<sub>`. */
	fun fn(sub: String): FunctionArgument = ref(ns, "blocks/$dir/$sub")

	val give get() = fn("give")
	val handlePlacement get() = fn("handle_placement")
	val tick get() = fn("tick")
	val goggles get() = fn("goggles")

	/** The item components every copy of this block's item carries, in one place. */
	fun ComponentsScope.blockItemComponents() {
		itemModel(model.substringAfter(':'), model.substringBefore(':', "minecraft"))
		// CustomComponent (raw NBT), because this same scope becomes a recipe result's JSON and Kore's
		// typed item_name / custom_data / entity_data only serialize to SNBT (2.28: they throw).
		this["item_name"] = CustomComponent(NbtString(name))
		this["custom_data"] = CustomComponent(nbt { this["ra"] = nbt { this[id] = true } })
		this["entity_data"] = CustomComponent(entityDataNbt())
	}


	/** `bat_spawn_egg[...]`, for `give` and item tests. */
	val item: ItemArgument get() = Items.BAT_SPAWN_EGG { blockItemComponents() }

	/** The bat the egg spawns ([PlacementBat]: silent, frozen, invulnerable and invisible). */
	fun entityDataNbt(): NbtCompound = PlacementBat.nbt(listOf(Tag.RA, Tag.SPAWNED, placeTag))

	/**
	 * The item as an SNBT item stack (`{id, count, components}`), for `summon item`, bundle
	 * contents and anything else that embeds an item in NBT rather than in an item argument.
	 */
	fun itemStackNbt(count: Int = 1): NbtCompound = nbt {
		this["id"] = "minecraft:bat_spawn_egg"
		this["count"] = count
		this["components"] = nbt {
			this["minecraft:item_model"] = model
			this["item_name"] = name
			this["custom_data"] = nbt { this["ra"] = nbt { this[id] = true } }
			this["entity_data"] = entityDataNbt()
		}
	}
}
