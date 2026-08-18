# /ra_infinite:blocks/mineral_generator/handle_placement
# Handle Mineral Generator placement
# Context: as bat, at bat position
#
# A real DROPPER, with no block_display skin over it. A generator needs a
# `facing` state so you can see where it will grow, and a dropper's front is
# unmistakable. It was a skinned barrel until a skin's own darkness gave it away:
# a block_display samples the light inside the opaque block it covers, which is
# zero, so every skin rendered black.
#
# Ejection is not a concern the way it is for the storage blocks: a generator
# never puts anything in its own inventory and never reads redstone.

execute unless entity @s[tag=ra.place.mineral_generator] run return 0

function ra_lib:placement/place {block_id:"minecraft:dropper",block_tag:"mineral_generator",dir_type:2}

data modify entity @e[type=marker,tag=ra.custom_block.mineral_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.cooldown set value 100


tag @e[type=marker,tag=ra.custom_block.mineral_generator,tag=ra.new] remove ra.new

return 1
