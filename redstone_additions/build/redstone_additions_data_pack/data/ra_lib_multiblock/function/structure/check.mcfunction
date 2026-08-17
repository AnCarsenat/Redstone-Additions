# /ra_lib_multiblock:structure/check {type:"...",facing:"..."}
# Check every block a registered structure requires, in one facing.
# Context: at the base block position (or as a marker sitting on it).
# Returns 1 when the whole pattern matches, 0 on the first mismatch.
#
# This replaces the per-multiblock check_blocks macro: the pattern comes from the
# registered spec, so there is nothing to keep in sync by hand.

$data modify storage ra:multiblock chk_q set from storage ra:multiblock types.$(type).facings.$(facing).blocks

scoreboard players set #mb_ok ra.multiblock 1
function ra_lib_multiblock:structure/check_next

data remove storage ra:multiblock chk_q
data remove storage ra:multiblock chk_cur

return run scoreboard players get #mb_ok ra.multiblock
