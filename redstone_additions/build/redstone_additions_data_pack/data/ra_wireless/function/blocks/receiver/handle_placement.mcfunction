# /ra_wireless:blocks/receiver/handle_placement
# Handle Receiver placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.receiver] run return 0

# Place purpur block with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:purpur_block",block_tag:"receiver",dir_type:0}

# Set default channel (string) and enabled state
data modify entity @e[type=marker,tag=ra.custom_block.receiver,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.receiver,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b

# Remove ra.new tag
tag @e[type=marker,tag=ra.custom_block.receiver,tag=ra.new,distance=..0.5] remove ra.new

# Remove placement tag
tag @s remove ra.place.receiver

return 1
