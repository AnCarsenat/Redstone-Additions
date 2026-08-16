# /ra_storage:storage_box/take_first_from_box
# Context: positioned at the container side whose Items[0] is the storage box.
# Takes the box's first stored stack out whole, into storage ra:temp mover_item,
# and writes the shrunken box back. Sets #unboxer_done to 1 on success.
#
# The stack is removed BEFORE anything tries to insert it. The previous order
# inserted first and consumed afterwards, so any path that reached the insert but
# not the consume duplicated the items outright.

scoreboard players set #unboxer_done ra.temp 0

execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0] run return 0
execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview set value []

# Migrate touched legacy boxes to the new key while preserving compatibility.
execute if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box_item run data modify block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.item_box set value 1b

# Hand the whole entry to the caller, then drop it from the box.
data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0]
execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}

data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0]
execute if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0] run data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0]

# Rebuild lore from the remaining preview list.
data modify storage ra:temp storage_box.target_box set from block ~ ~ ~ Items[0]
function ra_storage:storage_box/update_lore_storage_target
data modify block ~ ~ ~ Items[0] set from storage ra:temp storage_box.target_box

scoreboard players set #unboxer_done ra.temp 1
return 1
