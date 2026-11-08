package ra.multiblocklib.io

import io.github.ayfri.kore.arguments.maths.vec3
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.returnRun
import io.github.ayfri.kore.commands.returnValue
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.functions.Function
import ra.api.MultiblockLib
import ra.api.RaLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Storage
import ra.core.define
import ra.core.macroCall
import ra.core.raw
import ra.core.ref
import ra.multiblocklib.MB
import ra.multiblocklib.mb
import ra.multiblocklib.mbCopy
import ra.multiblocklib.mbRemove
import ra.multiblocklib.returnUnlessMb

/**
 * `ra_lib_multiblock:io/` functions: item and block access to a multiblock's named IO positions.
 *
 * All of these take the marker as `@s`, positioned at the base, and resolve names against the
 * marker's own `data.inputs` / `data.outputs` / `data.controls`. They work for any multiblock that
 * went through setup_marker, compiled or storage-registered -- a tick function never needs to
 * branch on facing. That was the main reason every multiblock ended up with four near-identical
 * copies of its processing logic.
 *
 * For items with components, build `storage ra:multiblock io_item` yourself and call
 * `io/at {name:"...",run:"ra_lib_multiblock:io/insert_here"}` -- that avoids quoting a
 * component compound through a macro argument.
 */
object Io {
	val at = MultiblockLib.Io.at
	val atRun = ref(Ns.LIB_MULTIBLOCK, "io/at_run")
	val insert = ref(Ns.LIB_MULTIBLOCK, "io/insert")
	val insertHere = MultiblockLib.Io.insertHere
	val extract = ref(Ns.LIB_MULTIBLOCK, "io/extract")
	val extractHere = ref(Ns.LIB_MULTIBLOCK, "io/extract_here")
	val peek = ref(Ns.LIB_MULTIBLOCK, "io/peek")
	val peekHere = ref(Ns.LIB_MULTIBLOCK, "io/peek_here")
	val count = ref(Ns.LIB_MULTIBLOCK, "io/count")
	val countHere = ref(Ns.LIB_MULTIBLOCK, "io/count_here")
	val isBlock = MultiblockLib.Io.isBlock
	val isBlockHere = ref(Ns.LIB_MULTIBLOCK, "io/is_block_here")
	val isBlockTest = ref(Ns.LIB_MULTIBLOCK, "io/is_block_test")

	fun Ra.registerIo() {
		// `io/at {name, run}`: returns 1 when the slot exists, 0 when the name is unknown.
		pack.define(at) {
			mbRemove("io_pos")
			for (map in listOf("inputs", "outputs", "controls")) {
				raw("\$execute if data entity @s data.$map.\$(name) run data modify storage ra:multiblock io_pos set from entity @s data.$map.\$(name)")
			}
			returnUnlessMb("io_pos")
			raw("\$data modify storage ra:multiblock io_pos.run set value \"\$(run)\"")
			function(atRun, MB, "io_pos")
			returnValue(1)
		}

		// Offset from the base to the IO block and hand over.
		pack.define(atRun) {
			raw("\$execute positioned ~\$(x) ~\$(y) ~\$(z) run function \$(run)")
		}

		// `io/insert {name, id, count, components}`: returns 1 when the slot exists (the insert
		// itself may still be refused by a full container).
		pack.define(insert) {
			raw("\$data modify storage ra:multiblock io_item set value {id:\"\$(id)\",count:\$(count),components:\$(components)}")
			raw("\$return run " + macroCall(at, "{name:\"\$(name)\",run:\"${insertHere.asId()}\"}"))
		}

		// insert_or_drop: a multiblock producing into a full output barrel used to have its
		// product deleted by `loot insert`.
		pack.define(insertHere) {
			data(Storage.INVENTORY) { modify("item") { set(MB, "io_item") } }
			function(RaLib.Inventory.insertOrDrop)
		}

		// `io/extract {name, id, count}`: returns 1 only when the full amount was removed.
		// All-or-nothing: a container holding too few leaves its contents untouched.
		pack.define(extract) {
			scoreboard.players.set(mb("#mb_io"), Obj.MULTIBLOCK, 0)
			raw("\$data modify storage ra:multiblock io_req set value {id:\"\$(id)\",count:\$(count)}")
			callAt(extractHere)
			returnRun { scoreboard.players.get(mb("#mb_io"), Obj.MULTIBLOCK) }
		}

		pack.define(extractHere) {
			execute {
				storeResult { score(mb("#mb_io"), Obj.MULTIBLOCK) }
				run { function(RaLib.Inventory.remove, MB, "io_req") }
			}
		}

		// `io/peek {name}`: copy the first stack to storage ra:multiblock io_item. Use it to look
		// at what is waiting before deciding whether to consume it.
		pack.define(peek) {
			mbRemove("io_item")
			scoreboard.players.set(mb("#mb_io"), Obj.MULTIBLOCK, 0)
			callAt(peekHere)
			returnRun { scoreboard.players.get(mb("#mb_io"), Obj.MULTIBLOCK) }
		}

		pack.define(peekHere) {
			execute {
				unlessCondition { data(vec3().relative, "Items[0]") }
				run { returnValue(0) }
			}
			data(MB) { modify("io_item") { set(vec3().relative, "Items[0]") } }
			scoreboard.players.set(mb("#mb_io"), Obj.MULTIBLOCK, 1)
		}

		// `io/count {name}`: occupied slots, 0 if empty or if the name is unknown.
		pack.define(count) {
			scoreboard.players.set(mb("#mb_io"), Obj.MULTIBLOCK, 0)
			callAt(countHere)
			returnRun { scoreboard.players.get(mb("#mb_io"), Obj.MULTIBLOCK) }
		}

		pack.define(countHere) {
			execute {
				ifCondition { data(vec3().relative, "Items") }
				storeResult { score(mb("#mb_io"), Obj.MULTIBLOCK) }
				run { data(vec3().relative) { get("Items") } }
			}
		}

		// `io/is_block {name, block}`: `block` is substituted verbatim, so #block_tags and block
		// states work.
		//   execute store result score #x obj run function ra_lib_multiblock:io/is_block {name:"redstone_in",block:"minecraft:redstone_block"}
		pack.define(isBlock) {
			scoreboard.players.set(mb("#mb_io"), Obj.MULTIBLOCK, 0)
			raw("\$data modify storage ra:multiblock io_test set value {match:\"\$(block)\"}")
			callAt(isBlockHere)
			returnRun { scoreboard.players.get(mb("#mb_io"), Obj.MULTIBLOCK) }
		}

		// Hands the comparison to a macro fed from its own small compound -- passing the whole
		// ra:multiblock storage as macro arguments would stringify every unrelated key on every call.
		pack.define(isBlockHere) {
			function(isBlockTest, MB, "io_test")
		}

		// Split out so the match string can be a macro.
		pack.define(isBlockTest) {
			raw("\$execute if block ~ ~ ~ \$(match) run scoreboard players set #mb_io ${Obj.MULTIBLOCK} 1")
		}
	}

	/** `$function ra_lib_multiblock:io/at {name:"$(name)",run:"<here>"}`. */
	private fun Function.callAt(here: FunctionArgument) {
		raw("\$" + macroCall(at, "{name:\"\$(name)\",run:\"${here.asId()}\"}"))
	}
}
