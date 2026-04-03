# /ra_gates:blocks/clock/handle_placement
# Handle AND gate placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.clock] run return 0
execute if entity @e[type=armor_stand,tag=ra.custom_block.clock,distance=..0.5,limit=1] run return 0

# Place daylight sensor with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:daylight_detector",block_tag:"clock",dir_type:0}

# Display item above the clock block
kill @e[type=item_display,tag=ra.custom_block.clock,distance=..0.6]
kill @e[type=item_display,tag=ra.custom_block.display_item.clock,distance=..0.6]
summon item_display ~ ~0.46 ~ {item:{id:"minecraft:clock",count:1},item_display:"fixed",transformation:{left_rotation:[0.7071f,0f,0f,0.7071f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]},Tags:["ra.custom_block.display_item","ra.custom_block.display_item.clock"]}

# Always set cooldown property to 20 after copying (overwrites any delay property)
data modify entity @n[tag=ra.custom_block.clock,tag=ra.new] data.properties.cooldown set value 20
scoreboard players set @n[tag=ra.custom_block.clock,tag=ra.new] ra.cooldown 0

# Remove ra.new tag now that setup is complete
tag @e[tag=ra.custom_block.clock,tag=ra.new] remove ra.new

# Remove placement tag
tag @s remove ra.place.clock

return 1