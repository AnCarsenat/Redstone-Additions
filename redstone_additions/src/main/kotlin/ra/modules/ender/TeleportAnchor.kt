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
import ra.core.block.newMarkerSel
import ra.core.Tag
import ra.modules.ender.anchors.AnchorTable

/**
 * Teleport Anchor: sends whoever stands on it to the anchor the redstone strength names.
 *
 * `anchor_id` is what other anchors aim at. `targets` is the table of fifteen ids, one per
 * redstone strength: signal 1 uses targets[0], signal 15 uses targets[14]. An empty string means
 * "nothing wired to that strength".
 *
 * Ids are unique and in format ID. Every anchor files itself in the destinations table,
 * `storage ra:ender anchors` (see [AnchorTable]), when it is placed and when its id changes, and
 * leaves it when broken; a teleport is one key lookup there, not a search through the loaded
 * anchors, so the destination may be in an unloaded chunk or another dimension.
 *
 * (6.0.0 put the wanted id inside an `nbt=` selector through a macro, so a target such as
 * `A"}}}] as @a if function ...` ran any function it named. The id now reaches a command only as a
 * path key, after `ra_lib:safe/check` in format ID: [AnchorTable.lookup].)
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
	private val registerCheck = block.fn("register_check")
	/**
	 * As an anchor: make `targets` a table of exactly fifteen entries. Run once per anchor by the
	 * 6.0.1 migration (ra_migrations:6.0.0-to-6.0.1/teleport_anchor). Safe to run twice.
	 */
	val repairTargets = block.fn("repair_targets")
	private val repairPad = block.fn("repair_pad")

	private const val T = Obj.TEMP

	/**
	 * The zero-based targets row the signal names, for `/compute entity @s`: min(power, 15) - 1.
	 * ra_lib reports 16 for a redstone block; the table only has fifteen rows, so that is 15.
	 */
	private val ANCHOR_SLOT = provider { sub(min(thisScoreOf(Obj.POWER), 15), constant(1)) }

	fun Ra.registerTeleportAnchor() {
		block(block)
		properties(block) {
			// A path key of the destinations table, so format ID, and one per world: the Data Handler
			// refuses an id another anchor has.
			text("anchor_id", TextFormat.ID, unique = "ra:ender" to AnchorTable.TABLE)
			// One per strength, each [Edit] and [Clear]; padded, so clearing one never moves another.
			textList("targets", max = 15, pad = true, format = TextFormat.ID, entryLabel = "Strength ")
		}
		pack.blockRegisterMessage(block)
		pack.enderGive(
			block, item,
			"""{text:" — set its id and target table with ",color:"gray"},{text:"/function ${ModulesApi.Ender.anchorHelp.asId()}",color:"aqua"}""",
		)
		pack.enderRecipe(block, item, RecipeCategory.MISC) {
			pattern("OEO", "ECE", "OEO")
			key("O", Items.OBSIDIAN)
			key("E", Items.ENDER_EYE)
			key("C", Items.CRYING_OBSIDIAN)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties.targets", EMPTY_TABLE)
			// Who placed it, for the messages about its id. Then its id: none yet, so it gets the
			// first free one of anchor, anchor_2, ... and files itself in the table.
			raw("data modify entity ${block.newMarkerSel()} data.data.owner set from entity @a[tag=${Tag.PLACER},limit=1] UUID")
			raw("execute as ${block.newMarkerSel()} run function ${AnchorTable.register.asId()}")
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

		// kept: beet contract. Tag this anchor if its id is the one being aimed at, first match
		// wins. As a candidate anchor marker. Nothing calls it any more (ids became strings,
		// keys of the destinations table since 6.0.3); kept because the beet pack shipped it.
		pack.define(block.fn("match_target")) {
			raw("execute if entity @e[type=marker,tag=${EnderIds.TP_DEST},limit=1] run return 0")

			raw("execute store result score #ender.candidate $T run data get entity @s data.properties.anchor_id")
			raw("execute unless score #ender.candidate $T = #ender.target $T run return 0")

			tag(self()) { add(EnderIds.TP_DEST) }
		}

		pack.define(block.goggles) { goggles() }

		// A broken anchor leaves the destinations table.
		val onBreak = pack.enderOnBreak(block, item) { function(AnchorTable.drop) }

		// As an anchor: has its id changed since it filed it (the Data Handler, a command, a
		// Clipboard paste, /data)? Then file it again. One comparison while it has not.
		pack.define(registerCheck) {
			raw("data remove storage ra:ender rc")
			raw("data modify storage ra:ender rc set from entity @s data.data.registered_id")
			raw("execute store success score #ender.chg $T run data modify storage ra:ender rc set from entity @s data.properties.anchor_id")
			raw("execute unless data entity @s data.properties.anchor_id run scoreboard players set #ender.chg $T 1")
			raw("data remove storage ra:ender rc")
			raw("execute if score #ender.chg $T matches 1 run function ${AnchorTable.register.asId()}")
		}
		registerRepair()
		pack.blockTick(block, onBreak, BreakTest.NotBlock(Blocks.CRYING_OBSIDIAN)) {
			// An anchor from an older version (no anchor_id, a malformed targets table) is brought up
			// to date by the migrations before it ever gets here.
			// Its id in the destinations table (cheap when unchanged: one comparison).
			executeRun("as ${block.markerSel()}", registerCheck)

			// A selector on scores skips entities that have no score at all, so every anchor needs
			// one before the cooldown gate can mean anything. Adding zero creates it.
			raw("scoreboard players add ${block.markerSel()} ${EnderIds.TP_CD} 0")
			raw("scoreboard players remove ${block.markerSel("scores={${EnderIds.TP_CD}=1..}")} ${EnderIds.TP_CD} 1")
			executeRun("as ${block.markerSel("scores={${EnderIds.TP_CD}=..0}")} at @s", process)
		}
	}

	/** The one-time repair of old target tables (6.0.1 migration). */
	private fun Ra.registerRepair() {
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

		// A row naming this anchor's own id sends nobody anywhere.
		raw("data modify storage ra:ender anchor.self set from entity @s data.properties.anchor_id")
		raw("execute store success score #ender.diff $T run data modify storage ra:ender anchor.self set from storage ra:ender anchor.want")
		raw("data remove storage ra:ender anchor.self")
		raw("execute if score #ender.diff $T matches 0 run return 0")

		// The destination is one key of the table (AnchorTable.send): no search, any chunk, any dimension.
		raw("data modify storage ra:ender lookup set value {}")
		raw("data modify storage ra:ender lookup.id set from storage ra:ender anchor.want")
		raw("scoreboard players set #ender.sent $T 0")
		raw("execute store result score #ender.sent $T as @a[distance=..2,scores={${EnderIds.GRACE}=..0},limit=1,sort=nearest] run function ${AnchorTable.send.asId()}")
		raw("data remove storage ra:ender lookup")
		raw("execute if score #ender.sent $T matches 0 run return 0")

		raw("playsound minecraft:entity.enderman.teleport block ${soundListenersSel(12)} ~ ~ ~ 0.7 1.2")
		raw("particle minecraft:portal ~ ~0.6 ~ 0.3 0.5 0.3 0.2 30 normal ${particleViewersSel()}")
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
		val wired = "if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:\"\"}"
		raw("execute $wired run data modify storage ra:temp ender_line.value set from storage ra:ender anchor.want")
		// Green when the table has that anchor, red with a note when it has none.
		raw("data modify storage ra:ender lookup set value {}")
		raw("data modify storage ra:ender lookup.id set from storage ra:ender anchor.want")
		raw("execute store result score #ender.found $T run function ${AnchorTable.lookup.asId()}")
		raw("data remove storage ra:ender lookup")
		raw("execute $wired if score #ender.found $T matches 1 run data modify storage ra:temp ender_line.color set value \"green\"")
		raw("execute $wired if score #ender.found $T matches 0 run data modify storage ra:temp ender_line.suffix set value \" (no such anchor)\"")
		stackedTextLineFromStorage()
	}
}
