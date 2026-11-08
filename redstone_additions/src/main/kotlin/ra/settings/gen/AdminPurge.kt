package ra.settings.gen

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Ns
import ra.core.Obj
import ra.core.RA_CHAT_PREFIX
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.settings.Fns
import ra.settings.SettingsObjectives.ADMIN_TAG
import ra.settings.purge.Purge
import ra.settings.purge.Purge.CONFIRM_TICKS
import ra.settings.purge.Purge.HIT
import ra.settings.purge.Purge.ME
import ra.settings.purge.Purge.OBJ
import ra.settings.purge.Purge.OBJ_T

/**
 * The Purge page of the server settings, `ra_settings:admin/purge/...`: one button per purge
 * option, each going through a review that counts what it would remove and arms a [Confirm]
 * for that player, that option, for 30 seconds.
 *
 * TWO STEPS, LIKE THE UNINSTALL, BUT TIED TO ONE PERSON. The review stores the option in the
 * player's `ra.purge` and the expiry in `ra.purge.t`. The confirm runs only for a player whose
 * armed option is that one and whose window has not passed, and re-checks `ra.admin` itself
 * (the dispatcher already did; a direct /function does not). A [Confirm] left in chat, another
 * admin's, or one for a different option does nothing.
 *
 * Every button here registers its action code as it is built, after every code the pages and the
 * index already had, so adding this page shifted nothing.
 */
internal class AdminPurge(private val pack: DataPack, private val actions: ActionTable) {
	private fun r(path: String) = ref(Ns.SETTINGS, "admin/purge/$path")
	private fun review(o: Purge.Option) = r("${o.id}/review")
	private fun confirm(o: Purge.Option) = r("${o.id}/confirm")
	private val cancel = r("cancel")
	private val expired = r("expired")
	private val stop = r("cleanup/stop")
	private val resume = r("cleanup/resume")

	private val T = Obj.SET_TMP

	fun write() {
		// Codes first, in a fixed order: they are what the buttons carry.
		val reviewBtn = Purge.options.associateWith { o ->
			Text.btn(o.title, review(o), "red", "Review what this would remove", actions.act(review(o), nav = true))
		}
		val confirmBtn = Purge.options.associateWith { o ->
			Text.btn("CONFIRM", confirm(o), "dark_red", "Remove them now — cannot be undone", actions.act(confirm(o), nav = true))
		}
		val cancelBtn = Text.btn("Cancel", cancel, "green", "Leave everything alone", actions.act(cancel, nav = true))
		val stopBtn = Text.btn("Stop", stop, "yellow", "Stop removing old RA entities as chunks load", actions.act(stop, nav = true))
		val resumeBtn = Text.btn("Resume", resume, "yellow", "Remove RA entities from before the last purge as chunks load", actions.act(resume, nav = true))
		val backBtn = Text.btn("Back", AdminTree.show, "gray", "Back to all server settings", actions.act(AdminTree.show, nav = true))

		pack.define(show) {
			function(Fns.adminGrant)
			raw("tellraw @s ${Text.bar("Purge Entities")}")
			raw("""tellraw @s [{text:"  Removes Redstone Additions entities from ",color:"gray"},{text:"loaded",color:"yellow"},{text:" chunks. Blocks stay as plain blocks; the pack, its settings and scoreboards stay installed.",color:"gray"}]""")
			for (o in Purge.options) raw("""tellraw @s [{text:"  "},${reviewBtn.getValue(o)},{text:" ${o.summary}",color:"gray"}]""")
			raw("""tellraw @s [{text:""}]""")
			// The unloaded-area clean-up, see ra.settings.purge.Purge.
			raw("""execute if score #sweep $OBJ matches 1 run tellraw @s [{text:"  Unloaded chunks: ",color:"white"},{text:"cleaning",color:"gold"},{text:" — RA entities from before the last purge are removed as their chunks load (",color:"gray"},{score:{name:"#swept",objective:"$OBJ"},color:"aqua"},{text:" so far). ",color:"gray"},$stopBtn]""")
			raw("""execute unless score #sweep $OBJ matches 1 if score #epoch $OBJ matches 1.. run tellraw @s [{text:"  Unloaded chunks: ",color:"white"},{text:"not cleaned",color:"gray"},{text:" — RA entities from before the last purge come back as they load. ",color:"gray"},$resumeBtn]""")
			raw("""execute unless score #epoch $OBJ matches 1.. run tellraw @s [{text:"  Unloaded chunks: ",color:"white"},{text:"this world has never been purged.",color:"gray"}]""")
			raw("""tellraw @s [{text:""}]""")
			raw(Text.buttonLine(backBtn))
		}

		for (o in Purge.options) {
			pack.define(review(o)) { review(o, confirmBtn.getValue(o), cancelBtn) }
			pack.define(confirm(o)) { confirm(o, reviewBtn.getValue(o)) }
		}

		pack.define(cancel) {
			disarm()
			raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"Purge cancelled. Nothing was removed.",color:"green"}]""")
		}

		pack.define(expired) {
			disarm()
			raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"That confirmation expired (${CONFIRM_TICKS / 20} s). Nothing was removed; review it again.",color:"red"}]""")
		}

		pack.define(stop) {
			raw("scoreboard players set #sweep $OBJ 0")
			raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"Clean-up stopped. RA entities from before the last purge stay where they are as chunks load.",color:"yellow"}]""")
			function(show)
		}

		pack.define(resume) {
			raw("""execute unless score #epoch $OBJ matches 1.. run return run tellraw @s [$RA_CHAT_PREFIX,{text:"This world has never been purged: there is nothing to clean up.",color:"gray"}]""")
			raw("scoreboard players set #sweep $OBJ 1")
			raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"Clean-up resumed: RA entities from before the last purge are removed as their chunks load.",color:"yellow"}]""")
			function(show)
		}
	}

	private fun Function.disarm() {
		raw("scoreboard players reset @s $OBJ")
		raw("scoreboard players reset @s $OBJ_T")
	}

	/** The red warning: what goes, how many are loaded now, and the armed [Confirm]. */
	private fun Function.review(o: Purge.Option, confirmBtn: String, cancelBtn: String) {
		// A confirmation belongs to a person; the console and command blocks have nobody to arm.
		raw("execute unless entity @s[type=minecraft:player] run return fail")
		// Reached through the panel (already tagged) or by an operator's /function (permission 2):
		// either way they may hold the tag the [Confirm] needs.
		function(Fns.adminGrant)

		function(o.count)
		raw("tag @e[tag=$HIT] remove $HIT")

		raw("scoreboard players set @s $OBJ ${o.code}")
		raw("execute store result score @s $OBJ_T run time query gametime")
		raw("scoreboard players add @s $OBJ_T $CONFIRM_TICKS")

		raw("""tellraw @s [{text:"\n"},$RA_CHAT_PREFIX,{text:"Purge: ${o.title}?",color:"red",bold:true}]""")
		for (line in o.warning) raw("""tellraw @s [{text:"  $line",color:"gray"}]""")
		raw("""tellraw @s [{text:"  Loaded now: ",color:"white"},${counts(o)}]""")
		raw("""tellraw @s [{text:"  Only loaded chunks are counted and purged.",color:"yellow"}]""")
		raw("""tellraw @s [{text:"  "},$confirmBtn,{text:"   "},$cancelBtn,{text:"   valid for ${CONFIRM_TICKS / 20} s, for you only",color:"dark_gray"}]""")
	}

	private fun Function.confirm(o: Purge.Option, reviewBtn: String) {
		raw("""execute unless entity @s[tag=$ADMIN_TAG] run return run tellraw @s [$RA_CHAT_PREFIX,{text:"You do not have server-settings access.",color:"red"}]""")
		raw("""execute unless score @s $OBJ matches ${o.code} run return run tellraw @s [$RA_CHAT_PREFIX,{text:"Nothing to confirm: review this purge first. ",color:"red"},$reviewBtn]""")
		raw("execute store result score #now $T run time query gametime")
		raw("execute if score #now $T > @s $OBJ_T run return run function ${expired.asId()}")
		disarm()

		// Counted again: the review's numbers are up to 30 s old.
		function(o.count)
		function(o.run)
		raw("tag @e[tag=$HIT] remove $HIT")

		raw("""tellraw @s [$RA_CHAT_PREFIX,{text:"Purged (${o.title}): ",color:"green"},${counts(o)}]""")
		if (o == Purge.all) {
			raw("""tellraw @s [{text:"  Unloaded chunks are cleaned as they load. Stop that from ",color:"gray"},{text:"Server Settings → Purge",color:"yellow"},{text:".",color:"gray"}]""")
		}

		// The other admins: something this large should not happen silently.
		raw("tag @s add $ME")
		raw("""tellraw @a[tag=$ADMIN_TAG,tag=!$ME] [$RA_CHAT_PREFIX,{selector:"@s",color:"yellow"},{text:" purged ${o.title.lowercase()}: ",color:"gray"},{score:{name:"#p.n",objective:"$T"},color:"aqua"},{text:" entities removed.",color:"gray"}]""")
		raw("tag @s remove $ME")
	}

	/** "N block markers, N displays, …" from the scores [Purge.Option.count] left. */
	private fun counts(o: Purge.Option): String {
		val parts = o.categories.map { c ->
			"""{score:{name:"${c.holder}",objective:"$T"},color:"aqua"},{text:" ${c.label}",color:"gray"}"""
		}.toMutableList()
		if (o.forceloads) parts += """{score:{name:"#p.f",objective:"$T"},color:"aqua"},{text:" chunk loader forceloads",color:"gray"}"""
		return parts.joinToString(""",{text:", ",color:"gray"},""")
	}

	companion object {
		val show = ref(Ns.SETTINGS, "admin/purge/show")
	}
}
