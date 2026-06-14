# /ra_interactive:blocks/item_mover/check_output_capacity
# Check if output side can accept at least one selected item.
# Context: positioned at primary output container
# Input: storage ra:temp mover_item
# Output: #mover_can_forward ra.temp

scoreboard players set #mover_can_forward ra.temp 0

# Never treat vanilla dispensers as valid outputs.
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] unless entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] run return 0

data modify storage ra:temp inv_item set from storage ra:temp mover_item
data modify storage ra:temp inv_item.count set value 1
execute unless data storage ra:temp inv_item.components run data modify storage ra:temp inv_item.components set value {}

# Primary output
execute run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

# If primary is full, try connected chest partner
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

# Trapped chest partner variants
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #mover_can_forward ra.temp matches 0 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 if score #inv_can_insert ra.temp matches 1.. run scoreboard players set #mover_can_forward ra.temp 1

return run scoreboard players get #mover_can_forward ra.temp
