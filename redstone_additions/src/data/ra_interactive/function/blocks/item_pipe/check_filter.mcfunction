# /ra_interactive:blocks/item_pipe/check_filter
# Check the pipe's filter against the item about to move. At pipe position.
# Input: storage ra:temp pipe_item = item to check
# Output: returns 1 if the item was sent to a side container
#
# The filter is cached on the marker. Finding it means scanning for item frame
# entities, and doing that on every pipe on every tick was the most expensive
# thing left in the item pipe path once whole-stack moves landed. A frame only
# changes when a player puts one up, takes one down or rotates it, so a rescan
# every 20 ticks is far more often than it needs to be and costs a twentieth as
# much.

scoreboard players remove @s ra.filter_cd 1
execute if score @s ra.filter_cd matches ..0 run function ra_interactive:blocks/item_pipe/refresh_filter

# No filter on this pipe: the item carries on forward.
execute unless data entity @s data.data.filter.id run return 0

data modify storage ra:temp filter_item set from entity @s data.data.filter

# Compare item IDs. `data modify ... set from` reports success only when it
# actually changed something, so success 0 means the two ids were already equal.
data modify storage ra:temp check_id set from storage ra:temp pipe_item.id
execute store success score @s ra.temp run data modify storage ra:temp check_id set from storage ra:temp filter_item.id

# Different item: not ours to divert.
execute if score @s ra.temp matches 1 run return 0

# Match. Try each side except forward.
execute positioned ~ ~-1 ~ if block ~ ~ ~ #ra_lib:containers store result score @s ra.temp run function ra_interactive:blocks/item_pipe/filter_insert {sx:"~ ~1 ~"}
execute if score @s ra.temp matches 1 run return 1
execute positioned ~ ~1 ~ if block ~ ~ ~ #ra_lib:containers store result score @s ra.temp run function ra_interactive:blocks/item_pipe/filter_insert {sx:"~ ~-1 ~"}
execute if score @s ra.temp matches 1 run return 1
execute positioned ^-1 ^ ^ if block ~ ~ ~ #ra_lib:containers store result score @s ra.temp run function ra_interactive:blocks/item_pipe/filter_insert {sx:"^1 ^ ^"}
execute if score @s ra.temp matches 1 run return 1
execute positioned ^1 ^ ^ if block ~ ~ ~ #ra_lib:containers store result score @s ra.temp run function ra_interactive:blocks/item_pipe/filter_insert {sx:"^-1 ^ ^"}
execute if score @s ra.temp matches 1 run return 1
execute positioned ^ ^ ^-1 if block ~ ~ ~ #ra_lib:containers store result score @s ra.temp run function ra_interactive:blocks/item_pipe/filter_insert {sx:"^ ^ ^1"}
execute if score @s ra.temp matches 1 run return 1
return 0
