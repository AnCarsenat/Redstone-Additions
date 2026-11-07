package ra.tools.goggles

import ra.core.Ns
import ra.core.ref

/**
 * Goggles entry points that are not in `ra.api.Goggles` (the foundation's API names only the
 * billboard writers block readouts call). Defined by this package; another part that needs one
 * of these (Hub's `ra:tick` calls [tick], `ra_multiblock` readouts call the I/O indicators) can
 * use these values or keep its own ref to the same path.
 */
object GogglesRefs {
	/** Called every tick from ra:tick. */
	val tick = ref(Ns.RA, "tools/goggles/tick")
	val give = ref(Ns.RA, "tools/goggles/give")
	val scanRange = ref(Ns.RA, "tools/goggles/scan_range")

	/** Built from `ra.goggles`: runs the readout of whichever block this marker is. */
	val drawBlock = ref(Ns.RA, "tools/goggles/draw_block")

	/** Built from `ra.multiblockGoggles`. */
	val drawMultiblock = ref(Ns.RA, "tools/goggles/draw_multiblock")

	/** Name of the block this marker is, into `storage ra:temp block_name` (via [drawBlock]). */
	val blockName = ref(Ns.RA, "tools/block_name")

	/** Billboard writers beside those in `ra.api.Goggles.Billboard`. */
	object Billboard {
		/** Macro `{name}`, as a multiblock marker: name billboard + I/O indicators. */
		val handleMultiblock = ref(Ns.RA, "tools/goggles/billboard/handle_multiblock")
		val showInput = ref(Ns.RA, "tools/goggles/billboard/show_input")
		val showOutput = ref(Ns.RA, "tools/goggles/billboard/show_output")
		val showControl = ref(Ns.RA, "tools/goggles/billboard/show_control")
		val showLiteralLine = ref(Ns.RA, "tools/goggles/billboard/show_literal_line")
		val renderName = ref(Ns.RA, "tools/goggles/billboard/render_name")
		val renderLiteralLine = ref(Ns.RA, "tools/goggles/billboard/render_literal_line")
		val renderIoLine = ref(Ns.RA, "tools/goggles/billboard/render_io_line")
		val stackNext = ref(Ns.RA, "tools/goggles/billboard/stack_next")
	}
}
