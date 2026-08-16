# /ra_lib_multiblock:structure/check_next
# Internal: test one queued block requirement, then recurse. Stops at the first
# mismatch, so a structure missing its first block costs three commands.

execute if score #mb_ok ra.multiblock matches 0 run return 0
execute unless data storage ra:multiblock chk_q[0] run return 0

data modify storage ra:multiblock chk_cur set from storage ra:multiblock chk_q[0]
data remove storage ra:multiblock chk_q[0]

function ra_lib_multiblock:structure/check_one with storage ra:multiblock chk_cur
function ra_lib_multiblock:structure/check_next
