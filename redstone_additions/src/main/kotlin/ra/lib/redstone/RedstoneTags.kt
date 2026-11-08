package ra.lib.redstone

import io.github.ayfri.kore.DataPack
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.tag
import io.github.ayfri.kore.features.tags.blockTag
import io.github.ayfri.kore.generated.Blocks
import io.github.ayfri.kore.generated.Tags
import ra.api.RaLib
import ra.core.Obj
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.lib.dummyObjectives
import ra.lib.lib

/**
 * `ra_lib:redstone/init`, `clear`, `tags` and the source block tags. Every redstone source the
 * readers know is a block-tag entry: adding one is a tag edit, never a code change.
 */
internal object RedstoneTags {
	val init = lib("redstone/init")

	/** Internal: apply the ra.powered.* direction tags. Called by detect_local only. */
	val tags = lib("redstone/tags")

	/** `#ra_lib:redstone/...` block tag names, as written into block tests. */
	const val ANALOG_OMNI = "#ra_lib:redstone/analog_omni"
	const val ANALOG_SOURCES = "#ra_lib:redstone/analog_sources"
	const val BINARY_SOURCES = "#ra_lib:redstone/binary_sources"
	const val DIRECTIONAL_SOURCES = "#ra_lib:redstone/directional_sources"

	/** Per-source tags the old detect set and nothing ever read; still swept by clear. */
	private val legacySourceTags = listOf("repeater", "comparator", "dust", "block", "lever", "button", "torch")

	/** World then look-space direction tags, in the order tags / clear handle them. */
	private val directionTags = listOf(Side.NORTH, Side.SOUTH, Side.EAST, Side.WEST, Side.UP, Side.DOWN).map { it.id to it.objective } +
		LocalSide.entries.map { it.tagName to it.objective }

	fun Ra.registerRedstoneTags() {
		pack.blockTags()

		// Setup redstone detection scoreboards.
		pack.define(init) {
			dummyObjectives(
				Obj.POWER, Obj.RS_INPUTS,
				Obj.POWER_NORTH, Obj.POWER_SOUTH, Obj.POWER_EAST, Obj.POWER_WEST, Obj.POWER_UP, Obj.POWER_DOWN,
				Obj.POWER_FRONT, Obj.POWER_BACK, Obj.POWER_LEFT, Obj.POWER_RIGHT, Obj.POWER_LOCAL_UP, Obj.POWER_LOCAL_DOWN,
			)
		}

		// Only reached when ra.power is non-zero: ra.power is the maximum over the six world
		// directions and the look-space scores are copies of those, so a zero aggregate guarantees
		// every individual direction is zero too.
		//
		// ra.powered and ra.powered.strong are NOT set here -- ra_lib:redstone/detect owns those
		// two, so a block that calls detect on its own still gets them.
		//
		// The per-source tags this used to also set (ra.powered.dust, .lever, .block and the rest)
		// are gone. Nothing in the pack read them, and the per-side reader detect is now built on
		// reports a level rather than which source produced it.
		pack.define(tags) {
			for ((name, objective) in directionTags) execute {
				ifCondition { score(self(), objective) greaterThanOrEqualTo 1 }
				run { tag(self()) { add("ra.powered.$name") } }
			}
		}

		// Clear every redstone tag this library has ever set. As the marker.
		//
		// Two callers. ra_lib:redstone/detect_local runs it each pass, because direction tags have
		// to be dropped before they are recomputed. ra:load runs it once over every custom block,
		// to sweep up the per-source tags -- ra.powered.dust, .lever, .block and the rest -- that
		// the old detect used to set and nothing ever read. They are no longer produced, so without
		// that sweep a block that happened to be powered at the moment of the upgrade would carry
		// them forever.
		//
		// ra_lib:redstone/detect deliberately does NOT call this: it owns exactly two tags and
		// removes those two itself, which is most of why it now costs a third of what it used to.
		pack.define(RaLib.Redstone.clear) {
			tag(self()) { remove("ra.powered") }
			tag(self()) { remove("ra.powered.strong") }
			for (t in legacySourceTags) tag(self()) { remove("ra.powered.$t") }
			for ((name, _) in directionTags) tag(self()) { remove("ra.powered.$name") }
		}
	}

	private fun DataPack.blockTags() {
		val ns = Ns.LIB.id

		// Analog sources that power every neighbour regardless of orientation.
		blockTag("redstone/analog_omni", ns) {
			add(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)
			add(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
			add(Blocks.DAYLIGHT_DETECTOR)
		}

		// Every block whose `power` state is the level it hands to its neighbours. Anything else
		// with a `power` state can join and needs no code change in redstone/analog.
		blockTag("redstone/analog_sources", ns) {
			add(ANALOG_OMNI)
			add(Blocks.REDSTONE_WIRE)
		}

		// Everything with a `powered` flag that drives all its neighbours at full strength when set.
		blockTag("redstone/binary_sources", ns) {
			add(Blocks.LEVER)
			add(Tags.Block.BUTTONS)
			add(Tags.Block.WOODEN_PRESSURE_PLATES)
			add(Blocks.STONE_PRESSURE_PLATE)
			add(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE)
			add(Blocks.TRIPWIRE_HOOK)
			add(Blocks.LIGHTNING_ROD)
		}

		// Sources that drive one direction only, and strongly. They output towards the OPPOSITE of
		// `facing` (a repeater's and comparator's `facing` points at their input, an observer's at
		// the block it watches); `powered` is whether they are on.
		blockTag("redstone/directional_sources", ns) {
			add(Blocks.REPEATER)
			add(Blocks.COMPARATOR)
			add(Blocks.OBSERVER)
		}
	}
}
