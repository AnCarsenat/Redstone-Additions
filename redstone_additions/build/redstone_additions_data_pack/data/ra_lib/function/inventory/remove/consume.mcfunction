# /ra_lib:inventory/remove/consume
# Internal helper for ra_lib:inventory/remove.
# Take as much as is still outstanding from the current entry. The entry is only
# carried over to the output list when something is left of it, so emptied stacks
# free their slot instead of lingering as count:0.

execute store result score #inv_entry ra.temp run data get storage ra:inventory scan[0].count

# Stack is no larger than the outstanding amount: it is consumed whole.
execute if score #inv_entry ra.temp <= #inv_need ra.temp run scoreboard players operation #inv_need ra.temp -= #inv_entry ra.temp
execute if score #inv_entry ra.temp <= #inv_need ra.temp run return 0

# Stack covers the remainder: shrink it and keep what is left.
scoreboard players operation #inv_entry ra.temp -= #inv_need ra.temp
scoreboard players set #inv_need ra.temp 0
execute store result storage ra:inventory scan[0].count int 1 run scoreboard players get #inv_entry ra.temp
data modify storage ra:inventory out append from storage ra:inventory scan[0]
