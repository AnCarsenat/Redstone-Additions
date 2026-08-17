# /ra_ender:blocks/power_vault/handle_placement
# Context: as bat, at bat position.
#
# Electric is not on the transport engine: EU sits in each node's own buffer and
# moves between adjacent nodes. So a power vault is tagged ra.wires.electric_node
# and carries the same data.data fields, which is all ra_wires needs to treat it
# as part of a wire run. Its capacity is set here because ra_wires:electric/
# init_node only knows the defaults of its own blocks.

execute unless entity @s[tag=ra.place.ender_power_vault] run return 0

function ra_lib:placement/place {block_id:"minecraft:purpur_pillar",block_tag:"ender_power_vault",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.mode set value "link"
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.transfer_rate set value 80
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.data.eu set value 0
data modify entity @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.data.capacity set value 400

tag @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5] add ra.wires.electric_node

tag @e[type=marker,tag=ra.custom_block.ender_power_vault,tag=ra.new,distance=..0.5] remove ra.new
tag @s remove ra.place.ender_power_vault

return 1
