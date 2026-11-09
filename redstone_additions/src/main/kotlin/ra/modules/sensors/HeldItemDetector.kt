package ra.modules.sensors

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.WrenchCycle
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockGive
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.block.soundListenersSel
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.lib.safe.SafeText.guardProperty
import ra.modules.sensors.SensorPoll.startIfIdle

/**
 * Held Item Detector: powers its shell while a player within `radius` carries `item` in the
 * slots of its `slot` mode, one of [SLOTS]: `mainhand`, `offhand`, `either` (hand), `inventory`
 * (hotbar, main inventory, offhand and armour). Output as the Entity Detector's: iron blocks in
 * the 3x3x3 around it become redstone blocks. Spectators are never counted.
 *
 * `item` is an item id or an item tag (`minecraft:diamond`, `#minecraft:logs`), pasted into
 * `execute if items entity @a[...] <slots> $(item)`. Two checks stand before that macro:
 *   - `ra_lib:safe/guard`, format ID: one bare word (letters, digits, `_ . + - : / #`), so the
 *     value can only ever be the item predicate argument;
 *   - a PROBE, `ra_sensors:blocks/held_item_detector/probe`, the same predicate in a macro against
 *     the marker (which has no slots): an id or tag the game does not know fails the macro when it
 *     is instantiated, so the probe returns 0 instead of 1. The value last probed is kept in
 *     `data.data.valid_item` or `data.data.invalid_item`, so an unchanged value is probed once,
 *     not every second (an unknown id is one error line in the log per change, not per read).
 *
 * Read once a second by [SensorPoll].
 */
object HeldItemDetector {
	val block = CustomBlock(Ns.SENSORS, "held_item_detector", "Held Item Detector", model = "minecraft:waxed_exposed_chiseled_copper")
	val item = block.loreItem { lore(loreLine("Powers when a nearby player carries an item", "gray")) }

	/** Cycle order, the first the default. */
	val SLOTS = listOf("mainhand", "offhand", "either", "inventory")

	/** Players may set up to this radius; admins up to [ADMIN_MAX_RADIUS] (one player selector a second). */
	const val MAX_RADIUS = 16
	const val ADMIN_MAX_RADIUS = 64
	const val DEFAULT_RADIUS = 5
	const val DEFAULT_ITEM = "minecraft:diamond"

	/** As the marker, at it: look for the item, switch the shell. */
	val update = block.fn("update")
	val cycleSlot = block.fn("cycle_slot")
	private val find = block.fn("find")
	private val probe = block.fn("probe")
	private val detect = block.fn("detect")

	private const val T = Obj.TEMP
	private const val ON = "#sn.held"
	private const val M = "#sn.held_slot"
	private const val S = "storage ra:temp held_item"

	/** Slot arguments of `execute if items` per mode (index in [SLOTS]). */
	private val slotArgs = listOf(
		listOf("weapon.mainhand"),
		listOf("weapon.offhand"),
		listOf("weapon.*"),
		listOf("container.*", "weapon.offhand", "armor.*"),
	)

	fun Ra.registerHeldItemDetector() {
		block(block)
		wrenchCycle(block.id, WrenchCycle("Slot", "slot", cycleSlot))
		properties(block) {
			item("item", default = DEFAULT_ITEM)
			text("slot", default = SLOTS.first())
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.ITEM_FRAME), count = null) {
			pattern("CFC", "ROR", "CCC")
			key("C", Items.COPPER_INGOT)
			key("F", Items.ITEM_FRAME)
			key("R", Items.REDSTONE)
			key("O", Items.OBSERVER)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", """{item:"$DEFAULT_ITEM",slot:"${SLOTS.first()}",radius:$DEFAULT_RADIUS}""")
			// First read now rather than up to a second later.
			executeRun("as ${block.newMarkerSel()} at @s", update)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(update) {
			raw("execute store result score $ON $T run function ${find.asId()}")
			raw("data remove $S")
			raw("execute if score $ON $T matches 1 run data modify entity @s data.status.output set value \"on\"")
			raw("execute unless score $ON $T matches 1 run data modify entity @s data.status.output set value \"off\"")
			// One fill or the other, never off-then-on: that would pulse the wires every read.
			raw("execute if score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
			raw("execute unless score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		}
		pack.define(find) { find() }

		// Macro {item}: as the marker, which has no slots, so it never matches. Instantiating it
		// is the test: an unknown item id or tag fails here, before this returns 1.
		pack.define(probe) {
			raw("\$execute if items entity @s weapon.mainhand \$(item)")
			raw("return 1")
		}

		// Macro {item, r}: only ever an item that passed guard and probe, and a number.
		pack.define(detect) {
			val players = "@a[distance=..\$(r),gamemode=!spectator]"
			slotArgs.forEachIndexed { mode, slots ->
				for (slot in slots) raw("\$execute if score $M $T matches $mode if items entity $players $slot \$(item) run return 1")
			}
			raw("return 0")
		}

		pack.define(cycleSlot) { cycleSlot() }

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("item", "Item: ", "aqua", y = 0.8)
			propLine("slot", "Slot: ", "aqua", y = 0.6)
			propLine("radius", "Radius: ", "aqua", y = 0.4)
			dataLine("output", "Output: ", "yellow", y = 0.2)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) { startIfIdle(block) }
	}

	/** As the marker: returns 1 when a player in range carries the item. */
	private fun Function.find() {
		raw("data remove $S")
		raw("execute unless data entity @s data.properties.item run return 0")
		raw("execute if data entity @s data.properties{item:\"\"} run return 0")
		guardProperty("item", TextFormat.ID)
		raw("data modify $S.item set from entity @s data.properties.item")

		// Probed already: known bad, stop; known good, skip the probe.
		raw("data modify $S.was set from entity @s data.data.invalid_item")
		raw("execute store success score #sn.chg $T run data modify $S.was set from $S.item")
		raw("execute if score #sn.chg $T matches 0 run return 0")
		raw("data remove $S.was")
		raw("data modify $S.was set from entity @s data.data.valid_item")
		raw("execute store success score #sn.chg $T run data modify $S.was set from $S.item")
		raw("scoreboard players set #sn.ok $T 1")
		// 0 first: a macro that fails to instantiate leaves it so, whatever `store` does on a failure.
		raw("execute if score #sn.chg $T matches 1 run scoreboard players set #sn.ok $T 0")
		raw("execute if score #sn.chg $T matches 1 store result score #sn.ok $T run function ${probe.asId()} with $S")
		raw("execute if score #sn.chg $T matches 1 if score #sn.ok $T matches 1 run data modify entity @s data.data.valid_item set from $S.item")
		raw("execute if score #sn.chg $T matches 1 if score #sn.ok $T matches 1 run data remove entity @s data.data.invalid_item")
		raw("execute if score #sn.ok $T matches 0 run data modify entity @s data.data.invalid_item set from $S.item")
		raw("execute if score #sn.ok $T matches 0 run data remove entity @s data.data.valid_item")
		raw("execute if score #sn.ok $T matches 0 run return 0")
		raw("data remove $S.was")

		// Slot mode: anything else (typed in the Data Handler) detects nothing.
		raw("scoreboard players set $M $T -1")
		SLOTS.forEachIndexed { i, slot ->
			raw("execute if data entity @s data.properties{slot:\"$slot\"} run scoreboard players set $M $T $i")
		}
		raw("execute if score $M $T matches -1 run return 0")

		readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
		raw("execute store result $S.r int 1 run scoreboard players get #prop $T")
		raw("return run function ${detect.asId()} with $S")
	}

	/** Wrench: the next of [SLOTS], from anything else to the first. As the marker, at the block. */
	private fun Function.cycleSlot() {
		raw("scoreboard players set $M $T 0")
		SLOTS.forEachIndexed { i, slot ->
			raw("execute if data entity @s data.properties{slot:\"$slot\"} run scoreboard players set $M $T ${i + 1}")
		}
		// Score k (slot k-1) -> slot k, the last wrapping to the first; 0 (unknown, missing) -> the first.
		for (k in 0..SLOTS.size) {
			raw("execute if score $M $T matches $k run data modify entity @s data.properties.slot set value \"${SLOTS[k % SLOTS.size]}\"")
		}
		raw("""tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"${block.name}: ",color:"gray"},{nbt:"data.properties.slot",entity:"@s",color:"aqua"}]""")
		raw("playsound minecraft:block.lever.click block ${soundListenersSel(10)} ~ ~ ~ 0.6 1.4")
		// The output follows the new slot now, not at the next read.
		function(update)
	}
}
