# /ra_infinite:blocks/poppy_generator/handle_placement
# Handle Poppy Generator placement
# Context: as bat, at bat position
#
# A real dropper, no skin — see the Mineral Generator for why.

execute unless entity @s[tag=ra.place.poppy_generator] run return 0

function ra_lib:placement/place {block_id:"minecraft:dropper",block_tag:"poppy_generator",dir_type:2}

data modify entity @e[type=marker,tag=ra.custom_block.poppy_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b
data modify entity @e[type=marker,tag=ra.custom_block.poppy_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.cooldown set value 80
data modify entity @e[type=marker,tag=ra.custom_block.poppy_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.mode set value "single"


tag @e[type=marker,tag=ra.custom_block.poppy_generator,tag=ra.new] remove ra.new

return 1
