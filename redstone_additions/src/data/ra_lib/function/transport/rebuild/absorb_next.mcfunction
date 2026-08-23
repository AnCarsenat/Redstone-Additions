# /ra_lib:transport/rebuild/absorb_next
# Internal: fold the head of the carried list into the network, then the rest.
# Context: as the carrying node, with storage ra:transport absq holding the
# network id and what is left of the list.

execute unless data storage ra:transport absq.queue[0] run return 0

data modify storage ra:transport absq.m set from storage ra:transport absq.queue[0].m
execute store result score #abs.a ra.tr.tmp run data get storage ra:transport absq.queue[0].a
data remove storage ra:transport absq.queue[0]

function ra_lib:transport/rebuild/absorb_one with storage ra:transport absq
function ra_lib:transport/rebuild/absorb_next
