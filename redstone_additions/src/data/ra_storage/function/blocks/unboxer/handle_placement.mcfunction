# /ra_storage:blocks/unboxer/handle_placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.unboxer] run return 0

function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"unboxer",dir_type:2}

data modify entity @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.input1 set value "~ ~ ~"
data modify entity @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.output1 set value "^ ^ ^1"

tag @e[tag=ra.custom_block.unboxer,tag=ra.new] remove ra.new

return 1
