package ra.modules.jetpacks.kit

import io.github.ayfri.kore.arguments.ARMOR
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.utils.nbt
import io.github.ayfri.kore.utils.set
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.modules.jetpacks.Jp
import ra.modules.jetpacks.Jp.PARTICLE_VIEWERS
import ra.modules.jetpacks.Jp.SELF_IF_SOUND
import ra.modules.jetpacks.Jp.tmp
import ra.modules.jetpacks.Kits
import ra.modules.jetpacks.selfItem

/**
 * The upgrade menu, reached with /trigger ra.jp.kits. Its buttons come back through the same
 * trigger with a value:
 *
 *    1       open the menu          (what a bare /trigger ra.jp.kits does)
 *    11..13  toggle a kit off or on ([Kits.Upgrade.toggle])
 *    21..23  remove a kit and hand the item back ([Kits.Upgrade.drop])
 */
object KitMenu {
	val menuAction = ref(Ns.JETPACKS, "kit/menu_action")
	val menu = ref(Ns.JETPACKS, "kit/menu")
	/** Macro `{key, mute, label, toggle, drop}`. */
	val menuRow = ref(Ns.JETPACKS, "kit/menu_row")
	/** Macro `{mute, label}`. */
	val toggle = ref(Ns.JETPACKS, "kit/toggle")
	/** Macro `{bit, key, kit, label}`. */
	val remove = ref(Ns.JETPACKS, "kit/remove")

	fun Ra.registerKitMenu() {
		pack.define(menuAction) { menuAction() }
		pack.define(menu) { menu() }
		pack.define(menuRow) { menuRow() }
		pack.define(toggle) { toggle() }
		pack.define(remove) { remove() }
	}

	/**
	 * Handle one /trigger ra.jp.kits value. As the player, at the player.
	 *
	 * The score is consumed and the trigger re-enabled first, so a command that fails partway
	 * cannot leave the player stuck with a trigger they can never fire again.
	 */
	private fun Function.menuAction() {
		val act = tmp("#jp.act")
		raw("execute store result score #jp.act ${Obj.TEMP} run scoreboard players get @s ${Jp.KITS}")
		scoreboard.players.set(self(), Jp.KITS, 0)
		scoreboard.players.enable(self(), Jp.KITS)

		for (u in Kits.upgrades) execute {
			ifCondition { score(act, Obj.TEMP) equalTo u.toggle }
			run { function(toggle, nbt { this["mute"] = u.mute; this["label"] = u.name }) }
		}
		for (u in Kits.upgrades) execute {
			ifCondition { score(act, Obj.TEMP) equalTo u.drop }
			run { function(remove, nbt { this["bit"] = u.bit; this["key"] = u.key; this["kit"] = u.kit; this["label"] = u.name }) }
		}

		function(menu)
	}

	/**
	 * Show the upgrade menu, re-shown after every action so it behaves like a panel rather than a
	 * one-shot message.
	 *
	 * Everything it reports is read from the worn chestplate, because that is where fitted
	 * upgrades live. The one thing it keeps on the player is which of them are switched OFF --
	 * see [toggle] for why that side belongs to the player.
	 */
	private fun Function.menu() {
		execute {
			unlessCondition { selfItem(ARMOR.CHEST) { this["jetpack"] = true } }
			run { returnRun(KitFitting.needsJetpack) }
		}

		raw("""tellraw @s [{text:""}]""")
		raw("""tellraw @s [{text:"── ",color:"dark_gray"},{text:"Jetpack Upgrades",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]""")

		for (u in Kits.upgrades) function(menuRow, nbt {
			this["key"] = u.key; this["mute"] = u.mute; this["label"] = u.name; this["toggle"] = u.toggle; this["drop"] = u.drop
		})

		raw("""tellraw @s [{text:"Remove",color:"dark_gray"},{text:" gives the kit back as an item.",color:"dark_gray"}]""")
	}

	/** One line of the menu. Three states, and only one line is ever printed: not fitted, fitted and on, fitted and off. */
	private fun Function.menuRow() {
		val fitted = "items entity @s armor.chest *[minecraft:custom_data~{ra:{\$(key):1b}}]"
		val remove = """{text:"[ REMOVE ]",color:"red",bold:true,hover_event:{action:"show_text",value:"Take the kit off and get the item back"},click_event:{action:"run_command",command:"/trigger ${Jp.KITS} set ${'$'}(drop)"}}"""
		raw("""${'$'}execute unless $fitted run tellraw @s [{text:"  ${'$'}(label)  ",color:"gray"},{text:"not fitted",color:"dark_gray",italic:true}]""")
		raw("""${'$'}execute if $fitted unless entity @s[tag=${'$'}(mute)] run tellraw @s [{text:"  ${'$'}(label)  ",color:"white"},{text:"[ ON ]",color:"green",bold:true,hover_event:{action:"show_text",value:"Switch it off without removing it"},click_event:{action:"run_command",command:"/trigger ${Jp.KITS} set ${'$'}(toggle)"}},{text:"  ",color:"gray"},$remove]""")
		raw("""${'$'}execute if $fitted if entity @s[tag=${'$'}(mute)] run tellraw @s [{text:"  ${'$'}(label)  ",color:"gray"},{text:"[ OFF ]",color:"dark_gray",bold:true,hover_event:{action:"show_text",value:"Switch it back on"},click_event:{action:"run_command",command:"/trigger ${Jp.KITS} set ${'$'}(toggle)"}},{text:"  ",color:"gray"},$remove]""")
	}

	/**
	 * Switch one fitted upgrade off or on without removing it. As the player.
	 *
	 * THE STATE IS READ BEFORE ANYTHING CHANGES IT
	 * This was two lines: remove the tag if present, then add it if absent. The second line runs
	 * AFTER the first, sees the tag the first line just removed, and puts it straight back -- so
	 * every click ended muted and a kit could be switched off but never on again. The same shape
	 * broke the Block Breaker's cooldown once already: a condition re-tested after your own first
	 * line has invalidated it.
	 *
	 * WHY "OFF" IS ON THE PLAYER AND "FITTED" IS ON THE ITEM
	 * Being fitted is a property of the jetpack -- it survives being handed to someone else, and it
	 * cost a kit. Being switched off is a preference of whoever is wearing it, in the same family
	 * as /trigger ra.jp.sound and ra.jp.power. It is also the only place it can go cheaply: fitted
	 * state is one generated item modifier per reachable combination, and an on/off flag per kit
	 * would take that from sixteen files to a hundred and twenty-eight.
	 */
	private fun Function.toggle() {
		scoreboard.players.set(tmp("#jp.was"), Obj.TEMP, 0)
		raw("\$execute if entity @s[tag=\$(mute)] run scoreboard players set #jp.was ${Obj.TEMP} 1")

		raw("\$execute if score #jp.was ${Obj.TEMP} matches 1 run tag @s remove \$(mute)")
		raw("\$execute if score #jp.was ${Obj.TEMP} matches 0 run tag @s add \$(mute)")

		raw("\$execute if score #jp.was ${Obj.TEMP} matches 1 run title @s actionbar [{text:\"\$(label) switched on\",color:\"green\"}]")
		raw("\$execute if score #jp.was ${Obj.TEMP} matches 0 run title @s actionbar [{text:\"\$(label) switched off\",color:\"gray\"}]")
		raw("playsound minecraft:block.lever.click player $SELF_IF_SOUND ~ ~ ~ 0.7 1.4")
	}

	/**
	 * Take one upgrade off the chestplate and hand the kit back. As the player.
	 *
	 * The same read-whole-state / write-whole-state route fitting uses, with the bit cleared
	 * instead of set -- see [FitModifiers] for why an item modifier cannot simply unset one flag.
	 */
	private fun Function.remove() {
		raw("\$execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{\$(key):1b}}] run return 0")

		function(KitFitting.readFitted)
		raw("\$scoreboard players remove #jp.n ${Obj.TEMP} \$(bit)")
		function(KitFitting.writeFitted)

		// Switched-off state goes with it, or refitting the kit later would arrive muted for no
		// visible reason.
		raw("\$execute if entity @s[tag=ra.jp.mute_\$(kit)] run tag @s remove ra.jp.mute_\$(kit)")

		// Hand the kit back. The beet pack wrote `$function ra_jetpacks:items/give_$(kit)_kit`;
		// PackValidator cannot read a macro inside a function path and rejects that line, so the
		// kit is picked by its bit instead -- same function, same result.
		raw("\$scoreboard players set #jp.rm ${Obj.TEMP} \$(bit)")
		for (u in Kits.upgrades) execute {
			ifCondition { score(tmp("#jp.rm"), Obj.TEMP) equalTo u.bit }
			run { function(u.give) }
		}

		raw("\$title @s actionbar [{text:\"\$(label) kit removed\",color:\"yellow\"}]")
		raw("particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.4 25 normal $PARTICLE_VIEWERS")
		raw("playsound minecraft:block.grindstone.use player $SELF_IF_SOUND ~ ~ ~ 0.7 1.2")
	}
}
