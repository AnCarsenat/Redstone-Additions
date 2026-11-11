package ra.tools.goggles.lens

import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
import ra.core.block.noticeOnly
import ra.core.define
import ra.core.raw
import ra.tools.ToolItems
import ra.tools.tool

/**
 * The Goggles Lens: an upgrade that fits onto any helmet and makes it see what the Goggles see.
 *
 * Fitted the way a jetpack kit is fitted -- hold it, wear the helmet, right-click; the lens is used
 * up -- and managed the way the kits are, from a small chat menu on `/trigger ra.goggles`: switch
 * it off and on, or take it out and get the lens back.
 *
 * THE HELMET SIMPLY BECOMES GOGGLES
 * A lensed helmet that is switched on carries `{ra:{goggles:1b,lensed:1b}}`, so goggles/tick finds
 * it with the very predicate it already runs for the Goggles -- `custom_data~{ra:{goggles:1b}}`
 * on the head and hands -- and the lens costs the detection nothing. Switched off it carries
 * `{ra:{lensed:1b}}` only, and that same predicate does not match.
 *
 * WHY "OFF" IS ON THE HELMET, NOT ON THE PLAYER LIKE A KIT'S
 * A kit's switch is a player tag because the jetpack's fitted state is one generated item
 * modifier per combination, and a per-kit flag would multiply those. The lens is one flag with
 * two states, and putting "off" on the player would cost the Goggles a second, per-tick check
 * (`unless entity @s[tag=...]`) on every wearer. On the helmet it costs nothing.
 *
 * ANY HELMET, AND ITS OWN DATA STAYS
 * The helmet may already carry lore, a name, enchantments, a trim, damage, a dye or another
 * pack's `custom_data`; none of it is touched. `set_custom_data` deep-merges `ra.goggles` and
 * `ra.lensed` into whatever custom data is there, and the lens lines are inserted at the top of
 * the lore (offset 0) so the helmet's own lines keep their place below them. Being at the top,
 * they are always lines 0..2 (on) or 0..1 (off), whatever the helmet's own lore holds, so a
 * switch or a removal replaces exactly those lines and nothing else. Taking a key out of
 * `custom_data` goes through storage: the `ra` compound is copied out, the lens keys removed, and
 * the rest copied back with `copy_custom_data`, in the same modifier list as the lore change so
 * the lines and the keys never disagree. The lens is used up only once the helmet reads lensed,
 * and given back only once it no longer does. A component left empty by the removal is dropped,
 * so a helmet that was plain is plain again.
 */
object GogglesLens {
	/** `/trigger ra.goggles`: 1 the menu, 2 switch the lens off or on, 3 take it out. */
	const val TRIGGER = "ra.goggles"

	val give = tool("goggles/lens/give")
	val onUse = tool("goggles/lens/on_use")
	val menuAction = tool("goggles/lens/menu_action")
	val menu = tool("goggles/lens/menu")
	val toggle = tool("goggles/lens/toggle")
	val remove = tool("goggles/lens/remove")
	val toggleOff = tool("goggles/lens/toggle_off")

	private const val HEAD = "items entity @s armor.head"
	const val LENSED = "$HEAD *[minecraft:custom_data~{ra:{lensed:1b}}]"
	private const val LENS_ON = "$HEAD *[minecraft:custom_data~{ra:{lensed:1b,goggles:1b}}]"
	private const val SELF_IF_SOUND = "@s[scores={${Obj.USER_SOUND}=1..}]"

	private fun line(text: String, color: String) = """{text:"$text",color:"$color",italic:false}"""

	private fun lore(on: Boolean): String = (if (on) listOf(
		line("Goggles Lens - on", "aqua"),
		line("Shows every block's status, like the Goggles", "gray"),
		line("/trigger $TRIGGER switches it off", "dark_gray"),
	) else listOf(
		line("Goggles Lens - off", "dark_gray"),
		line("/trigger $TRIGGER switches it on", "dark_gray"),
	)).joinToString(",", "[", "]")

	/** How many lore lines the lens shows in each state: the lines a switch or a removal replaces. */
	private fun lines(on: Boolean) = if (on) 3 else 2

	/** Fit the lens: merge its keys into the custom data and put its lines above the helmet's own lore. */
	private val FIT = """[{type:"minecraft:set_custom_data",tag:{ra:{goggles:1b,lensed:1b}}},""" +
		"""{type:"minecraft:set_lore",mode:"insert",offset:0,lore:${lore(on = true)}}]"""

	/** Replace the lens lines of state [from] with those of state [to] (empty when [to] is null). */
	private fun relore(from: Boolean, to: Boolean?) =
		"""{type:"minecraft:set_lore",mode:"replace_section",offset:0,size:${lines(from)},lore:${if (to == null) "[]" else lore(to)}}"""

	private const val RA_DATA = """equipment.head.components."minecraft:custom_data".ra"""

	/** Write storage ra:temp lens back as the helmet's `custom_data.ra`, the other keys untouched. */
	private const val WRITE_RA = """{type:"minecraft:copy_custom_data",source:{type:"minecraft:storage",source:"ra:temp"},ops:[{source:"lens",target:"ra",op:"replace"}]}"""

	/**
	 * Copy the worn helmet's `custom_data.ra` to storage ra:temp lens without the listed keys, for
	 * [WRITE_RA] to write back. Leaves storage empty when the helmet's data cannot be read; the
	 * caller must then change nothing, so the helmet's lore and keys cannot drift apart.
	 */
	private fun Function.readRaWithout(vararg keys: String) {
		raw("data remove storage ra:temp lens")
		raw("data modify storage ra:temp lens set from entity @s $RA_DATA")
		for (k in keys) raw("data remove storage ra:temp lens.$k")
	}

	/** The lore change and the key change in ONE modifier list: the item gets both or neither. */
	private fun rewrite(from: Boolean, to: Boolean?) = "[${relore(from, to)},$WRITE_RA]"

	fun Ra.registerGogglesLens() {
		pack.define(give) {
			raw("give @s ${ToolItems.gogglesLens.spec.giveArg()}")
			raw("""tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Goggles Lens",color:"light_purple"}]""")
		}
		pack.define(onUse) { onUse() }
		pack.define(menuAction) { menuAction() }
		pack.define(menu) { menu() }
		pack.define(toggle) { toggle() }
		pack.define(remove) { remove() }
		pack.define(toggleOff) { toggleOff() }
	}

	private fun Function.refuse(condition: String, message: String) {
		raw("""execute $condition run playsound minecraft:block.fire.extinguish player $SELF_IF_SOUND ~ ~ ~ 0.6 0.6""")
		raw("""execute $condition run return run title @s actionbar [{text:"$message",color:"red"}]""")
	}

	/** The lens was right-clicked. As the player, from the ra:tools/goggles_lens_use advancement. */
	private fun Function.onUse() {
		raw("advancement revoke @s only ra:tools/goggles_lens_use")
		// A consumable fires the advancement every tick the button is held: one fit per click.
		// hub/tick clears the pair once the button is released, as for the other tools.
		raw("tag @s add ra.lens_clicked")
		raw("execute if entity @s[tag=ra.lens_active] run return fail")
		raw("tag @s add ra.lens_active")

		refuse("if $LENSED", "That helmet already has a lens")
		refuse("if $HEAD *[minecraft:custom_data~{ra:{goggles:1b}}]", "Those are Goggles already")
		refuse("unless $HEAD #minecraft:head_armor", "Wear a helmet to fit the lens to")

		raw("item modify entity @s armor.head $FIT")
		// The lens is used up only once the helmet really carries it.
		refuse("unless $LENSED", "The lens did not fit that helmet")
		raw("clear @s *[minecraft:custom_data~{ra:{goggles_lens:1b}}] 1")
		raw("scoreboard players enable @s $TRIGGER")

		raw("""title ${noticeOnly("@s")} actionbar [{text:"Goggles Lens fitted",color:"green"},{text:" - /trigger $TRIGGER to switch it off",color:"gray"}]""")
		raw("particle minecraft:enchant ~ ~1.8 ~ 0.3 0.3 0.3 0.6 40 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.amethyst_block.resonate player ${soundListenersSel()} ~ ~ ~ 0.8 1.2")
		raw("playsound minecraft:entity.player.levelup player $SELF_IF_SOUND ~ ~ ~ 0.6 1.6")
	}

	/**
	 * One /trigger ra.goggles value. As the player, at the player. The score is consumed and the
	 * trigger re-enabled first, so a command that fails partway cannot leave it unusable.
	 */
	private fun Function.menuAction() {
		raw("execute store result score #lens.act ${Obj.TEMP} run scoreboard players get @s $TRIGGER")
		raw("scoreboard players set @s $TRIGGER 0")
		raw("scoreboard players enable @s $TRIGGER")
		raw("execute if score #lens.act ${Obj.TEMP} matches 2 run function ${toggle.asId()}")
		raw("execute if score #lens.act ${Obj.TEMP} matches 3 run return run function ${remove.asId()}")
		raw("function ${menu.asId()}")
	}

	/** The menu: one row, re-shown after a switch so it reads as a panel. Read from the worn helmet. */
	private fun Function.menu() {
		refuse("unless $LENSED", "Wear a helmet with a Goggles Lens first")
		val off = """{text:"[ OFF ]",color:"dark_gray",bold:true,hover_event:{action:"show_text",value:"Switch it back on"},click_event:{action:"run_command",command:"/trigger $TRIGGER set 2"}}"""
		val on = """{text:"[ ON ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Switch it off without removing it"},click_event:{action:"run_command",command:"/trigger $TRIGGER set 2"}}"""
		val rm = """{text:"[ REMOVE ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Take the lens out and get the item back"},click_event:{action:"run_command",command:"/trigger $TRIGGER set 3"}}"""
		raw("""tellraw @s [{text:""}]""")
		raw("""tellraw @s [{text:"── ",color:"dark_gray"},{text:"Goggles Lens",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]""")
		raw("""execute if $LENS_ON run tellraw @s [{text:"  Lens  ",color:"white"},$on,{text:"  "},$rm]""")
		raw("""execute unless $LENS_ON run tellraw @s [{text:"  Lens  ",color:"gray"},$off,{text:"  "},$rm]""")
		raw("""tellraw @s [{text:"Remove gives the lens back as an item.",color:"dark_gray"}]""")
	}

	/** Switch the worn helmet's lens off or on. The state is read once, before anything changes it. */
	private fun Function.toggle() {
		raw("execute unless $LENSED run return 0")
		raw("execute store success score #lens.was ${Obj.TEMP} if $LENS_ON")
		// Each way, the lines and the keys change in one modifier list, so they always agree.
		raw("execute if score #lens.was ${Obj.TEMP} matches 1 run function ${toggleOff.asId()}")
		raw("""execute if score #lens.was ${Obj.TEMP} matches 0 run item modify entity @s armor.head [${relore(from = false, to = true)},{type:"minecraft:set_custom_data",tag:{ra:{goggles:1b}}}]""")
		// Said from the helmet as it now is, so a switch that did not happen is not reported as one.
		raw("""execute unless $LENS_ON run title @s actionbar [{text:"Goggles Lens switched off",color:"gray"}]""")
		raw("""execute if $LENS_ON run title @s actionbar [{text:"Goggles Lens switched on",color:"green"}]""")
		raw("playsound minecraft:block.lever.click player $SELF_IF_SOUND ~ ~ ~ 0.7 1.4")
	}

	/** On to off: drop ra.goggles and swap the three "on" lines for the two "off" ones, together. */
	private fun Function.toggleOff() {
		readRaWithout("goggles")
		raw("execute unless data storage ra:temp lens run return fail")
		raw("item modify entity @s armor.head ${rewrite(from = true, to = false)}")
		raw("data remove storage ra:temp lens")
	}

	/** Take the lens out of the worn helmet and hand it back. Only the lens's own lines and keys go. */
	private fun Function.remove() {
		refuse("unless $LENSED", "Wear a helmet with a Goggles Lens first")
		raw("execute store success score #lens.was ${Obj.TEMP} if $LENS_ON")
		readRaWithout("goggles", "lensed")
		refuse("unless data storage ra:temp lens", "Could not read that helmet's lens")
		raw("execute if score #lens.was ${Obj.TEMP} matches 1 run item modify entity @s armor.head ${rewrite(from = true, to = null)}")
		raw("execute if score #lens.was ${Obj.TEMP} matches 0 run item modify entity @s armor.head ${rewrite(from = false, to = null)}")
		raw("data remove storage ra:temp lens")
		// Drop what the lens alone filled, so a helmet that was plain is plain again.
		raw("""execute if $HEAD *[minecraft:custom_data={ra:{}}] run item modify entity @s armor.head {type:"minecraft:set_components",components:{"!minecraft:custom_data":{}}}""")
		raw("""execute if $HEAD *[minecraft:lore=[]] run item modify entity @s armor.head {type:"minecraft:set_components",components:{"!minecraft:lore":{}}}""")
		// The lens comes back only once the helmet no longer carries it, so it can never be doubled.
		refuse("if $LENSED", "The lens would not come out of that helmet")
		raw("give @s ${ToolItems.gogglesLens.spec.giveArg()}")
		raw("""title @s actionbar [{text:"Goggles Lens removed",color:"yellow"}]""")
		raw("particle minecraft:enchant ~ ~1.8 ~ 0.3 0.3 0.3 0.4 25 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.grindstone.use player $SELF_IF_SOUND ~ ~ ~ 0.7 1.2")
	}
}
