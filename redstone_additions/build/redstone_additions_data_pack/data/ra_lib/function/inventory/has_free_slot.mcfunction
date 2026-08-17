# /ra_lib:inventory/has_free_slot
# Does the container at the current position have a completely empty slot?
# Returns 1 or 0.

function ra_lib:inventory/container_size

scoreboard players set #inv_len ra.temp 0
execute if data block ~ ~ ~ Items store result score #inv_len ra.temp run data get block ~ ~ ~ Items

execute if score #inv_len ra.temp < #inv_max ra.temp run return 1
return 0
