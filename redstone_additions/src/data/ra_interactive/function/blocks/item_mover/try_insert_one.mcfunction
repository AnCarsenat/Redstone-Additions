# /ra_interactive:blocks/item_mover/try_insert_one
# Try to insert exactly one item into the container at current position.
# Output: @s ra.temp = inserted count (0 or 1)

scoreboard players set @s ra.temp 0

# Never insert into vanilla dispensers. Only RA custom blocks are valid dispenser targets.
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] unless entity @e[type=armor_stand,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] run return 0

data modify storage ra:temp mover_item_single set from storage ra:temp mover_item
data modify storage ra:temp mover_item_single.count set value 1
execute unless data storage ra:temp mover_item_single.components run data modify storage ra:temp mover_item_single.components set value {}

data modify storage ra:temp inv_item set from storage ra:temp mover_item_single
execute run function ra_lib:inventory/can_accept_one with storage ra:temp inv_item
execute if score #inv_can_insert ra.temp matches 1.. store result score @s ra.temp run function ra_lib:inventory/insert with storage ra:temp mover_item_single
