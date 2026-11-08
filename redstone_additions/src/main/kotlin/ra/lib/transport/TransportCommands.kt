package ra.lib.transport

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.generated.EntityTypes
import ra.core.Storage
import ra.lib.fake

/** Shared names of the transport engine. Everything it keeps lives in `storage ra:transport`. */
internal val TR = Storage.TRANSPORT

/** A scratch fake player on `ra.tr.tmp` (`#net_want`, `#next_net`...). */
internal fun tr(name: String) = fake(name)

/** Tags the engine puts on node markers. */
internal object TrTag {
	const val NODE = "ra.tr.node"
	const val ROOT = "ra.tr.root"
	const val FRONTIER = "ra.tr.frontier"
	const val WAVE = "ra.tr.wave"

	/** Rebuild only: a node of a network this rebuild is allowed to take apart and put back. */
	const val SCOPE = "ra.tr.scope"

	/** Rebuild only: a node given a network during this rebuild, whose capacity and carry still have to be added. */
	const val CLAIMED = "ra.tr.claimed"

	/** Rebuild only: the root of a network created from nothing in this rebuild, the only kind the clamp may trim. */
	const val FRESH = "ra.tr.fresh"
}

/** `@e[type=marker,tag=<tag>]`, optionally narrowed further. */
internal fun trMarkers(tag: String, extra: io.github.ayfri.kore.arguments.selector.SelectorArguments.() -> Unit = {}) =
	allEntities {
		type = EntityTypes.MARKER
		this.tag = tag
		extra()
	}
