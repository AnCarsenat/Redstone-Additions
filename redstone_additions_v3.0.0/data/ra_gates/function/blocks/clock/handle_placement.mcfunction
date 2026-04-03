# /ra_gates:blocks/clock/handle_placement
# Handle AND gate placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.clock] run return 0

# Place daylight sensor with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:daylight_detector",block_tag:"clock",dir_type:0}

# Display item above the clock block
kill @e[type=item_display,tag=ra.custom_block.clock,distance=..0.6]
summon item_display ~ ~0.3 ~ {item:{id:"minecraft:clock",count:1},item_display:"fixed",transformation:{left_rotation:[0.7071f,0f,0f,0.7071f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]},Tags:["ra.custom_block.display_item","ra.custom_block.clock"]}

# Always set cooldown property to 20 after copying (overwrites any delay property)
data modify entity @n[tag=ra.custom_block.clock,tag=ra.new] data.properties.cooldown set value 20
scoreboard players set @n[tag=ra.custom_block.clock,tag=ra.new] ra.cooldown 0

# Remove placement tag
tag @s remove ra.place.clock

return 1