package ra.tools.goggles.lens

import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.block.particleViewersSel
import ra.core.block.soundListenersSel
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
 * THE HELMET MUST BE PLAIN
 * `set_components` writes `custom_data` and `lore` whole, and removing the lens deletes both. So
 * the lens only goes onto a helmet that has neither: anything carrying custom data is some pack's
 * item (the Goggles themselves, another data pack's gear) whose data we would destroy, and lore
 * would be overwritten. Refusing is honest; silently erasing someone's item is not.
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

	private const val HEAD = "items entity @s armor.head"
	const val LENSED = "$HEAD *[minecraft:custom_data~{ra:{lensed:1b}}]"
	private const val LENS_ON = "$HEAD *[minecraft:custom_data~{ra:{lensed:1b,goggles:1b}}]"
	private const val SELF_IF_SOUND = "@s[scores={${Obj.USER_SOUND}=1..}]"

	private fun line(text: String, color: String) = """{text:"$text",color:"$color",italic:false}"""

	/** The helmet's whole lens state, as an inline item modifier. */
	private fun state(on: Boolean): String {
		val data = if (on) "{ra:{goggles:1b,lensed:1b}}" else "{ra:{lensed:1b}}"
		val lore = if (on) listOf(
			line("Goggles Lens - on", "aqua"),
			line("Shows every block's status, like the Goggles", "gray"),
			line("/trigger $TRIGGER switches it off", "dark_gray"),
		) else listOf(
			line("Goggles Lens - off", "dark_gray"),
			line("/trigger $TRIGGER switches it on", "dark_gray"),
		)
		return """{type:"minecraft:set_components",components:{"minecraft:custom_data":$data,"minecraft:lore":${lore.joinToString(",", "[", "]")}}}"""
	}

	private const val STRIP = """{type:"minecraft:set_components",components:{"!minecraft:custom_data":{},"!minecraft:lore":{}}}"""

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
		refuse("if $HEAD *[minecraft:custom_data]", "That helmet carries data of its own")
		refuse("if $HEAD *[minecraft:lore]", "That helmet carries a description of its own")

		raw("item modify entity @s armor.head ${state(on = true)}")
		raw("clear @s *[minecraft:custom_data~{ra:{goggles_lens:1b}}] 1")
		raw("scoreboard players enable @s $TRIGGER")

		raw("""title @s actionbar [{text:"Goggles Lens fitted",color:"green"},{text:" - /trigger $TRIGGER to switch it off",color:"gray"}]""")
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
		raw("execute if score #lens.was ${Obj.TEMP} matches 1 run item modify entity @s armor.head ${state(on = false)}")
		raw("execute if score #lens.was ${Obj.TEMP} matches 0 run item modify entity @s armor.head ${state(on = true)}")
		raw("""execute if score #lens.was ${Obj.TEMP} matches 1 run title @s actionbar [{text:"Goggles Lens switched off",color:"gray"}]""")
		raw("""execute if score #lens.was ${Obj.TEMP} matches 0 run title @s actionbar [{text:"Goggles Lens switched on",color:"green"}]""")
		raw("playsound minecraft:block.lever.click player $SELF_IF_SOUND ~ ~ ~ 0.7 1.4")
	}

	/** Take the lens out of the worn helmet and hand it back. The helmet is plain again. */
	private fun Function.remove() {
		refuse("unless $LENSED", "Wear a helmet with a Goggles Lens first")
		raw("item modify entity @s armor.head $STRIP")
		raw("give @s ${ToolItems.gogglesLens.spec.giveArg()}")
		raw("""title @s actionbar [{text:"Goggles Lens removed",color:"yellow"}]""")
		raw("particle minecraft:enchant ~ ~1.8 ~ 0.3 0.3 0.3 0.4 25 normal ${particleViewersSel()}")
		raw("playsound minecraft:block.grindstone.use player $SELF_IF_SOUND ~ ~ ~ 0.7 1.2")
	}
}
