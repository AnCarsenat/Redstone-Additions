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
 * full lore) and then from the registered blocks (anything no bundle holds).
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

	// Longest tag first, so the tier is tested before the bare `multiblock_base` it extends.
	val ordered = stacks.entries.sortedByDescending { it.key.length }

	pack.define(refund) {
		for ((tag, stack) in ordered) executeReturnRun("if entity @s[tag=$tag]", refundItem, "{item:${stack.toSnbt()}}")
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
