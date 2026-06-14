# /ra_storage:storage_box/add_cached_input_block_item_box
# Context: positioned at output container
# Uses cached input at storage ra:temp storage_box.input_item and input_preview.

data modify storage ra:temp storage_box.target_box set from block ~ ~ ~ Items[{id:"minecraft:player_head",components:{"minecraft:custom_data":{ra:{item_box:1b}}}}]
execute unless data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items set value []
execute unless data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview set value []

# 27-slot shulker-like limit
execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[26] run return 0

data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items append from storage ra:temp storage_box.input_item
data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
function ra_storage:storage_box/update_lore_storage_target

data modify block ~ ~ ~ Items[{id:"minecraft:player_head",components:{"minecraft:custom_data":{ra:{item_box:1b}}}}] set from storage ra:temp storage_box.target_box

scoreboard players set #boxer_added ra.temp 1
return 1
