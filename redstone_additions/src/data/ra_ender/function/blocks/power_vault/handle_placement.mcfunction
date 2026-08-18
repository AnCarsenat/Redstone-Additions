# /ra_ender:blocks/power_vault/handle_placement
# Context: as bat, at bat position.
#
# A vault is a wireless bridge between two electric grids. It carries
# ra.wires.electric_node so ra_wires:electric/adopt enrols it, and contributes no
# capacity: it is a link, not a battery. What it sends comes out of the grid it
# is wired into and lands on the grid at the far end.
#
# `transfer_rate` is the only knob -- how much EU crosses per cycle.

execute unless entity @s[tag=ra.place.ender_power_vault] run return 0

function ra_lib:placement/place {block_id:"minecraft:purpur_pillar",block_tag:"ender_power_vault",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.mode set value "link"
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.transfer_rate set value 80

tag @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5] add ra.wires.electric_node

tag @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5] remove ra.new
tag @s remove ra.place.ender_power_vault

return 1
