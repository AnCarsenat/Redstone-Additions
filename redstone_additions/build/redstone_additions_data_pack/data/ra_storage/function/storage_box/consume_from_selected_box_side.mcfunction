# /ra_storage:storage_box/consume_from_selected_box_side
# Context: positioned at selected input container side with box entry at Items[0].
# Uses #mover_inserted as the amount to remove from stored entry 0.

execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0] run return 0
execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview set value []

# Migrate touched legacy boxes to the new key while preserving compatibility.
execute if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box_item run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.item_box set value 1b

execute store result score @s ra.inv.count run data get block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0].count
scoreboard players operation @s ra.inv.count -= #mover_inserted ra.temp

# Remove first stored entry and preview when fully consumed
execute if score @s ra.inv.count matches ..0 run data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0]
execute if score @s ra.inv.count matches ..0 if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0] run data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0]

# Keep first entry and refresh preview count when partially consumed
execute if score @s ra.inv.count matches 1.. store result block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0].count int 1 run scoreboard players get @s ra.inv.count
execute if score @s ra.inv.count matches 1.. run data modify storage ra:temp storage_box.input_macro.id set from block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0].id
execute if score @s ra.inv.count matches 1.. run execute store result storage ra:temp storage_box.input_macro.count int 1 run data get block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0].count
execute if score @s ra.inv.count matches 1.. run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
execute if score @s ra.inv.count matches 1.. run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0] set from storage ra:temp storage_box.input_preview

# Rebuild lore from preview list
data modify storage ra:temp storage_box.target_box set from block ~ ~ ~ Items[0]
function ra_storage:storage_box/update_lore_storage_target
data modify block ~ ~ ~ Items[0] set from storage ra:temp storage_box.target_box

scoreboard players set #unboxer_done ra.temp 1
return 1
