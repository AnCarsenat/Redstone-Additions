package ra.api

import ra.core.Ns
import ra.core.ref

/**
 * Public entry points of the shared libraries, as typed references.
 *
 * These objects are the contract between a library and its callers. The library defines each
 * function with `define(RaLib.Redstone.detect) { ... }`; callers write
 * `function(RaLib.Redstone.detect)`. Moving or renaming a function is one edit here, and
 * PackValidator fails the build if a reference has no definition.
 *
 * Only cross-namespace entry points belong here. A library's private helpers stay private to its
 * package.
 */
object RaLib {
	val init = ref(Ns.LIB, "init")

	/** Custom-block placement. See ra.lib.placement. */
	object Placement {
		/** Macro `{block_id, block_tag, dir_type}`. Places the block and summons its marker tagged `ra.new`. */
		val place = ref(Ns.LIB, "placement/place")
	}

	/** Redstone power readers. Cheapest first: [any] < [detectSwitch] < [Local] < [side] < [detect] < [detectLocal]. */
	object Redstone {
		val any = ref(Ns.LIB, "redstone/any")
		val detectSwitch = ref(Ns.LIB, "redstone/detect_switch")
		val detect = ref(Ns.LIB, "redstone/detect")
		val detectLocal = ref(Ns.LIB, "redstone/detect_local")
		val side = ref(Ns.LIB, "redstone/side")
		val countInputs = ref(Ns.LIB, "redstone/count_inputs")
		val clear = ref(Ns.LIB, "redstone/clear")

		object Local {
			val front = ref(Ns.LIB, "redstone/local/front")
			val back = ref(Ns.LIB, "redstone/local/back")
			val left = ref(Ns.LIB, "redstone/local/left")
			val right = ref(Ns.LIB, "redstone/local/right")
			val up = ref(Ns.LIB, "redstone/local/up")
			val down = ref(Ns.LIB, "redstone/local/down")
		}
	}

	/** Container helpers. `insert` destroys overflow: prefer [insertOrDrop] or [moveSlot]. */
	object Inventory {
		val moveSlot = ref(Ns.LIB, "inventory/move_slot")
		val insertOrDrop = ref(Ns.LIB, "inventory/insert_or_drop")
		val insert = ref(Ns.LIB, "inventory/insert")
		val remove = ref(Ns.LIB, "inventory/remove")
		val findFreeSlot = ref(Ns.LIB, "inventory/find_free_slot")
		val hasFreeSlot = ref(Ns.LIB, "inventory/has_free_slot")
		val canAcceptOne = ref(Ns.LIB, "inventory/can_accept_one")
		val consumeEntry0 = ref(Ns.LIB, "inventory/consume_entry0")
		val consumeEntry0Count = ref(Ns.LIB, "inventory/consume_entry0_count")
		val containerSize = ref(Ns.LIB, "inventory/container_size")
		val clear = ref(Ns.LIB, "inventory/clear")
	}

	/** Shared network engine for fluids, gases, items and electricity. */
	object Transport {
		val tick = ref(Ns.LIB, "transport/tick")
		val markDirty = ref(Ns.LIB, "transport/mark_dirty")
		val updateConnectionStatus = ref(Ns.LIB, "transport/update_connection_status")

		object Net {
			val join = ref(Ns.LIB, "transport/net/join")
			val rejoin = ref(Ns.LIB, "transport/net/rejoin")
			val leave = ref(Ns.LIB, "transport/net/leave")
			val offer = ref(Ns.LIB, "transport/net/offer")
			val take = ref(Ns.LIB, "transport/net/take")
			val takeAny = ref(Ns.LIB, "transport/net/take_any")
			val read = ref(Ns.LIB, "transport/net/read")
		}
	}

	/** Draw one block's look over another block's mechanics (see "Block Skins"). */
	object Skin {
		val apply = ref(Ns.LIB, "skin/apply")
		val applyStatic = ref(Ns.LIB, "skin/apply_static")
		val applyLit = ref(Ns.LIB, "skin/apply_lit")
		val setLit = ref(Ns.LIB, "skin/set_lit")
		val clear = ref(Ns.LIB, "skin/clear")
	}

	/** Typed player input sessions (trigger + writable book backends). */
	object Input {
		val tick = ref(Ns.LIB, "input/tick")
		val openNumber = ref(Ns.LIB, "input/open_number")
		val openText = ref(Ns.LIB, "input/open_text")
		val openChoice = ref(Ns.LIB, "input/open_choice")
		val poll = ref(Ns.LIB, "input/poll")
		val consume = ref(Ns.LIB, "input/consume")
		val cancel = ref(Ns.LIB, "input/cancel")
		val sessionCreate = ref(Ns.LIB, "input/session/create")
		val routerOpen = ref(Ns.LIB, "input/router/open")
		val routerSelectBackend = ref(Ns.LIB, "input/router/select_backend")
	}

	object Util {
		/** Macro `{name, default, min}`: reads numeric `data.properties.<name>` into `#prop ra.temp`. Hot path, keep it tiny. */
		val property = ref(Ns.LIB, "util/property")
	}
}

/** `ra_lib_multiblock`: assembly, validation and disassembly of multiblock structures. */
object MultiblockLib {
	val init = ref(Ns.LIB_MULTIBLOCK, "init")
	val register = ref(Ns.LIB_MULTIBLOCK, "register")
	val tryAssemble = ref(Ns.LIB_MULTIBLOCK, "try_assemble")
	val tryTier = ref(Ns.LIB_MULTIBLOCK, "try_tier")
	val validateAll = ref(Ns.LIB_MULTIBLOCK, "validate_all")
	val disassemble = ref(Ns.LIB_MULTIBLOCK, "disassemble")

	object Io {
		val at = ref(Ns.LIB_MULTIBLOCK, "io/at")
		val isBlock = ref(Ns.LIB_MULTIBLOCK, "io/is_block")
	}

	/** Function tags other modules add hooks to, as `ns:path` of `#ns:path`. */
	object Hooks {
		const val LOAD = "load"
		const val TICK = "tick"
		const val VALIDATE = "validate"
		const val SETUP_TYPE = "setup_type"
		const val CHECK_STRUCTURE = "check_structure"
		const val ON_BREAK = "on_break"
	}
}
