# /ra_lib_multiblock:generic/validate_next
# Internal: try each registered type until one claims the assembly.

execute if score #mb_result ra.multiblock matches 1 run return 1
execute unless data storage ra:multiblock scan_q[0] run return 0

data modify storage ra:multiblock scan.type set from storage ra:multiblock scan_q[0]
data remove storage ra:multiblock scan_q[0]

function ra_lib_multiblock:generic/validate_type with storage ra:multiblock scan
function ra_lib_multiblock:generic/validate_next
