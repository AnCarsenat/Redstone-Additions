# /ra_lib_multiblock:tier_next
# Internal: walk the registry, stopping at the first structure that assembles.

execute if score #mb_tier_done ra.temp matches 1 run return 1
execute unless data storage ra:multiblock tier_q[0] run return 0

data modify storage ra:multiblock tier_scan.type set from storage ra:multiblock tier_q[0]
data remove storage ra:multiblock tier_q[0]

function ra_lib_multiblock:tier_try with storage ra:multiblock tier_scan
function ra_lib_multiblock:tier_next
