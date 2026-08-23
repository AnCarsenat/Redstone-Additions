# /ra_lib:transport/rebuild/snapshot_next
# Internal: park the head of the network's media list on the root node, then the
# rest. Context: as the root marker, with storage ra:transport snapq holding the
# network id and what is left of the list.

execute unless data storage ra:transport snapq.queue[0] run return 0

data modify storage ra:transport snapq.m set from storage ra:transport snapq.queue[0].m
data remove storage ra:transport snapq.queue[0]

function ra_lib:transport/rebuild/snapshot_one with storage ra:transport snapq
function ra_lib:transport/rebuild/snapshot_next
