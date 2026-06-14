# /ra_sensors:blocks/tag_adder/handle_placement
# Handle tag adder placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.tag_adder] run return 0

# Place green glazed terracotta with armor stand marker (dir_type:0 = non-directional)
function ra_lib:placement/place {block_id:"minecraft:green_glazed_terracotta",block_tag:"tag_adder",dir_type:0}

# Initialize default properties first
data modify entity @e[type=marker,tag=ra.custom_block.tag_adder,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties set value {entity_selector:"@e[type=pig,distance=..5]",tag:"custom_tag"}

# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.tag_adder,tag=ra.new,distance=..0.5] remove ra.new

return 1
