package ra.modules.sensors

import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockTick
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreItem
import ra.modules.infinite.registerBlockMessage

/**
 * Tag Adder: on a rising redstone edge, adds `tag` to every entity matching `entity_selector`.
 * Properties: entity_selector (default `@e[type=pig,distance=..5]`), tag (default `custom_tag`).
 */
object TagAdder {
	val block = CustomBlock(Ns.SENSORS, "tag_adder", "Tag Adder", model = "minecraft:green_glazed_terracotta")
	val item = block.loreItem {}

	private val addTagMacro = block.fn("add_tag_macro")
	val registerBlock = block.fn("register_block")

	fun Ra.registerTagAdder() {
		block(block)
		pack.registerBlockMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item, unlockedBy = Items.NAME_TAG, count = null) {
			pattern("CCC", "CNC", "CCC")
			key("C", Items.COBBLESTONE)
			key("N", Items.NAME_TAG)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			batGuard(block)
			// Green glazed terracotta with a marker (dir_type 0 = non-directional).
			placeBlock(block)
			setOnNew(block, "data.properties", """{entity_selector:"@e[type=pig,distance=..5]",tag:"custom_tag"}""")
			clearNew(block)
			returnValue(1)
		}

		// Macro {tag, entity_selector}. As the marker, at its position.
		pack.define(addTagMacro) {
			// Anchor position to the marker for distance calculations in the selector.
			raw("\$execute store success score @s ${Obj.TEMP} at @s run execute as \$(entity_selector) if entity @s[tag=!ra.custom_block] run tag @s add \$(tag)")

			raw("execute if score @s ${Obj.TEMP} matches 1.. at @s run playsound minecraft:block.note_block.bit block @a[distance=..16] ~ ~ ~ 0.5 1.5")
			raw("execute if score @s ${Obj.TEMP} matches 0 at @s run playsound minecraft:block.note_block.bass block @a[distance=..16] ~ ~ ~ 0.5 0.5")
		}

		// Macro {entity_selector, range}: detection only. Nothing in the pack calls it any more;
		// kept because the beet pack shipped it.
		pack.define(block.fn("detect_macro")) {
			raw("\$execute store success score @s ${Obj.TEMP} run execute as \$(entity_selector) if entity @s[tag=!ra.custom_block]")

			// Output redstone signal based on detection.
			shell(on = false)
			raw("execute if score @s ${Obj.TEMP} matches 1 at @s run return run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("tag", "Tag: ", "aqua", y = 0.8)
		}

		val onBreak = pack.plainOnBreak(block, item, particle = true)
		pack.blockTick(block, onBreak) {
			detectAll(block)

			// Trigger on rising edge (just powered).
			val rising = risingEdge(block)
			raw("execute as $rising at @s run data modify storage ra:temp tag set from entity @s data.properties.tag")
			raw("execute as $rising at @s run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			exec("as $rising at @s", addTagMacro, "with storage ra:temp")

			trackPower(block)
		}
	}
}
