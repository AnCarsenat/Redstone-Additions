# /ra_interactive:blocks/item_mover/handle_placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.item_mover] run return 0

# Place dispenser with full 6-directional facing (same block family as item_pipe).
function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"item_mover",dir_type:2}

# Remove ra.new tag
tag @e[tag=ra.custom_block.item_mover,tag=ra.new] remove ra.new

return 1
