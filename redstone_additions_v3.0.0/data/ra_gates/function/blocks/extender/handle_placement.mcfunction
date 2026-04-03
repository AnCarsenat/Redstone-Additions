# /ra_gates:blocks/extender/handle_placement
# Handle Extender placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.extender] run return 0

# Place quartz block with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:quartz_block",block_tag:"extender",dir_type:0}

# Set default extend property (20 ticks = 1 second)
data modify entity @e[type=marker,tag=ra.custom_block.extender,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.extend set value 20

# Add checkRed tag for signal detection
tag @e[type=marker,tag=ra.custom_block.extender,tag=ra.new,distance=..0.5] add ra.checkRed
tag @e[type=marker,tag=ra.custom_block.extender,tag=ra.new,distance=..0.5] remove ra.new

# Remove placement tag
tag @s remove ra.place.extender

return 1
