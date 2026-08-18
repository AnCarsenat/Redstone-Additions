# /ra_infinite:blocks/nether_generator/handle_placement
# Handle Nether Generator placement
# Context: as bat, at bat position
#
# A real dropper, no skin — see the Mineral Generator for why.

execute unless entity @s[tag=ra.place.nether_generator] run return 0

function ra_lib:placement/place {block_id:"minecraft:dropper",block_tag:"nether_generator",dir_type:2}

data modify entity @e[type=marker,tag=ra.custom_block.nether_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.cooldown set value 100


tag @e[type=marker,tag=ra.custom_block.nether_generator,tag=ra.new] remove ra.new

return 1
