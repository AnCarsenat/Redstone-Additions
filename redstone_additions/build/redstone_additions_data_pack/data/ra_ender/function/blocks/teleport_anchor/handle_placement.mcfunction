# /ra_ender:blocks/teleport_anchor/handle_placement
# Context: as bat, at bat position.
#
# `id` is what other anchors aim at. `targets` is the table of fifteen ids, one
# per redstone strength: signal 1 uses targets[0], signal 15 uses targets[14].
# Zero means "nothing wired to that strength".

execute unless entity @s[tag=ra.place.teleport_anchor] run return 0

function ra_lib:placement/place {block_id:"minecraft:crying_obsidian",block_tag:"teleport_anchor",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.anchor_id set value "A"
data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.targets set value ["","","","","","","","","","","","","","",""]

tag @e[type=marker,tag=ra.custom_block.teleport_anchor,tag=ra.new,distance=..0.5] remove ra.new
tag @s remove ra.place.teleport_anchor

return 1
