# /ra_interactive:blocks/big_torch/handle_placement
# Handle Big Torch placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.big_torch] run return 0

# An end rod, and always standing up.
#
# dir_type 0 places the block plain, and an end rod's default facing is `up`, so
# vertical is what you get without a facing to resolve -- which is the whole
# requirement. A torch lying on its side would also put its flame inside the
# block beside it.
#
# The end rod is the light: it gives off level 14 on its own, so the block lights
# its area whether or not the display over it ever renders.
function ra_lib:placement/place {block_id:"minecraft:end_rod",block_tag:"big_torch",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.big_torch,tag=ra.new,limit=1,sort=nearest] data.properties set value {radius:16}

function ra_interactive:blocks/big_torch/skin

tag @e[type=marker,tag=ra.custom_block.big_torch,tag=ra.new] remove ra.new

return 1
