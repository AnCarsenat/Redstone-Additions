# /ra_sensors:blocks/tag_remover/handle_placement
# Handle tag remover placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.tag_remover] run return 0

# Place red glazed terracotta with armor stand marker (dir_type:0 = non-directional)
function ra_lib:placement/place {block_id:"minecraft:red_glazed_terracotta",block_tag:"tag_remover",dir_type:0}

# Initialize default properties
data modify entity @e[type=marker,tag=ra.custom_block.tag_remover,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties set value {entity_selector:"@e[distance=..3]",tag:"custom_tag"}

# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.tag_remover,tag=ra.new,distance=..0.5] remove ra.new

return 1
