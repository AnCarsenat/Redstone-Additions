package ra.modules.ender

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.recipes.types.RecipeCategory
import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Items
import ra.api.ModulesApi
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.block.CustomBlock
import ra.core.block.gogglesHeader
import ra.core.block.placeBlock
import ra.core.block.stackReset
import ra.core.block.stackedPropLine
import ra.core.define
import ra.core.raw
import ra.core.item.loreLine
import ra.core.blockRegisterMessage
import ra.core.executeRun
import ra.core.block.placementGuard
import ra.core.block.setOnNew
import ra.core.block.defaultProperty
import ra.core.block.finishPlacement
import ra.core.block.blockTick
import ra.core.block.BreakTest
import ra.core.block.markerSel
import ra.core.block.soundListenersSel
import ra.core.block.particleViewersSel

/**
 * Teleport Anchor: sends whoever stands on it to the anchor the redstone strength names.
 *
 * `anchor_id` is what other anchors aim at. `targets` is the table of fifteen ids, one per
 * redstone strength: signal 1 uses targets[0], signal 15 uses targets[14]. An empty string means
 * "nothing wired to that strength".
 */
object TeleportAnchor {
	val block = CustomBlock(Ns.ENDER, "teleport_anchor", "Teleport Anchor", model = "minecraft:crying_obsidian")
	val item = block.enderItem(
		"epic",
		loreLine("Teleports whoever stands on it", "gray"),
		loreLine("Redstone strength 1-15 picks the target id", "gray"),
		loreLine("/function ${ModulesApi.Ender.anchorHelp.asId()}", "dark_gray"),
	)

	/** Fifteen empty rows. */
	const val EMPTY_TABLE = """["","","","","","","","","","","","","","",""]"""

	val process = block.fn("process")
	val readTarget = block.fn("read_target")
	private val findDest = block.fn("find_dest")
	private val sendPlayer = block.fn("send_player")

	private const val T = Obj.TEMP

	/**
	 * The zero-based targets row the signal names, for `/compute entity @s`: min(power, 15) - 1.
	 * ra_lib reports 16 for a redstone block; the table only has fifteen rows, so that is 15.
	 */
	private val ANCHOR_SLOT = EnderCip.sub(EnderCip.min(EnderCip.thisScore(Obj.POWER), 15), 1)

	fun Ra.registerTeleportAnchor() {
		block(block)
		pack.blockRegisterMessage(block)
		pack.enderGive(
			block, item,
			"""{text:" — set its id and target table with ",color:"gray"},{text:"/function ${ModulesApi.Ender.anchorHelp.asId()}",color:"aqua"}""",
		)
		pack.enderRecipe(block, item, RecipeCategory.MISC) {
			pattern("OEO", "ECE", "OEO")
			key("O", Items.OBSIDIAN)
			key("E", Items.ENDER_PEARL)
			key("C", Items.CRYING_OBSIDIAN)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.anchor_id", "\"A\"")
			setOnNew(block, "data.properties.targets", EMPTY_TABLE)
			finishPlacement(block)
		}

		pack.define(process) { process() }

		// Macro {i}: read one row of the target table into storage ra:ender anchor.want. As the
		// anchor marker. Ids are strings, so the row is copied rather than stored in a score, and
		// the match downstream happens in a macro.
		pack.define(readTarget) {
			raw("data remove storage ra:ender anchor.want")
			raw("\$data modify storage ra:ender anchor.want set from entity @s data.properties.targets[\$(i)]")
		}

		// Macro {want}: tag the nearest OTHER anchor whose anchor_id is `want`. As the sending
		// anchor marker, wearing ra.ender.self.
		//
		// The id test sits INSIDE the selector (`nbt=`), before `sort=nearest,limit=1`. The beet
		// line (`as @e[...,limit=1,sort=nearest] if data entity @s data.properties{anchor_id:...}`)
		// limited first and tested after, so it only ever looked at the single nearest anchor:
		// with a third anchor closer than the target, nothing was tagged. v5.1.8 then deleted
		// that line altogether while dropping the `enabled` clause, and from then on no anchor
		// teleported. The nbt= scan is only paid on a redstone pulse with a player in range.
		pack.define(findDest) {
			// The id test before sort/limit, as explained above.
			val dest = block.markerSel("tag=!${EnderIds.SELF}", "nbt={data:{properties:{anchor_id:\"\$(want)\"}}}", "sort=nearest", "limit=1")
			raw("\$execute as $dest run tag @s add ${EnderIds.TP_DEST}")
		}

		// kept: beet contract. Tag this anchor if its id is the one being aimed at, first match
		// wins. As a candidate anchor marker. Nothing calls it any more (ids became strings,
		// matched in find_dest's macro); kept because the beet pack shipped it.
		pack.define(block.fn("match_target")) {
			raw("execute if entity @e[type=marker,tag=${EnderIds.TP_DEST},limit=1] run return 0")

			raw("execute store result score #ender.candidate $T run data get entity @s data.properties.anchor_id")
			raw("execute unless score #ender.candidate $T = #ender.target $T run return 0")

			tag(self()) { add(EnderIds.TP_DEST) }
		}

		// Move one player to the tagged destination anchor. As the player.
		pack.define(sendPlayer) {
			// `at` the destination marker rather than a macro with its coordinates: the marker sits
			// at the middle of its block, so ~ ~1 ~ is standing on top of it.
			raw("execute at @e[type=marker,tag=${EnderIds.TP_DEST},limit=1] run tp @s ~ ~1 ~")

			// Long enough that an anchor which is itself powered does not bounce the player straight
			// back, short enough to be usable as a two-way door.
			scoreboard.players.set(self(), EnderIds.GRACE, 30)

			raw("playsound minecraft:entity.enderman.teleport player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1")
			raw("execute at @s run particle minecraft:portal ~ ~1 ~ 0.4 0.8 0.4 0.3 40")
		}

		pack.define(block.goggles) { goggles() }

		val onBreak = pack.enderOnBreak(block, item)
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.CRYING_OBSIDIAN)) {
			// Defaults for anchors placed before a property existed.
			defaultProperty(block, "anchor_id", "\"A\"")
			defaultProperty(block, "targets", EMPTY_TABLE)

			// A selector on scores skips entities that have no score at all, so every anchor needs
			// one before the cooldown gate can mean anything. Adding zero creates it.
			raw("scoreboard players add ${block.markerSel()} ${EnderIds.TP_CD} 0")
			raw("scoreboard players remove ${block.markerSel("scores={${EnderIds.TP_CD}=1..}")} ${EnderIds.TP_CD} 1")
			executeRun("as ${block.markerSel("scores={${EnderIds.TP_CD}=..0}")} at @s", process)
		}
	}

	/** Send whoever is standing here to the anchor the redstone strength names. As the anchor marker. */
	private fun Function.process() {
		// Nobody to send. Checked before the redstone scan, which is the expensive part.
		raw("execute unless entity @a[distance=..2,scores={${EnderIds.GRACE}=..0}] run return 0")

		// Strength of the strongest side, 0-16. ra_lib reports 16 for a redstone block, which is
		// stronger than any dust can be; the table only has fifteen rows, so it is treated as 15.
		function(RaLib.Redstone.detect)
		raw("execute if score @s ${Obj.POWER} matches ..0 run return 0")
		// targets is zero-based, the signal is one-based: anchor.i = min(power, 15) - 1.
		storeCompute("store result storage ra:ender anchor.i int 1", ANCHOR_SLOT, source = "entity @s")

		function(readTarget, Storage.ENDER, "anchor")
		raw("execute unless data storage ra:ender anchor.want run return 0")
		raw("execute if data storage ra:ender anchor{want:\"\"} run return 0")

		// Find the destination. The tag is the handle for the teleport, since carrying a position
		// through a macro would mean formatting doubles. `self` keeps an anchor from sending a
		// player onto itself when a row names its own id.
		raw("tag @e[type=marker,tag=${EnderIds.TP_DEST}] remove ${EnderIds.TP_DEST}")
		tag(self()) { add(EnderIds.SELF) }
		function(findDest, Storage.ENDER, "anchor")
		tag(self()) { remove(EnderIds.SELF) }
		raw("execute unless entity @e[type=marker,tag=${EnderIds.TP_DEST}] run return 0")

		raw("playsound minecraft:entity.enderman.teleport block ${soundListenersSel(12)} ~ ~ ~ 0.7 1.2")
		raw("particle minecraft:portal ~ ~0.6 ~ 0.3 0.5 0.3 0.2 30 normal ${particleViewersSel()}")

		executeRun("as @a[distance=..2,scores={${EnderIds.GRACE}=..0},limit=1,sort=nearest]", sendPlayer)

		raw("tag @e[type=marker,tag=${EnderIds.TP_DEST}] remove ${EnderIds.TP_DEST}")
		scoreboard.players.set(self(), EnderIds.TP_CD, 20)
	}

	private fun Function.goggles() {
		gogglesHeader(block, nameY = 1.4)
		stackReset(top = 120, step = 16)
		stackedPropLine("anchor_id", "Id: ", "light_purple")

		// The current signal, and where it points right now -- the two things you actually want to
		// know while wiring one up.
		function(RaLib.Redstone.detect)
		raw("data modify storage ra:temp ender_line set value {label:\"Signal: \",value:\"0\",color:\"gray\",suffix:\"\"}")
		raw("execute store result storage ra:temp ender_line.value int 1 run scoreboard players get @s ${Obj.POWER}")
		raw("execute if score @s ${Obj.POWER} matches 1.. run data modify storage ra:temp ender_line.color set value \"yellow\"")
		stackedTextLineFromStorage()

		storeCompute("store result score #ender.slot $T", ANCHOR_SLOT, source = "entity @s")
		raw("data remove storage ra:ender anchor.want")
		raw("execute if score #ender.slot $T matches 0..14 store result storage ra:ender anchor.i int 1 run scoreboard players get #ender.slot $T")
		executeRun("if score #ender.slot $T matches 0..14", readTarget, "with storage ra:ender anchor")

		raw("data modify storage ra:temp ender_line set value {label:\"Target: \",value:\"none\",color:\"red\",suffix:\"\"}")
		raw("execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:\"\"} run data modify storage ra:temp ender_line.value set from storage ra:ender anchor.want")
		raw("execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:\"\"} run data modify storage ra:temp ender_line.color set value \"green\"")
		stackedTextLineFromStorage()
	}
}
