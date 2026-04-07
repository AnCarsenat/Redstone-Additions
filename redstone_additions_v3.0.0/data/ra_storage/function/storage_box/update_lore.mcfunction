# /ra_storage:storage_box/update_lore
# Context: as storage-box item entity
# Keeps lore as first five preview entries (gray text)

execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.items run data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.items set value []
execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview run data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.preview set value []

data modify entity @s Item.components."minecraft:lore" set value []

execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[0] run data modify entity @s Item.components."minecraft:lore" append from entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[0]
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[1] run data modify entity @s Item.components."minecraft:lore" append from entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[1]
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[2] run data modify entity @s Item.components."minecraft:lore" append from entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[2]
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[3] run data modify entity @s Item.components."minecraft:lore" append from entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[3]
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[4] run data modify entity @s Item.components."minecraft:lore" append from entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[4]

execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[5] run data modify entity @s Item.components."minecraft:lore" append value {text:"... and more",color:"gray",italic:false}
execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview[0] run data modify entity @s Item.components."minecraft:lore" append value {text:"(empty)",color:"gray",italic:false}
