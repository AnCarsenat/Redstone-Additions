# /ra_storage:blocks/unboxer/process
# MACRO FUNCTION - unboxes one stored stack from input1 boxes into output1.
# Called with entity @s data.properties (requires input1/output1 strings).

# Hopper-like cooldown
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown matches ..3 run return 0

# Require valid input/output containers
$execute positioned $(input1) unless block ~ ~ ~ #ra_lib:containers run return 0
$execute positioned $(output1) unless block ~ ~ ~ #ra_lib:containers run return 0

# Select one candidate box item from input1 (or partner chest half)
$execute positioned $(input1) run function ra_interactive:blocks/item_mover/select_input
execute if score #mover_has_input ra.temp matches 0 run return 0

# Only process storage boxes (new key or legacy key) that contain at least one stored stack
execute unless data storage ra:temp mover_item.components."minecraft:custom_data".ra.item_box unless data storage ra:temp mover_item.components."minecraft:custom_data".ra.storage_box_item run return 0
execute unless data storage ra:temp mover_item.components."minecraft:custom_data".ra.storage_box.items[0] run return 0

# Forward the first stored stack into output1 (partial insert safe)
data modify storage ra:temp mover_item set from storage ra:temp mover_item.components."minecraft:custom_data".ra.storage_box.items[0]
execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}

scoreboard players set #unboxer_done ra.temp 0
$execute positioned $(output1) run function ra_interactive:blocks/item_mover/check_output_capacity
execute if score #mover_can_forward ra.temp matches 0 run return 0

$execute positioned $(output1) run function ra_interactive:blocks/item_mover/try_insert with storage ra:temp mover_item
execute if score #mover_inserted ra.temp matches 0 run return 0

# Consume inserted amount from the selected box's first stored entry (inlined dispatcher)
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 0 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_storage:storage_box/consume_from_selected_box_side
$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 1 if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_storage:storage_box/consume_from_selected_box_side
execute if score #unboxer_done ra.temp matches 0 run return 0

scoreboard players set @s ra.cooldown 0
playsound minecraft:block.wood.break block @a[distance=..16] ~ ~ ~ 0.8 1.1
particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5
