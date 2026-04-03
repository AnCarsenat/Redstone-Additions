# /ra_interactive:blocks/item_pipe/transfer
# Transfer item from pipe to container in front.
# Positioned at ^ ^ ^1 (container location). Pipe is at ^ ^ ^-1.
# Input: storage ra:temp pipe_item = item to transfer
# Output: returns 1 if successful

execute unless block ~ ~ ~ #ra_lib:containers run return 0

# Avoid triggering vanilla dispensers by only allowing dispenser targets that are RA custom blocks.
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] unless entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] run return 0

# If destination cannot accept one item, keep items in current pipe.
data modify storage ra:temp inv_item set from storage ra:temp pipe_item
data modify storage ra:temp inv_item.count set value 1
execute unless data storage ra:temp inv_item.components run data modify storage ra:temp inv_item.components set value {}
execute run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #inv_can_insert ra.temp matches 0 run return 0

# Initialize loop state
tag @s remove ra.pipe_moved_any

# Move stack one-by-one until blocked or source empty.
function ra_interactive:blocks/item_pipe/transfer_loop

# If nothing moved, transfer failed
execute unless entity @s[tag=ra.pipe_moved_any] run return 0
tag @s remove ra.pipe_moved_any

# If destination is another item pipe, process it immediately for same-tick propagation.
execute if block ~ ~ ~ minecraft:dispenser as @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] run tag @s remove ra.pipe_chain_visited
execute if block ~ ~ ~ minecraft:dispenser as @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] at @s run function ra_interactive:blocks/item_pipe/process
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] as @e[type=armor_stand,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] run tag @s remove ra.pipe_chain_visited
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] as @e[type=armor_stand,tag=ra.custom_block.item_pipe,distance=..0.75,limit=1,sort=nearest] at @s run function ra_interactive:blocks/item_pipe/process
return 1
