# /ra_interactive:blocks/block_placer/handle_placement
# Handle block placer placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.block_placer] run return 0

# Place dispenser block with armor stand marker (dir_type:2 = full 6-directional)
function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"block_placer",dir_type:2}

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.block_placer,tag=ra.new] remove ra.new

return 1
