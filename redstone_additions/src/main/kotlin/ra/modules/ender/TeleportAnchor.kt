package ra.modules.ender

import ra.core.TextFormat
import ra.core.text
import ra.core.textList
import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.min
import io.github.ayfri.kore.features.predicates.providers.sub
import ra.core.COMPUTE_SELF
import ra.core.computeInto
import ra.core.computeScore
import ra.core.provider
import ra.core.storeInt
import ra.core.thisScoreOf
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
 *
 * Every anchor files its id in `storage ra:ender anchor_ids` (see [registerCheck]), the list the
 * Data Handler's [Choose] offers for each strength: a player sets a destination by clicking a
 * name, not by typing SNBT. The list survives the anchor's chunk unloading, so a far anchor kept
 * loaded by a Chunk Loader can still be picked.
 *
 * Ids are compared, never pasted into a command: [findDest] tests each anchor's id with a
 * `data modify ... set from` that changes nothing on a match. (6.0.0 put the wanted id inside an
 * `nbt=` selector through a macro, so a target such as `A"}}}] as @a if function ...` ran any
 * function it named.)
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
	private val matchId = block.fn("match_id")
	private val registerCheck = block.fn("register_check")
	private val registerId = block.fn("register_id")
	private val registerWalk = block.fn("register_walk")
	private val dropId = block.fn("drop_id")
	private val dropCount = block.fn("drop_count")
	private val dropWalk = block.fn("drop_walk")
	/**
	 * As an anchor: make `targets` a table of exactly fifteen entries. Run once per anchor by the
	 * 6.0.1 migration (ra_migrations:6.0.0-to-6.0.1/teleport_anchor). Safe to run twice.
	 */
	val repairTargets = block.fn("repair_targets")
	private val repairPad = block.fn("repair_pad")

	/** The known anchor ids, in the order they were first seen: `storage ra:ender anchor_ids`. */
	const val REGISTRY = "anchor_ids"

	/** At most this many ids are remembered. */
	private const val REGISTRY_MAX = 200

	private val sendPlayer = block.fn("send_player")

	private const val T = Obj.TEMP

	/**
	 * The zero-based targets row the signal names, for `/compute entity @s`: min(power, 15) - 1.
	 * ra_lib reports 16 for a redstone block; the table only has fifteen rows, so that is 15.
	 */
	private val ANCHOR_SLOT = provider { sub(min(thisScoreOf(Obj.POWER), 15), constant(1)) }

	fun Ra.registerTeleportAnchor() {
		block(block)
		properties(block) {
			// Compared, never pasted into a command, so spaces are fine ("Main base").
			text("anchor_id", TextFormat.NAME)
			// One per strength. The Data Handler shows a row per strength with [Choose] (a known
			// anchor id), [Edit] and [Clear]; padded, so clearing one never moves another.
			textList(
				"targets", max = 15, pad = true, format = TextFormat.NAME,
				choices = "ra:ender" to REGISTRY, entryLabel = "Strength ",
			)
		}
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

		// Tag the nearest OTHER anchor whose anchor_id is storage ra:ender anchor.want. As the
		// sending anchor marker, wearing ra.ender.self, at it.
		//
		// Every anchor's id is compared, then the nearest match is taken -- the order the beet line
		// got wrong (it took the nearest anchor first and compared after, so a third anchor closer
		// than the target hid it). No macro: the id never becomes part of a command.
		pack.define(findDest) {
			raw("execute as ${block.markerSel("tag=!${EnderIds.SELF}")} run function ${matchId.asId()}")
			raw("execute as @e[type=marker,tag=$CANDIDATE,sort=nearest,limit=1] run tag @s add ${EnderIds.TP_DEST}")
			raw("tag @e[type=marker,tag=$CANDIDATE] remove $CANDIDATE")
		}
		// As a candidate anchor: tag it when its id is the wanted one. `set from` over a copy of the
		// id succeeds only when it changes something, so 0 means equal.
		pack.define(matchId) {
			raw("data remove storage ra:ender cmp")
			raw("data modify storage ra:ender cmp set from entity @s data.properties.anchor_id")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender cmp set from storage ra:ender anchor.want")
			raw("execute if score #ender.diff $T matches 0 run tag @s add $CANDIDATE")
			raw("data remove storage ra:ender cmp")
		}

		registerRegistry()

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
			raw("execute at @s run particle minecraft:portal ~ ~1 ~ 0.4 0.8 0.4 0.3 40 normal ${particleViewersSel()}")
		}

		pack.define(block.goggles) { goggles() }

		// A broken anchor takes its id off the known list, unless another loaded anchor shares it.
		val onBreak = pack.enderOnBreak(block, item) {
			raw("data modify storage ra:ender reg set value {}")
			raw("data modify storage ra:ender reg.drop set from entity @s data.properties.anchor_id")
			function(dropId)
			raw("data remove storage ra:ender reg")
		}
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.CRYING_OBSIDIAN)) {
			// An anchor from an older version (no anchor_id, a malformed targets table) is brought up
			// to date by the 6.0.1 migration before it ever gets here.
			// Its id on the known list (cheap when unchanged: one comparison).
			executeRun("as ${block.markerSel()}", registerCheck)

			// A selector on scores skips entities that have no score at all, so every anchor needs
			// one before the cooldown gate can mean anything. Adding zero creates it.
			raw("scoreboard players add ${block.markerSel()} ${EnderIds.TP_CD} 0")
			raw("scoreboard players remove ${block.markerSel("scores={${EnderIds.TP_CD}=1..}")} ${EnderIds.TP_CD} 1")
			executeRun("as ${block.markerSel("scores={${EnderIds.TP_CD}=..0}")} at @s", process)
		}
	}

	/** The tag a matching anchor wears while the nearest is picked. */
	private const val CANDIDATE = "ra.ender.tp_cand"

	/** The known-ids list and the one-time repair of old target tables. */
	private fun Ra.registerRegistry() {
		// As an anchor: has its id changed since it was filed? Then file the new one and drop the
		// old one (if no other loaded anchor still has it).
		pack.define(registerCheck) {
			raw("data modify storage ra:ender reg set value {}")
			raw("data modify storage ra:ender reg.id set from entity @s data.properties.anchor_id")
			raw("data modify storage ra:ender reg.was set from entity @s data.data.registered_id")
			raw("data modify storage ra:ender reg.cmp set from storage ra:ender reg.was")
			raw("execute store success score #ender.chg $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.id")
			raw("execute if score #ender.chg $T matches 0 run return run data remove storage ra:ender reg")
			raw("data modify entity @s data.data.registered_id set from storage ra:ender reg.id")
			raw("execute unless data storage ra:ender reg.id run data remove entity @s data.data.registered_id")
			raw("data modify storage ra:ender reg.drop set from storage ra:ender reg.was")
			raw("execute if data storage ra:ender reg.drop run function ${dropId.asId()}")
			raw("""execute if data storage ra:ender reg.id unless data storage ra:ender reg{id:""} run function ${registerId.asId()}""")
			raw("data remove storage ra:ender reg")
		}
		// reg.id onto the list, unless it is there already.
		pack.define(registerId) {
			raw("execute unless data storage ra:ender $REGISTRY run data modify storage ra:ender $REGISTRY set value []")
			raw("execute store result score #ender.n $T run data get storage ra:ender $REGISTRY")
			raw("execute if score #ender.n $T matches $REGISTRY_MAX.. run return 0")
			raw("data modify storage ra:ender reg.walk set from storage ra:ender $REGISTRY")
			raw("scoreboard players set #ender.found $T 0")
			function(registerWalk)
			raw("execute if score #ender.found $T matches 0 run data modify storage ra:ender $REGISTRY append from storage ra:ender reg.id")
			raw("data remove storage ra:ender reg.walk")
		}
		pack.define(registerWalk) {
			raw("execute unless data storage ra:ender reg.walk[0] run return 0")
			raw("data modify storage ra:ender reg.cmp set from storage ra:ender reg.walk[0]")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.id")
			raw("execute if score #ender.diff $T matches 0 run return run scoreboard players set #ender.found $T 1")
			raw("data remove storage ra:ender reg.walk[0]")
			function(registerWalk)
		}
		// As an anchor: take reg.drop off the list when no OTHER loaded anchor has that id.
		pack.define(dropId) {
			raw("tag @s add ${EnderIds.SELF}")
			raw("scoreboard players set #ender.others $T 0")
			raw("execute as ${block.markerSel("tag=!${EnderIds.SELF}")} run function ${dropCount.asId()}")
			raw("tag @s remove ${EnderIds.SELF}")
			raw("execute if score #ender.others $T matches 1.. run return 0")
			raw("data modify storage ra:ender reg.walk set from storage ra:ender $REGISTRY")
			raw("data modify storage ra:ender $REGISTRY set value []")
			function(dropWalk)
			raw("data remove storage ra:ender reg.walk")
		}
		pack.define(dropCount) {
			raw("data modify storage ra:ender reg.cmp set from entity @s data.properties.anchor_id")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.drop")
			raw("execute if score #ender.diff $T matches 0 run scoreboard players add #ender.others $T 1")
		}
		pack.define(dropWalk) {
			raw("execute unless data storage ra:ender reg.walk[0] run return 0")
			raw("data modify storage ra:ender reg.cmp set from storage ra:ender reg.walk[0]")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.drop")
			raw("execute unless score #ender.diff $T matches 0 run data modify storage ra:ender $REGISTRY append from storage ra:ender reg.walk[0]")
			raw("data remove storage ra:ender reg.walk[0]")
			function(dropWalk)
		}

		// As an anchor, once: a target table of exactly fifteen entries. 6.0.0's Data Handler pasted
		// whatever parsed, so a table may be a compound, or a list of another length.
		pack.define(repairTargets) {
			raw("data remove storage ra:ender fix")
			raw("data modify storage ra:ender fix set from entity @s data.properties.targets")
			raw("execute store success score #ender.lst $T run data modify storage ra:ender fix insert 0 value 0b")
			raw("execute if score #ender.lst $T matches 0 run data modify entity @s data.properties.targets set value $EMPTY_TABLE")
			raw("execute if score #ender.lst $T matches 0 run return run data remove storage ra:ender fix")
			raw("data remove storage ra:ender fix[0]")
			function(repairPad)
			raw("data modify entity @s data.properties.targets set from storage ra:ender fix")
			raw("data remove storage ra:ender fix")
		}
		pack.define(repairPad) {
			raw("execute store result score #ender.len $T run data get storage ra:ender fix")
			raw("execute if score #ender.len $T matches 16.. run data remove storage ra:ender fix[-1]")
			raw("execute if score #ender.len $T matches ..14 run data modify storage ra:ender fix append value \"\"")
			raw("execute unless score #ender.len $T matches 15 run function ${repairPad.asId()}")
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
		computeInto(storeInt(Storage.ENDER, "anchor.i"), ANCHOR_SLOT, source = COMPUTE_SELF)

		function(readTarget, Storage.ENDER, "anchor")
		raw("execute unless data storage ra:ender anchor.want run return 0")
		raw("execute if data storage ra:ender anchor{want:\"\"} run return 0")

		// Find the destination. The tag is the handle for the teleport, since carrying a position
		// through a macro would mean formatting doubles. `self` keeps an anchor from sending a
		// player onto itself when a row names its own id.
		raw("tag @e[type=marker,tag=${EnderIds.TP_DEST}] remove ${EnderIds.TP_DEST}")
		tag(self()) { add(EnderIds.SELF) }
		function(findDest)
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

		computeScore("#ender.slot", T, ANCHOR_SLOT, source = COMPUTE_SELF)
		raw("data remove storage ra:ender anchor.want")
		raw("execute if score #ender.slot $T matches 0..14 store result storage ra:ender anchor.i int 1 run scoreboard players get #ender.slot $T")
		executeRun("if score #ender.slot $T matches 0..14", readTarget, "with storage ra:ender anchor")

		raw("data modify storage ra:temp ender_line set value {label:\"Target: \",value:\"none\",color:\"red\",suffix:\"\"}")
		raw("execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:\"\"} run data modify storage ra:temp ender_line.value set from storage ra:ender anchor.want")
		raw("execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:\"\"} run data modify storage ra:temp ender_line.color set value \"green\"")
		stackedTextLineFromStorage()
	}
}
