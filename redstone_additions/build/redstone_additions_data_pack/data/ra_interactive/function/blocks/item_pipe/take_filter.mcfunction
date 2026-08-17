# /ra_interactive:blocks/item_pipe/take_filter
# Internal: adopt this frame's item as the filter, if it is attached to the pipe.
# Context: as a candidate item frame.

execute if data storage ra:temp filter_item.id run return 0
execute unless data entity @s Item.id run return 0

execute store result score #bx ra.temp run data get entity @s block_pos[0]
execute store result score #by ra.temp run data get entity @s block_pos[1]
execute store result score #bz ra.temp run data get entity @s block_pos[2]

execute unless score #bx ra.temp = #fx ra.temp run return 0
execute unless score #by ra.temp = #fy ra.temp run return 0
execute unless score #bz ra.temp = #fz ra.temp run return 0

data modify storage ra:temp filter_item set from entity @s Item
