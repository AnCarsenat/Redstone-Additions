# /ra_interactive:blocks/message_block/handle_placement
# Handle message_block block placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.message_block] run return 0

# Place note_block with armor stand marker (dir_type:0 = no direction)
function ra_lib:placement/place {block_id:"minecraft:note_block",block_tag:"message_block",dir_type:0}

# Initialize default properties first
data modify entity @e[type=marker,tag=ra.custom_block.message_block,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties set value {message_block:"Hello World!",entity_selector:"@a[distance=..16]"}

# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.message_block,tag=ra.new,distance=..0.5] remove ra.new

return 1
