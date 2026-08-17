# /ra:tools/block_name
# Resolve the display name of the custom block this marker is.
# Context: as the block's marker.
# Output: storage ra:temp block_name, "Unknown Block" when nothing claims it.
#
# Reuses the goggles dispatch rather than keeping its own table. The two Data
# Handlers each had a hand-written tag-to-name list; they had drifted to 22 and
# 38 entries, so a block missing from one showed as "Unknown Block" there while
# the other named it correctly.

data remove storage ra:temp block_name

data modify storage ra:temp name_only set value 1b
function ra:tools/goggles/draw_block
data remove storage ra:temp name_only

execute unless data storage ra:temp block_name run data modify storage ra:temp block_name set value "Unknown Block"
