# /ra:tools/multimeter/media_next
# Internal: print the head of the media list, then the rest.
#
# The list is walked rather than the `amounts` compound because a compound's keys
# cannot be enumerated in a function, and because the list order is the order the
# media arrived in -- the same order everything else in the pack reports them.

execute unless data storage ra:temp meterq.queue[0] run return 0

data modify storage ra:temp meterq.m set from storage ra:temp meterq.queue[0].m
data remove storage ra:temp meterq.queue[0]

function ra:tools/multimeter/media_one with storage ra:temp meterq
function ra:tools/multimeter/media_next
