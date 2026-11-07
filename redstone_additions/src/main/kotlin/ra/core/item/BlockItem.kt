package ra.core.item

import ra.core.block.CustomBlock

/** The bat the egg spawns, as [CustomBlock.entityDataNbt] spells it. */
fun CustomBlock.entityDataSnbt() = comp(
	"id" to SStr("minecraft:bat"),
	"Tags" to SList(listOf(SStr("ra"), SStr("ra.spawned"), SStr(placeTag))),
	"Silent" to SByte(true),
	"NoAI" to SByte(true),
	"Invulnerable" to SByte(true),
)

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
