# /ra_lib:inventory/find_free_slot
# Index of an empty slot in the container at the current position.
# Output: #inv_slot ra.temp, or -1 when the container is full.
#
# Items is a list of the stacks that exist, each carrying its own Slot number, so
# the list index is not the slot index and a container can be non-contiguous.
# The number of stacks present is tried first because containers usually do fill
# from slot 0 upward, which makes the common case a single check; only when that
# guess is taken does the linear scan run.

function ra_lib:inventory/container_size

scoreboard players set #inv_len ra.temp 0
execute if data block ~ ~ ~ Items store result score #inv_len ra.temp run data get block ~ ~ ~ Items

scoreboard players set #inv_slot ra.temp -1
execute if score #inv_len ra.temp >= #inv_max ra.temp run return -1

# Fast guess.
scoreboard players operation #inv_probe ra.temp = #inv_len ra.temp
execute store result storage ra:inventory slotq.n int 1 run scoreboard players get #inv_probe ra.temp
function ra_lib:inventory/find_free_slot/test with storage ra:inventory slotq
execute if score #inv_slot ra.temp matches 0.. run return run scoreboard players get #inv_slot ra.temp

# Otherwise walk the slots.
scoreboard players set #inv_probe ra.temp 0
function ra_lib:inventory/find_free_slot/step

data remove storage ra:inventory slotq
return run scoreboard players get #inv_slot ra.temp
