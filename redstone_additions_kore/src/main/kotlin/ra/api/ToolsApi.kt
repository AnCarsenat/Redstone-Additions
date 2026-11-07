package ra.api

import ra.core.Ns
import ra.core.ref

/**
 * Entry points of the `ra` namespace (tools) and `ra_settings` that modules call.
 * See [RaLib] for how these reference objects are meant to be used.
 */
object Goggles {
	/**
	 * Billboard line writers, called from a block's `goggles` function as the marker.
	 * `prop_line` reads `data.properties`, `data_line` reads `data.status`; the `stacked_*`
	 * variants read properties / status / `data.data` respectively and lay lines out from
	 * [stackReset]. Asking the wrong one renders "N/A".
	 */
	object Billboard {
		val handleBillboard = ref(Ns.RA, "tools/goggles/billboard/handle_billboard")
		val initOffsets = ref(Ns.RA, "tools/goggles/billboard/init_offsets")
		val propLine = ref(Ns.RA, "tools/goggles/billboard/prop_line")
		val dataLine = ref(Ns.RA, "tools/goggles/billboard/data_line")
		val textLine = ref(Ns.RA, "tools/goggles/billboard/text_line")
		val itemLine = ref(Ns.RA, "tools/goggles/billboard/item_line")
		val showIoLine = ref(Ns.RA, "tools/goggles/billboard/show_io_line")
		val stackReset = ref(Ns.RA, "tools/goggles/billboard/stack_reset")
		val stackedPropLine = ref(Ns.RA, "tools/goggles/billboard/stacked_prop_line")
		val stackedStatusLine = ref(Ns.RA, "tools/goggles/billboard/stacked_status_line")
		val stackedDataLine = ref(Ns.RA, "tools/goggles/billboard/stacked_data_line")
		val stackedTextLine = ref(Ns.RA, "tools/goggles/billboard/stacked_text_line")
	}

	object Status {
		val readStatusMultiblock = ref(Ns.RA, "tools/goggles/status/read_status_multiblock")
	}
}

/** `ra_settings`: pack-wide (global) and per-player (user) settings. Reads must fail open. */
object Settings {
	val init = ref(Ns.SETTINGS, "init")
	val tick = ref(Ns.SETTINGS, "tick")
	val inputTick = ref(Ns.SETTINGS, "input_tick")
	val uninstall = ref(Ns.SETTINGS, "uninstall")

	/** Macro `{key, default}` -> returns the global value. The default is required. */
	val get = ref(Ns.SETTINGS, "get")
	/** Macro `{block, prop, default}`. */
	val prop = ref(Ns.SETTINGS, "prop")
	/** Macro `{obj, default}`: the executing player's user setting. */
	val user = ref(Ns.SETTINGS, "user")
	/** Macro `{block}`: is this block type enabled. */
	val enabled = ref(Ns.SETTINGS, "enabled")

	object Admin {
		val show = ref(Ns.SETTINGS, "admin/show")
	}

	object Placement {
		val check = ref(Ns.SETTINGS, "placement/check")
		val refuse = ref(Ns.SETTINGS, "placement/refuse")
		val seed = ref(Ns.SETTINGS, "placement/seed")
	}
}

/** Cross-module entry points of gameplay modules. */
object ModulesApi {
	object Wires {
		/** Electricity network offer / take, used by the Ender power vault. */
		val offerEu = ref(Ns.WIRES, "electric/offer_eu")
		val takeEu = ref(Ns.WIRES, "electric/take_eu")
	}

	object Ender {
		val anchorHelp = ref(Ns.ENDER, "tools/anchor/help")
	}

	object Interactive {
		/** Item Mover internals, reused by the Boxer and Unboxer. */
		val itemMoverSelectInput = ref(Ns.INTERACTIVE, "blocks/item_mover/select_input")
		val itemMoverTryInsert = ref(Ns.INTERACTIVE, "blocks/item_mover/try_insert")
		val itemMoverCheckOutputCapacity = ref(Ns.INTERACTIVE, "blocks/item_mover/check_output_capacity")
	}

	object Migrations {
		val run = ref(Ns.MIGRATIONS, "run")
	}
}
