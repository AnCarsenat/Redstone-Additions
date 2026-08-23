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

# Item pipes join the shared transport engine as an item-class network, so a
# connected run is one network rather than a chain of independent hops.
execute as @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] run function ra_lib:transport/net/join {class:"item",capacity:0}

# Seeded EMPTY rather than left absent. The Data Handler draws a row only for a
# property the block actually has -- props/render returns before drawing when the
# name is missing -- so an absent filter_item meant a pipe with no way to set one.
# Empty string is the "no filter" value, which check_filter tests for.
data modify entity @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.filter_item set value ""

# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.item_pipe,tag=ra.new] remove ra.new

return 1
