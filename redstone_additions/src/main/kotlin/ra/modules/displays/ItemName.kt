package ra.modules.displays

import ra.core.Ns
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * `ra_displays:item_name/build`: the name an item stack shows, as a text component, for a
 * `text_display`. Shared by the display blocks (the Item Hologram's name line, the Text Screen's
 * text).
 *
 * In: `storage ra:temp dsp.item`, an item stack. Out: `storage ra:temp dsp.name`.
 *
 * NO TEXT IS PASTED. A renamed item's `custom_name`, else its `item_name` (every RA item has
 * one), is a component already and is copied as it is, by reference. Only a plain vanilla item
 * has neither: its name is a translation, built from its id with one macro line. An item id
 * comes from the item registry (`[a-z0-9_./-]` only, always `minecraft:` -- data packs add no
 * items), never from a player.
 *
 * WHICH KEY. A block's item is named by `block.minecraft.<id>`, anything else by
 * `item.minecraft.<id>`, and nothing in a data pack says which an id is. The component asks for
 * the item key with `fallback:"%s"`: when the key is missing, the fallback is the template and
 * `%s` is the block key in `with`. An id with neither key shows the id itself.
 */
internal object ItemName {
	val build = ref(Ns.DISPLAYS, "item_name/build")
	private val translate = ref(Ns.DISPLAYS, "item_name/translate")

	private const val ITEM = "storage ra:temp dsp.item"

	fun Ra.registerItemName() {
		pack.define(build) {
			raw("data remove storage ra:temp dsp.name")
			raw("""execute if data $ITEM.components."minecraft:custom_name" run return run data modify storage ra:temp dsp.name set from $ITEM.components."minecraft:custom_name"""")
			raw("""execute if data $ITEM.components."minecraft:item_name" run return run data modify storage ra:temp dsp.name set from $ITEM.components."minecraft:item_name"""")
			// The id without its `minecraft:` (10 characters).
			raw("data modify storage ra:temp dsp.path set string $ITEM.id 10")
			callWith(translate, "storage ra:temp dsp")
		}
		// Macro {path}: an item id's path, see the class comment.
		pack.define(translate) {
			raw("""${'$'}data modify storage ra:temp dsp.name set value {translate:"item.minecraft.$(path)",fallback:"%s",with:[{translate:"block.minecraft.$(path)",fallback:"$(path)"}]}""")
		}
	}
}
