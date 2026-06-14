# /ra_storage:storage_box/update_lore_storage_target
# Context: storage ra:temp storage_box.target_box
# Keeps lore as the first five preview entries (gray text).

execute unless data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items set value []
execute unless data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview set value []

data modify storage ra:temp storage_box.target_box.components."minecraft:lore" set value []

execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[0] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[0]
execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[1] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[1]
execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[2] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[2]
execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[3] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[3]
execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[4] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[4]

execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[5] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append value {text:"... and more",color:"gray",italic:false}
execute unless data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview[0] run data modify storage ra:temp storage_box.target_box.components."minecraft:lore" append value {text:"(empty)",color:"gray",italic:false}
