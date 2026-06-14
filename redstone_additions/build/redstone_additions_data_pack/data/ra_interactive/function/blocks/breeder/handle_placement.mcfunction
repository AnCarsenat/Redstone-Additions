# /ra_interactive:blocks/breeder/handle_placement
# Handle breeder placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.breeder] run return 0

# Place dispenser block with armor stand marker (dir_type:2 = full 6-directional)
function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"breeder",dir_type:2}

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.breeder,tag=ra.new] remove ra.new

return 1
