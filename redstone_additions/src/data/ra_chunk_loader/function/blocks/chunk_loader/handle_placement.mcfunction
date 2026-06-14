# /ra_chunk_loader:blocks/chunk_loader/handle_placement
# Handle chunk loader placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.chunk_loader] run return 0

# Place lodestone with armor stand marker (dir_type:0 = no direction)
function ra_lib:placement/place {block_id:"minecraft:lodestone",block_tag:"chunk_loader",dir_type:0}

# Remove setup tag after initialization
tag @e[tag=ra.custom_block.chunk_loader,tag=ra.new] remove ra.new

return 1
