# /ra_ender:blocks/item_vault/handle_placement
# Context: as bat, at bat position.
#
# A real barrel: the vault's contents are the barrel's contents, so a hopper, a
# player and an Item Pipe all interact with it without any special case.

execute unless entity @s[tag=ra.place.ender_item_vault] run return 0

function ra_lib:placement/place {block_id:"minecraft:barrel",block_tag:"ender_item_vault",dir_type:0}

data modify entity @e[type=marker,tag=ra.custom_block.ender_item_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.channel set value "default"
data modify entity @e[type=marker,tag=ra.custom_block.ender_item_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.mode set value "link"
data modify entity @e[type=marker,tag=ra.custom_block.ender_item_vault,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.enabled set value 1b

tag @e[type=marker,tag=ra.custom_block.ender_item_vault,tag=ra.new,distance=..0.5] remove ra.new
tag @s remove ra.place.ender_item_vault

return 1
