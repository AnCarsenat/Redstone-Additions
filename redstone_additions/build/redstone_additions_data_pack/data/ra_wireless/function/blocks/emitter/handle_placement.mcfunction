# /ra_wireless:blocks/emitter/handle_placement
# Handle Emitter placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.emitter] run return 0

# Place end stone bricks with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:end_stone_bricks",block_tag:"emitter",dir_type:0}

# Set default channel (string) and enabled state
data modify entity @e[type=marker,tag=ra.custom_block.emitter,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.emitter,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b

tag @e[type=marker,tag=ra.custom_block.emitter,tag=ra.new,distance=..0.5] remove ra.new

# Remove placement tag
tag @s remove ra.place.emitter

return 1
