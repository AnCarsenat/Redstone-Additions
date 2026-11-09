package ra.modules.sensors

import io.github.ayfri.kore.features.recipes.types.key
import io.github.ayfri.kore.features.recipes.types.pattern
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.Items
import ra.api.RaLib
import ra.core.Admin.readCappedProperty
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.block.CustomBlock
import ra.core.block.STONE_BREAK_SOUND
import ra.core.block.Unlock
import ra.core.block.blockGive
import ra.core.block.blockOnBreak
import ra.core.block.blockTick
import ra.core.block.dataLine
import ra.core.block.dropItem
import ra.core.block.finishPlacement
import ra.core.block.gogglesHeader
import ra.core.block.newMarkerSel
import ra.core.block.placeBlock
import ra.core.block.placementGuard
import ra.core.block.propLine
import ra.core.block.recipeResult
import ra.core.block.setOnNew
import ra.core.block.shapedRecipe
import ra.core.blockRegisterMessage
import ra.core.define
import ra.core.executeRun
import ra.core.item.loreItem
import ra.core.item.loreLine
import ra.core.raw
import ra.core.textList
import ra.api.Goggles
import ra.modules.sensors.SensorPoll.startIfIdle

/**
 * Player Detector: powers its shell while a player named in its `players` list is within
 * `radius`. Output as the Entity Detector's. Spectators are never counted.
 *
 * `players` is a list of up to [MAX] names (format ID), edited per entry in the Data Handler.
 *
 * CHEAP MATCHING. A selector cannot OR names, and a player's name is not in its NBT, so each name
 * is one `execute if entity @a[name="<name>",distance=..<r>]`: a selector over the players only,
 * stopping at the first hit. The names are not re-checked on every read: when `players` changes
 * (compared, not walked, with `data.data.players_src`, the list last seen), every entry is checked
 * once with `ra_lib:safe/check` (ID: no space, quote or backslash, so pasted inside the selector's
 * quotes it can only ever be the name) and the ones that pass are kept in `data.data.players`.
 * Each read then walks that list with a macro; an unchanged list costs one comparison more.
 * Entries that fail are counted in `data.status.skipped`; an empty entry is skipped silently.
 *
 * Read once a second by [SensorPoll].
 */
object PlayerDetector {
	val block = CustomBlock(Ns.SENSORS, "player_detector", "Player Detector", model = "minecraft:waxed_oxidized_chiseled_copper")
	val item = block.loreItem { lore(loreLine("Powers when a listed player is near", "gray")) }

	const val MAX = 16
	const val MAX_RADIUS = HeldItemDetector.MAX_RADIUS
	const val ADMIN_MAX_RADIUS = HeldItemDetector.ADMIN_MAX_RADIUS
	const val DEFAULT_RADIUS = HeldItemDetector.DEFAULT_RADIUS

	/** As the marker, at it: look for the listed players, switch the shell. */
	val update = block.fn("update")
	private val sync = block.fn("sync")
	private val syncStep = block.fn("sync_step")
	private val find = block.fn("find")
	private val findStep = block.fn("find_step")
	private val match = block.fn("match")

	private const val T = Obj.TEMP
	private const val ON = "#sn.player"
	private const val S = "storage ra:temp player_detector"

	fun Ra.registerPlayerDetector() {
		block(block)
		properties(block) {
			textList("players", max = MAX, format = TextFormat.ID, entryLabel = "Player ", default = emptyList())
			number("radius", min = 1, max = MAX_RADIUS, adminMax = ADMIN_MAX_RADIUS, default = DEFAULT_RADIUS)
		}
		pack.blockRegisterMessage(block)
		pack.blockGive(block)
		pack.shapedRecipe(block.ns, block.id, item.recipeResult(), Unlock(Items.WRITABLE_BOOK), count = null) {
			pattern("CBC", "ROR", "CCC")
			key("C", Items.COPPER_INGOT)
			key("B", Items.WRITABLE_BOOK)
			key("R", Items.REDSTONE)
			key("O", Items.OBSERVER)
		}

		// As bat, at bat position.
		pack.define(block.handlePlacement) {
			placementGuard(block, checkExisting = false)
			placeBlock(block)
			setOnNew(block, "data.properties", """{players:[],radius:$DEFAULT_RADIUS}""")
			executeRun("as ${block.newMarkerSel()} at @s", update)
			finishPlacement(block, removePlaceTag = false)
		}

		pack.define(update) {
			raw("data remove $S")
			raw("function ${sync.asId()}")
			raw("scoreboard players set $ON $T 0")
			raw("execute if data entity @s data.data.players[0] store result score $ON $T run function ${find.asId()}")
			raw("data remove $S")
			raw("execute if score $ON $T matches 1 run data modify entity @s data.status.output set value \"on\"")
			raw("execute unless score $ON $T matches 1 run data modify entity @s data.status.output set value \"off\"")
			// One fill or the other, never off-then-on: that would pulse the wires every read.
			raw("execute if score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block")
			raw("execute unless score $ON $T matches 1 run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block")
		}

		pack.define(sync) { sync() }

		// One entry off the front of `in`: "" skipped, a safe name kept, anything else counted.
		pack.define(syncStep) {
			raw("data remove $S.c")
			raw("data modify $S.c set from $S.in[0]")
			raw("data remove $S.in[0]")
			raw("""execute if data $S{c:""} run return run execute if data $S.in[0] run function ${syncStep.asId()}""")
			raw("data remove storage ra:safe s")
			raw("data modify storage ra:safe s set from $S.c")
			raw("execute store result score #pd.ok $T run function ${RaLib.Safe.check.asId()}")
			raw("execute if score #pd.ok $T matches 1 run data modify $S.ok append from $S.c")
			raw("execute if score #pd.ok $T matches 0 run scoreboard players add #pd.bad $T 1")
			raw("execute if data $S.in[0] run function ${syncStep.asId()}")
		}

		pack.define(find) {
			raw("data modify $S.w set from entity @s data.data.players")
			readCappedProperty("radius", default = DEFAULT_RADIUS, min = 1, adminMax = ADMIN_MAX_RADIUS)
			raw("execute store result $S.r int 1 run scoreboard players get #prop $T")
			raw("return run function ${findStep.asId()}")
		}
		// Returns 1 at the first listed player in range.
		pack.define(findStep) {
			raw("data modify $S.n set from $S.w[0]")
			raw("data remove $S.w[0]")
			raw("execute store result score #pd.hit $T run function ${match.asId()} with $S")
			raw("execute if score #pd.hit $T matches 1 run return 1")
			raw("execute if data $S.w[0] run return run function ${findStep.asId()}")
			raw("return 0")
		}
		// Macro {n, r}: n only ever an entry of data.data.players, which check passed as ID.
		pack.define(match) {
			raw("\$execute if entity @a[name=\"\$(n)\",distance=..\$(r),gamemode=!spectator] run return 1")
			raw("return 0")
		}

		pack.define(block.goggles) {
			gogglesHeader(block, nameY = 1.0)
			dataLine("listed", "Players: ", "aqua", y = 0.8)
			propLine("radius", "Radius: ", "aqua", y = 0.6)
			dataLine("output", "Output: ", "yellow", y = 0.4)
			raw("execute if data entity @s data.status.skipped run function ${Goggles.Billboard.dataLine.asId()} {path:\"skipped\",label:\"Refused names: \",color:\"red\",suffix:\"\",y:0.2}")
		}

		val onBreak = pack.blockOnBreak(block, drop = { dropItem(item) }, sound = STONE_BREAK_SOUND, cloud = true)
		pack.blockTick(block, onBreak) { startIfIdle(block) }
	}

	/**
	 * As the marker: when `players` differs from `data.data.players_src`, rebuild
	 * `data.data.players` (the entries that pass), `data.status.listed` and `data.status.skipped`.
	 */
	private fun Function.sync() {
		// Removed with a command: nothing to look for.
		raw("execute unless data entity @s data.properties.players run data remove entity @s data.data.players_src")
		raw("execute unless data entity @s data.properties.players run data remove entity @s data.data.players")
		raw("execute unless data entity @s data.properties.players run data remove entity @s data.status.skipped")
		raw("execute unless data entity @s data.properties.players run return run data modify entity @s data.status.listed set value 0")
		// Unchanged: success 0 when the copy over the last seen list changes nothing.
		raw("data modify $S.was set from entity @s data.data.players_src")
		raw("execute store success score #pd.chg $T run data modify $S.was set from entity @s data.properties.players")
		raw("execute if score #pd.chg $T matches 0 run return 0")

		raw("data modify entity @s data.data.players_src set from entity @s data.properties.players")
		raw("data modify $S.ok set value []")
		raw("scoreboard players set #pd.bad $T 0")
		raw("data modify $S.in set from entity @s data.properties.players")
		// A list, of any kind: `insert` fails on anything else. The probe is taken out again.
		raw("execute store success score #pd.list $T run data modify $S.in insert 0 value 0b")
		raw("execute if score #pd.list $T matches 1 run data remove $S.in[0]")
		raw("execute if score #pd.list $T matches 0 run data remove $S.in")
		raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
		raw("execute if data $S.in[0] run function ${syncStep.asId()}")
		raw("data remove storage ra:safe s")
		raw("data modify entity @s data.data.players set from $S.ok")
		raw("execute store result entity @s data.status.listed int 1 run data get $S.ok")
		raw("execute if score #pd.bad $T matches 1.. store result entity @s data.status.skipped int 1 run scoreboard players get #pd.bad $T")
		raw("execute if score #pd.bad $T matches 0 run data remove entity @s data.status.skipped")
	}
}
