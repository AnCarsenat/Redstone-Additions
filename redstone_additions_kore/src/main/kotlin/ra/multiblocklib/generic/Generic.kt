package ra.multiblocklib.generic

import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.functions.Function
import net.benwoodworth.knbt.NbtByte
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.hook
import ra.core.raw
import ra.core.ref
import ra.multiblocklib.MB
import ra.multiblocklib.mbCopy
import ra.multiblocklib.mbRemove
import ra.multiblocklib.returnIfMbScore
import ra.multiblocklib.returnUnlessMb
import ra.multiblocklib.structure.Structure

/**
 * `ra_lib_multiblock:generic/` functions: the assembly validator, marker tagging and integrity check for
 * every structure registered with `ra_lib_multiblock:register`. They run from the same hook tags
 * as the hand-written validators (blast_forge, upgrade_platform), which answer for their own
 * types; these only answer for registered ones.
 */
object Generic {
	val validate = ref(Ns.LIB_MULTIBLOCK, "generic/validate")
	val validateNext = ref(Ns.LIB_MULTIBLOCK, "generic/validate_next")
	val validateType = ref(Ns.LIB_MULTIBLOCK, "generic/validate_type")
	val accept = ref(Ns.LIB_MULTIBLOCK, "generic/accept")
	val setupType = ref(Ns.LIB_MULTIBLOCK, "generic/setup_type")
	val setupTypeRun = ref(Ns.LIB_MULTIBLOCK, "generic/setup_type_run")
	val checkStructure = ref(Ns.LIB_MULTIBLOCK, "generic/check_structure")

	private val FACINGS = listOf("north", "south", "east", "west")

	fun Ra.registerGeneric() {
		// Hook: #ra_lib_multiblock:validate. At the base position; storage ra:multiblock type
		// holds the type being attempted.
		pack.define(validate) {
			returnIfMbScore("#mb_result", 1, 0)
			mbCopy("scan_q", "registry")
			function(validateNext)
			mbRemove("scan_q", "scan")
		}

		// Try each registered type until one claims the assembly.
		pack.define(validateNext) {
			returnIfMbScore("#mb_result", 1, 1)
			returnUnlessMb("scan_q[0]")
			mbCopy("scan.type", "scan_q[0]")
			mbRemove("scan_q[0]")
			function(validateType, MB, "scan")
			function(validateNext)
		}

		pack.define(validateType) { validateType() }

		// Publish the winning facing and its IO maps in the shape setup_marker expects.
		pack.define(accept) {
			raw("\$data modify storage ra:multiblock facing set value \"\$(facing)\"")
			for (map in listOf("inputs", "outputs", "controls")) {
				raw("\$data modify storage ra:multiblock $map set from storage ra:multiblock types.\$(type).facings.\$(facing).$map")
			}
		}

		// Hook: #ra_lib_multiblock:setup_type. As the new marker.
		pack.define(setupType) {
			data(MB) { modify("st.type") { set(self(), "data.type") } }
			function(setupTypeRun, MB, "st")
			mbRemove("st")
		}

		// Apply ra.multiblock.{type} and mark the marker as registry-driven, so
		// generic/check_structure knows it can validate it from the spec.
		pack.define(setupTypeRun) {
			raw("\$execute unless data storage ra:multiblock types.\$(type) run return 0")
			raw("\$tag @s add ra.multiblock.\$(type)")
			data(self()) { modify("data.registry_driven") { set(NbtByte(1)) } }
		}

		// Hook: #ra_lib_multiblock:check_structure. As the marker, at the base position. Sets
		// @s ra.multiblock to 1 when the structure still stands.
		pack.define(checkStructure) {
			execute {
				unlessCondition { data(self(), "data{registry_driven:1b}") }
				run { returnValue(0) }
			}
			data(MB) { modify("chk_marker.type") { set(self(), "data.type") } }
			data(MB) { modify("chk_marker.facing") { set(self(), "data.facing") } }
			execute {
				storeResult { score(self(), Obj.MULTIBLOCK) }
				run { function(Structure.check, MB, "chk_marker") }
			}
			mbRemove("chk_marker")
		}

		// Last in each tag, as in beet: the generic handlers only cover types no structure-specific
		// hook claimed, so every other part must have appended its entries first.
		afterAllParts {
			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.CHECK_STRUCTURE, checkStructure)
			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.SETUP_TYPE, setupType)
			pack.hook(Ns.LIB_MULTIBLOCK, MultiblockLib.Hooks.VALIDATE, validate)
		}
	}

	/**
	 * `generic/validate_type {type}`: try all four facings of one registered type at the current
	 * position. The first facing that matches wins and publishes its IO maps for create_marker.
	 */
	private fun Function.validateType() {
		raw("\$execute unless data storage ra:multiblock {type:\"\$(type)\"} run return 0")
		for (facing in FACINGS) {
			val args = "{type:\"\$(type)\",facing:\"$facing\"}"
			raw("\$execute store result score #mb_result ${Obj.MULTIBLOCK} run function ${Structure.check.asId()} $args")
			raw("\$execute if score #mb_result ${Obj.MULTIBLOCK} matches 1 run function ${accept.asId()} $args")
			returnIfMbScore("#mb_result", 1, 1)
		}
		returnValue(0)
	}
}
