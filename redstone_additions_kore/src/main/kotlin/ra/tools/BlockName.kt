package ra.tools

import io.github.ayfri.kore.commands.function
import ra.api.Goggles
import ra.core.Ra
import ra.core.define
import ra.core.raw

/**
 * `ra:tools/block_name`: the display name of the custom block this marker is, into
 * `storage ra:temp block_name` ("Unknown Block" when nothing claims it). As the block's marker.
 *
 * Reuses the goggles dispatch rather than keeping its own table: the two Data Handlers each had a
 * hand-written tag-to-name list, they had drifted to 22 and 38 entries, and a block missing from one
 * showed as "Unknown Block" there while the other named it correctly.
 */
internal fun Ra.registerBlockName() = pack.define(Goggles.blockName) {
	raw("data remove storage ra:temp block_name")
	raw("data modify storage ra:temp name_only set value 1b")
	function(Goggles.drawBlock)
	raw("data remove storage ra:temp name_only")
	raw("execute unless data storage ra:temp block_name run data modify storage ra:temp block_name set value \"Unknown Block\"")
}
