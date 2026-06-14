# /ra_interactive:blocks/item_pipe/handle_placement
# Handle item pipe placement
# Context: as bat, at bat position
# Called via function tag ra:register_placement

# Only process if this is an item pipe bat
execute unless entity @s[tag=ra.place.item_pipe] run return 0

# Place the block using the library (dir_type:2 = full 6-directional)
function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"item_pipe",dir_type:2}

# Snap marker to centered world-grid coordinates.
execute as @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.item_pipe,tag=ra.new] remove ra.new

return 1