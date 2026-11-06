package ra.modules.infinite

import io.github.ayfri.kore.arguments.components.ComponentsScope
import io.github.ayfri.kore.arguments.components.item.CustomComponent
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import ra.core.block.CustomBlock

/**
 * Items whose components go beyond what [CustomBlock] carries (lore, rarity, glint, food,
 * consumable), described once and written in every shape the pack needs: a `give` argument, an
 * SNBT item stack (drops, enchant results, bundles) and a recipe result.
 *
 * Kore's own item arguments write every boolean as `1b`/`0b`. The beet pack spelled booleans in
 * components as `true`/`false` and in custom data as `1b`, and the two are the same NBT value, so
 * this small SNBT model keeps the spelling the beet build had while producing real NBT for data
 * files. Shared by ra_chunk_loader, ra_infinite and ra_jetpacks.
 */
sealed interface Snbt {
	fun text(): String
	fun nbt(): NbtTag
}

private val BARE_KEY = Regex("[A-Za-z0-9_.+-]+")

data class SStr(val v: String) : Snbt {
	override fun text() = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
	override fun nbt() = NbtString(v)
}

/** A boolean spelled `true` / `false`. Same NBT as a byte. */
data class SBool(val v: Boolean) : Snbt {
	override fun text() = v.toString()
	override fun nbt() = NbtByte(v)
}

/** A boolean spelled `1b` / `0b`. */
data class SByte(val v: Boolean) : Snbt {
	override fun text() = if (v) "1b" else "0b"
	override fun nbt() = NbtByte(v)
}

data class SInt(val v: Int) : Snbt {
	override fun text() = v.toString()
	override fun nbt() = NbtInt(v)
}

/** A float with an explicit spelling (`0.0f`, `0`...). */
data class SFloat(val v: Float, val spelled: String) : Snbt {
	override fun text() = spelled
	override fun nbt() = NbtFloat(v)
}

data class SComp(val entries: List<Pair<String, Snbt>>) : Snbt {
	override fun text() = entries.joinToString(",", "{", "}") { (k, v) ->
		(if (BARE_KEY.matches(k)) k else "\"$k\"") + ":" + v.text()
	}

	override fun nbt() = NbtCompound(entries.associate { (k, v) -> k to v.nbt() })
}

data class SList(val items: List<Snbt>) : Snbt {
	override fun text() = items.joinToString(",", "[", "]") { it.text() }
	override fun nbt(): NbtTag = when (val first = items.firstOrNull()) {
		null -> NbtList(emptyList<NbtString>())
		is SStr -> NbtList(items.map { it.nbt() as NbtString })
		else -> {
			check(first is SComp) { "SList of ${first::class.simpleName} not supported" }
			NbtList(items.map { it.nbt() as NbtCompound })
		}
	}
}

fun comp(vararg entries: Pair<String, Snbt>) = SComp(entries.toList())

/** One lore line: `{text:"...",color:"...",italic:false}`. */
fun loreLine(text: String, color: String) = comp("text" to SStr(text), "color" to SStr(color), "italic" to SBool(false))

/** `{ra:{<key>:1b, ...}}`, the pack's custom_data shape. */
fun raData(vararg entries: Pair<String, Snbt>) = comp("ra" to SComp(entries.toList()))
fun flag(key: String) = key to SByte(true)

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
