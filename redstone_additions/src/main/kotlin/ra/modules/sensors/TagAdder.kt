package ra.modules.sensors

import ra.core.TextFormat
import ra.core.text
import ra.lib.safe.SafeText.guardProperty
import ra.modules.sensors.SensorTags.DEFAULT_SNBT
import ra.modules.sensors.SensorsModule.SELECTOR_SETTING
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
import ra.core.block.outputShell
import ra.core.block.finishPlacement
import ra.core.Tag
import ra.api.Goggles
import ra.modules.sensors.SensorTags.done
import ra.modules.sensors.SensorTags.tagsList

/**
 * Tag Adder: on a rising redstone edge, adds every tag listed in `tags` to every entity matching
 * `entity_selector`. An empty list adds nothing.
 *
 * Properties: entity_selector (default `@e[type=pig,distance=..5]`), tags (default
 * `["custom_tag"]`, see [SensorTags]; a block from before v6.0.2 had a single `tag`, which the
 * 6.0.1-to-6.0.2 migration made into the list).
 */
object TagAdder {
	const val DEFAULT_SELECTOR = "@e[type=pig,distance=..5]"

	val block = CustomBlock(Ns.SENSORS, "tag_adder", "Tag Adder", model = "minecraft:green_glazed_terracotta")
	val item = block.loreItem {}

	private val fire = block.fn("fire")
	private val fireList = block.fn("fire_list")
	private val addListedMacro = block.fn("add_listed_macro")

	fun Ra.registerTagAdder() {
		block(block)
		properties(block) {
			// Pasted into commands, and able to reach any entity: admins only, unless the server
			// lets players edit them (Sensors settings). Checked again before every use.
			text("entity_selector", TextFormat.SELECTOR, adminOnly = true, adminSetting = SELECTOR_SETTING, default = DEFAULT_SELECTOR)
			tagsList()
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.NAME_TAG), count = null) {
			pattern("CCC", "CNC", "CCC")
			key("C", Items.COBBLESTONE)
			key("N", Items.NAME_TAG)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			// Green glazed terracotta with a marker (dir_type 0 = non-directional).
			placeBlock(block)
			setOnNew(block, "data.properties", """{entity_selector:"$DEFAULT_SELECTOR",tags:$DEFAULT_SNBT}""")
			finishPlacement(block, removePlaceTag = false)
		}

		// kept: beet contract. Macro {entity_selector, range}: detection only. Nothing in the pack
		// calls it any more; kept because the beet pack shipped it.
		pack.define(block.fn("detect_macro")) {
			raw("\$execute store success score @s ${Obj.TEMP} run execute as \$(entity_selector) if entity @s[tag=!${Tag.CUSTOM_BLOCK}]")

			// Output redstone signal based on detection.
			outputShell(on = false)
			raw("execute if score @s ${Obj.TEMP} matches 1 at @s run return run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
		}

		// Macro {entity_selector}, with the checked tags in storage ra:temp sensor_tags.ok. As the
		// marker, at its position. One pass of the selector; each entity gets every listed tag.
		pack.define(addListedMacro) {
			raw("scoreboard players set #st.any ${Obj.TEMP} 0")
			raw("\$execute at @s as \$(entity_selector) if entity @s[tag=!${Tag.CUSTOM_BLOCK}] run function ${SensorTags.addEach.asId()}")
			raw("execute if score #st.any ${Obj.TEMP} matches 1 at @s run playsound minecraft:block.note_block.bit block @a[distance=..16] ~ ~ ~ 0.5 1.5")
			raw("execute if score #st.any ${Obj.TEMP} matches 0 at @s run playsound minecraft:block.note_block.bass block @a[distance=..16] ~ ~ ~ 0.5 0.5")
		}

		// One rising edge. As the marker, at its position.
		//
		// Per marker, not per command line: with `execute as <every rising marker>` on each copy,
		// the last marker's values were in storage by the time any of them ran the macro. The paths
		// are removed before the copy so a marker missing a property cannot run with the previous
		// marker's value; it does nothing instead.
		pack.define(fire) {
			raw("data remove storage ra:temp entity_selector")
			raw("data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			raw("execute unless data storage ra:temp entity_selector run return 0")
			// Pasted into the command: refuse anything that could be more than one argument.
			guardProperty("entity_selector", TextFormat.SELECTOR)
			function(fireList)
		}

		// `tags`: the entries that passed the check. Nothing for an empty list, a missing one, a
		// value that is not a list, or a list with no valid entry.
		pack.define(fireList) {
			function(SensorTags.load)
			raw("execute if score #st.n ${Obj.TEMP} matches 1.. run function ${addListedMacro.asId()} with storage ra:temp")
			done()
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("entity_selector", "Targets: ", "yellow", y = 0.8)
			raw("""function ${SensorTags.gogglesLine.asId()} {empty:"none"}""")
			raw("execute if data entity @s data.status.skipped run function ${Goggles.Billboard.dataLine.asId()} {path:\"skipped\",label:\"Refused tags: \",color:\"red\",suffix:\"\",y:0.6}")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) {
			detectAll(block)

			// Trigger on rising edge (just powered).
			val rising = risingEdge(block)
			executeRun("as $rising at @s", fire)

			trackPower(block)
		}
	}
}
