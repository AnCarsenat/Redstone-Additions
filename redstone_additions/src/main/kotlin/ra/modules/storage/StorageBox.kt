package ra.modules.storage

import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.item.SBool
import ra.core.item.SComp
import ra.core.item.SInt
import ra.core.item.SList
import ra.core.item.SRaw
import ra.core.item.SStr
import ra.core.item.comp
import ra.core.item.flag
import ra.core.item.itemSpec
import ra.core.item.loreLine
import ra.core.item.raData
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import ra.core.executeRun
import ra.core.callWith
import ra.core.macroCall

/**
 * The Item Crate (the `storage_box/` functions): a whole container's contents packed into one item.
 *
 * A command block drawn as a player head. The stacks it holds live in
 * `custom_data.ra.storage_box.items`, and `preview` keeps one lore line per stack, of which the
 * first five are shown. Up to 27 stacks, like a shulker box. `item_box` marks a crate;
 * `storage_box_item` is the key crates had before it and is still accepted.
 */
object StorageBox {
	/** The empty crate, lore included. */
	val crate = itemSpec("minecraft:command_block") {
		model("minecraft:player_head")
		add("custom_name", comp("text" to SStr("Item Crate"), "color" to SStr("white"), "italic" to SBool(false)))
		add("profile", SKIN)
		add("max_stack_size", SInt(64))
		lore(EMPTY)
		customData(raData(flag("item_box"), "storage_box" to comp("items" to SList(emptyList()), "preview" to SList(emptyList()))))
	}

	private val EMPTY: SComp get() = loreLine("(empty)", "gray")

	/**
	 * The crate's head skin, BoxMan01234's, as a static profile: the id plus the `textures`
	 * property, so the game never has to look the name up. A name alone (`profile:"BoxMan01234"`,
	 * before 6.0.3) is resolved at runtime and drew a broken head when that failed. The property
	 * is base64 of `{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/dccbb5f1dbb551fde8d5e26835742a557540dec82b5fcad1581e4a2e101da7d0"}}}`.
	 */
	private val SKIN: SRaw get() = SRaw(
		"{id:[I;1762048015,484985711,-1926722649,111618629],name:\"BoxMan01234\",properties:[{name:\"textures\"," +
			"value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGNjYmI1ZjFkYmI1NTFmZGU4ZDVlMjY4MzU3NDJhNTU3NTQwZGVjODJiNWZjYWQxNTgxZTRhMmUxMDFkYTdkMCJ9fX0=\"}]}"
	)

	val giveStorageBox = ref(Ns.STORAGE, "items/give_storage_box")
	val updateLoreStorageTarget = fn("update_lore_storage_target")
	val makePreviewFromInput = fn("make_preview_from_input")
	val emptyCrateHere = fn("empty_crate_here")
	private val updateLore = fn("update_lore")
	private val emptyCrateLoop = fn("empty_crate_loop")
	private val deliverStack = fn("deliver_stack")

	private fun fn(path: String) = ref(Ns.STORAGE, "storage_box/$path")

	/** NBT path prefixes of a crate held in storage ra:temp storage_box.target_box, and of a crate item entity. */
	private const val TARGET = "storage ra:temp storage_box.target_box"
	private const val ENTITY = "entity @s Item"
	private const val BOX = """components."minecraft:custom_data".ra.storage_box"""
	private const val LORE = """components."minecraft:lore""""
	private const val PROFILE = """components."minecraft:profile""""

	fun Ra.registerStorageBox() {
		pack.define(giveStorageBox) { raw("give @s ${crate.giveArg()}") }

		pack.define(updateLore) { updateLore(ENTITY) }
		pack.define(updateLoreStorageTarget) { updateLore(TARGET) }
		pack.define(makePreviewFromInput) {
			// Macro `{id, count}`.
			raw("\$data modify storage ra:temp storage_box.input_preview set value {text:\"\$(id) x\$(count)\",color:\"gray\",italic:false}")
		}

		pack.define(fn("add_cached_input")) { addCachedInput() }
		pack.define(fn("add_cached_input_block_item_box")) { addCachedInputBlock(legacy = false) }
		pack.define(fn("add_cached_input_block_legacy")) { addCachedInputBlock(legacy = true) }
		pack.define(fn("create_with_cached_input")) { createWithCachedInput() }
		pack.define(fn("spawn_with_cached_input")) { spawnWithCachedInput() }
		pack.define(fn("unbox_first")) { unboxFirst() }

		pack.define(emptyCrateHere) { emptyCrateHere() }
		pack.define(emptyCrateLoop) { emptyCrateLoop() }
		pack.define(deliverStack) { deliverStack() }
	}

	/**
	 * Lore = the first five preview entries, then "... and more" past five, or "(empty)". [holder]
	 * is the crate's NBT root: `entity @s Item` (as a crate item entity) or the crate in storage.
	 */
	private fun Function.updateLore(holder: String) {
		raw("execute unless data $holder.$BOX.items run data modify $holder.$BOX.items set value []")
		raw("execute unless data $holder.$BOX.preview run data modify $holder.$BOX.preview set value []")

		raw("data modify $holder.$LORE set value []")
		// Crates made before 6.0.3 carry the name-only profile: give them the skin as they are touched.
		raw("data modify $holder.$PROFILE set value ${SKIN.text()}")

		for (i in 0..4) raw("execute if data $holder.$BOX.preview[$i] run data modify $holder.$LORE append from $holder.$BOX.preview[$i]")

		raw("execute if data $holder.$BOX.preview[5] run data modify $holder.$LORE append value {text:\"... and more\",color:\"gray\",italic:false}")
		raw("execute unless data $holder.$BOX.preview[0] run data modify $holder.$LORE append value ${EMPTY.text()}")
	}

	/** Shared tail: append the cached input (storage ra:temp storage_box.input_item / input_preview) to [holder]'s crate. */
	private fun Function.appendCachedInput(holder: String) {
		raw("data modify $holder.$BOX.items append from storage ra:temp storage_box.input_item")
		raw("data modify $holder.$BOX.preview append from storage ra:temp storage_box.input_preview")
	}

	/** Make sure [holder]'s crate has both lists, then stop when it is full (27 stacks, shulker-like). */
	private fun Function.ensureListsAndRoom(holder: String) {
		raw("execute unless data $holder.$BOX.items run data modify $holder.$BOX.items set value []")
		raw("execute unless data $holder.$BOX.preview run data modify $holder.$BOX.preview set value []")
		raw("execute if data $holder.$BOX.items[26] run return 0")
	}

	/** As a crate item entity: add the cached input to it. */
	private fun Function.addCachedInput() {
		ensureListsAndRoom(ENTITY)
		appendCachedInput(ENTITY)
		function(updateLore)
		scoreboard.players.set(literal("#boxer_added"), Obj.TEMP, 1)
		returnValue(1)
	}

	/**
	 * Positioned at an output container: add the cached input to the crate in it. [legacy]: the
	 * crate still carries the old `storage_box_item` key -- it is migrated to `item_box` as it is
	 * touched, keeping the old key for compatibility.
	 */
	private fun Function.addCachedInputBlock(legacy: Boolean) {
		val key = if (legacy) "storage_box_item" else "item_box"
		val slot = """Items[{components:{"minecraft:custom_data":{ra:{$key:1b}}}}]"""
		raw("data modify $TARGET set from block ~ ~ ~ $slot")
		ensureListsAndRoom(TARGET)
		appendCachedInput(TARGET)
		if (legacy) raw("data modify $TARGET.components.\"minecraft:custom_data\".ra.item_box set value 1b")
		function(updateLoreStorageTarget)
		raw("data modify block ~ ~ ~ $slot set from $TARGET")
		scoreboard.players.set(literal("#boxer_added"), Obj.TEMP, 1)
		returnValue(1)
	}

	/** A new crate holding the cached input, in storage ra:temp storage_box.generated_item. */
	private fun Function.createWithCachedInput() {
		raw("data modify $TARGET set value ${crate.stackSnbt()}")
		appendCachedInput(TARGET)
		function(updateLoreStorageTarget)
		raw("data modify storage ra:temp storage_box.generated_item set from $TARGET")
		returnValue(1)
	}

	/** At the Boxer's front output position: drop a new crate holding the cached input. */
	private fun Function.spawnWithCachedInput() {
		raw("summon item ~ ~ ~ {Item:${crate.stackSnbt()},PickupDelay:10,Tags:[\"${Tag.RA}\",\"ra.storage_box.new\"]}")
		executeRun("as @e[type=item,tag=ra.storage_box.new,distance=..0.6,limit=1,sort=nearest]", fn("add_cached_input"))
		raw("tag @e[type=item,tag=ra.storage_box.new,distance=..0.6,limit=1,sort=nearest] remove ra.storage_box.new")
		raw("return run scoreboard players get #boxer_added ${Obj.TEMP}")
	}

	/** As a crate item entity: drop its first stack at the crate and remove it from the crate. */
	private fun Function.unboxFirst() {
		raw("execute unless data $ENTITY.$BOX.items[0] run return 0")

		raw("data modify storage ra:temp storage_box.output_item set from $ENTITY.$BOX.items[0]")

		raw("""summon item ~ ~ ~ {Item:{id:"minecraft:stone",count:1},Tags:["${Tag.RA}","ra.storage_box.drop"],PickupDelay:10}""")
		raw("data modify entity @e[type=item,tag=ra.storage_box.drop,distance=..0.6,limit=1,sort=nearest] Item set from storage ra:temp storage_box.output_item")
		raw("tag @e[type=item,tag=ra.storage_box.drop,distance=..0.6,limit=1,sort=nearest] remove ra.storage_box.drop")

		raw("data remove $ENTITY.$BOX.items[0]")
		raw("execute if data $ENTITY.$BOX.preview[0] run data remove $ENTITY.$BOX.preview[0]")

		function(updateLore)

		scoreboard.players.set(literal("#unboxer_done"), Obj.TEMP, 1)
		returnValue(1)
	}

	/**
	 * Positioned at the container half whose Items[0] is the crate: empty it completely into the
	 * output named by storage ra:temp unboxer, then destroy the spent crate.
	 */
	private fun Function.emptyCrateHere() {
		// The bound is a safety net, not a limit: a crate holds at most one container's worth of
		// stacks, and a double chest is 54.
		scoreboard.players.set(literal("#unboxer_guard"), Obj.TEMP, 64)
		function(emptyCrateLoop)

		// The crate is consumed by unboxing. Removing it also clears the way for the next crate in
		// the container, which an emptied one left sitting at Items[0] would otherwise block.
		raw("execute unless data block ~ ~ ~ Items[0].$BOX.items[0] run data remove block ~ ~ ~ Items[0]")
	}

	/**
	 * Move one stored stack out of the crate at Items[0], then recurse.
	 *
	 * The stack leaves the crate BEFORE it is delivered. Delivering first and clearing afterwards is
	 * what duplicated items when the two steps disagreed.
	 */
	private fun Function.emptyCrateLoop() {
		raw("execute if score #unboxer_guard ${Obj.TEMP} matches ..0 run return 0")
		scoreboard.players.remove(literal("#unboxer_guard"), Obj.TEMP, 1)

		raw("execute unless data block ~ ~ ~ Items[0].$BOX.items[0] run return 0")

		raw("data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0].$BOX.items[0]")
		raw("execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}")

		raw("data remove block ~ ~ ~ Items[0].$BOX.items[0]")
		raw("execute if data block ~ ~ ~ Items[0].$BOX.preview[0] run data remove block ~ ~ ~ Items[0].$BOX.preview[0]")

		callWith(deliverStack, "storage ra:temp unboxer")
		scoreboard.players.add(literal("#unboxer_moved"), Obj.TEMP, 1)

		function(emptyCrateLoop)
	}

	/**
	 * Macro `{output}`: put storage ra:temp mover_item into the Unboxer's output container.
	 *
	 * `at @s` first: the caller is positioned at the input container, and a caret offset is
	 * measured from the current position, so without this the output offset would be applied
	 * relative to the input side rather than to the block.
	 */
	private fun Function.deliverStack() {
		raw("data modify storage ra:inventory item set from storage ra:temp mover_item")
		raw("\$execute at @s positioned \$(output) run ${macroCall(RaLib.Inventory.insertOrDrop)}")
	}
}
