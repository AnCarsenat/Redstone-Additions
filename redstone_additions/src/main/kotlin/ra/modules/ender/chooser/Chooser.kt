package ra.modules.ender.chooser

import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom
import ra.modules.ender.AnchorTerminal
import ra.modules.ender.anchors.AnchorTable
import ra.modules.ender.chooser.ChooserInput.registerChooserInput
import ra.modules.ender.tablet.AnchorTablet

/**
 * THE DESTINATION MENU of the Anchor Terminal and the Anchor Tablet: one chat panel, one trigger.
 *
 * The panel lists the favourites of whatever opened it (the terminal's `favourites` property, the
 * tablet's own `favs` on its custom_data) with a [Go] each, and a [Type a destination] that asks
 * for an anchor id through the shared Input Form book (ra_lib:input). Nothing lists the anchors of
 * the world: a destination is a favourite the player saved, or an id they type.
 *
 * Every button runs `/trigger ra.ender.go set <n>`:
 *
 *    1       show the menu again       (what a bare /trigger ra.ender.go does)
 *    2       type a destination        (the Input Form book)
 *    3       the tablet's last trip
 *    4       save the tablet's last trip as a favourite
 *    11..18  favourite 1..8
 *    21..28  forget the tablet's favourite 1..8
 *
 * WHICH MENU. [SRC] says what opened it: 1 the terminal (the player must still stand on one when
 * they click), 2 the tablet (it must be in their main hand; its hotbar slot is kept in
 * [AnchorTablet.SLOT] for the book, which takes the hand). The favourites are read again from the
 * terminal or the item on every click, never trusted from the moment the menu was drawn.
 *
 * The trigger is enabled while a menu is live: [MENU] holds the game time it expires, and the
 * module tick takes the trigger back after that (one selector while no menu is open anywhere).
 */
object Chooser {
	private fun fn(path: String) = ref(Ns.ENDER, "chooser/$path")

	/** As a player, [SRC] set and `storage ra:ender menu` {favs, last} loaded: draw the menu. */
	val open = fn("open")
	/** As a player whose [GO] is 1 or more. */
	val act = fn("act")
	/** As a player with [MENU] set: close the menu once it has expired. */
	val expire = fn("expire")
	/** As a player: take the trigger back. */
	val close = fn("close")
	/**
	 * As a player, `storage ra:ender lookup.id` the wanted id: clean it, find it, pay for it (the
	 * tablet), send the player there. Returns 1 when sent.
	 */
	val go = fn("go")

	/** The menu's buttons. */
	const val GO = "ra.ender.go"
	/** The game time a player's menu expires at. */
	const val MENU = "ra.ender.menu"
	/** 1 the Anchor Terminal opened the menu, 2 the Anchor Tablet. */
	const val SRC = "ra.ender.src"

	/** How long a drawn menu stays clickable: one minute. */
	private const val LIFETIME = 1200

	/** Favourites shown, at most (and kept, on the tablet). */
	const val MAX_FAVS = 8

	private const val T = Obj.TEMP
	private const val PREFIX = """{text:"[RA] ",color:"gold"}"""

	internal fun say(text: String, color: String = "gray") = """tellraw @s [$PREFIX,{text:"$text",color:"$color"}]"""

	private fun button(label: String, color: String, code: String, hover: String) =
		"""{text:"$label",color:"$color",click_event:{action:"run_command",command:"/trigger $GO set $code"},hover_event:{action:"show_text",value:"$hover"}}"""

	fun Ra.registerChooser() {
		pack.define(open) { open() }
		pack.define(act) { act() }
		pack.define(expire) {
			raw("execute store result score #ender.now $T run time query gametime")
			raw("execute if score @s $MENU < #ender.now $T run function ${close.asId()}")
		}
		pack.define(close) {
			raw("scoreboard players reset @s $GO")
			raw("scoreboard players reset @s $MENU")
		}
		pack.define(go) { go() }
		registerChooserInput()
	}

	private fun Function.open() {
		raw("scoreboard players enable @s $GO")
		raw("execute store result score @s $MENU run time query gametime")
		raw("scoreboard players add @s $MENU $LIFETIME")

		raw("tellraw @s \"\"")
		raw("""execute if score @s $SRC matches 1 run tellraw @s [{text:"── ",color:"dark_gray"},{text:"${AnchorTerminal.block.name}",color:"light_purple",bold:true},{text:" ──",color:"dark_gray"}]""")
		raw("""execute if score @s $SRC matches 2 run tellraw @s [{text:"── ",color:"dark_gray"},{text:"Anchor Tablet",color:"light_purple",bold:true},{text:" ──",color:"dark_gray"}]""")

		// One line per favourite that is not empty; the button carries the index, never the id.
		raw("scoreboard players set #ender.nf $T 0")
		for (i in 0 until MAX_FAVS) {
			raw("data remove storage ra:ender menu.e")
			raw("data modify storage ra:ender menu.e set from storage ra:ender menu.favs[$i]")
			val shown = """execute if data storage ra:ender menu.e unless data storage ra:ender menu{e:""}"""
			raw("$shown run scoreboard players add #ender.nf $T 1")
			val goBtn = button("[Go]", "green", "${11 + i}", "Teleport to this anchor")
			val line = """{text:"  ★ ",color:"dark_gray"},{nbt:"menu.favs[$i]",storage:"ra:ender",color:"aqua"},{text:"  "},$goBtn"""
			raw("$shown if score @s $SRC matches 1 run tellraw @s [$line]")
			val forgetBtn = button("[Forget]", "red", "${21 + i}", "Remove it from this tablet's favourites")
			raw("""$shown if score @s $SRC matches 2 run tellraw @s [$line,{text:" "},$forgetBtn]""")
		}
		raw("data remove storage ra:ender menu.e")
		raw("""execute if score #ender.nf $T matches 0 if score @s $SRC matches 1 run tellraw @s [{text:"  No favourites: add some to its ",color:"gray",italic:true},{text:"favourites",color:"aqua",italic:true},{text:" with the Data Handler.",color:"gray",italic:true}]""")
		raw("""execute if score #ender.nf $T matches 0 if score @s $SRC matches 2 run tellraw @s [{text:"  No favourites yet: after a trip, ",color:"gray",italic:true},{text:"[Save]",color:"yellow",italic:true},{text:" keeps it here.",color:"gray",italic:true}]""")

		// The tablet's last trip.
		val last = """execute if score @s $SRC matches 2 if data storage ra:ender menu.last unless data storage ra:ender menu{last:""}"""
		raw("""$last run tellraw @s [{text:"  Last: ",color:"gray"},{nbt:"menu.last",storage:"ra:ender",color:"aqua"},{text:"  "},${button("[Go]", "green", "3", "Teleport there again")},{text:" "},${button("[Save]", "yellow", "4", "Add it to this tablet's favourites")}]""")

		raw("""tellraw @s [{text:"  "},${button("[Type a destination]", "yellow", "2", "Write an anchor id in the Input Form book")}]""")

		// What a trip costs, from the server settings.
		raw("execute if score @s $SRC matches 2 run function ${AnchorTablet.showCost.asId()}")
	}

	private fun Function.act() {
		raw("scoreboard players operation #ender.act $T = @s $GO")
		raw("scoreboard players set @s $GO 0")
		raw("scoreboard players enable @s $GO")

		// An expired menu (the tick takes the trigger back, but not before this tick's click).
		raw("execute store result score #ender.now $T run time query gametime")
		raw("execute if score @s $MENU < #ender.now $T run return run function ${close.asId()}")

		// Where the menu belongs, read again now: the favourites may have changed since it was drawn.
		raw("data modify storage ra:ender menu set value {}")
		raw("scoreboard players set #ender.ctx $T 0")
		raw("execute if score @s $SRC matches 1 store result score #ender.ctx $T run function ${AnchorTerminal.find.asId()}")
		raw("execute if score @s $SRC matches 2 store result score #ender.ctx $T run function ${AnchorTablet.loadHeld.asId()}")
		raw("execute if score #ender.ctx $T matches 0 if score @s $SRC matches 1 run return run ${say("Stand on the ${AnchorTerminal.block.name} to use its menu.", "red")}")
		raw("execute if score #ender.ctx $T matches 0 if score @s $SRC matches 2 run return run ${say("Hold the Anchor Tablet to use its menu.", "red")}")

		raw("execute if score #ender.act $T matches 1 run return run function ${open.asId()}")
		raw("execute if score #ender.act $T matches 2 run return run function ${ChooserInput.ask.asId()}")

		raw("data remove storage ra:ender lookup")
		raw("execute if score #ender.act $T matches 3 if score @s $SRC matches 2 unless data storage ra:ender menu{last:\"\"} run data modify storage ra:ender lookup.id set from storage ra:ender menu.last")
		for (i in 0 until MAX_FAVS) {
			raw("execute if score #ender.act $T matches ${11 + i} run data modify storage ra:ender lookup.id set from storage ra:ender menu.favs[$i]")
		}
		raw("execute if data storage ra:ender lookup.id run return run function ${go.asId()}")

		// The tablet's own list.
		raw("execute if score @s $SRC matches 2 if score #ender.act $T matches 4 run return run function ${AnchorTablet.save.asId()}")
		raw("execute if score @s $SRC matches 2 if score #ender.act $T matches 21..28 run return run function ${AnchorTablet.forget.asId()}")
		raw("execute if score #ender.act $T matches 3..18 run ${say("Nothing is saved there any more.", "red")}")
	}

	private fun Function.go() {
		raw("""execute unless data storage ra:ender lookup.id run return run ${say("That favourite is empty.", "red")}""")
		// In format ID, as every anchor id is: `Main base` is `Mainbase`, as the anchor cleaned its own.
		cleanFrom("storage ra:ender lookup.id", TextFormat.ID)
		raw("""execute if score ${SafeClean.OK} $T matches 0 run return run ${say("An anchor id takes ${TextFormat.ID.hint}.", "red")}""")
		raw("data modify storage ra:ender lookup.id set from storage ra:safe out")

		raw("execute store result score #ender.found $T run function ${AnchorTable.lookup.asId()}")
		raw("""execute if score #ender.found $T matches 0 run return run tellraw @s [$PREFIX,{text:"No anchor is named ",color:"red"},{nbt:"lookup.id",storage:"ra:ender",color:"yellow"},{text:".",color:"red"}]""")
		raw("""execute if data storage ra:ender lookup.dest{dim:""} run return run tellraw @s [$PREFIX,{nbt:"lookup.id",storage:"ra:ender",color:"yellow"},{text:" is in a dimension anchors cannot reach.",color:"red"}]""")

		// The tablet pays first: a cooldown, ender pearls (server settings).
		raw("scoreboard players set #ender.pay $T 1")
		raw("execute if score @s $SRC matches 2 store result score #ender.pay $T run function ${AnchorTablet.canPay.asId()}")
		raw("execute if score #ender.pay $T matches 0 run return 0")

		raw("data modify storage ra:ender trip set from storage ra:ender lookup.id")
		raw("execute store result score #ender.sent $T run function ${AnchorTable.send.asId()}")
		raw("data remove storage ra:ender lookup")
		raw("execute if score #ender.sent $T matches 0 run return run data remove storage ra:ender trip")
		raw("execute if score @s $SRC matches 2 run function ${AnchorTablet.charge.asId()}")

		raw("""title @s actionbar [{text:"→ ",color:"dark_purple"},{nbt:"trip",storage:"ra:ender",color:"light_purple"}]""")
		raw("data remove storage ra:ender trip")
		// A trip closes the menu; the next one is a new sneak on a terminal, or a new click of the tablet.
		raw("function ${close.asId()}")
		raw("return 1")
	}
}
