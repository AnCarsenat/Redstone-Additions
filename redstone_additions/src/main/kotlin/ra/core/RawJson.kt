package ra.core

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.Generator

/**
 * A data file written as JSON text, for the few shapes Kore's typed builders cannot express.
 *
 * Today: an enchantment's `location_changed` attribute effect. Vanilla spells it
 * `{"type":"minecraft:attribute",...}`, and Kore 2.28's `AttributeEffect` only serializes the
 * untyped form the always-on `attributes` component uses (its effect hierarchy is sealed, so no
 * subclass can add the `type`). The game rejects the enchantment without it.
 *
 * Prefer a builder whenever one exists: this text is not checked by anything but the game.
 */
private class RawJsonFile(folder: String, override var fileName: String, private val json: String) : Generator(folder) {
	override fun generateJson(dataPack: DataPack) = json
}

/** `data/<ns>/<folder>/<name>.json` holding [json]. */
fun DataPack.rawJson(ns: Ns, folder: String, name: String, json: String) {
	val file = RawJsonFile(folder, name, json.trimIndent())
	file.namespace = ns.id
	// An ArrayList in Kore, exposed as a read-only List: every list in it is written at generation.
	@Suppress("UNCHECKED_CAST")
	(generators as MutableList<List<Generator>>).add(listOf(file))
}
