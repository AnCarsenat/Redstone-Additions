package ra.modules.sensors

import ra.core.TextFormat
import ra.core.text
import ra.lib.safe.SafeText.guardProperty
import ra.modules.sensors.SensorsModule.SELECTOR_SETTING
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.generated.Items
import ra.core.Ns
import ra.core.Ra
import ra.core.block.CustomBlock
import ra.core.block.blockGive
import ra.core.block.blockTick
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.propLine
import ra.core.define
import ra.core.raw
import ra.core.Storage
import io.github.ayfri.kore.commands.function
import ra.core.item.loreItem
import ra.core.block.shapedRecipe
import ra.core.block.recipeResult
import ra.core.block.Unlock
import ra.core.block.blockOnBreak
import ra.core.block.dropItem
import ra.core.block.STONE_BREAK_SOUND
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.finishPlacement
import ra.core.Tag
import ra.core.block.soundListenersSel

/**
 * Tag Remover: on a rising redstone edge, removes `tag` from every entity matching
 * `entity_selector`, or clears all their tags when `tag` is empty or missing.
 * Properties: entity_selector (default `@e[distance=..3]`), tag (default `custom_tag`).
 */
object TagRemover {
	val block = CustomBlock(Ns.SENSORS, "tag_remover", "Tag Remover", model = "minecraft:red_glazed_terracotta")
	val item = block.loreItem {}

	private val removeTagMacro = block.fn("remove_tag_macro")
	private val clearAllTagsMacro = block.fn("clear_all_tags_macro")
	private val fire = block.fn("fire")

	fun Ra.registerTagRemover() {
		block(block)
		properties(block) {
			// Pasted into commands, and able to reach any entity: admins only, unless the server
			// lets players edit them (Sensors settings). Checked again before every use.
			text("entity_selector", TextFormat.SELECTOR, adminOnly = true, adminSetting = SELECTOR_SETTING)
			text("tag", TextFormat.TAG, adminOnly = true, adminSetting = SELECTOR_SETTING)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.NAME_TAG), count = null) {
			pattern("CCC", "CWC", "CCC")
			key("C", Items.COBBLESTONE)
			key("W", Items.WATER_BUCKET)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Red glazed terracotta with a marker (dir_type 0 = non-directional).
			placeBlock(block)
			setOnNew(block, "data.properties", """{entity_selector:"@e[distance=..3]",tag:"custom_tag"}""")
			finishPlacement(block, removePlaceTag = false)
		}

		// Macro {tag, entity_selector}. As the marker, at its position.
		pack.define(removeTagMacro) {
			// Anchor position to the marker for distance calculations in the selector.
			raw("\$execute at @s as \$(entity_selector) if entity @s[tag=!${Tag.CUSTOM_BLOCK}] run tag @s remove \$(tag)")
			raw("playsound minecraft:block.note_block.bit block ${soundListenersSel()} ~ ~ ~ 0.5 0.8")
		}

		// Macro {entity_selector}: clear every tag. As the marker, at its position.
		pack.define(clearAllTagsMacro) {
			raw("\$execute at @s as \$(entity_selector) if entity @s[tag=!${Tag.CUSTOM_BLOCK}] run data modify entity @s Tags set value []")
			raw("playsound minecraft:block.note_block.bit block ${soundListenersSel()} ~ ~ ~ 0.5 0.5")
		}

		// One rising edge. As the marker, at its position.
		//
		// Per marker, not per command line: with `execute as <every rising marker>` on each copy,
		// the last marker's values were in storage by the time any of them ran a macro. The paths
		// are removed before the copy so a marker missing its selector cannot run with the previous
		// marker's; it does nothing instead. A missing or empty tag still means "clear them all".
		pack.define(fire) {
			raw("data remove storage ra:temp tag")
			raw("data remove storage ra:temp entity_selector")
			raw("data modify storage ra:temp tag set from entity @s data.properties.tag")
			raw("data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			raw("execute unless data storage ra:temp entity_selector run return 0")
			// Both are pasted into the command: refuse anything that could be more than one argument.
			guardProperty("entity_selector", TextFormat.SELECTOR)
			raw("execute unless data storage ra:temp tag run return run function ${clearAllTagsMacro.asId()} with storage ra:temp")
			raw("execute if data storage ra:temp {tag:\"\"} run return run function ${clearAllTagsMacro.asId()} with storage ra:temp")
			guardProperty("tag", TextFormat.TAG)
			function(removeTagMacro, Storage.TEMP)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("tag", "Tag: ", "aqua", y = 0.8)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) {
			detectAll(block)

			// Trigger on rising edge (just powered).
			executeRun("as ${risingEdge(block)} at @s", fire)

			trackPower(block)
		}
	}
}
