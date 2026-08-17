# /ra_ender:blocks/fluid_vault/handle_placement
# Context: as bat, at bat position.
#
# The vault is an ordinary fluid node with a small buffer, so pipes, pumps and
# tanks connect to it with no special case. What it adds is the link: whatever
# reaches its network can be pushed to the network of the vault on its channel.

execute unless entity @s[tag=ra.place.ender_fluid_vault] run return 0

function ra_lib:placement/place {block_id:"minecraft:purpur_block",block_tag:"ender_fluid_vault",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.mode set value "link"
data modify entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b
data modify entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.transfer_rate set value 200

# Fluid network membership, and the fluid flag the wires status readouts look for.
tag @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5] add ra.wires.fluid_node
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] run function ra_lib:transport/net/join {class:"fluid",capacity:1000}

tag @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=ra.new,distance=..0.5] remove ra.new
tag @s remove ra.place.ender_fluid_vault

return 1
