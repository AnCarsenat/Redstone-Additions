# /ra_gates:blocks/shortener/handle_placement
# Handle Shortener placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.shortener] run return 0

# Place quartz block with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:quartz_block",block_tag:"shortener",dir_type:0}

# Set default pulse length property (2 ticks)
data modify entity @e[type=marker,tag=ra.custom_block.shortener,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.pulse set value 2

# Add checkRed tag for signal detection
tag @e[type=marker,tag=ra.custom_block.shortener,tag=ra.new,distance=..0.5] add ra.checkRed
tag @e[type=marker,tag=ra.custom_block.shortener,tag=ra.new,distance=..0.5] remove ra.new

# Remove placement tag
tag @s remove ra.place.shortener

return 1
