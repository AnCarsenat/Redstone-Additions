package ra.multiblocklib

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.maths.Axes
import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrDoubleEnd
import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.literal
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.selector.Sort
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.generated.EntityTypes
import io.github.ayfri.kore.functions.Function
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref
import ra.multiblocklib.spec.Compiled

/** Assembly: `try_assemble`, `try_tier` and the marker it creates. */
internal object Assembly {
	val tierNext = ref(Ns.LIB_MULTIBLOCK, "tier_next")
	val tierTry = ref(Ns.LIB_MULTIBLOCK, "tier_try")
	val createMarker = ref(Ns.LIB_MULTIBLOCK, "create_marker")
	val setupMarker = ref(Ns.LIB_MULTIBLOCK, "setup_marker")

	const val NEW = "ra.multiblock.new"
	const val GRACE = "ra.multiblock.grace"

	private val tierDone = literal("#mb_tier_done")

	fun Ra.registerAssembly() {
		pack.define(MultiblockLib.tryAssemble) { tryAssemble() }

		// `try_tier {tier}`: assemble any registered structure belonging to a base tier, at the
		// current position. Returns 1 on the first structure that assembles, else 0.
		//
		// This is what lets a new multiblock be added without touching the wrench: the tier
		// functions ask the registry instead of naming one hardcoded type each.
		pack.define(MultiblockLib.tryTier) {
			scoreboard.players.set(tierDone, Obj.TEMP, 0)
			raw("\$data modify storage ra:multiblock tier_scan.tier set value \"\$(tier)\"")
			// Compiled specs first: a static list per tier, no macro walk.
			function(Compiled.tryTier)
			// Then the storage-registered types (tier_next returns at once if one already assembled).
			mbCopy("tier_q", "registry_runtime")
			function(tierNext)
			mbRemove("tier_q", "tier_scan")
			returnRun { scoreboard.players.get(tierDone, Obj.TEMP) }
		}

		// Walk the registry, stopping at the first structure that assembles.
		pack.define(tierNext) {
			execute {
				ifCondition { score(tierDone, Obj.TEMP) equalTo 1 }
				run { returnValue(1) }
			}
			returnUnlessMb("tier_q[0]")
			mbCopy("tier_scan.type", "tier_q[0]")
			mbRemove("tier_q[0]")
			function(tierTry, MB, "tier_scan")
			function(tierNext)
		}

		// `tier_try {type, tier}`: attempt one registered type, if it belongs to the requested tier.
		pack.define(tierTry) {
			raw("\$execute unless data storage ra:multiblock types.\$(type){tier:\"\$(tier)\"} run return 0")
			raw("\$data modify storage ra:multiblock type set value \"\$(type)\"")
			execute {
				storeResult { score(tierDone, Obj.TEMP) }
				run { function(MultiblockLib.tryAssemble) }
			}
		}

		pack.define(createMarker) { createMarker() }
		pack.define(setupMarker) { setupMarker() }
	}

	/**
	 * At the block position. Input storage ra:multiblock {type}. Returns 1 if assembled.
	 */
	private fun Function.tryAssemble() {
		scoreboard.players.set(mb("#mb_result"), Obj.MULTIBLOCK, 0)

		// Normalised to the block centre for consistent distance checks: already assembled here?
		execute {
			align(Axes.XYZ)
			positioned(vec3(0.5, 0.0, 0.5).relative)
			ifCondition { entity(allEntities(true) { type = EntityTypes.MARKER; tag = Tag.MULTIBLOCK; distance = rangeOrDoubleEnd(0.5); sort = Sort.NEAREST }) }
			run { returnValue(0) }
		}

		// Each validator checks its own type and sets #mb_result to 1 if valid.
		function(hookTag(MultiblockLib.Hooks.VALIDATE))

		execute {
			ifCondition { score(mb("#mb_result"), Obj.MULTIBLOCK) equalTo 1 }
			run { function(createMarker) }
		}

		// Clean up the staging data. `type` is cleared too: it used to survive a failed attempt,
		// so a later assembly could be validated against a type the player never asked for.
		mbRemove("type", "facing", "inputs", "outputs", "controls")

		returnRun { scoreboard.players.get(mb("#mb_result"), Obj.MULTIBLOCK) }
	}

	/** At the multiblock base. Input storage ra:multiblock {type, facing, inputs, outputs, controls}. */
	private fun Function.createMarker() {
		// data:{_init:1b} ensures the compound exists -- an empty data:{} gets removed by the game.
		raw("execute align xyz run summon marker ~0.5 ~ ~0.5 {data:{_init:1b},Tags:[\"ra\",\"ra.multiblock\",\"$NEW\"]}")

		// type + distance are required: without them a stale or concurrently created
		// ra.multiblock.new marker anywhere in the world could be picked instead.
		execute {
			align(Axes.XYZ)
			positioned(vec3(0.5, 0.0, 0.5).relative)
			asTarget(allEntities(true) { type = EntityTypes.MARKER; tag = NEW; distance = rangeOrDoubleEnd(0.5); sort = Sort.NEAREST })
			run { function(setupMarker) }
		}

		// Assembly effects.
		raw("playsound minecraft:block.respawn_anchor.set_spawn block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 1 0.8")
		raw("playsound minecraft:block.beacon.activate block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.8 1.5")
		raw("playsound minecraft:entity.player.levelup block @a[distance=..16,scores={${Obj.USER_SOUND}=1..}] ~ ~ ~ 0.5 0.5")
		raw("particle minecraft:totem_of_undying ~ ~1 ~ 0.5 0.5 0.5 0.3 50 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
		raw("particle minecraft:electric_spark ~ ~0.5 ~ 1 1 1 0.1 30 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")
		raw("particle minecraft:entity_effect{color:[1.0,0.85,0.0,1.0]} ~ ~0.5 ~ 0.5 0.5 0.5 0 20 normal @a[scores={${Obj.USER_PARTICLES}=1..}]")

		raw("""tellraw @a[distance=..16] [{text:"[RA] ",color:"gold"},{text:"Multiblock assembled!",color:"green",bold:true}]""")
	}

	/**
	 * As the newly summoned marker (tagged ra.multiblock.new). Standard multiblock data:
	 *   data.type        multiblock type id ("blast_forge")
	 *   data.facing      "north" / "south" / "east" / "west"
	 *   data.properties  {speed:1}
	 *   data.inputs      {input_1:{x,y,z}, ...}  IO block offsets from the base
	 *   data.outputs     {output_1:{x,y,z}, ...}
	 *   data.controls    {redstone_in:{x,y,z}, ...}
	 */
	private fun Function.setupMarker() {
		// One merge, so the compound and every key exist before the individual writes.
		raw("""data merge entity @s {data:{type:"unknown",facing:"none",properties:{speed:1},inputs:{},outputs:{},controls:{}}}""")

		data(self()) { modify("data.type") { set(MB, "type") } }
		data(self()) { modify("data.facing") { set(MB, "facing") } }
		for (map in listOf("inputs", "outputs", "controls")) execute {
			ifCondition { data(MB, map) }
			run { data(self()) { modify("data.$map") { set(MB, map) } } }
		}

		// Type-specific tags via registered hooks.
		function(hookTag(MultiblockLib.Hooks.SETUP_TYPE))

		// Grace period: skip the first validation cycle to avoid race conditions.
		tag(self()) { add(GRACE) }
		tag(self()) { remove(NEW) }
	}
}
