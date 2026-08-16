# /ra_lib:inventory/find_free_slot/step
# Internal: test one slot index, then move on.

execute if score #inv_slot ra.temp matches 0.. run return 0
execute if score #inv_probe ra.temp >= #inv_max ra.temp run return 0

execute store result storage ra:inventory slotq.n int 1 run scoreboard players get #inv_probe ra.temp
function ra_lib:inventory/find_free_slot/test with storage ra:inventory slotq

execute if score #inv_slot ra.temp matches 0.. run return 0
scoreboard players add #inv_probe ra.temp 1
function ra_lib:inventory/find_free_slot/step
