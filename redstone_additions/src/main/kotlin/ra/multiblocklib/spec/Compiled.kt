package ra.multiblocklib.spec

import io.github.ayfri.kore.arguments.types.BlockOrTagArgument
import io.github.ayfri.kore.arguments.types.resources.BlockArgument
import io.github.ayfri.kore.arguments.types.resources.tagged.BlockTagArgument
import io.github.ayfri.kore.features.predicates.conditions.allOf
import io.github.ayfri.kore.features.predicates.conditions.locationCheck
import io.github.ayfri.kore.features.predicates.predicate
import io.github.ayfri.kore.features.predicates.sub.block
import io.github.ayfri.kore.functions.Function
import io.github.ayfri.kore.generated.arguments.types.PredicateArgument
import ra.api.MultiblockLib
import ra.api.MultiblockSpec
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref

/**
 * `ra_lib_multiblock:spec/` functions and `ra_lib_multiblock:structure/` predicates: the
 * multiblocks described in Kotlin ([MultiblockSpec], registered with [ra.api.multiblock]),
 * compiled when the pack is generated.
 *
 * Everything the runtime path (`register`, `build/`, `structure/check`, `generic/`) computes
 * with macro queues on every load, assembly and validation is known here, so it is written out:
 *
 * - one predicate per spec x facing, an `all_of` of offset `location_check`s, so a whole
 *   structure is one `execute if predicate`;
 * - a validator per spec that tries its facings' predicates and publishes the facing and IO maps
 *   as SNBT literals;
 * - an integrity check per spec that tests the marker's facing's predicate;
 * - a tagger, a `try_tier` entry per tier, and the storage view (`types.<id>`) as one literal.
 *
 * The dispatchers below join the same hook tags as every other structure, ahead of `generic/`.
 */
object Compiled {
	val load = ref(Ns.LIB_MULTIBLOCK, "spec/load")
	val validate = ref(Ns.LIB_MULTIBLOCK, "spec/validate")
	val checkStructure = ref(Ns.LIB_MULTIBLOCK, "spec/check_structure")
	val setupType = ref(Ns.LIB_MULTIBLOCK, "spec/setup_type")
	val tryTier = ref(Ns.LIB_MULTIBLOCK, "spec/try_tier")
	/** As any compiled type's marker, at its base: that type's per-block report ([debugOf]). */
	val debug = ref(Ns.LIB_MULTIBLOCK, "spec/debug")

	private fun typeFn(spec: MultiblockSpec, path: String) = ref(Ns.LIB_MULTIBLOCK, "spec/${spec.id}/$path")

	/** `spec/<id>/validate`: at the base, with `storage ra:multiblock type` set to [spec]'s id. */
	fun validateOf(spec: MultiblockSpec) = typeFn(spec, "validate")

	/** `spec/<id>/check`: as one of [spec]'s markers, at the base; sets `@s ra.multiblock`. */
	fun checkOf(spec: MultiblockSpec) = typeFn(spec, "check")

	/**
	 * `spec/<id>/debug`: as one of [spec]'s markers, at the base. Tells `@a[tag=ra.debug]` which
	 * blocks of the marker's facing are wrong, then how many are right.
	 */
	fun debugOf(spec: MultiblockSpec) = typeFn(spec, "debug")

	/** `spec/<id>/debug/<facing>`: the same report at a bare base, no marker needed. */
	fun debugOf(spec: MultiblockSpec, facing: String) = typeFn(spec, "debug/$facing")
	private fun tierFn(tier: String) = ref(Ns.LIB_MULTIBLOCK, "spec/tier/$tier")

	private const val TIER_DONE = "#mb_tier_done"

	/**
	 * Queued with afterAllParts from the library's register(), so it runs once every module has
	 * registered its specs, and before generic's own afterAllParts: the compiled hooks therefore
	 * sit after the hand-written structures' and ahead of `generic/` in each tag.
	 */
	fun Ra.registerCompiled() {
		afterAllParts {
			multiblockSpecsCompiled = true
			val specs = multiblockSpecs.toList()

			for (spec in specs) compile(spec)

			// Called from init, before #ra_lib_multiblock:load: the storage view of every compiled
			// type. `registry` lists every type (compatibility); `registry_runtime`, which the
			// runtime build / validate / try_tier walks read, lists only storage-registered ones.
			pack.define(load) {
				for (spec in specs) {
					raw("data modify storage ra:multiblock types.${spec.id} set value ${StorageView.snbt(spec)}")
					raw("data modify storage ra:multiblock registry append value ${StorageView.str(spec.id)}")
				}
			}

			// Hook: #ra_lib_multiblock:validate. At the base; storage ra:multiblock type is the type
			// being attempted.
			pack.define(validate) {
				raw("execute if score #mb_result ${Obj.MULTIBLOCK} matches 1 run return 0")
				for (spec in specs)
					raw("execute if data storage ra:multiblock {type:${StorageView.str(spec.id)}} run return run function ${typeFn(spec, "validate").asId()}")
			}

			// Hook: #ra_lib_multiblock:check_structure. As the marker, at the base.
			pack.define(checkStructure) {
				for (spec in specs)
					raw("execute if entity @s[tag=${spec.tag}] run return run function ${typeFn(spec, "check").asId()}")
			}

			// Hook: #ra_lib_multiblock:setup_type. As the new marker.
			pack.define(setupType) {
				for (spec in specs)
					raw("execute if data entity @s data{type:${StorageView.str(spec.id)}} run return run tag @s add ${spec.tag}")
			}

			// Called by try_tier, before the runtime registry walk. storage ra:multiblock
			// tier_scan.tier is the requested tier; sets #mb_tier_done ra.temp to 1 on assembly.
			val tiers = specs.groupBy { it.tier }
			pack.define(tryTier) {
				for (tier in tiers.keys)
					raw("execute if data storage ra:multiblock tier_scan{tier:${StorageView.str(tier)}} run return run function ${tierFn(tier).asId()}")
			}
			for ((tier, members) in tiers) pack.define(tierFn(tier)) {
				for (spec in members) {
					raw("data modify storage ra:multiblock type set value ${StorageView.str(spec.id)}")
					raw("execute store result score $TIER_DONE ${Obj.TEMP} run function ${MultiblockLib.tryAssemble.asId()}")
					raw("execute if score $TIER_DONE ${Obj.TEMP} matches 1 run return 1")
				}
				raw("return 0")
			}

			// As a marker, at its base: the per-block report of whichever compiled type it is.
			pack.define(debug) {
				for (spec in specs)
					raw("execute if entity @s[tag=${spec.tag}] run return run function ${debugOf(spec).asId()}")
				raw("return 0")
			}

			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.CHECK_STRUCTURE, checkStructure)
			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.SETUP_TYPE, setupType)
			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.VALIDATE, validate)
		}
	}

	/** The predicates and per-type functions of one spec. */
	private fun Ra.compile(spec: MultiblockSpec) {
		for (facing in spec.facings) structurePredicate(spec, facing)

		// At the base. Tries each facing; the first whose predicate holds publishes its data.
		pack.define(typeFn(spec, "validate")) {
			for (facing in spec.facings)
				raw("execute if predicate ${spec.predicate(facing).asId()} run return run function ${typeFn(spec, "accept/$facing").asId()}")
			raw("return 0")
		}

		// The winning facing and its IO maps, in the shape setup_marker copies onto the marker.
		for (facing in spec.facings) pack.define(typeFn(spec, "accept/$facing")) {
			raw("scoreboard players set #mb_result ${Obj.MULTIBLOCK} 1")
			raw("data modify storage ra:multiblock facing set value ${StorageView.str(facing)}")
			raw("data modify storage ra:multiblock inputs set value ${StorageView.ports(spec.portsFacing(spec.inputs, facing))}")
			raw("data modify storage ra:multiblock outputs set value ${StorageView.ports(spec.portsFacing(spec.outputs, facing))}")
			raw("data modify storage ra:multiblock controls set value ${StorageView.ports(spec.portsFacing(spec.controls, facing))}")
			raw("return 1")
		}

		// As the marker, at the base. validate_single zeroed @s ra.multiblock; one predicate sets it.
		pack.define(typeFn(spec, "check")) { check(spec) }

		// The per-block report, generated from the same rotated block list as the predicates.
		for (facing in spec.facings) pack.define(debugOf(spec, facing)) { debugBlocks(spec, facing) }
		pack.define(debugOf(spec)) {
			if (!spec.rotates) {
				raw("return run function ${debugOf(spec, spec.symmetricFacing).asId()}")
				return@define
			}
			for (facing in spec.facings)
				raw("execute if data entity @s data{facing:${StorageView.str(facing)}} run return run function ${debugOf(spec, facing).asId()}")
			raw("""tellraw @a[tag=${Tag.DEBUG}] [$DEBUG_PREFIX,{text:${StorageView.str("${spec.name}: ")}},{text:"no facing on the marker",color:"red"},{text:" data="},{nbt:"data",entity:"@s",color:"yellow"}]""")
			raw("return 0")
		}
	}

	private const val DEBUG_PREFIX = """{text:"[MB Debug] ",color:"gold"}"""
	private const val DEBUG_OK = "#mb_debug_ok"

	/**
	 * At the base: one FAIL line per block of [spec] turned to [facing] that is missing, with its
	 * offset and what belongs there, then `<ok>/<total> blocks OK`. Returns the OK count.
	 */
	private fun Function.debugBlocks(spec: MultiblockSpec, facing: String) {
		val to = "@a[tag=${Tag.DEBUG}]"
		val blocks = spec.blocksFacing(facing)
		raw("""tellraw $to [$DEBUG_PREFIX,{text:${StorageView.str("${spec.name} ($facing):")},color:"aqua"}]""")
		raw("scoreboard players set $DEBUG_OK ${Obj.TEMP} 0")
		for (b in blocks) {
			val at = "~${b.pos.x} ~${b.pos.y} ~${b.pos.z}"
			raw("execute if block $at ${b.match} run scoreboard players add $DEBUG_OK ${Obj.TEMP} 1")
			raw("""execute unless block $at ${b.match} run tellraw $to [{text:"  FAIL ",color:"red"},{text:${StorageView.str(at)},color:"yellow"},{text:" needs ",color:"gray"},{text:${StorageView.str(b.match)}}]""")
		}
		raw("""tellraw $to [$DEBUG_PREFIX,{score:{name:"$DEBUG_OK",objective:"${Obj.TEMP}"},color:"green"},{text:"/${blocks.size} blocks OK",color:"gray"}]""")
		raw("return run scoreboard players get $DEBUG_OK ${Obj.TEMP}")
	}

	private fun Function.check(spec: MultiblockSpec) {
		// Markers assembled before this type was compiled carry generic's registry_driven flag,
		// which would have generic/check_structure walk the storage pattern a second time.
		raw("execute if data entity @s data.registry_driven run data remove entity @s data.registry_driven")
		if (!spec.rotates) {
			raw("execute store result score @s ${Obj.MULTIBLOCK} if predicate ${spec.predicate().asId()}")
			return
		}
		for (facing in spec.facings)
			raw("execute if data entity @s data{facing:${StorageView.str(facing)}} run return run execute store result score @s ${Obj.MULTIBLOCK} if predicate ${spec.predicate(facing).asId()}")
	}

	/** `ra_lib_multiblock:structure/<id>[/<facing>]`: every block of [spec], turned to [facing]. */
	private fun Ra.structurePredicate(spec: MultiblockSpec, facing: String): PredicateArgument {
		val id = spec.predicate(facing)
		pack.predicate(id.name) {
			namespace = id.namespace
			allOf {
				for (b in spec.blocksFacing(facing)) {
					val (target, states) = parseMatch(spec, b.match)
					locationCheck(b.pos.x.takeIf { it != 0 }, b.pos.y.takeIf { it != 0 }, b.pos.z.takeIf { it != 0 }) {
						block(target) { if (states.isNotEmpty()) state = states }
					}
				}
			}
		}
		return id
	}

	/** `id`, `ns:id`, `#ns:tag` or `ns:id[k=v,...]` -> a block or tag argument and its states. */
	internal fun parseMatch(spec: MultiblockSpec, match: String): Pair<BlockOrTagArgument, Map<String, String>> {
		val bad = { "multiblock '${spec.id}': can't read block match '$match'" }
		if (match.startsWith("#")) {
			val (ns, path) = split(match.removePrefix("#"))
			require('[' !in path) { bad() }
			return BlockTagArgument(path, ns) to emptyMap()
		}
		val idPart = match.substringBefore('[')
		val states = if ('[' in match) {
			require(match.endsWith("]")) { bad() }
			match.substringAfter('[').removeSuffix("]").split(',').filter { it.isNotBlank() }.associate {
				val kv = it.split('=')
				require(kv.size == 2) { bad() }
				kv[0].trim() to kv[1].trim()
			}
		} else emptyMap()
		val (ns, path) = split(idPart)
		return BlockArgument(path, ns) to states
	}

	private fun split(id: String): Pair<String, String> =
		if (':' in id) id.substringBefore(':') to id.substringAfter(':') else "minecraft" to id
}

/** SNBT literals of a spec, in exactly the shapes the runtime build writes to storage. */
internal object StorageView {
	private val bareKey = Regex("[A-Za-z0-9_.+-]+")

	fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
	private fun key(s: String) = if (bareKey.matches(s)) s else str(s)
	private fun xyz(p: MultiblockSpec.Pos) = "x:${p.x},y:${p.y},z:${p.z}"

	/** `{name:{x,y,z}, ...}`: an IO map as setup_marker copies it onto the marker. */
	fun ports(ports: List<MultiblockSpec.Port>) = ports.joinToString(",", "{", "}") { "${key(it.name)}:{${xyz(it.pos)}}" }

	private fun portList(ports: List<MultiblockSpec.Port>) = ports.joinToString(",", "[", "]") { "{name:${str(it.name)},${xyz(it.pos)}}" }

	/** `types.<id>`: the authored spec, plus `facings.<f>` as build/facing would have derived it. */
	fun snbt(spec: MultiblockSpec): String = buildString {
		append("{id:${str(spec.id)},name:${str(spec.name)},tier:${str(spec.tier)},hint:${str(spec.hint)},compiled:1b")
		if (!spec.rotates) append(",rotates:0b")
		append(",blocks:").append(spec.blocks.joinToString(",", "[", "]") { "{${xyz(it.pos)},match:${str(it.match)}}" })
		append(",inputs:").append(portList(spec.inputs))
		append(",outputs:").append(portList(spec.outputs))
		append(",controls:").append(portList(spec.controls))
		append(",facings:{")
		// Every facing, even for a non-rotating spec, whose four are identical (as build/type writes them).
		append(MultiblockSpec.FACINGS.joinToString(",") { f ->
			val blocks = spec.blocksFacing(f).joinToString(",", "[", "]") { b ->
				"{pos:\"~${b.pos.x} ~${b.pos.y} ~${b.pos.z}\",match:${str(b.match)}}"
			}
			"$f:{blocks:$blocks,inputs:${ports(spec.portsFacing(spec.inputs, f))},outputs:${ports(spec.portsFacing(spec.outputs, f))},controls:${ports(spec.portsFacing(spec.controls, f))}}"
		})
		append("}}")
	}
}
