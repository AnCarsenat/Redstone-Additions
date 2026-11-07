package ra.modules.sensors

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

	fun Ra.registerTagRemover() {
		block(block)
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

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("tag", "Tag: ", "aqua", y = 0.8)
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) {
			detectAll(block)

			// Trigger on rising edge (just powered).
			val edge = risingEdge(block)
			val rising = "as $edge at @s run execute"
			raw("execute as $edge at @s run data modify storage ra:temp tag set from entity @s data.properties.tag")
			raw("execute as $edge at @s run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			// A named tag removes that tag; an empty or missing one clears them all.
			executeRun("$rising if data entity @s data.properties.tag run execute unless data entity @s {data:{properties:{tag:\"\"}}}", removeTagMacro, "with storage ra:temp")
			executeRun("$rising if data entity @s {data:{properties:{tag:\"\"}}}", clearAllTagsMacro, "with storage ra:temp")
			executeRun("$rising unless data entity @s data.properties.tag", clearAllTagsMacro, "with storage ra:temp")

			trackPower(block)
		}
	}
}
