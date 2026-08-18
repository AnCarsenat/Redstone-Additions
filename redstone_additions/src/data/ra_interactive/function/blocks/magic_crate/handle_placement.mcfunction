# /ra_interactive:blocks/magic_crate/handle_placement
# Handle Magic Crate placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.magic_crate] run return 0

# dir_type 0: it reaches in every direction, so which way it was placed means
# nothing and a facing would only be one more thing to get wrong.
#
# A plain barrel, with no skin over it. It wore a hopper for a while, which was a
# lie in two directions: a hopper is five slots that push downwards, and this is
# twenty-seven slots that pull inwards. A crate looks like what it is.
function ra_lib:placement/place {block_id:"minecraft:barrel",block_tag:"magic_crate",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.magic_crate,tag=ra.new,limit=1,sort=nearest] data.properties set value {radius:8,cooldown:20}

tag @e[type=marker,tag=ra.custom_block.magic_crate,tag=ra.new] remove ra.new

return 1
