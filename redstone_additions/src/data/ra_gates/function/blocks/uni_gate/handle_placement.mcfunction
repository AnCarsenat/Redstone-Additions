# /ra_gates:blocks/uni_gate/handle_placement
# Handle UNI gate placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.uni_gate] run return 0
execute if entity @e[type=marker,tag=ra.custom_block.uni_gate,distance=..0.5,limit=1] run return 0
# Place smooth stone slab with armor stand marker
function ra_lib:placement/place {block_id:"minecraft:smooth_stone_slab",block_tag:"uni_gate",dir_type:0}

# Display offsets are measured from the marker, which sits at the BLOCK CENTRE
# (y + 0.5), not at the block's floor. An offset of ~0.5 therefore lands a full
# block above the floor — half a block clear of a slab's top face, which is why
# these displays floated. The offset below is (surface height - 0.5) plus a
# hair to keep the item out of the surface it rests on.
# Slab top face is at 0.5, so the offset from the centre is 0.
kill @e[type=item_display,tag=ra.custom_block.uni_gate,distance=..0.6]
kill @e[type=item_display,tag=ra.custom_block.display_item.uni_gate,distance=..0.6]
summon item_display ~ ~0.01 ~ {item:{id:"minecraft:comparator",count:1},item_display:"fixed",transformation:{left_rotation:[0.7071f,0f,0f,0.7071f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]},Tags:["ra.custom_block.display_item","ra.custom_block.display_item.uni_gate"]}

# Set default properties (gate_type: and, or, not, xor, nand, nor, xnor)
data modify entity @e[type=marker,tag=ra.custom_block.uni_gate,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.gate_type set value "and"
scoreboard players set @e[type=marker,tag=ra.custom_block.uni_gate,tag=ra.new,distance=..0.5] ra.cooldown 0


# Remove ra.new tag now that setup is complete
tag @e[type=marker,tag=ra.custom_block.uni_gate,tag=ra.new,distance=..0.5] remove ra.new

return 1
