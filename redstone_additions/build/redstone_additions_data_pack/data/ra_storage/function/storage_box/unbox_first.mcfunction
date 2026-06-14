# /ra_storage:storage_box/unbox_first
# Context: as storage-box item entity
# Removes first stored stack and drops it at the box position

execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.items[0] run return 0

data modify storage ra:temp storage_box.output_item set from entity @s Item.components."minecraft:custom_data".ra.storage_box.items[0]

summon item ~ ~ ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra.storage_box.drop"],PickupDelay:10}
data modify entity @e[type=item,tag=ra.storage_box.drop,distance=..0.6,limit=1,sort=nearest] Item set from storage ra:temp storage_box.output_item
tag @e[type=item,tag=ra.storage_box.drop,distance=..0.6,limit=1,sort=nearest] remove ra.storage_box.drop

data remove entity @s Item.components."minecraft:custom_data".ra.storage_box.items[0]
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[0] run data remove entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[0]

function ra_storage:storage_box/update_lore

scoreboard players set #unboxer_done ra.temp 1
return 1
