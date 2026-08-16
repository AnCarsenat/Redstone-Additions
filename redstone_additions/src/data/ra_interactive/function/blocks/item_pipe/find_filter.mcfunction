# /ra_interactive:blocks/item_pipe/find_filter
# Locate the item frame acting as this pipe's filter.
# Context: as the pipe marker, at the pipe position.
# Output: storage ra:temp filter_item, absent when there is no filter.
#
# A frame attached to a block does not sit inside it — it occupies the
# neighbouring block, a full block away from the pipe's centre. The old check
# looked for frames within 0.75, which is shorter than that distance, and simply
# widening the radius does not help: at 1.0 a frame attached to some *other*
# nearby block is exactly as close as one attached to this pipe, so the radius
# cannot tell whose filter it is.
#
# Since 1.21.5 hanging entities record the block they are attached to in a single
# `block_pos` field (it replaced TileX/TileY/TileZ). Matching on that is exact:
# only frames actually stuck to this pipe are considered, on any of its six faces.

data remove storage ra:temp filter_item

# Block coordinates of this pipe. Markers sit at the block centre, so reading Pos
# scaled by 2 gives 2*block+1 exactly — an integer, with no truncation error at
# negative coordinates the way a plain `data get Pos` would have.
execute store result score #fx ra.temp run data get entity @s Pos[0] 2
execute store result score #fy ra.temp run data get entity @s Pos[1] 2
execute store result score #fz ra.temp run data get entity @s Pos[2] 2
scoreboard players remove #fx ra.temp 1
scoreboard players remove #fy ra.temp 1
scoreboard players remove #fz ra.temp 1
scoreboard players set #two ra.temp 2
scoreboard players operation #fx ra.temp /= #two ra.temp
scoreboard players operation #fy ra.temp /= #two ra.temp
scoreboard players operation #fz ra.temp /= #two ra.temp

execute as @e[type=item_frame,distance=..1.6] run function ra_interactive:blocks/item_pipe/take_filter
execute as @e[type=glow_item_frame,distance=..1.6] run function ra_interactive:blocks/item_pipe/take_filter
