package ra.settings.purge

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.api.Settings
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * The admin entity purge: remove Redstone Additions' ENTITIES from a world while the pack stays
 * installed, its settings and objectives untouched. The panel that asks for it is generated with
 * the other server pages (ra.settings.gen.AdminPurge); this file is what it runs.
 *
 * WHAT A PURGE CAN SEE. `@e` only finds entities in loaded chunks, and nothing in a data pack can
 * load a chunk's entities on purpose without side effects. So a purge removes what is loaded, and
 * "Everything" starts a clean-up that finishes the job as the rest of the world loads:
 *
 * - Every purge of everything adds one to the world's purge epoch (`#epoch ra.purge`, mirrored in
 *   `storage ra:purge cur.e` for the macros) and switches the clean-up on (`#sweep ra.purge`).
 * - Every persistent marker summoned after a purge is stamped with the tag `ra.pe.<epoch>`
 *   ([Settings.Purge.stampLine], at the three places that summon one: block placement, the
 *   infinite cauldrons, multiblock assembly).
 * - While the clean-up is on, [Settings.Purge.tick] runs ONE selector per tick:
 *   `@e[type=marker,tag=ra,tag=!ra.pe.<epoch>]`. A marker from before the latest purge lacks that
 *   tag whether it was never stamped or stamped by an older purge, which is why it is a tag and
 *   not a score (a `scores=` filter never matches an entity without one). A chunk that loads is
 *   cleaned the first tick it is loaded, before the migration pass or any block tick sees it.
 * - While it is off, the tick costs one score test.
 *
 * Markers are what the clean-up looks for because every block's other entities (skins, pipe and
 * wire displays, the Clock's and UNI Gate's items) sit at the marker's own position, in the same
 * chunk: they are removed with it ([sweepOne]).
 */
internal object Purge {
	/** Fake players `#epoch`, `#sweep`, `#swept`; on a player, the purge option they armed. */
	const val OBJ = "ra.purge"

	/** On a player: the game time their armed confirmation expires. */
	const val OBJ_T = "ra.purge.t"

	/** On an entity a count has picked out (orphans, input books), for the run that follows it. */
	const val HIT = "ra.purge.hit"

	/** On the admin running a purge, for the length of the broadcast. */
	const val ME = "ra.purge.me"

	/** The clean-up's epoch tag prefix: `ra.pe.<epoch>`. */
	const val EPOCH_TAG = "ra.pe."

	/** How long a confirmation stays valid, in ticks (30 s). */
	const val CONFIRM_TICKS = 600

	private const val T = Obj.SET_TMP

	private val sweep = ref(Ns.SETTINGS, "purge/sweep")
	private val sweepOne = ref(Ns.SETTINGS, "purge/sweep_one")

	/** One counted kind of entity: what the report calls it, and the `ra.set.tmp` holder. */
	class Category(val label: String, val holder: String)

	/**
	 * A purge the panel offers. [count] leaves one score per category (and anything it picked out
	 * tagged [HIT]); [run] removes what [count] found. [code] is what a player's [OBJ] holds while
	 * this option is armed, so a [Confirm] for one option can never run another.
	 */
	class Option(
		val id: String,
		val code: Int,
		val title: String,
		val summary: String,
		val warning: List<String>,
		val categories: List<Category>,
		val forceloads: Boolean = false,
	) {
		val count = ref(Ns.SETTINGS, "purge/$id/count")
		val run = ref(Ns.SETTINGS, "purge/$id/run")
	}

	val all = Option(
		"all", 1, "Everything",
		"every RA marker, display, readout and dropped RA item, plus networks and pending edits",
		listOf(
			"Every placed Redstone Additions block becomes an ordinary block: its stored properties and contents are gone.",
			"Transport networks, multiblocks, ender links and the Teleport Anchor id list are cleared; Chunk Loaders release their chunks.",
			"Open Data Handler edits are cancelled. Server settings, disabled blocks and player preferences are KEPT.",
			"Chunks that are not loaded now are cleaned as they load, until you stop that on the Purge page.",
		),
		listOf(
			Category("block markers", "#p.1"),
			Category("displays", "#p.2"),
			Category("dropped items", "#p.3"),
			Category("pending placements", "#p.4"),
		),
		forceloads = true,
	)

	val orphans = Option(
		"orphans", 2, "Orphaned displays",
		"skins, pipe and wire displays with no block marker under them",
		listOf(
			"Removes block skins, pipe and wire displays and floating block items whose block marker is gone.",
			"Working blocks are not touched. Goggles readouts are not counted (they have their own option).",
		),
		listOf(Category("orphaned displays", "#p.1")),
	)

	val billboards = Option(
		"billboards", 3, "Goggles readouts",
		"the floating text and icons drawn for goggles",
		listOf("Removes every goggles readout. Anyone still wearing goggles gets theirs redrawn."),
		listOf(Category("readouts", "#p.1")),
	)

	val items = Option(
		"items", 4, "Dropped RA items",
		"RA-tagged items on the ground and abandoned input books",
		listOf(
			"Removes item entities Redstone Additions tagged, and Data Handler / settings input books lying on the ground.",
			"This includes a Clock, UNI Gate or Message Block dropped by breaking one that nobody has picked up yet.",
		),
		listOf(Category("items", "#p.1")),
	)

	val options = listOf(all, orphans, billboards, items)

	/** Display entity types that belong to a block (never a player's). */
	private val DISPLAY_TYPES = listOf("block_display", "item_display", "text_display", "interaction")

	/** Item entities that are an input book (ra_lib:input's writable-book backend), tagged [HIT]. */
	private const val TAG_INPUT_BOOKS =
		"execute as @e[type=minecraft:item,tag=!ra] if items entity @s contents *[minecraft:custom_data~{ra:{input_book:1b}}] run tag @s add $HIT"

	private const val LOADERS = "@e[type=minecraft:marker,tag=ra.custom_block.chunk_loader,tag=ra.was_powered]"

	fun Ra.registerPurge() {
		pack.define(Settings.Purge.init) {
			raw("scoreboard objectives add $OBJ dummy")
			raw("scoreboard objectives add $OBJ_T dummy")
			// `add 0` creates them at 0 the first time and leaves them alone after: the epoch must
			// never go back, or markers stamped by a later purge would look current again.
			raw("scoreboard players add #epoch $OBJ 0")
			raw("scoreboard players add #sweep $OBJ 0")
			raw("scoreboard players add #swept $OBJ 0")
			// The macros read the epoch from here; written from the score on every load so the two
			// cannot disagree, and so `e` is always there for them.
			raw("execute store result storage ra:purge cur.e int 1 run scoreboard players get #epoch $OBJ")
		}

		pack.define(Settings.Purge.tick) {
			raw("execute if score #sweep $OBJ matches 1 run function ${sweep.asId()} with storage ra:purge cur")
		}

		// {e}: every RA marker from before the latest purge, in the chunks loaded this tick.
		pack.define(sweep) {
			raw("\$execute as @e[type=minecraft:marker,tag=ra,tag=!$EPOCH_TAG\$(e)] at @s run function ${sweepOne.asId()}")
		}

		// As one leftover marker, at it: what the purge would have done to its block.
		pack.define(sweepOne) {
			raw("execute if entity @s[tag=ra.custom_block.chunk_loader,tag=ra.was_powered] run forceload remove ~ ~")
			// Its displays sit on its position (skins and pipe displays at the block centre, the
			// Clock's item 0.115 below it), the same radius the blocks' own on_break uses.
			for (type in DISPLAY_TYPES) {
				val notBillboard = if (type == "item_display" || type == "text_display") ",tag=!ra.billboard" else ""
				raw("kill @e[type=minecraft:$type,tag=ra$notBillboard,distance=..0.6]")
			}
			raw("scoreboard players add #swept $OBJ 1")
			raw("kill @s")
		}

		// {e}, as a marker.
		pack.define(Settings.Purge.stamp) {
			raw("\$tag @s add $EPOCH_TAG\$(e)")
		}

		pack.defineAll()
		pack.defineOrphans()
		pack.defineBillboards()
		pack.defineItems()
	}

	/** `execute store result score <holder> ra.set.tmp if entity <selector>`, summed over [selectors]. */
	private fun Function.countInto(holder: String, selectors: List<String>) {
		raw("scoreboard players set $holder $T 0")
		for (sel in selectors) {
			raw("execute store result score #p.x $T if entity $sel")
			raw("scoreboard players operation $holder $T += #p.x $T")
		}
	}

	/** `#p.n` = the sum of [option]'s categories. */
	private fun Function.total(option: Option) {
		raw("scoreboard players set #p.n $T 0")
		for (c in option.categories) raw("scoreboard players operation #p.n $T += ${c.holder} $T")
	}

	private val displaySelectors = DISPLAY_TYPES.map { "@e[type=minecraft:$it,tag=ra]" }

	private fun DataPack.defineAll() {
		define(all.count) {
			countInto("#p.1", listOf("@e[type=minecraft:marker,tag=ra]"))
			countInto("#p.2", displaySelectors)
			raw(TAG_INPUT_BOOKS)
			countInto("#p.3", listOf("@e[type=minecraft:item,tag=ra]", "@e[type=minecraft:item,tag=$HIT]"))
			// A bat from an RA spawn egg lives for the tick between use and placement.
			countInto("#p.4", listOf("@e[type=minecraft:bat,tag=ra]"))
			countInto("#p.f", listOf(LOADERS))
			total(all)
		}

		define(all.run) {
			// FORCELOADS FIRST, as the uninstall does: killing a powered Chunk Loader's marker runs
			// none of its code, and its chunk would stay loaded for ever with nothing left that
			// knows why. `ra.was_powered` is exactly the set that forceloaded.
			raw("execute as $LOADERS at @s run forceload remove ~ ~")

			// A Data Handler edit waiting for typed input names a block that is about to go. Close
			// the input session (it removes its book too) before the pending record goes.
			raw("execute as @a[scores={ra.dh.pending=1..},tag=ra.input.active] at @s run function ${RaLib.Input.cancel.asId()}")

			raw("kill @e[type=minecraft:marker,tag=ra]")
			for (sel in displaySelectors) raw("kill $sel")
			raw("kill @e[type=minecraft:item,tag=ra]")
			raw("kill @e[type=minecraft:item,tag=$HIT]")
			raw("kill @e[type=minecraft:bat,tag=ra]")

			// WORLD STATE THAT NAMED THOSE ENTITIES. Scores on the entities went with them; what is
			// left is storage and the per-player selections. Configuration (settings, registries
			// rebuilt on load, wrench and Data Handler tables) and players' own things (clipboards,
			// preferences) are not touched.
			//
			// Transport: every network record and every queued rebuild. `#net_seq` is kept on
			// purpose -- it is what stops a retired network id being handed out again.
			raw("data modify storage ra:transport nets set value {}")
			for (key in listOf("dirtyq", "dirtyset", "pending", "full")) raw("data remove storage ra:transport $key")
			raw("data modify storage ra:transport dirty set value 0b")
			// Ender: the Teleport Anchor id list the Data Handler offers. Every anchor re-files its
			// id when it next ticks, so a list of ids nothing carries any more must not survive.
			raw("data modify storage ra:ender anchor_ids set value []")
			// Data Handler: pending edits (keyed by input request), the hand-over slots, and every
			// player's selected block number (offline players too, hence `*`). `#next ra.dh.mid`
			// stays, so a new block never gets an old number.
			for (key in listOf("pend", "pending_name", "pending_kind", "pending_j", "cur")) raw("data remove storage ra:dh $key")
			raw("scoreboard players reset * ra.dh.sel")
			raw("scoreboard players reset * ra.dh.pending")

			// Start (or restart) the clean-up of unloaded chunks under a new epoch.
			raw("scoreboard players add #epoch $OBJ 1")
			raw("execute store result storage ra:purge cur.e int 1 run scoreboard players get #epoch $OBJ")
			raw("scoreboard players set #sweep $OBJ 1")
			raw("scoreboard players set #swept $OBJ 0")
		}
	}

	private fun DataPack.defineOrphans() {
		define(orphans.count) {
			// A block's displays are summoned on its marker's position (skins and pipe/wire displays
			// at the block centre, the Clock's item 0.115 below it), so "no RA marker within 0.6" is
			// "its block is gone". Both are in the same chunk, so an unloaded marker cannot make a
			// loaded display look orphaned.
			for (type in DISPLAY_TYPES) {
				val notBillboard = if (type == "item_display" || type == "text_display") ",tag=!ra.billboard" else ""
				raw("execute as @e[type=minecraft:$type,tag=ra$notBillboard] at @s unless entity @e[type=minecraft:marker,tag=ra,distance=..0.6] run tag @s add $HIT")
			}
			countInto("#p.1", listOf("@e[tag=$HIT]"))
			total(orphans)
		}
		define(orphans.run) { raw("kill @e[tag=$HIT]") }
	}

	private val billboardSelectors = listOf("@e[type=minecraft:text_display,tag=ra.billboard]", "@e[type=minecraft:item_display,tag=ra.billboard]")

	private fun DataPack.defineBillboards() {
		define(billboards.count) {
			countInto("#p.1", billboardSelectors)
			total(billboards)
		}
		define(billboards.run) { for (sel in billboardSelectors) raw("kill $sel") }
	}

	private fun DataPack.defineItems() {
		define(items.count) {
			raw(TAG_INPUT_BOOKS)
			countInto("#p.1", listOf("@e[type=minecraft:item,tag=ra]", "@e[type=minecraft:item,tag=$HIT]"))
			total(items)
		}
		define(items.run) {
			raw("kill @e[type=minecraft:item,tag=ra]")
			raw("kill @e[type=minecraft:item,tag=$HIT]")
		}
	}

	private val markerSummon = Regex("""summon (minecraft:)?marker .*Tags:\[([^]]*)]""")
	private val functionCall = Regex("""function ([a-z0-9_]+:[a-z0-9_/.-]+)""")

	/**
	 * Functions that summon a persistent RA marker without stamping it with the purge epoch,
	 * directly or through a function they call: once a world has been purged, the clean-up would
	 * kill such a marker the tick after it appeared. A marker the same function kills again by one
	 * of its tags (the placement facing probe) is transient and exempt.
	 */
	fun unstampedMarkers(pack: DataPack): List<String> {
		val byId = (pack.functions + pack.generatedFunctions).associateBy { it.asId() }
		val stamp = "function ${Settings.Purge.stamp.asId()} "
		fun stamps(id: String, seen: MutableSet<String>): Boolean {
			if (!seen.add(id)) return false
			val lines = byId[id]?.lines.orEmpty()
			if (lines.any { stamp in it }) return true
			return lines.flatMap { line -> functionCall.findAll(line).map { it.groupValues[1] } }.any { stamps(it, seen) }
		}
		fun persistent(f: Function, line: String): Boolean {
			val tags = markerSummon.find(line)?.groupValues?.get(2)?.split(',')?.map { it.trim(' ', '"') } ?: return false
			if ("ra" !in tags) return false
			return tags.filter { it != "ra" }.none { t ->
				val killed = Regex("""tag=${Regex.escape(t)}[,\]]""")
				f.lines.any { it.startsWith("kill ") && killed.containsMatchIn(it) }
			}
		}
		return byId.values
			.filter { f -> f.lines.any { persistent(f, it) } && !stamps(f.asId(), mutableSetOf()) }
			.map { it.asId() }
	}
}
