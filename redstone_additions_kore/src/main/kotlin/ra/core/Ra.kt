package ra.core

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import net.benwoodworth.knbt.NbtCompound
import ra.core.block.CustomBlock

/**
 * One part of the pack: a library, a tool set, or a gameplay module.
 *
 * Parts never edit each other's files. Anything a part must tell the rest of the pack about
 * (a placeable block, a goggles readout, a wrench cycle) goes through [Ra], and the part that
 * owns the shared function (placement router, goggles dispatch, wrench registry...) builds it
 * from what was registered. Adding a block therefore touches the block's own files and nothing
 * shared - the opposite of the beet version's nine-place checklist.
 */
interface PackPart {
	/** Declares this part's functions and resources, and registers what it shares. */
	fun Ra.register()
}

/**
 * A gameplay module (`ra_gates`, `ra_wires`...). `ra:load` and `ra:tick` call [load] and [tick]
 * of every module, in [ra.RedstoneAdditions.modules] order.
 */
interface GameplayModule : PackPart {
	val ns: Ns

	/** Human name, used in the load message and the give bundle. */
	val title: String

	val load: FunctionArgument get() = ref(ns, "load")
	val tick: FunctionArgument? get() = ref(ns, "tick")
}

/** A tag test plus the function it routes to: `execute if entity @s[tag=<tag>] run function <fn>`. */
data class TagRoute(val tag: String, val fn: FunctionArgument)

/** One row of the wrench's cycle menu for a block type. See ra:tools/wrench. */
data class WrenchCycle(val label: String, val prop: String, val fn: FunctionArgument)

/**
 * A give bundle in `/function ra:give_all_items`: a `minecraft:bundle` prefilled with [items].
 * [id] names the function `ra:items/bundles/give_<id>_bundle`.
 */
data class GiveBundle(val id: String, val title: String, val lore: String, val ns: Ns, val items: List<NbtCompound>)

/**
 * The registry shared by every [PackPart] while the pack is being built.
 *
 * Registration order is preserved everywhere, because several of these become dispatch
 * functions whose order is observable (tick order, the order bundles are handed out).
 */
class Ra(val pack: DataPack) {
	/**
	 * Every placeable block, in registration order. Deliberately not called `blocks`: inside a
	 * part's `fun Ra.register()`, a module's own `blocks` list would silently resolve to this
	 * one instead (the extension receiver wins over the enclosing object), which once made
	 * `ra_gates:tick` re-run every earlier module's block ticks.
	 */
	val allBlocks = mutableListOf<CustomBlock>()

	/** Function tag `#ra:placement_handlers`, run as the spawned bat, at the block centre. */
	val placementHandlers = mutableListOf<FunctionArgument>()

	/** `ra:tools/goggles/draw_block`: marker tag -> readout. Also answers block names (see [CustomBlock.goggles]). */
	val goggles = mutableListOf<TagRoute>()

	/** `ra:tools/goggles/draw_multiblock`: multiblock marker tag -> readout. */
	val multiblockGoggles = mutableListOf<TagRoute>()

	/** Wrench Shift+RMB cycle rows, keyed by the marker's `data.type`. */
	val wrenchCycles = linkedMapOf<String, MutableList<WrenchCycle>>()

	/** Wrench plain-RMB toggles: marker tag -> toggle function, run as the marker. */
	val wrenchToggles = mutableListOf<TagRoute>()

	/** Properties neither tool may edit, keyed by `data.type`. */
	val readonly = linkedMapOf<String, MutableSet<String>>()

	/** Property names the Data Handlers list, in menu order. */
	val dataHandlerProperties = linkedSetOf<String>()

	/** Bundles for `ra:give_all_items`, in hand-out order. */
	val bundles = mutableListOf<GiveBundle>()

	/**
	 * Work deferred until every part has registered, run in the order added. For a part whose
	 * entry must come AFTER entries other parts append to the same shared file -- e.g.
	 * `ra_lib_multiblock:generic/validate` closes `#ra_lib_multiblock:validate`, after every
	 * structure's own validator, because it only covers types nobody else handled.
	 */
	private val deferred = mutableListOf<() -> Unit>()

	fun afterAllParts(action: () -> Unit) {
		deferred += action
	}

	/** Called once by [ra.RedstoneAdditions] after the last part. */
	internal fun runDeferred() {
		deferred.forEach { it() }
		deferred.clear()
	}

	/** Registers a placeable block, its placement handler and its goggles readout. */
	fun block(block: CustomBlock, placement: Boolean = true, goggles: Boolean = true): CustomBlock {
		allBlocks += block
		if (placement) placementHandlers += block.handlePlacement
		if (goggles) this.goggles += TagRoute(block.tag, block.goggles)
		return block
	}

	fun wrenchCycle(type: String, vararg rows: WrenchCycle) {
		wrenchCycles.getOrPut(type) { mutableListOf() } += rows
	}

	fun readonly(type: String, vararg props: String) {
		readonly.getOrPut(type) { linkedSetOf() } += props
	}

	fun dataHandlerProperties(vararg props: String) {
		dataHandlerProperties += props
	}
}
