package ra.hub.placement

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.utils.toSnbt
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtString
import ra.core.Ns
import ra.core.RA_CHAT_PREFIX
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.executeReturnRun
import ra.core.raw
import ra.core.ref

/**
 * Giving a refused placement its item back.
 *
 * A spawn egg is used up by vanilla before the bat it spawns ever reaches the pack, so a refused
 * placement used to cost the player the item, silently. The bat carries only its
 * `ra.place.<id>` tag, so the item is rebuilt from that: [refund] maps every place tag to the
 * item stack that carries it, read from the give bundles (every item a player can get, with its
 * full lore), then from the registered blocks (anything no bundle holds), then from
 * [Ra.extraPlaceables] (the example bases); a retired place tag ([Ra.retiredPlaceTags]) refunds
 * the item that now places the same block.
 *
 * WHY NOT THE EXACT ITEM. The placer's stack is already one shorter (or gone) when the bat is
 * seen, and an egg cannot carry a copy of itself in its own entity_data. So the table is built
 * from the item definitions instead, and the build fails if any place tag a handler accepts has
 * no entry: it cannot fall behind a new or retired item.
 */
internal val refund = ref(Ns.RA, "placement/refund")
private val refundItem = ref(Ns.RA, "placement/refund_item")

/** `refuse {why}`: tell the placer why, then [refund]. As the bat, at it. */
internal val refuse = ref(Ns.RA, "placement/refuse")

internal fun Ra.registerRefund() {
	// The most specific place tag of each stack: a Multiblock Base bat carries both
	// `ra.place.multiblock_base` and `ra.place.multiblock_base.<tier>`, and only the tier says
	// which item it was.
	val stacks = linkedMapOf<String, NbtCompound>()
	for (stack in bundles.flatMap { it.items }) placeTagOf(stack)?.let { stacks.putIfAbsent(it, stack) }
	for (block in allBlocks) stacks.putIfAbsent(block.placeTag, block.itemStackNbt())
	for (stack in extraPlaceables) placeTagOf(stack)?.let { stacks.putIfAbsent(it, stack) }
	for ((retired, successor) in retiredPlaceTags) {
		val stack = checkNotNull(stacks[successor]) { "retired place tag $retired refunds $successor, which no item carries" }
		stacks.putIfAbsent(retired, stack)
	}

	// Longest tag first, so the tier is tested before the bare `multiblock_base` it extends.
	val ordered = stacks.entries.sortedByDescending { it.key.length }

	pack.define(refund) {
		for ((tag, stack) in ordered) executeReturnRun("if entity @s[tag=$tag]", refundItem, "{item:${stack.toSnbt()}}")
	}

	// NO PLACE TAG WITHOUT A REFUND. Once the pack is complete, every `ra.place.<id>` any function
	// tests for (a placement handler accepting it) must be one [refund] answers, or a proper
	// prefix of one (`ra.place.multiblock_base` rides along with the tier's tag). A new block, a
	// retired item kept placeable, an add-on's base: each fails the build until it can be refunded.
	val refundable = stacks.keys.toSet()
	whenPackComplete {
		val tested = Regex("""tag=(ra\.place\.[a-z0-9_.]*[a-z0-9_])(?![a-z0-9_.$(])""")
		val missing = (pack.functions + pack.generatedFunctions).flatMap { fn ->
			fn.lines.flatMap { line -> tested.findAll(line).map { it.groupValues[1] to fn.asId() } }
		}.filter { (tag, _) -> tag !in refundable && refundable.none { it.startsWith("$tag.") } }
			.groupBy({ it.first }, { it.second })
		check(missing.isEmpty()) {
			"Placeable items a refused placement cannot give back (add them to a bundle, allBlocks, extraPlaceables or retiredPlaceTags):\n" +
				missing.entries.joinToString("\n") { (tag, at) -> "  - $tag, accepted in ${at.distinct().take(3).joinToString(", ")}" }
		}
	}

	// `refund_item {item}`. At the placer's feet with no pickup delay, which is a give that keeps
	// the item's components exactly, whatever they are. Nothing for a creative placer, whose egg
	// was never used up. Nobody near at all (a dispenser fired the egg): dropped where the bat
	// was, which is in front of the dispenser that lost it.
	pack.define(refundItem) {
		raw("\$execute at @a[tag=${Tag.PLACER},distance=..8,gamemode=!creative,limit=1] run return run summon minecraft:item ~ ~ ~ {Item:\$(item),PickupDelay:0s}")
		raw("\$execute unless entity @a[tag=${Tag.PLACER},distance=..8] run summon minecraft:item ~ ~ ~ {Item:\$(item)}")
	}

	pack.define(refuse) {
		raw("\$tellraw @a[tag=${Tag.PLACER},limit=1] [$RA_CHAT_PREFIX,{text:\"\$(why)\",color:\"red\"}]")
		raw("playsound minecraft:block.note_block.bass block @a[tag=${Tag.PLACER},limit=1] ~ ~ ~ 0.6 0.7")
		function(refund)
	}
}

/** The longest `ra.place.*` tag in a stack's `entity_data`, or null for an item that places nothing. */
private fun placeTagOf(stack: NbtCompound): String? {
	val components = stack["components"] as? NbtCompound ?: return null
	val entityData = (components["entity_data"] ?: components["minecraft:entity_data"]) as? NbtCompound ?: return null
	val tags = entityData["Tags"] as? NbtList<*> ?: return null
	return tags.filterIsInstance<NbtString>().map { it.value }.filter { it.startsWith("ra.place.") }.maxByOrNull { it.length }
}
