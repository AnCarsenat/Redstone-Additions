package ra.multiblocklib

import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import net.benwoodworth.knbt.buildNbtCompound
import net.benwoodworth.knbt.buildNbtList
import net.benwoodworth.knbt.put
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.raw
import ra.multiblocklib.build.Build
import ra.multiblocklib.spec.Compiled
import io.github.ayfri.kore.features.tags.functionTag
import io.github.ayfri.kore.functions.Function

/**
 * `ra_lib_multiblock:init` and `ra_lib_multiblock:register`: the library's state and the type
 * registry every other file reads.
 */
internal fun Ra.registerLifecycle() {
	// Called from ra:load.
	pack.define(MultiblockLib.init) {
		scoreboard.objectives.add(Obj.MULTIBLOCK)
		scoreboard.objectives.add(Obj.MB_TIMER)

		data(MB) { merge(buildNbtCompound { }) }
		data(Storage.TEMP) { merge(buildNbtCompound { put("bf", buildNbtCompound { }) }) }

		// Constant used by the rotation maths.
		scoreboard.players.set(mb("#mb_neg"), Obj.MULTIBLOCK, -1)

		// Type registry. Rebuilt from scratch on every load so a removed or edited type cannot
		// linger. A module registers its structures from #ra_lib_multiblock:load; everything the
		// library derives from them (rotations, IO maps) is regenerated right after.
		//
		// `registry` lists every type, compiled or not, for anything that enumerates them;
		// `registry_runtime` only the storage-registered ones, which build/all, generic/validate
		// and try_tier still have to walk at runtime. Compiled specs (spec/load) skip all three.
		data(MB) { modify("registry") { set(buildNbtList<Nothing> { }) } }
		data(MB) { modify("registry_runtime") { set(buildNbtList<Nothing> { }) } }
		data(MB) { modify("types") { set(buildNbtCompound { }) } }

		function(Compiled.load)
		function(hookTag(MultiblockLib.Hooks.LOAD))
		function(Build.all)
	}

	// Register one multiblock type, from a function listed in #ra_lib_multiblock:load, after
	// writing the spec to storage ra:multiblock spec:
	//
	//   data modify storage ra:multiblock spec set value {id:"rock_metallic_drill", name:"...",
	//     blocks:[{x:0,y:0,z:0,match:"minecraft:waxed_copper_block"}, ...],
	//     inputs:[{name:"input_1",x:0,y:0,z:-1}], outputs:[...], controls:[...]}
	//   function ra_lib_multiblock:register with storage ra:multiblock spec
	//
	// Offsets are written ONCE, relative to the base block, for a structure facing NORTH. The
	// library rotates them into the other three facings itself -- the four hand-maintained
	// coordinate tables the older multiblocks carry are what made adding a structure so
	// error-prone. `match` accepts a block id or a #block_tag; it is passed straight to
	// `execute if block`, so block states work too. Set rotates:0b on a rotationally symmetric
	// structure to have all four facings resolve to the authored offsets unchanged.
	pack.define(MultiblockLib.register) {
		raw("\$data modify storage ra:multiblock types.\$(id) set from storage ra:multiblock spec")
		raw("\$data modify storage ra:multiblock registry append value \"\$(id)\"")
		raw("\$data modify storage ra:multiblock registry_runtime append value \"\$(id)\"")
		mbRemove("spec")
	}
}

/**
 * The hook tags other modules add to (see [MultiblockLib.Hooks]). Declared here, empty, so that
 * the library's own `function #ra_lib_multiblock:*` calls resolve whether or not any module has
 * hooked into them yet; hooks appended later land in the same file.
 */
internal fun Ra.declareHookTags() {
	with(MultiblockLib.Hooks) {
		for (tag in listOf(LOAD, TICK, VALIDATE, SETUP_TYPE, CHECK_STRUCTURE, ON_BREAK)) {
			pack.functionTag(tag, Ns.LIB_MULTIBLOCK.id)
		}
	}
}

/** `#ra_lib_multiblock:<tag>` as a callable argument. */
internal fun Function.hookTag(tag: String) = datapack.functionTag(tag, Ns.LIB_MULTIBLOCK.id)
