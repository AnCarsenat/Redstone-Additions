# /ra_gates:blocks/randomizer/handle_placement
# Handle Randomizer placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.randomizer] run return 0

# Place bookshelf with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:bookshelf",block_tag:"randomizer",dir_type:0}

# Initialize default properties (50% chance)
data modify entity @e[type=marker,tag=ra.custom_block.randomizer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.chance set value 50

tag @e[type=marker,tag=ra.custom_block.randomizer,tag=ra.new,distance=..0.5] remove ra.new

# Remove placement tag
tag @s remove ra.place.randomizer

return 1
