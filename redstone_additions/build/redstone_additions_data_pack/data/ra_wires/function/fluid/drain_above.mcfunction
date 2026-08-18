# /ra_wires:fluid/drain_above
# Empty one filled container out of the block sitting on top of the drain.
# Context: as the drain marker, at the drain position.
#
# One container per cycle, so the drain's cooldown still governs throughput —
# emptying a whole barrel of buckets in a single tick would make the setting
# meaningless.

execute unless data block ~ ~1 ~ Items[0] run return 0

data remove storage ra:wires iq
data modify storage ra:wires iq.queue set from storage ra:wires item_sources
function ra_wires:fluid/drain_above_next
