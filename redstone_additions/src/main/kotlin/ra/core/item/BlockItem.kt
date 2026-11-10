package ra.core.item

import ra.core.block.CustomBlock
import ra.core.block.PlacementBat

/** The bat the egg spawns, as [CustomBlock.entityDataNbt] spells it. */
fun CustomBlock.entityDataSnbt() = PlacementBat.snbt(listOf("ra", "ra.spawned", placeTag))

/**
 * A placeable block's egg with extra components. Same identity as [CustomBlock.item]; the extra
 * components ([extra]) go between the name and custom_data, where the beet pack had them.
 */
fun CustomBlock.loreItem(extra: ItemSpecBuilder.() -> Unit) = itemSpec("minecraft:bat_spawn_egg") {
	model(model)
	name(name)
	extra()
	customData(raData(flag(id)))
	add("entity_data", entityDataSnbt())
}
