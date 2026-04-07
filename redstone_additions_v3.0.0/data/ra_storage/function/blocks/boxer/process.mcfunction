# /ra_storage:blocks/boxer/process
# MACRO FUNCTION - copy all items from input1 into one item_box and insert into output1.
# Called with entity @s data.properties (requires input1/output1 strings).

# Hopper-like cooldown
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown matches ..3 run return 0

# Require valid input/output containers
$execute positioned $(input1) unless block ~ ~ ~ #ra_lib:containers run return 0
$execute positioned $(output1) unless block ~ ~ ~ #ra_lib:containers run return 0

# Require input container to have at least one item entry
$execute positioned $(input1) unless data block ~ ~ ~ Items[0] run return 0

# Build one storage box item from the entire input container state (inlined)
$execute positioned $(input1) run data modify storage ra:temp storage_box.target_box set value {id:"minecraft:player_head",count:1,components:{"minecraft:item_name":"Storage Box","minecraft:profile":"BoxMan01234","minecraft:max_stack_size":64,"minecraft:lore":[{text:"(empty)",color:"gray",italic:false}],"minecraft:custom_data":{ra:{item_box:1b,storage_box:{items:[],preview:[]}}}}}
$execute positioned $(input1) run execute if data block ~ ~ ~ Items run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items set from block ~ ~ ~ Items
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0] run data modify storage ra:temp storage_box.input_macro.id set from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0].id
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0] store result storage ra:temp storage_box.input_macro.count int 1 run data get storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0].count
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0] run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[0] run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1] run data modify storage ra:temp storage_box.input_macro.id set from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1].id
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1] store result storage ra:temp storage_box.input_macro.count int 1 run data get storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1].count
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1] run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[1] run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2] run data modify storage ra:temp storage_box.input_macro.id set from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2].id
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2] store result storage ra:temp storage_box.input_macro.count int 1 run data get storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2].count
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2] run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[2] run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3] run data modify storage ra:temp storage_box.input_macro.id set from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3].id
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3] store result storage ra:temp storage_box.input_macro.count int 1 run data get storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3].count
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3] run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[3] run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4] run data modify storage ra:temp storage_box.input_macro.id set from storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4].id
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4] store result storage ra:temp storage_box.input_macro.count int 1 run data get storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4].count
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4] run function ra_storage:storage_box/make_preview_from_input with storage ra:temp storage_box.input_macro
$execute positioned $(input1) run execute if data storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.items[4] run data modify storage ra:temp storage_box.target_box.components."minecraft:custom_data".ra.storage_box.preview append from storage ra:temp storage_box.input_preview
$execute positioned $(input1) run function ra_storage:storage_box/update_lore_storage_target
$execute positioned $(input1) run data modify storage ra:temp storage_box.generated_item set from storage ra:temp storage_box.target_box

# Insert only if output side can accept one item_box
data modify storage ra:temp mover_item set from storage ra:temp storage_box.generated_item
$execute positioned $(output1) run function ra_interactive:blocks/item_mover/check_output_capacity
execute if score #mover_can_forward ra.temp matches 0 run return 0

$execute positioned $(output1) run function ra_interactive:blocks/item_mover/try_insert with storage ra:temp mover_item
execute if score #mover_inserted ra.temp matches 0 run return 0

# Remove all input entries only after successful insertion
$execute positioned $(input1) run data remove block ~ ~ ~ Items

scoreboard players set @s ra.cooldown 0
playsound minecraft:block.wood.place block @a[distance=..16] ~ ~ ~ 0.8 1.2
particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5
