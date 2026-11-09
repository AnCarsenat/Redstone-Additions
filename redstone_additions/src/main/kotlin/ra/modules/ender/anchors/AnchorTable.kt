package ra.modules.ender.anchors

import io.github.ayfri.kore.functions.Function
import ra.api.RaLib
import ra.core.Admin
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.TextFormat
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.core.block.particleViewersSel
import ra.lib.safe.SafeClean
import ra.lib.safe.SafeClean.cleanFrom
import ra.modules.ender.EnderIds

/**
 * THE DESTINATIONS TABLE: every Teleport Anchor in the world, by id, in `storage ra:ender anchors`.
 *
 * ```
 * anchors: {
 *   "home": {id:"home", x:12, y:64, z:-30, dim:"minecraft:overworld", owner:[I; ...]},
 *   "mine": {id:"mine", x:400, y:12, z:88, dim:"minecraft:overworld"},
 * }
 * ```
 *
 * x, y, z are the anchor's BLOCK (ints); `dim` its dimension, "" in a dimension other than the
 * three vanilla ones (a data pack cannot read an entity's dimension, only test for one it names).
 * `owner` is the UUID of the player who placed it, absent for an anchor placed before 6.0.3. Ids are
 * unique, in format ID (letters, digits and `_ . + - : / #`), so an id can be a path key.
 *
 * WHO WRITES IT. Only the anchor itself, as its marker: [register] when it is placed and whenever
 * its `anchor_id` differs from the id it filed (`data.data.registered_id`, compared each tick, one
 * `data modify`), and [drop] when it is broken. The table outlives the anchor's chunk, so a
 * teleport to a far anchor needs no entity search and no loaded chunk.
 *
 * WHO READS IT. [lookup] (one key, no walk) and [send]; the Teleport Anchor's pad, and the Anchor
 * Terminal and Tablet to come. Destinations are not listed: a compound has no key walk, and the
 * table is not a menu of everyone's anchors.
 *
 * ONE ID, ONE ANCHOR. [register] refuses to share an id: an anchor whose wanted id is taken by
 * another anchor's entry becomes `<id>_2` (`_3`, ...) and the owner, the admins and players within
 * 8 blocks are told. The Data Handler refuses the taken id before it gets that far
 * (`unique` on the property, ra.tools.datahandler.props.DataHandlerUnique).
 */
object AnchorTable {
	/** `storage ra:ender <TABLE>`. */
	const val TABLE = "anchors"

	/**
	 * API. storage ra:ender lookup.id (a string) -> storage ra:ender lookup.dest, a copy of the
	 * entry. Returns 1 when found, 0 when not (or the id is not in format ID). Any context.
	 */
	val lookup = ref(Ns.ENDER, "anchors/lookup")

	/**
	 * API. As a player: teleport `@s` onto the anchor storage ra:ender lookup.id names, in its
	 * dimension, with the arrival grace, sound and particles. Returns 1 when sent, 0 when there is no
	 * such anchor (or it is in a dimension the table cannot name). Uses [lookup].
	 */
	val send = ref(Ns.ENDER, "anchors/send")

	/** As an anchor marker: file its id (see the class comment). Idempotent. */
	val register = ref(Ns.ENDER, "anchors/register")

	/** As an anchor marker: take its entry out of the table, if the entry is still its own. */
	val drop = ref(Ns.ENDER, "anchors/drop")

	private val lookupAt = ref(Ns.ENDER, "anchors/lookup_at")
	private val tpTo = ref(Ns.ENDER, "anchors/tp_to")
	private val dropKey = ref(Ns.ENDER, "anchors/drop_key")
	private val findFree = ref(Ns.ENDER, "anchors/find_free")
	private val claim = ref(Ns.ENDER, "anchors/claim")
	private val suffix = ref(Ns.ENDER, "anchors/suffix")
	private val put = ref(Ns.ENDER, "anchors/put")
	private val tell = ref(Ns.ENDER, "anchors/tell")
	private val ownerMatch = ref(Ns.ENDER, "anchors/owner_match")

	/** Suffixes tried before giving up (`_2` .. `_999`). */
	private const val MAX_SUFFIX = 1000

	private const val T = Obj.TEMP
	private const val TELL = "ra.ender.tell"
	private val DIMS = listOf("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")

	/** storage ra:ender reg.me = {x, y, z, dim} of @s, the anchor marker. */
	private fun Function.here() {
		raw("data modify storage ra:ender reg.me set value {dim:\"\"}")
		for ((i, axis) in listOf("x", "y", "z").withIndex())
			raw("execute store result storage ra:ender reg.me.$axis int 1 run data get entity @s Pos[$i]")
		for (d in DIMS) raw("execute at @s if dimension $d run data modify storage ra:ender reg.me.dim set value \"$d\"")
	}

	/** #ender.idok = 1 when the value at [source] is a string in format ID (safe as a path key). */
	private fun Function.idCheck(source: String) {
		raw("data remove storage ra:safe s")
		raw("data modify storage ra:safe s set from $source")
		raw("data modify storage ra:safe f set value ${TextFormat.ID.code}")
		raw("execute store result score #ender.idok $T run function ${RaLib.Safe.check.asId()}")
	}

	fun Ra.registerAnchorTable() {
		pack.define(lookup) {
			raw("data remove storage ra:ender lookup.dest")
			raw("execute unless data storage ra:ender lookup.id run return 0")
			idCheck("storage ra:ender lookup.id")
			raw("execute if score #ender.idok $T matches 0 run return 0")
			raw("function ${lookupAt.asId()} with storage ra:ender lookup")
			raw("execute unless data storage ra:ender lookup.dest run return 0")
			raw("return 1")
		}
		// {id}, in format ID.
		pack.define(lookupAt) { raw("\$data modify storage ra:ender lookup.dest set from storage ra:ender $TABLE.\"\$(id)\"") }

		pack.define(send) {
			raw("execute store result score #ender.ok $T run function ${lookup.asId()}")
			raw("execute if score #ender.ok $T matches 0 run return 0")
			raw("""execute if data storage ra:ender lookup.dest{dim:""} run return 0""")
			// Standing on top of the block: y + 1. Integer x and z put the player at the centre.
			raw("execute store result score #ender.y $T run data get storage ra:ender lookup.dest.y")
			raw("scoreboard players add #ender.y $T 1")
			raw("execute store result storage ra:ender lookup.dest.y1 int 1 run scoreboard players get #ender.y $T")
			raw("function ${tpTo.asId()} with storage ra:ender lookup.dest")
			// Long enough that an anchor which is itself powered does not bounce the player straight
			// back, short enough to be usable as a two-way door.
			raw("scoreboard players set @s ${EnderIds.GRACE} 30")
			raw("execute at @s run playsound minecraft:entity.enderman.teleport player @s[scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1")
			raw("execute at @s run particle minecraft:portal ~ ~1 ~ 0.4 0.8 0.4 0.3 40 normal ${particleViewersSel()}")
			raw("return 1")
		}
		// {dim, x, y1, z}: written by register only.
		pack.define(tpTo) { raw("\$execute in \$(dim) run tp @s \$(x) \$(y1) \$(z)") }

		registerWrites()
	}

	private fun Ra.registerWrites() {
		// As the anchor marker. Cheap enough for a rename, not for every tick: the tick calls it
		// only when anchor_id differs from registered_id.
		pack.define(register) {
			raw("data modify storage ra:ender reg set value {n:1}")
			here()
			// What this anchor filed before leaves the table, if the entry is still its own.
			raw("data modify storage ra:ender reg.key set from entity @s data.data.registered_id")
			idCheck("storage ra:ender reg.key")
			raw("execute if data storage ra:ender reg.key if score #ender.idok $T matches 1 run function ${dropKey.asId()} with storage ra:ender reg")

			// The wanted id, in format ID. None at all (a new anchor) is quiet; anything changed is said.
			raw("scoreboard players set #ender.q $T 0")
			raw("execute unless data entity @s data.properties.anchor_id run scoreboard players set #ender.q $T 1")
			raw("""execute if data entity @s data.properties{anchor_id:""} run scoreboard players set #ender.q $T 1""")
			raw("data modify storage ra:ender reg.ask set from entity @s data.properties.anchor_id")
			cleanFrom("entity @s data.properties.anchor_id", TextFormat.ID)
			raw("data modify storage ra:ender reg.base set value \"anchor\"")
			raw("execute if score ${SafeClean.OK} $T matches 1 run data modify storage ra:ender reg.base set from storage ra:safe out")
			raw("data modify storage ra:ender reg.id set from storage ra:ender reg.base")

			// Free, or this anchor's own entry: the first of base, base_2, base_3, ...
			raw("scoreboard players set #ender.n $T 1")
			raw("execute store result score #ender.free $T run function ${findFree.asId()}")

			// Whatever happens, the id on the block is final, so the tick stops asking.
			raw("execute store success score #ender.chg $T run data modify entity @s data.properties.anchor_id set from storage ra:ender reg.id")
			raw("execute if score #ender.chg $T matches 1 if score #ender.q $T matches 0 run function ${tell.asId()}")
			raw("data modify entity @s data.data.registered_id set from storage ra:ender reg.id")
			raw("execute if score #ender.free $T matches 0 run return run data remove storage ra:ender reg")

			raw("data modify storage ra:ender reg.e set from storage ra:ender reg.me")
			raw("data modify storage ra:ender reg.e.id set from storage ra:ender reg.id")
			raw("data modify storage ra:ender reg.e.owner set from entity @s data.data.owner")
			raw("function ${put.asId()} with storage ra:ender reg")
			raw("data remove storage ra:ender reg")
		}

		pack.define(drop) {
			raw("data modify storage ra:ender reg set value {}")
			here()
			raw("data modify storage ra:ender reg.key set from entity @s data.data.registered_id")
			idCheck("storage ra:ender reg.key")
			raw("execute if data storage ra:ender reg.key if score #ender.idok $T matches 1 run function ${dropKey.asId()} with storage ra:ender reg")
			raw("data remove storage ra:ender reg")
		}

		// {key}: remove anchors.<key> when its position is reg.me. `set from` that changes nothing is equal.
		pack.define(dropKey) {
			raw("data remove storage ra:ender reg.cmp")
			raw("\$data modify storage ra:ender reg.cmp set from storage ra:ender $TABLE.\"\$(key)\"")
			raw("data remove storage ra:ender reg.cmp.id")
			raw("data remove storage ra:ender reg.cmp.owner")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.me")
			raw("\$execute if score #ender.diff $T matches 0 run data remove storage ra:ender $TABLE.\"\$(key)\"")
			raw("data remove storage ra:ender reg.cmp")
		}

		// reg.id free (or reg.me's own)? Otherwise try the next suffix. Returns 1 when one was found.
		pack.define(findFree) {
			raw("execute store result score #ender.ok $T run function ${claim.asId()} with storage ra:ender reg")
			raw("execute if score #ender.ok $T matches 1 run return 1")
			raw("scoreboard players add #ender.n $T 1")
			raw("execute if score #ender.n $T matches $MAX_SUFFIX.. run return 0")
			raw("execute store result storage ra:ender reg.n int 1 run scoreboard players get #ender.n $T")
			raw("function ${suffix.asId()} with storage ra:ender reg")
			raw("return run function ${findFree.asId()}")
		}
		// {id}: 1 when anchors.<id> is absent or is the anchor at reg.me.
		pack.define(claim) {
			raw("\$execute unless data storage ra:ender $TABLE.\"\$(id)\" run return 1")
			raw("data remove storage ra:ender reg.cmp")
			raw("\$data modify storage ra:ender reg.cmp set from storage ra:ender $TABLE.\"\$(id)\"")
			raw("data remove storage ra:ender reg.cmp.id")
			raw("data remove storage ra:ender reg.cmp.owner")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.cmp set from storage ra:ender reg.me")
			raw("data remove storage ra:ender reg.cmp")
			raw("execute if score #ender.diff $T matches 0 run return 1")
			raw("return 0")
		}
		// {base, n}: both in format ID, so the result is too.
		pack.define(suffix) { raw("\$data modify storage ra:ender reg.id set value \"\$(base)_\$(n)\"") }
		// {id}
		pack.define(put) { raw("\$data modify storage ra:ender $TABLE.\"\$(id)\" set from storage ra:ender reg.e") }

		// The anchor's id was changed for it: its owner, the admins and players nearby are told.
		// As the anchor marker, storage ra:ender reg {ask, id, me}, #ender.n the suffix tried last.
		pack.define(tell) {
			raw("tag @a remove $TELL")
			raw("tag @a[tag=${Admin.TAG}] add $TELL")
			raw("execute at @s run tag @a[distance=..8] add $TELL")
			raw("data modify storage ra:ender reg.owner set from entity @s data.data.owner")
			raw("execute if data storage ra:ender reg.owner as @a run function ${ownerMatch.asId()}")
			val where = """{text:"[RA] ",color:"gold"},{text:"Teleport Anchor at ",color:"gray"},{nbt:"reg.me.x",storage:"ra:ender",color:"white"},{text:" "},{nbt:"reg.me.y",storage:"ra:ender",color:"white"},{text:" "},{nbt:"reg.me.z",storage:"ra:ender",color:"white"},{text:": ",color:"gray"}"""
			val now = """{text:" so it is now ",color:"gray"},{nbt:"reg.id",storage:"ra:ender",color:"aqua"},{text:".",color:"gray"}"""
			raw("""execute if score #ender.n $T matches 2.. run tellraw @a[tag=$TELL] [$where,{text:"another anchor already has the id ",color:"gray"},{nbt:"reg.base",storage:"ra:ender",color:"yellow"},{text:",",color:"gray"},$now]""")
			raw("""execute if score #ender.n $T matches ..1 run tellraw @a[tag=$TELL] [$where,{text:"the id ",color:"gray"},{nbt:"reg.ask",storage:"ra:ender",color:"yellow"},{text:" may only use letters, digits and _ . + - : / #,",color:"gray"},$now]""")
			raw("tag @a remove $TELL")
		}
		// As a player: tagged when their UUID is reg.owner.
		pack.define(ownerMatch) {
			raw("data modify storage ra:ender reg.u set from entity @s UUID")
			raw("execute store success score #ender.diff $T run data modify storage ra:ender reg.u set from storage ra:ender reg.owner")
			raw("execute if score #ender.diff $T matches 0 run tag @s add $TELL")
			raw("data remove storage ra:ender reg.u")
		}
	}
}
