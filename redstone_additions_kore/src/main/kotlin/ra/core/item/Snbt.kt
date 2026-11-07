package ra.core.item

import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.StringifiedNbt

/**
 * A small SNBT model that keeps the spelling the beet pack had while producing real NBT for data
 * files.
 *
 * Kore's own item arguments write every boolean as `1b`/`0b`. The beet pack spelled booleans in
 * components as `true`/`false` and in custom data as `1b`, and the two are the same NBT value, so
 * each value here carries both its text ([text]) and its NBT ([nbt]). Used by [ItemSpec].
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

/**
 * A value written as SNBT text and kept exactly as written; its NBT is parsed from that text with
 * [parseSnbt]. For components copied verbatim from a beet `give` command (the tools' items).
 */
data class SRaw(val spelled: String) : Snbt {
	override fun text() = spelled
	override fun nbt() = parseSnbt(spelled)
}

fun comp(vararg entries: Pair<String, Snbt>) = SComp(entries.toList())

/** One lore line: `{text:"...",color:"...",italic:false}`. */
fun loreLine(text: String, color: String) = comp("text" to SStr(text), "color" to SStr(color), "italic" to SBool(false))

/** `{ra:{<key>:1b, ...}}`, the pack's custom_data shape. */
fun raData(vararg entries: Pair<String, Snbt>) = comp("ra" to SComp(entries.toList()))

/** `<key>:1b`, one entry of [raData]. */
fun flag(key: String) = key to SByte(true)

private val snbtParser = StringifiedNbt { }
private val snbtBool = Regex("""(?<=[:\[,=]|^)(true|false)(?=[,}\]]|$)""")

/** SNBT as the game reads it; knbt does not know `true` / `false`, which are bytes in SNBT. */
fun parseSnbt(text: String): NbtTag =
	snbtParser.decodeFromString(NbtTag.serializer(), snbtBool.replace(text) { if (it.value == "true") "1b" else "0b" })
