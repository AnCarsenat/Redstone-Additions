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
import ra.core.Obj
import ra.api.Goggles
import ra.modules.sensors.SensorTags.done
import ra.modules.sensors.SensorTags.tagsList
import ra.core.block.soundListenersSel

/**
 * Tag Remover: on a rising redstone edge, removes every tag listed in `tags` from every entity
 * matching `entity_selector`, or clears all their tags (except the pack's own `ra.` ones) when
 * `tags` is an empty list. The pack's own entities are never touched.
 *
 * Properties: entity_selector (default `@e[distance=..3]`), tags (default `["custom_tag"]`, see
 * [SensorTags]). A block from before v6.0.2 has no `tags` and runs on its single `tag` instead,
 * as it did then (empty or missing: clear them all); `tag` is hidden from the Data Handler for
 * players and ignored as soon as `tags` is written.
 */
object TagRemover {
	val block = CustomBlock(Ns.SENSORS, "tag_remover", "Tag Remover", model = "minecraft:red_glazed_terracotta")
	val item = block.loreItem {}

	private val removeTagMacro = block.fn("remove_tag_macro")
	private val clearAllTagsMacro = block.fn("clear_all_tags_macro")
	private val clearTags = block.fn("clear_tags")
	private val clearTagsStep = block.fn("clear_tags_step")
	private val clearOneTag = block.fn("clear_one_tag")

	private const val T = Obj.TEMP

	/** Never touched: the pack's own entities. `ra` is on all of them; markers also carry `ra.custom_block`. */
	private val NOT_RA = "tag=!${Tag.RA},tag=!${Tag.CUSTOM_BLOCK}"
	private val fire = block.fn("fire")
	private val fireSingle = block.fn("fire_single")
	private val fireList = block.fn("fire_list")
	private val removeListedMacro = block.fn("remove_listed_macro")

	fun Ra.registerTagRemover() {
		block(block)
		properties(block) {
			// Pasted into commands, and able to reach any entity: admins only, unless the server
			// lets players edit them (Sensors settings). Checked again before every use.
			text("entity_selector", TextFormat.SELECTOR, adminOnly = true, adminSetting = SELECTOR_SETTING)
			tagsList()
			// Pre-6.0.2 blocks only, read while they have no `tags`. Admins still see and edit it.
			text("tag", TextFormat.TAG, adminOnly = true, adminSetting = SELECTOR_SETTING, visible = false)
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
			setOnNew(block, "data.properties", """{entity_selector:"@e[distance=..3]",tags:["custom_tag"]}""")
			finishPlacement(block, removePlaceTag = false)
		}

		// Macro {tag, entity_selector}. As the marker, at its position.
		pack.define(removeTagMacro) {
			// Anchor position to the marker for distance calculations in the selector.
			raw("\$execute at @s as \$(entity_selector) if entity @s[$NOT_RA] run tag @s remove \$(tag)")
			raw("playsound minecraft:block.note_block.bit block ${soundListenersSel()} ~ ~ ~ 0.5 0.8")
		}

		// Macro {entity_selector}: clear every tag the pack does not own. As the marker, at its
		// position. Not `Tags set value []`: that also took `ra.` tags off players (ra.admin,
		// tool and prompt state), and, before the pack's entities were left out, wiped the
		// `ra` / `ra.multiblock` / skin tags that keep markers and displays alive.
		pack.define(clearAllTagsMacro) {
			raw("\$execute at @s as \$(entity_selector) if entity @s[$NOT_RA] run function ${clearTags.asId()}")
			raw("playsound minecraft:block.note_block.bit block ${soundListenersSel()} ~ ~ ~ 0.5 0.5")
		}

		// As the entity: walk a copy of its tags, removing each one that is not `ra` or `ra.*`.
		pack.define(clearTags) {
			raw("data modify storage ra:temp tag_remover.tags set from entity @s Tags")
			raw("execute if data storage ra:temp tag_remover.tags[0] run function ${clearTagsStep.asId()}")
			raw("data remove storage ra:temp tag_remover")
		}

		pack.define(clearTagsStep) {
			raw("data modify storage ra:temp tag_remover.t set from storage ra:temp tag_remover.tags[0]")
			// The first three characters; a shorter tag fails the copy, so clear the last one first.
			raw("data remove storage ra:temp tag_remover.p")
			raw("data modify storage ra:temp tag_remover.p set string storage ra:temp tag_remover.t 0 3")
			raw("""execute unless data storage ra:temp tag_remover{p:"ra."} unless data storage ra:temp tag_remover{t:"ra"} run function ${clearOneTag.asId()} with storage ra:temp tag_remover""")
			raw("data remove storage ra:temp tag_remover.tags[0]")
			raw("execute if data storage ra:temp tag_remover.tags[0] run function ${clearTagsStep.asId()}")
		}

		// Macro {t}. A tag read back from the entity: `tag remove` takes one word, so nothing else
		// can ride along; an odd tag only makes the line fail.
		pack.define(clearOneTag) {
			raw("\$tag @s remove \$(t)")
		}

		// One rising edge. As the marker, at its position.
		//
		// Per marker, not per command line: with `execute as <every rising marker>` on each copy,
		// the last marker's values were in storage by the time any of them ran a macro. The paths
		// are removed before the copy so a marker missing its selector cannot run with the previous
		// marker's; it does nothing instead.
		pack.define(fire) {
			raw("data remove storage ra:temp tag")
			raw("data remove storage ra:temp entity_selector")
			raw("data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector")
			raw("execute unless data storage ra:temp entity_selector run return 0")
			// Pasted into the command: refuse anything that could be more than one argument.
			guardProperty("entity_selector", TextFormat.SELECTOR)
			raw("execute unless data entity @s data.properties.tags run function ${fireSingle.asId()}")
			raw("execute if data entity @s data.properties.tags run function ${fireList.asId()}")
		}

		// `tags`: each listed tag, every tag (an empty list), or nothing (not a list, or no entry
		// passed the check -- a list of only refused entries never means "clear them all").
		pack.define(fireList) {
			function(SensorTags.load)
			raw("execute if score #st.list $T matches 0 run return run data remove storage ra:temp sensor_tags")
			raw("execute if score #st.len $T matches 0 run function ${clearAllTagsMacro.asId()} with storage ra:temp")
			raw("execute if score #st.n $T matches 1.. run function ${removeListedMacro.asId()} with storage ra:temp")
			done()
		}

		// Macro {entity_selector}, with the checked tags in storage ra:temp sensor_tags.ok. As the
		// marker, at its position. One pass of the selector; each entity drops every listed tag.
		pack.define(removeListedMacro) {
			raw("\$execute at @s as \$(entity_selector) if entity @s[$NOT_RA] run function ${SensorTags.removeEach.asId()}")
			raw("playsound minecraft:block.note_block.bit block ${soundListenersSel()} ~ ~ ~ 0.5 0.8")
		}

		// The single `tag` of a block from before v6.0.2, as it worked then. A missing or empty tag
		// means "clear them all".
		pack.define(fireSingle) {
			raw("data remove entity @s data.status.skipped")
			raw("data modify storage ra:temp tag set from entity @s data.properties.tag")
			raw("execute unless data storage ra:temp tag run return run function ${clearAllTagsMacro.asId()} with storage ra:temp")
			raw("execute if data storage ra:temp {tag:\"\"} run return run function ${clearAllTagsMacro.asId()} with storage ra:temp")
			guardProperty("tag", TextFormat.TAG)
			function(removeTagMacro, Storage.TEMP)
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			propLine("entity_selector", "Targets: ", "yellow", y = 0.8)
			raw("""execute if data entity @s data.properties.tags run function ${SensorTags.gogglesLine.asId()} {empty:"all (except ra.*)"}""")
			// A pre-6.0.2 block: its single tag, or "all" when it has none.
			raw("""execute unless data entity @s data.properties.tags run function ${SensorTags.legacyLine.asId()} {empty:"all (except ra.*)"}""")
			raw("execute if data entity @s data.status.skipped run function ${Goggles.Billboard.dataLine.asId()} {path:\"skipped\",label:\"Refused tags: \",color:\"red\",suffix:\"\",y:0.6}")
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
