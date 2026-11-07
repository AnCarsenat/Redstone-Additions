package ra.core.item

import io.github.ayfri.kore.arguments.components.ComponentsScope
import io.github.ayfri.kore.arguments.components.item.CustomComponent
import ra.core.block.CustomBlock

/**
 * Items whose components go beyond what [CustomBlock] carries (lore, rarity, glint, food,
 * consumable), described once and written in every shape the pack needs: a `give` argument, an
 * SNBT item stack (drops, enchant results, bundles) and a recipe result. Component values are
 * [Snbt], so each keeps the spelling the beet build had.
 */

/**
 * One item component. [give] is how it is spelled in a `give` argument, [stack] how it is spelled
 * inside an SNBT item stack (the beet pack wrote a few of them differently there).
 */
data class ItemComp(val name: String, val give: Snbt, val stack: Snbt = give)

data class ItemSpec(val id: String, val components: List<ItemComp>) {
	/** `minecraft:id[name=value,...]`, for `give` and item tests. */
	fun giveArg() = id + components.joinToString(",", "[", "]") { "${it.name}=${it.give.text()}" }

	fun stack(): SComp = comp(
		"id" to SStr(id),
		"count" to SInt(1),
		"components" to SComp(components.map { "minecraft:${it.name}" to it.stack }),
	)

	/** The item as an SNBT item stack. */
	fun stackSnbt() = stack().text()

	/** The item as an NBT item stack, for bundles. */
	fun stackNbt() = stack().nbt()

	fun without(name: String) = copy(components = components.filterNot { it.name == name })

	fun replace(name: String, give: Snbt, stack: Snbt = give) =
		copy(components = components.map { if (it.name == name) ItemComp(name, give, stack) else it })

	/** Writes the components into a recipe result. Raw NBT, see the Custom Blocks item-components gotcha. */
	fun applyTo(scope: ComponentsScope) {
		for (c in components) scope[c.name] = CustomComponent(c.stack.nbt())
	}

	companion object {
		/**
		 * An item whose components are SNBT text copied from a beet `give` command, `name -> value`
		 * in the order the command wrote them. The text is kept verbatim for `give`; NBT for stacks
		 * and recipe results is parsed from it ([SRaw]).
		 */
		fun fromSnbt(id: String, vararg components: Pair<String, String>) =
			ItemSpec(id, components.map { (k, v) -> ItemComp(k, SRaw(v)) })
	}
}

class ItemSpecBuilder(private val id: String) {
	private val comps = mutableListOf<ItemComp>()

	fun add(name: String, give: Snbt, stack: Snbt = give) { comps += ItemComp(name, give, stack) }
	fun model(model: String) = add("item_model", SStr(model))
	fun name(name: String) = add("item_name", SStr(name))
	fun rarity(rarity: String) = add("rarity", SStr(rarity))
	fun glint() = add("enchantment_glint_override", SBool(true))
	fun lore(vararg lines: SComp) = add("lore", SList(lines.toList()))
	fun customData(data: SComp) = add("custom_data", data)

	fun build() = ItemSpec(id, comps.toList())
}

fun itemSpec(id: String, block: ItemSpecBuilder.() -> Unit) = ItemSpecBuilder(id).apply(block).build()
