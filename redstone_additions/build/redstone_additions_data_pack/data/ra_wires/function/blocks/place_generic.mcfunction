# /ra_wires:blocks/place_generic {block,marker,...}
# Internal: place one RA Wires block and finish setting its marker up.
# Context: as the placement bat, at the block position.

$function ra_lib:placement/place {block_id:"$(block)",block_tag:"$(marker)",dir_type:$(dir)}

$execute as @e[type=marker,tag=ra.custom_block.$(marker),tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:blocks/place_finish
$tag @e[type=marker,tag=ra.custom_block.$(marker),tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
