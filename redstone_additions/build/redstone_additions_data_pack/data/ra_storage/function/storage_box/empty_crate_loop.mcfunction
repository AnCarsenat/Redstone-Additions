# /ra_storage:storage_box/empty_crate_loop
# Internal: move one stored stack out of the crate at Items[0], then recurse.
#
# The stack leaves the crate BEFORE it is delivered. Delivering first and
# clearing afterwards is what duplicated items when the two steps disagreed.

execute if score #unboxer_guard ra.temp matches ..0 run return 0
scoreboard players remove #unboxer_guard ra.temp 1

execute unless data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0] run return 0

data modify storage ra:temp mover_item set from block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0]
execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}

data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.items[0]
execute if data block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0] run data remove block ~ ~ ~ Items[0].components."minecraft:custom_data".ra.storage_box.preview[0]

function ra_storage:storage_box/deliver_stack with storage ra:temp unboxer
scoreboard players add #unboxer_moved ra.temp 1

function ra_storage:storage_box/empty_crate_loop
