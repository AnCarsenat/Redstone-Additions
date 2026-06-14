# /ra_sensors:blocks/entity_detector/handle_placement
# Handle entity detector placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.entity_detector] run return 0

# Place sculk sensor with armor stand marker (dir_type:0 = non-directional)
function ra_lib:placement/place {block_id:"minecraft:waxed_chiseled_copper",block_tag:"entity_detector",dir_type:0}

# Initialize default properties first
data modify entity @e[type=marker,tag=ra.custom_block.entity_detector,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties set value {entity_selector:"@e[type=minecraft:pig,distance=..5]"}

# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.entity_detector,tag=ra.new,distance=..0.5] remove ra.new

return 1
