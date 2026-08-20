# /ra_interactive:blocks/big_torch/handle_placement
# Handle Big Torch placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.big_torch] run return 0

# dir_type 0: it works in every direction, so which way it was placed means
# nothing.
#
# Shroomlight rather than a real torch: the marker needs a full block to sit in
# and to test for when it checks whether it has been broken, and a torch is
# neither full nor solid. It glows on its own, which is what a torch should do,
# and the item still shows as a torch.
function ra_lib:placement/place {block_id:"minecraft:shroomlight",block_tag:"big_torch",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.big_torch,tag=ra.new,limit=1,sort=nearest] data.properties set value {radius:16}

tag @e[type=marker,tag=ra.custom_block.big_torch,tag=ra.new] remove ra.new

return 1
