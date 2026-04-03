# /ra_lib:inventory/can_accept_one
# Check if container at current position can accept one ra:temp inv_item.
# Input: storage ra:temp inv_item
# Output: #inv_can_insert ra.temp (1=yes, 0=no)

scoreboard players set #inv_can_insert ra.temp 0
scoreboard players set #inv_len ra.temp 0
scoreboard players set #inv_max ra.temp 27
scoreboard players set #inv_comp_len ra.temp 0

# Capacity by container type
execute if block ~ ~ ~ minecraft:hopper run scoreboard players set #inv_max ra.temp 5
execute if block ~ ~ ~ minecraft:dropper run scoreboard players set #inv_max ra.temp 9
execute if block ~ ~ ~ minecraft:dispenser run scoreboard players set #inv_max ra.temp 9

# Free entry slot means we can insert
execute if data block ~ ~ ~ Items store result score #inv_len ra.temp run data get block ~ ~ ~ Items
execute if score #inv_len ra.temp < #inv_max ra.temp run scoreboard players set #inv_can_insert ra.temp 1

# If no free entries, allow stacking onto compatible partial stacks
execute if score #inv_can_insert ra.temp matches 0 if data storage ra:temp inv_item.components store result score #inv_comp_len ra.temp run data get storage ra:temp inv_item.components
execute if score #inv_can_insert ra.temp matches 0 if score #inv_comp_len ra.temp matches 1.. run function ra_lib:inventory/can_accept_components with storage ra:temp inv_item
execute if score #inv_can_insert ra.temp matches 0 if score #inv_comp_len ra.temp matches 0 run function ra_lib:inventory/can_accept_id_only with storage ra:temp inv_item

return run scoreboard players get #inv_can_insert ra.temp
