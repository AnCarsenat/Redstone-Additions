# /ra_lib_multiblock:try_tier {tier:"copper"}
# Try to assemble any registered structure belonging to a base tier, at the
# current position. Returns 1 on the first structure that assembles, else 0.
#
# This is what lets a new multiblock be added without touching the wrench: the
# tier functions ask the registry instead of naming one hardcoded type each.

scoreboard players set #mb_tier_done ra.temp 0

data modify storage ra:multiblock tier_q set from storage ra:multiblock registry
$data modify storage ra:multiblock tier_scan.tier set value "$(tier)"

function ra_lib_multiblock:tier_next

data remove storage ra:multiblock tier_q
data remove storage ra:multiblock tier_scan

return run scoreboard players get #mb_tier_done ra.temp
