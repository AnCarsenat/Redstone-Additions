# /ra_interactive:blocks/item_pipe/check_filter
# Check the pipe's filter against the item about to move. At pipe position.
# Input: storage ra:temp pipe_item = item to check
# Output: returns 1 if the item was sent to a side container
#
# THE FILTER IS A PROPERTY NOW, NOT AN ITEM FRAME
# It used to be an item frame stuck to the pipe. Reading it meant selecting every
# item_frame and glow_item_frame within 1.6 blocks of every pipe, then comparing
# each one's `block_pos` against the pipe's own coordinates -- an entity selector
# and three score comparisons per candidate, per pipe. That was expensive enough
# that it had to be cached and rescanned only every 20 ticks, which in turn meant
# a frame you had just put up did nothing for up to a second.
#
# The filter is now `filter_item`, an item id on the pipe's own properties, set
# with the Data Handler's [Set from hand] button. Reading it is one `data modify`
# against the marker that is already @s. No entity selector, no cache, no
# cooldown, and no stale second after a change.
#
# A pipe built before this carries its frame's item in data.data.filter; the
# migration copies it across and the frame becomes decoration.

execute unless data entity @s data.properties.filter_item run return 0

data modify storage ra:temp filter_item set from entity @s data.properties.filter_item

# Compare item IDs. `data modify ... set from` reports success only when it
# actually changed something, so success 0 means the two ids were already equal.
data modify storage ra:temp check_id set from storage ra:temp pipe_item.id
execute store success score @s ra.temp run data modify storage ra:temp check_id set from storage ra:temp filter_item

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
