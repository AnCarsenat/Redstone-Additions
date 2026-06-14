# /ra_interactive:blocks/spitter/handle_placement
# Handle spitter placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.spitter] run return 0

# Place dropper block with armor stand marker (dir_type:2 = full 6-directional)
function ra_lib:placement/place {block_id:"minecraft:dropper",block_tag:"spitter",dir_type:2}

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.spitter,tag=ra.new] remove ra.new

return 1
