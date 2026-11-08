package ra.api

import ra.core.Ns
import ra.core.ref

/**
 * Entry points of the `ra` namespace (tools) and `ra_settings` that modules call.
 * See [RaLib] for how these reference objects are meant to be used.
 */
object Goggles {
	/** Called every tick from ra:tick. */
	val tick = ref(Ns.RA, "tools/goggles/tick")
	val give = ref(Ns.RA, "tools/goggles/give")
	val scanRange = ref(Ns.RA, "tools/goggles/scan_range")

	/** Built from `Ra.goggles`: runs the readout of whichever block this marker is. */
	val drawBlock = ref(Ns.RA, "tools/goggles/draw_block")

	/** Built from `Ra.multiblockGoggles`. */
	val drawMultiblock = ref(Ns.RA, "tools/goggles/draw_multiblock")

	/** Name of the block this marker is, into `storage ra:temp block_name` (via [drawBlock]). */
	val blockName = ref(Ns.RA, "tools/block_name")

	/**
	 * Billboard line writers, called from a block's `goggles` function as the marker.
	 * `prop_line` reads `data.properties`, `data_line` reads `data.status`; the `stacked_*`
	 * variants read properties / status / `data.data` respectively. Asking the wrong one renders
	 * "N/A". Every writer appends one line to the billboard being built ([begin] .. [flushHere]),
	 * in call order; their `y` arguments and [stackReset] are accepted and ignored.
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

		/** Macro `{name}`, as a multiblock marker: name billboard + I/O indicators. */
		val handleMultiblock = ref(Ns.RA, "tools/goggles/billboard/handle_multiblock")
		val showInput = ref(Ns.RA, "tools/goggles/billboard/show_input")
		val showOutput = ref(Ns.RA, "tools/goggles/billboard/show_output")
		val showControl = ref(Ns.RA, "tools/goggles/billboard/show_control")
		val showLiteralLine = ref(Ns.RA, "tools/goggles/billboard/show_literal_line")
		val renderName = ref(Ns.RA, "tools/goggles/billboard/render_name")
		val renderLiteralLine = ref(Ns.RA, "tools/goggles/billboard/render_literal_line")
		val renderIoLine = ref(Ns.RA, "tools/goggles/billboard/render_io_line")

		/** Starts an empty billboard (storage `ra:temp gg`, score `#gg.lines`). */
		val begin = ref(Ns.RA, "tools/goggles/billboard/begin")
		/** Appends `storage ra:temp gg.line` (a text component) as the next line. */
		val append = ref(Ns.RA, "tools/goggles/billboard/append")
		/** Summons the billboard built since [begin], its bottom edge here. Nothing if empty. */
		val flushHere = ref(Ns.RA, "tools/goggles/billboard/flush_here")
		/** As the freshly summoned text_display: style and text. */
		val spawnText = ref(Ns.RA, "tools/goggles/billboard/spawn_text")
		/** Macro `{id, y}`: the item of an [itemLine], `y` above here. */
		val spawnItem = ref(Ns.RA, "tools/goggles/billboard/spawn_item")
		/** As the marker: its type's property schema into `storage ra:temp gg.schema`, once per billboard. */
		val loadSchema = ref(Ns.RA, "tools/goggles/billboard/load_schema")
		val loadSchemaAt = ref(Ns.RA, "tools/goggles/billboard/load_schema_at")
	}

	object Status {
		val readStatusMultiblock = ref(Ns.RA, "tools/goggles/status/read_status_multiblock")
	}
}

/** The tools' registries and per-tick entry points, called by the Hub's `ra:load` / `ra:tick`. */
object Tools {
	object Wrench {
		val initRegistry = ref(Ns.RA, "tools/wrench/init_registry")
		val menuAction = ref(Ns.RA, "tools/wrench/menu_action")
	}

	object DataHandler {
		val tick = ref(Ns.RA, "tools/data_handler/tick")
		val initRegistry = ref(Ns.RA, "tools/data_handler/init_registry")
	}

	object Readonly {
		val initRegistry = ref(Ns.RA, "tools/readonly/init_registry")
	}
}

/** Entry points of the Hub (`ra:*` outside `tools/`). */
object Hub {
	/** `/function ra:uninstall`: asks twice, then removes the pack's entities and data. */
	val uninstall = ref(Ns.RA, "uninstall")
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

	/** The admin entity purge (`ra/settings/purge`). */
	object Purge {
		/** Called from ra:load (through ra_settings:init). */
		val init = ref(Ns.SETTINGS, "purge/init")

		/** Called every tick from ra:tick, before the migration pass. One score test while idle. */
		val tick = ref(Ns.SETTINGS, "purge/tick")

		/** Macro `{e}`, as a marker: stamp it with the current purge epoch. */
		val stamp = ref(Ns.SETTINGS, "purge/stamp")

		/**
		 * The line that stamps [selector] (one freshly summoned persistent marker) as born after
		 * the latest purge, so the unloaded-area clean-up never takes it for a leftover. Every
		 * path that summons a marker meant to stay calls this next to its other stamps; it does
		 * nothing in a world that has never been purged.
		 */
		fun stampLine(selector: String) =
			"execute if score #epoch ra.purge matches 1.. as $selector run function ${stamp.asId()} with storage ra:purge cur"
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

		/** ra_interactive's list in `#ra_enchanting:recipes`; ra_enchanting keeps it last in the tag. */
		val enchantRecipes = ref(Ns.INTERACTIVE, "enchant_recipes")
	}

	object Migrations {
		/** From ra:load: the world steps the world has not had yet. */
		val run = ref(Ns.MIGRATIONS, "run")

		/** From ra:tick: the entity steps, as every custom block marker not stamped with this version. */
		val entities = ref(Ns.MIGRATIONS, "entities")
	}
}
