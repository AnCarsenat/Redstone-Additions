# /ra_storage:storage_box/add_cached_input
# Context: as storage-box item entity
# Uses cached input at storage ra:temp storage_box.input_item and input_preview

execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.items run data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.items set value []
execute unless data entity @s Item.components."minecraft:custom_data".ra.storage_box.preview run data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.preview set value []

# 27-slot shulker-like limit
execute if data entity @s Item.components."minecraft:custom_data".ra.storage_box.items[26] run return 0

data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.items append from storage ra:temp storage_box.input_item
data modify entity @s Item.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview

function ra_storage:storage_box/update_lore

scoreboard players set #boxer_added ra.temp 1
return 1
